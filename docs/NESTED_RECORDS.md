# Nested Records Feature

## Overview
The record type system now supports nested records - records within records. This allows you to define complex hierarchical data structures with type validation at all levels.

## Syntax

### Simple Nested Record
```javascript
var person:record{
    name: string,
    age: int,
    address: record {
        street: string,
        city: string,
        zipCode: string
    }
};
```

### Multiple Levels of Nesting
```javascript
var employee:record{
    id: int,
    name: string,
    contact: record {
        email: string,
        phone: string,
        address: record {
            street: string,
            city: string,
            state: string
        }
    }
};
```

### Arrays with Nested Records
```javascript
var employees:array.record{
    id: int,
    name: string,
    department: record {
        name: string,
        location: string
    }
};
```

## Usage

### Initialization
```javascript
person = {
    "name": "John Doe",
    "age": 30,
    "address": {
        "street": "123 Main St",
        "city": "New York",
        "zipCode": "10001"
    }
};
```

### Type Conversion
Type conversion works at all nesting levels:
```javascript
product = {
    "sku": "ABC123",
    "price": "49.99",  // String → double
    "supplier": {
        "name": "Acme Corp",
        "rating": "5"  // String → int
    }
};
```

### Field Assignment
Top-level field assignment works:
```javascript
person.name = "Jane Doe";
person.age = 35;
```

Nested field assignment (`person.address.city = "Boston"`) requires the property access feature to be fully implemented.

## Implementation Details

### RecordType Class
- Added `nestedRecords` map to store nested RecordType definitions
- `addNestedRecord()` method to add nested record fields
- `getNestedRecordType()` method to retrieve nested record type
- `validateValue()` recursively validates nested records
- `convertValue()` recursively converts nested record values

### Parser
- `parseRecordFields()` now recognizes "record" keyword within field definitions
- Recursive parsing of nested record structures
- Supports unlimited nesting depth

### Type Validation
- Validation occurs at all nesting levels
- Type conversion happens recursively for nested structures
- Works with arrays of records containing nested records

## Examples

See test scripts:
- `test_nested_records.ebs` - Comprehensive nested record tests
- `test_nested_field_assignment.ebs` - Field assignment tests

## Limitations

- Nested field assignment (e.g., `record.nested.field = value`) requires property access expression support
- Property access in expressions still being developed
