package com.eb.script.interpreter.db;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PostgreSQL JDBC implementation of Interpreter.DbAdapter.
 *
 * Connection spec accepted by connect(spec):
 *   1) String  -> treated as full JDBC URL, e.g. "jdbc:postgresql://host:5432/database"
 *   2) Map     -> { url?: String, host?: String, port?: int, database?: String,
 *                   user?: String, password?: String, props?: Map<String,Object> }
 *      If url is absent, a URL is built from host/port/database.
 *
 * Supports:
 *   - executeSelect(sql, named, positional)
 *   - openCursor(sql, named, positional) with named-bind rewrite ":name" -> "?"
 *   - configurable fetchSize (JDBC) via props
 *
 * Requires: PostgreSQL JDBC driver on classpath.
 */
public final class PostgreSqlDbAdapter implements DbAdapter {

    // --- public adapter API ---
    @Override
    public DbConnection connect(Object spec) throws Exception {
        final ConnectConfig cfg = ConnectConfig.from(spec);
        final Properties props = cfg.jdbcProperties();

        // Build Connection
        final Connection conn = DriverManager.getConnection(cfg.url, props);
        return new PostgreSqlDbConnection(conn, cfg.defaultFetchSize);
    }

    // ------------------- ConnectConfig (spec parsing) -------------------
    private static final class ConnectConfig {
        final String url;
        final String user;
        final String password;
        final Integer defaultFetchSize;   // JDBC hint

        static ConnectConfig from(Object spec) {
            if (spec instanceof String s) {
                // URL only; credentials are expected in Properties or embedded in the URL
                return new ConnectConfig(s, null, null, null);
            } else if (spec instanceof Map<?,?> m) {
                @SuppressWarnings("unchecked")
                Map<String,Object> map = (Map<String,Object>) m;

                String url = asString(map.get("url"));
                String host = asString(map.get("host"));
                Integer port = asInt(map.get("port"));
                String database = asString(map.get("database"));

                // Build URL if absent: jdbc:postgresql://host:port/database
                if (url == null) {
                    if (host != null && database != null) {
                        int p = (port == null ? 5432 : port);
                        url = "jdbc:postgresql://" + host + ":" + p + "/" + database;
                    } else {
                        throw new IllegalArgumentException(
                          "Missing URL or host+database in connect() spec");
                    }
                }

                String user = asString(map.get("user"));
                String pwd  = asString(map.get("password"));

                // Optional connection properties
                @SuppressWarnings("unchecked")
                Map<String,Object> props = (Map<String,Object>) map.get("props");
                Integer fetchSize = null;
                if (props != null) {
                    fetchSize = asInt(props.get("fetchSize"));
                }

                return new ConnectConfig(url, user, pwd, fetchSize);
            }
            throw new IllegalArgumentException("Unsupported connect() spec type: " + spec);
        }

        Properties jdbcProperties() {
            final Properties p = new Properties();
            if (user != null)     p.setProperty("user", user);
            if (password != null) p.setProperty("password", password);
            return p;
        }

        private ConnectConfig(String url, String user, String password, Integer fetchSize) {
            this.url = url;
            this.user = user;
            this.password = password;
            this.defaultFetchSize = fetchSize;
        }

        private static String asString(Object o) { return (o == null ? null : String.valueOf(o)); }
        private static Integer asInt(Object o) {
            if (o == null) return null;
            if (o instanceof Number n) return n.intValue();
            return Integer.valueOf(String.valueOf(o));
        }
    }

    // ------------------- PostgreSqlDbConnection -------------------
    private static final class PostgreSqlDbConnection implements DbConnection {
        private final Connection conn;
        private final Integer defaultFetchSize;

        PostgreSqlDbConnection(Connection conn, Integer fetchSize) {
            this.conn = conn;
            this.defaultFetchSize = fetchSize;
        }

        @Override
        public DbCursor openCursor(String sql,
                                   Map<String,Object> named,
                                   List<Object> positional) throws Exception {
            final Rewritten rw = rewriteNamedBindsIfAny(sql, named);
            final PreparedStatement ps = conn.prepareStatement(rw.sql);
            applyFetchHints(ps, defaultFetchSize);
            bindAll(ps, rw.bindOrder, named, positional);
            final ResultSet rs = ps.executeQuery();
            return new PostgreSqlDbCursor(ps, rs);
        }

