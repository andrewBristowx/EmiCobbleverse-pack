#!/bin/bash
# Prueba rapida de la logica pura (sin Minecraft): compila los archivos puros + la prueba y la ejecuta.
#   GSON_JAR=<gson-2.x.jar> ./probar.sh      (si no se da, busca uno en la cache de Gradle)
cd "$(dirname "$0")"
GSON_JAR=${GSON_JAR:-$(find "$HOME/.gradle" -name 'gson-2*.jar' 2>/dev/null | head -1)}
: "${GSON_JAR:?no encuentro gson*.jar; define GSON_JAR}"
OUT=$(mktemp -d)
SRC=../src/main/java/emi/survivors
javac -encoding UTF-8 -cp "$GSON_JAR" -d "$OUT" $SRC/Curvas.java $SRC/Stats.java $SRC/Catalogo.java $SRC/Cartas.java $SRC/Meta.java $SRC/Datos.java $SRC/Grupos.java $SRC/Objeto.java $SRC/Jefes.java $SRC/Mareas.java $SRC/Pers.java $SRC/Biomas.java $SRC/Periodos.java $SRC/Retos.java $SRC/Tienda.java $SRC/Clima.java $SRC/Grietas.java PruebaLogica.java || exit 1
java -cp "$OUT:$GSON_JAR" emi.survivors.PruebaLogica
code=$?
rm -rf "$OUT"
exit $code
