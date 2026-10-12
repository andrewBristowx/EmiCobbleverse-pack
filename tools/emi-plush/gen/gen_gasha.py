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
  '$tellraw @s [{"text":"✦ Gasha Especial de Eventos: ","color":"light_purple"},{"text":"$(n)","color":"$(c)","bold":true}]\n')
w('anunciar.mcfunction', '$tellraw @a [{"text":"✦ ","color":"light_purple"},{"selector":"@s","color":"yellow"},{"text":" ha sacado ","color":"gray"},{"text":"$(n)","color":"$(c)","bold":true},{"text":" en el Gasha Especial de Eventos","color":"gray"}]\n')
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
  'title @s actionbar [{"text":"Gasha Especial de Eventos: ","color":"light_purple"},{"score":{"name":"@s","objective":"emi_gasha"},"color":"yellow"},{"text":"/10 para tu pareja garantizada (Pokémon + peluche)","color":"gray"}]\n')

# ---- textura del ticket (32x32): boleto rosa con marco dorado, el michialien con sus antenas y una estrella dorada en el talon
W = H = 32
im=Image.new('RGBA',(W,H),(0,0,0,0)); P=im.load()
ORO=(255,205,45,255); ORO_D=(196,134,20,255); ORO_L=(255,245,160,255); OUT=(92,40,70,255)
ROSA_T=(255,170,210,255); ROSA_B=(222,84,150,255)
BL=(250,246,255,255); GR=(200,196,215,255); GR_D=(150,146,175,255); MOR=(150,90,205,255); MOR_D=(92,52,140,255); NEG=(34,28,48,255)
def lerp(a,b,t): return tuple(int(a[i]+(b[i]-a[i])*t) for i in range(4))
# boleto: x 1..30, y 7..25 con muescas semicirculares en los lados
x0,x1,y0,y1=1,30,5,26
def dentro(x,y):
    if not (x0<=x<=x1 and y0<=y<=y1): return False
    cy=(y0+y1)/2
    for cx in (x0,x1):
        if (x-cx)**2+(y-cy)**2<=3.2**2: return False
    return True
for y in range(H):
    for x in range(W):
        if dentro(x,y):
            borde=not all(dentro(x+dx,y+dy) for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)))
            if borde: P[x,y]=OUT
            else:
                t=(y-y0)/(y1-y0); P[x,y]=lerp(ROSA_T,ROSA_B,t)
# marco dorado interior
for y in range(H):
    for x in range(W):
        if dentro(x,y) and P[x,y]!=OUT:
            vec=[dentro(x+dx,y+dy) and P[x+dx,y+dy]!=OUT for dx,dy in ((1,0),(-1,0),(0,1),(0,-1))]
            if not all(vec): P[x,y]=ORO
for y in range(H):
    for x in range(W):
        if P[x,y]==ORO and (y==y0+1): P[x,y]=ORO_L
        if P[x,y]==ORO and (y==y1-1): P[x,y]=ORO_D
# perforado
for y in range(y0+2,y1-1,2): P[22,y]=(255,255,255,255)
# cabeza del michialien (cubo) x 5..17, y 13..22
for y in range(13,23):
    for x in range(5,18):
        c=BL
        if y==13: c=(255,255,255,255)
        if x==17 or y==22: c=GR
        if x==5: c=BL
        P[x,y]=c
