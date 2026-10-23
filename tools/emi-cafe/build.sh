#!/bin/bash
# Compila el mod. Necesita los jars de Cobblemon y de Kotlin SOLO para compilar:
#   COBBLEMON_JAR=<Cobblemon-fabric-1.7.3+1.21.1.jar>  KOTLIN_JAR=<kotlin-stdlib-*.jar (dentro de fabric-language-kotlin/META-INF/jars)>
cd "$(dirname "$0")"
: "${COBBLEMON_JAR:?define COBBLEMON_JAR}" "${KOTLIN_JAR:?define KOTLIN_JAR}"
export COBBLEMON_JAR KOTLIN_JAR
gradle --no-daemon --offline build -q "$@"
