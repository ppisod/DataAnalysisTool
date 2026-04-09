package org.jackl.Data;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

public class CsvLoader {

    public static String load(File csvFile) throws Exception {
        CsvFile csv = CsvFile.Load(csvFile, 100);
        Connection conn = Database.getConnection();

        createTable(conn, csv);
        insertRows(conn, csv);

        TableRegistry.register(csv.name, csvFile.getAbsolutePath());
        return csv.name;
    }

    private static String sqlType(DataTypes type) {
        return switch (type) {
            case INT -> "INTEGER";
            case FLOAT -> "REAL";
            case TEXT -> "TEXT";
        };
    }

    private static void createTable(Connection conn, CsvFile csv) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS \"" + csv.name + "\""); // replaced

            StringBuilder sql = new StringBuilder("CREATE TABLE \"" + csv.name + "\" (");
            for (int i = 0; i < csv.columns.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append("\"").append(csv.columns.get(i).replace("\"", "\"\"")).append("\" ").append(sqlType(csv.columnTypes[i]));
            }
            sql.append(")");
            stmt.execute(sql.toString());
        }
    }

    private static void insertRows(Connection conn, CsvFile csv) throws SQLException {
        int colCount = csv.columns.size();
        String placeholders = String.join(", ", Arrays.stream(new int[colCount])
                .mapToObj(i -> "?").toArray(String[]::new));
        String sql = "INSERT INTO \"" + csv.name + "\" VALUES (" + placeholders + ")";

        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] row : csv.rows) {
                for (int i = 0; i < colCount; i++) {
                    String val = i < row.length ? row[i].trim() : null;
                    if (val == null || val.isEmpty()) {
                        ps.setNull(i + 1, java.sql.Types.NULL);
                    } else {
                        switch (csv.columnTypes[i]) {
                            case INT -> ps.setLong(i + 1, Long.parseLong(val));
                            case FLOAT -> ps.setDouble(i + 1, Double.parseDouble(val));
                            case TEXT -> ps.setString(i + 1, val);
                        }
                    }
                }
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
