# Class Size Analysis & Refactoring Recommendations

This document analyzes classes in the Maven Script Interpreter that have grown too large and provides refactoring recommendations.

## Summary

| Class | Lines | Methods | Category | Priority |
|-------|-------|---------|----------|----------|
| ScreenFactory.java | 3,193 | ~55 | Screen UI | HIGH |
| Builtins.java | 2,658 | ~135 cases | Built-in Functions | HIGH |
| Parser.java | 2,513 | ~74 | Parsing | MEDIUM |
| InterpreterScreen.java | 2,128 | ~22 | Screen Interpreter | MEDIUM |
| Interpreter.java | 1,974 | ~67 | Interpretation | MEDIUM |
| LogStructureProbe.java | 1,810 | N/A | Utility | LOW |
| BuiltinsScreen.java | 1,337 | N/A | Screen Builtins | LOW |
| Json.java | 1,280 | N/A | JSON Processing | LOW |
| EbsTab.java | 1,200 | N/A | UI | LOW |
| BuiltinsFile.java | 1,179 | N/A | File Builtins | LOW |

---

## Detailed Analysis

### 1. ScreenFactory.java (3,193 lines) - HIGHEST PRIORITY

**Current Responsibilities:**
- Screen creation and initialization
- Area and item creation
- Variable binding setup
- Validation handlers
- Control listener setup
- Schema validation
- Multi-record expansion
- Debug mode handling
- Menu bar creation

**Problems:**
- Single Responsibility Principle (SRP) violation - handles too many concerns
- Methods are tightly coupled
- Difficult to test individual components
- Hard to maintain and extend

**Recommended Refactoring: Extract Class / Strategy Pattern**

```
ScreenFactory.java (reduced to ~800 lines - orchestration only)
├── ScreenBuilder.java (~400 lines)
│   └── Creates and assembles screen components
├── VariableBindingManager.java (~500 lines)
│   └── Handles variable bindings and listeners
├── ValidationManager.java (~300 lines)
│   └── Schema validation and control validation
├── ControlListenerFactory.java (~400 lines)
│   └── Creates and attaches control listeners
├── MultiRecordExpander.java (~200 lines)
│   └── Handles multi-record item expansion
└── ScreenDebugger.java (~200 lines)
    └── Debug mode handling and logging
```

**Key Methods to Extract:**
- `setupVariableBinding()` → VariableBindingManager
- `setupValidationHandler()`, `attachValidationListener()` → ValidationManager
- `addControlListener()` → ControlListenerFactory
- `expandMultiRecordItems()`, `createExpandedItem()` → MultiRecordExpander
- `logNodeDebug()`, `logSceneGraph()`, debug mode methods → ScreenDebugger

---

### 2. Builtins.java (2,658 lines) - HIGH PRIORITY

**Current Responsibilities:**
- Registers all built-in function signatures
- Executes all built-in function logic via massive switch statement
- Contains 135+ case statements across categories:
  - JSON functions (20 cases)
  - String functions (22 cases)
  - Screen functions (22 cases)
  - File functions (24 cases)
  - HTTP functions (9 cases)
  - System functions (8 cases)
  - Debug functions (12 cases)
  - Array functions (5 cases)
  - AI functions (4 cases)
  - Other (2+ cases)

**Problems:**
- Massive switch statement (anti-pattern)
- Single class violates SRP - registration AND execution
- Adding new builtins requires modifying this class
- Difficult to test individual function groups

**Recommended Refactoring: Strategy Pattern + Registration System**

```
Builtins.java (reduced to ~300 lines - registry only)
├── BuiltinRegistry.java (~200 lines)
│   └── Manages registration and lookup
├── BuiltinHandler.java (interface)
│   └── Object handle(Environment env, InterpreterContext ctx, Object[] args)
├── handlers/
│   ├── JsonBuiltins.java (~400 lines) - Already partially exists
│   ├── StringBuiltins.java (~400 lines)
│   ├── ScreenBuiltins.java (~400 lines) - BuiltinsScreen already exists!
│   ├── FileBuiltins.java (~300 lines) - BuiltinsFile already exists!
│   ├── HttpBuiltins.java (~300 lines)
│   ├── SystemBuiltins.java (~200 lines)
│   ├── DebugBuiltins.java (~200 lines)
│   ├── ArrayBuiltins.java (~150 lines)
│   └── AiBuiltins.java (~150 lines)
```

**Implementation Approach:**
1. Create `BuiltinHandler` interface with `handle()` method
2. Move each category's case logic to dedicated handler class
3. Update `Builtins.java` to delegate to handlers instead of switch
4. Leverage existing `BuiltinsFile` and `BuiltinsScreen` patterns

**Note:** The project already has `BuiltinsScreen.java` and `BuiltinsFile.java` - follow this pattern for other categories.

---

### 3. Parser.java (2,513 lines) - MEDIUM PRIORITY

**Current Responsibilities:**
- Tokenization interface
- Statement parsing (20+ statement types)
- Expression parsing (15+ expression types)
- Type handling
- Error handling

**Problems:**
- All parsing logic in single class
- Long methods for complex statements
- Hard to extend with new language features

**Recommended Refactoring: Partial Extraction**

```
Parser.java (reduced to ~1,200 lines - core parsing)
├── StatementParser.java (~600 lines)
│   └── Screen statements, database statements, control flow
├── ExpressionParser.java (~400 lines)
│   └── Complex expression parsing (JSON literals, arrays)
└── TypeResolver.java (~100 lines)
    └── Type name to DataType conversion
```

