package org.jackl.Data;

import java.util.List;

public class QueryBuilder {
    private final StringBuilder StringBuilder;
    private boolean firstClause;

    private QueryBuilder(String initial) {
        StringBuilder = new StringBuilder(initial);
    }

    public static QueryBuilder select() {
        return new QueryBuilder("SELECT ");
    }

    public static QueryBuilder where() {
        QueryBuilder QueryBuilder = new QueryBuilder(" WHERE ");
        QueryBuilder.firstClause = true;

        return QueryBuilder;
    }

    public static QueryBuilder createTable(String table, List<String> columns, DataTypes[] types) {
        QueryBuilder qb = new QueryBuilder("CREATE TABLE \"" + esc(table) + "\" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) qb.StringBuilder.append(", ");
            qb.StringBuilder.append("\"").append(esc(columns.get(i))).append("\" ").append(sqlType(types[i]));
        }
        qb.StringBuilder.append(")");

        return qb;
    }

    public QueryBuilder col(String col) {
        if (!StringBuilder.toString().endsWith("SELECT ")) StringBuilder.append(", ");
        StringBuilder.append("\"").append(esc(col)).append("\"");

        return this;
    }

    public QueryBuilder cast(String col) {
        if (!StringBuilder.toString().endsWith("SELECT ")) StringBuilder.append(", ");
        StringBuilder.append("CAST(\"").append(esc(col)).append("\" AS REAL)");

        return this;
    }

    public QueryBuilder from(String table) {
        StringBuilder.append(" FROM \"").append(esc(table)).append("\"");

        return this;
    }

    public QueryBuilder where(String clause) {
        StringBuilder.append(clause);

        return this;
    }

    public QueryBuilder notNull(String col) {
        if (!firstClause) StringBuilder.append(" AND ");
        else firstClause = false;
        StringBuilder.append("\"").append(esc(col)).append("\" IS NOT NULL");

        return this;
    }

    public QueryBuilder constrain(String col, String op, double val) {
        if (!firstClause) StringBuilder.append(" AND ");
        else firstClause = false;
        StringBuilder.append("CAST(\"").append(esc(col)).append("\" AS REAL) ").append(op).append(" ").append(val);

        return this;
    }

    public QueryBuilder orderBy(String col) {
        StringBuilder.append(" ORDER BY \"").append(esc(col)).append("\"");



        return this;
    }

    public QueryBuilder orderByRandom() {
        StringBuilder.append(" ORDER BY RANDOM()");
        return this;
    }

    public QueryBuilder desc() {
        StringBuilder.append(" DESC");
        return this;
    }

    public QueryBuilder limit(int n) {
        StringBuilder.append(" LIMIT ").append(n);
        return this;
    }

    public String build() {
        return StringBuilder.toString();
    }

    ///
    public static String esc(String s) {
        return s.replace("\"", "\"\"");
    }

    private static String sqlType(DataTypes type) {
        return switch (type) {
            case INT -> "INTEGER";
            case FLOAT -> "REAL";
            case TEXT -> "TEXT";
        };
    }
}
