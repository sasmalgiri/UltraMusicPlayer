#!/bin/bash
# Setup script for SoundTouch library
# Run this before building the native code

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SOUNDTOUCH_DIR="$SCRIPT_DIR/soundtouch"
SOUNDTOUCH_VERSION="2.3.2"
SOUNDTOUCH_URL="https://codeberg.org/soundtouch/soundtouch/archive/refs/tags/${SOUNDTOUCH_VERSION}.tar.gz"

echo "=========================================="
echo "UltraMusic - SoundTouch Setup"
echo "=========================================="

# Check if already downloaded
if [ -f "$SOUNDTOUCH_DIR/SoundTouch.cpp" ]; then
    echo "✅ SoundTouch source already present"
    exit 0
fi

echo "📥 Downloading SoundTouch ${SOUNDTOUCH_VERSION}..."

# Create temp directory
TEMP_DIR=$(mktemp -d)
cd "$TEMP_DIR" || exit 1

# Download
if command -v curl &> /dev/null; then
    curl -L -o soundtouch.tar.gz "$SOUNDTOUCH_URL"
elif command -v wget &> /dev/null; then
    wget -O soundtouch.tar.gz "$SOUNDTOUCH_URL"
else
    echo "❌ Error: curl or wget required"
    exit 1
fi

# Extract
echo "📦 Extracting..."
tar -xzf soundtouch.tar.gz

# Find source directory
SRC_DIR=$(find . -name "SoundTouch.cpp" -type f | head -1 | xargs dirname)

if [ -z "$SRC_DIR" ]; then
    echo "❌ Error: Could not find SoundTouch source"
    exit 1
fi

# Copy source files
echo "📁 Copying source files to $SOUNDTOUCH_DIR..."
mkdir -p "$SOUNDTOUCH_DIR"

# Copy all .cpp and .h files
cp "$SRC_DIR"/*.cpp "$SOUNDTOUCH_DIR/" 2>/dev/null
cp "$SRC_DIR"/*.h "$SOUNDTOUCH_DIR/" 2>/dev/null

# Also copy from include directory if present
if [ -d "${SRC_DIR}/../include" ]; then
    cp "${SRC_DIR}/../include"/*.h "$SOUNDTOUCH_DIR/" 2>/dev/null
fi

# Cleanup
cd /
rm -rf "$TEMP_DIR"

# Verify
if [ -f "$SOUNDTOUCH_DIR/SoundTouch.cpp" ]; then
    echo "✅ SoundTouch setup complete!"
    echo ""
    echo "Files installed:"
    ls -la "$SOUNDTOUCH_DIR"/*.cpp 2>/dev/null | wc -l
    echo " .cpp files"
    ls -la "$SOUNDTOUCH_DIR"/*.h 2>/dev/null | wc -l  
    echo " .h files"
else
    echo "❌ Setup failed - SoundTouch.cpp not found"
    exit 1
fi

echo ""
echo "=========================================="
echo "Now you can build the project!"
echo "=========================================="
