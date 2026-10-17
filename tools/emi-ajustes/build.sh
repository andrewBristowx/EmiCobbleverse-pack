#!/bin/bash
# Compila el mod. Necesita el jar de Alexandria SOLO para compilar: ALEXANDRIA_JAR=<Alexandria-1.0.3.3.2+1.21.1-fabric.jar>. Jar en build/libs/.
cd "$(dirname "$0")"
unset JAVA_TOOL_OPTIONS
: "${ALEXANDRIA_JAR:?define ALEXANDRIA_JAR}"
export ALEXANDRIA_JAR
gradle --no-daemon --offline build -q "$@"
