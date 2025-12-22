# EBS Script Packaging Guide

## Overview

The EBS Script Packaging feature allows you to package EBS scripts into binary `.ebsp` (EBS Package) files. This enables you to distribute EBS applications without exposing the source code, protecting your intellectual property while maintaining full functionality.

## How It Works

The packaging system:
1. **Parses** your EBS script into an Abstract Syntax Tree (AST)
2. **Serializes** the AST using Java serialization
3. **Compresses** the binary data using GZIP compression
4. **Encodes** the compressed data using Base64 encoding
5. **Adds** a version header comment
6. **Saves** the result as a text-based `.ebsp` file

The `.ebsp` file format:
```
// packaged esb language ver X.Y.Z.W
<base64-encoded compressed bytecode>
```

When you run a `.ebsp` file, the interpreter automatically:
1. Detects the `.ebsp` extension
2. Reads and validates the version header
3. Decodes the Base64 content
4. Deserializes and decompresses the AST
5. Executes the pre-parsed code directly (no re-parsing needed)

### Benefits

- **Source Code Protection**: The original source code is not stored in the package
- **Version Tracking**: Header comment shows which EBS version created the package
- **Transfer-Friendly**: Base64 encoding makes files safe for text-based protocols
- **Obfuscation**: Serialization and compression make reverse engineering difficult
- **Fast Loading**: Pre-parsed AST loads faster than parsing source code
- **Compression**: GZIP compression creates compact packages
- **Seamless Execution**: `.ebsp` files run exactly like `.ebs` files
- **Text-Based**: Files can be opened in text editors (though content is encoded)

## Usage

### Method 1: Command-Line Packager

#### Basic Usage

Package a single script:
```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.package_tool.EbsPackager myapp.ebs
```

This creates `myapp.ebsp` in the same directory.

#### Specify Output File

```bash
java -cp target/classes com.eb.script.package_tool.EbsPackager myapp.ebs -o myapp-v1.0.ebsp
```

#### Package Script with Dependencies

If your script imports other files, make sure to package the main script (the one that contains the imports):

```bash
java -cp target/classes com.eb.script.package_tool.EbsPackager main.ebs -o myapp.ebsp
```

The import statements in the packaged script will still work, as long as the imported files are available at runtime in their expected locations.

#### Help Command

```bash
java -cp target/classes com.eb.script.package_tool.EbsPackager --help
```

### Method 2: Console Command

From the EBS interactive console, use the `/package` command:

```
/package myapp.ebs
/package myapp.ebs myapp-v1.0.ebsp
```

The console will display:
- Parsing status
- Package creation status
- Original file size
- Packaged file size
- Compression ratio

### Method 3: Programmatic Packaging

You can also package scripts programmatically in your Java code:

```java
import com.eb.script.RuntimeContext;
import com.eb.script.parser.Parser;
import com.eb.script.package_tool.RuntimeContextSerializer;
import java.nio.file.Path;

// Parse the script
RuntimeContext context = Parser.parse(Path.of("myapp.ebs"));

// Package it
RuntimeContextSerializer.serialize(context, Path.of("myapp.ebsp"));
```

## Running Packaged Scripts

### Command-Line Execution

Run a packaged script exactly like a regular `.ebs` script:

```bash
cd ScriptInterpreter
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="myapp.ebsp"
```

Or with the compiled class path:
```bash
java -cp target/classes com.eb.script.Run myapp.ebsp
```

### From Interactive Console

Load and execute a packaged script from the console by running it as you would any script. The console automatically detects `.ebsp` files and loads them appropriately.

### From JavaFX Application

The JavaFX application supports opening and running `.ebsp` files through the file menu, just like regular `.ebs` files.

## Example Workflow

### 1. Create Your Application

Create `calculator.ebs`:
```javascript
// Calculator application
add(a: int, b: int) return int {
    return a + b;
}

multiply(a: int, b: int) return int {
    return a * b;
}

// Main execution
var x: int = 5;
var y: int = 10;
var sum: int = call add(x, y);
var product: int = call multiply(x, y);

print "Sum: " + sum;
print "Product: " + product;
```

### 2. Package the Application

```bash
cd ScriptInterpreter
java -cp target/classes com.eb.script.package_tool.EbsPackager calculator.ebs
```

Output:
```
Parsing: calculator.ebs
Packaging to: calculator.ebsp
Original size: 312 bytes
Packaged size: 1847 bytes
Size increase: 492.6%
Successfully packaged to: calculator.ebsp
```

**Note**: The packaged size is typically larger than the original source because it includes the full serialized AST structure with type information, line numbers, and parsed expressions. This is a trade-off for source code protection and faster loading (no parsing required).

### 3. Distribute the Package

Share `calculator.ebsp` with users. They can run it without seeing your source code:

```bash
mvn exec:java -Dexec.mainClass="com.eb.script.Run" -Dexec.args="calculator.ebsp"
```

Output:
```
Sum: 15
Product: 50
```

