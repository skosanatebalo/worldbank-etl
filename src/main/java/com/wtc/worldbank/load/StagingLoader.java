package com.wtc.worldbank.load;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class StagingLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public void loadObservations(Connection conn, Path rawDir) throws IOException, SQLException {
        conn.setAutoCommit(false);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE stg_observations");
        }

        String sql = "INSERT INTO stg_observations "
                + "(indicator_id, country_iso3, country_name, year, value, source_file) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             DirectoryStream<Path> files = Files.newDirectoryStream(rawDir, "*_page*.json")) {

            for (Path file : files) {
                JsonNode records = mapper.readTree(Files.readString(file)).get(1);

                for (JsonNode r : records) {
                    ps.setString(1, r.get("indicator").get("id").asText());
                    ps.setString(2, r.get("countryiso3code").asText());
                    ps.setString(3, r.get("country").get("value").asText());
                    ps.setString(4, r.get("date").asText());
                    ps.setString(5, r.get("value").isNull() ? null : r.get("value").asText());
                    ps.setString(6, file.getFileName().toString());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
        conn.commit();
    }

    public void loadCountries(Connection conn, Path countriesFile) throws IOException, SQLException {
        conn.setAutoCommit(false);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE stg_countries");
        }

        String sql = "INSERT INTO stg_countries "
                + "(id, iso2_code, name, region, income_level, capital_city, longitude, latitude, source_file) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        JsonNode records = mapper.readTree(Files.readString(countriesFile)).get(1);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (JsonNode r : records) {
                ps.setString(1, r.get("id").asText());
                ps.setString(2, r.get("iso2Code").asText());
                ps.setString(3, r.get("name").asText());
                ps.setString(4, r.get("region").get("value").asText().trim());
                ps.setString(5, r.get("incomeLevel").get("value").asText());
                ps.setString(6, r.get("capitalCity").asText());
                ps.setString(7, r.get("longitude").asText());
                ps.setString(8, r.get("latitude").asText());
                ps.setString(9, countriesFile.getFileName().toString());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        conn.commit();
    }
}