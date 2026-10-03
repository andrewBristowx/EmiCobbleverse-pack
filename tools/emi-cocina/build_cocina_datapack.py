#!/usr/bin/env python3
"""Genera datapacks/EmiCocina-DP.zip: platos de bayas y otros para la olla de Farmer's Delight (farmersdelight:cooking) y el
Festin del Directo (comida cara que al comerla activa un bono aleatorio de servidor de 20 min, como la Caja Misteriosa).

Los platos son recetas cuyo resultado es un item base (vanilla) con componentes: nombre, lore, `minecraft:food` con efectos
(de Minecraft y de CobbleCuisine, que son efectos de estado normales) y, el Festin, `minecraft:custom_data` para detectarlo.
Uso: build_cocina_datapack.py <salida.zip> <platos.md>
"""
import json, sys, zipfile

TICKS = 20
BOWL = {"id": "minecraft:bowl", "count": 1}

def eff(effect_id, seconds, amplifier=0):
    return {"effect": {"id": effect_id, "duration": int(seconds * TICKS), "amplifier": amplifier, "show_particles": True, "show_icon": True}, "probability": 1.0}

def dish(name, color, base, lore, nutrition, saturation, effects, container=True, rarity=None, glint=False, custom_data=None,
         count=1, stack=16, eat_seconds=1.6):
    food = {"nutrition": nutrition, "saturation": saturation, "can_always_eat": True, "eat_seconds": eat_seconds, "effects": effects}
    if container:
        food["using_converts_to"] = dict(BOWL)
    comp = {
        "minecraft:custom_name": {"text": name, "italic": False, "color": color},
        "minecraft:lore": [{"text": l, "italic": False, "color": "gray"} for l in lore],
        "minecraft:food": food,
        "minecraft:max_stack_size": stack,
    }
    if rarity: comp["minecraft:rarity"] = rarity
    if glint: comp["minecraft:enchantment_glint_override"] = True
    if custom_data: comp["minecraft:custom_data"] = custom_data
    return {"id": base, "count": count, "components": comp}

def recipe(ingredients, result, container=True, tab="meals", experience=1.0, cookingtime=200):
    r = {"type": "farmersdelight:cooking", "experience": experience, "cookingtime": cookingtime,
         "ingredients": [{"item": i} for i in ingredients], "recipe_book_tab": tab, "result": result}
    assert 1 <= len(ingredients) <= 6
    if container:
        r["container"] = dict(BOWL)
    return r

# --- 18 curris de baya de tipo: potencia de tipo de CobbleCuisine + un efecto de Minecraft tematico ---
# (tipo, nombre, baya que resiste ese tipo, color, efecto de Minecraft, segundos, amplificador)
TYPES = [
    ("normal", "Normal", "chilan", "white", "minecraft:saturation", 5, 0),
    ("fire", "Fuego", "occa", "red", "minecraft:fire_resistance", 180, 0),
    ("water", "Agua", "passho", "blue", "minecraft:water_breathing", 180, 0),
    ("electric", "Eléctrico", "wacan", "yellow", "minecraft:speed", 120, 1),
    ("grass", "Planta", "rindo", "green", "minecraft:regeneration", 30, 0),
    ("ice", "Hielo", "yache", "aqua", "minecraft:resistance", 90, 0),
    ("fighting", "Lucha", "chople", "dark_red", "minecraft:strength", 90, 0),
    ("poison", "Veneno", "kebia", "dark_purple", "minecraft:absorption", 120, 1),
    ("ground", "Tierra", "shuca", "gold", "minecraft:haste", 120, 1),
    ("flying", "Volador", "coba", "dark_aqua", "minecraft:slow_falling", 180, 0),
    ("psychic", "Psíquico", "payapa", "light_purple", "minecraft:night_vision", 300, 0),
    ("bug", "Bicho", "tanga", "dark_green", "minecraft:luck", 300, 0),
    ("rock", "Roca", "charti", "gold", "minecraft:health_boost", 180, 1),
    ("ghost", "Fantasma", "kasib", "dark_blue", "minecraft:invisibility", 60, 0),
    ("dragon", "Dragón", "haban", "dark_aqua", "minecraft:strength", 60, 1),
    ("dark", "Siniestro", "colbur", "dark_gray", "minecraft:jump_boost", 120, 1),
    ("steel", "Acero", "babiri", "gray", "minecraft:resistance", 90, 1),
    ("fairy", "Hada", "roseli", "light_purple", "minecraft:regeneration", 20, 1),
]
EFFECT_ES = {"minecraft:saturation": "Saturación", "minecraft:fire_resistance": "Resistencia al fuego", "minecraft:water_breathing": "Respiración acuática",
             "minecraft:speed": "Velocidad", "minecraft:regeneration": "Regeneración", "minecraft:resistance": "Resistencia", "minecraft:strength": "Fuerza",
             "minecraft:absorption": "Absorción", "minecraft:haste": "Prisa", "minecraft:slow_falling": "Caída lenta", "minecraft:night_vision": "Visión nocturna",
             "minecraft:luck": "Suerte", "minecraft:health_boost": "Salud extra", "minecraft:invisibility": "Invisibilidad", "minecraft:jump_boost": "Salto mejorado"}
