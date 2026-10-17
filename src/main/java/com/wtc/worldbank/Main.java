package com.wtc.worldbank;

import com.wtc.worldbank.config.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.wtc.worldbank.extract.WorldBankClient;
import java.nio.file.Files;
import java.nio.file.Path;
import com.wtc.worldbank.extract.IndicatorExtractor;
public class Main {

    public static void main(String[] args) throws Exception{
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version()")) {
            if (rs.next()) {
                System.out.println(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
        WorldBankClient client = new WorldBankClient();
        IndicatorExtractor extractor = new IndicatorExtractor(client, Path.of("raw"));

        for (String indicator : new String[] {"NY.GDP.MKTP.CD", "SP.POP.TOTL", "SL.UEM.TOTL.ZS"}) {
            extractor.extract(indicator, 2015, 2023);
        }
    }
}