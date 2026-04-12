package org.jackl.Data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TableRegistry {

    static void initialize(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS _table_registry (
                    table_name TEXT PRIMARY KEY,
                    source_path TEXT NOT NULL,
                    imported_at TEXT DEFAULT (datetime('now'))
                )
            """);
        }
    }

    public static void register(String tableName, String sourcePath) throws Exception {
        Connection conn = Database.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO _table_registry (table_name, source_path) VALUES (?, ?)")) {
            ps.setString(1, tableName);
            ps.setString(2, sourcePath);
            ps.executeUpdate();
        }
    }

    public static void unregister(String tableName) throws Exception {
        Connection conn = Database.getConnection();
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS \"" + tableName.replace("\"", "\"\"") + "\"");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM _table_registry WHERE table_name = ?")) {
            ps.setString(1, tableName);
            ps.executeUpdate();
        }
    }

    public static List<TableInfo> getAll() throws Exception {
        List<TableInfo> tables = new ArrayList<>();
        Connection Connection = Database.getConnection();
        try (Statement State = Connection.createStatement();
             ResultSet ResultSetPerTable = State.executeQuery(
                     "SELECT table_name, source_path, imported_at FROM _table_registry ORDER BY imported_at DESC")) {
            while (ResultSetPerTable.next()) {
                tables.add(new TableInfo(
                        ResultSetPerTable.getString("table_name"),
                        ResultSetPerTable.getString("source_path"),
                        ResultSetPerTable.getString("imported_at")
                ));
            }
        }
        return tables;
    }

    public record TableInfo(String tableName, String sourcePath, String importedAt) {}
}