ROMAN = ["", " II", " III", " IV"]

def fmt(seconds):
    return f"{seconds // 60} min" if seconds % 60 == 0 and seconds >= 60 else f"{seconds} s"

DISHES = {}  # id -> (receta, resumen para el .md)

for key, es, berry, color, mc_eff, secs, amp in TYPES:
    rid = f"curry_baya_{berry}"
    result = dish(f"Curry de Baya {berry.capitalize()} ({es})", color, "minecraft:rabbit_stew",
                  [f"Poder de tipo {es} (5 min, CobbleCuisine)", f"{EFFECT_ES[mc_eff]}{ROMAN[amp]} ({fmt(secs)})"],
                  8, 0.8, [eff(f"cobblecuisine:{key}_spawn", 300), eff(mc_eff, secs, amp)], rarity="uncommon")
    DISHES[rid] = (recipe([f"cobblemon:{berry}_berry"] * 2 + ["farmersdelight:cooked_rice", "minecraft:chicken"], result),
                   (f"Curry de Baya {berry.capitalize()} ({es})", f"2× baya {berry.capitalize()}, arroz cocido, pollo crudo",
                    f"Poder de tipo {es} 5 min + {EFFECT_ES[mc_eff]}{ROMAN[amp]} {fmt(secs)}"))

def special(rid, name, color, base, ingredients, lore, nutrition, sat, effects, summary_effects, tab="meals", container=True, count=1, **kw):
    result = dish(name, color, base, lore, nutrition, sat, effects, container=container, count=count, **kw)
    DISHES[rid] = (recipe(ingredients, result, container=container, tab=tab, experience=kw.get("experience", 1.0) if False else 1.0),
                   (name, ", ".join(ingredients_es(ingredients)), summary_effects))

NAMES_ES = {"minecraft:potato": "patata", "minecraft:carrot": "zanahoria", "minecraft:chicken": "pollo crudo", "minecraft:wheat": "trigo", "minecraft:sugar": "azúcar",
            "minecraft:egg": "huevo", "minecraft:apple": "manzana", "minecraft:diamond": "diamante", "minecraft:golden_apple": "manzana dorada",
            "cobblemon:yellow_apricorn": "aprilima amarilla", "cobblemon:pink_apricorn": "aprilima rosa", "cobblemon:revival_herb": "hierba revividora",
            "cobblemon:medicinal_leek": "puerro medicinal", "cobblemon:enigma_berry": "baya Enigma", "cobblemon:rare_candy": "caramelo raro"}

def ingredients_es(ings):
    out, seen = [], {}
    for i in ings: seen[i] = seen.get(i, 0) + 1
    for i, n in seen.items():
        nm = NAMES_ES.get(i)
        if nm is None:
            nm = i.split(":")[1].replace("_", " ")
            if nm.endswith(" berry"): nm = "baya " + nm[:-6].capitalize()
        out.append(f"{n}× {nm}" if n > 1 else nm)
    return out

