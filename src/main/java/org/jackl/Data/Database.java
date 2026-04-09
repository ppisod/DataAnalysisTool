package org.jackl.Data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class  Database {

    // stores at working dir
    private static final String DB_PATH = System.getProperty("user.dir") + "/dataAnalysis/data.db";
    private static Connection connection;

    public static Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            boolean ignored = new File(DB_PATH).getParentFile().mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL"); // writes concurrently does not hang
                stmt.execute("PRAGMA foreign_keys=ON"); // allow for fkey
            }
            TableRegistry.initialize(connection);
        }
        return connection;
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
