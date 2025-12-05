# Java Date Type Recommendation for EBS

This document provides guidance on which Java date type to use in the EBS interpreter for handling dates across different use cases: display formatting, calculations, and SQL operations.

## Recommendation Summary

**Use `java.time.Instant` as the primary internal representation**, with conversions to other types as needed:

| Use Case | Recommended Type | Reason |
|----------|-----------------|--------|
| Internal storage | `java.time.Instant` | Timezone-neutral, precise millisecond storage |
| Display formatting | `java.time.LocalDateTime` or `java.time.ZonedDateTime` | Flexible formatting with `DateTimeFormatter` |
| Date-only values | `java.time.LocalDate` | Simple date without time component |
| Calculations | `java.time.Instant` or `java.time.Duration` | Easy arithmetic with milliseconds |
| JDBC/SQL | `java.sql.Timestamp` | JDBC-compatible, converts easily from Instant |

## Detailed Analysis

### Why `java.time.Instant`?

`Instant` represents a point on the timeline with nanosecond precision (stored internally as seconds and nanoseconds since the Unix epoch 1970-01-01T00:00:00Z). It's the ideal choice because:

1. **Timezone-neutral**: Stores time as UTC, avoiding timezone ambiguity
2. **Precise**: Nanosecond precision for calculations (millisecond precision for most practical uses)
3. **Easy conversions**: Simple conversion to all other date/time types
4. **SQL compatible**: Direct conversion to `java.sql.Timestamp`
5. **Calculation-friendly**: Add/subtract using `Duration` or convert to epoch milliseconds

### Conversion Examples

#### Creating an Instant

```java
// Current time
Instant now = Instant.now();

// From epoch milliseconds (useful for calculations)
Instant instant = Instant.ofEpochMilli(1699876543210L);

// From date string
Instant parsed = Instant.parse("2024-01-15T10:30:00Z");
```

#### Display Formatting

```java
import java.time.*;
import java.time.format.DateTimeFormatter;

Instant instant = Instant.now();

// Convert to LocalDateTime for formatting (uses system timezone)
LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

// Format for display
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formatted = ldt.format(formatter);
// Output: "2024-01-15 10:30:45"

// Different format patterns
DateTimeFormatter usFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
DateTimeFormatter euFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
DateTimeFormatter isoDate = DateTimeFormatter.ISO_LOCAL_DATE;
```

#### Calculations

```java
import java.time.*;

Instant now = Instant.now();

// Add days
Instant futureDate = now.plus(Duration.ofDays(7));

// Subtract hours
Instant pastDate = now.minus(Duration.ofHours(24));

// Calculate difference
Duration between = Duration.between(pastDate, futureDate);
long daysBetween = between.toDays();
```

#### SQL/JDBC Operations

```java
import java.sql.Timestamp;
import java.time.Instant;

// Convert Instant to SQL Timestamp for database operations
Instant instant = Instant.now();
Timestamp sqlTimestamp = Timestamp.from(instant);

// Convert SQL Timestamp back to Instant
Instant fromDb = sqlTimestamp.toInstant();
```

### Current EBS Implementation

The EBS interpreter currently uses:
- `java.time.LocalDateTime` for date+time values
- `java.time.LocalDate` for date-only values
- `java.util.Date` for legacy compatibility

These are stored in the `DataType.DATE` category and handled in:
- `Interpreter.java` - Type detection and parsing
- `ControlUpdater.java` - DatePicker UI updates
- `JsonSchema.java` - Date validation

### Recommended EBS Date Builtin Functions

To fully support the recommended approach, consider implementing these builtin functions:

```javascript
// Date/Time Creation
var now = call date.now();           // Returns current Instant
var today = call date.today();       // Returns current date (LocalDate)
var ts = call date.timestamp(ms);    // Creates Instant from epoch milliseconds

// Formatting
var str = call date.format(dateValue, "yyyy-MM-dd");
var str = call date.formatTime(dateValue, "HH:mm:ss");

// Parsing
var date = call date.parse("2024-01-15", "yyyy-MM-dd");
var datetime = call date.parseDateTime("2024-01-15 10:30:00", "yyyy-MM-dd HH:mm:ss");

// Calculations
var future = call date.addDays(dateValue, 7);
var past = call date.subtractHours(dateValue, 24);
var diff = call date.daysBetween(date1, date2);

// Conversions
var epoch = call date.toEpochMs(dateValue);     // To milliseconds
var sqlTs = call date.toSqlTimestamp(dateValue); // For SQL operations
```

### SQL Integration Example

For database cursors in EBS:

```javascript
// Using date values with SQL
var startDate = call date.parse("2024-01-01", "yyyy-MM-dd");
var endDate = call date.now();

cursor orders = select * from orders 
    where order_date >= :startDate 
    and order_date <= :endDate;

open orders(startDate = startDate, endDate = endDate);

while call orders.hasNext() {
    var row = call orders.next();
    var orderDate = row.order_date;  // Returns as Instant
    print call date.format(orderDate, "MM/dd/yyyy");
}
close orders;
```

## Implementation Notes

### Type Detection in Interpreter

When parsing string literals that look like dates:

```java
// In visitLiteralExpression
if (str.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z?")) {
    return Instant.parse(str);
} else if (str.matches("\\d{4}-\\d{2}-\\d{2}")) {
    return LocalDate.parse(str);
}
```

### Screen DatePicker Support

For UI date pickers, convert between `Instant` and `LocalDate`:

```java
// Setting DatePicker value
if (value instanceof Instant) {
    LocalDate localDate = LocalDate.ofInstant((Instant) value, ZoneId.systemDefault());
    datePicker.setValue(localDate);
}

// Getting DatePicker value
LocalDate selected = datePicker.getValue();
Instant instant = selected.atStartOfDay(ZoneId.systemDefault()).toInstant();
```

## Summary

Using `java.time.Instant` as the primary date type provides:

1. ✅ **Display flexibility**: Easy conversion to any format
2. ✅ **Calculation support**: Simple arithmetic operations
3. ✅ **SQL compatibility**: Direct conversion to `java.sql.Timestamp`
4. ✅ **Timezone safety**: UTC-based storage
5. ✅ **Modern API**: Part of Java 8+ `java.time` package

This approach allows EBS scripts to work with dates consistently across display, calculation, and database operations.

## References

- [Java Time API Documentation](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html)
- [DateTimeFormatter Patterns](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html)
- [JDBC and java.time](https://docs.oracle.com/en/java/javase/21/docs/api/java.sql/java/sql/Timestamp.html)
