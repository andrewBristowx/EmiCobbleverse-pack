#!/usr/bin/env python3
"""Genera los archivos del local de nivel 6 a 10 (0.20.0): 8 estaciones nuevas (4 cajas de ingredientes y 4 maquinas), 5 ingredientes y 15 platos.
Todo se dibuja aqui con Pillow (pixel-art de 16x16 y modelos de cubos como el resto del mod, nada copiado). No toca lo que genera generar.py.
Uso: python3 assets/generar_nivel10.py (desde tools/emi-cafe)."""
import json, os, math, random
from PIL import Image, ImageDraw

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'emi_cafe')
T_ITEM = os.path.join(RAIZ, 'textures', 'item'); T_BLOQ = os.path.join(RAIZ, 'textures', 'block')
M_ITEM = os.path.join(RAIZ, 'models', 'item'); M_BLOQ = os.path.join(RAIZ, 'models', 'block'); BS = os.path.join(RAIZ, 'blockstates')


def C(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def w(ruta, obj):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write('\n')


def lienzo(): return Image.new('RGBA', (16, 16), (0, 0, 0, 0))


def disco(im, cx, cy, rx, ry, rel, borde=None):
    d = ImageDraw.Draw(im)
    d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=rel, outline=borde)


def pt(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height: im.putpixel((x, y), c)


# ------------------------------------------------------------------ ingredientes
def tomate():
    im = lienzo(); disco(im, 8, 9, 5, 4.5, C('d8342c'), C('7a1414')); disco(im, 7, 7, 1.5, 1, C('f57a6e'))
    d = ImageDraw.Draw(im); d.polygon([(6, 5), (8, 3), (10, 5), (8, 6)], fill=C('3e9d46'), outline=C('1f5a26')); pt(im, 8, 2, C('3e9d46')); return im


def pescado():
    im = lienzo(); d = ImageDraw.Draw(im)
    disco(im, 7, 8, 5, 3.2, C('7fa8c9'), C('2d4a66')); d.polygon([(11, 8), (15, 4), (15, 12)], fill=C('5d86a8'), outline=C('2d4a66'))
    disco(im, 7, 9, 3.5, 1.2, C('d6e6f2')); pt(im, 4, 7, C('ffffff')); pt(im, 4, 7, C('101820')); d.line([6, 6, 6, 10], fill=C('4d7396')); return im


def cacao():
    im = lienzo(); disco(im, 8, 8, 3.6, 6, C('8a4b25'), C('3e1f0c')); d = ImageDraw.Draw(im)
    for x in (6, 8, 10): d.line([x, 4, x, 12], fill=C('b0683a'))
    pt(im, 8, 1, C('3e1f0c')); pt(im, 8, 2, C('3e1f0c')); return im


def miel():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([4, 6, 11, 14], fill=C('f0b429'), outline=C('8a5a0a')); d.rectangle([5, 7, 6, 13], fill=C('ffd869'))
    d.rectangle([4, 3, 11, 5], fill=C('9a6a3a'), outline=C('4e3018')); d.rectangle([6, 9, 9, 11], fill=C('fff2c2'))
    pt(im, 7, 10, C('c98a14')); return im


# ------------------------------------------------------------------ platos y cosas cocinadas
def pescado_asado():
    im = tomate_base_plato(); d = ImageDraw.Draw(im)
    disco(im, 7, 8, 5, 3, C('c9833c'), C('5a3314')); d.polygon([(11, 8), (14, 5), (14, 11)], fill=C('a8672a'), outline=C('5a3314'))
    for x in (5, 7, 9): d.line([x, 6, x + 1, 10], fill=C('5a3314'))
    pt(im, 4, 7, C('101010')); return im


def tomate_base_plato():
    im = lienzo(); disco(im, 8, 12, 7, 2.5, C('f4f4fa'), C('9aa0b8')); return im


def bistec():
    im = tomate_base_plato(); d = ImageDraw.Draw(im)
    disco(im, 8, 8, 5.5, 3.6, C('b5603a'), C('4a2010')); disco(im, 7, 7, 3, 1.5, C('d98860'))
    for k in (-2, 0, 2): d.line([5 + k, 5, 8 + k, 10], fill=C('5a2a14'))
    d.rectangle([12, 7, 14, 8], fill=C('f0e6d0'), outline=C('8a7a5a')); return im


def chocolate_fundido():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([3, 7, 12, 13], fill=C('5a5f78'), outline=C('2a2d3e')); d.rectangle([4, 6, 11, 8], fill=C('5b2c14'))
    d.line([5, 7, 10, 7], fill=C('8a4a24')); d.rectangle([5, 9, 6, 11], fill=C('5b2c14')); d.rectangle([10, 8, 11, 10], fill=C('5b2c14'))
    d.rectangle([12, 8, 14, 9], fill=C('9aa0b8')); return im


def sopa_tomate():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.pieslice([2, 4, 14, 16], 0, 180, fill=C('f4f4fa'), outline=C('7a80a0')); disco(im, 8, 8, 5.5, 1.8, C('d8432c'), C('8a2418'))
    pt(im, 6, 8, C('f58a6e')); pt(im, 10, 8, C('3e9d46')); pt(im, 9, 7, C('3e9d46')); return im


def crepes():
    im = tomate_base_plato(); d = ImageDraw.Draw(im)
    d.polygon([(2, 11), (8, 4), (14, 11)], fill=C('f0cf8a'), outline=C('a8782a')); d.line([8, 5, 8, 11], fill=C('d6a95a')); d.line([5, 8, 8, 11], fill=C('d6a95a'))
    pt(im, 8, 4, C('a8782a')); return im


def onigiri():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.polygon([(8, 2), (2, 13), (14, 13)], fill=C('fafafa'), outline=C('9aa0b8')); d.rectangle([5, 10, 10, 13], fill=C('1d2a22'))
    disco(im, 8, 7, 1.5, 1.2, C('f0867a')); pt(im, 6, 8, C('e8e8e8')); return im


def bayas_chocolate():
    im = tomate_base_plato()
    for (x, y, c) in ((5, 8, '3d56c8'), (9, 8, 'd8342c'), (7, 6, '3d56c8'), (11, 10, 'd8342c'), (4, 11, 'd8342c')):
        disco(im, x, y, 2, 2, C('5b2c14'), C('2e1408')); pt(im, x - 1, y - 1, C(c))
    return im


def pizza_margarita():
    im = lienzo(); disco(im, 8, 8, 6.5, 6.5, C('f0c070'), C('8a5a1a')); d = ImageDraw.Draw(im)
    for (x, y) in ((5, 6), (9, 5), (10, 9), (6, 10), (8, 8)): disco(im, x, y, 1.3, 1.3, C('d8342c'))
    for (x, y) in ((7, 5), (11, 7), (5, 9), (9, 11)): pt(im, x, y, C('fafafa')); pt(im, x + 1, y, C('fafafa'))
    for (x, y) in ((6, 7), (10, 7), (8, 10)): pt(im, x, y, C('3e9d46'))
    return im


def crepes_miel():
    im = crepes(); d = ImageDraw.Draw(im)
    d.line([4, 9, 12, 9], fill=C('f0b429')); d.line([6, 7, 6, 11], fill=C('f0b429')); d.line([10, 8, 10, 12], fill=C('f0b429')); pt(im, 6, 12, C('f0b429')); return im


def pan_miel():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rounded_rectangle([2, 5, 13, 13], 3, fill=C('d9a45a'), outline=C('7a4c14')); d.rectangle([4, 6, 11, 8], fill=C('f0b429'))
    for x in (4, 7, 10): d.line([x, 8, x, 11], fill=C('f0b429'))
    pt(im, 4, 6, C('ffe08a')); return im


def bistec_papas():
    im = tomate_base_plato(); d = ImageDraw.Draw(im)
    disco(im, 6, 8, 4.5, 3.2, C('b5603a'), C('4a2010')); disco(im, 5, 7, 2.5, 1.2, C('d98860'))
    for x in (10, 11, 12, 13): d.rectangle([x, 4 + (x % 2), x, 11], fill=C('f2c84a'), outline=None)
    d.line([4, 6, 6, 10], fill=C('5a2a14')); return im


def cesta_vapor(im):
    d = ImageDraw.Draw(im)
    d.rectangle([2, 8, 13, 14], fill=C('d2a45a'), outline=C('7a5018')); d.line([2, 11, 13, 11], fill=C('9a6a28'))
    for x in (4, 7, 10): d.line([x, 8, x, 14], fill=C('9a6a28'))
    d.rectangle([1, 7, 14, 8], fill=C('b8863c'), outline=C('7a5018')); return im


def pescado_vapor():
    im = lienzo(); cesta_vapor(im); d = ImageDraw.Draw(im)
    disco(im, 8, 5, 4, 2.4, C('cfe0ee'), C('5a7a96')); d.polygon([(11, 5), (14, 3), (14, 7)], fill=C('9ab6cc')); pt(im, 5, 4, C('101820')); return im


def bollos_vapor():
    im = lienzo(); cesta_vapor(im)
    for x in (5, 10): disco(im, x, 5, 3, 3, C('fafafa'), C('b0b4c8')); pt(im, x - 1, 4, C('ffffff'))
    return im


def pastel_chocolate():
    im = tomate_base_plato(); d = ImageDraw.Draw(im)
    d.rectangle([3, 6, 12, 11], fill=C('6a3418'), outline=C('2e1408')); d.rectangle([3, 6, 12, 7], fill=C('3e1c0a')); d.line([3, 9, 12, 9], fill=C('f4e6c8'))
    disco(im, 8, 5, 1.5, 1.5, C('d8342c'), C('7a1414')); return im


def helado_chocolate():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.polygon([(4, 9), (12, 9), (8, 15)], fill=C('d9a45a'), outline=C('7a4c14')); disco(im, 8, 7, 4, 3, C('6a3418'), C('2e1408')); disco(im, 8, 4, 2.6, 2.2, C('f4e6c8'), C('a89a78'))
    pt(im, 7, 3, C('ffffff')); pt(im, 6, 6, C('8a4a24')); return im


def menu_gran_chef():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([1, 12, 14, 13], fill=C('c8ccd8'), outline=C('6a6f88'))
    d.pieslice([2, 3, 13, 14], 180, 360, fill=C('e8c24a'), outline=C('8a6a10')); d.rectangle([2, 9, 13, 12], fill=C('e8c24a'), outline=C('8a6a10'))
    pt(im, 7, 2, C('8a6a10')); pt(im, 8, 2, C('8a6a10')); pt(im, 5, 6, C('fff2a8')); pt(im, 6, 5, C('fff2a8')); return im


ITEMS = {
    'tomate': (tomate, 'Tomate', 'Tomato'), 'pescado': (pescado, 'Pescado', 'Fish'), 'cacao': (cacao, 'Cacao', 'Cocoa'), 'miel': (miel, 'Miel', 'Honey'),
    'pescado_asado': (pescado_asado, 'Pescado a la parrilla', 'Grilled Fish'), 'bistec': (bistec, 'Bistec', 'Steak'), 'chocolate_fundido': (chocolate_fundido, 'Chocolate fundido', 'Melted Chocolate'),
    'sopa_tomate': (sopa_tomate, 'Sopa de tomate', 'Tomato Soup'), 'crepes': (crepes, 'Crepes', 'Crepes'), 'onigiri': (onigiri, 'Onigiri de pescado', 'Fish Onigiri'),
    'bayas_chocolate': (bayas_chocolate, 'Bayas con chocolate', 'Chocolate Berries'), 'pizza_margarita': (pizza_margarita, 'Pizza margarita', 'Margherita Pizza'),
    'crepes_miel': (crepes_miel, 'Crepes con miel', 'Honey Crepes'), 'pan_miel': (pan_miel, 'Pan con miel', 'Honey Bread'), 'bistec_papas': (bistec_papas, 'Bistec con papas', 'Steak and Fries'),
    'pescado_vapor': (pescado_vapor, 'Pescado al vapor', 'Steamed Fish'), 'bollos_vapor': (bollos_vapor, 'Bollos al vapor', 'Steamed Buns'),
    'pastel_chocolate': (pastel_chocolate, 'Pastel de chocolate', 'Chocolate Cake'), 'helado_chocolate': (helado_chocolate, 'Copa de helado de chocolate', 'Chocolate Ice Cream Cup'),
    'menu_gran_chef': (menu_gran_chef, 'Menú del gran chef', 'Grand Chef Menu'),
}


# ------------------------------------------------------------------ texturas de las estaciones (16x16)
def fondo(base, oscuro, claro, sem):
    rnd = random.Random(sem); im = Image.new('RGBA', (16, 16), base)
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < 0.15: im.putpixel((x, y), oscuro)
            elif r < 0.25: im.putpixel((x, y), claro)
    return im


def t_tomates():
    im = fondo(C('5a3a1e'), C('3e2610'), C('7a5028'), 1); rnd = random.Random(2)
    for (cx, cy) in ((4, 4), (11, 4), (4, 11), (11, 11), (8, 8)):
        disco(im, cx, cy, 2.4, 2.4, C('d8342c'), C('7a1414')); pt(im, cx - 1, cy - 1, C('f57a6e')); pt(im, cx, cy - 2, C('3e9d46'))
    return im


def t_pescado():
    im = fondo(C('cfe6f2'), C('a8cce0'), C('f4fbff'), 3); d = ImageDraw.Draw(im)
    for (cx, cy) in ((4, 5), (11, 4), (6, 11), (12, 11)):
        disco(im, cx, cy, 3, 1.6, C('7fa8c9'), C('2d4a66')); d.polygon([(cx + 3, cy), (cx + 5, cy - 1), (cx + 5, cy + 1)], fill=C('5d86a8')); pt(im, cx - 2, cy - 1, C('101820'))
    return im


def t_cacao():
    im = fondo(C('6a3a1c'), C('4a2410'), C('8a5028'), 4)
    for (cx, cy) in ((4, 5), (11, 5), (7, 11), (13, 12), (3, 12)):
        disco(im, cx, cy, 2, 3, C('8a4b25'), C('3e1f0c')); pt(im, cx, cy - 1, C('b0683a'))
    return im


def t_miel():
    im = Image.new('RGBA', (16, 16), C('f0b429')); d = ImageDraw.Draw(im)
    for y in range(-1, 16, 4):
        for x in range(0, 16, 4):
            ox = 2 if (y // 4) % 2 else 0
            d.regular_polygon((x + ox + 2, y + 2, 2.4), 6, fill=C('ffd869'), outline=C('b9801a'))
    return im


def t_parrilla():
    im = Image.new('RGBA', (16, 16), C('2a2a34')); d = ImageDraw.Draw(im)
    for x in range(1, 16, 3): d.line([x, 0, x, 15], fill=C('6a6a7c'))
    d.line([0, 4, 15, 4], fill=C('8a8a9c')); d.line([0, 11, 15, 11], fill=C('8a8a9c'))
    for (x, y) in ((3, 7), (9, 8), (12, 2), (5, 13)): pt(im, x, y, C('ff7a2a')); pt(im, x + 1, y, C('ffb040'))
    return im


def t_crepera():
    im = Image.new('RGBA', (16, 16), C('3a3a46')); disco(im, 8, 8, 7, 7, C('2a2a34'), C('1a1a22')); disco(im, 8, 8, 5.5, 5.5, C('f0cf8a'), C('a8782a')); disco(im, 6, 6, 1.5, 1, C('fff0c0'))
    pt(im, 10, 9, C('d6a95a')); return im


def t_vaporera():
    im = Image.new('RGBA', (16, 16), C('d2a45a')); d = ImageDraw.Draw(im)
    for y in range(0, 16, 3): d.line([0, y, 15, y], fill=C('9a6a28'))
    for x in range(0, 16, 3): d.line([x, 0, x, 15], fill=C('b8863c'))
    return im


def t_fuente():
    im = fondo(C('5b2c14'), C('3e1c0a'), C('8a4a24'), 5)
    for (x, y) in ((3, 3), (4, 3), (10, 9), (11, 9), (6, 12)): pt(im, x, y, C('b0683a'))
    return im


TEXTURAS = {'tomates_t': t_tomates, 'pescado_t': t_pescado, 'cacao_t': t_cacao, 'miel_t': t_miel, 'parrilla_t': t_parrilla, 'crepera_t': t_crepera, 'vaporera_t': t_vaporera, 'fuente_t': t_fuente}


# ------------------------------------------------------------------ modelos (los mismos ayudantes que generar.py)
def caras(tex):
    if isinstance(tex, str): tex = {'up': tex, 'down': tex, 'lados': tex}
    f = {}
    for k in ('north', 'south', 'east', 'west'): f[k] = {'texture': '#' + tex['lados']}
    f['up'] = {'texture': '#' + tex['up']}; f['down'] = {'texture': '#' + tex['down']}
    for k in ('north', 'south', 'east', 'west'):
        if k in tex: f[k] = {'texture': '#' + tex[k]}
    return f


def caja(a, b, tex): return {'from': list(a), 'to': list(b), 'faces': caras(tex)}


def base(): return caja((0, 0, 0), (16, 10, 16), {'up': 'encimera', 'down': 'mueble', 'lados': 'mueble'})


EPS = 0.02


def _sol(a0, a1, b0, b1): return min(a1, b1) - max(a0, b0) > 1e-6


def conflictos(el):
    res = []
    for j in range(len(el)):
        for i in range(j):
            a, b = el[i], el[j]
            for eje in range(3):
                o1, o2 = [k for k in range(3) if k != eje]
                if not (_sol(a['from'][o1], a['to'][o1], b['from'][o1], b['to'][o1]) and _sol(a['from'][o2], a['to'][o2], b['from'][o2], b['to'][o2])): continue
                if abs(a['to'][eje] - b['to'][eje]) < 1e-6: res.append((i, j, eje, 1))
                if abs(a['from'][eje] - b['from'][eje]) < 1e-6: res.append((i, j, eje, -1))
    return res


def sin_zfight(el):
    for _ in range(4):
        cs = conflictos(el)
        if not cs: break
        for (i, j, eje, sg) in cs:
            e = el[j]
            if sg > 0: e['to'][eje] = round(e['to'][eje] + EPS, 4)
            else: e['from'][eje] = round(e['from'][eje] - EPS, 4)
    return el


def modelo(el, usa):
    el = sin_zfight(el)
    return {'parent': 'minecraft:block/block', 'textures': dict({'particle': 'emi_cafe:block/' + usa[0]}, **{u: 'emi_cafe:block/' + u for u in usa}), 'elements': el,
            'display': {'gui': {'rotation': [30, 225, 0], 'scale': [0.6, 0.6, 0.6]}, 'fixed': {'scale': [0.5, 0.5, 0.5]}}}


def cajon(top):
    return modelo([base(), caja((2, 10, 2), (14, 15, 14), {'up': top, 'down': 'madera', 'lados': 'madera'}),
                   caja((2, 10, 2), (4, 16, 4), 'madera_oscura'), caja((12, 10, 2), (14, 16, 4), 'madera_oscura'),
                   caja((2, 10, 12), (4, 16, 14), 'madera_oscura'), caja((12, 10, 12), (14, 16, 14), 'madera_oscura')], ['mueble', 'encimera', 'madera', 'madera_oscura', top])


MODELOS = {
    'caja_tomate': cajon('tomates_t'),
    'caja_pescado': cajon('pescado_t'),
    'caja_cacao': modelo([base(), caja((3, 10, 3), (13, 16, 13), {'up': 'cacao_t', 'down': 'cacao_t', 'lados': 'cacao_t'}), caja((6, 16, 6), (10, 17, 10), 'madera_oscura')],
                         ['mueble', 'encimera', 'cacao_t', 'madera_oscura']),
    'caja_miel': modelo([base(), caja((3, 10, 3), (13, 15, 13), {'up': 'miel_t', 'down': 'miel_t', 'lados': 'miel_t'}), caja((2, 15, 2), (14, 17, 14), 'madera_oscura'),
                         caja((7, 17, 7), (9, 19, 9), 'madera')], ['mueble', 'encimera', 'miel_t', 'madera_oscura', 'madera']),
    'parrilla': modelo([base(), caja((1, 10, 2), (15, 13, 14), {'up': 'parrilla_t', 'down': 'metal_oscuro', 'lados': 'metal_oscuro'}), caja((1, 13, 13), (15, 17, 14), 'metal'),
                        caja((15, 11, 6), (16, 12, 10), 'madera_oscura')], ['mueble', 'encimera', 'metal', 'metal_oscuro', 'parrilla_t', 'madera_oscura']),
    'crepera': modelo([base(), caja((2, 10, 2), (14, 11, 14), {'up': 'crepera_t', 'down': 'metal_oscuro', 'lados': 'metal_oscuro'}), caja((14, 10, 7), (16, 11, 9), 'madera_oscura'),
                       caja((6, 11, 12), (10, 14, 14), 'metal')], ['mueble', 'encimera', 'metal', 'metal_oscuro', 'crepera_t', 'madera_oscura']),
    'vaporera': modelo([base(), caja((3, 10, 3), (13, 13, 13), 'vaporera_t'), caja((3, 13, 3), (13, 16, 13), 'vaporera_t'), caja((2, 16, 2), (14, 17, 14), 'madera_oscura'),
                        caja((7, 17, 7), (9, 18, 9), 'madera')], ['mueble', 'encimera', 'vaporera_t', 'madera_oscura', 'madera']),
    'fuente_chocolate': modelo([base(), caja((2, 10, 2), (14, 12, 14), {'up': 'fuente_t', 'down': 'metal', 'lados': 'metal'}), caja((4, 12, 4), (12, 14, 12), 'fuente_t'),
                                caja((6, 14, 6), (10, 16, 10), 'fuente_t'), caja((7, 16, 7), (9, 18, 9), 'fuente_t')], ['mueble', 'encimera', 'metal', 'fuente_t']),
}
BLOQUES = {'caja_tomate': ('Caja de tomates', 'Tomato Crate'), 'caja_pescado': ('Caja de pescado', 'Fish Crate'), 'caja_cacao': ('Saco de cacao', 'Cocoa Sack'),
           'caja_miel': ('Tarro de miel', 'Honey Jar'), 'parrilla': ('Parrilla', 'Grill'), 'crepera': ('Crepera', 'Crepe Maker'), 'vaporera': ('Vaporera', 'Steamer'),
           'fuente_chocolate': ('Fuente de chocolate', 'Chocolate Fountain')}

os.makedirs(T_ITEM, exist_ok=True); os.makedirs(T_BLOQ, exist_ok=True)
for n, (f, es, en) in ITEMS.items():
    f().save(os.path.join(T_ITEM, n + '.png'))
    w(os.path.join(M_ITEM, n + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'emi_cafe:item/' + n}})
for n, f in TEXTURAS.items(): f().save(os.path.join(T_BLOQ, n + '.png'))
for n, m in MODELOS.items():
    w(os.path.join(M_BLOQ, n + '.json'), m)
    w(os.path.join(BS, n + '.json'), {'variants': {'': {'model': 'emi_cafe:block/' + n}}})
    w(os.path.join(M_ITEM, n + '.json'), {'parent': 'emi_cafe:block/' + n})
for fich, idx in (('es_es', 1), ('en_us', 2)):
    ruta = os.path.join(RAIZ, 'lang', fich + '.json')
    d = json.load(open(ruta, encoding='utf-8'))
    for n, (f, es, en) in ITEMS.items(): d['item.emi_cafe.' + n] = es if idx == 1 else en
    for n, (es, en) in BLOQUES.items(): d['block.emi_cafe.' + n] = es if idx == 1 else en
    with open(ruta, 'w', encoding='utf-8') as fh:
        json.dump(d, fh, indent=2, ensure_ascii=False); fh.write('\n')
print('listo', len(ITEMS), 'objetos,', len(MODELOS), 'estaciones')
