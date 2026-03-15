#!/bin/sh
# Gradle wrapper script
# Generated for terminal-buffer project

# Resolve script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$SCRIPT_DIR"

# Locate java
if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

# Execute Gradle wrapper
exec "$JAVACMD" \
  -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
  org.gradle.wrapper.GradleWrapperMain "$@"
