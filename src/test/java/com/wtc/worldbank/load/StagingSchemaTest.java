package com.wtc.worldbank.load;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StagingSchemaTest {

    @Test
    void createsRequiredStagingTables() throws SQLException {
        List<String> executedSql = new ArrayList<>();
        Connection conn = connectionWithSqlCapture(executedSql);

        StagingSchema.create(conn);

        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("CREATE TABLE IF NOT EXISTS stg_observations")));
        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("CREATE TABLE IF NOT EXISTS stg_countries")));
        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("CREATE TABLE IF NOT EXISTS rejected_rows")));
    }

    private static Connection connectionWithSqlCapture(List<String> executedSql) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "createStatement":
                            return (Statement) Proxy.newProxyInstance(
                                    Statement.class.getClassLoader(),
                                    new Class<?>[]{Statement.class},
                                    (stmtProxy, stmtMethod, stmtArgs) -> {
                                        switch (stmtMethod.getName()) {
                                            case "execute":
                                                executedSql.add((String) stmtArgs[0]);
                                                return true;
                                            case "close":
                                                return null;
                                            case "isClosed":
                                                return false;
                                            default:
                                                return defaultValue(stmtMethod.getReturnType());
                                        }
                                    }
                            );
                        case "close":
                        case "commit":
                        case "rollback":
                        case "setAutoCommit":
                            return null;
                        case "getAutoCommit":
                            return true;
                        default:
                            return defaultValue(method.getReturnType());
                    }
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }
}
