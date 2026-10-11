#!/usr/bin/env python3
"""Gasha Michi: ticket (objeto), textura, textos y funciones de datos con los premios.
Premios (mil casillas por tirada) y garantia: cada 10 tiradas sin pareja sale una PAREJA (Pokemon + su peluche).
  40% comun  25% bueno  9% epico (objetos utiles)  10% Pokemon del mod  7% peluche  7% Pokemon Emi/GatitoAlien  2% pareja
Uso: python3 gen_gasha.py <carpeta resources>   (se ejecuta antes de gen_dolls: los textos se mezclan en los idiomas existentes)"""
import json, os, sys
from PIL import Image, ImageDraw
from michi_forms import ACC

RES = sys.argv[1]
A = f'{RES}/assets/emi_plush'
D = f'{RES}/data/emi_plush/function/gasha'
os.makedirs(D, exist_ok=True)
NOOP = 'scoreboard players add @s emi_gasha 0'

# ------------------------------------------------------------------ objetos utiles: (nombre, objeto, cantidad)
COMUN = [('Poké Ball x8', 'cobblemon:poke_ball', 8), ('Gran Ball x4', 'cobblemon:great_ball', 4), ('Premier Ball x6', 'cobblemon:premier_ball', 6),
         ('Poción x4', 'cobblemon:potion', 4), ('Superpoción x3', 'cobblemon:super_potion', 3), ('Revivir x2', 'cobblemon:revive', 2),
         ('Frascos de experiencia x8', 'minecraft:experience_bottle', 8), ('Carne cocinada x12', 'minecraft:cooked_beef', 12),
         ('Lingotes de hierro x8', 'minecraft:iron_ingot', 8), ('Lingotes de oro x4', 'minecraft:gold_ingot', 4), ('Caramelo de experiencia S x3', 'cobblemon:exp_candy_s', 3)]
BUENO = [('Ultra Ball x6', 'cobblemon:ultra_ball', 6), ('Hiperpoción x4', 'cobblemon:hyper_potion', 4), ('Máx. Poción x2', 'cobblemon:max_potion', 2),
         ('Caramelo de experiencia M x3', 'cobblemon:exp_candy_m', 3), ('Caramelo de experiencia L x2', 'cobblemon:exp_candy_l', 2), ('Diamantes x3', 'minecraft:diamond', 3),
         ('Esmeraldas x6', 'minecraft:emerald', 6), ('Manzanas doradas x2', 'minecraft:golden_apple', 2), ('PP Más x1', 'cobblemon:pp_up', 1),
         ('Caramelo raro x1', 'cobblemon:rare_candy', 1), ('Fichas de casino x5', 'emipokemon:casino_chip', 5), ('Ticket Gacha x1', 'emipokemon:gacha_ticket', 1)]
EPICO = [('Caramelos raros x5', 'cobblemon:rare_candy', 5), ('Caramelo de experiencia XL x2', 'cobblemon:exp_candy_xl', 2), ('Máx. Revivir x2', 'cobblemon:max_revive', 2),
         ('Restaurar todo x3', 'cobblemon:full_restore', 3), ('PP Máx x1', 'cobblemon:pp_max', 1), ('Cápsula de habilidad x1', 'cobblemon:ability_capsule', 1),
         ('Diamantes x8', 'minecraft:diamond', 8), ('Tickets de Tesoros de Emi x2', 'emipokemon:treasure_gacha_ticket', 2), ('Tickets Gacha x3', 'emipokemon:gacha_ticket', 3),
         ('Tickets de Cosméticos x2', 'emipokemon:sticker_gacha_ticket', 2), ('Lingote de netherita x1', 'minecraft:netherite_ingot', 1),
         ('Manzana dorada encantada x1', 'minecraft:enchanted_golden_apple', 1), ('Nudo Destino x1', 'cobblemon:destiny_knot', 1), ('Huevo Suerte x1', 'cobblemon:lucky_egg', 1),
         ('Parche de habilidad x1', 'cobblemon:ability_patch', 1), ('Amuleto Shiny de Emi x1', 'emipokemon:shiny_amulet', 1), ('Master Ball x1', 'cobblemon:master_ball', 1),
         ('Piedra Fuego x1', 'cobblemon:fire_stone', 1), ('Piedra Agua x1', 'cobblemon:water_stone', 1), ('Piedra Trueno x1', 'cobblemon:thunder_stone', 1),
         ('Piedra Hoja x1', 'cobblemon:leaf_stone', 1), ('Piedra Hielo x1', 'cobblemon:ice_stone', 1), ('Piedra Lunar x1', 'cobblemon:moon_stone', 1),
         ('Piedra Solar x1', 'cobblemon:sun_stone', 1), ('Piedra Noche x1', 'cobblemon:dusk_stone', 1), ('Piedra Alba x1', 'cobblemon:dawn_stone', 1), ('Piedra Brillante x1', 'cobblemon:shiny_stone', 1)]
