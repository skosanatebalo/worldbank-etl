package com.wtc.worldbank.load;

import com.wtc.worldbank.model.Country;
import com.wtc.worldbank.model.Observation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

public class WarehouseLoader {

    private static final Map<String, String> INDICATOR_NAMES = Map.of(
            "NY.GDP.MKTP.CD", "GDP (current US$)",
            "SP.POP.TOTL", "Population, total",
            "SL.UEM.TOTL.ZS", "Unemployment, total (% of total labor force)");

    public void load(Connection conn, List<Country> countries, List<Observation> observations)
            throws SQLException {
        boolean originalAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            upsertCountries(conn, countries);
            upsertIndicators(conn);
            upsertObservations(conn, observations);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    private void upsertCountries(Connection conn, List<Country> countries) throws SQLException {
        String sql = """
            INSERT INTO countries (iso3, iso2, name, region, income_level, capital, longitude, latitude)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (iso3) DO UPDATE SET
                iso2 = EXCLUDED.iso2,
                name = EXCLUDED.name,
                region = EXCLUDED.region,
                income_level = EXCLUDED.income_level,
                capital = EXCLUDED.capital,
                longitude = EXCLUDED.longitude,
                latitude = EXCLUDED.latitude
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Country c : countries) {
                ps.setString(1, c.iso3());
                ps.setString(2, c.iso2());
                ps.setString(3, c.name());
                ps.setString(4, c.region());
                ps.setString(5, c.incomeLevel());
                ps.setString(6, c.capital());
                setNullableDouble(ps, 7, c.longitude());
                setNullableDouble(ps, 8, c.latitude());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void upsertIndicators(Connection conn) throws SQLException {
        String sql = """
            INSERT INTO indicators (id, name) VALUES (?, ?)
            ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> e : INDICATOR_NAMES.entrySet()) {
                ps.setString(1, e.getKey());
                ps.setString(2, e.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void upsertObservations(Connection conn, List<Observation> observations) throws SQLException {
        String sql = """
            INSERT INTO observations (indicator_id, country_iso3, year, value)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (indicator_id, country_iso3, year) DO UPDATE SET value = EXCLUDED.value
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Observation o : observations) {
                ps.setString(1, o.indicatorId());
                ps.setString(2, o.countryIso3());
                ps.setInt(3, o.year());
                ps.setDouble(4, o.value());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void setNullableDouble(PreparedStatement ps, int index, Double value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DOUBLE);
        } else {
            ps.setDouble(index, value);
        }
    }
}