package com.wtc.worldbank.load;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class WarehouseSchema {

    public static void create(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS countries (
                    iso3          CHAR(3) PRIMARY KEY,
                    iso2          VARCHAR(2),
                    name          TEXT NOT NULL,
                    region        TEXT NOT NULL,
                    income_level  TEXT,
                    capital       TEXT,
                    longitude     DOUBLE PRECISION,
                    latitude      DOUBLE PRECISION
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS indicators (
                    id    TEXT PRIMARY KEY,
                    name  TEXT NOT NULL
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS observations (
                    indicator_id  TEXT      NOT NULL REFERENCES indicators(id),
                    country_iso3  CHAR(3)   NOT NULL REFERENCES countries(iso3),
                    year          SMALLINT  NOT NULL,
                    value         DOUBLE PRECISION NOT NULL,
                    PRIMARY KEY (indicator_id, country_iso3, year)
                )
                """);
        }
    }
}