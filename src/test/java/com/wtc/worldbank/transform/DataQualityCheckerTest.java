package com.wtc.worldbank.transform;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataQualityCheckerTest {

    private final DataQualityChecker checker = new DataQualityChecker();

    @Test
    void passesWhenAllChecksAreClean() throws SQLException {
        List<String> executedSql = new ArrayList<>();
        Connection conn = connectionForValues(executedSql, 0, 0, 0, 0, 0);

        assertDoesNotThrow(() -> checker.runAll(conn));
        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("observations")));
    }

    @Test
    void throwsWhenMandatoryCheckFails() {
        List<String> executedSql = new ArrayList<>();
        Connection conn = connectionForValues(executedSql, 1, 0, 0, 0, 0);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> checker.runAll(conn));
        assertTrue(ex.getMessage().contains("Data quality checks failed"));
        assertTrue(executedSql.stream().anyMatch(sql -> sql.contains("SELECT CASE WHEN count(*) = 0 THEN 1 ELSE 0 END FROM observations")));
    }

    private static Connection connectionForValues(List<String> executedSql, int... values) {
        AtomicInteger valueIndex = new AtomicInteger();

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
                                            case "executeQuery":
                                                executedSql.add((String) stmtArgs[0]);
                                                return (ResultSet) Proxy.newProxyInstance(
                                                        ResultSet.class.getClassLoader(),
                                                        new Class<?>[]{ResultSet.class},
                                                        (rsProxy, rsMethod, rsArgs) -> {
                                                            switch (rsMethod.getName()) {
                                                                case "next":
                                                                    return true;
                                                                case "getInt":
                                                                    int index = valueIndex.getAndIncrement();
                                                                    return values[index];
                                                                case "close":
                                                                case "wasNull":
                                                                    return defaultValue(rsMethod.getReturnType());
                                                                default:
                                                                    return defaultValue(rsMethod.getReturnType());
                                                            }
                                                        }
                                                );
                                            case "close":
                                                return null;
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
