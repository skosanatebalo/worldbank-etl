package com.wtc.worldbank.load;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class StagingSchema {

    public static void create(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stg_observations (
                    indicator_id  TEXT,
                    country_iso3  TEXT,
                    country_name  TEXT,
                    year          TEXT,
                    value         TEXT,
                    source_file   TEXT,
                    loaded_at     TIMESTAMP DEFAULT now()
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS stg_countries (
                    id            TEXT,
                    iso2_code     TEXT,
                    name          TEXT,
                    region        TEXT,
                    income_level  TEXT,
                    capital_city  TEXT,
                    longitude     TEXT,
                    latitude      TEXT,
                    source_file   TEXT,
                    loaded_at     TIMESTAMP DEFAULT now()
                )
                """);
        }
    }
}
