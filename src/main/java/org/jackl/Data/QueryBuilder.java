package org.jackl.Data;

import java.util.List;

public class QueryBuilder {
    private final StringBuilder sb;
    private boolean firstClause;

    private QueryBuilder(String initial) {
        sb = new StringBuilder(initial);
    }

    public static QueryBuilder select() {
        return new QueryBuilder("SELECT ");
    }

    public static QueryBuilder where() {
        QueryBuilder qb = new QueryBuilder(" WHERE ");
        qb.firstClause = true;
        return qb;
    }

    public static QueryBuilder createTable(String table, List<String> columns, DataTypes[] types) {
        QueryBuilder qb = new QueryBuilder("CREATE TABLE \"" + esc(table) + "\" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) qb.sb.append(", ");
            qb.sb.append("\"").append(esc(columns.get(i))).append("\" ").append(sqlType(types[i]));
        }
        qb.sb.append(")");
        return qb;
    }

    public QueryBuilder col(String col) {
        if (!sb.toString().endsWith("SELECT ")) sb.append(", ");
        sb.append("\"").append(esc(col)).append("\"");
        return this;
    }

    public QueryBuilder cast(String col) {
        if (!sb.toString().endsWith("SELECT ")) sb.append(", ");
        sb.append("CAST(\"").append(esc(col)).append("\" AS REAL)");
        return this;
    }

    public QueryBuilder from(String table) {
        sb.append(" FROM \"").append(esc(table)).append("\"");
        return this;
    }

    public QueryBuilder where(String clause) {
        sb.append(clause);
        return this;
    }

    public QueryBuilder notNull(String col) {
        if (!firstClause) sb.append(" AND ");
        else firstClause = false;
        sb.append("\"").append(esc(col)).append("\" IS NOT NULL");
        return this;
    }

    public QueryBuilder constrain(String col, String op, double val) {
        if (!firstClause) sb.append(" AND ");
        else firstClause = false;
        sb.append("CAST(\"").append(esc(col)).append("\" AS REAL) ").append(op).append(" ").append(val);
        return this;
    }

    public QueryBuilder orderBy(String col) {
        sb.append(" ORDER BY \"").append(esc(col)).append("\"");
        return this;
    }

    public QueryBuilder orderByRandom() {
        sb.append(" ORDER BY RANDOM()");
        return this;
    }

    public QueryBuilder desc() {
        sb.append(" DESC");
        return this;
    }

    public QueryBuilder limit(int n) {
        sb.append(" LIMIT ").append(n);
        return this;
    }

    public String build() {
        return sb.toString();
    }

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
