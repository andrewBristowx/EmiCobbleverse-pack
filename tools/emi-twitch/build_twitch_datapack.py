#!/usr/bin/env python3
"""Genera datapacks/EmiTwitch-DP.zip (loot tables de tickets aleatorios) y tools/emi-twitch/comandos.txt.

Cada tabla emipokemon:twitch/random_tickets_<N> entrega N tickets de gacha al azar (mismo peso cada tipo).
Las reglas de recompensa de Twitch la usan con la accion `comando`:  loot give {player} loot emipokemon:twitch/random_tickets_<N>
El datapack incluye ademas la funcion emipokemon:twitch/setup_recompensas que crea todas las reglas de una vez:
    /function emipokemon:twitch/setup_recompensas
Uso: build_twitch_datapack.py <salida.zip> <comandos.txt>
"""
import json, sys, zipfile

TICKETS = ["gacha_ticket", "emi_special_banner_ticket", "treasure_gacha_ticket",
           "sticker_gacha_ticket", "card_booster_ticket", "type_booster_ticket"]
# escalon -> numero de tickets (se duplica al duplicar la cantidad)
GIFTSUBS = [(1, 2), (2, 4), (4, 8), (8, 16), (16, 32), (32, 64), (64, 128), (128, 256)]
BITS = [(100, 1), (200, 2), (400, 4), (800, 8), (1600, 16), (3200, 32), (6400, 64), (12800, 128)]

def table(n):
    return {"type": "minecraft:chest", "pools": [{"rolls": n, "entries": [
        {"type": "minecraft:item", "name": "emipokemon:" + t, "weight": 1} for t in TICKETS]}]}

def rule_lines():
    lines = ["emi twitch eventos recompensa borrar_todas"]
    for thr, n in GIFTSUBS:
        lines.append(f"emi twitch eventos recompensa giftsubs {thr} donador comando loot give {{player}} loot emipokemon:twitch/random_tickets_{n}")
    for thr, n in BITS:
        lines.append(f"emi twitch eventos recompensa bits {thr} donador comando loot give {{player}} loot emipokemon:twitch/random_tickets_{n}")
    lines.append("emi twitch eventos recompensa listar")
    return lines

def main(dest, cmds):
    counts = sorted({n for _, n in GIFTSUBS + BITS})
    with zipfile.ZipFile(dest, 'w', zipfile.ZIP_DEFLATED) as z:
        def add(name, data):
            zi = zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0))
            zi.compress_type = zipfile.ZIP_DEFLATED; zi.external_attr = 0o644 << 16
            z.writestr(zi, data)
        add('pack.mcmeta', json.dumps({"pack": {"pack_format": 48, "description": "EmiCobbleverse: sorteo de tickets aleatorios para las recompensas de Twitch"}}, indent=2))
        for n in counts:
            add(f'data/emipokemon/loot_table/twitch/random_tickets_{n}.json', json.dumps(table(n), indent=2) + '\n')
        add('data/emipokemon/function/twitch/setup_recompensas.mcfunction',
            "# Crea las recompensas de Twitch (borra las anteriores). Ejecutar como OP: /function emipokemon:twitch/setup_recompensas\n" + '\n'.join(rule_lines()) + '\n')
    lines = ["# Recompensas Twitch. Forma facil: /function emipokemon:twitch/setup_recompensas (del datapack).",
             "# O pega estas lineas una a una como OP; se guardan en el servidor.",
             "/emi twitch eventos recompensa borrar_todas", ""]
    for thr, n in GIFTSUBS:
        lines.append(f"/emi twitch eventos recompensa giftsubs {thr} donador comando loot give {{player}} loot emipokemon:twitch/random_tickets_{n}")
    lines.append("")
    for thr, n in BITS:
        lines.append(f"/emi twitch eventos recompensa bits {thr} donador comando loot give {{player}} loot emipokemon:twitch/random_tickets_{n}")
    lines += ["", "/emi twitch eventos recompensa listar", "/emi twitch eventos iniciar"]
    open(cmds, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
    print(f'{len(counts)} loot tables -> {dest}; {len(GIFTSUBS)+len(BITS)} reglas -> {cmds}')

if __name__ == '__main__':
    if len(sys.argv) != 3: sys.exit(__doc__)
    main(*sys.argv[1:])
