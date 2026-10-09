#!/usr/bin/env python3
"""Genera la variante "Emi" de Happiny, Chansey y Blissey (aspecto `emi`): pelo largo plateado con mechones rosas, ojos de dos colores
(naranja y magenta), frunces y detalles en negro, luna rosa y toques dorados, como la Emi del canal. Parte de los modelos y texturas de Cobblemon.
Uso: COBBLEMON_JAR=<jar> gen_emi_poke.py <carpeta resources>"""
import sys, os, json, math, random
from PIL import Image
from emi_lib import *

OUT = sys.argv[1] if len(sys.argv) > 1 else 'resources'
Z = cobblemon_jar()
random.seed(7)

# paleta de Emi
SILVER = (214, 217, 226); SILVER_D = (176, 181, 195); SILVER_L = (240, 241, 246)
BLACK = (34, 31, 42); BLACK_L = (58, 54, 70)
PINK_H = (236, 92, 150); PINK_L = (247, 160, 200); PINK_P = (252, 205, 224)
GOLD = (236, 190, 60); GOLD_D = (190, 140, 30)
ORANGE_L = (255, 206, 120); ORANGE = (236, 128, 24)
MAGENTA_L = (255, 170, 236); MAGENTA = (196, 40, 170)
SKIN = (250, 224, 232)