# peso en el reparto de los epicos (el resto pesa 3); lo mas raro baja
PESO_EPICO = {'cobblemon:master_ball': 1, 'minecraft:enchanted_golden_apple': 1, 'emipokemon:shiny_amulet': 1, 'minecraft:netherite_ingot': 2, 'cobblemon:ability_patch': 2}

# ------------------------------------------------------------------ Pokemon que metimos en el mod de Emi (especies emipokemon:*): (nombre, especie, nivel, peso)
MOD = [('Unown 6', 'emipokemon:unown_6', 20, 3), ('Unown 7', 'emipokemon:unown_7', 20, 3), ('Unown 67', 'emipokemon:unown_67', 20, 3),
       ('Espurr de Emi', 'emipokemon:espurr_emi', 15, 5), ('Meowstic de Emi', 'emipokemon:meowstic_emi', 35, 3), ('Zorua de Emi', 'emipokemon:zorua_emi', 15, 5),
       ('Zoroark de Emi', 'emipokemon:zoroark_emi', 35, 3), ('Sprigatito de Emi', 'emipokemon:sprigatito_emi', 10, 5), ('Floragato de Emi', 'emipokemon:floragato_emi', 25, 4),
       ('Meowscarada de Emi', 'emipokemon:meowscarada_emi', 40, 2), ('Eevee Gatito', 'emipokemon:eevee_gatito', 20, 4), ('Snorlax de Emi', 'emipokemon:snorlax_emi', 40, 2)]

# ------------------------------------------------------------------ Pokemon + peluche: (nombre, comando del Pokemon, objeto del peluche, peso)
PREMIOS = []
EMI = [('Cleffa', 'cleffa', 15, 5), ('Clefairy', 'clefairy', 25, 4), ('Clefable', 'clefable', 40, 3), ('Igglybuff', 'igglybuff', 15, 5), ('Jigglypuff', 'jigglypuff', 25, 4),
       ('Wigglytuff', 'wigglytuff', 40, 3), ('Ralts', 'ralts', 15, 5), ('Kirlia', 'kirlia', 25, 4), ('Gardevoir', 'gardevoir', 40, 3), ('Happiny', 'happiny', 15, 5),
       ('Chansey', 'chansey', 25, 4), ('Blissey', 'blissey', 40, 3), ('Eevee', 'eevee', 15, 5), ('Vaporeon', 'vaporeon', 35, 3), ('Jolteon', 'jolteon', 35, 3),
       ('Flareon', 'flareon', 35, 3), ('Espeon', 'espeon', 35, 3), ('Umbreon', 'umbreon', 35, 3), ('Leafeon', 'leafeon', 35, 3), ('Glaceon', 'glaceon', 35, 3), ('Sylveon', 'sylveon', 35, 3)]
for n, sp, lvl, w in EMI:
    PREMIOS.append((f'{n} de Emi', f'givepokemon @s {sp} level={lvl} emi=true', f'emi_plush:{sp}_emi', w))
PREMIOS.append(('GatitoAlien Dorado', 'givepokemon @s gatitoalien level=50 gold=true', 'emi_plush:gatitoalien_gold', 2))
for acc, fn, es, en in ACC:
    PREMIOS.append((f'GatitoAlien {es}', f'givepokemon @s gatitoalien level=40 accessory={acc}', f'emi_plush:gatitoalien_{acc}', 2))
for acc, fn, es, en in ACC:
    PREMIOS.append((f'GatitoAlien Dorado {es}' if es.startswith('con ') else f'GatitoAlien {es} Dorado', f'givepokemon @s gatitoalien level=50 gold=true accessory={acc}', f'emi_plush:gatitoalien_gold_{acc}', 1))
PREMIOS.append(('Michi Dramático (peluche original)', None, 'emi_plush:michi_dramatico', 0))   # solo suelta como peluche

def snbt(s):
    return "'" + s.replace('\\', '\\\\').replace("'", "\\'") + "'"
def entrada(n, c, cmd1, cmd2=NOOP):
    return '{n:%s,c:%s,cmd1:%s,cmd2:%s}' % (snbt(n), snbt(c), snbt(cmd1), snbt(cmd2))

def pesos(lst):
    """indice de cada entrada repetido tantas veces como pese (la tirada elige una posicion de la lista de indices)"""
    out = []
    for i, w in enumerate(lst): out += [i] * w
    return out

