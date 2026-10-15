package com.wtc.worldbank;

import com.wtc.worldbank.config.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version()")) {
            // TODO: call rs.next(), then print rs.getString(1)
        } catch (SQLException e) {
            // TODO: print a readable error using e.getMessage()
        }
    }
}