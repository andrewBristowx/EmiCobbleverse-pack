#!/usr/bin/env python3
"""GatitoAlien: version shiny DORADA y accesorios (aspecto `accessory`: sunglasses, beach_hat, crown, bow, headphones, scarf).
Se ejecuta despues de gen_pokemon.py. Cada accesorio es un modelo aparte (copia del modelo base + cubos nuevos en el hueso `head`)
que usa la misma textura (los accesorios estan pintados en la zona libre de abajo) y se elige con el aspecto `<accesorio>-accessory`.
Uso: python3 gen_michi_extras.py <carpeta resources>"""
import json, os, sys, copy
from PIL import Image
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from emi_lib import box_rects

OUT = sys.argv[1]
B = os.path.join(OUT, 'assets/cobblemon')
GEO = f'{B}/bedrock/pokemon/models/michi_dramatico'
TEX = f'{B}/textures/pokemon/michi_dramatico'

def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))
BLACK = (30, 28, 38); PINK = (240, 98, 156); PINK_L = (250, 170, 205); GOLD = (240, 196, 60); GOLD_D = (190, 140, 30); STRAW = (232, 200, 120); STRAW_D = (205, 170, 90)
WHITE = (250, 247, 250)

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

def cube(geo, free, name, origin, size, painter, rotation=None, pivot=None):
    sx, sy, sz = size
    W, H = 2 * sz + 2 * sx, sz + sy
    ux, uy = free.alloc(W, H)
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
}

def main():
    geo0 = json.load(open(f'{GEO}/michi_dramatico.geo.json'))
    img = Image.open(f'{TEX}/michi_dramatico.png').convert('RGBA')
    free = Free(img, 112)
    variantes = []
    for nombre, fn in ACCESORIOS.items():
        g = copy.deepcopy(geo0)
        g['minecraft:geometry'][0]['description']['identifier'] = f'geometry.michi_dramatico_{nombre}'
        fn(g, free)
        json.dump(g, open(f'{GEO}/michi_dramatico_{nombre}.geo.json', 'w'), separators=(',', ':'))
        variantes.append({'aspects': [f'{nombre}-accessory'], 'model': f'cobblemon:michi_dramatico_{nombre}.geo'})
    img.save(f'{TEX}/michi_dramatico.png')
    gold_shiny(img).save(f'{TEX}/michi_dramatico_shiny.png')
    rd = f'{B}/bedrock/pokemon/resolvers/michi_dramatico'
    json.dump({'species': 'cobblemon:gatitoalien', 'order': 1, 'variations': variantes}, open(f'{rd}/1_michi_dramatico_accessories.json', 'w'), indent=2)
    data = os.path.join(OUT, 'data/cobblemon')
    os.makedirs(f'{data}/species_features', exist_ok=True); os.makedirs(f'{data}/species_feature_assignments', exist_ok=True)
    json.dump({'type': 'choice', 'keys': ['accessory'], 'default': 'none', 'choices': ['none'] + list(ACCESORIOS), 'isAspect': True, 'aspectFormat': '{{choice}}-accessory'}, open(f'{data}/species_features/accessory.json', 'w'))
    json.dump({'pokemon': ['gatitoalien'], 'features': ['accessory']}, open(f'{data}/species_feature_assignments/accessory.json', 'w'))
    print('extras de GatitoAlien ok:', ', '.join(ACCESORIOS))

main()