pools = {}   # nombre -> (lista de entradas, pesos, color, efecto, anunciar)
pools['comun'] = ([entrada(n, 'white', f'give @s {i} {c}') for n, i, c in COMUN], pesos([1] * len(COMUN)), 'fx_comun', False)
pools['bueno'] = ([entrada(n, 'green', f'give @s {i} {c}') for n, i, c in BUENO], pesos([1] * len(BUENO)), 'fx_bueno', False)
pools['epico'] = ([entrada(n, 'aqua', f'give @s {i} {c}') for n, i, c in EPICO], pesos([PESO_EPICO.get(i, 3) for n, i, c in EPICO]), 'fx_epico', False)
pools['mod'] = ([entrada(n, 'gold', f'givepokemon @s {sp} level={l}') for n, sp, l, w in MOD], pesos([w for *_, w in MOD]), 'fx_premio', True)
solo_peluche = [(n, pl, w if w else 1) for n, pk, pl, w in PREMIOS]
pools['peluche'] = ([entrada(f'Peluche {n}' if not n.startswith('Michi') else 'Peluche Michi Dramático', 'light_purple', f'give @s {pl}') for n, pl, w in solo_peluche], pesos([w for *_, w in solo_peluche]), 'fx_premio', True)
con_pk = [(n, pk, pl, w) for n, pk, pl, w in PREMIOS if pk]
pools['pokemon'] = ([entrada(n, 'yellow', pk) for n, pk, pl, w in con_pk], pesos([w for *_, w in con_pk]), 'fx_premio', True)
pools['pareja'] = ([entrada(f'{n} + su peluche', 'red', pk, f'give @s {pl}') for n, pk, pl, w in con_pk], pesos([w for *_, w in con_pk]), 'fx_pareja', True)

def w(path, text):
    open(f'{D}/{path}', 'w', encoding='utf-8').write(text)

# ---- carga: marcador y listas de premios
lineas = ['scoreboard objectives add emi_gasha dummy']
for nom, (ents, idx, fx, anun) in pools.items():
    lineas.append(f'data modify storage emi_plush:gasha {nom} set value [{",".join(ents)}]')
    lineas.append(f'data modify storage emi_plush:gasha {nom}_w set value {idx}')
w('load.mcfunction', '\n'.join(lineas) + '\n')
os.makedirs(f'{RES}/data/minecraft/tags/function', exist_ok=True)
json.dump({'values': ['emi_plush:gasha/load']}, open(f'{RES}/data/minecraft/tags/function/load.json', 'w'))

# ---- una funcion por reparto: elige posicion, entrega, efectos y aviso
for nom, (ents, idx, fx, anun) in pools.items():
    t = [f'execute store result storage emi_plush:gasha arg.i int 1 run random value 0..{len(idx) - 1}',
         f'data modify storage emi_plush:gasha arg.pool set value "{nom}"',
         'function emi_plush:gasha/elegir with storage emi_plush:gasha arg', f'function emi_plush:gasha/{fx}']
    if anun: t.append('function emi_plush:gasha/anunciar with storage emi_plush:gasha sel')
    if nom == 'pareja': t.append('scoreboard players set @s emi_gasha 0')
    w(f'dar_{nom}.mcfunction', '\n'.join(t) + '\n')

w('elegir.mcfunction', '$data modify storage emi_plush:gasha arg.j set from storage emi_plush:gasha $(pool)_w[$(i)]\nfunction emi_plush:gasha/elegir2 with storage emi_plush:gasha arg\n')
w('elegir2.mcfunction', '$data modify storage emi_plush:gasha sel set from storage emi_plush:gasha $(pool)[$(j)]\nfunction emi_plush:gasha/entregar with storage emi_plush:gasha sel\n')
w('entregar.mcfunction', '$$(cmd1)\n$$(cmd2)\n'
  '$tellraw @s [{"text":"✦ Gasha Michi: ","color":"light_purple"},{"text":"$(n)","color":"$(c)","bold":true}]\n')
w('anunciar.mcfunction', '$tellraw @a [{"text":"✦ ","color":"light_purple"},{"selector":"@s","color":"yellow"},{"text":" ha sacado ","color":"gray"},{"text":"$(n)","color":"$(c)","bold":true},{"text":" en el Gasha Michi","color":"gray"}]\n')
w('fx_comun.mcfunction', 'playsound minecraft:entity.item.pickup player @s ~ ~ ~ 1 1\n')
w('fx_bueno.mcfunction', 'playsound minecraft:entity.experience_orb.pickup player @s ~ ~ ~ 1 1\nparticle minecraft:happy_villager ~ ~1 ~ 0.4 0.5 0.4 0 8\n')
w('fx_epico.mcfunction', 'playsound minecraft:entity.player.levelup player @s ~ ~ ~ 1 1.2\nparticle minecraft:end_rod ~ ~1 ~ 0.4 0.6 0.4 0.05 20\n')
w('fx_premio.mcfunction', 'playsound minecraft:ui.toast.challenge_complete player @s ~ ~ ~ 1 1\nparticle minecraft:totem_of_undying ~ ~1 ~ 0.4 0.6 0.4 0.3 30\n')
w('fx_pareja.mcfunction', 'playsound minecraft:ui.toast.challenge_complete player @a ~ ~ ~ 1 0.8\nplaysound minecraft:entity.firework_rocket.twinkle player @a ~ ~ ~ 1 1\n'
  'particle minecraft:totem_of_undying ~ ~1 ~ 0.6 0.9 0.6 0.4 80\nparticle minecraft:heart ~ ~2 ~ 0.6 0.4 0.6 0 12\n')

