package com.eb.script.interpreter.builtins;

import com.eb.script.interpreter.InterpreterError;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Built-in functions for Date/Time operations.
 * Uses java.time.LocalDateTime for datetime values and java.time.LocalDate for date-only values.
 * Handles all date.* builtins.
 *
 * @author Earl Bosch
 */
public class BuiltinsDate {

    /**
     * Dispatch a Date builtin by name.
     *
     * @param name Lowercase builtin name (e.g., "date.now")
     * @param args Arguments passed to the builtin
     * @return Result of the builtin call
     * @throws InterpreterError if the call fails
     */
    public static Object dispatch(String name, Object[] args) throws InterpreterError {
        return switch (name) {
            case "date.now" -> now();
            case "date.today" -> today();
            case "date.format" -> format(args);
            case "date.parse" -> parse(args);
            case "date.parsedatetime" -> parseDateTime(args);
            case "date.adddays" -> addDays(args);
            case "date.addhours" -> addHours(args);
            case "date.addminutes" -> addMinutes(args);
            case "date.addseconds" -> addSeconds(args);
            case "date.daysbetween" -> daysBetween(args);
            case "date.getyear" -> getYear(args);
            case "date.getmonth" -> getMonth(args);
            case "date.getday" -> getDay(args);
            case "date.gethour" -> getHour(args);
            case "date.getminute" -> getMinute(args);
            case "date.getsecond" -> getSecond(args);
            case "date.toepochms" -> toEpochMs(args);
            case "date.fromepochms" -> fromEpochMs(args);
            case "date.tosqltimestamp" -> toSqlTimestamp(args);
            default -> throw new InterpreterError("Unknown Date builtin: " + name);
        };
    }

    /**
     * Checks if the given builtin name is a Date builtin.
     */
    public static boolean handles(String name) {
        return name.startsWith("date.");
    }

    // --- Individual builtin implementations ---

    /**
     * date.now() - Returns current instant (date + time)
     */
    private static Object now() {
        return LocalDateTime.now();
    }

    /**
     * date.today() - Returns current date (no time component)
     */
    private static Object today() {
        return LocalDate.now();
    }

    /**
     * date.format(dateValue, pattern) - Format date for display
     */
    private static Object format(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("date.format requires 2 arguments: dateValue, pattern");
        }
        Object dateValue = args[0];
        String pattern = requireString(args[1], "date.format", "pattern");
        
