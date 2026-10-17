package com.wtc.worldbank;

import com.wtc.worldbank.config.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import com.wtc.worldbank.extract.WorldBankClient;
import java.nio.file.Files;
import java.nio.file.Path;
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
        String json = client.get("https://api.worldbank.org/v2/country?format=json&per_page=400");
        Files.createDirectories(Path.of("raw"));
        Files.writeString(Path.of("raw/countries.json"), json);
        System.out.println("Saved " + json.length() + " characters to raw/countries.json");
    }
}