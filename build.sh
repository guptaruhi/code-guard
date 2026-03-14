#!/bin/bash

# CodeGuard Build Script
# Compiles and packages CodeGuard into an executable JAR

set -e  # Exit on error

echo "🔨 Building CodeGuard..."

# Clean previous build
echo "🧹 Cleaning previous build..."
rm -rf build/
rm -f codeguard.jar

# Create build directory
mkdir -p build

# Compile Java files
echo "📦 Compiling Java sources..."
javac -d build analyzer/CodeGuard.java

# Create JAR
echo "📦 Creating JAR file..."
jar cfm codeguard.jar MANIFEST.MF -C build .

# Make executable (optional)
chmod +x codeguard.jar

echo "✅ Build complete!"
echo ""
echo "📦 codeguard.jar created successfully"
echo ""
echo "Usage:"
echo "  java -jar codeguard.jar <file-or-directory>"
echo "  java -jar codeguard.jar --all src/"
echo ""
echo "Or run install.sh to install globally"
