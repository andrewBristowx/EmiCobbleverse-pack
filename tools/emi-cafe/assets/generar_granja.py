#!/usr/bin/env python3
"""Genera los archivos de la granja (0.24.0): el ingrediente cebolla, 4 platos nuevos, 4 iconos del menu (huerto, corral, despensa, granjero) y la estacion «Caja de cebollas».
Todo se dibuja aqui con Pillow (pixel-art de 16x16 y modelos de cubos como el resto del mod, nada copiado). No toca lo que generan los otros scripts.
Uso: python3 assets/generar_granja.py (desde tools/emi-cafe)."""
import json, os, random
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
    ImageDraw.Draw(im).ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=rel, outline=borde)


def pt(im, x, y, c):
    if 0 <= x < im.width and 0 <= y < im.height: im.putpixel((x, y), c)


def plato():
    im = lienzo(); disco(im, 8, 12, 7, 2.5, C('f4f4fa'), C('9aa0b8')); return im


def cuenco(im, caldo, borde='7a80a0'):
    d = ImageDraw.Draw(im)
    d.pieslice([2, 4, 14, 16], 0, 180, fill=C('f4f4fa'), outline=C(borde)); disco(im, 8, 8, 5.5, 1.8, C(caldo), C('5a3a14'))


def aro(im, cx, cy, r=2.4, c='e8b64a', borde='8a5a14'):
    d = ImageDraw.Draw(im); d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=C(borde)); d.ellipse([cx - r + 1, cy - r + 1, cx + r - 1, cy + r - 1], outline=C(c))


# ------------------------------------------------------------------ ingrediente y platos
def cebolla():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.polygon([(8, 2), (5, 6), (3, 10), (5, 13), (11, 13), (13, 10), (11, 6)], fill=C('e9c27a'), outline=C('8a5a22'))
    d.line([8, 4, 6, 12], fill=C('c99a4a')); d.line([8, 4, 10, 12], fill=C('c99a4a')); d.line([8, 4, 8, 13], fill=C('f4dca0'))
    pt(im, 8, 1, C('4e9a3a')); pt(im, 7, 2, C('4e9a3a')); pt(im, 9, 2, C('4e9a3a')); pt(im, 6, 11, C('8a5a22')); pt(im, 7, 14, C('d9d0b0')); pt(im, 9, 14, C('d9d0b0'))
    return im


def cebolla_picada():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([2, 10, 13, 13], fill=C('b98a4a'), outline=C('6a4818'))
    for (x, y) in ((4, 8), (6, 9), (8, 8), (10, 9), (5, 7), (9, 7), (7, 6), (11, 7)):
        d.rectangle([x, y, x + 1, y + 1], fill=C('f2dd9a'), outline=C('c99a4a'))
    return im


def sopa_cebolla():
    im = lienzo(); cuenco(im, 'b9772a')
    for (x, y) in ((6, 8), (9, 8), (11, 8)): pt(im, x, y, C('f2dd9a'))
    pt(im, 7, 7, C('f2dd9a')); pt(im, 10, 7, C('f2dd9a')); return im


def aros_cebolla():
    im = plato()
    aro(im, 5, 9); aro(im, 10, 8); aro(im, 8, 6, 2.0); aro(im, 11, 11, 1.8)
    return im


def sopa_cebolla_gratinada():
    im = lienzo(); cuenco(im, 'f0c84a', '8a6a14'); d = ImageDraw.Draw(im)
    d.line([4, 8, 12, 8], fill=C('fff2a8')); pt(im, 6, 7, C('c9922a')); pt(im, 10, 7, C('c9922a')); pt(im, 8, 9, C('c9922a'))
    for x in (5, 8, 11): pt(im, x, 9, C('f0c84a')); pt(im, x, 10, C('f0c84a'))
    return im


def ensalada_completa():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.pieslice([2, 4, 14, 16], 0, 180, fill=C('f4f4fa'), outline=C('7a80a0'))
    for (x, y) in ((5, 8), (8, 7), (11, 8), (7, 6)): disco(im, x, y, 2.2, 1.6, C('6fbf4a'), C('3a7a26'))
    disco(im, 6, 7, 1.4, 1.4, C('d8342c'), C('7a1414')); disco(im, 10, 7, 1.4, 1.4, C('d8342c'), C('7a1414')); disco(im, 8, 6, 1.1, 1.1, C('3d56c8'), C('1e2c70'))
    return im


# ------------------------------------------------------------------ iconos del menu
def icono_huerto():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([1, 9, 14, 14], fill=C('6a4426'), outline=C('3e2610'))
    for x in (3, 7, 11):
        d.line([x, 9, x, 13], fill=C('4a2e16'))
        d.line([x + 1, 8, x + 1, 5], fill=C('4aa83a')); pt(im, x, 5, C('6fd04a')); pt(im, x + 2, 6, C('6fd04a')); pt(im, x, 7, C('6fd04a'))
    pt(im, 4, 3, C('f2c84a')); pt(im, 8, 2, C('f2c84a')); pt(im, 12, 3, C('f2c84a')); return im


def icono_corral():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([1, 12, 14, 13], fill=C('4aa83a'))
    for x in (2, 6, 10, 14): d.rectangle([x, 5, x + 1, 13], fill=C('b98a4a'), outline=C('6a4818'))
    d.rectangle([1, 7, 14, 8], fill=C('c9985a'), outline=C('6a4818')); d.rectangle([1, 10, 14, 11], fill=C('c9985a'), outline=C('6a4818'))
    disco(im, 8, 4, 2, 1.5, C('fafafa'), C('9aa0b8')); pt(im, 7, 3, C('101010')); return im


