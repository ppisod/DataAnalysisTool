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

    private static void createTable(Connection conn, CsvFile csv) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS \"" + csv.name + "\"");

            String sql = QueryBuilder.createTable(csv.name, csv.columns, csv.columnTypes).build();
            stmt.execute(sql);
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
                            case INT -> {
                                try {
                                    ps.setLong(i + 1, Long.parseLong(val));
                                } catch (NumberFormatException e) {
                                    try {
                                        ps.setDouble(i + 1, Double.parseDouble(val));
                                    } catch (NumberFormatException e2) {
                                        ps.setNull(i + 1, java.sql.Types.NULL);
                                    }
                                }
                            }
                            case FLOAT -> {
                                try {
                                    ps.setDouble(i + 1, Double.parseDouble(val));
                                } catch (NumberFormatException e) {
                                    ps.setNull(i + 1, java.sql.Types.NULL);
                                }
                            }
                            case TEXT -> ps.setString(i + 1, val);
                        }
                    }
                }
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit(); // instead of commiting with each stmt, this is faster
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