special("estofado_bayas_oran", "Estofado de Bayas Oran", "gold", "minecraft:beetroot_soup",
        ["cobblemon:oran_berry"] * 3 + ["minecraft:potato", "minecraft:carrot"],
        ["Regeneración II (30 s)", "Absorción II (2 min)"], 10, 0.8,
        [eff("minecraft:regeneration", 30, 1), eff("minecraft:absorption", 120, 1)], "Regeneración II 30 s, Absorción II 2 min", rarity="uncommon")
special("sopa_bayas_sitrus", "Sopa de Bayas Sitrus", "yellow", "minecraft:mushroom_stew",
        ["cobblemon:sitrus_berry"] * 3 + ["minecraft:chicken", "minecraft:potato"],
        ["Resistencia (90 s), Salud extra II (3 min)", "Bono de EXP (5 min, CobbleCuisine)"], 10, 0.8,
        [eff("minecraft:resistance", 90, 0), eff("minecraft:health_boost", 180, 1), eff("cobblecuisine:exp_boost", 300)],
        "Resistencia 90 s, Salud extra II 3 min, Bono de EXP 5 min", rarity="uncommon")
special("ensalada_bayas_mixtas", "Ensalada de Bayas Mixtas", "green", "minecraft:beetroot_soup",
        [f"cobblemon:{b}_berry" for b in ("cheri", "chesto", "pecha", "rawst", "aspear", "leppa")],
        ["Velocidad II y Salto II (2 min)", "Bono de captura (5 min, CobbleCuisine)"], 8, 0.7,
        [eff("minecraft:speed", 120, 1), eff("minecraft:jump_boost", 120, 1), eff("cobblecuisine:catch_boost", 300)],
        "Velocidad II + Salto II 2 min, Bono de captura 5 min", rarity="uncommon")
special("galletas_aprilima", "Galletas de Aprilima", "gold", "minecraft:cookie",
        ["cobblemon:yellow_apricorn", "cobblemon:pink_apricorn", "minecraft:wheat", "minecraft:sugar"],
        ["Prisa y Velocidad (90 s)"], 3, 0.4, [eff("minecraft:haste", 90, 0), eff("minecraft:speed", 90, 0)],
        "Prisa + Velocidad 90 s (x4 galletas)", tab="misc", container=False, count=4, stack=64)
special("infusion_hierbas", "Infusión de Hierbas Revividoras", "dark_green", "minecraft:mushroom_stew",
        ["cobblemon:revival_herb", "cobblemon:medicinal_leek", "minecraft:sugar", "minecraft:apple"],
        ["Regeneración III (20 s)", "Repelente (5 min, CobbleCuisine)"], 6, 0.6,
        [eff("minecraft:regeneration", 20, 2), eff("minecraft:saturation", 5, 0), eff("cobblecuisine:dubious_spawn", 300)],
        "Regeneración III 20 s, Repelente 5 min", tab="drinks", rarity="rare")
special("tarta_bayas_dulces", "Tarta de Bayas Dulces", "light_purple", "minecraft:pumpkin_pie",
        ["cobblemon:razz_berry", "cobblemon:pinap_berry", "cobblemon:nanab_berry", "minecraft:wheat", "minecraft:sugar", "minecraft:egg"],
        ["Suerte II (5 min)", "Bono de EXP (5 min, CobbleCuisine)"], 8, 0.8,
        [eff("minecraft:luck", 300, 1), eff("cobblecuisine:exp_boost", 300)], "Suerte II 5 min, Bono de EXP 5 min", tab="misc", container=False, rarity="rare")