# ---- tirada: suma 1 al contador, garantia en la 10 y reparto por casillas
tirada = ['scoreboard players add @s emi_gasha 1',
          'execute if score @s emi_gasha matches 10.. run return run function emi_plush:gasha/dar_pareja',
          'execute store result score #r emi_gasha run random value 1..1000']
for rango, nom in (('1..400', 'comun'), ('401..650', 'bueno'), ('651..740', 'epico'), ('741..840', 'mod'), ('841..910', 'peluche'), ('911..980', 'pokemon'), ('981..1000', 'pareja')):
    tirada.append(f'execute if score #r emi_gasha matches {rango} run return run function emi_plush:gasha/dar_{nom}')
w('tirada.mcfunction', '\n'.join(tirada) + '\n')
w('tirar.mcfunction', 'function emi_plush:gasha/tirada\n'
  'title @s actionbar [{"text":"Gasha Michi: ","color":"light_purple"},{"score":{"name":"@s","objective":"emi_gasha"},"color":"yellow"},{"text":"/10 para tu pareja garantizada (Pokémon + peluche)","color":"gray"}]\n')

# ---- textura del ticket: boleto rosa con muescas, estrella dorada y huellita
im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
ROSA, ROSA_D, ORO, ORO_L, NEGRO = (240, 98, 156, 255), (190, 60, 120, 255), (255, 200, 40, 255), (255, 240, 150, 255), (40, 30, 50, 255)
d.rectangle([1, 3, 14, 12], fill=ROSA); d.rectangle([1, 3, 14, 3], fill=ROSA_D); d.rectangle([1, 12, 14, 12], fill=ROSA_D)
for y in (3, 12):
    for x in (4, 7, 10, 13): im.putpixel((x, y), (0, 0, 0, 0))
for x, y in ((1, 7), (1, 8), (14, 7), (14, 8)): im.putpixel((x, y), (0, 0, 0, 0))
for x, y in ((8, 5), (7, 6), (8, 6), (9, 6), (6, 7), (7, 7), (8, 7), (9, 7), (10, 7), (7, 8), (8, 8), (9, 8), (6, 9), (7, 9), (9, 9), (10, 9)): im.putpixel((x, y), ORO)
im.putpixel((8, 7), ORO_L); im.putpixel((8, 6), ORO_L)
for x, y in ((3, 6), (3, 9), (12, 6), (12, 9)): im.putpixel((x, y), ORO_L)
os.makedirs(f'{A}/textures/item', exist_ok=True); os.makedirs(f'{A}/models/item', exist_ok=True)
im.save(f'{A}/textures/item/ticket_gasha_michi.png')
json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'emi_plush:item/ticket_gasha_michi'}}, open(f'{A}/models/item/ticket_gasha_michi.json', 'w'), indent=2)

# ---- textos
TXT = {'es': {'item.emi_plush.ticket_gasha_michi': 'Ticket Gasha Michi',
              'item.emi_plush.ticket_gasha_michi.desc': 'Clic derecho: 1 tirada. Agachado: hasta 10 de golpe.',
              'item.emi_plush.ticket_gasha_michi.desc2': 'La tirada 10 sin pareja trae un Pokémon con su peluche.'},
       'en': {'item.emi_plush.ticket_gasha_michi': 'Michi Gasha Ticket',
              'item.emi_plush.ticket_gasha_michi.desc': 'Right click: 1 pull. Sneak: up to 10 at once.',
              'item.emi_plush.ticket_gasha_michi.desc2': 'Every 10th pull without a pair gives a Pokemon with its plush.'}}
for code, k in (('en_us', 'en'), ('es_es', 'es'), ('es_mx', 'es')):
    p = f'{A}/lang/{code}.json'
    d_ = json.load(open(p, encoding='utf-8')); d_.update(TXT[k])
    json.dump(d_, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
tot = {nom: len(e) for nom, (e, *_r) in pools.items()}
print('Gasha Michi:', tot, '| casillas de pareja', len(pools['pareja'][1]))