**Alternatively, use inner classes to group related methods:**
- Keep as single file but organize with clear section comments
- Use private inner classes for complex statement types

---

### 4. InterpreterScreen.java (2,128 lines) - MEDIUM PRIORITY

**Current Responsibilities:**
- Screen statement execution
- Screen lifecycle (show/hide/close/submit)
- Variable set processing
- Display item handling
- Callback invocation

**Problems:**
- Variable set processing is complex and could be extracted
- Display item handling duplicates some ScreenFactory logic

**Recommended Refactoring: Extract Helper Classes**

```
InterpreterScreen.java (reduced to ~1,200 lines)
├── VarSetProcessor.java (~400 lines)
│   └── Process variable sets and variable lists
├── DisplayItemProcessor.java (~300 lines)
│   └── Handle display item storage and merging
└── ScreenCallbackManager.java (~200 lines)
    └── Manage and invoke screen callbacks
```

---

### 5. Interpreter.java (1,974 lines) - MEDIUM PRIORITY

**Current Responsibilities:**
- Statement visitor implementation
- Expression visitor implementation
- Operator evaluation
- Variable handling
- Delegation to sub-interpreters (screen, database, array)

**Current Strength:**
- Already delegates to `InterpreterScreen`, `InterpreterDatabase`, `InterpreterArray`
- Good use of composition pattern

**Recommended Refactoring: Complete Delegation**

```
Interpreter.java (reduced to ~1,000 lines)
├── InterpreterExpression.java (~500 lines) - NEW
│   └── Move expression evaluation logic
├── OperatorEvaluator.java (~400 lines) - NEW
│   └── Move evalOperator() and related methods
├── InterpreterScreen.java (existing)
├── InterpreterDatabase.java (existing)
└── InterpreterArray.java (existing)
```

**Key Methods to Extract:**
- `evalOperator()` → OperatorEvaluator
- Expression visitor methods → InterpreterExpression

---

## Lower Priority Classes (1,000-1,500 lines)

### 6. LogStructureProbe.java (1,810 lines) - LOW PRIORITY

Utility class for analyzing log structures. Size is acceptable for its specialized purpose.

**Recommendation:** Leave as-is unless it grows further or needs modification.

---

### 7. BuiltinsScreen.java (1,337 lines) - LOW PRIORITY

Already an extracted helper class following good patterns.

**Recommendation:** Could split into:
- `ScreenPropertyBuiltins.java` - property get/set operations
- `ScreenStatusBuiltins.java` - status management
- `ScreenNavigationBuiltins.java` - show/hide/close

---

### 8. Json.java (1,280 lines) - LOW PRIORITY

JSON parsing and manipulation utilities.

**Recommendation:** Could split into:
- `JsonParser.java` - parsing logic
- `JsonPath.java` - path navigation (get/set/remove)
- `JsonFormatter.java` - stringify/pretty-print

---

### 9. EbsTab.java (1,200 lines) - LOW PRIORITY

UI component for tab management.

**Recommendation:** Could extract:
- `TabMenuHandler.java` - context menu handling
- `TabFileOperations.java` - file open/save operations

---

### 10. BuiltinsFile.java (1,179 lines) - LOW PRIORITY

Already an extracted helper class following good patterns.

**Recommendation:** Leave as-is, size is manageable.

---

## Refactoring Priority Order

1. **Builtins.java** - Highest impact, establishes pattern for other extractions
2. **ScreenFactory.java** - Largest file, clear separation of concerns possible
3. **Parser.java** - Moderate complexity, partial extraction beneficial
4. **InterpreterScreen.java** - Helper extraction for variable set processing
5. **Interpreter.java** - Expression and operator evaluation extraction

---

## Implementation Guidelines

### When Extracting Classes:

1. **Preserve Existing Tests** - Ensure any existing tests continue to pass
2. **Use Package-Private Access** - New helper classes should be package-private when possible
3. **Favor Composition** - Use dependency injection for extracted components
4. **Incremental Approach** - Extract one component at a time, test after each extraction
5. **Follow Existing Patterns** - Use `BuiltinsScreen` and `BuiltinsFile` as templates

### Refactoring Patterns to Apply:

| Pattern | Apply To | Benefit |
|---------|----------|---------|
| Extract Class | All large classes | Single Responsibility |
| Strategy Pattern | Builtins | Remove switch statement |
| Composition | Interpreter | Better testability |
| Template Method | Parser statements | Reduce duplication |
| Factory Pattern | ScreenFactory | Isolate creation logic |

---

## Metrics After Proposed Refactoring

| Current Class | Lines | After Refactoring | New Classes |
|---------------|-------|-------------------|-------------|
| ScreenFactory.java | 3,193 | ~800 | +5 new classes |
| Builtins.java | 2,658 | ~300 | +8 new handler classes |
| Parser.java | 2,513 | ~1,200 | +2 new classes |
| InterpreterScreen.java | 2,128 | ~1,200 | +3 new classes |
| Interpreter.java | 1,974 | ~1,000 | +2 new classes |

**Target:** No class should exceed 1,500 lines; ideally under 1,000 lines.

---

## Conclusion

The largest classes (`ScreenFactory`, `Builtins`, `Parser`, `InterpreterScreen`, `Interpreter`) have grown beyond maintainable sizes. The recommended refactorings follow established patterns and the project's existing conventions (as seen with `BuiltinsScreen` and `BuiltinsFile`).

Key benefits of proposed refactoring:
- Improved maintainability
- Better testability (smaller units to test)
- Easier onboarding for new developers
- Clearer separation of concerns
- Reduced risk of merge conflicts