# --- Festin del Directo: caro de cocinar; al comerlo se activa un bono de servidor (/emi twitch bonus activar) ---
# Limite: MAX_SEGUIDOS festines seguidos; al servir el ultimo hay COOLDOWN_MIN minutos de enfriamiento (para todo el servidor).
# Mientras haya enfriamiento (o si el bono no se pudo activar) el plato se devuelve. Por eso el item no da hambre ni efectos por si mismo:
# si no, se podria comer, recibir el plato de vuelta y repetir. Los efectos al jugador los da la funcion al activarse.
MAX_SEGUIDOS = 3
COOLDOWN_MIN = 60
FESTIN = dish("Festín del Directo", "gold", "minecraft:enchanted_golden_apple",
              ["Al comerlo se activa un bono aleatorio de servidor", "durante 20 minutos para todos los jugadores", "(shiny, tipo, IVs, raros o legendarios).",
               f"Máximo {MAX_SEGUIDOS} seguidos; después, {COOLDOWN_MIN} min de enfriamiento."],
              0, 0.0, [], container=False, rarity="epic", glint=True, custom_data={"emicocina": "festin_directo"}, stack=1, eat_seconds=2.5)
FESTIN_ING = ["minecraft:diamond", "minecraft:golden_apple", "cobblemon:enigma_berry", "cobblemon:revival_herb", "cobblemon:rare_candy"]
DISHES["festin_directo"] = (recipe(FESTIN_ING, FESTIN, container=False, experience=5.0, cookingtime=600),
                            ("Festín del Directo", "diamante, manzana dorada, baya Enigma, hierba revividora, caramelo raro",
                             f"Bono aleatorio de servidor 20 min (para todos) + Regeneración II, Absorción IV, EXP y captura 10 min. Máx. {MAX_SEGUIDOS} seguidos y {COOLDOWN_MIN} min de enfriamiento"))

ADVANCEMENT = {
    "criteria": {"eat": {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": "minecraft:enchanted_golden_apple",
                 "components": {"minecraft:custom_data": {"emicocina": "festin_directo"}}}}}},
    "requirements": [["eat"]],
    "rewards": {"function": "emicocina:festin_directo"},
}
OBJ = "emicocina_festin"
TICKS_COOLDOWN = COOLDOWN_MIN * 60 * 20
FUNCTIONS = {
    # se ejecuta al cargar/recargar los datapacks
    "cargar": f"""scoreboard objectives add {OBJ} dummy
scoreboard players add #count {OBJ} 0
scoreboard players add #ready {OBJ} 0
scoreboard players set #cooldown {OBJ} {TICKS_COOLDOWN}
scoreboard players set #1200 {OBJ} 1200
""",
    # se ejecuta (como el jugador, nivel 2) al terminar de comer el Festin del Directo
    "festin_directo": f"""advancement revoke @s only emicocina:festin_directo
execute store result score #now {OBJ} run time query gametime
execute if score #count {OBJ} matches {MAX_SEGUIDOS}.. if score #now {OBJ} >= #ready {OBJ} run scoreboard players set #count {OBJ} 0
execute if score #count {OBJ} matches {MAX_SEGUIDOS}.. run function emicocina:festin_bloqueado
execute unless score #count {OBJ} matches {MAX_SEGUIDOS}.. run function emicocina:festin_activar
""",
    "festin_activar": f"""scoreboard players set #ok {OBJ} 0
execute store success score #ok {OBJ} run emi twitch bonus activar
execute if score #ok {OBJ} matches 0 run function emicocina:festin_fallo
execute if score #ok {OBJ} matches 1 run function emicocina:festin_servido
""",
    "festin_servido": f"""scoreboard players add #count {OBJ} 1
effect give @s minecraft:regeneration 60 1
effect give @s minecraft:absorption 120 3
effect give @s cobblecuisine:exp_boost 600 0
effect give @s cobblecuisine:catch_boost 600 0
tellraw @a [{{"selector":"@s","color":"gold"}},{{"text":" ha servido un ","color":"yellow"}},{{"text":"Festín del Directo","color":"gold","bold":true}},{{"text":" para todo el servidor.","color":"yellow"}}]
playsound minecraft:ui.toast.challenge_complete master @a ~ ~ ~ 1 1
particle minecraft:totem_of_undying ~ ~1 ~ 0.5 0.8 0.5 0.3 60
execute if score #count {OBJ} matches {MAX_SEGUIDOS}.. run function emicocina:festin_agotado
""",
    "festin_agotado": f"""scoreboard players operation #ready {OBJ} = #now {OBJ}
scoreboard players operation #ready {OBJ} += #cooldown {OBJ}
scoreboard players operation #cdmin {OBJ} = #cooldown {OBJ}
scoreboard players operation #cdmin {OBJ} /= #1200 {OBJ}
tellraw @a [{{"text":"⏳ Se agotaron los Festines del Directo. Podrán servirse otra vez en ","color":"gray"}},{{"score":{{"name":"#cdmin","objective":"{OBJ}"}},"color":"yellow"}},{{"text":" min.","color":"gray"}}]
""",
    "festin_bloqueado": f"""loot give @s loot emicocina:festin_directo
scoreboard players operation #left {OBJ} = #ready {OBJ}
scoreboard players operation #left {OBJ} -= #now {OBJ}
scoreboard players add #left {OBJ} 1199
scoreboard players operation #left {OBJ} /= #1200 {OBJ}
tellraw @s [{{"text":"⏳ Los Festines del Directo están en enfriamiento. Faltan ","color":"red"}},{{"score":{{"name":"#left","objective":"{OBJ}"}},"color":"yellow"}},{{"text":" min. Te devolvimos el plato.","color":"red"}}]
playsound minecraft:entity.villager.no master @s ~ ~ ~ 1 1
""",
    "festin_fallo": """loot give @s loot emicocina:festin_directo
tellraw @s {"text":"No se pudo activar el bono ahora mismo. Te devolvimos el plato.","color":"red"}
playsound minecraft:entity.villager.no master @s ~ ~ ~ 1 1
""",
}
# loot table que devuelve el plato exacto (mismos componentes que la receta)
REFUND = {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": [{
    "type": "minecraft:item", "name": FESTIN["id"],
    "functions": [{"function": "minecraft:set_components", "components": FESTIN["components"]}]}]}]}

