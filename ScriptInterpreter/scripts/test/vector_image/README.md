# Vector Image Builtin Test Scripts

This directory contains comprehensive test scripts for all EBS vector image builtins added in the SVG image loading fix PR.

## Test Scripts Overview

### 1. `test_vector_all.ebs` - Master Test Suite
**All-in-one test script** that validates all 25 vector image builtins in one execution.

**Coverage:**
- 3 loading/saving tests
- 5 information method tests
- 3 basic manipulation tests
- 3 color operation tests
- 6 filter effect tests
- 5 conversion method tests

**Total: 25 tests covering all vector image functionality**

Run with:
```bash
java -cp target/classes:$CLASSPATH com.eb.script.Run test_vector_all.ebs
```

### 2. `test_vector_loading.ebs` - Loading and Saving Tests
Tests: `vector.load`, `vector.save`, `vector.create`

**Test Coverage:**
- Test 1: Load SVG file
- Test 2: Save SVG file
- Test 3: Create vector image from bytes
- Test 4: Load multiple SVG files
- Test 5: Save to custom directory

**Total: 5 tests**

### 3. `test_vector_info.ebs` - Information Methods Tests
Tests: `vector.getwidth`, `vector.getheight`, `vector.getinfo`, `vector.getname`, `vector.setname`

**Test Coverage:**
- Test 1: Get width
- Test 2: Get height
- Test 3: Get info (JSON metadata)
- Test 4: Get name
- Test 5: Set name
- Test 6: Get dimensions from multiple pieces

**Total: 6 tests**

### 4. `test_vector_manipulations.ebs` - Basic Manipulation Tests
Tests: `vector.scale`, `vector.rotate`, `vector.setdimensions`

**Test Coverage:**
- Test 1: Scale uniformly (2x)
- Test 2: Scale non-uniformly (3x width, 1.5x height)
- Test 3: Rotate 45 degrees
- Test 4: Rotate 90 degrees
- Test 5: Set custom dimensions
- Test 6: Chain operations (scale + rotate)

**Total: 6 tests**

### 5. `test_vector_colors.ebs` - Color Operation Tests
Tests: `vector.setfillcolor`, `vector.setstrokecolor`, `vector.setstrokewidth`

**Test Coverage:**
- Test 1: Set fill color to red
- Test 2: Set fill color to blue
- Test 3: Set fill color to green
- Test 4: Set stroke color to purple
- Test 5: Set stroke width to 3.0
- Test 6: Combine fill and stroke colors
- Test 7: Create multiple color variations

**Total: 7 tests**

### 6. `test_vector_filters.ebs` - Filter Effects Tests
Tests: `vector.applyblur`, `vector.applydropshadow`, `vector.applygrayscale`, `vector.applysepia`, `vector.applybrightness`, `vector.applyhuerotate`

**Test Coverage:**
- Test 1: Apply blur filter (radius 2.5)
- Test 2: Apply drop shadow (dx=4, dy=4, blur=2)
- Test 3: Apply grayscale filter
- Test 4: Apply sepia filter
- Test 5: Apply brightness adjustment (darker, factor=0.5)
- Test 6: Apply brightness adjustment (brighter, factor=1.8)
- Test 7: Apply hue rotation (120 degrees)
- Test 8: Apply hue rotation (240 degrees)
- Test 9: Combine multiple filters (blur + shadow)

**Total: 9 tests**

### 7. `test_vector_conversions.ebs` - Conversion Methods Tests
Tests: `vector.toraster`, `vector.toimage`, `vector.tobytes`, `vector.tostring`

**Test Coverage:**
- Test 1: Convert to raster (default size)
- Test 2: Convert to raster (custom size 128x128)
- Test 3: Convert to raster (large size 512x512)
- Test 4: Use toimage() alias
- Test 5: Convert to bytes
- Test 6: Convert to SVG string
- Test 7: Rasterize filtered image
- Test 8: Bytes roundtrip (load → bytes → create)

**Total: 8 tests**

## Running the Tests

### Prerequisites
```bash
cd ScriptInterpreter
mvn clean compile
```

### Run Individual Test Suites
```bash
# Master test suite (all tests)
java -cp target/classes:$(mvn dependency:build-classpath -DincludeScope=runtime -Dmdep.outputFile=/dev/stdout -q) \
  com.eb.script.Run test_vector_all.ebs

# Individual test suites
java -cp target/classes:$CP com.eb.script.Run test_vector_loading.ebs
java -cp target/classes:$CP com.eb.script.Run test_vector_info.ebs
java -cp target/classes:$CP com.eb.script.Run test_vector_manipulations.ebs
java -cp target/classes:$CP com.eb.script.Run test_vector_colors.ebs
java -cp target/classes:$CP com.eb.script.Run test_vector_filters.ebs
java -cp target/classes:$CP com.eb.script.Run test_vector_conversions.ebs
```

### Quick Test (Master Suite)
```bash
cd ScriptInterpreter
CP=$(mvn dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q && cat /tmp/cp.txt)
java -cp "target/classes:$CP" com.eb.script.Run /path/to/test_vector_all.ebs
```

## Test Output

Each test script provides:
- ✓ PASS indicators for successful tests
- ✗ FAIL indicators with error messages for failed tests
- Test summary with pass/fail counts
- Overall result (ALL TESTS PASSED or SOME TESTS FAILED)

Example output:
```
========================================
  Test Summary
========================================
Total Tests:  6
Passed:       6
Failed:       0
Result:       ALL TESTS PASSED ✓
========================================
```

## Test Data

All tests use the chess piece SVG files located in:
```
src/main/resources/images/chess/
```

Available pieces:
- white_king.svg, white_queen.svg, white_rook.svg
- white_bishop.svg, white_knight.svg, white_pawn.svg
- black_king.svg, black_queen.svg, black_rook.svg
- black_bishop.svg, black_knight.svg, black_pawn.svg

## Output Files

Tests create various output files in `/tmp/` for verification:
- Manipulated SVG files (scaled, rotated, colored, filtered)
- Rasterized PNG images
- Test directories

## Complete Builtin Coverage

All 24 vector image builtins are tested:

**Loading & Saving (3):**
- vector.load, vector.save, vector.create

**Information (5):**
- vector.getwidth, vector.getheight, vector.getinfo, vector.getname, vector.setname

**Basic Manipulation (3):**
- vector.scale, vector.rotate, vector.setdimensions

**Color Operations (3):**
- vector.setfillcolor, vector.setstrokecolor, vector.setstrokewidth

**Filter Effects (6):**
- vector.applyblur, vector.applydropshadow, vector.applygrayscale
- vector.applysepia, vector.applybrightness, vector.applyhuerotate

**Conversions (4):**
- vector.toraster, vector.toimage (alias), vector.tobytes, vector.tostring

## Test Strategy

Tests follow these principles:
1. **Comprehensive**: Every builtin is tested
2. **Isolated**: Each test is independent
3. **Exception Handling**: All tests use try/exceptions blocks
4. **Verification**: Tests verify output/results where possible
5. **Real Data**: Uses actual chess piece SVG files
6. **Chained Operations**: Tests combinations of operations
7. **Edge Cases**: Tests various parameter combinations

## Success Criteria

- All test scripts should show 100% pass rate
- No exceptions should be thrown during normal operation
- Output files should be created successfully
- Conversions should maintain data integrity
- Filters should produce valid SVG output
