# Bidirectional Array Iteration

## Overview

The EBS scripting language now supports bidirectional iteration over arrays, allowing you to iterate arrays in both forward and reverse directions using the `array.reverse()` builtin function.

## Features

- **Forward Iteration**: Standard iteration from index 0 to n-1 (existing behavior)
- **Reverse Iteration**: New capability to iterate from index n-1 to 0
- **Compatible with all array types**: Works with dynamic arrays, fixed arrays, and specialized types (byte, int, bitmap, intmap)
- **Uses existing `foreach` syntax**: No new language constructs needed
- **Supports break and continue**: Full control flow support in reverse iteration

## Implementation

### ArrayDef Interface Enhancement

The `ArrayDef<E, A>` interface now includes a `reverseIterator()` method:

```java
public interface ArrayDef<E, A> extends Iterable<E> {
    // ... existing methods ...
    
    /**
     * Returns an iterator that iterates over the array in reverse order.
     * This enables bidirectional iteration over arrays.
     * 
     * @return an iterator that traverses the array from end to beginning
     */
    public Iterator<E> reverseIterator();
}
```

### Implementations

All concrete array classes implement `reverseIterator()`:

- **ArrayFixed**: Reverse iterator for generic fixed arrays
- **ArrayDynamic**: Reverse iterator for dynamic arrays backed by ArrayList
- **ArrayFixedByte**: Reverse iterator for byte and bitmap arrays
- **ArrayFixedInt**: Reverse iterator for integer and intmap arrays

### ReverseArrayWrapper

A lightweight wrapper class that makes reverse iteration compatible with the `foreach` statement:

```java
public class ReverseArrayWrapper<E> implements Iterable<E> {
    private final ArrayDef<E, ?> array;
    
    @Override
    public Iterator<E> iterator() {
        return array.reverseIterator();
    }
}
```

## Usage

### Basic Syntax

```ebs
foreach item in #array.reverse(myArray) {
    // Process items in reverse order
}
```

### Examples

#### Example 1: String Array Reverse Iteration

```ebs
var names = ["Alice", "Bob", "Charlie", "Diana"];

print "Forward:";
foreach name in names {
    print name;  // Alice, Bob, Charlie, Diana
}

print "Reverse:";
foreach name in #array.reverse(names) {
    print name;  // Diana, Charlie, Bob, Alice
}
```

#### Example 2: Integer Array with Calculations

```ebs
var numbers = [1, 2, 3, 4, 5];
var sum: int = 0;

// Sum from start to end
foreach num in numbers {
    sum = sum + num;
    print "Forward sum: " + sum;
}

sum = 0;

// Sum from end to start
foreach num in #array.reverse(numbers) {
    sum = sum + num;
    print "Reverse sum: " + sum;
}
```

#### Example 3: Using Break in Reverse Iteration

```ebs
var countdown = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1];

foreach num in #array.reverse(countdown) {
    if (num == 5) {
        print "Stopping at 5";
        break;
    }
    print num;  // Prints: 1, 2, 3, 4
}
```

#### Example 4: Fixed Array Reverse Iteration

```ebs
var values: int[5];
values[0] = 10;
values[1] = 20;
values[2] = 30;
values[3] = 40;
values[4] = 50;

foreach val in #array.reverse(values) {
    print val;  // Prints: 50, 40, 30, 20, 10
}
```

## API Reference

### array.reverse(array)

Returns a wrapper that enables reverse iteration over the provided array.

**Parameters:**
- `array` (ArrayDef): The array to iterate in reverse

**Returns:**
- ReverseArrayWrapper: An iterable wrapper that provides reverse iteration

**Throws:**
- InterpreterError if array is null or not an array type

**Example:**
```ebs
var items = ["first", "second", "third"];
foreach item in #array.reverse(items) {
    print item;
}
```

## Use Cases

### 1. Reverse Processing
Process items in reverse order without modifying the original array:
```ebs
var history = ["step1", "step2", "step3"];
foreach step in #array.reverse(history) {
    print "Undo: " + step;
}
```

### 2. Countdown Operations
```ebs
var countdown = [10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0];
foreach num in #array.reverse(countdown) {
    print "T-minus " + num;
}
```

### 3. Stack-like Processing (LIFO)
```ebs
var stack = [];
call array.add(stack, "first");
call array.add(stack, "second");
call array.add(stack, "third");

// Process like a stack (Last-In-First-Out)
foreach item in #array.reverse(stack) {
    print "Pop: " + item;
}
```

### 4. Reverse Search
```ebs
var items = ["apple", "banana", "cherry", "date"];
var searchFor = "banana";
var found = false;

foreach item in #array.reverse(items) {
    if (item == searchFor) {
        print "Found from end: " + item;
        found = true;
        break;
    }
}
```

## Performance Considerations

- **Memory**: The `array.reverse()` function creates a lightweight wrapper object; it does not create a reversed copy of the array
- **Iteration Speed**: Reverse iteration has the same performance characteristics as forward iteration
- **No Side Effects**: The original array is never modified by reverse iteration

## Compatibility

- **Fully backward compatible**: Existing code continues to work unchanged
- **No breaking changes**: Forward iteration behavior is unchanged
- **Works with all array types**: Dynamic, fixed, byte, int, bitmap, and intmap arrays

## Implementation Details

### Iterator Pattern

The implementation follows the standard Java Iterator pattern:

```java
@Override
public Iterator<E> reverseIterator() {
    return new Iterator<E>() {
        int idx = elements.length - 1;

        @Override
        public boolean hasNext() {
            return idx >= 0;
        }

        @Override
        public E next() {
            return elements[idx--];
        }
    };
}
```

### Integration with foreach

The `foreach` statement already supports any `Iterable<?>`, so the ReverseArrayWrapper integrates seamlessly:

```java
// In Interpreter.java visitForEachStatement()
else if (it instanceof Iterable<?> each) {
    for (Object elem : each) {
        try {
            runBodyWith.accept(elem);
        } catch (BreakSignal b) {
            break;
        }
    }
}
```

## Testing

A comprehensive test suite is available in `scripts/examples/bidirectional_iteration.ebs`:

- String array forward/reverse iteration
- Integer array forward/reverse iteration
- Calculations in both directions
- Empty array handling
- Single element array handling
- Break statement in reverse iteration

Run the test:
```bash
mvn exec:java -Dexec.mainClass="com.eb.script.Run" \
  -Dexec.args="scripts/examples/bidirectional_iteration.ebs"
```

## Future Enhancements

Potential future improvements:
- Bidirectional iterator with `hasPrevious()` and `previous()` methods
- Slice iteration (e.g., iterate a subrange in reverse)
- Custom comparator-based iteration orders
- Parallel iteration over multiple arrays

## See Also

- [EBS Language Reference](README.md)
- [Array Operations](ScriptInterpreter/scripts/examples/array.ebs)
- [foreach Statement](ScriptInterpreter/scripts/examples/foreach.ebs)
