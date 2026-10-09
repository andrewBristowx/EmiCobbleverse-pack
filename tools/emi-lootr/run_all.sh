#!/bin/bash
# uso: run_all.sh <carpeta_mods_servidor> ; genera ../../datapacks/EmiLootr-DP.zip
M=${1:?carpeta de mods}
D=$(dirname "$0")/../../datapacks
python3 "$(dirname "$0")/convert.py" --items "$(dirname "$0")/items-ids.txt" "$D/EmiLootr-DP.zip" "$D/COBBLEVERSE-DP-v31.zip" "$D/extra/COBBLEVERSE-Hoenn-DP.zip" "$D/extra/COBBLEVERSE-Johto-DP.zip" "$D/extra/COBBLEVERSE-Sinnoh-DP.zip" "$M"/LegendaryMonuments-Cobbleverse.jar "$M"/LumyMon-*.jar
