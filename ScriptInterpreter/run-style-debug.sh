#!/bin/bash
# Script to run the StyleDebugTest

cd "$(dirname "$0")"
java -cp target/classes com.eb.ui.StyleDebugTest
