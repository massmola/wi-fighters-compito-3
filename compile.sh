#!/bin/bash

# Create bin directory if it doesn't exist
mkdir -p bin

# Compile all Java files
javac -d bin -sourcepath src src/server/*.java src/client/*.java src/worker/*.java src/common/*.java

echo "Compilation finished."
