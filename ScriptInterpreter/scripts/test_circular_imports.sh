#!/bin/bash
# test_circular_imports.sh
# Automated test runner for circular import detection feature
# This script runs all test cases and reports results

set -e

echo "=========================================="
echo "Circular Import Detection Test Runner"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run a test that should succeed
run_success_test() {
    local test_name="$1"
    local test_file="$2"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo "Running: $test_name"
    
    if java -cp target/classes com.eb.script.Run "scripts/$test_file" > /tmp/test_output.txt 2>&1; then
        echo -e "${GREEN}[PASS]${NC} $test_name"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        echo -e "${RED}[FAIL]${NC} $test_name"
        echo "  Error output:"
        cat /tmp/test_output.txt | sed 's/^/  /'
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Function to run a test that should fail with circular import error
run_failure_test() {
    local test_name="$1"
    local test_file="$2"
    local expected_error="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo "Running: $test_name"
    
    if java -cp target/classes com.eb.script.Run "scripts/$test_file" > /tmp/test_output.txt 2>&1; then
        echo -e "${RED}[FAIL]${NC} $test_name - Should have failed but succeeded"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    else
        # Check if the error message contains the expected text
        if grep -q "$expected_error" /tmp/test_output.txt; then
            echo -e "${GREEN}[PASS]${NC} $test_name - Failed with expected error"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            return 0
        else
            echo -e "${RED}[FAIL]${NC} $test_name - Failed with unexpected error"
            echo "  Expected: $expected_error"
            echo "  Actual error:"
            cat /tmp/test_output.txt | sed 's/^/  /'
            FAILED_TESTS=$((FAILED_TESTS + 1))
            return 1
        fi
    fi
}

echo "Building project..."
mvn -q clean compile
if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}Build successful${NC}"
echo ""

echo "=========================================="
echo "Running Test Suite"
echo "=========================================="
echo ""

# Test 1: Valid import should work
run_success_test "Test 1: Valid Import" "test_valid_import.ebs"
echo ""

# Test 2: Multiple non-circular imports should work
run_success_test "Test 2: Multiple Non-Circular Imports" "test_multiple_import.ebs"
echo ""

# Test 3: Direct circular import should be detected
run_failure_test "Test 3: Direct Circular Import (A->B->A)" "test_circular_direct.ebs" "Circular import detected"
echo ""

# Test 4: Indirect circular import should be detected
run_failure_test "Test 4: Indirect Circular Import (A->B->C->A)" "test_circular.ebs" "Circular import detected"
echo ""

# Test 5: Comprehensive test suite
run_success_test "Test 5: Comprehensive Test Suite" "test_circular_imports_comprehensive.ebs"
echo ""

# Print summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi
