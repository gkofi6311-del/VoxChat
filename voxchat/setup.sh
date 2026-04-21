#!/usr/bin/env bash
# Run this once to download the Gradle wrapper JAR before building.
# After this, use ./gradlew build normally.
set -e
WRAPPER_DIR="gradle/wrapper"
JAR_PATH="$WRAPPER_DIR/gradle-wrapper.jar"

if [ -f "$JAR_PATH" ]; then
  echo "gradle-wrapper.jar already present."
  exit 0
fi

echo "Downloading gradle-wrapper.jar …"
mkdir -p "$WRAPPER_DIR"

# Try curl first, then wget
if command -v curl &>/dev/null; then
  curl -fsSL \
    "https://github.com/gradle/gradle/raw/v8.8.0/gradle/wrapper/gradle-wrapper.jar" \
    -o "$JAR_PATH"
elif command -v wget &>/dev/null; then
  wget -q \
    "https://github.com/gradle/gradle/raw/v8.8.0/gradle/wrapper/gradle-wrapper.jar" \
    -O "$JAR_PATH"
else
  echo "ERROR: Neither curl nor wget found. Please install one and retry."
  exit 1
fi

echo "Done. You can now run: ./gradlew build"