def mix(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def hair_painter(tip=PINK_L, tip_from=0.78, vertical=True):
    """Pelo plateado con hebras verticales; la punta se tiñe de rosa."""
    def f(face, i, j, w, h):
        t = j / max(1, h - 1)
        k = (i * 7 + (i // 2) * 3) % 5
        c = mix(SILVER, SILVER_D, 0.18 * k / 4 + 0.10 * (i % 2))
        if face in ('up', 'down'): c = mix(SILVER, SILVER_L, 0.4)
        if tip and t > tip_from and face not in ('up', 'down'):
            c = mix(c, tip, min(1.0, (t - tip_from) / (1 - tip_from) * 1.6))
        return c + (255,)
    return f

def solid(c): return lambda face, i, j, w, h: c + (255,)

def finish_species(name, folder, geo, atlas, extra_note=''):
    geo, img = finish(geo, atlas, f'geometry.{name}_emi')
    mdir = f'{OUT}/assets/cobblemon/bedrock/pokemon/models/{folder}'
    tdir = f'{OUT}/assets/cobblemon/textures/pokemon/{folder}'
    rdir = f'{OUT}/assets/cobblemon/bedrock/pokemon/resolvers/{folder}'
    for d in (mdir, tdir, rdir): os.makedirs(d, exist_ok=True)
    json.dump(geo, open(f'{mdir}/{name}_emi.geo.json', 'w'), separators=(',', ':'))
    img.save(f'{tdir}/{name}_emi.png')
    # variante brillante: mismo modelo, mechones y detalles en otros tonos
    sh = img.copy()
    sp = sh.load()
    for x in range(sh.width):
        for y in range(sh.height):
            r, g, b, a = sp[x, y]
            if a: sp[x, y] = (r, int(g * 0.85), min(255, int(b * 1.1)), a)   # leve giro hacia el violeta
    sh.save(f'{tdir}/{name}_emi_shiny.png')
    res = {'species': f'cobblemon:{name}', 'order': 1, 'variations': [
        {'aspects': ['emi'], 'poser': f'cobblemon:{name}', 'model': f'cobblemon:{name}_emi.geo',
         'texture': f'cobblemon:textures/pokemon/{folder}/{name}_emi.png', 'layers': []},
        {'aspects': ['emi', 'shiny'], 'texture': f'cobblemon:textures/pokemon/{folder}/{name}_emi_shiny.png'}]}
    json.dump(res, open(f'{rdir}/1_{name}_emi.json', 'w'), indent=2)

def emi_eyes(geo, img):
    px = img.load()
    for nm, (l, d) in {'left_eye': (MAGENTA_L, MAGENTA), 'right_eye': (ORANGE_L, ORANGE)}.items():
        c = bone(geo, nm)['cubes'][0]
        x, y, w, h = cube_rects(c)['north']
        px[x, y] = l + (255,); px[x, y + 1] = d + (255,)

def pouch_moon(geo, img):
    pc = bone(geo, 'pouch')['cubes'][0]
    fx, fy, fw, fh = cube_rects(pc)['north']
    px = img.load()
    for (dx, dy) in [(3, 0), (4, 0), (3, 1), (3, 2), (3, 3), (4, 3)]:
        if dx < fw and dy < fh: px[fx + dx, fy + dy] = PINK_H + (255,)

# ------------------------------------------------------------------------------------------------ Blissey
def blissey():
    geo, tex = load_base(Z, '0242_blissey', 'blissey')
    atlas = Atlas(tex, 128)
    img = atlas.img
    # --- colores
    body = ['torso', 'left_arm', 'right_arm', 'left_foot', 'right_foot', 'left_toes', 'right_toes']
    retint(img, bone_rects(geo, ['torso']), SKIN, keep=0.9)
    retint(img, bone_rects(geo, ['left_arm', 'right_arm']), PINK_L, keep=0.9)
    retint(img, bone_rects(geo, ['left_tendril_front', 'left_tendril_middle', 'left_tendril_back', 'right_tendril_front', 'right_tendril_middle', 'right_tendril_back']), PINK_H, keep=0.9)
    retint(img, bone_rects(geo, ['left_frill1', 'left_frill2', 'left_frill3', 'left_frill4', 'right_frill1', 'right_frill2', 'right_frill3', 'right_frill4', 'left_arm_frills', 'right_arm_frills']), BLACK, keep=0.8, floor=0.6)
    retint(img, bone_rects(geo, ['left_foot', 'right_foot', 'left_toes', 'right_toes']), BLACK, keep=0.8, floor=0.6)
    retint(img, bone_rects(geo, ['pouch']), BLACK, keep=0.8, floor=0.6)
    pouch_moon(geo, img)
    emi_eyes(geo, img)
    # --- pelo
    # parte de atras: cae por la espalda hasta cerca de los pies
    add_cube(geo, atlas, 'hair_back', 'torso', [0, 16, 9.5], [-10.5, 4, 9.5], [21, 25, 3], hair_painter())
    # tapa superior y laterales (el pelo cubre la parte alta de la cabeza)
    add_cube(geo, atlas, 'hair_top', 'torso', [0, 28, 0], [-10.5, 27.5, -10], [21, 3, 20], hair_painter(tip=None))
    # flequillo: mechones que caen sobre la frente con raya al medio
    for (x0, w_, hh) in [(-10.5, 4, 2), (-6.5, 4, 3), (-2.5, 2, 1), (0.5, 2, 1), (2.5, 4, 3), (6.5, 4, 2)]:
        add_cube(geo, atlas, 'bangs', 'torso', [0, 27, -10], [x0, 28 - hh, -11], [w_, hh, 1], hair_painter(tip=None))
    # mechones largos a los lados de la cara, sobre el pecho
    for sx in (-1, 1):
        x0 = 7.5 if sx > 0 else -10.5
        add_cube(geo, atlas, 'hair_lock_l' if sx > 0 else 'hair_lock_r', 'torso', [0, 24, -10], [x0, 7, -11.5], [3, 21, 1], hair_painter(tip_from=0.7))
    return geo, atlas


# ------------------------------------------------------------------------------------------------ Chansey
def chansey():
    geo, tex = load_base(Z, '0113_chansey', 'chansey')
    atlas = Atlas(tex, 128)
    img = atlas.img
    retint(img, bone_rects(geo, ['torso', 'head']), SKIN, keep=0.9)
    retint(img, bone_rects(geo, ['left_arm', 'right_arm']), PINK_L, keep=0.9)
    tend = [n + s_ for n in ('left_tendril_front', 'left_tendril_middle', 'left_tendril_back', 'right_tendril_front', 'right_tendril_middle', 'right_tendril_back') for s_ in ('', '2')]
    retint(img, bone_rects(geo, [t for t in tend if t in [b['name'] for b in bones(geo)]]), PINK_H, keep=0.9)
    retint(img, bone_rects(geo, ['left_foot', 'right_foot', 'left_toes', 'right_toes', 'pouch']), BLACK, keep=0.8, floor=0.6)
    retint(img, bone_rects(geo, ['tail1', 'tail2']), PINK_L, keep=0.9)
    pouch_moon(geo, img)
    emi_eyes(geo, img)
    # pelo: tapa, melena por detras (la parte baja con puntas rosas), flequillo y mechones laterales
    add_cube(geo, atlas, 'hair_top', 'head', [0, 22, 0], [-8, 21.5, -8], [16, 3, 16], hair_painter(tip=None))
    add_cube(geo, atlas, 'hair_back', 'torso', [0, 15, 8], [-8, 15, 7.5], [16, 8, 3], hair_painter(tip=None))
    add_cube(geo, atlas, 'hair_back', 'torso', [0, 15, 8], [-8, 6, 8.6], [16, 9, 3], hair_painter(tip_from=0.55))
    for (x0, w_, hh) in [(-8, 4, 2), (-4, 3, 2), (-1.5, 1, 1), (0.5, 1, 1), (1, 3, 2), (4, 4, 2)]:
        pass
    for (x0, w_, hh) in [(-8, 4, 2), (-4, 3, 2), (-1.5, 1, 1), (0.5, 1, 1), (1.0, 3, 2), (4, 4, 2)]:
        add_cube(geo, atlas, 'bangs', 'head', [0, 22, -8], [x0, 22.5 - hh, -8.6], [w_, hh, 1], hair_painter(tip=None))
    for sx in (-1, 1):
        x0 = 7.0 if sx > 0 else -10.0
        add_cube(geo, atlas, 'hair_lock_l' if sx > 0 else 'hair_lock_r', 'head', [0, 22, -8], [x0, 6, -9.2], [3, 16, 1], hair_painter(tip_from=0.6))
    return geo, atlas

# ------------------------------------------------------------------------------------------------ Happiny
def happiny():
    geo, tex = load_base(Z, '0440_happiny', 'happiny')
    atlas = Atlas(tex, 64)
    img = atlas.img
    retint(img, bone_rects(geo, ['head']), SKIN, keep=0.9)
    retint(img, bone_rects(geo, ['torso']), BLACK, keep=0.8, floor=0.6)
    retint(img, bone_rects(geo, ['left_arm', 'right_arm']), PINK_L, keep=0.9)
    retint(img, bone_rects(geo, ['left_foot', 'right_foot']), BLACK, keep=0.8, floor=0.6)
    retint(img, bone_rects(geo, ['bangs']), SILVER, keep=0.8)
    retint(img, bone_rects(geo, ['ponytail']), SILVER, keep=0.7)
    retint(img, bone_rects(geo, ['hairband']), PINK_H, keep=0.9)
    emi_eyes(geo, img)
    add_cube(geo, atlas, 'hair_back', 'head', [0, 7, 4.5], [-4.5, 3.5, 4.6], [9, 10, 2], hair_painter(tip_from=0.6))
    for sx in (-1, 1):
        x0 = 4.4 if sx > 0 else -6.4
        add_cube(geo, atlas, 'hair_lock_l' if sx > 0 else 'hair_lock_r', 'head', [0, 13, -3], [x0, 3, -3.6], [2, 9, 1], hair_painter(tip_from=0.6))
    return geo, atlas

if __name__ == '__main__':
    only = sys.argv[2].split(',') if len(sys.argv) > 2 else ['happiny', 'chansey', 'blissey']
    if 'happiny' in only:
        g, a = happiny(); finish_species('happiny', '0440_happiny', g, a)
    if 'chansey' in only:
        g, a = chansey(); finish_species('chansey', '0113_chansey', g, a)
    if 'blissey' in only:
        g, a = blissey(); finish_species('blissey', '0242_blissey', g, a)
    print('listo')