### 4. Verify Source Protection

Try to view the file:
```bash
cat calculator.ebsp
# Shows binary gibberish - source code is not readable

strings calculator.ebsp
# Shows no meaningful source code
```

## Multi-File Applications

For applications with multiple script files, you have two options:

### Option 1: Package Main Script with Imports (Recommended)

Keep your modular structure with separate `.ebs` files:

**lib/utils.ebs**:
```javascript
formatNumber(n: int) return string {
    return "Number: " + n;
}
```

**main.ebs**:
```javascript
import "lib/utils.ebs";

var result: string = call formatNumber(42);
print result;
```

Package the main script:
```bash
java -cp target/classes com.eb.script.package_tool.EbsPackager main.ebs -o myapp.ebsp
```

When distributing:
- Include `myapp.ebsp` (packaged)
- Include `lib/utils.ebs` (or package it separately as `lib/utils.ebsp`)

The import statement will still work, loading either `.ebs` or `.ebsp` files.

### Option 2: Combine Into Single Script

Alternatively, manually combine all scripts into one file before packaging:
1. Copy all function definitions into a single file
2. Remove import statements
3. Package the combined script

This creates a truly self-contained package but loses modularity.

## Technical Details

### What Gets Packaged?

The `.ebsp` file contains:
- **Parsed AST**: Complete syntax tree of your code
- **Function definitions**: All function blocks with parameters and return types
- **Statements**: All executable statements
- **Exception handlers**: Try-exceptions blocks
- **Type information**: Variable types, casts, and type definitions

### What Doesn't Get Packaged?

The `.ebsp` file does NOT contain:
- **Original source code**: The text source is not stored
- **Comments**: Comments are removed during parsing
- **Whitespace**: Formatting is not preserved
- **Environment state**: Runtime variables and connections are not saved
- **Source file path**: The original file location (but the name is kept)

### Security Considerations

**Protection Level**: The packaging provides basic obfuscation through:
- Binary serialization (not human-readable)
- GZIP compression (further obscures content)
- AST representation (no direct source code)

**Not Cryptographically Secure**: This is NOT encryption. Determined individuals with Java knowledge could potentially:
- Deserialize the AST
- Reconstruct the logic
- Reverse engineer the algorithm

**Best Use Cases**:
- Protecting source code from casual viewing
- Distributing commercial applications to end users
- Preventing simple copy-paste of code
- Maintaining proprietary business logic

**Not Recommended For**:
- High-security applications requiring encryption
- Protecting cryptographic keys or passwords (store these externally)
- Applications where algorithm secrecy is critical to security

### File Format

`.ebsp` files use the following structure:
```
[GZIP Header]
[Java Serialization Stream]
  - RuntimeContext object
    - Script name
    - Function blocks (Map<String, BlockStatement>)
    - Statements (Statement[])
```

The GZIP wrapper provides:
- Compression (reduces file size)
- Binary encoding (obscures content)
- Standard format (can be verified with `file` command)

## Troubleshooting

### Package Size Larger Than Source

This is expected. The serialized AST includes:
- Full parse tree structure
- Type information
- Line numbers
- Java object overhead

Trade-off: Larger file size for source code protection and faster loading.

### Import Errors When Running Package

If you get "Import file not found" errors:
1. Ensure imported files are in the correct relative paths
2. Package imported files as `.ebsp` as well
3. Consider combining scripts into a single file

### NotSerializableException During Packaging

If you see serialization errors:
1. Report this as a bug - all AST classes should be Serializable
2. Check if you're using custom extensions
3. Verify you're using the latest version

### Package Won't Run

If the packaged script fails to execute:
1. Verify the original `.ebs` script runs correctly first
2. Check that all dependencies (imports, files) are available
3. Ensure you're using the same Java version for packaging and running
4. Try re-packaging with the latest version

## Best Practices

1. **Test Before Packaging**: Always run your `.ebs` script successfully before packaging
2. **Version Your Packages**: Include version numbers in package filenames (`myapp-v1.0.ebsp`)
3. **Package Once, Run Anywhere**: Packaged scripts are portable across systems with EBS installed
4. **Keep Source Backups**: Always maintain the original `.ebs` source files
5. **Document Dependencies**: Clearly document any required external files or imports
6. **Use Meaningful Names**: Name your packages descriptively for easy identification

## Future Enhancements

Planned features for future releases:
- **Encryption**: Optional AES encryption for enhanced security
- **Multi-script bundling**: Package multiple scripts into a single archive
- **Metadata**: Include version, author, and description in packages
- **Digital signatures**: Sign packages to verify authenticity
- **Selective packaging**: Choose which functions to include

## Summary

EBS script packaging provides an effective way to distribute your applications while protecting source code. The combination of binary serialization and GZIP compression creates packages that are:
- Not human-readable
- Difficult to reverse engineer
- Fully functional
- Easy to distribute

Use packaging for commercial applications, proprietary tools, and any scenario where you want to share functionality without exposing implementation details.
