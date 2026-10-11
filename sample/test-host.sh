#!/bin/bash
set -euo pipefail

# sample/test-host.sh: Offline host-side compiler and test runner for DiagnosticsFormatter

SCRIPT_DIR="$(cd -P "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
ROOT_DIR="$(cd -P "$SCRIPT_DIR/.." && pwd -P)"

echo "=== Open-PMCA Sample Host Formatter Test ==="
echo "Root directory: $ROOT_DIR"

# 1. Resolve JDK 17 / Java compiler: JAVAC -> JAVA_HOME/bin/javac -> java_home -v 17 -> PATH javac
JAVAC_BIN="${JAVAC:-}"
JAVA_BIN="${JAVA:-}"

if [ -z "$JAVAC_BIN" ] && [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC_BIN="$JAVA_HOME/bin/javac"
    [ -z "$JAVA_BIN" ] && [ -x "$JAVA_HOME/bin/java" ] && JAVA_BIN="$JAVA_HOME/bin/java"
fi

# On macOS, detect JDK 17 if default PATH is not JDK 17
if [ -z "$JAVAC_BIN" ] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
    JDK17_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
    if [ -n "$JDK17_HOME" ] && [ -x "$JDK17_HOME/bin/javac" ]; then
        JAVAC_BIN="$JDK17_HOME/bin/javac"
        [ -z "$JAVA_BIN" ] && [ -x "$JDK17_HOME/bin/java" ] && JAVA_BIN="$JDK17_HOME/bin/java"
    fi
fi

if [ -z "$JAVAC_BIN" ]; then
    JAVAC_BIN="$(command -v javac 2>/dev/null || true)"
fi

if [ -z "$JAVA_BIN" ]; then
    JAVA_BIN="$(command -v java 2>/dev/null || true)"
fi

if [ -z "$JAVAC_BIN" ] || [ ! -x "$JAVAC_BIN" ]; then
    echo "ERROR: Java compiler (javac) not found. Please set JAVA_HOME or JAVAC to JDK 17." >&2
    exit 1
fi

if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
    echo "ERROR: Java runtime (java) not found. Please set JAVA_HOME or JAVA to JDK 17." >&2
    exit 1
fi

echo "Java compiler: $JAVAC_BIN"
echo "Java runtime:  $JAVA_BIN"

# Check compiler support for Java 7 class output (--release 7)
COMPILE_OPTS=()
if "$JAVAC_BIN" --release 7 -help >/dev/null 2>&1; then
    COMPILE_OPTS=(--release 7)
else
    echo "ERROR: Compiler '$JAVAC_BIN' does not support '--release 7'." >&2
    echo "JDK 21+ removed support for compiling to Java 7/class version 51." >&2
    echo "Please configure JDK 17 (e.g. JAVA_HOME=\$(/usr/libexec/java_home -v 17) or set JAVAC)." >&2
    exit 1
fi

# Locate built framework JAR
FRAMEWORK_JAR="${FRAMEWORK_JAR:-}"
if [ -z "$FRAMEWORK_JAR" ]; then
    # Look for built JAR in framework/build/libs/
    for jar in "$ROOT_DIR"/framework/build/libs/*.jar; do
        if [ -f "$jar" ]; then
            FRAMEWORK_JAR="$jar"
            break
        fi
    done
fi

if [ -z "$FRAMEWORK_JAR" ] || [ ! -f "$FRAMEWORK_JAR" ]; then
    echo "ERROR: Framework JAR not found." >&2
    echo "Please build the core framework first using: ./gradlew :framework:jar" >&2
    echo "Or supply FRAMEWORK_JAR=/path/to/framework.jar $0" >&2
    exit 1
fi

echo "Using Framework JAR: $FRAMEWORK_JAR"

BUILD_DIR="$SCRIPT_DIR/build/host-test-classes"
rm -rf "$BUILD_DIR"
FIXTURE_DIR="$BUILD_DIR/fixtures"
mkdir -p "$FIXTURE_DIR/android" "$FIXTURE_DIR/scalar"

echo "Compiling isolated runtime fixtures with ${COMPILE_OPTS[*]}..."
"$JAVAC_BIN" "${COMPILE_OPTS[@]}" -d "$FIXTURE_DIR/android" \
    "$SCRIPT_DIR/src/test/fixtures/android/android/os/Build.java"
"$JAVAC_BIN" "${COMPILE_OPTS[@]}" -d "$FIXTURE_DIR/scalar" \
    "$SCRIPT_DIR/src/test/fixtures/scalar/com/sony/scalar/sysutil/ScalarProperties.java"

echo "Compiling DiagnosticsFormatter and Host Test with ${COMPILE_OPTS[*]}..."
"$JAVAC_BIN" "${COMPILE_OPTS[@]}" -cp "$FRAMEWORK_JAR" -d "$BUILD_DIR" \
    "$SCRIPT_DIR/src/main/java/org/openpmca/sample/DiagnosticsFormatter.java" \
    "$SCRIPT_DIR/src/test/java/org/openpmca/framework/RuntimeInspectionTestAccess.java" \
    "$SCRIPT_DIR/src/test/java/org/openpmca/sample/DiagnosticsFormatterHostTest.java"

echo "Executing DiagnosticsFormatterHostTest..."
"$JAVA_BIN" -cp "$FRAMEWORK_JAR:$BUILD_DIR" org.openpmca.sample.DiagnosticsFormatterHostTest "$FIXTURE_DIR"

echo "SUCCESS: Host formatter tests passed."