for x in range(5,18): P[x,13]=(255,255,255,255)
for y in range(13,23): P[17,y]=GR_D
for x in range(5,18): P[x,22]=GR_D
# orejas moradas laterales
for (x,y) in ((4,12),(5,12),(4,13),(4,14),(18,12),(17,12),(18,13),(18,14)): P[x,y]=MOR
# antenas con bolitas moradas
for y in (11,12): P[8,y]=GR; P[14,y]=GR
for (x,y) in ((7,9),(8,9),(9,9),(7,10),(8,10),(9,10),(13,9),(14,9),(15,9),(13,10),(14,10),(15,10)): P[x,y]=MOR
for (x,y) in ((7,9),(13,9)): P[x,y]=(190,140,240,255)
for (x,y) in ((9,10),(15,10)): P[x,y]=MOR_D
# ojos enfadados (morados con brillo) y cejas
for (x,y) in ((7,16),(8,16),(7,17),(8,17),(14,16),(15,16),(14,17),(15,17)): P[x,y]=MOR_D
for (x,y) in ((7,16),(14,16)): P[x,y]=(230,200,255,255)
for (x,y) in ((6,15),(7,15),(15,15),(16,15)): P[x,y]=NEG
P[8,15]=NEG; P[14,15]=NEG
# nariz y boca con luna
P[11,18]=GR_D; 
for x in range(9,14): P[x,20]=NEG
for x in range(8,15): P[x,21]=NEG
P[10,20]=MOR; P[10,21]=MOR; P[11,21]=MOR
# patitas
for (x,y) in ((7,23),(8,23),(14,23),(15,23)): P[x,y]=(255,200,215,255)
# estrella dorada en el talon
star=[(26,11),(26,12),(25,13),(26,13),(27,13),(23,14),(24,14),(25,14),(26,14),(27,14),(28,14),(29,14),(24,15),(25,15),(26,15),(27,15),(28,15),(25,16),(26,16),(27,16),(24,17),(25,17),(27,17),(28,17),(24,18),(28,18)]
for x,y in star: P[x,y]=ORO
for x,y in ((26,12),(26,13),(25,14),(26,14),(26,15)): P[x,y]=ORO_L
P[28,17]=ORO_D; P[28,18]=ORO_D; P[28,15]=ORO_D
# destellos
for (x,y) in ((20,10),(19,24),(3,8),(29,22),(21,19)): P[x,y]=(255,255,255,255)
for (x,y) in ((20,9),(20,11),(19,10),(21,10)): P[x,y]=(255,230,245,255)
os.makedirs(f'{A}/textures/item', exist_ok=True); os.makedirs(f'{A}/models/item', exist_ok=True)
im.save(f'{A}/textures/item/ticket_gasha_michi.png')
json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'emi_plush:item/ticket_gasha_michi'}}, open(f'{A}/models/item/ticket_gasha_michi.json', 'w'), indent=2)

# ---- textos
TXT = {'es': {'item.emi_plush.ticket_gasha_michi': 'Ticket Gasha Especial de Eventos',
              'item.emi_plush.ticket_gasha_michi.desc': 'Clic derecho: 1 tirada. Agachado: hasta 10 de golpe.',
              'item.emi_plush.ticket_gasha_michi.desc2': 'La tirada 10 sin pareja trae un Pokémon con su peluche.'},
       'en': {'item.emi_plush.ticket_gasha_michi': 'Special Event Gasha Ticket',
              'item.emi_plush.ticket_gasha_michi.desc': 'Right click: 1 pull. Sneak: up to 10 at once.',
              'item.emi_plush.ticket_gasha_michi.desc2': 'Every 10th pull without a pair gives a Pokemon with its plush.'}}
for code, k in (('en_us', 'en'), ('es_es', 'es'), ('es_mx', 'es')):
    p = f'{A}/lang/{code}.json'
    d_ = json.load(open(p, encoding='utf-8')); d_.update(TXT[k])
    json.dump(d_, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)

# ================================================================== maquina del Gasha Especial de Eventos
# Usa la geometria y las animaciones GeckoLib de la maquina de Emipokemon (emipokemon:geo/emi_gacha_machine.geo.json) con una textura propia recoloreada a michialien.
import colorsys
EP_JAR = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..', 'mods', 'Emipokemon-0.6.0-alpha.1.jar')
def alien(img):
    """Cambia los carmesi/cremas de Emi por morado y lila del michialien; los demas colores (oro, cristal, capsulas) se quedan."""
    img = img.convert('RGBA'); px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0: continue
            h, sat, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if (h > 0.86 or h < 0.03) and sat > 0.4 and v < 0.75:                       # carmesi / magenta oscuro -> morado
                r, g, b = (int(c * 255) for c in colorsys.hsv_to_rgb(0.74, min(1, sat * 0.8), min(1, v * 1.02)))
            elif (r, g, b) == (235, 228, 215): r, g, b = 244, 238, 255                    # crema -> lila blanco
            px[x, y] = (r, g, b, a)
    return img
