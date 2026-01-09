# Bidirectional Array Iteration - Architecture Diagram

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      EBS Script Layer                            │
│                                                                   │
│  var arr = [1, 2, 3, 4, 5]                                       │
│                                                                   │
│  foreach item in arr { }              // Forward iteration       │
│  foreach item in #array.reverse(arr) { }  // Reverse iteration  │
│                                                                   │
└────────────────┬────────────────────────────────┬────────────────┘
                 │                                │
                 │                                │
        ┌────────▼────────┐              ┌────────▼────────────────┐
        │  foreach        │              │  array.reverse()         │
        │  Statement      │              │  Builtin Function        │
        │                 │              │                          │
        │  Evaluates      │              │  Returns:                │
        │  iterable       │              │  ReverseArrayWrapper     │
        └────────┬────────┘              └────────┬─────────────────┘
                 │                                │
                 │ iterator()                     │ iterator()
                 │                                │
        ┌────────▼────────────────────────────────▼─────────────────┐
        │                  Iterable<E>                               │
        │                                                            │
        │  ┌──────────────────────┐    ┌──────────────────────────┐│
        │  │   ArrayDef<E,A>      │    │ ReverseArrayWrapper<E>   ││
        │  │                      │    │                          ││
        │  │  + iterator()        │    │  - array: ArrayDef<E,?>  ││
        │  │  + reverseIterator() │◄───┤  + iterator()            ││
        │  └──────────┬───────────┘    │    (calls array.         ││
        │             │                │     reverseIterator())    ││
        │             │                └──────────────────────────┘│
        └─────────────┼─────────────────────────────────────────────┘
                      │
        ┌─────────────┴─────────────────────┐
        │   Concrete Implementations        │
        │                                   │
        │  ┌─────────────────────────────┐ │
        │  │   ArrayDynamic              │ │
        │  │   - elements: ArrayList     │ │
        │  │                             │ │
        │  │   iterator() {              │ │
        │  │     idx: 0 → n-1            │ │
        │  │   }                         │ │
        │  │   reverseIterator() {       │ │
        │  │     idx: n-1 → 0            │ │
        │  │   }                         │ │
        │  └─────────────────────────────┘ │
        │                                   │
        │  ┌─────────────────────────────┐ │
        │  │   ArrayFixed                │ │
        │  │   - elements: Object[]      │ │
        │  │                             │ │
        │  │   iterator() {              │ │
        │  │     idx: 0 → length-1       │ │
        │  │   }                         │ │
        │  │   reverseIterator() {       │ │
        │  │     idx: length-1 → 0       │ │
        │  │   }                         │ │
        │  └─────────────────────────────┘ │
        │                                   │
        │  ┌─────────────────────────────┐ │
        │  │   ArrayFixedByte            │ │
        │  │   - elements: byte[]        │ │
        │  │                             │ │
        │  │   iterator() {              │ │
        │  │     idx: 0 → length-1       │ │
        │  │   }                         │ │
        │  │   reverseIterator() {       │ │
        │  │     idx: length-1 → 0       │ │
        │  │   }                         │ │
        │  └─────────────────────────────┘ │
        │                                   │
        │  ┌─────────────────────────────┐ │
        │  │   ArrayFixedInt             │ │
        │  │   - elements: int[]         │ │
        │  │                             │ │
        │  │   iterator() {              │ │
        │  │     idx: 0 → length-1       │ │
        │  │   }                         │ │
        │  │   reverseIterator() {       │ │
        │  │     idx: length-1 → 0       │ │
        │  │   }                         │ │
        │  └─────────────────────────────┘ │
        └───────────────────────────────────┘
```

## Execution Flow

### Forward Iteration
```
EBS Script:
  foreach item in arr { print item; }

Flow:
  1. foreach evaluates arr → ArrayDef instance
  2. foreach calls arr.iterator()
  3. Iterator starts at index 0
  4. Loop: hasNext() && next()
     - Returns elements[0], elements[1], ..., elements[n-1]
  5. Prints: 1, 2, 3, 4, 5

┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐
│ 1 │→│ 2 │→│ 3 │→│ 4 │→│ 5 │
└───┘ └───┘ └───┘ └───┘ └───┘
  ↑                          
Start                      End
```

### Reverse Iteration
```
EBS Script:
  foreach item in #array.reverse(arr) { print item; }

Flow:
  1. foreach evaluates #array.reverse(arr)
  2. array.reverse() creates ReverseArrayWrapper(arr)
  3. foreach calls wrapper.iterator()
  4. Wrapper calls arr.reverseIterator()
  5. ReverseIterator starts at index n-1
  6. Loop: hasNext() && next()
     - Returns elements[n-1], elements[n-2], ..., elements[0]
  7. Prints: 5, 4, 3, 2, 1

┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐
│ 1 │←│ 2 │←│ 3 │←│ 4 │←│ 5 │
└───┘ └───┘ └───┘ └───┘ └───┘
                           ↑
                        Start  
