#!/bin/sh
# Minimal Gradle wrapper starter for Inkora. Requires JDK 17+ and Android SDK.
# If gradle-wrapper.jar is missing, use: gradle wrapper --gradle-version 8.7
exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
