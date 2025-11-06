package com.eb.script.interpreter.db;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Oracle JDBC implementation of Interpreter.DbAdapter.
 *
 * Connection spec accepted by connect(spec):
 *   1) String  -> treated as full JDBC URL, e.g. "jdbc:oracle:thin:@//host:1521/service"
 *   2) Map     -> { url?: String, host?: String, port?: int, service?: String, sid?: String,
 *                   user?: String, password?: String, props?: Map<String,Object> }
 *      If url is absent, an EZConnect URL is built from host/port/service (preferred) or SID.
 *
 * Supports:
 *   - executeSelect(sql, named, positional)
 *   - openCursor(sql, named, positional) with named-bind rewrite ":name" -> "?"
 *   - configurable fetchSize (JDBC) and defaultRowPrefetch (Oracle) via props
 *
 * Requires: Oracle JDBC (ojdbc) on classpath.
 */
public final class OracleDbAdapter implements DbAdapter {

    // --- public adapter API ---
    @Override
    public DbConnection connect(Object spec) throws Exception {
        final ConnectConfig cfg = ConnectConfig.from(spec);
        final Properties props = cfg.jdbcProperties();

        // Build Connection
        final Connection conn = DriverManager.getConnection(cfg.url, props);
        // Optional: set session-wide tuning here (autoCommit, etc.)
        return new OracleDbConnection(conn, cfg.defaultRowPrefetch, cfg.defaultFetchSize);
    }

    // ------------------- ConnectConfig (spec parsing) -------------------
    private static final class ConnectConfig {
        final String url;
        final String user;
        final String password;
        final Integer defaultRowPrefetch; // Oracle extension
        final Integer defaultFetchSize;   // JDBC hint

        static ConnectConfig from(Object spec) {
            if (spec instanceof String s) {
                // URL only; credentials are expected in Properties or embedded in the URL if supported
                return new ConnectConfig(s, null, null, null, null);
            } else if (spec instanceof Map<?,?> m) {
                @SuppressWarnings("unchecked")
                Map<String,Object> map = (Map<String,Object>) m;

                String url = asString(map.get("url"));
                String host = asString(map.get("host"));
                Integer port = asInt(map.get("port"));
                String service = asString(map.get("service"));
                String sid = asString(map.get("sid")); // alternative legacy format

                // Build URL if absent (prefer service syntax: jdbc:oracle:thin:@//host:port/service)
                if (url == null) {
                    if (host != null && (service != null || sid != null)) {
                        int p = (port == null ? 1521 : port);
                        if (service != null) {
                            url = "jdbc:oracle:thin:@//" + host + ":" + p + "/" + service; // EZConnect (service)
                        } else {
                            url = "jdbc:oracle:thin:@" + host + ":" + p + ":" + sid;       // legacy SID
                        }
                    } else {
                        throw new IllegalArgumentException(
                          "Missing URL or host+service/sid in connect() spec");
                    }
                }

                String user = asString(map.get("user"));
                String pwd  = asString(map.get("password"));

                // Optional connection properties
                @SuppressWarnings("unchecked")
                Map<String,Object> props = (Map<String,Object>) map.get("props");
                Integer prefetch = null;
                Integer fetchSize = null;
                if (props != null) {
                    prefetch  = asInt(props.get("defaultRowPrefetch")); // Oracle driver property
                    fetchSize = asInt(props.get("fetchSize"));          // JDBC Statement/PreparedStatement hint
                }

                return new ConnectConfig(url, user, pwd, prefetch, fetchSize);
            }
            throw new IllegalArgumentException("Unsupported connect() spec type: " + spec);
        }

        Properties jdbcProperties() {
            final Properties p = new Properties();
            if (user != null)     p.setProperty("user", user);
            if (password != null) p.setProperty("password", password);
            // Oracle prefetch: defaultRowPrefetch = number of rows per roundtrip (default 10)
            // (You may also put this into the URL as a query parameter.)
            if (defaultRowPrefetch != null) {
                p.setProperty("defaultRowPrefetch", String.valueOf(defaultRowPrefetch));
            }
            return p;
        }

        private ConnectConfig(String url, String user, String password,
                              Integer prefetch, Integer fetchSize) {
            this.url = url;
            this.user = user;
            this.password = password;
            this.defaultRowPrefetch = prefetch;
            this.defaultFetchSize   = fetchSize;
        }

        private static String asString(Object o) { return (o == null ? null : String.valueOf(o)); }
        private static Integer asInt(Object o) {
            if (o == null) return null;
            if (o instanceof Number n) return n.intValue();
            return Integer.valueOf(String.valueOf(o));
        }
    }

    // ------------------- OracleDbConnection -------------------
    private static final class OracleDbConnection implements DbConnection {
        private final Connection conn;
        private final Integer defaultRowPrefetch; // for OracleStatement#setRowPrefetch
        private final Integer defaultFetchSize;   // for Statement#setFetchSize

