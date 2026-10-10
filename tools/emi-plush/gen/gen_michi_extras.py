#!/usr/bin/env python3
"""GatitoAlien: version shiny DORADA y accesorios (aspecto `accessory`: sunglasses, beach_hat, crown, bow, headphones, scarf).
Se ejecuta despues de gen_pokemon.py. Cada accesorio es un modelo aparte (copia del modelo base + cubos nuevos en el hueso `head`)
que usa la misma textura (los accesorios estan pintados en la zona libre de abajo) y se elige con el aspecto `<accesorio>-accessory`.
Uso: python3 gen_michi_extras.py <carpeta resources>"""
import json, os, sys, copy
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from emi_lib import box_rects
from michi_forms import FORMAS

OUT = sys.argv[1]
B = os.path.join(OUT, 'assets/cobblemon')
GEO = f'{B}/bedrock/pokemon/models/michi_dramatico'
TEX = f'{B}/textures/pokemon/michi_dramatico'

def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
BLACK = (30, 28, 38); PINK = (240, 98, 156); PINK_L = (250, 170, 205); GOLD = (240, 196, 60); GOLD_D = (190, 140, 30); STRAW = (232, 200, 120); STRAW_D = (205, 170, 90)
WHITE = (250, 247, 250)
RED = (214, 52, 62); RED_D = (156, 30, 44); GREEN = (96, 170, 84); GREEN_D = (62, 130, 62); PURPLE = (92, 56, 140); PURPLE_D = (58, 34, 98)
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
    b['cubes'].append(c)

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

