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

OBJ = "emitwitch_don"
STEPS = [256, 256, 256, 256, 128, 64, 32, 16, 8, 4, 2, 1]  # 1 tirada por dolar (descomposicion binaria); tope 1279 USD por llamada

def donacion_function():
    """Funcion con macros: /function emipokemon:twitch/donacion {jugador:"Nombre",dolares:5}  -> 1 tirada (ticket de gacha aleatorio) por dolar.
    Devuelve (return) los dolares entregados, o 0 si no se entrego nada: asi RCON/el script saben el resultado ("Function ... returned N")."""
    msg_off = '[{"text":"Donacion NO entregada: ","color":"red"},{"text":"$(jugador)","color":"yellow"},{"text":" no esta conectado.","color":"red"}]'
    L = ["# Donaciones (Streamlabs u otras): 1 tirada por dolar entero. Uso (como OP):",
         '#   /function emipokemon:twitch/donacion {jugador:"NombreDeMinecraft",dolares:5}',
         "# El jugador debe estar conectado. Los dolares son un numero entero (5, no 5.5). Maximo 1279 por llamada.",
         "# Devuelve los dolares entregados (\"returned 5\") o 0 si no se entrego (jugador desconectado o menos de 1 dolar).",
         f'$execute unless entity @a[name="$(jugador)"] run tellraw @s {msg_off}',
         '$execute unless entity @a[name="$(jugador)"] run return fail',
         f"$scoreboard players set #usd {OBJ} $(dolares)",
         f"scoreboard players operation #total {OBJ} = #usd {OBJ}",
         f'execute if score #total {OBJ} matches ..0 run tellraw @s [{{"text":"Donacion sin tiradas: minimo 1 USD.","color":"red"}}]',
         f"execute if score #total {OBJ} matches ..0 run return fail"]
    for n in STEPS:
        L.append(f"$execute if score #usd {OBJ} matches {n}.. run loot give $(jugador) loot emipokemon:twitch/random_tickets_{n}")
        L.append(f"execute if score #usd {OBJ} matches {n}.. run scoreboard players remove #usd {OBJ} {n}")
    L.append(f'$tellraw @a [{{"text":"\\u2726 ","color":"light_purple"}},{{"text":"$(jugador)","color":"gold","bold":true}},{{"text":" don\\u00f3 ","color":"yellow"}},{{"text":"$(dolares) USD","color":"green"}},{{"text":" y recibe sus tiradas de gacha. \\u00a1Gracias!","color":"yellow"}}]')
    L.append("$return $(dolares)")
    return "\n".join(L) + "\n"

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
        add('data/emipokemon/function/twitch/donacion.mcfunction', donacion_function())
        add('data/emipokemon/function/twitch/cargar.mcfunction', f"scoreboard objectives add {OBJ} dummy\n")
        add('data/minecraft/tags/function/load.json', json.dumps({"values": ["emipokemon:twitch/cargar"]}, indent=2) + "\n")
    lines = ["# Recompensas Twitch. Forma facil: /function emipokemon:twitch/setup_recompensas (del datapack).",
             "# O pega estas lineas una a una como OP; se guardan en el servidor.",
             "/emi twitch eventos recompensa borrar_todas", ""]
    for thr, n in GIFTSUBS:
        lines.append(f"/emi twitch eventos recompensa giftsubs {thr} donador comando loot give {{player}} loot emipokemon:twitch/random_tickets_{n}")
    lines.append("")
    for thr, n in BITS:
        lines.append(f"/emi twitch eventos recompensa bits {thr} donador comando loot give {{player}} loot emipokemon:twitch/random_tickets_{n}")
    lines += ["", "/emi twitch eventos recompensa listar", "/emi twitch eventos iniciar", "",
              "# Donaciones (Streamlabs u otras), 1 tirada por dolar entero; el jugador debe estar conectado:",
              '# /function emipokemon:twitch/donacion {jugador:"NombreDeMinecraft",dolares:5}']
    open(cmds, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
    print(f'{len(counts)} loot tables -> {dest}; {len(GIFTSUBS)+len(BITS)} reglas -> {cmds}')

if __name__ == '__main__':
    if len(sys.argv) != 3: sys.exit(__doc__)
    main(*sys.argv[1:])