        @Override
        public List<Map<String,Object>> executeSelect(String sql,
                                                       Map<String,Object> named,
                                                       List<Object> positional) throws Exception {
            final Rewritten rw = rewriteNamedBindsIfAny(sql, named);
            try (PreparedStatement ps = conn.prepareStatement(rw.sql)) {
                applyFetchHints(ps, defaultFetchSize);
                bindAll(ps, rw.bindOrder, named, positional);
                try (ResultSet rs = ps.executeQuery()) {
                    final List<Map<String,Object>> out = new ArrayList<>();
                    final ResultSetMetaData md = rs.getMetaData();
                    final int cols = md.getColumnCount();
                    while (rs.next()) {
                        out.add(readRow(rs, md, cols));
                    }
                    return out;
                }
            }
        }

        @Override
        public void close() throws Exception {
            conn.close();
        }

        // ---------- helpers ----------
        private static void applyFetchHints(Statement st, Integer fetchSize) {
            try {
                if (fetchSize != null) st.setFetchSize(fetchSize);
            } catch (SQLException ignored) { /* driver may ignore */ }
        }

        private static void bindAll(PreparedStatement ps,
                                    List<String> bindOrder,
                                    Map<String,Object> named,
                                    List<Object> positional) throws SQLException {
            if (!bindOrder.isEmpty()) {
                // Named binds were found and rewritten
                for (int i = 0; i < bindOrder.size(); i++) {
                    String name = bindOrder.get(i);
                    Object val = (named == null ? null : named.get(name));
                    ps.setObject(i + 1, val);
                }
            } else {
                // Positional binds
                if (positional != null) {
                    for (int i = 0; i < positional.size(); i++) {
                        ps.setObject(i + 1, positional.get(i));
                    }
                }
            }
        }

        private static Map<String,Object> readRow(ResultSet rs, ResultSetMetaData md, int cols)
                throws SQLException {
            Map<String,Object> row = new LinkedHashMap<>(cols);
            for (int c = 1; c <= cols; c++) {
                String label = md.getColumnLabel(c);
                if (label == null || label.isBlank()) label = md.getColumnName(c);
                row.put(label, rs.getObject(c));
            }
            return row;
        }

        /**
         * Rewrites any :name bind markers to '?' and returns the bind order.
         */
        private static Rewritten rewriteNamedBindsIfAny(String sql, Map<String,Object> named) {
            if (sql == null) return new Rewritten(sql, List.of());
            if (sql.indexOf(':') < 0) return new Rewritten(sql, List.of());

            Pattern p = Pattern.compile(":(?!:)([A-Za-z_][A-Za-z0-9_]*)");
            Matcher m = p.matcher(sql);
            StringBuffer sb = new StringBuffer(sql.length());
            List<String> order = new ArrayList<>();
            while (m.find()) {
                String name = m.group(1);
                order.add(name.toLowerCase());
                m.appendReplacement(sb, "?");
            }
            m.appendTail(sb);

            return new Rewritten(sb.toString(), order);
        }

        private static final class Rewritten {
            final String sql;
            final List<String> bindOrder;
            Rewritten(String sql, List<String> order) { this.sql = sql; this.bindOrder = order; }
        }
    }

    // ------------------- PostgreSqlDbCursor -------------------
    private static final class PostgreSqlDbCursor implements DbCursor {
        private final PreparedStatement ps;
        private final ResultSet rs;

        private boolean peeked = false;
        private boolean hasRow = false;
        private Map<String,Object> cachedRow = null;

        PostgreSqlDbCursor(PreparedStatement ps, ResultSet rs) {
            this.ps = ps;
            this.rs = rs;
        }

        @Override
        public boolean hasNext() throws Exception {
            if (!peeked) {
                hasRow = rs.next();
                if (hasRow) {
                    ResultSetMetaData md = rs.getMetaData();
                    cachedRow = PostgreSqlDbConnection.readRow(rs, md, md.getColumnCount());
                }
                peeked = true;
            }
            return hasRow;
        }

        @Override
        public Map<String, Object> next() throws Exception {
            if (!peeked) {
                if (rs.next()) {
                    ResultSetMetaData md = rs.getMetaData();
                    return PostgreSqlDbConnection.readRow(rs, md, md.getColumnCount());
                }
                return null;
            }
            if (!hasRow) return null;
            try {
                return cachedRow;
            } finally {
                peeked = false; hasRow = false; cachedRow = null;
            }
        }

        @Override
        public void close() throws Exception {
            rs.close();
            ps.close();
        }
    }
}