if os.path.exists(EP_JAR):
    import zipfile, io
    zf = zipfile.ZipFile(EP_JAR)
    os.makedirs(f'{A}/textures/block', exist_ok=True); os.makedirs(f'{A}/textures/item', exist_ok=True)
    alien(Image.open(io.BytesIO(zf.read('assets/emipokemon/textures/block/emi_gacha_machine.png')))).save(f'{A}/textures/block/maquina_gasha_michi.png')
    if not os.path.exists(f'{A}/textures/item/maquina_gasha_michi.png'):
        alien(Image.open(io.BytesIO(zf.read('assets/emipokemon/textures/item/emi_gacha_machine.png')))).save(f'{A}/textures/item/maquina_gasha_michi.png')
json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'emi_plush:item/maquina_gasha_michi'}}, open(f'{A}/models/item/maquina_gasha_michi.json', 'w'), indent=2)
os.makedirs(f'{A}/models/block', exist_ok=True)
json.dump({'textures': {'particle': 'emi_plush:item/maquina_gasha_michi'}, 'elements': []}, open(f'{A}/models/block/maquina_gasha_vacio.json', 'w'))
os.makedirs(f'{A}/blockstates', exist_ok=True)
for nombre in ('maquina_gasha_michi', 'maquina_gasha_michi_arriba'):
    json.dump({'variants': {f'facing={f}': {'model': 'emi_plush:block/maquina_gasha_vacio'} for f in ('north', 'east', 'south', 'west')}}, open(f'{A}/blockstates/{nombre}.json', 'w'))
if os.path.exists(f'{A}/models/block/maquina_gasha_michi.json'): os.remove(f'{A}/models/block/maquina_gasha_michi.json')
os.makedirs(f'{RES}/data/emi_plush/loot_table/blocks', exist_ok=True)
json.dump({'type': 'minecraft:block', 'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}], 'entries': [{'type': 'minecraft:item', 'name': 'emi_plush:maquina_gasha_michi'}], 'rolls': 1.0}]},
          open(f'{RES}/data/emi_plush/loot_table/blocks/maquina_gasha_michi.json', 'w'), indent=2)
json.dump({'type': 'minecraft:block', 'pools': []}, open(f'{RES}/data/emi_plush/loot_table/blocks/maquina_gasha_michi_arriba.json', 'w'))
TXT_M = {'es': {'block.emi_plush.maquina_gasha_michi': 'Máquina Gasha Especial de Eventos', 'item.emi_plush.maquina_gasha_michi': 'Máquina Gasha Especial de Eventos',
                'block.emi_plush.maquina_gasha_michi.sin_ticket': 'Necesitas un Ticket Gasha Especial de Eventos en la mano.'},
         'en': {'block.emi_plush.maquina_gasha_michi': 'Special Event Gasha Machine', 'item.emi_plush.maquina_gasha_michi': 'Special Event Gasha Machine',
                'block.emi_plush.maquina_gasha_michi.sin_ticket': 'You need a Special Event Gasha Ticket in your hand.'}}
for code, k in (('en_us', 'en'), ('es_es', 'es'), ('es_mx', 'es')):
    p = f'{A}/lang/{code}.json'; d_ = json.load(open(p, encoding='utf-8')); d_.update(TXT_M[k])
    json.dump(d_, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)

tot = {nom: len(e) for nom, (e, *_r) in pools.items()}
print('Gasha Michi:', tot, '| casillas de pareja', len(pools['pareja'][1]))