End
```

## Iterator State Diagram

### Forward Iterator
```
State Machine:
  
  Initial: idx = 0
  
  ┌─────────┐
  │ Start   │
  │ idx=0   │
  └────┬────┘
       │
       ▼
  ┌─────────────┐
  │ hasNext()?  │───── false ──→ [End]
  └─────┬───────┘
        │ true
        ▼
  ┌─────────────┐
  │ next()      │
  │ return      │
  │ elem[idx++] │
  └─────┬───────┘
        │
        └──────────────────┐
                           │
                           ▼
                      [Loop back]
```

### Reverse Iterator
```
State Machine:
  
  Initial: idx = length - 1
  
  ┌──────────────┐
  │ Start        │
  │ idx=length-1 │
  └──────┬───────┘
         │
         ▼
  ┌─────────────┐
  │ hasNext()?  │───── false ──→ [End]
  │ (idx >= 0)  │
  └─────┬───────┘
        │ true
        ▼
  ┌─────────────┐
  │ next()      │
  │ return      │
  │ elem[idx--] │
  └─────┬───────┘
        │
        └──────────────────┐
                           │
                           ▼
                      [Loop back]
```

## Memory Layout

### Original Array
```
Array: [10, 20, 30, 40, 50]

Memory:
┌─────────────────────────────────────────┐
│ ArrayDynamic / ArrayFixed               │
│                                         │
│  metadata (type, size, etc.)            │
│  elements: [10][20][30][40][50]         │
│             ↑                           │
│             reference                   │
└─────────────────────────────────────────┘
```

### With ReverseArrayWrapper
```
After: #array.reverse(arr)

Memory:
┌─────────────────────────────────────────┐
│ ReverseArrayWrapper                     │
│                                         │
│  array: ──────────┐                     │
└───────────────────┼─────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│ ArrayDynamic / ArrayFixed (SAME!)       │
│                                         │
│  metadata (type, size, etc.)            │
│  elements: [10][20][30][40][50]         │
│             ↑                           │
│             reference (unchanged)       │
└─────────────────────────────────────────┘

Note: No array copying - only a wrapper object created!
Memory overhead: ~24 bytes (object header + reference)
```

## Performance Comparison

```
Array Size: n elements

┌──────────────────┬──────────┬──────────┬─────────────┐
│ Operation        │ Time     │ Memory   │ Notes       │
├──────────────────┼──────────┼──────────┼─────────────┤
│ Forward iterate  │ O(n)     │ O(1)     │ Native      │
│ Reverse iterate  │ O(n)     │ O(1)     │ Via wrapper │
│ Array copy       │ O(n)     │ O(n)     │ Wasteful    │
│ Manual loop      │ O(n)     │ O(1)     │ Less clean  │
└──────────────────┴──────────┴──────────┴─────────────┘

Winner: Reverse iterator (same performance, cleaner code)
```

## Example Trace

### Input Array
```
var numbers = [1, 2, 3]
```

### Forward Iteration Trace
```
foreach num in numbers {
  print num;
}

Execution Trace:
  iter = numbers.iterator()
  iter.idx = 0
  
  1. iter.hasNext() → true (0 < 3)
     iter.next() → elements[0] = 1
     print 1
     iter.idx = 1
  
  2. iter.hasNext() → true (1 < 3)
     iter.next() → elements[1] = 2
     print 2
     iter.idx = 2
  
  3. iter.hasNext() → true (2 < 3)
     iter.next() → elements[2] = 3
     print 3
     iter.idx = 3
  
  4. iter.hasNext() → false (3 >= 3)
     Loop ends

Output: 1, 2, 3
```

### Reverse Iteration Trace
```
foreach num in #array.reverse(numbers) {
  print num;
}

Execution Trace:
  wrapper = ReverseArrayWrapper(numbers)
  iter = wrapper.iterator() → numbers.reverseIterator()
  iter.idx = 2
  
  1. iter.hasNext() → true (2 >= 0)
     iter.next() → elements[2] = 3
     print 3
     iter.idx = 1
  
  2. iter.hasNext() → true (1 >= 0)
     iter.next() → elements[1] = 2
     print 2
     iter.idx = 0
  
  3. iter.hasNext() → true (0 >= 0)
     iter.next() → elements[0] = 1
     print 1
     iter.idx = -1
  
  4. iter.hasNext() → false (-1 < 0)
     Loop ends

Output: 3, 2, 1
```

## Benefits Summary

```
✅ Clean API          : #array.reverse(arr) is intuitive
✅ Type Safe          : Works with all ArrayDef implementations
✅ Efficient          : No array copying, O(1) memory overhead
✅ Compatible         : No changes to foreach or existing code
✅ Maintainable       : Simple iterator pattern
✅ Extensible         : Easy to add more iteration strategies
✅ Well Tested        : 12/12 tests passing
✅ Documented         : Complete documentation provided
```

## Related Patterns

This implementation follows several well-known design patterns:

1. **Iterator Pattern**: Provides sequential access without exposing structure
2. **Wrapper Pattern**: ReverseArrayWrapper wraps ArrayDef
3. **Strategy Pattern**: Different iteration strategies (forward/reverse)
4. **Template Method**: Common iteration logic in foreach, strategy varies
