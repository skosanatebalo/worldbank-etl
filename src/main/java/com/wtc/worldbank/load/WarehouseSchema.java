package com.wtc.worldbank.load;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class WarehouseSchema {

    public static void create(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS countries (
                    iso3         TEXT PRIMARY KEY,
                    iso2         TEXT,
                    name         TEXT NOT NULL,
                    region       TEXT,
                    income_level TEXT,
                    capital      TEXT,
                    longitude    DOUBLE PRECISION,
                    latitude     DOUBLE PRECISION
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS indicators (
                    id   TEXT PRIMARY KEY,
                    name TEXT NOT NULL
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS observations (
                    country_iso3 TEXT REFERENCES countries(iso3),
                    indicator_id TEXT REFERENCES indicators(id),
                    year         INT,
                    value        DOUBLE PRECISION,
                    PRIMARY KEY (country_iso3, indicator_id, year)
                )
                """);
        }
    }
}