def icono_despensa():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 13, 14], fill=C('9a6a38'), outline=C('4e3018'))
    d.line([2, 6, 13, 6], fill=C('4e3018')); d.line([2, 10, 13, 10], fill=C('4e3018'))
    disco(im, 5, 4, 1.3, 1.3, C('d8342c')); disco(im, 9, 4, 1.3, 1.3, C('e9c27a')); disco(im, 11, 8, 1.3, 1.3, C('f2c84a'))
    disco(im, 5, 8, 1.3, 1.3, C('6fbf4a')); disco(im, 8, 12, 2.2, 1.2, C('f4f4fa')); pt(im, 4, 12, C('d8a24a')); return im


def icono_granjero():
    im = lienzo(); d = ImageDraw.Draw(im)
    disco(im, 8, 9, 5, 5, C('f2f2f6'), C('9aa0b8')); pt(im, 6, 8, C('101010')); pt(im, 10, 8, C('101010')); pt(im, 8, 10, C('d87a8a'))
    d.polygon([(2, 6), (14, 6), (11, 2), (5, 2)], fill=C('e8c24a'), outline=C('8a6a10')); d.rectangle([4, 5, 12, 6], fill=C('c9922a'))
    d.polygon([(4, 3), (3, 0), (5, 2)], fill=C('f2f2f6'), outline=C('9aa0b8')); d.polygon([(12, 3), (13, 0), (11, 2)], fill=C('f2f2f6'), outline=C('9aa0b8'))
    return im


ITEMS = {
    'cebolla': (cebolla, 'Cebolla', 'Onion'),
    'cebolla_picada': (cebolla_picada, 'Cebolla picada', 'Chopped Onion'),
    'sopa_cebolla': (sopa_cebolla, 'Sopa de cebolla', 'Onion Soup'),
    'aros_cebolla': (aros_cebolla, 'Aros de cebolla', 'Onion Rings'),
    'sopa_cebolla_gratinada': (sopa_cebolla_gratinada, 'Sopa de cebolla gratinada', 'French Onion Soup'),
    'ensalada_completa': (ensalada_completa, 'Ensalada completa', 'Full Salad'),
    'icono_huerto': (icono_huerto, 'Huerto', 'Garden'),
    'icono_corral': (icono_corral, 'Corral', 'Pen'),
    'icono_despensa': (icono_despensa, 'Despensa', 'Pantry'),
    'icono_granjero': (icono_granjero, 'Michigranjero', 'Farmer Michi'),
}


# ------------------------------------------------------------------ la caja de cebollas (mismos ayudantes que generar_nivel10.py)
def fondo(base, oscuro, claro, sem):
    rnd = random.Random(sem); im = Image.new('RGBA', (16, 16), base)
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            if r < 0.15: im.putpixel((x, y), oscuro)
            elif r < 0.25: im.putpixel((x, y), claro)
    return im


def t_cebollas():
    im = fondo(C('5a3a1e'), C('3e2610'), C('7a5028'), 7)
    for (cx, cy) in ((4, 4), (11, 4), (4, 11), (11, 11), (8, 8)):
        disco(im, cx, cy, 2.4, 2.6, C('e9c27a'), C('8a5a22')); pt(im, cx - 1, cy - 1, C('f8e4b0')); pt(im, cx, cy - 3, C('4e9a3a'))
    return im


def caras(tex):
    if isinstance(tex, str): tex = {'up': tex, 'down': tex, 'lados': tex}
    f = {}
    for k in ('north', 'south', 'east', 'west'): f[k] = {'texture': '#' + tex['lados']}
    f['up'] = {'texture': '#' + tex['up']}; f['down'] = {'texture': '#' + tex['down']}
    return f


def caja(a, b, tex): return {'from': list(a), 'to': list(b), 'faces': caras(tex)}


def base(): return caja((0, 0, 0), (16, 10, 16), {'up': 'encimera', 'down': 'mueble', 'lados': 'mueble'})


EPS = 0.02


def _sol(a0, a1, b0, b1): return min(a1, b1) - max(a0, b0) > 1e-6


def sin_zfight(el):
    for _ in range(4):
        cs = []
        for j in range(len(el)):
            for i in range(j):
                a, b = el[i], el[j]
                for eje in range(3):
                    o1, o2 = [k for k in range(3) if k != eje]
                    if not (_sol(a['from'][o1], a['to'][o1], b['from'][o1], b['to'][o1]) and _sol(a['from'][o2], a['to'][o2], b['from'][o2], b['to'][o2])): continue
                    if abs(a['to'][eje] - b['to'][eje]) < 1e-6: cs.append((i, j, eje, 1))
                    if abs(a['from'][eje] - b['from'][eje]) < 1e-6: cs.append((i, j, eje, -1))
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


MODELOS = {'caja_cebolla': cajon('cebollas_t')}
BLOQUES = {'caja_cebolla': ('Caja de cebollas', 'Onion Crate')}

os.makedirs(T_ITEM, exist_ok=True); os.makedirs(T_BLOQ, exist_ok=True)
for n, (f, es, en) in ITEMS.items():
    f().save(os.path.join(T_ITEM, n + '.png'))
    w(os.path.join(M_ITEM, n + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'emi_cafe:item/' + n}})
t_cebollas().save(os.path.join(T_BLOQ, 'cebollas_t.png'))
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
print('listo', len(ITEMS), 'objetos,', len(MODELOS), 'estacion')
