#!/bin/sh
set -e

# Find Java
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Find Gradle wrapper jar
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"

# Run Gradle
exec "$JAVACMD" -jar "$WRAPPER_JAR" "$@"
