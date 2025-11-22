# Class Tree Lister

A utility to analyze and display the class hierarchy tree structure of Java source files in the maven-script-interpreter project.

## Features

- Scans Java source files and extracts class information
- Displays class hierarchy organized by package
- Shows inheritance relationships (extends/implements)
- Identifies abstract classes, interfaces, and enums
- Provides statistics on the codebase structure

## Usage

### Using Maven

```bash
cd ScriptInterpreter
mvn compile
java -cp target/classes com.eb.util.ClassTreeLister [source-directory]
```

If no source directory is specified, it defaults to `src/main/java`.

### Example

```bash
cd ScriptInterpreter
mvn compile
java -cp target/classes com.eb.util.ClassTreeLister src/main/java
```

## Output Format

The utility produces a hierarchical tree structure showing:
- Package organization
- Class types (with special markers for interfaces, enums, and abstract classes)
- Inheritance relationships
- Interface implementations
- Statistics summary

### Output Example

```
================================================================================
CLASS HIERARCHY TREE
================================================================================

Package: com.eb.script.arrays
--------------------------------------------------------------------------------
«interface» ArrayDef (extends Iterable)
  └─ ArrayFixedByte (implements ArrayDef)
  └─ ArrayFixed (implements ArrayDef)
  └─ ArrayDynamic (implements ArrayDef)

...

================================================================================
STATISTICS
================================================================================
Total classes: 82
Total interfaces: 9
Total enums: 5
Abstract classes: 3
```

## Implementation Details

The `ClassTreeLister` class:
1. Walks through the source directory recursively
2. Parses each Java file to extract class declarations
3. Handles generics, comments, and various class modifiers
4. Builds a hierarchical map of class relationships
5. Prints the tree structure organized by package

## Requirements

- Java 21 or higher
- Maven 3.x
- Source code must be valid Java syntax
