#!/bin/bash
# Compila el mod. Necesita el jar de Universal Graves SOLO para compilar (en el servidor ya esta instalado):
#   GRAVES_JAR=<graves-3.4.4+1.21.jar>
# La primera vez baja Fantasy de https://maven.nucleoid.xyz (despues funciona sin conexion con --offline).
cd "$(dirname "$0")"
: "${GRAVES_JAR:?define GRAVES_JAR}"
export GRAVES_JAR
gradle --no-daemon build -q "$@"
