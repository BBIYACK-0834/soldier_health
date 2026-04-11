#!/usr/bin/env bash
set -euo pipefail

# Generates Gradle Wrapper files for backend without requiring a pre-existing wrapper jar in git.
# Usage:
#   cd backend
#   ./scripts/bootstrap-wrapper.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TMP_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

cat > "$TMP_DIR/settings.gradle" <<'SETTINGS'
rootProject.name = 'wrapper-bootstrap'
SETTINGS

cat > "$TMP_DIR/build.gradle" <<'BUILD'
BUILD

(
  cd "$TMP_DIR"
  gradle wrapper --gradle-version 8.10.2 --distribution-type bin --no-validate-url --no-daemon
)

cp "$TMP_DIR/gradlew" "$BACKEND_DIR/gradlew"
cp "$TMP_DIR/gradlew.bat" "$BACKEND_DIR/gradlew.bat"
mkdir -p "$BACKEND_DIR/gradle/wrapper"
cp "$TMP_DIR/gradle/wrapper/gradle-wrapper.jar" "$BACKEND_DIR/gradle/wrapper/gradle-wrapper.jar"
cp "$TMP_DIR/gradle/wrapper/gradle-wrapper.properties" "$BACKEND_DIR/gradle/wrapper/gradle-wrapper.properties"

chmod +x "$BACKEND_DIR/gradlew"

echo "[OK] backend Gradle Wrapper files generated."
echo "Run: cd backend && ./gradlew bootRun"