        if (dateValue == null) {
            return null;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            
            if (dateValue instanceof LocalDateTime ldt) {
                return ldt.format(formatter);
            } else if (dateValue instanceof LocalDate ld) {
                return ld.format(formatter);
            } else if (dateValue instanceof Instant instant) {
                LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                return ldt.format(formatter);
            } else if (dateValue instanceof java.util.Date date) {
                Instant instant = date.toInstant();
                LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                return ldt.format(formatter);
            } else {
                throw new InterpreterError("date.format: first argument must be a date value, got: " + dateValue.getClass().getSimpleName());
            }
        } catch (IllegalArgumentException e) {
            throw new InterpreterError("date.format: invalid pattern '" + pattern + "': " + e.getMessage());
        }
    }

    /**
     * date.parse(dateString, pattern) - Parse date string to LocalDate
     */
    private static Object parse(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("date.parse requires 2 arguments: dateString, pattern");
        }
        String dateString = requireString(args[0], "date.parse", "dateString");
        String pattern = requireString(args[1], "date.parse", "pattern");
        
        if (dateString == null) {
            return null;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDate.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            throw new InterpreterError("date.parse: cannot parse '" + dateString + "' with pattern '" + pattern + "': " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new InterpreterError("date.parse: invalid pattern '" + pattern + "': " + e.getMessage());
        }
    }

    /**
     * date.parseDateTime(dateTimeString, pattern) - Parse datetime string to LocalDateTime
     */
    private static Object parseDateTime(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("date.parseDateTime requires 2 arguments: dateTimeString, pattern");
        }
        String dateTimeString = requireString(args[0], "date.parseDateTime", "dateTimeString");
        String pattern = requireString(args[1], "date.parseDateTime", "pattern");
        
        if (dateTimeString == null) {
            return null;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            return LocalDateTime.parse(dateTimeString, formatter);
        } catch (DateTimeParseException e) {
            throw new InterpreterError("date.parseDateTime: cannot parse '" + dateTimeString + "' with pattern '" + pattern + "': " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new InterpreterError("date.parseDateTime: invalid pattern '" + pattern + "': " + e.getMessage());
        }
    }

    /**
     * date.addDays(dateValue, days) - Add days to date
     */
    private static Object addDays(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("date.addDays requires 2 arguments: dateValue, days");
        }
        Object dateValue = args[0];
        int days = requireInt(args[1], "date.addDays", "days");
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.plusDays(days);
        } else if (dateValue instanceof LocalDate ld) {
            return ld.plusDays(days);
        } else if (dateValue instanceof Instant instant) {
            return instant.plus(days, ChronoUnit.DAYS);
        } else if (dateValue instanceof java.util.Date date) {
            Instant instant = date.toInstant();
            return LocalDateTime.ofInstant(instant.plus(days, ChronoUnit.DAYS), ZoneId.systemDefault());
        } else {
            throw new InterpreterError("date.addDays: first argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.addHours(dateValue, hours) - Add hours to date
     */
    private static Object addHours(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("date.addHours requires 2 arguments: dateValue, hours");
        }
        Object dateValue = args[0];
        int hours = requireInt(args[1], "date.addHours", "hours");
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.plusHours(hours);
        } else if (dateValue instanceof LocalDate ld) {
            return ld.atStartOfDay().plusHours(hours);
        } else if (dateValue instanceof Instant instant) {
            return instant.plus(hours, ChronoUnit.HOURS);
        } else if (dateValue instanceof java.util.Date date) {
            Instant instant = date.toInstant();
            return LocalDateTime.ofInstant(instant.plus(hours, ChronoUnit.HOURS), ZoneId.systemDefault());
        } else {
            throw new InterpreterError("date.addHours: first argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.addMinutes(dateValue, minutes) - Add minutes to date
     */
    private static Object addMinutes(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("date.addMinutes requires 2 arguments: dateValue, minutes");
        }
        Object dateValue = args[0];
        int minutes = requireInt(args[1], "date.addMinutes", "minutes");
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.plusMinutes(minutes);
        } else if (dateValue instanceof LocalDate ld) {
            return ld.atStartOfDay().plusMinutes(minutes);
        } else if (dateValue instanceof Instant instant) {
            return instant.plus(minutes, ChronoUnit.MINUTES);
        } else if (dateValue instanceof java.util.Date date) {
            Instant instant = date.toInstant();
            return LocalDateTime.ofInstant(instant.plus(minutes, ChronoUnit.MINUTES), ZoneId.systemDefault());
        } else {
            throw new InterpreterError("date.addMinutes: first argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.addSeconds(dateValue, seconds) - Add seconds to date
     */
    private static Object addSeconds(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("date.addSeconds requires 2 arguments: dateValue, seconds");
        }
        Object dateValue = args[0];
        int seconds = requireInt(args[1], "date.addSeconds", "seconds");
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.plusSeconds(seconds);
        } else if (dateValue instanceof LocalDate ld) {
            return ld.atStartOfDay().plusSeconds(seconds);
        } else if (dateValue instanceof Instant instant) {
            return instant.plus(seconds, ChronoUnit.SECONDS);
        } else if (dateValue instanceof java.util.Date date) {
            Instant instant = date.toInstant();
            return LocalDateTime.ofInstant(instant.plus(seconds, ChronoUnit.SECONDS), ZoneId.systemDefault());
        } else {
            throw new InterpreterError("date.addSeconds: first argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.daysBetween(date1, date2) - Calculate days between two dates
     */
    private static Object daysBetween(Object[] args) throws InterpreterError {
        if (args.length < 2) {
            throw new InterpreterError("date.daysBetween requires 2 arguments: date1, date2");
        }
        Object date1 = args[0];
        Object date2 = args[1];
        
        if (date1 == null || date2 == null) {
            return null;
        }
        
        LocalDate ld1 = toLocalDate(date1, "date.daysBetween first argument");
        LocalDate ld2 = toLocalDate(date2, "date.daysBetween second argument");
        
        return ChronoUnit.DAYS.between(ld1, ld2);
    }

    /**
     * date.getYear(dateValue) - Get year component
     */
    private static Object getYear(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("date.getYear requires 1 argument: dateValue");
        }
        Object dateValue = args[0];
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.getYear();
        } else if (dateValue instanceof LocalDate ld) {
            return ld.getYear();
        } else if (dateValue instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).getYear();
        } else if (dateValue instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).getYear();
        } else {
            throw new InterpreterError("date.getYear: argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.getMonth(dateValue) - Get month component (1-12)
     */
    private static Object getMonth(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("date.getMonth requires 1 argument: dateValue");
        }
        Object dateValue = args[0];
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.getMonthValue();
        } else if (dateValue instanceof LocalDate ld) {
            return ld.getMonthValue();
        } else if (dateValue instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).getMonthValue();
        } else if (dateValue instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).getMonthValue();
        } else {
            throw new InterpreterError("date.getMonth: argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.getDay(dateValue) - Get day of month component (1-31)
     */
    private static Object getDay(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("date.getDay requires 1 argument: dateValue");
        }
        Object dateValue = args[0];
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.getDayOfMonth();
        } else if (dateValue instanceof LocalDate ld) {
            return ld.getDayOfMonth();
        } else if (dateValue instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).getDayOfMonth();
        } else if (dateValue instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).getDayOfMonth();
        } else {
            throw new InterpreterError("date.getDay: argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.getHour(dateValue) - Get hour component (0-23)
     */
    private static Object getHour(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("date.getHour requires 1 argument: dateValue");
        }
        Object dateValue = args[0];
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.getHour();
        } else if (dateValue instanceof LocalDate ld) {
            return 0; // LocalDate has no time component
        } else if (dateValue instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).getHour();
        } else if (dateValue instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).getHour();
        } else {
            throw new InterpreterError("date.getHour: argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.getMinute(dateValue) - Get minute component (0-59)
     */
    private static Object getMinute(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("date.getMinute requires 1 argument: dateValue");
        }
        Object dateValue = args[0];
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.getMinute();
        } else if (dateValue instanceof LocalDate ld) {
            return 0; // LocalDate has no time component
        } else if (dateValue instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).getMinute();
        } else if (dateValue instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).getMinute();
        } else {
            throw new InterpreterError("date.getMinute: argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.getSecond(dateValue) - Get second component (0-59)
     */
    private static Object getSecond(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("date.getSecond requires 1 argument: dateValue");
        }
        Object dateValue = args[0];
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.getSecond();
        } else if (dateValue instanceof LocalDate ld) {
            return 0; // LocalDate has no time component
        } else if (dateValue instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).getSecond();
        } else if (dateValue instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).getSecond();
        } else {
            throw new InterpreterError("date.getSecond: argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.toEpochMs(dateValue) - Convert to epoch milliseconds
     */
    private static Object toEpochMs(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("date.toEpochMs requires 1 argument: dateValue");
        }
        Object dateValue = args[0];
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else if (dateValue instanceof LocalDate ld) {
            return ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } else if (dateValue instanceof Instant instant) {
            return instant.toEpochMilli();
        } else if (dateValue instanceof java.util.Date date) {
            return date.getTime();
        } else {
            throw new InterpreterError("date.toEpochMs: argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    /**
     * date.fromEpochMs(epochMs) - Create date from epoch milliseconds
     */
    private static Object fromEpochMs(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("date.fromEpochMs requires 1 argument: epochMs");
        }
        
        if (args[0] == null) {
            return null;
        }
        
        long epochMs = requireLong(args[0], "date.fromEpochMs", "epochMs");
        Instant instant = Instant.ofEpochMilli(epochMs);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    /**
     * date.toSqlTimestamp(dateValue) - Convert to SQL Timestamp
     */
    private static Object toSqlTimestamp(Object[] args) throws InterpreterError {
        if (args.length < 1) {
            throw new InterpreterError("date.toSqlTimestamp requires 1 argument: dateValue");
        }
        Object dateValue = args[0];
        
        if (dateValue == null) {
            return null;
        }
        
        if (dateValue instanceof LocalDateTime ldt) {
            return Timestamp.valueOf(ldt);
        } else if (dateValue instanceof LocalDate ld) {
            return Timestamp.valueOf(ld.atStartOfDay());
        } else if (dateValue instanceof Instant instant) {
            return Timestamp.from(instant);
        } else if (dateValue instanceof java.util.Date date) {
            return new Timestamp(date.getTime());
        } else {
            throw new InterpreterError("date.toSqlTimestamp: argument must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }

    // --- Helper methods ---

    /**
     * Extract a string argument with type validation.
     */
    private static String requireString(Object arg, String functionName, String paramName) throws InterpreterError {
        if (arg == null) {
            return null;
        }
        if (!(arg instanceof String)) {
            throw new InterpreterError(functionName + ": " + paramName + " must be a string, got: " + arg.getClass().getSimpleName());
        }
        return (String) arg;
    }

    /**
     * Extract an integer argument with type validation.
     */
    private static int requireInt(Object arg, String functionName, String paramName) throws InterpreterError {
        if (arg == null) {
            throw new InterpreterError(functionName + ": " + paramName + " cannot be null");
        }
        if (!(arg instanceof Number)) {
            throw new InterpreterError(functionName + ": " + paramName + " must be a number, got: " + arg.getClass().getSimpleName());
        }
        return ((Number) arg).intValue();
    }

    /**
     * Extract a long argument with type validation.
     */
    private static long requireLong(Object arg, String functionName, String paramName) throws InterpreterError {
        if (arg == null) {
            throw new InterpreterError(functionName + ": " + paramName + " cannot be null");
        }
        if (!(arg instanceof Number)) {
            throw new InterpreterError(functionName + ": " + paramName + " must be a number, got: " + arg.getClass().getSimpleName());
        }
        return ((Number) arg).longValue();
    }

    /**
     * Convert various date types to LocalDate
     */
    private static LocalDate toLocalDate(Object dateValue, String context) throws InterpreterError {
        if (dateValue instanceof LocalDate ld) {
            return ld;
        } else if (dateValue instanceof LocalDateTime ldt) {
            return ldt.toLocalDate();
        } else if (dateValue instanceof Instant instant) {
            return LocalDate.ofInstant(instant, ZoneId.systemDefault());
        } else if (dateValue instanceof java.util.Date date) {
            return LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
        } else {
            throw new InterpreterError(context + ": must be a date value, got: " + dateValue.getClass().getSimpleName());
        }
    }
}
