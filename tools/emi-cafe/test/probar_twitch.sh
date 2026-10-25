#!/bin/bash
# Prueba rapida (sin Minecraft) de la limpieza de textos del chat de Twitch: enlaces, insultos, comillas, largo, nombres.
cd "$(dirname "$0")/.." || exit 1
unset JAVA_TOOL_OPTIONS
rm -rf /tmp/pruebatwitch && mkdir /tmp/pruebatwitch
javac -encoding UTF-8 -d /tmp/pruebatwitch src/main/java/emi/cafe/TwitchTexto.java test/PruebaTwitch.java && java -cp /tmp/pruebatwitch emi.cafe.PruebaTwitch
