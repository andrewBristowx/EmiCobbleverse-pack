#!/usr/bin/env bash
# Compila emi-plush contra los jars reales (nombres intermediary). Variables: MC (client-intermediary.jar), GECKO, POKEBLOCKS,
# FAPI (carpeta con los jars de Fabric API ya extraidos), LIBS (classpath de librerias de Minecraft), OUT (jar de salida).
set -euo pipefail
cd "$(dirname "$0")"
: "${MC:?}" "${GECKO:?}" "${POKEBLOCKS:?}" "${FAPI:?}" "${LIBS:?}" "${OUT:?}"
unset JAVA_TOOL_OPTIONS
rm -rf build && mkdir -p build/classes
python3 gen/gen_michi.py resources
python3 gen/gen_pokemon.py resources
python3 gen/gen_michi_extras.py resources
if [ -n "${COBBLEMON_JAR:-}" ]; then
  (cd gen && python3 gen_all.py ../resources)
else
  echo "(sin COBBLEMON_JAR: se usan las texturas y modelos Emi ya generados)"
fi
python3 gen/gen_dolls.py resources src/emi/plush/gen
CP="$MC:$GECKO:$POKEBLOCKS:$(ls $FAPI/*.jar | tr '\n' ':')$LIBS"
javac --release 21 -encoding UTF-8 -nowarn -cp "$CP" -d build/classes $(find src -name '*.java')
cp -r resources/* build/classes/
(cd build/classes && rm -f "$OUT" && zip -qr "$OUT" .)
echo "jar: $OUT"
