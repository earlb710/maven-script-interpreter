# Complex Data Types Guide

## Overview

This guide covers EBS's complex data types - **Arrays** and **Records** - providing practical examples and best practices for working with structured data in your applications.

**Related Documentation:**
- [EBS_COLLECTIONS_REFERENCE.md](../../docs/EBS_COLLECTIONS_REFERENCE.md) - Comprehensive collection types reference
- [ARRAY_SYNTAX_GUIDE.md](../../docs/ARRAY_SYNTAX_GUIDE.md) - Array syntax and implementation details
- [EBS_SCRIPT_SYNTAX.md](../../docs/EBS_SCRIPT_SYNTAX.md) - Complete language syntax

---

## Table of Contents

1. [Arrays](#arrays)
2. [Records](#records)
3. [Arrays of Records](#arrays-of-records)
4. [Nested Records](#nested-records)
5. [Common Patterns](#common-patterns)
6. [Best Practices](#best-practices)
7. [Performance Tips](#performance-tips)

---

## Arrays

Arrays are ordered collections of elements, accessed by numeric index. EBS supports both fixed-size and dynamic arrays with optional type safety.

### Array Declaration

#### Fixed-Size Arrays

```
// Traditional syntax - fixed size
var numbers: int[5];              // Array of 5 integers
var names: string[10];            // Array of 10 strings
var prices: double[20];           // Array of 20 doubles

// Enhanced syntax - better performance
var scores: array.int[100];       // Primitive int array (faster)
var temperatures: array.double[365];  // Primitive double array
```

**When to use enhanced syntax (`array.type`):**
- Better performance (uses primitive arrays internally)
- Lower memory overhead (no boxing/unboxing)
- Recommended for numeric arrays with many elements

#### Dynamic Arrays

```
// Dynamic arrays grow as needed
var items: string[*];             // Dynamic string array
var data: array.int[*];           // Dynamic int array

// Add elements dynamically
items[0] = "First";
items[1] = "Second";
items[10] = "Eleventh";  // Array grows automatically
```

#### Multi-Dimensional Arrays

```
// 2D arrays
var matrix: int[3, 4];            // 3 rows, 4 columns
var grid: double[10, 10];         // 10x10 grid

// 3D arrays
var cube: int[5, 5, 5];           // 3D cube

// Access elements
matrix[0, 0] = 10;
matrix[2, 3] = 42;
var value = grid[5, 7];
```

### Array Operations

#### Initialization

```
// Initialize during declaration
var colors: string[3];
colors[0] = "red";
colors[1] = "green";
colors[2] = "blue";

// Initialize from JSON array
var jsonArray: json = ["apple", "banana", "cherry"];
var fruits: array.string = call array(jsonArray);
```

#### Iteration

```
// Using for loop
var numbers: int[5] = [1, 2, 3, 4, 5];
var i: int = 0;
while i < numbers.length do {
    print "Number at index " + i + ": " + numbers[i];
    i = i + 1;
}

// Using enhanced for loop
for num in numbers do {
    print "Number: " + num;
}
```

#### Common Array Operations

```
// Get length
var size: int = numbers.length;

// Add element (dynamic arrays)
var items: string[*];
call array.push(items, "new item");

// Remove last element
var last: string = call array.pop(items);

// Sort array
call array.sort(numbers);

// Reverse array
call array.reverse(numbers);

// Find element
var index: int = call array.indexOf(numbers, 42);

// Check if contains
var found: bool = call array.contains(items, "target");
```

### Array Best Practices

**✅ DO:**
- Use `array.type` syntax for numeric arrays with many elements
- Specify array size if known (better performance than dynamic)
- Use `.length` property instead of tracking size manually
- Initialize arrays before use to avoid null pointer errors

**❌ DON'T:**
- Access array elements beyond bounds (causes runtime error)
- Mix types in typed arrays (defeats type safety)
- Use excessive array nesting (3+ dimensions) - consider records instead

---

## Records

Records are structured types with named fields, similar to structs or objects. Each field has a specific type, providing type safety for complex data.

### Record Declaration

#### Basic Record

```
// Define a record type
var person: record {
    name: string,
    age: int,
    email: string
};

// Initialize the record
person = {
    "name": "Alice Smith",
    "age": 30,
    "email": "alice@example.com"
};

// Access fields
print person.name;        // "Alice Smith"
print person.age;         // 30

// Update fields
person.age = 31;
person.email = "alice.smith@example.com";
```

#### Record with Multiple Types

```
// Record with various field types
var employee: record {
    id: int,
    name: string,
    salary: double,
    active: bool,
    hireDate: date
};

employee = {
    "id": 12345,
    "name": "Bob Johnson",
    "salary": 75000.50,
    "active": true,
    "hireDate": now()
};
```

### Type Conversion

Records automatically convert field values to match declared types:

```
var product: record {
    sku: string,
    price: double,
    quantity: int
};

// Automatic type conversion
product = {
    "sku": "ABC123",
    "price": "49.99",      // String → double
    "quantity": "100"      // String → int
};

print product.price;       // 49.99 (as double)
print product.quantity;    // 100 (as int)
```

### Record Validation

```
// Type validation happens on assignment
var config: record {
    timeout: int,
    retries: int,
    enabled: bool
};

// Valid assignment
config = {
    "timeout": 5000,
    "retries": 3,
    "enabled": true
};

// Invalid assignment (would cause error)
// config.timeout = "not a number";  // ERROR: type mismatch
```

---

## Arrays of Records

Combine arrays and records to create collections of structured data - perfect for tables, lists, and datasets.

### Declaration and Initialization

```
// Array of records
var employees: array.record {
    id: int,
    name: string,
    department: string,
    salary: double
};

// Initialize as dynamic array
employees = call array.record.create();

// Add records
employees[0] = {
    "id": 1,
    "name": "Alice",
    "department": "Engineering",
    "salary": 85000.00
};

employees[1] = {
    "id": 2,
    "name": "Bob",
    "department": "Sales",
    "salary": 65000.00
};
```

### Fixed-Size Arrays of Records

```
// Fixed size array of records
var customers: array.record[100] {
    customerId: int,
    name: string,
    email: string,
    balance: double
};

// Initialize records
var i: int = 0;
while i < 10 do {
    customers[i] = {
        "customerId": i + 1,
        "name": "Customer " + (i + 1),
        "email": "customer" + (i + 1) + "@example.com",
        "balance": 0.0
    };
    i = i + 1;
}
```

### Working with Record Arrays

```
// Iterate through records
var i: int = 0;
while i < employees.length do {
    var emp = employees[i];
    print emp.name + " works in " + emp.department;
    print "Salary: $" + emp.salary;
    i = i + 1;
}

// Filter records
var highEarners: array.record = call array.record.create();
var j: int = 0;
i = 0;
while i < employees.length do {
    if employees[i].salary > 75000 then {
        highEarners[j] = employees[i];
        j = j + 1;
    }
    i = i + 1;
}

// Sort by field (implement comparison function)
sortEmployeesBySalary(employees) {
    // Custom sorting logic
}
```

### Practical Example: Contact List

```
// Define contact record type
var contacts: array.record {
    id: int,
    firstName: string,
    lastName: string,
    phone: string,
    email: string,
    notes: string
};

// Add contacts
addContact(id: int, firstName: string, lastName: string, 
           phone: string, email: string) {
    var index: int = contacts.length;
    contacts[index] = {
        "id": id,
        "firstName": firstName,
        "lastName": lastName,
        "phone": phone,
        "email": email,
        "notes": ""
    };
}

// Find contact by id
findContact(searchId: int) return record {
    var i: int = 0;
    while i < contacts.length do {
        if contacts[i].id == searchId then {
            return contacts[i];
        }
        i = i + 1;
    }
    return null;
}

// Display all contacts
displayContacts() {
    print "Contact List (" + contacts.length + " contacts)";
    print "========================================";
    var i: int = 0;
    while i < contacts.length do {
        var c = contacts[i];
        print c.firstName + " " + c.lastName;
        print "  Phone: " + c.phone;
        print "  Email: " + c.email;
        print "----------------------------------------";
        i = i + 1;
    }
}
```

---

## Nested Records

Records can contain other records, creating hierarchical data structures.

### Basic Nested Records

```
// Record containing another record
var person: record {
    name: string,
    age: int,
    address: record {
        street: string,
        city: string,
        state: string,
        zipCode: string
    }
};

// Initialize nested record
person = {
    "name": "John Doe",
    "age": 35,
    "address": {
        "street": "123 Main St",
        "city": "Springfield",
        "state": "IL",
        "zipCode": "62701"
    }
};

// Access nested fields
print person.name;                    // "John Doe"
print person.address.city;            // "Springfield"
print person.address.zipCode;         // "62701"

// Update nested fields
person.address.street = "456 Oak Ave";
person.address.city = "Boston";
```

### Multiple Nesting Levels

```
// Deep nesting
var company: record {
    name: string,
    ceo: record {
        name: string,
        email: string,
        contact: record {
            phone: string,
            address: record {
                street: string,
                city: string,
                country: string
            }
        }
    }
};

// Initialize deeply nested structure
company = {
    "name": "Tech Corp",
    "ceo": {
        "name": "Jane CEO",
        "email": "jane@techcorp.com",
        "contact": {
            "phone": "555-1234",
            "address": {
                "street": "1 Corporate Plaza",
                "city": "San Francisco",
                "country": "USA"
            }
        }
    }
};

// Access deeply nested field
print company.ceo.contact.address.city;  // "San Francisco"
```

### Arrays of Nested Records

```
// Array of records with nested structure
var employees: array.record {
    id: int,
    name: string,
    department: record {
        id: int,
        name: string,
        location: string
    },
    contact: record {
        email: string,
        phone: string
    }
};

// Add employee with nested data
employees[0] = {
    "id": 100,
    "name": "Alice Engineer",
    "department": {
        "id": 10,
        "name": "Engineering",
        "location": "Building A"
    },
    "contact": {
        "email": "alice@company.com",
        "phone": "555-0100"
    }
};

// Access nested fields in array
print employees[0].department.name;      // "Engineering"
print employees[0].contact.email;        // "alice@company.com"
```

### Practical Example: Order System

```
// Order with nested structure
var orders: array.record {
    orderId: int,
    orderDate: date,
    customer: record {
        customerId: int,
        name: string,
        email: string
    },
    items: array.record {
        productId: int,
        productName: string,
        quantity: int,
        price: double
    },
    shipping: record {
        address: string,
        city: string,
        state: string,
        zipCode: string
    },
    total: double
};

// Create an order
createOrder(customerId: int, customerName: string, 
            customerEmail: string) return record {
    var order = {
        "orderId": call generateOrderId(),
        "orderDate": now(),
        "customer": {
            "customerId": customerId,
            "name": customerName,
            "email": customerEmail
        },
        "items": call array.record.create(),
        "shipping": {
            "address": "",
            "city": "",
            "state": "",
            "zipCode": ""
        },
        "total": 0.0
    };
    return order;
}

// Add item to order
addOrderItem(order: record, productId: int, productName: string,
             quantity: int, price: double) {
    var itemIndex: int = order.items.length;
    order.items[itemIndex] = {
        "productId": productId,
        "productName": productName,
        "quantity": quantity,
        "price": price
    };
    
    // Update total
    order.total = order.total + (quantity * price);
}
```

---

## Common Patterns

### Pattern 1: Data Table

```
// Represent tabular data
var salesData: array.record {
    date: date,
    product: string,
    quantity: int,
    revenue: double,
    region: string
};

// Load data
loadSalesData() {
    // Load from database or file
    salesData[0] = {
        "date": call date.parse("2025-01-15"),
        "product": "Widget A",
        "quantity": 100,
        "revenue": 5000.00,
        "region": "North"
    };
    // ... more data
}

// Aggregate by region
calculateRegionTotal(region: string) return double {
    var total: double = 0.0;
    var i: int = 0;
    while i < salesData.length do {
        if salesData[i].region == region then {
            total = total + salesData[i].revenue;
        }
        i = i + 1;
    }
    return total;
}
```

### Pattern 2: Configuration Object

```
// Application configuration as record
var appConfig: record {
    database: record {
        host: string,
        port: int,
        username: string,
        password: string,
        database: string
    },
    server: record {
        port: int,
        ssl: bool,
        timeout: int
    },
    logging: record {
        level: string,
        file: string,
        maxSize: int
    }
};

// Load configuration
loadConfig() {
    appConfig = {
        "database": {
            "host": "localhost",
            "port": 5432,
            "username": "admin",
            "password": "secret",
            "database": "myapp"
        },
        "server": {
            "port": 8080,
            "ssl": true,
            "timeout": 30
        },
        "logging": {
            "level": "INFO",
            "file": "/var/log/app.log",
            "maxSize": 10485760
        }
    };
}
```

### Pattern 3: Tree/Hierarchy

```
// Tree node with children array
var node: record {
    id: int,
    name: string,
    value: string,
    children: array.record {
        id: int,
        name: string,
        value: string
    }
};

// Build tree
buildTree() {
    node = {
        "id": 1,
        "name": "root",
        "value": "Root Node",
        "children": call array.record.create()
    };
    
    // Add children
    node.children[0] = {
        "id": 2,
        "name": "child1",
        "value": "First Child"
    };
    
    node.children[1] = {
        "id": 3,
        "name": "child2",
        "value": "Second Child"
    };
}
```

### Pattern 4: State Machine

```
// State with transitions
var stateMachine: record {
    currentState: string,
    states: array.record {
        name: string,
        transitions: array.record {
            event: string,
            toState: string,
            action: string
        }
    }
};

// Initialize state machine
initStateMachine() {
    stateMachine = {
        "currentState": "idle",
        "states": call array.record.create()
    };
    
    // Define idle state
    stateMachine.states[0] = {
        "name": "idle",
        "transitions": call array.record.create()
    };
    
    stateMachine.states[0].transitions[0] = {
        "event": "start",
        "toState": "running",
        "action": "startProcess"
    };
}
```

---

## Best Practices

### Choosing Data Types

**Use Arrays when:**
- You need ordered, sequential data
- You access elements by numeric index
- All elements are the same type
- You need to iterate in order

**Use Records when:**
- You have structured data with named fields
- Different fields have different types
- You need type validation
- Data represents a real-world entity

**Use Arrays of Records when:**
- You have multiple instances of the same structure
- You need a collection of entities (customers, orders, products)
- You're representing tabular data
- You need to search, filter, or sort structured data

### Type Safety

```
// ✅ GOOD: Explicit types
var customer: record {
    id: int,
    name: string,
    balance: double
};

// ✅ GOOD: Type-safe array
var numbers: array.int[100];

// ❌ AVOID: Untyped JSON (no validation)
var data: json = {"id": 1, "name": "test"};
```

### Initialization

```
// ✅ GOOD: Initialize before use
var person: record {
    name: string,
    age: int
};
person = {
    "name": "Alice",
    "age": 30
};

// ✅ GOOD: Initialize array with size
var scores: int[50];

// ❌ AVOID: Using uninitialized records
// print person.name;  // Error if not initialized
```

### Field Naming

```
// ✅ GOOD: Clear, descriptive names
var employee: record {
    employeeId: int,
    firstName: string,
    lastName: string,
    hireDate: date,
    annualSalary: double
};

// ❌ AVOID: Cryptic abbreviations
var emp: record {
    eid: int,
    fn: string,
    ln: string,
    dt: date,
    sal: double
};
```

### Nesting Depth

```
// ✅ GOOD: Reasonable nesting (2-3 levels)
var order: record {
    orderId: int,
    customer: record {
        name: string,
        address: record {
            street: string,
            city: string
        }
    }
};

// ❌ AVOID: Excessive nesting (4+ levels)
// Makes code hard to read and maintain
```

---

## Performance Tips

### Array Performance

1. **Use Fixed-Size Arrays When Possible**
```
// ✅ BETTER: Known size
var items: string[1000];

// ⚠️ SLOWER: Dynamic resizing
var items: string[*];
```

2. **Use Enhanced Syntax for Numeric Arrays**
```
// ✅ FASTER: Primitive array
var numbers: array.int[10000];

// ⚠️ SLOWER: Boxed array
var numbers: int[10000];
```

3. **Cache Array Length in Loops**
```
// ✅ EFFICIENT
var len: int = items.length;
var i: int = 0;
while i < len do {
    // process items[i]
    i = i + 1;
}

// ⚠️ LESS EFFICIENT (recalculates length each iteration)
var i: int = 0;
while i < items.length do {
    // process items[i]
    i = i + 1;
}
```

### Record Performance

1. **Reuse Records Instead of Creating New Ones**
```
// ✅ GOOD: Reuse
var temp: record {name: string, value: int};
var i: int = 0;
while i < 1000 do {
    temp.name = "Item " + i;
    temp.value = i;
    call processRecord(temp);
    i = i + 1;
}

// ⚠️ SLOWER: Create new each time
var i: int = 0;
while i < 1000 do {
    var temp: record {name: string, value: int};
    temp = {"name": "Item " + i, "value": i};
    call processRecord(temp);
    i = i + 1;
}
```

2. **Minimize Nesting for Frequently Accessed Data**
```
// ✅ FASTER: Flat structure
var customer: record {
    id: int,
    name: string,
    city: string
};

// ⚠️ SLOWER: Nested (more lookups)
var customer: record {
    id: int,
    name: string,
    address: record {
        city: string
    }
};
```

### Memory Management

1. **Use Appropriate Array Sizes**
```
// ✅ GOOD: Right-sized
var monthlyData: int[12];

// ❌ WASTEFUL: Oversized
var monthlyData: int[10000];
```

2. **Clear Large Arrays When Done**
```
// Free memory for large arrays
var largeData: array.record[10000];
// ... use largeData ...
largeData = null;  // Allow garbage collection
```

---

## Quick Reference

### Array Syntax Comparison

| Feature | Traditional | Enhanced | Notes |
|---------|------------|----------|-------|
| Fixed int array | `int[10]` | `array.int[10]` | Enhanced is faster |
| Dynamic int array | `int[*]` | `array.int[*]` | Enhanced is faster |
| Fixed string array | `string[10]` | `array.string[10]` | Similar performance |
| 2D array | `int[3,4]` | `array.int[3,4]` | Enhanced is faster |

### Common Record Patterns

| Pattern | Syntax Example |
|---------|----------------|
| Simple record | `record {name: string, age: int}` |
| Nested record | `record {name: string, address: record {city: string}}` |
| Array of records | `array.record {id: int, name: string}` |
| Fixed size record array | `array.record[100] {id: int, name: string}` |

---

## Additional Resources

- **[EBS_COLLECTIONS_REFERENCE.md](../../docs/EBS_COLLECTIONS_REFERENCE.md)** - Complete collection types documentation
- **[ARRAY_SYNTAX_GUIDE.md](../../docs/ARRAY_SYNTAX_GUIDE.md)** - Detailed array implementation guide  
- **[RECORD_TYPE_IMPLEMENTATION.md](../../docs/RECORD_TYPE_IMPLEMENTATION.md)** - Record type implementation details
- **[NESTED_RECORDS.md](../../docs/NESTED_RECORDS.md)** - Nested record feature documentation

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-18  
**Maintained by:** EBS Development Team
