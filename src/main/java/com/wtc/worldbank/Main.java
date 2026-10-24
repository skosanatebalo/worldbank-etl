package com.wtc.worldbank;

import com.wtc.worldbank.config.Database;
import com.wtc.worldbank.extract.IndicatorExtractor;
import com.wtc.worldbank.extract.WorldBankClient;
import com.wtc.worldbank.load.StagingLoader;
import com.wtc.worldbank.load.StagingSchema;
import com.wtc.worldbank.transform.TransformRunner;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) throws Exception {
        // Stage 0: check the database connection
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version()")) {
            if (rs.next()) {
                System.out.println(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }

        // Stage 1: extract raw JSON from the World Bank API
        WorldBankClient client = new WorldBankClient();
        IndicatorExtractor extractor = new IndicatorExtractor(client, Path.of("raw"));

        for (String indicator : new String[] {"NY.GDP.MKTP.CD", "SP.POP.TOTL", "SL.UEM.TOTL.ZS"}) {
            extractor.extract(indicator, 2015, 2023);
        }

        // Stage 2: load raw files into staging tables
        try (Connection conn = Database.getConnection()) {
            StagingSchema.create(conn);
            StagingLoader loader = new StagingLoader();
            loader.loadObservations(conn, Path.of("raw"));
            loader.loadCountries(conn, Path.of("raw/countries.json"));
        }

        // Stage 3: transform and validate
        try (Connection conn = Database.getConnection()) {
            TransformRunner.Output out = new TransformRunner().run(conn);
            System.out.println("Valid countries: " + out.countries().size());
            System.out.println("Valid observations: " + out.observations().size());
            System.out.println("Rejected rows: " + out.rejectedCount());
        }
    }
}