ACCESORIOS = {
    'sunglasses': lambda g, f: [
        cube(g, f, 'acc_sunglasses', (-15, 15, -19), (12, 11, 2), lens), cube(g, f, 'acc_sunglasses', (3, 15, -19), (12, 11, 2), lens),
        cube(g, f, 'acc_sunglasses', (-3, 21, -19), (6, 3, 2), flat(PINK)),
        cube(g, f, 'acc_sunglasses', (-19, 21, -19), (2, 3, 12), flat(PINK)), cube(g, f, 'acc_sunglasses', (17, 21, -19), (2, 3, 12), flat(PINK))],
    'beach_hat': lambda g, f: [
        cube(g, f, 'acc_beach_hat', (-24, 29, -22), (48, 2, 44), straw),
        cube(g, f, 'acc_beach_hat', (-11, 30, -10), (22, 9, 20), straw),
        cube(g, f, 'acc_beach_hat', (-11.5, 30.5, -10.5), (23, 3, 21), band(PINK, PINK_L))],
    'crown': lambda g, f: [
        cube(g, f, 'acc_crown', (-13, 30, -11), (26, 4, 22), golden),
        cube(g, f, 'acc_crown', (-13, 34, -11), (5, 6, 5), golden), cube(g, f, 'acc_crown', (8, 34, -11), (5, 6, 5), golden),
        cube(g, f, 'acc_crown', (-13, 34, 6), (5, 6, 5), golden), cube(g, f, 'acc_crown', (8, 34, 6), (5, 6, 5), golden),
        cube(g, f, 'acc_crown', (-2.5, 34, -11), (5, 7, 5), golden),
        cube(g, f, 'acc_crown', (-1.5, 31, -11.6), (3, 3, 1), flat(PINK))],
    'bow': lambda g, f: [
        cube(g, f, 'acc_bow', (-3, 30, -12), (6, 6, 5), flat(PINK)),
        cube(g, f, 'acc_bow', (-12, 30, -12), (9, 8, 4), dots(PINK, WHITE)), cube(g, f, 'acc_bow', (3, 30, -12), (9, 8, 4), dots(PINK, WHITE))],
    'headphones': lambda g, f: [
        cube(g, f, 'acc_headphones', (-17, 30, -3), (34, 2, 5), flat(BLACK)),
        cube(g, f, 'acc_headphones', (-21, 12, -7), (5, 14, 14), flat(PINK)), cube(g, f, 'acc_headphones', (16, 12, -7), (5, 14, 14), flat(PINK)),
        cube(g, f, 'acc_headphones', (-19, 26, -3), (2, 5, 5), flat(BLACK)), cube(g, f, 'acc_headphones', (17, 26, -3), (2, 5, 5), flat(BLACK)),
        cube(g, f, 'acc_headphones', (-22, 15, -4), (1, 8, 8), flat(BLACK)), cube(g, f, 'acc_headphones', (21, 15, -4), (1, 8, 8), flat(BLACK))],
    'scarf': lambda g, f: [
        cube(g, f, 'acc_scarf', (-19.5, 8, -18.5), (39, 5, 38), band(PINK, WHITE)),
        cube(g, f, 'acc_scarf', (6, 0.5, -19.5), (7, 11, 2), band(PINK, WHITE))],
    'halo': lambda g, f: [   # aro dorado que descansa sobre las bolitas de las antenas (se solapa con ellas: no flota suelto)
        cube(g, f, 'acc_halo', (-14, 53.6, -13), (28, 2, 3), glow), cube(g, f, 'acc_halo', (-14, 53.6, 10), (28, 2, 3), glow),
        cube(g, f, 'acc_halo', (-14, 53.6, -10), (4, 2, 20), glow), cube(g, f, 'acc_halo', (10, 53.6, -10), (4, 2, 20), glow),
        cube(g, f, 'acc_halo', (-14, 54.8, -13), (28, 1, 1), flat((255, 250, 205))), cube(g, f, 'acc_halo', (-14, 54.8, 12), (28, 1, 1), flat((255, 250, 205)))],
    'devil_horns': lambda g, f: [
        cube(g, f, 'acc_devil_horns', (9, 31, -13), (6, 4, 6), fur(RED, RED_D)), cube(g, f, 'acc_devil_horns', (-15, 31, -13), (6, 4, 6), fur(RED, RED_D)),
        cube(g, f, 'acc_devil_horns', (10, 35, -12), (4, 6, 4), fur(RED, RED_D)), cube(g, f, 'acc_devil_horns', (-14, 35, -12), (4, 6, 4), fur(RED, RED_D)),
        cube(g, f, 'acc_devil_horns', (11, 41, -11), (2, 5, 2), flat(RED_D)), cube(g, f, 'acc_devil_horns', (-13, 41, -11), (2, 5, 2), flat(RED_D))],
    'witch_hat': lambda g, f: [
        cube(g, f, 'acc_witch_hat', (-22, 30, -22), (44, 2, 44), flat(PURPLE_D)),
        cube(g, f, 'acc_witch_hat', (-12, 32, -12), (24, 6, 24), flat(PURPLE)),
        cube(g, f, 'acc_witch_hat', (-12.5, 32.5, -12.5), (25, 2, 25), band(PINK, PINK_L)),
        cube(g, f, 'acc_witch_hat', (-3, 32.5, -13.5), (6, 3, 1), flat(GOLD)),
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
        cube(g, f, 'acc_pirate', (-20.5, 26, -16.5), (41, 2, 33), flat(BLACK)),
        cube(g, f, 'acc_pirate', (-20, 31, -14), (40, 2, 28), flat(BLACK)),
        cube(g, f, 'acc_pirate', (-12, 33, -11), (24, 6, 22), flat(BLACK)),
        cube(g, f, 'acc_pirate', (-5, 33, -14.5), (10, 5, 3), flat((60, 52, 70))),
        cube(g, f, 'acc_pirate', (-3, 34, -15), (6, 4, 1), skull)],
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
                    if a: lu = 0.299 * r + 0.587 * g + 0.114 * b; t = min(1.0, lu / 235.0); px[x + i, y + j] = mix((120, 126, 146), (236, 240, 252), t) + (a,)
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
    sp['forms'] = [{'name': n, 'aspects': asp, 'pokedex': ['emi_plush.species.gatitoalien.desc']} for n, asp, _, _ in FORMAS]
    json.dump(sp, open(sp_path, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
    dx_path = os.path.join(OUT, 'data/emi_plush/dex_entries/pokemon/gatitoalien.json')
    dx = json.load(open(dx_path, encoding='utf-8'))
    dx['forms'] = [{'displayForm': 'Normal', 'unlockForms': ['Normal']}] + [{'displayForm': n, 'unlockForms': [n]} for n, *_ in FORMAS]
    json.dump(dx, open(dx_path, 'w', encoding='utf-8'), indent=2)
    print('extras de GatitoAlien ok:', ', '.join(ACCESORIOS))

main()
