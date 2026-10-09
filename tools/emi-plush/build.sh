#!/usr/bin/env bash
# Compila emi-plush contra los jars reales (nombres intermediary). Variables: MC (client-intermediary.jar), GECKO, POKEBLOCKS,
# FAPI (carpeta con los jars de Fabric API ya extraidos), LIBS (classpath de librerias de Minecraft), OUT (jar de salida).
set -euo pipefail
cd "$(dirname "$0")"
: "${MC:?}" "${GECKO:?}" "${POKEBLOCKS:?}" "${FAPI:?}" "${LIBS:?}" "${OUT:?}"
unset JAVA_TOOL_OPTIONS
rm -rf build && mkdir -p build/classes
CP="$MC:$GECKO:$POKEBLOCKS:$(ls $FAPI/*.jar | tr '\n' ':')$LIBS"
javac --release 21 -encoding UTF-8 -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
python3 gen/gen_michi.py resources
python3 gen/gen_pokemon.py resources
[ -n "${COBBLEMON_JAR:-}" ] && (cd gen && python3 gen_emi_poke.py ../resources) || echo "(sin COBBLEMON_JAR: se usan las texturas y modelos Emi ya generados)"
cp -r resources/* build/classes/
(cd build/classes && rm -f "$OUT" && zip -qr "$OUT" .)
echo "jar: $OUT"
