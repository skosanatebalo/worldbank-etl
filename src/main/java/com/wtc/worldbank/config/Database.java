package com.wtc.worldbank.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/worldbank";
    private static final String DEFAULT_USER = "etl";
    private static final String DEFAULT_PASSWORD = "etl_pass";

    public static Connection getConnection() throws SQLException {
        String url = envOrDefault("DB_URL", DEFAULT_URL);
        String user = envOrDefault("DB_USER", DEFAULT_USER);
        String password = envOrDefault("DB_PASSWORD", DEFAULT_PASSWORD);
        return DriverManager.getConnection(url, user, password);
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value != null ? value : fallback;
    }
}