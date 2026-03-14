#!/bin/bash

# CodeGuard Installation Script
# Installs CodeGuard globally for easy access

set -e  # Exit on error

INSTALL_DIR="$HOME/.local/bin"
JAR_NAME="codeguard.jar"
SCRIPT_NAME="codeguard"

echo "🚀 Installing CodeGuard..."

# Check if JAR exists
if [ ! -f "$JAR_NAME" ]; then
    echo "❌ Error: $JAR_NAME not found!"
    echo "Please run ./build.sh first to build the JAR"
    exit 1
fi

# Create install directory if it doesn't exist
mkdir -p "$INSTALL_DIR"

# Copy JAR to install location
echo "📦 Copying JAR to $INSTALL_DIR..."
cp "$JAR_NAME" "$INSTALL_DIR/$JAR_NAME"

# Create wrapper script
echo "📝 Creating wrapper script..."
cat > "$INSTALL_DIR/$SCRIPT_NAME" << 'EOF'
#!/bin/bash
# CodeGuard wrapper script
java -jar "$HOME/.local/bin/codeguard.jar" "$@"
EOF

# Make script executable
chmod +x "$INSTALL_DIR/$SCRIPT_NAME"

echo "✅ Installation complete!"
echo ""
echo "CodeGuard installed to: $INSTALL_DIR"
echo ""

# Check if directory is in PATH
if [[ ":$PATH:" == *":$INSTALL_DIR:"* ]]; then
    echo "✅ $INSTALL_DIR is already in your PATH"
    echo ""
    echo "You can now run: codeguard <file-or-directory>"
else
    echo "⚠️  $INSTALL_DIR is not in your PATH"
    echo ""
    echo "Add this line to your ~/.bashrc or ~/.zshrc:"
    echo "  export PATH=\"\$HOME/.local/bin:\$PATH\""
    echo ""
    echo "Then reload your shell:"
    echo "  source ~/.bashrc  # or source ~/.zshrc"
    echo ""
    echo "After that, you can run: codeguard <file-or-directory>"
fi

echo ""
echo "📖 Usage Examples:"
echo "  codeguard MyFile.java          # Scan single file"
echo "  codeguard src/                 # Scan directory"
echo "  codeguard --all src/           # Show all issues"
