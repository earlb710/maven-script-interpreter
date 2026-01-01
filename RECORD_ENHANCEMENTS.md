# Record Type Enhancements

This document describes the enhanced record type system with field-level constraints and validation.

## Overview

The record type system has been enhanced to support field-level metadata including:
- **Default values** - Automatically applied when fields are not provided
- **Mandatory fields** - Enforced as required (not null)
- **Maximum length constraints** - Validated for string fields

## Syntax

Field properties are specified in square brackets after the field type:

```javascript
var recordName: record {
    fieldName: type[property1, property2:value, ...],
    ...
};
```

### Available Properties

#### mandatory / required / notnull
Marks a field as required. The field must be present in the record data.

```javascript
var person: record {
    name: string[mandatory],
    email: string[required]
};

// Must provide mandatory fields
person = {"name": "Alice", "email": "alice@example.com"};
```

#### maxlength:N / maxlen:N
Specifies the maximum length for string fields (N is an integer).

```javascript
var user: record {
    username: string[maxlength:20],
    bio: string[maxlen:500]
};

// Strings are validated against max length
user = {"username": "johndoe", "bio": "Software developer"};
```

#### default:value
Provides a default value that will be used if the field is not present in the data.

```javascript
var product: record {
    name: string[mandatory],
    price: double[default:0.0],
    quantity: int[default:1],
    status: string[default:"available"]
};

// Only name is required, others get defaults
product = {"name": "Widget"};
// product.price will be 0.0
// product.quantity will be 1
// product.status will be "available"
```

### Combining Properties

Multiple properties can be combined on a single field:

```javascript
var employee: record {
    id: int[mandatory],
    name: string[mandatory, maxlength:50],
    email: string[mandatory, maxlength:100],
    department: string[default:"Unassigned"],
    salary: double[default:0.0]
};
```

## Validation

### Automatic Validation

Record validation happens automatically during assignment. If a record violates its constraints, an error is thrown:

```javascript
var user: record {
    username: string[mandatory, maxlength:10]
};

// This will throw an error - username too long
user = {"username": "verylongusername"};  // ERROR: exceeds max length

// This will throw an error - missing mandatory field
user = {};  // ERROR: missing mandatory field 'username'
```

### Explicit Validation

Use the `record.validate()` builtin function to explicitly validate a record:

```javascript
var person: record {
    name: string[mandatory],
    age: int[default:0]
};

person = {"name": "Alice"};

if call record.validate("person") then {
    print "Person record is valid";
} else {
    print "Person record is invalid";
}
```

**Note**: The `record.validate()` function takes the **variable name as a string**, not the variable itself.

## Examples

### Example 1: User Profile with Constraints

```javascript
var userProfile: record {
    username: string[mandatory, maxlength:20],
    email: string[mandatory, maxlength:100],
    displayName: string[maxlength:50, default:"Anonymous"],
    bio: string[maxlength:500],
    isActive: bool[default:true],
    createdAt: string[default:"2024-01-01"]
};

// Create profile - only required fields needed
userProfile = {
    "username": "johndoe",
    "email": "john@example.com"
};

// Defaults are automatically applied:
// userProfile.displayName = "Anonymous"
// userProfile.isActive = true
// userProfile.createdAt = "2024-01-01"

print "Display name: " + userProfile.displayName;
print "Active: " + userProfile.isActive;
```

### Example 2: Product Catalog Entry

```javascript
var product: record {
    sku: string[mandatory, maxlength:20],
    name: string[mandatory, maxlength:100],
    description: string[maxlength:500],
    price: double[default:0.0],
    quantity: int[default:0],
    category: string[default:"General"],
    available: bool[default:true]
};

// Minimal product creation
product = {
    "sku": "WIDGET-001",
    "name": "Premium Widget"
};

// Check if valid
if call record.validate("product") then {
    print "Product is valid";
    print "Price: " + product.price;  // 0.0
    print "Category: " + product.category;  // "General"
}
```

### Example 3: Configuration Object

```javascript
var config: record {
    host: string[mandatory],
    port: int[default:8080],
    ssl: bool[default:false],
    timeout: int[default:30],
    apiKey: string[maxlength:64, default:"none"],
    maxConnections: int[default:100]
};

// Simple configuration
config = {"host": "api.example.com"};

print "Configuration:";
print "  Host: " + config.host;
print "  Port: " + config.port;  // 8080
print "  SSL: " + config.ssl;  // false
print "  Timeout: " + config.timeout;  // 30
print "  API Key: " + config.apiKey;  // "none"
print "  Max Connections: " + config.maxConnections;  // 100
```

## Validation Error Messages

When validation fails, descriptive error messages are provided:

- **Missing mandatory field**: `"Error: Mandatory field 'fieldName' is missing from record"`
- **Exceeds max length**: `"Error: Field 'fieldName' exceeds maximum length of N (actual: M)"`
- **Invalid type**: `"Error: Field 'fieldName' has invalid type. Expected TYPE, got ACTUAL"`

## Implementation Notes

1. **Default values are applied during conversion**: The `RecordType.convertValue()` method applies defaults for missing fields.

2. **Validation happens at assignment**: When you assign a value to a typed record variable, validation occurs automatically.

3. **Case-insensitive field names**: Field names are matched case-insensitively during validation.

4. **Nested records**: Field properties are not currently supported for nested record fields.

5. **Type conversion**: Default values are automatically converted to match the field type.

## Technical Details

### RecordFieldMetadata Class

The `RecordFieldMetadata` class stores field-level constraints:

```java
public class RecordFieldMetadata {
    private final String fieldName;
    private final DataType fieldType;
    private final boolean mandatory;      // Field cannot be null
    private final Integer maxLength;      // Maximum length for strings
    private final Object defaultValue;    // Default value if not provided
    
    // ... methods for validation and access
}
```

### RecordType Updates

The `RecordType` class has been updated to:
- Store field metadata alongside field types
- Apply default values during conversion
- Validate mandatory and length constraints
- Provide access to field metadata

### Parser Updates

The parser now recognizes field property syntax within record definitions and creates appropriate `RecordFieldMetadata` objects.

## See Also

- [NESTED_RECORDS.md](docs/NESTED_RECORDS.md) - Documentation on nested record support
- [README.md](README.md) - General EBS language documentation
- Test scripts:
  - `test_record_features.ebs` - Comprehensive feature demonstration
  - `test_record_enhancements.ebs` - All enhancement tests
  - `test_record_validation.ebs` - Validation scenarios
