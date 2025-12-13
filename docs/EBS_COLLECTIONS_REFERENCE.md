# EBS Collections Reference

**Documentation Version: 1.0.7.11**

This document provides a comprehensive overview of all collection types available in the EBS (Earl Bosch Script) language.

## Table of Contents

1. [Overview](#overview)
2. [Arrays](#arrays)
3. [Queues](#queues)
4. [Maps](#maps)
5. [JSON Collections](#json-collections)
6. [Collection Type Comparison](#collection-type-comparison)
7. [Choosing the Right Collection](#choosing-the-right-collection)

---

## Overview

EBS provides several collection types for storing and managing groups of data:

| Collection Type | Description | Key Characteristics | Primary Use Case |
|----------------|-------------|---------------------|------------------|
| **Array** | Ordered, indexed collection | Fixed or dynamic size, type-safe options | Sequential data, indexed access |
| **Queue** | FIFO (First-In-First-Out) | Ordered processing, enqueue/dequeue | Task processing, ordered workflows |
| **Map** | Key-value store | String keys, any value type | Configuration, dictionaries, lookups |
| **JSON** | Flexible JSON structure | Supports objects and arrays | Data interchange, nested structures |

---

## Arrays

Arrays are ordered collections with indexed access. EBS provides multiple array variants for different use cases.

### Array Declaration Variants

#### 1. Traditional Typed Arrays
```javascript
// Fixed-size arrays
var numbers: int[5];           // Array of 5 integers
var names: string[10];         // Array of 10 strings
var matrix: int[3, 4];         // 2D array: 3 rows, 4 columns
var cube: double[2, 3, 4];     // 3D array

// Dynamic arrays (growable)
var items: string[*];          // Dynamic string array
```

#### 2. Enhanced array.type Syntax
```javascript
// Fixed-size with explicit type
var strings: array.string[5];      // String array
var ints: array.int[10];           // Integer array (primitive int[])
var nums: array.number[5];         // Number (double) array
var bytes: array.byte[10];         // Byte array (primitive byte[])
var floats: array.float[5];        // Float array
var longs: array.long[3];          // Long array
var bools: array.bool[8];          // Boolean array
var dates: array.date[7];          // Date array

// Dynamic arrays
var dynamicStrings: array.string[*];  // Dynamic string array
var dynamicInts: array.int[*];        // Dynamic integer array
```

#### 3. Generic Arrays
```javascript
var items: array[10];          // Generic array of 10 elements
var collection: array[*];      // Dynamic generic array
var anyType: array.any[*];     // Explicit any type (same as array)
```

#### 4. Specialized Arrays
```javascript
// Bitmap arrays (byte storage with named bit fields)
var flags: array.bitmap[10];   // Bitmap array (uses ArrayFixedByte)

// Intmap arrays (integer storage with named bit fields)
var configs: array.intmap[10]; // Intmap array (uses ArrayFixedInt)

// Record arrays
var employees: array.record[*]; // Dynamic array of records
```

### Array Literals
```javascript
var numbers = [1, 2, 3, 4, 5];
var names = ["Alice", "Bob", "Charlie"];
var mixed = [1, "two", 3.0, true];  // Generic array

// Nested arrays
var matrix = [[1, 2], [3, 4], [5, 6]];
```

### Array Operations

#### Access and Modification
```javascript
var first = numbers[0];         // Get first element
numbers[2] = 99;                // Set element
var cell = matrix[1, 2];        // Multi-dimensional access

// Array properties
var count = numbers.length;     // Get array length
```

#### Built-in Array Functions
```javascript
// Fill array with value
call array.fill(numbers, 0);

// Expand dynamic array
var dynamic: array[*];
call array.expand(dynamic, 10);  // Expand to 10 elements

// Array manipulation (via builtin functions)
call array.push(arr, value);     // Add to end
var last = call array.pop(arr);  // Remove from end
call array.shift(arr);           // Remove from start
call array.unshift(arr, value);  // Add to start

// Slicing and joining
var subset = call array.slice(arr, start, end);
var text = call array.join(arr, separator);
```

#### Type Casting Between Array Variants
```javascript
// Byte and Bitmap arrays (interchangeable)
var byteArray: array.byte[5];
var bitmapArray = call array.asBitmap(byteArray);
var backToByte = call array.asByte(bitmapArray);

// Int and Intmap arrays (interchangeable)
var intArray: array.int[5];
var intmapArray = call array.asIntmap(intArray);
var backToInt = call array.asInt(intmapArray);
```

### Array Type Comparison

| Syntax | Storage Class | Memory Model | Best For |
|--------|---------------|--------------|----------|
| `int[10]` | ArrayFixed | Object[] with boxed Integer | Small arrays (< 100 elements) |
| `array.int[10]` | ArrayFixedInt | Primitive int[] | Large arrays, performance-critical |
| `array.byte[10]` | ArrayFixedByte | Primitive byte[] | Byte data, low memory usage |
| `array.bitmap[10]` | ArrayFixedByte | Primitive byte[] | Bit-packed data with named fields |
| `array.intmap[10]` | ArrayFixedInt | Primitive int[] | Bit-packed 32-bit fields |
| `array[10]` | ArrayFixed | Object[] | Mixed types, small collections |
| `string[*]` | ArrayDynamic | Growable Object[] | Dynamic, type-safe collections |

**Performance Notes:**
- `array.int[n]` is faster than `int[n]` for large arrays due to primitive storage
- `array.byte[n]` has the smallest memory footprint
- Dynamic arrays (`[*]`) grow automatically with `array.expand()`

---

## Queues

Queues provide FIFO (First-In-First-Out) data structures for ordered processing.

### Queue Declaration

```javascript
// Queue with specific type
var stringQueue: queue.string;
var intQueue: queue.int;
var doubleQueue: queue.double;
var byteQueue: queue.byte;
var longQueue: queue.long;
var floatQueue: queue.float;
var boolQueue: queue.bool;
var dateQueue: queue.date;
```

### Queue Operations

```javascript
// Add element to the back of the queue
call queue.enqueue(myQueue, value);

// Remove and return element from the front
var item = call queue.dequeue(myQueue);

// View front element without removing
var front = call queue.peek(myQueue);

// Get number of elements
var count = call queue.size(myQueue);

// Check if queue is empty
var isEmpty = call queue.isEmpty(myQueue);

// Remove all elements
call queue.clear(myQueue);

// Check if queue contains a value
var found = call queue.contains(myQueue, searchValue);

// Convert queue to array
var arr = call queue.toArray(myQueue);
```

### Queue Example: Task Processing

```javascript
// Create a task queue
var tasks: queue.string;

// Add tasks
call queue.enqueue(tasks, "Process file A");
call queue.enqueue(tasks, "Process file B");
call queue.enqueue(tasks, "Send notification");

print "Tasks in queue: " + call queue.size(tasks);

// Process all tasks in order
while !call queue.isEmpty(tasks) {
    var task = call queue.dequeue(tasks);
    print "Executing: " + task;
}
```

### Queue Characteristics

- **Order**: FIFO (First-In-First-Out) - elements are processed in insertion order
- **Type Safety**: Queue type determines allowed element types
- **Implementation**: Backed by Java's ArrayDeque for efficient operations
- **Use Cases**: 
  - Task processing pipelines
  - Message queues
  - Print job management
  - Ordered workflows

---

## Maps

Maps provide key-value storage where keys are strings and values can be any type. Maps are backed by JSON objects.

### Map Declaration

```javascript
// Declare a map variable
var config: map = {"host": "localhost", "port": 8080, "debug": true};

// Empty map
var settings: map = {};
```

### Casting JSON to Map

```javascript
// Cast JSON object to map
var jsonData: json = {"name": "Alice", "age": 30, "city": "NYC"};
var userMap = map(jsonData);

// Error: JSON arrays cannot be cast to map
var arrayData: json = [1, 2, 3];
var badCast = map(arrayData);  // Error: Only JSON objects can be cast to map
```

### Map Operations

Maps use the `json.*` builtin functions for operations:

```javascript
var myMap: map = {"key1": "value1", "key2": 123};

// Get values (with type-specific functions)
var name = call json.get(myMap, "key1");           // Generic get
var age = call json.getint(myMap, "key2");         // Get as integer
var city = call json.getstring(myMap, "city", ""); // Get with default

// Set/add values
call json.set(myMap, "key1", "newValue");     // Update existing
call json.set(myMap, "key3", "newKey");       // Add new key

// Remove keys
call json.remove(myMap, "key2");

// Nested maps
var nested: json = {"user": {"name": "Bob", "settings": {"theme": "dark"}}};
var nestedMap = map(nested);
var userName = call json.get(call json.get(nestedMap, "user"), "name");
```

### Map Characteristics

- **Keys**: Always strings
- **Values**: Any type (string, int, bool, arrays, nested maps/JSON)
- **Flexible**: No predefined schema required
- **Implementation**: Backed by JSON objects (Java's LinkedHashMap)
- **Use Cases**:
  - Configuration storage
  - Dictionary/lookup tables
  - Dynamic key-value stores
  - API response handling

---

## JSON Collections

JSON in EBS can represent both arrays and objects, providing flexible data structures.

### JSON Objects (Map-like)

```javascript
var person: json = {
    "name": "Alice",
    "age": 30,
    "email": "alice@example.com",
    "address": {
        "city": "New York",
        "zip": "10001"
    }
};

// Alternative: JavaScript-style unquoted keys
var config: json = {
    theme: "dark",
    volume: 80,
    notifications: true
};
```

### JSON Arrays

```javascript
var numbers: json = [1, 2, 3, 4, 5];
var names: json = ["Alice", "Bob", "Charlie"];
var mixed: json = [1, "two", true, {"key": "value"}];

// Nested structures
var data: json = {
    "users": [
        {"name": "Alice", "age": 30},
        {"name": "Bob", "age": 25}
    ],
    "count": 2
};
```

### JSON Operations

#### Object Operations
```javascript
// Get values
var name = call json.get(person, "name");
var age = call json.getint(person, "age");

// Set values
call json.set(person, "age", 31);
call json.set(person, "phone", "555-1234");  // Add new field

// Remove fields
call json.remove(person, "email");
```

#### Array Operations
```javascript
// JSON arrays can be accessed like regular arrays
var listData: json = {"items": [1, 2, 3]};
var list = call json.get(listData, "items");

// Access by index (direct array access)
var first = list[0];

// Get length
var size = list.length;

// Add elements (using path notation)
call json.add(listData, "items", 4);         // Append to items array
call json.insert(listData, "items", 0, 0);   // Insert at index 0
```

#### Parse and Stringify
```javascript
// Parse JSON string
var jsonString = '{"name": "Bob", "age": 25}';
var obj = call json.jsonfromstring(jsonString);

// JavaScript-style JSON (unquoted keys)
var jsonString2 = '{name: "Charlie", age: 35}';
var obj2 = call json.jsonfromstring(jsonString2);

// Convert to string
var str = call string.tostring(person);
```

### Variable References in JSON

You can reference EBS variables directly in JSON using `$variable` syntax:

```javascript
var userName: string = "Alice";
var userAge: int = 30;

var person: json = {
    "name": $userName,    // References userName variable (no quotes)
    "age": $userAge       // References userAge variable
};
// Result: {"name": "Alice", "age": 30}
```

**Important**: `$variable` (without quotes) = variable reference; `"$variable"` (with quotes) = literal string

### JSON Characteristics

- **Flexible**: Can hold objects, arrays, primitives, or nested structures
- **Dynamic**: No predefined schema
- **Type Support**: Strings, numbers, booleans, null, arrays, objects
- **Parsing**: Supports both standard JSON (quoted keys) and JavaScript-style (unquoted keys)
- **Use Cases**:
  - Data interchange
  - API responses
  - Configuration files
  - Complex nested data structures

---

## Collection Type Comparison

| Feature | Array | Queue | Map | JSON |
|---------|-------|-------|-----|------|
| **Ordered** | Yes (indexed) | Yes (FIFO) | No (unordered keys) | Yes (arrays), No (objects) |
| **Indexed Access** | Yes (`arr[0]`) | No | No | Yes (arrays only) |
| **Key Access** | No | No | Yes (string keys) | Yes (objects only) |
| **Type Safety** | Optional | Yes | No | No |
| **Dynamic Size** | Optional (`[*]`) | Yes | Yes | Yes |
| **Nesting** | Yes | No | Yes | Yes |
| **Duplicate Values** | Yes | Yes | Yes | Yes |
| **Primary Operation** | Index-based | Enqueue/Dequeue | Key-value lookup | Flexible access |

---

## Choosing the Right Collection

### Use **Array** when:
- You need indexed access to elements
- Order matters and you need random access
- You want type safety for elements
- You're working with fixed-size or predictable data
- Performance is critical (use `array.int[n]` for large numeric arrays)

**Examples**: List of items, coordinates, matrix operations, buffer storage

### Use **Queue** when:
- You need FIFO (First-In-First-Out) processing
- You're implementing task queues or workflows
- Order of processing is important
- You want to decouple producers from consumers

**Examples**: Task scheduler, job queue, message processing, print queue

### Use **Map** when:
- You need key-value lookups
- Keys are strings
- Schema is flexible or unknown at compile time
- You're building configuration stores or dictionaries

**Examples**: Configuration settings, user preferences, lookup tables, caches

### Use **JSON** when:
- You need to work with external JSON data
- Structure is deeply nested
- You need both arrays and objects in the same data
- You're interfacing with APIs or files
- Schema changes frequently

**Examples**: API responses, configuration files, data interchange, complex nested data

### Performance Considerations

| Collection Type | Memory Efficiency | Access Speed | Insertion Speed | Best Size Range |
|----------------|------------------|--------------|-----------------|-----------------|
| `int[n]` | Medium | Fast | N/A (fixed) | < 100 elements |
| `array.int[n]` | High | Very Fast | N/A (fixed) | ≥ 100 elements |
| `array.byte[n]` | Very High | Very Fast | N/A (fixed) | Any |
| `array[*]` | Medium | Fast | Medium | Dynamic |
| `queue.type` | Medium | Fast | Fast | Dynamic |
| `map` | Medium | Medium | Fast | Any |
| `json` | Medium | Medium | Fast | Any |

---

## Advanced Collection Examples

### Example 1: Array Processing Pipeline
```javascript
// Process numeric data with arrays
var data: array.int[100];
call array.fill(data, 0);

// Fill with computed values
for (var i: int = 0; i < data.length; i++) {
    data[i] = i * i;
}

// Convert to dynamic array for filtering
var filtered: array.int[*];
foreach value in data {
    if value > 100 then {
        call array.expand(filtered, filtered.length + 1);
        filtered[filtered.length - 1] = value;
    }
}
```

### Example 2: Queue-Based Task Processor
```javascript
// Multi-stage task processing
var pendingTasks: queue.string;
var completedTasks: queue.string;

// Add tasks
call queue.enqueue(pendingTasks, "Download file");
call queue.enqueue(pendingTasks, "Process data");
call queue.enqueue(pendingTasks, "Generate report");

// Process tasks
while !call queue.isEmpty(pendingTasks) {
    var task = call queue.dequeue(pendingTasks);
    print "Processing: " + task;
    
    // Simulate work
    call thread.sleep(100);
    
    // Mark complete
    call queue.enqueue(completedTasks, task);
}

print "Completed " + call queue.size(completedTasks) + " tasks";
```

### Example 3: Configuration Management with Maps
```javascript
// Application configuration
var appConfig: map = {
    "database": {
        "host": "localhost",
        "port": 5432,
        "name": "myapp"
    },
    "logging": {
        "level": "INFO",
        "file": "app.log"
    },
    "features": {
        "enableCache": true,
        "maxConnections": 100
    }
};

// Access nested configuration
var dbConfig = call json.get(appConfig, "database");
var dbHost = call json.getstring(dbConfig, "host");
var dbPort = call json.getint(dbConfig, "port");

print "Connecting to " + dbHost + ":" + dbPort;

// Update configuration
call json.set(appConfig, "features", 
    {"enableCache": false, "maxConnections": 200});
```

### Example 4: JSON Data Processing
```javascript
// Parse JSON from API
var jsonResponse = '{
    "users": [
        {"id": 1, "name": "Alice", "active": true},
        {"id": 2, "name": "Bob", "active": false},
        {"id": 3, "name": "Charlie", "active": true}
    ],
    "total": 3
}';

var data = call json.jsonfromstring(jsonResponse);
var users = call json.get(data, "users");
var total = call json.getint(data, "total");

// Process users (arrays can be accessed directly)
for (var i: int = 0; i < users.length; i++) {
    var user = users[i];
    var name = call json.getstring(user, "name");
    var active = call json.getbool(user, "active");
    
    if active then {
        print name + " is active";
    }
}
```

---

## Summary

EBS provides four main collection types, each optimized for specific use cases:

1. **Arrays**: Ordered, indexed collections with optional type safety and multiple implementation variants
2. **Queues**: FIFO data structures for ordered processing and task management
3. **Maps**: Flexible key-value stores for configuration and lookup tables
4. **JSON**: Universal data structures supporting both arrays and objects for data interchange

Choose the collection type that best matches your data access patterns and performance requirements. For detailed syntax and all available operations, see:

- [EBS Script Syntax Reference](EBS_SCRIPT_SYNTAX.md)
- [Array Syntax Guide](ARRAY_SYNTAX_GUIDE.md)
- [EBS Language Reference](../EBS_LANGUAGE_REFERENCE.md)

---

**Document Version**: 1.0.7.11 (Last Updated: 2025-12-13)
