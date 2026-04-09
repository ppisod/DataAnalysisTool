package org.jackl.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvFile {

    public final String name;
    public final List<String> columns;
    public final DataTypes[] columnTypes;
    public final List<String[]> rows;

    private CsvFile(String name, List<String> columns, DataTypes[] columnTypes, List<String[]> rows) {
        this.name = name;
        this.columns = columns;
        this.columnTypes = columnTypes;
        this.rows = rows;
    }

    public static CsvFile Load(File file, int entriesToCheckForType) throws IOException {
        List<String[]> allRows = new ArrayList<>();
        String[] header;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty?");
            }

            header = parseRow(headerLine);

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    allRows.add(parseRow(line));
                }
            }
        }

        int checkCount = entriesToCheckForType <= 0 ? allRows.size() : Math.min(entriesToCheckForType, allRows.size());
        DataTypes[] types = inferTypes(header.length, allRows.subList(0, checkCount));

        String name = sanitizeName(file.getName());
        return new CsvFile(name, List.of(header), types, allRows);
    }

    private static DataTypes[] inferTypes(int colCount, List<String[]> sample) {
        DataTypes[] types = new DataTypes[colCount];
        Arrays.fill(types, DataTypes.INT);

        for (String[] row : sample) {
            for (int i = 0; i < colCount; i++) {
                if (types[i] == DataTypes.TEXT) continue;

                String val = i < row.length ? row[i].trim() : "";
                if (val.isEmpty()) continue;

                if (types[i] == DataTypes.INT) {
                    try {
                        Long.parseLong(val);
                        continue;
                    } catch (NumberFormatException ignored) {
                    }
                    types[i] = DataTypes.FLOAT;
                }

                if (types[i] == DataTypes.FLOAT) {
                    try {
                        Double.parseDouble(val);
                        continue;
                    } catch (NumberFormatException ignored) {
                    }
                    types[i] = DataTypes.TEXT;
                }
            }
        }

        return types;
    }

    /// "abc,def,123" -> ["abc", "def", "123"]
    private static String[] parseRow(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString().trim());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    private static String sanitizeName(String filename) {
        String name = filename.replaceAll("\\.csv$", "");
        name = name.replaceAll("[^a-zA-Z0-9_]", "_");
        if (name.isEmpty() || Character.isDigit(name.charAt(0))) {
            name = "t_" + name;
        }
        return name.toLowerCase();
    }
}
