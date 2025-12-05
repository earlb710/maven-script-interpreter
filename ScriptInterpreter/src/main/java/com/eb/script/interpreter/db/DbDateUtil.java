package com.eb.script.interpreter.db;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Utility class for converting between java.time types and JDBC types.
 * Used by all database adapters for consistent date/time handling.
 *
 * @author Earl Bosch
 */
public final class DbDateUtil {

    private DbDateUtil() {
        // Utility class - no instantiation
    }

    /**
     * Convert java.time types to JDBC-compatible types.
     * LocalDateTime -> Timestamp, LocalDate -> java.sql.Date, Instant -> Timestamp
     *
     * @param val The value to convert
     * @return JDBC-compatible value, or the original value if no conversion needed
     */
    public static Object convertForJdbc(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime ldt) {
            return Timestamp.valueOf(ldt);
        } else if (val instanceof LocalDate ld) {
            return java.sql.Date.valueOf(ld);
        } else if (val instanceof Instant instant) {
            return Timestamp.from(instant);
        }
        return val;
    }

    /**
     * Convert JDBC date types to java.time types.
     * Timestamp -> LocalDateTime, java.sql.Date -> LocalDate, java.sql.Time -> LocalTime
     *
     * @param val The JDBC value to convert
     * @return java.time value, or the original value if no conversion needed
     */
    public static Object convertFromJdbc(Object val) {
        if (val == null) return null;
        if (val instanceof Timestamp ts) {
            return ts.toLocalDateTime();
        } else if (val instanceof java.sql.Date d) {
            return d.toLocalDate();
        } else if (val instanceof java.sql.Time t) {
            return t.toLocalTime();
        }
        return val;
    }
}