        OracleDbConnection(Connection conn, Integer prefetch, Integer fetchSize) {
            this.conn = conn;
            this.defaultRowPrefetch = prefetch;
            this.defaultFetchSize   = fetchSize;
        }

        @Override
        public DbCursor openCursor(String sql,
                                               Map<String,Object> named,
                                               List<Object> positional) throws Exception {
            final Rewritten rw = rewriteNamedBindsIfAny(sql, named);
            final PreparedStatement ps = conn.prepareStatement(rw.sql);
            applyFetchHints(ps, defaultRowPrefetch, defaultFetchSize);
            bindAll(ps, rw.bindOrder, named, positional);
            final ResultSet rs = ps.executeQuery();
            return new OracleDbCursor(ps, rs);
        }

        @Override
        public List<Map<String,Object>> executeSelect(String sql,
                                                      Map<String,Object> named,
                                                      List<Object> positional) throws Exception {
            final Rewritten rw = rewriteNamedBindsIfAny(sql, named);
            try (PreparedStatement ps = conn.prepareStatement(rw.sql)) {
                applyFetchHints(ps, defaultRowPrefetch, defaultFetchSize);
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
        private static void applyFetchHints(Statement st, Integer prefetch, Integer fetchSize) {
            try {
                if (fetchSize != null) st.setFetchSize(fetchSize); // standard JDBC hint
            } catch (SQLException ignored) { /* driver may ignore */ }

            // Oracle-specific prefetch (if driver classes are present)
            if (prefetch != null) {
                try {
                    // Avoid hard dependency: reflectively call OracleStatement#setRowPrefetch
                    Class<?> oracleStmtClass = Class.forName("oracle.jdbc.OracleStatement");
                    if (oracleStmtClass.isInstance(st)) {
                        oracleStmtClass.getMethod("setRowPrefetch", int.class).invoke(st, prefetch);
                    }
                } catch (Throwable ignored) {
                    // ignore if not an OracleStatement or method not available
                }
            }
        }

        private static void bindAll(PreparedStatement ps,
                                    List<String> bindOrder,
                                    Map<String,Object> named,
                                    List<Object> positional) throws SQLException {
            if (!bindOrder.isEmpty()) {
                // Named binds were found and rewritten; bind by discovered order
                for (int i = 0; i < bindOrder.size(); i++) {
                    String name = bindOrder.get(i);
                    Object val = (named == null ? null : named.get(name));
                    ps.setObject(i + 1, val);
                }
            } else {
                // Positional binds (standard ? markers)
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
         * If no named markers are present, returns empty bindOrder and original SQL.
         */
        private static Rewritten rewriteNamedBindsIfAny(String sql, Map<String,Object> named) {
            if (sql == null) return new Rewritten(sql, List.of());
            // Quick scan for ':'
            if (sql.indexOf(':') < 0) return new Rewritten(sql, List.of());

            // Regex for :identifier (skip '::' or ':/')
            Pattern p = Pattern.compile(":(?!:)([A-Za-z_][A-Za-z0-9_]*)");
            Matcher m = p.matcher(sql);
            StringBuffer sb = new StringBuffer(sql.length());
            List<String> order = new ArrayList<>();
            while (m.find()) {
                String name = m.group(1);
                order.add(name.toLowerCase()); // store lower-case to match Parameter normalization
                m.appendReplacement(sb, "?");
            }
            m.appendTail(sb);

            // If the query used :name but caller provided no named values, we can still proceed (nulls)
            return new Rewritten(sb.toString(), order);
        }

        private static final class Rewritten {
            final String sql;
            final List<String> bindOrder; // empty if no named markers discovered
            Rewritten(String sql, List<String> order) { this.sql = sql; this.bindOrder = order; }
        }
    }

    // ------------------- OracleDbCursor -------------------
    private static final class OracleDbCursor implements DbCursor {
        private final PreparedStatement ps;
        private final ResultSet rs;

        private boolean peeked = false;
        private boolean hasRow = false;
        private Map<String,Object> cachedRow = null;

        OracleDbCursor(PreparedStatement ps, ResultSet rs) {
            this.ps = ps;
            this.rs = rs;
        }

        @Override
        public boolean hasNext() throws Exception {
            if (!peeked) {
                hasRow = rs.next();
                if (hasRow) {
                    ResultSetMetaData md = rs.getMetaData();
                    cachedRow = OracleDbConnection.readRow(rs, md, md.getColumnCount());
                }
                peeked = true;
            }
            return hasRow;
        }

        @Override
        public Map<String, Object> next() throws Exception {
            if (!peeked) {
                // direct next without hasNext()
                if (rs.next()) {
                    ResultSetMetaData md = rs.getMetaData();
                    return OracleDbConnection.readRow(rs, md, md.getColumnCount());
                }
                return null;
            }
            if (!hasRow) return null;
            try {
                return cachedRow;
            } finally {
                // reset peek state
                peeked = false; hasRow = false; cachedRow = null;
            }
        }

        @Override
        public void close() throws Exception {
            try { rs.close(); } finally { ps.close(); }
        }
    }
}