def main(dest, md):
    with zipfile.ZipFile(dest, "w", zipfile.ZIP_DEFLATED) as z:
        def add(name, data):
            zi = zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0)); zi.compress_type = zipfile.ZIP_DEFLATED; zi.external_attr = 0o644 << 16
            z.writestr(zi, data)
        add("pack.mcmeta", json.dumps({"pack": {"pack_format": 48, "description": "EmiCobbleverse: platos de bayas y Festín del Directo (Farmer's Delight + CobbleCuisine)"}}, indent=2, ensure_ascii=False))
        for rid, (rec, _) in DISHES.items():
            add(f"data/emicocina/recipe/cooking/{rid}.json", json.dumps(rec, indent=2, ensure_ascii=False) + "\n")
        add("data/emicocina/advancement/festin_directo.json", json.dumps(ADVANCEMENT, indent=2) + "\n")
        add("data/emicocina/loot_table/festin_directo.json", json.dumps(REFUND, indent=2, ensure_ascii=False) + "\n")
        for fname, body in FUNCTIONS.items():
            add(f"data/emicocina/function/{fname}.mcfunction", body)
        add("data/minecraft/tags/function/load.json", json.dumps({"values": ["emicocina:cargar"]}, indent=2) + "\n")
    lines = ["# Platos de cocina de Emi (Farmer's Delight + Cobblemon)", "",
             "Se cocinan en la **olla de Farmer's Delight** (con un fuego o fogón debajo; los platos de cuenco necesitan un cuenco en la mano al sacarlos).", "",
             f"**Festín del Directo:** se pueden servir **{MAX_SEGUIDOS} seguidos** en todo el servidor; al servir el último hay **{COOLDOWN_MIN} min de enfriamiento** (para todos). "
             "Durante el enfriamiento, el plato se devuelve y no pasa nada. Al servirlo se anuncia a todo el servidor y se activa el bono aleatorio de 20 min.", "",
             "| Plato | Ingredientes | Efectos |", "|---|---|---|"]
    for rid, (_, (name, ing, effects)) in DISHES.items():
        lines.append(f"| {name} | {ing} | {effects} |")
    open(md, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
    print(f"{len(DISHES)} recetas -> {dest}")

if __name__ == "__main__":
    if len(sys.argv) != 3: sys.exit(__doc__)
    main(*sys.argv[1:])
