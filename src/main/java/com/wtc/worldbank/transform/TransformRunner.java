package com.wtc.worldbank.transform;

import com.wtc.worldbank.model.Country;
import com.wtc.worldbank.model.Observation;
import com.wtc.worldbank.model.RejectedRow;
import com.wtc.worldbank.transform.CountryTransformer.RawCountry;
import com.wtc.worldbank.transform.ObservationTransformer.RawObservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TransformRunner {

    public record Output(List<Country> countries, List<Observation> observations, int rejectedCount) {}

    public Output run(Connection conn) throws SQLException {
        CountryTransformer.Result countryResult =
                new CountryTransformer().transform(readCountries(conn));

        Set<String> validCodes = countryResult.valid().stream()
                .map(Country::iso3)
                .collect(Collectors.toSet());

        ObservationTransformer.Result obsResult =
                new ObservationTransformer().transform(readObservations(conn), validCodes);

        List<RejectedRow> allRejected = new ArrayList<>(countryResult.rejected());
        allRejected.addAll(obsResult.rejected());
        writeRejected(conn, allRejected);

        return new Output(countryResult.valid(), obsResult.valid(), allRejected.size());
    }

    private List<RawCountry> readCountries(Connection conn) throws SQLException {
        String sql = "SELECT id, iso2_code, name, region, income_level, capital_city, longitude, latitude "
                + "FROM stg_countries";
        List<RawCountry> out = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new RawCountry(
                        rs.getString("id"), rs.getString("iso2_code"), rs.getString("name"),
                        rs.getString("region"), rs.getString("income_level"),
                        rs.getString("capital_city"), rs.getString("longitude"),
                        rs.getString("latitude")));
            }
        }
        return out;
    }

    private List<RawObservation> readObservations(Connection conn) throws SQLException {
        String sql = "SELECT indicator_id, country_iso3, year, value FROM stg_observations";
        List<RawObservation> out = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                out.add(new RawObservation(
                        rs.getString("indicator_id"), rs.getString("country_iso3"),
                        rs.getString("year"), rs.getString("value")));
            }
        }
        return out;
    }

    private void writeRejected(Connection conn, List<RejectedRow> rejected) throws SQLException {
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE rejected_rows");
        }
        String sql = "INSERT INTO rejected_rows (source_table, row_key, reason) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (RejectedRow r : rejected) {
                ps.setString(1, r.sourceTable());
                ps.setString(2, r.key());
                ps.setString(3, r.reason());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }
}