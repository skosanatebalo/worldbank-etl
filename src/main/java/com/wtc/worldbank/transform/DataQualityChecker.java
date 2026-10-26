package com.wtc.worldbank.transform;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DataQualityChecker {

    private static final Logger LOG = Logger.getLogger(DataQualityChecker.class.getName());

    private record Check(String name, String sql, boolean mustPass) {}

    private static final List<Check> CHECKS = List.of(
            new Check("observations table is not empty",
                    "SELECT CASE WHEN count(*) = 0 THEN 1 ELSE 0 END FROM observations",
                    true),
            new Check("no negative population or GDP",
                    "SELECT count(*) FROM observations "
                            + "WHERE indicator_id IN ('SP.POP.TOTL', 'NY.GDP.MKTP.CD') AND value < 0",
                    true),
            new Check("unemployment rate between 0 and 100",
                    "SELECT count(*) FROM observations "
                            + "WHERE indicator_id = 'SL.UEM.TOTL.ZS' AND (value < 0 OR value > 100)",
                    true),
            new Check("years within 2015-2023",
                    "SELECT count(*) FROM observations WHERE year < 2015 OR year > 2023",
                    true),
            new Check("every country has at least one observation",
                    "SELECT count(*) FROM countries c WHERE NOT EXISTS "
                            + "(SELECT 1 FROM observations o WHERE o.country_iso3 = c.iso3)",
                    false));

    /** Runs all checks; throws if any must-pass check finds violations. */
    public void runAll(Connection conn) throws SQLException {
        List<String> failures = new ArrayList<>();

        try (Statement stmt = conn.createStatement()) {
            for (Check check : CHECKS) {
                try (ResultSet rs = stmt.executeQuery(check.sql())) {
                    rs.next();
                    int violations = rs.getInt(1);

                    if (violations == 0) {
                        LOG.info("PASS: " + check.name());
                    } else if (check.mustPass()) {
                        LOG.severe("FAIL: " + check.name() + " (" + violations + " violations)");
                        failures.add(check.name());
                    } else {
                        LOG.warning("WARN: " + check.name() + " (" + violations + " violations)");
                    }
                }
            }
        }

        if (!failures.isEmpty()) {
            throw new IllegalStateException("Data quality checks failed: " + failures);
        }
    }
}
