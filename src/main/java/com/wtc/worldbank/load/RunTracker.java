package com.wtc.worldbank.load;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RunTracker {

    public static void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS etl_runs (
                    id                    SERIAL PRIMARY KEY,
                    started_at            TIMESTAMP NOT NULL DEFAULT now(),
                    finished_at           TIMESTAMP,
                    status                TEXT NOT NULL,
                    countries_loaded      INT,
                    observations_loaded   INT,
                    rows_rejected         INT,
                    error_message         TEXT
                )
                """);
        }
    }

    public long start(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO etl_runs (status) VALUES ('RUNNING') RETURNING id");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public void success(Connection conn, long id, int countries, int observations, int rejected)
            throws SQLException {
        String sql = """
            UPDATE etl_runs
            SET status = 'SUCCESS', finished_at = now(),
                countries_loaded = ?, observations_loaded = ?, rows_rejected = ?
            WHERE id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, countries);
            ps.setInt(2, observations);
            ps.setInt(3, rejected);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    public void failure(Connection conn, long id, String message) throws SQLException {
        String sql = "UPDATE etl_runs SET status = 'FAILED', finished_at = now(), error_message = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }
}