#!/usr/bin/env python3
"""GatitoAlien: version shiny DORADA y accesorios (aspecto `accessory`: sunglasses, beach_hat, crown, bow, headphones, scarf).
Se ejecuta despues de gen_pokemon.py. Cada accesorio es un modelo aparte (copia del modelo base + cubos nuevos en el hueso `head`)
que usa la misma textura (los accesorios estan pintados en la zona libre de abajo) y se elige con el aspecto `<accesorio>-accessory`.
Uso: python3 gen_michi_extras.py <carpeta resources>"""
import json, os, sys, copy
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from emi_lib import box_rects
from michi_forms import FORMAS, ACC
import zfight
from michi_roles import ROLES, JS, abid

OUT = sys.argv[1]
B = os.path.join(OUT, 'assets/cobblemon')
GEO = f'{B}/bedrock/pokemon/models/michi_dramatico'
TEX = f'{B}/textures/pokemon/michi_dramatico'

def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
BLACK = (30, 28, 38); PINK = (240, 98, 156); PINK_L = (250, 170, 205); GOLD = (240, 196, 60); GOLD_D = (190, 140, 30); STRAW = (232, 200, 120); STRAW_D = (205, 170, 90)
WHITE = (250, 247, 250)
RED = (214, 52, 62); RED_D = (156, 30, 44); RED_L = (236, 92, 96); GREEN = (96, 170, 84); GREEN_D = (62, 130, 62); PURPLE = (92, 56, 140); PURPLE_D = (58, 34, 98)
YELLOW = (252, 218, 84); ORANGE = (244, 150, 52); HALO = (255, 238, 150)

# ---------------------------------------------------------------- shiny dorado
def gold_shiny(img):
    sh = img.copy(); px = img.load(); sp = sh.load()
    for y in range(min(img.height, 110)):          # solo el cuerpo (la zona de abajo es de los accesorios)
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if not a: continue
            lu = 0.299 * r + 0.587 * g + 0.114 * b
            if b > g + 25 and b > 120:             # lila y violeta -> oro intenso
                t = min(1.0, lu / 200.0)
                sp[x, y] = mix((70, 36, 6), (255, 205, 60), t ** 1.7) + (a,)
            elif lu > 215 and max(r, g, b) - min(r, g, b) < 40:   # blancos -> crema dorada
                sp[x, y] = mix((238, 188, 62), (255, 226, 122), (lu - 215) / 40.0) + (a,)
            elif lu > 150 and max(r, g, b) - min(r, g, b) < 40:   # sombras claras -> oro palido
                sp[x, y] = mix((190, 140, 38), (238, 188, 62), (lu - 150) / 65.0) + (a,)
    return sh

# ---------------------------------------------------------------- accesorios
class Free:
    """reparte huecos en la zona libre de la textura (desde y0)"""
    def __init__(self, img, y0):
        self.img = img; self.x = 0; self.y = y0; self.rowh = 0
    def alloc(self, w, h):
        if self.x + w > self.img.width: self.x = 0; self.y += self.rowh; self.rowh = 0
        if self.y + h > self.img.height: raise SystemExit('textura llena')
        r = (self.x, self.y); self.x += w; self.rowh = max(self.rowh, h); return r

REGIONES = {}   # hueso del accesorio -> rectangulos (x, y, w, h) de su textura (para repintarlos en la textura dorada)

def cube(geo, free, name, origin, size, painter, rotation=None, pivot=None):
    sx, sy, sz = size
    # Cobblemon solo carga el modelo si todos los UV son enteros (`Expected an int but was 227.5`): el tamaño de los cubos tambien tiene que ser entero
    assert all(float(v).is_integer() for v in size), f'{name}: tamaño {size} con decimales (el UV de los cubos siguientes dejaria de ser entero)'
    W, H = 2 * sz + 2 * sx, sz + sy
    ux, uy = free.alloc(W, H)
    REGIONES.setdefault(name, []).append((ux, uy, int(W), int(H)))
    px = free.img.load()
    for face, (x, y, w, h) in box_rects(ux, uy, sx, sy, sz).items():
        for i in range(w):
            for j in range(h):
                px[x + i, y + j] = painter(face, i, j, w, h)
    bones = geo['minecraft:geometry'][0]['bones']
    b = next(x for x in bones if x['name'] == name) if any(x['name'] == name for x in bones) else None
    if b is None:
        b = {'name': name, 'parent': 'head', 'pivot': [0, 14, 0], 'cubes': []}; bones.append(b)
    c = {'origin': list(origin), 'size': list(size), 'uv': [ux, uy]}
    if rotation: c['pivot'] = list(pivot); c['rotation'] = list(rotation)
    b.setdefault('cubes', []).append(c)

def ala(g, free, cubos):
    """Pone un ala en cada lado: `cubos` = [(origen, tamaño, pintor)] del ala izquierda (x negativo, saliendo de la cabeza); la derecha es su espejo.
    Los huesos acc_wing_left / acc_wing_right salen del modelo base (vacios) y solo las formas con alas les ponen cubos."""
    for o, s, p in cubos:
        cube(g, free, 'acc_wing_left', o, s, p)
        cube(g, free, 'acc_wing_right', (-(o[0] + s[0]), o[1], o[2]), s, p)

def flat(c): return lambda face, i, j, w, h: c + (255,)
def lens(face, i, j, w, h):
    c = (28, 26, 40)
    if face == 'north' and i > 1 and j < 3 and i < w // 2: c = mix(c, (110, 110, 150), 0.7)   # brillo
    return c + (255,)
def straw(face, i, j, w, h):
    c = STRAW if (i + j) % 3 else STRAW_D
    if (i // 3 + j // 2) % 2: c = mix(c, STRAW_D, 0.35)
    return c + (255,)
def band(c1, c2):
    def f(face, i, j, w, h): return (c1 if (i // 3) % 2 else c2) + (255,)
    return f
def dots(base, dot):
    def f(face, i, j, w, h): return (dot if (i % 4 == 1 and j % 4 == 1) else base) + (255,)
    return f
def golden(face, i, j, w, h):
    c = mix(GOLD, GOLD_D, 0.35 * ((i + j) % 3) / 2)
    if face == 'up': c = mix(c, (255, 240, 150), 0.4)
    return c + (255,)

def fur(base, dark):
    def f(face, i, j, w, h): return (dark if (i * 3 + j * 5) % 7 == 0 else base) + (255,)
    return f
def membrana(face, i, j, w, h): return (RED_D if (i % 7 == 3 or j % 5 == 0) else (RED if (i + j) % 11 else RED_L)) + (255,)
def pluma(face, i, j, w, h): return ((222, 228, 245) if (i % 6 == 4) else (WHITE if (i + 2 * j) % 9 else (236, 238, 250))) + (255,)
def pluma_punta(face, i, j, w, h): return ((246, 226, 150) if (i % 6 == 4) else (255, 244, 205)) + (255,)

def glow(face, i, j, w, h): return mix(HALO, (255, 255, 235), 0.5 * ((i + j) % 2)) + (255,)
def flower(petal):
    def f(face, i, j, w, h):
        c = YELLOW if (face in ('north', 'up') and 1 <= i < w - 1 and 1 <= j < h - 1 and w > 2 and i in (w // 2 - 1, w // 2) and j in (h // 2 - 1, h // 2)) else petal
        return c + (255,)
    return f
def skull(face, i, j, w, h):
    c = WHITE
    if face == 'north' and ((i, j) in ((1, 1), (w - 2, 1)) or (j == h - 1 and i % 2)): c = BLACK
    return c + (255,)
def leaves(face, i, j, w, h): return (GREEN_D if (i + 2 * j) % 4 == 0 else GREEN) + (255,)

def corona(g, f):
    """Aro dorado alrededor de la parte de arriba de la cabeza (la cabeza mide ±18 arriba y las orejas salen por dentro del aro), con puntas altas por delante y por detras,
    dos puntas a cada lado y gemas. Todos los tamaños enteros (los UV tienen que ser enteros)."""
    c = lambda o, sz, p=golden: cube(g, f, 'acc_crown', o, sz, p)
    out = [c((-18.5, 30, -18), (37, 4, 4)), c((-18.5, 30, 14), (37, 4, 4)),            # delante y detras
           c((-18.5, 30, -14), (5, 4, 28)), c((13.5, 30, -14), (5, 4, 28))]            # lados
    for x, alto in ((-15, 3), (-7.5, 3), (0, 5), (7.5, 3), (15, 3)):                    # puntas delanteras (la del medio mas alta)
        out += [c((x - 2.5, 34, -18), (5, 4, 4)), c((x - 1.5, 38, -17.5), (3, alto, 3))]
    for x, alto in ((-12, 3), (0, 4), (12, 3)):                                         # puntas traseras
        out += [c((x - 2.5, 34, 14), (5, 4, 4)), c((x - 1.5, 38, 14.5), (3, alto, 3))]
    for x0 in (-18.5, 13.5):                                                            # puntas de los lados
        for z in (-9, 5):
            out += [c((x0, 34, z), (5, 4, 4)), c((x0 + 1, 38, z + 0.5), (3, 3, 3))]
    out += [c((-1.5, 33, -18.6), (3, 3, 1), flat(PINK)),                                # gema delantera
            c((-19.1, 30.5, -2), (1, 3, 3), flat(PINK)), c((18.1, 30.5, -2), (1, 3, 3), flat(PINK)),   # gemas de los lados
            c((-1.5, 33, 18.6), (3, 3, 1), flat(PINK))]                                 # gema trasera
    return out

# alas (solo se ven al volar: fuera de eso estan a escala 0 por la animacion wings_off). Salen de los lados de la cabeza (x = -20), detras de los brazos.
ALAS_DEMONIO = [   # de murcielago: brazo con garra, tres varillas y membrana roja (las membranas vecinas van a distinta profundidad y las varillas suben hasta dentro del brazo para que no haya z-fighting)
    ((-46, 24, 5), (26, 3, 4), fur(RED_D, BLACK)), ((-50, 26, 6), (4, 5, 2), flat(BLACK)),
    ((-44, 15, 6), (24, 9, 1), membrana), ((-38, 8, 6.5), (18, 7, 1), membrana), ((-30, 3, 6), (10, 5, 1), membrana), ((-48, 18, 6.5), (3, 6, 1), membrana),
    ((-45, 11, 5.5), (2, 14, 3), flat(RED_D)), ((-37, 6, 5.5), (2, 19, 3), flat(RED_D)), ((-28, 2.5, 5.5), (2, 22, 3), flat(RED_D))]
ALAS_ANGEL = [     # de plumas: seis hileras que se acortan hacia abajo (cada una a distinta profundidad que la vecina)
    ((-38, 26, 5.0), (18, 5, 3), pluma), ((-46, 21, 5.6), (26, 5, 3), pluma), ((-52, 16, 5.0), (32, 5, 3), pluma),
    ((-48, 11, 5.6), (28, 5, 3), pluma_punta), ((-42, 6, 5.0), (22, 5, 3), pluma_punta), ((-34, 1, 5.6), (14, 5, 3), pluma_punta)]

ACCESORIOS = {
    'sunglasses': lambda g, f: [
        cube(g, f, 'acc_sunglasses', (-15, 15, -19), (12, 11, 2), lens), cube(g, f, 'acc_sunglasses', (3, 15, -19), (12, 11, 2), lens),
        cube(g, f, 'acc_sunglasses', (-3, 21, -19), (6, 3, 2), flat(PINK)),
        cube(g, f, 'acc_sunglasses', (-19, 21, -19), (2, 3, 12), flat(PINK)), cube(g, f, 'acc_sunglasses', (17, 21, -19), (2, 3, 12), flat(PINK))],
    'beach_hat': lambda g, f: [
        cube(g, f, 'acc_beach_hat', (-24, 29, -22), (48, 2, 44), straw),
        cube(g, f, 'acc_beach_hat', (-11, 30, -10), (22, 9, 20), straw),
        cube(g, f, 'acc_beach_hat', (-11.5, 30.5, -10.5), (23, 3, 21), band(PINK, PINK_L))],
    'crown': lambda g, f: corona(g, f),
    'bow': lambda g, f: [
        cube(g, f, 'acc_bow', (-3, 30, -12), (6, 6, 5), flat(PINK)),
        cube(g, f, 'acc_bow', (-12, 30, -12), (9, 8, 4), dots(PINK, WHITE)), cube(g, f, 'acc_bow', (3, 30, -12), (9, 8, 4), dots(PINK, WHITE))],
    'headphones': lambda g, f: [   # la cabeza mide ±20: las almohadillas van por fuera, detras de los brazos (sin arco por encima)
        cube(g, f, 'acc_headphones', (20.5, 10, 0.5), (5, 15, 12), flat(PINK)), cube(g, f, 'acc_headphones', (-25.5, 10, 0.5), (5, 15, 12), flat(PINK)),
        cube(g, f, 'acc_headphones', (25.5, 11.5, 2), (2, 12, 9), flat(BLACK)), cube(g, f, 'acc_headphones', (-27.5, 11.5, 2), (2, 12, 9), flat(BLACK))],
    'scarf': lambda g, f: [
        cube(g, f, 'acc_scarf', (-19.5, 7.5, -18.5), (39, 5, 38), band(PINK, WHITE)),
        cube(g, f, 'acc_scarf', (6, 1, -19.5), (7, 11, 3), band(PINK, WHITE))],
    'halo': lambda g, f: [   # aro dorado que descansa sobre las bolitas de las antenas (se solapa con ellas: no flota suelto)
        cube(g, f, 'acc_halo', (-14.5, 53.6, -13), (29, 2, 3), glow), cube(g, f, 'acc_halo', (-14.5, 53.6, 10), (29, 2, 3), glow),
        cube(g, f, 'acc_halo', (-14, 53.6, -10), (4, 2, 20), glow), cube(g, f, 'acc_halo', (10, 53.6, -10), (4, 2, 20), glow),
        ala(g, f, ALAS_ANGEL)],
    'devil_horns': lambda g, f: [
        cube(g, f, 'acc_devil_horns', (9, 31, -13), (6, 4, 6), fur(RED, RED_D)), cube(g, f, 'acc_devil_horns', (-15, 31, -13), (6, 4, 6), fur(RED, RED_D)),
        cube(g, f, 'acc_devil_horns', (10, 35, -12), (4, 6, 4), fur(RED, RED_D)), cube(g, f, 'acc_devil_horns', (-14, 35, -12), (4, 6, 4), fur(RED, RED_D)),
        cube(g, f, 'acc_devil_horns', (11, 41, -11), (2, 5, 2), flat(RED_D)), cube(g, f, 'acc_devil_horns', (-13, 41, -11), (2, 5, 2), flat(RED_D)),
        ala(g, f, ALAS_DEMONIO)],
    'witch_hat': lambda g, f: [
        cube(g, f, 'acc_witch_hat', (-22, 32, -22), (44, 2, 44), flat(PURPLE_D)),
        cube(g, f, 'acc_witch_hat', (-12, 32.5, -12), (24, 6, 24), flat(PURPLE)),
        cube(g, f, 'acc_witch_hat', (-12.5, 33, -12.5), (25, 2, 25), band(PINK, PINK_L)),
        cube(g, f, 'acc_witch_hat', (-3, 33.5, -13.5), (6, 3, 1), flat(GOLD)),
        cube(g, f, 'acc_witch_hat', (-9, 38, -9), (18, 6, 18), flat(PURPLE)),
        cube(g, f, 'acc_witch_hat', (-6, 44, -6), (12, 6, 12), flat(PURPLE)),
        cube(g, f, 'acc_witch_hat', (-3.5, 50, -3.5), (7, 5, 7), flat(PURPLE_D), rotation=(0, 0, 18), pivot=(0, 50, 0))],
    'santa_hat': lambda g, f: [
        cube(g, f, 'acc_santa_hat', (-14, 30, -14), (28, 3, 28), dots(WHITE, (225, 225, 235))),
        cube(g, f, 'acc_santa_hat', (-11, 33, -11), (22, 6, 22), flat(RED)),
        cube(g, f, 'acc_santa_hat', (-8, 39, -8), (16, 6, 16), flat(RED)),
        cube(g, f, 'acc_santa_hat', (-5, 45, -5), (10, 5, 10), flat(RED_D), rotation=(0, 0, -22), pivot=(0, 45, 0)),
        cube(g, f, 'acc_santa_hat', (-6.5, 48, -3.5), (7, 7, 7), dots(WHITE, (225, 225, 235)), rotation=(0, 0, -22), pivot=(0, 45, 0))],
    'flower_crown': lambda g, f: [
        cube(g, f, 'acc_flower_crown', (-14, 31, -13), (28, 2, 26), leaves)] + [
        cube(g, f, 'acc_flower_crown', pos, (4, 4, 4), flower(col)) for pos, col in (
            ((-10, 32, -14.5), PINK), ((-2, 32, -15), WHITE), ((6, 32, -14.5), ORANGE), ((-16, 32, -6), WHITE), ((12.5, 32, -6), PINK),
            ((-16, 32, 3), ORANGE), ((12.5, 32, 3), WHITE), ((-8, 32, 10.5), PINK), ((4, 32, 10.5), ORANGE))],
    'pirate': lambda g, f: [
        cube(g, f, 'acc_pirate', (3, 15, -19), (12, 11, 2), flat(BLACK)),
        cube(g, f, 'acc_pirate', (-20.5, 29.5, -18), (41, 2, 36), flat(BLACK)),
        cube(g, f, 'acc_pirate', (-17, 30, -14), (34, 3, 28), flat(BLACK)),
        cube(g, f, 'acc_pirate', (-12, 33, -11), (24, 6, 22), flat(BLACK)),
        cube(g, f, 'acc_pirate', (-5, 33, -14.5), (10, 5, 3), flat((60, 52, 70))),
        cube(g, f, 'acc_pirate', (-3, 33.5, -15), (6, 4, 1), skull)],
}

def main():
    geo0 = json.load(open(f'{GEO}/michi_dramatico.geo.json'))
    img = Image.open(f'{TEX}/michi_dramatico.png').convert('RGBA')
    free = Free(img, 112)
    variantes = [{'aspects': ['gold'], 'texture': 'cobblemon:textures/pokemon/michi_dramatico/michi_dramatico_shiny.png'},
                 {'aspects': ['gold', 'shiny'], 'texture': 'cobblemon:textures/pokemon/michi_dramatico/michi_dramatico.png'}]
    for nombre, fn in ACCESORIOS.items():
        g = copy.deepcopy(geo0)
        g['minecraft:geometry'][0]['description']['identifier'] = f'geometry.michi_dramatico_{nombre}'
        fn(g, free)
        malos = [(b['name'], c['origin']) for b in g['minecraft:geometry'][0]['bones'] if b['name'].startswith('acc_') for c in b.get('cubes', []) if not c.get('rotation') and zfight.conflictos(g, c, 0.3)]
        assert not malos, f'{nombre}: caras coplanares (z-fighting) en {malos}'
        json.dump(g, open(f'{GEO}/michi_dramatico_{nombre}.geo.json', 'w'), separators=(',', ':'))
        variantes.append({'aspects': [f'{nombre}-accessory'], 'model': f'cobblemon:michi_dramatico_{nombre}.geo'})
    img.save(f'{TEX}/michi_dramatico.png')
    oro = gold_shiny(img)
    # accesorios que se pierden sobre el pelaje dorado (la corona es dorada): en la textura dorada pasan a plata
    px = oro.load()
    for x, y, w, h in REGIONES.get('acc_crown', []):
        for i in range(w):
            for j in range(h):
                if 0 <= x + i < oro.width and 0 <= y + j < oro.height:
                    r, g, b, a = px[x + i, y + j]
                    if a and not (r > 200 and g < 130): lu = 0.299 * r + 0.587 * g + 0.114 * b; t = min(1.0, lu / 235.0); px[x + i, y + j] = mix((120, 126, 146), (236, 240, 252), t) + (a,)
    oro.save(f'{TEX}/michi_dramatico_shiny.png')
    rd = f'{B}/bedrock/pokemon/resolvers/michi_dramatico'
    json.dump({'species': 'cobblemon:gatitoalien', 'order': 1, 'variations': variantes}, open(f'{rd}/1_michi_dramatico_accessories.json', 'w'), indent=2)
    data = os.path.join(OUT, 'data/cobblemon')
    os.makedirs(f'{data}/species_features', exist_ok=True); os.makedirs(f'{data}/species_feature_assignments', exist_ok=True)
    json.dump({'type': 'choice', 'keys': ['accessory'], 'default': 'none', 'choices': ['none'] + list(ACCESORIOS), 'isAspect': True, 'aspectFormat': '{{choice}}-accessory'}, open(f'{data}/species_features/accessory.json', 'w'))
    json.dump({'pokemon': ['gatitoalien'], 'features': ['accessory']}, open(f'{data}/species_feature_assignments/accessory.json', 'w'))
    json.dump({'keys': ['gold'], 'type': 'flag', 'isAspect': True, 'default': False}, open(f'{data}/species_features/gold.json', 'w'))
    json.dump({'pokemon': ['gatitoalien'], 'features': ['gold']}, open(f'{data}/species_feature_assignments/gold.json', 'w'))
    # formas Pokemon (especie y entrada del Pokedex)
    sp_path = os.path.join(OUT, 'data/cobblemon/species/custom/gatitoalien.json')
    sp = json.load(open(sp_path, encoding='utf-8'))
    NOMBRES_STAT = ('hp', 'attack', 'defence', 'special_attack', 'special_defence', 'speed')
    formas = []
    for n, asp, acc, dorado in FORMAS:
        r = ROLES[acc]
        assert sum(r['stats']) == 470, n
        moves = list(sp['moves'])
        for m in r['moves']:
            if m not in moves: moves.append(m)
        formas.append({'name': n, 'aspects': asp, 'primaryType': 'fairy', 'secondaryType': r['tipo'],
                       'baseStats': dict(zip(NOMBRES_STAT, r['stats'])), 'evYield': {k: r['ev'].get(k, 0) for k in NOMBRES_STAT},
                       'abilities': [abid(r), 'h:drama'], 'moves': moves,
                       'pokedex': [f"emi_plush.species.gatitoalien_{acc or 'gold'}.desc"]})
    sp['forms'] = formas
    json.dump(sp, open(sp_path, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    dx_path = os.path.join(OUT, 'data/emi_plush/dex_entries/pokemon/gatitoalien.json')
    dx = json.load(open(dx_path, encoding='utf-8'))
    dx['forms'] = [{'displayForm': 'Normal', 'unlockForms': ['Normal']}] + [{'displayForm': n, 'unlockForms': [n]} for n, *_ in FORMAS]
    json.dump(dx, open(dx_path, 'w', encoding='utf-8'), indent=2)
    # habilidades propias (scripts de Showdown) y textos
    ab_dir = os.path.join(OUT, 'data/emi_plush/abilities'); os.makedirs(ab_dir, exist_ok=True)
    por_clave = {r['ab']: abid(r) for r in ROLES.values()}
    for f in os.listdir(ab_dir):                       # fuera las habilidades de versiones anteriores (ids en ingles); drama se queda
        if f.endswith('.js') and f != 'drama.js': os.remove(f'{ab_dir}/{f}')
    for clave, js in JS.items():
        assert '//' not in js, clave
        open(f'{ab_dir}/{por_clave[clave]}.js', 'w', encoding='utf-8').write(js + '\n')
    for code, es in (('en_us', False), ('es_es', True), ('es_mx', True)):
        p = os.path.join(OUT, f'assets/emi_plush/lang/{code}.json')
        lang = json.load(open(p, encoding='utf-8'))
        for k in [k for k in lang if k.startswith('cobblemon.ability.') and not k.startswith('cobblemon.ability.drama')]: del lang[k]
        for acc, r in ROLES.items():
            lang[f"cobblemon.ability.{abid(r)}"] = r['ab_es'] if es else r['ab_en']
            lang[f"cobblemon.ability.{abid(r)}.desc"] = r['d_es'] if es else r['d_en']
            lang[f"emi_plush.species.gatitoalien_{acc or 'gold'}.desc"] = r['dex_es'] if es else r['dex_en']
        # nombres de las formas en el Pokedex (clave = nombre de la forma en minusculas)
        nom = {a: (e if es else n) for a, _f, e, n in ACC}
        for fn, asp, acc, dorado in FORMAS:
            base = nom[acc] if acc else ''
            txt = ((('Dorado ' if es else 'Golden ') if dorado else '') + base).strip()
            if es and dorado and acc and base.startswith('con '): txt = 'Dorado ' + base
            lang[f"cobblemon.ui.pokedex.info.form.{fn.lower()}"] = (txt[:1].upper() + txt[1:]) if txt else ('Dorado' if es else 'Golden')
        json.dump(lang, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    print('extras de GatitoAlien ok:', ', '.join(ACCESORIOS))

main()
