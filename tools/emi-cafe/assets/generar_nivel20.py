#!/usr/bin/env python3
"""Genera los archivos del local de nivel 11 a 20 (0.36.0): 12 estaciones nuevas (6 cajas de ingredientes y 6 maquinas), 8 ingredientes/intermedios y 31 platos
(entre ellos los nuevos de las temporadas). Todo se dibuja aqui con Pillow (pixel-art de 16x16 y modelos de cubos como el resto del mod, nada copiado).
No toca lo que generan generar.py ni generar_nivel10.py (usa las mismas texturas compartidas: mueble, encimera, madera, metal...).
Uso: python3 assets/generar_nivel20.py (desde tools/emi-cafe)."""
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


def bol(im, relleno, borde='7a80a0'):
    """Un bol visto de lado con el contenido asomando."""
    d = ImageDraw.Draw(im)
    d.pieslice([2, 4, 14, 16], 0, 180, fill=C('f4f4fa'), outline=C(borde)); disco(im, 8, 8, 5.5, 1.8, C(relleno[0]), C(relleno[1])); return im


def vaso(im, liquido, claro, borde='6a7a96'):
    d = ImageDraw.Draw(im)
    d.polygon([(4, 3), (12, 3), (11, 14), (5, 14)], fill=C('e8f0fa', 200), outline=C(borde))
    d.polygon([(5, 6), (11, 6), (10, 13), (6, 13)], fill=C(liquido)); d.line([6, 7, 10, 7], fill=C(claro)); return im


def taza(im, liquido):
    d = ImageDraw.Draw(im)
    d.rectangle([3, 6, 11, 13], fill=C('f4f4fa'), outline=C('9aa0b8')); d.rectangle([4, 6, 10, 7], fill=C(liquido))
    d.arc([10, 7, 14, 12], 270, 90, fill=C('9aa0b8')); return im


# ------------------------------------------------------------------ ingredientes
def manzana():
    im = lienzo(); disco(im, 8, 9, 5, 5, C('d8342c'), C('7a1414')); disco(im, 6, 7, 1.5, 1.5, C('f57a6e')); d = ImageDraw.Draw(im)
    d.line([8, 3, 9, 1], fill=C('5a3a1e')); d.polygon([(9, 3), (12, 2), (11, 4)], fill=C('3e9d46')); return im


def fresa():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.polygon([(3, 6), (13, 6), (8, 14)], fill=C('e0344a'), outline=C('8a1426')); d.polygon([(4, 4), (8, 6), (12, 4), (8, 2)], fill=C('3e9d46'), outline=C('1f5a26'))
    for (x, y) in ((6, 8), (9, 8), (8, 10), (7, 12), (10, 10), (5, 7)): pt(im, x, y, C('ffd869'))
    return im


def coco():
    im = lienzo(); disco(im, 8, 9, 5.5, 5.5, C('6a4020'), C('2e1a08')); d = ImageDraw.Draw(im)
    d.pieslice([2, 3, 14, 15], 200, 340, fill=C('fafafa'), outline=C('b0b4c8')); pt(im, 6, 8, C('2e1a08')); pt(im, 9, 8, C('2e1a08')); pt(im, 8, 10, C('2e1a08')); return im


def canela():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([4, 4, 7, 13], fill=C('b0682e'), outline=C('5a3010')); d.rectangle([8, 3, 11, 12], fill=C('c47c3c'), outline=C('5a3010'))
    d.line([5, 5, 5, 12], fill=C('d6955a')); d.line([9, 4, 9, 11], fill=C('e0a868')); return im


def seta():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([6, 8, 9, 14], fill=C('f0e6d0'), outline=C('a89a78')); d.pieslice([2, 2, 14, 12], 180, 360, fill=C('b5603a'), outline=C('5a2a14'))
    d.rectangle([2, 7, 13, 8], fill=C('b5603a')); pt(im, 5, 5, C('f0e6d0')); pt(im, 9, 4, C('f0e6d0')); pt(im, 11, 6, C('f0e6d0')); return im


def maiz():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.polygon([(8, 1), (12, 5), (11, 12), (8, 14), (5, 12), (4, 5)], fill=C('f2c84a'), outline=C('a8801a'))
    for (x, y) in ((7, 4), (9, 4), (6, 6), (8, 6), (10, 6), (6, 8), (8, 8), (10, 8), (7, 10), (9, 10)): pt(im, x, y, C('ffe27a'))
    d.polygon([(2, 14), (7, 6), (8, 14)], fill=C('5aae48'), outline=C('2a6a22')); d.polygon([(14, 14), (9, 6), (8, 14)], fill=C('48993a'), outline=C('2a6a22')); return im


def nata():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([4, 7, 11, 14], fill=C('f4f4fa'), outline=C('9aa0b8')); d.rectangle([5, 8, 6, 13], fill=C('ffffff'))
    d.polygon([(5, 7), (8, 2), (11, 7)], fill=C('fffdf4'), outline=C('c8c0a8')); d.line([6, 5, 10, 5], fill=C('e0d8c0')); return im


def masa_canela():
    im = lienzo(); disco(im, 8, 9, 6, 4, C('ecd9a8'), C('a8884a')); rnd = random.Random(5)
    for _ in range(9): pt(im, rnd.randint(4, 11), rnd.randint(7, 11), C('b0682e'))
    return im


# ------------------------------------------------------------------ platos
def fideos():
    im = bol(lienzo(), ('f0cf8a', 'a8782a')); d = ImageDraw.Draw(im)
    for (a, b) in (((4, 8), (7, 6)), ((6, 7), (10, 9)), ((9, 6), (12, 8)), ((5, 9), (11, 7))): d.line([a[0], a[1], b[0], b[1]], fill=C('e0b45a'))
    d.line([11, 2, 8, 8], fill=C('7a4c14')); d.line([13, 3, 10, 8], fill=C('7a4c14')); return im


def palomitas():
    im = lienzo(); d = ImageDraw.Draw(im)
    for (x, y) in ((4, 4), (7, 3), (10, 4), (5, 6), (9, 6), (12, 6)): disco(im, x, y, 1.6, 1.6, C('fff6d8'), C('d8b45a')); pt(im, x, y + 1, C('f2c84a'))
    d.polygon([(3, 7), (13, 7), (11, 15), (5, 15)], fill=C('d8342c'), outline=C('7a1414'))
    for x in (6, 8, 10): d.line([x, 8, x - (1 if x < 8 else 0 if x == 8 else -1) * 0, 14], fill=C('fafafa'))
    return im


def manzana_caramelo():
    im = lienzo(); d = ImageDraw.Draw(im); d.line([8, 1, 8, 15], fill=C('9a6a3a'))
    disco(im, 8, 7, 5, 5, C('b8282c'), C('5a1018')); d.pieslice([3, 2, 13, 10], 180, 360, fill=C('c8782a')); disco(im, 6, 5, 1.2, 1.2, C('ffd0a0')); pt(im, 5, 11, C('c8782a')); return im


def granizado(color, oscuro, adorno):
    im = vaso(lienzo(), color, 'ffffff'); d = ImageDraw.Draw(im)
    d.polygon([(5, 6), (8, 1), (11, 6)], fill=C(color), outline=C(oscuro)); pt(im, 7, 3, C('ffffff'))
    d.line([11, 1, 9, 6], fill=C(adorno)); return im


def granizado_fresa(): return granizado('f06078', 'a02040', 'ffd869')
def granizado_coco(): return granizado('f4f4fa', 'a89a78', '3e9d46')


def sopa_maiz():
    im = bol(lienzo(), ('f2c84a', 'a8801a')); pt(im, 6, 8, C('ffe27a')); pt(im, 9, 8, C('fff2a8')); pt(im, 11, 7, C('3e9d46')); pt(im, 7, 7, C('3e9d46')); return im


def ensalada_fresa():
    im = bol(lienzo(), ('5aae48', '2a6a22'))
    for (x, y) in ((6, 8), (10, 8), (8, 7)): disco(im, x, y, 1.3, 1.3, C('e0344a'), C('8a1426'))
    pt(im, 8, 8, C('ffd869')); return im


def sopa_setas():
    im = bol(lienzo(), ('b58a5a', '6a4020')); d = ImageDraw.Draw(im)
    d.pieslice([5, 5, 10, 9], 180, 360, fill=C('e8d4b0'), outline=C('7a5a30')); d.rectangle([7, 7, 8, 8], fill=C('f0e6d0')); pt(im, 11, 8, C('3e9d46')); return im


def chocolate_nata():
    im = taza(lienzo(), '5b2c14'); d = ImageDraw.Draw(im)
    d.polygon([(4, 6), (7, 1), (10, 6)], fill=C('fffdf4'), outline=C('c8c0a8')); pt(im, 6, 4, C('ffffff')); pt(im, 8, 3, C('d8342c')); return im


def fresas_nata():
    im = plato(); d = ImageDraw.Draw(im)
    d.polygon([(4, 11), (8, 4), (12, 11)], fill=C('fffdf4'), outline=C('c8c0a8'))
    for (x, y) in ((5, 11), (11, 11), (8, 9)): disco(im, x, y, 1.8, 1.8, C('e0344a'), C('8a1426'))
    pt(im, 8, 3, C('e0344a')); return im


def yakisoba():
    im = plato(); d = ImageDraw.Draw(im)
    disco(im, 8, 9, 5.5, 3, C('e0b45a'), C('8a5a14'))
    for (a, b) in (((4, 8), (8, 10)), ((7, 7), (11, 10)), ((5, 10), (10, 8))): d.line([a[0], a[1], b[0], b[1]], fill=C('f4d88a'))
    pt(im, 6, 8, C('d8342c')); pt(im, 10, 9, C('3e9d46')); pt(im, 8, 7, C('d8342c')); pt(im, 9, 10, C('3e9d46')); return im


def arroz_frito():
    im = bol(lienzo(), ('f0dc9a', 'a8801a'))
    for (x, y) in ((6, 8), (9, 7), (11, 8), (7, 7)): pt(im, x, y, C('d8342c'))
    for (x, y) in ((8, 8), (10, 7)): pt(im, x, y, C('3e9d46'))
    pt(im, 5, 8, C('f2c84a')); return im


def setas_salteadas():
    im = plato(); d = ImageDraw.Draw(im)
    for (x, y) in ((5, 9), (9, 8), (12, 10)):
        d.pieslice([x - 3, y - 3, x + 3, y + 3], 180, 360, fill=C('b5603a'), outline=C('5a2a14')); d.rectangle([x - 1, y, x, y + 2], fill=C('f0e6d0'))
    pt(im, 7, 7, C('3e9d46')); return im


def tarta_base(c1, c2):
    im = plato(); d = ImageDraw.Draw(im)
    d.polygon([(2, 11), (14, 11), (12, 6), (4, 6)], fill=C(c1), outline=C(c2)); d.line([3, 9, 13, 9], fill=C(c2)); return im


def tarta_manzana():
    im = tarta_base('e0a84a', '7a4c14'); d = ImageDraw.Draw(im)
    for x in (5, 7, 9, 11): d.line([x, 6, x - 1, 10], fill=C('b8282c'))
    pt(im, 8, 5, C('b8282c')); return im


def fresas_caramelo():
    im = plato(); d = ImageDraw.Draw(im); d.line([5, 3, 5, 12], fill=C('9a6a3a')); d.line([11, 3, 11, 12], fill=C('9a6a3a'))
    for (x, y) in ((5, 5), (5, 8), (11, 5), (11, 8)): disco(im, x, y, 2, 2, C('e0344a'), C('8a1426')); pt(im, x - 1, y - 1, C('ffd0a0'))
    return im


def pescado_ahumado():
    im = plato(); d = ImageDraw.Draw(im)
    disco(im, 7, 8, 5, 3, C('a8683a'), C('4a2810')); d.polygon([(11, 8), (14, 5), (14, 11)], fill=C('8a5028'), outline=C('4a2810'))
    for x in (5, 7, 9): d.line([x, 6, x + 1, 10], fill=C('4a2810'))
    pt(im, 4, 7, C('101010'))
    for (x, y) in ((4, 3), (9, 2), (12, 3)): pt(im, x, y, C('9a9aa8')); pt(im, x, y - 1, C('c0c0cc'))
    return im


def costillas():
    im = plato(); d = ImageDraw.Draw(im)
    d.rectangle([2, 5, 13, 10], fill=C('a8421e'), outline=C('4a1808'))
    for x in (4, 7, 10): d.rectangle([x, 4, x + 1, 11], fill=C('f0e6d0'), outline=C('a89a78'))
    d.line([3, 6, 12, 6], fill=C('d8682e')); return im


def bocata_costillas():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.pieslice([1, 4, 15, 14], 180, 360, fill=C('d9a45a'), outline=C('7a4c14')); d.rectangle([2, 9, 13, 10], fill=C('a8421e'), outline=C('4a1808'))
    d.line([2, 8, 13, 8], fill=C('5aae48')); d.pieslice([1, 7, 15, 17], 0, 180, fill=C('d9a45a'), outline=C('7a4c14')); pt(im, 5, 5, C('ffe08a')); return im


def arroz_coco():
    im = bol(lienzo(), ('fafafa', 'b0b4c8')); d = ImageDraw.Draw(im)
    d.pieslice([6, 4, 12, 10], 180, 360, fill=C('fafafa'), outline=C('6a4020')); pt(im, 8, 7, C('6a4020')); pt(im, 5, 8, C('e8e8e8')); pt(im, 11, 8, C('e8e8e8')); return im


def copa_fresas():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.polygon([(3, 6), (13, 6), (9, 11), (7, 11)], fill=C('e8f0fa', 200), outline=C('6a7a96')); d.line([8, 11, 8, 14], fill=C('6a7a96')); d.line([5, 14, 11, 14], fill=C('6a7a96'))
    disco(im, 8, 5, 3.4, 2.4, C('fff2f4'), C('c8a8b0')); disco(im, 6, 3, 1.5, 1.5, C('e0344a'), C('8a1426')); disco(im, 10, 3, 1.5, 1.5, C('e0344a'), C('8a1426')); pt(im, 8, 2, C('3e9d46')); return im


def calabaza_canela():
    im = plato(); d = ImageDraw.Draw(im)
    d.pieslice([2, 5, 14, 14], 180, 360, fill=C('e8842a'), outline=C('8a4a10')); d.rectangle([2, 9, 13, 10], fill=C('e8842a'))
    for x in (5, 8, 11): d.line([x, 6, x, 9], fill=C('b0561a'))
    for (x, y) in ((5, 7), (8, 8), (10, 7), (7, 6)): pt(im, x, y, C('7a3a10'))
    return im


def galletas_canela():
    im = plato()
    for (x, y) in ((5, 9), (11, 9), (8, 6)):
        disco(im, x, y, 3, 3, C('c47c3c'), C('5a3010')); pt(im, x - 1, y - 1, C('e0a868')); pt(im, x + 1, y, C('7a4010')); pt(im, x, y + 1, C('7a4010'))
    return im


def cafe_canela():
    im = taza(lienzo(), '6a3a1c'); d = ImageDraw.Draw(im); d.line([8, 2, 12, 8], fill=C('b0682e'), width=2); pt(im, 6, 3, C('c8c8d8')); pt(im, 5, 4, C('c8c8d8')); return im


def tarta_fresa():
    im = tarta_base('f4e6c8', 'a89a78'); d = ImageDraw.Draw(im)
    d.rectangle([4, 6, 12, 7], fill=C('f06078'))
    for x in (5, 8, 11): disco(im, x, 5, 1.5, 1.5, C('e0344a'), C('8a1426'))
    return im


def ramen():
    im = bol(lienzo(), ('d8a860', '7a4c14')); d = ImageDraw.Draw(im)
    for (a, b) in (((4, 8), (8, 7)), ((7, 8), (11, 7))): d.line([a[0], a[1], b[0], b[1]], fill=C('f4d88a'))
    d.pieslice([5, 5, 10, 9], 180, 360, fill=C('e8d4b0'), outline=C('7a5a30')); disco(im, 10, 8, 1.4, 1, C('fafafa'), C('a89a78')); pt(im, 10, 8, C('f2c84a')); d.line([12, 1, 9, 7], fill=C('7a4c14')); return im


def arroz_setas():
    im = bol(lienzo(), ('f0dc9a', 'a8801a')); d = ImageDraw.Draw(im)
    for x in (6, 9): d.pieslice([x - 2, 5, x + 2, 9], 180, 360, fill=C('b5603a'), outline=C('5a2a14'))
    pt(im, 11, 8, C('3e9d46')); return im


def tarta_nata():
    im = tarta_fresa(); d = ImageDraw.Draw(im)
    d.polygon([(6, 5), (8, 1), (10, 5)], fill=C('fffdf4'), outline=C('c8c0a8')); disco(im, 8, 0.5, 1, 1, C('e0344a')); return im


def parrillada():
    im = plato(); d = ImageDraw.Draw(im)
    disco(im, 5, 8, 3.4, 2.6, C('b5603a'), C('4a2010')); d.rectangle([7, 6, 13, 9], fill=C('a8421e'), outline=C('4a1808'))
    for x in (8, 10, 12): d.rectangle([x, 5, x, 10], fill=C('f0e6d0'))
    pt(im, 4, 7, C('d98860')); pt(im, 6, 3, C('3e9d46')); pt(im, 7, 3, C('3e9d46')); pt(im, 10, 3, C('d8342c')); return im


def festin_mar():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([1, 12, 14, 13], fill=C('c8ccd8'), outline=C('6a6f88')); disco(im, 5, 8, 4, 2.4, C('a8683a'), C('4a2810')); d.polygon([(8, 8), (10, 6), (10, 10)], fill=C('8a5028'))
    disco(im, 11, 8, 3.2, 2.2, C('cfe0ee'), C('5a7a96')); d.polygon([(13, 8), (15, 6), (15, 10)], fill=C('9ab6cc')); pt(im, 3, 7, C('101010')); pt(im, 10, 7, C('101820'))
    pt(im, 7, 3, C('ffe08a')); pt(im, 8, 2, C('ffe08a')); return im


def banquete_postres():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([1, 12, 14, 13], fill=C('c8ccd8'), outline=C('6a6f88'))
    d.rectangle([2, 8, 8, 11], fill=C('f4e6c8'), outline=C('a89a78')); d.rectangle([2, 8, 8, 9], fill=C('f06078'))
    disco(im, 5, 6, 1.4, 1.4, C('e0344a')); d.polygon([(9, 10), (14, 10), (11.5, 14)], fill=C('d9a45a'), outline=C('7a4c14')); disco(im, 11.5, 8, 2.4, 2, C('6a3418'), C('2e1408')); disco(im, 11.5, 6, 1.6, 1.4, C('fffdf4'), C('c8c0a8'))
    pt(im, 12, 3, C('ffe08a')); pt(im, 3, 4, C('ffe08a')); return im


def menu_leyenda():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([0, 12, 15, 14], fill=C('b0b4c8'), outline=C('6a6f88'))
    d.pieslice([1, 2, 14, 14], 180, 360, fill=C('f4d24a'), outline=C('8a6a10')); d.rectangle([1, 8, 14, 12], fill=C('f4d24a'), outline=C('8a6a10'))
    d.polygon([(4, 3), (6, 0), (8, 3), (10, 0), (12, 3)], fill=C('f4d24a'), outline=C('8a6a10'))
    pt(im, 8, 1, C('d8342c')); pt(im, 5, 6, C('fff8c0')); pt(im, 6, 5, C('fff8c0')); pt(im, 10, 9, C('c89a14')); pt(im, 4, 10, C('c89a14')); return im


ITEMS = {
    'manzana': (manzana, 'Manzana', 'Apple'), 'fresa': (fresa, 'Fresa', 'Strawberry'), 'coco': (coco, 'Coco', 'Coconut'), 'canela': (canela, 'Canela', 'Cinnamon'),
    'seta': (seta, 'Seta', 'Mushroom'), 'maiz': (maiz, 'Maíz', 'Corn'), 'nata': (nata, 'Nata', 'Cream'), 'masa_canela': (masa_canela, 'Masa con canela', 'Cinnamon Dough'),
    'fideos': (fideos, 'Fideos', 'Noodles'), 'palomitas': (palomitas, 'Palomitas', 'Popcorn'), 'manzana_caramelo': (manzana_caramelo, 'Manzana caramelizada', 'Caramel Apple'),
    'granizado_fresa': (granizado_fresa, 'Granizado de fresa', 'Strawberry Slush'), 'granizado_coco': (granizado_coco, 'Granizado de coco', 'Coconut Slush'),
    'sopa_maiz': (sopa_maiz, 'Sopa de maíz', 'Corn Soup'), 'ensalada_fresa': (ensalada_fresa, 'Ensalada de fresas', 'Strawberry Salad'), 'sopa_setas': (sopa_setas, 'Sopa de setas', 'Mushroom Soup'),
    'chocolate_nata': (chocolate_nata, 'Chocolate con nata', 'Hot Chocolate with Cream'), 'fresas_nata': (fresas_nata, 'Fresas con nata', 'Strawberries and Cream'),
    'yakisoba': (yakisoba, 'Fideos salteados', 'Stir-fried Noodles'), 'arroz_frito': (arroz_frito, 'Arroz frito', 'Fried Rice'), 'setas_salteadas': (setas_salteadas, 'Setas salteadas', 'Sautéed Mushrooms'),
    'tarta_manzana': (tarta_manzana, 'Tarta de manzana', 'Apple Pie'), 'fresas_caramelo': (fresas_caramelo, 'Fresas caramelizadas', 'Caramel Strawberries'),
    'pescado_ahumado': (pescado_ahumado, 'Pescado ahumado', 'Smoked Fish'), 'costillas': (costillas, 'Costillas ahumadas', 'Smoked Ribs'), 'bocata_costillas': (bocata_costillas, 'Bocata de costillas', 'Rib Sandwich'),
    'arroz_coco': (arroz_coco, 'Arroz con coco', 'Coconut Rice'), 'copa_fresas': (copa_fresas, 'Copa de helado con fresas', 'Strawberry Sundae Cup'),
    'calabaza_canela': (calabaza_canela, 'Calabaza con canela', 'Cinnamon Pumpkin'), 'galletas_canela': (galletas_canela, 'Galletas de canela', 'Cinnamon Cookies'),
    'cafe_canela': (cafe_canela, 'Café con canela', 'Cinnamon Coffee'), 'tarta_fresa': (tarta_fresa, 'Tarta de fresas', 'Strawberry Cake'), 'ramen': (ramen, 'Ramen de setas', 'Mushroom Ramen'),
    'arroz_setas': (arroz_setas, 'Arroz frito con setas', 'Mushroom Fried Rice'), 'tarta_nata': (tarta_nata, 'Tarta de fresas con nata', 'Strawberry Cream Cake'),
    'parrillada': (parrillada, 'Parrillada del chef', "Chef's Grill Platter"), 'festin_mar': (festin_mar, 'Festín del mar', 'Seafood Feast'),
    'banquete_postres': (banquete_postres, 'Banquete de postres', 'Dessert Banquet'), 'menu_leyenda': (menu_leyenda, 'Menú de la leyenda', 'Legendary Menu'),
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


def t_manzanas():
    im = fondo(C('6a4a2a'), C('4a3018'), C('8a6438'), 11)
    for (cx, cy) in ((4, 4), (11, 4), (4, 11), (11, 11), (8, 8)):
        disco(im, cx, cy, 2.6, 2.6, C('d8342c'), C('7a1414')); pt(im, cx - 1, cy - 1, C('f57a6e')); pt(im, cx, cy - 3, C('3e9d46'))
    return im


def t_fresas():
    im = fondo(C('5a3a1e'), C('3e2610'), C('7a5028'), 12)
    for (cx, cy) in ((4, 5), (11, 4), (6, 11), (12, 11)):
        ImageDraw.Draw(im).polygon([(cx - 3, cy - 2), (cx + 3, cy - 2), (cx, cy + 3)], fill=C('e0344a'), outline=C('8a1426')); pt(im, cx - 1, cy - 3, C('3e9d46')); pt(im, cx + 1, cy - 3, C('3e9d46')); pt(im, cx, cy, C('ffd869'))
    return im


def t_cocos():
    im = fondo(C('8a6a3a'), C('6a4a22'), C('a8844a'), 13)
    for (cx, cy) in ((4, 4), (11, 5), (5, 11), (12, 12)):
        disco(im, cx, cy, 3, 3, C('6a4020'), C('2e1a08')); pt(im, cx - 1, cy - 1, C('8a5a30')); pt(im, cx, cy + 1, C('2e1a08')); pt(im, cx + 1, cy + 1, C('2e1a08'))
    return im


def t_canela():
    im = fondo(C('9a5a28'), C('6a3a14'), C('c07a3c'), 14); d = ImageDraw.Draw(im)
    for y in (3, 8, 13): d.line([1, y, 14, y - 1], fill=C('d6955a')); d.line([1, y + 1, 14, y], fill=C('5a3010'))
    return im


def t_setas():
    im = fondo(C('4a3a2a'), C('2e2418'), C('6a5638'), 15)
    for (cx, cy) in ((4, 5), (11, 4), (7, 11), (13, 11)):
        ImageDraw.Draw(im).pieslice([cx - 3, cy - 3, cx + 3, cy + 3], 180, 360, fill=C('b5603a'), outline=C('5a2a14')); pt(im, cx, cy + 1, C('f0e6d0')); pt(im, cx, cy + 2, C('f0e6d0')); pt(im, cx - 1, cy - 1, C('f0e6d0'))
    return im


def t_maiz():
    im = fondo(C('c8a83a'), C('a8881e'), C('e8c85a'), 16); rnd = random.Random(17)
    for _ in range(14): disco(im, rnd.randint(1, 14), rnd.randint(1, 14), 1.1, 1.1, C('f2c84a'), C('a8801a'))
    return im


def t_wok():
    im = Image.new('RGBA', (16, 16), C('2a2a34')); disco(im, 8, 8, 7, 7, C('1c1c24'), C('4a4a5c')); disco(im, 8, 8, 5, 5, C('3a2a22'), C('5a4838'))
    for (x, y) in ((6, 6), (9, 7), (7, 9), (10, 10)): pt(im, x, y, C('d8342c')); pt(im, x + 1, y, C('3e9d46'))
    pt(im, 8, 8, C('f2c84a')); return im


def t_ahumador():
    im = Image.new('RGBA', (16, 16), C('3a3a46')); d = ImageDraw.Draw(im)
    for y in range(1, 16, 3): d.line([0, y, 15, y], fill=C('5a5a6c'))
    for (x, y) in ((3, 4), (9, 3), (12, 9), (5, 11)): pt(im, x, y, C('9a9aa8')); pt(im, x, y - 1, C('c0c0cc'))
    return im


def t_fideera():
    im = Image.new('RGBA', (16, 16), C('b8c0cc')); d = ImageDraw.Draw(im)
    for x in range(1, 16, 2): d.line([x, 2, x, 13], fill=C('e8c872'))
    d.rectangle([0, 0, 15, 1], fill=C('7a8294')); d.rectangle([0, 14, 15, 15], fill=C('7a8294')); return im


def t_natera():
    im = Image.new('RGBA', (16, 16), C('f4f4fa')); d = ImageDraw.Draw(im)
    d.ellipse([2, 2, 13, 13], outline=C('b0b4c8'), fill=C('fffdf4'))
    for (a, b) in ((4, 8), (6, 5), (9, 6), (10, 9), (7, 11)): pt(im, a, b, C('d8d0b8'))
    d.line([8, 1, 8, 8], fill=C('8a90a8')); return im


def t_granizadora():
    im = fondo(C('cfe6f2'), C('a8cce0'), C('f4fbff'), 18); d = ImageDraw.Draw(im)
    d.ellipse([3, 3, 12, 12], fill=C('f06078'), outline=C('a02040'))
    for (x, y) in ((5, 5), (8, 7), (6, 9), (9, 5)): pt(im, x, y, C('ffffff'))
    return im


def t_caramelizadora():
    im = Image.new('RGBA', (16, 16), C('c8782a')); d = ImageDraw.Draw(im)
    for y in range(0, 16, 4):
        for x in range(0, 16, 4): disco(im, x + 2, y + 2, 1.8, 1.8, C('e0a050'), C('8a4a10'))
    pt(im, 4, 4, C('ffe08a')); pt(im, 10, 10, C('ffe08a')); return im


TEXTURAS = {'manzanas_t': t_manzanas, 'fresas_t': t_fresas, 'cocos_t': t_cocos, 'canela_t': t_canela, 'setas_t': t_setas, 'maiz_t': t_maiz,
            'wok_t': t_wok, 'ahumador_t': t_ahumador, 'fideera_t': t_fideera, 'natera_t': t_natera, 'granizadora_t': t_granizadora, 'caramelizadora_t': t_caramelizadora}


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


def saco(tex):
    return modelo([base(), caja((3, 10, 3), (13, 16, 13), {'up': tex, 'down': tex, 'lados': tex}), caja((6, 16, 6), (10, 17, 10), 'madera_oscura')], ['mueble', 'encimera', tex, 'madera_oscura'])


MODELOS = {
    'caja_manzana': cajon('manzanas_t'),
    'caja_fresa': cajon('fresas_t'),
    'caja_coco': saco('cocos_t'),
    'caja_canela': saco('canela_t'),
    'caja_seta': cajon('setas_t'),
    'caja_maiz': saco('maiz_t'),
    'wok': modelo([base(), caja((2, 10, 2), (14, 11, 14), {'up': 'metal_oscuro', 'down': 'metal_oscuro', 'lados': 'metal_oscuro'}), caja((3, 11, 3), (13, 12, 13), {'up': 'wok_t', 'down': 'metal_oscuro', 'lados': 'metal_oscuro'}),
                   caja((14, 11, 7), (16, 12, 9), 'madera_oscura'), caja((1, 11, 7), (2, 13, 9), 'metal')], ['mueble', 'encimera', 'metal', 'metal_oscuro', 'wok_t', 'madera_oscura']),
    'ahumador': modelo([base(), caja((2, 10, 2), (14, 18, 14), {'up': 'metal_oscuro', 'down': 'metal_oscuro', 'lados': 'ahumador_t'}), caja((6, 18, 6), (10, 20, 10), 'metal'),
                        caja((11, 13, 1), (13, 15, 2), 'madera_oscura')], ['mueble', 'encimera', 'metal', 'metal_oscuro', 'ahumador_t', 'madera_oscura']),
    'fideera': modelo([base(), caja((2, 10, 2), (14, 14, 14), {'up': 'metal', 'down': 'metal', 'lados': 'fideera_t'}), caja((4, 14, 4), (12, 16, 12), 'metal_oscuro'),
                       caja((7, 16, 7), (9, 18, 9), 'madera_oscura')], ['mueble', 'encimera', 'metal', 'metal_oscuro', 'fideera_t', 'madera_oscura']),
    'natera': modelo([base(), caja((3, 10, 3), (13, 12, 13), 'metal'), caja((5, 12, 5), (11, 17, 11), {'up': 'natera_t', 'down': 'natera_t', 'lados': 'natera_t'}),
                      caja((7, 17, 7), (9, 19, 9), 'metal_oscuro')], ['mueble', 'encimera', 'metal', 'metal_oscuro', 'natera_t']),
    'granizadora': modelo([base(), caja((2, 10, 2), (14, 12, 14), 'metal'), caja((4, 12, 4), (12, 17, 12), {'up': 'granizadora_t', 'down': 'granizadora_t', 'lados': 'granizadora_t'}),
                           caja((11, 12, 7), (14, 13, 9), 'metal_oscuro')], ['mueble', 'encimera', 'metal', 'granizadora_t', 'metal_oscuro']),
    'caramelizadora': modelo([base(), caja((2, 10, 2), (14, 13, 14), {'up': 'caramelizadora_t', 'down': 'metal_oscuro', 'lados': 'metal_oscuro'}), caja((2, 13, 2), (14, 14, 3), 'metal'),
                              caja((2, 13, 13), (14, 14, 14), 'metal'), caja((14, 11, 6), (16, 12, 10), 'madera_oscura')],
                             ['mueble', 'encimera', 'metal', 'metal_oscuro', 'caramelizadora_t', 'madera_oscura']),
}
BLOQUES = {'caja_manzana': ('Caja de manzanas', 'Apple Crate'), 'caja_fresa': ('Caja de fresas', 'Strawberry Crate'), 'caja_coco': ('Saco de cocos', 'Coconut Sack'),
           'caja_canela': ('Saco de canela', 'Cinnamon Sack'), 'caja_seta': ('Caja de setas', 'Mushroom Crate'), 'caja_maiz': ('Saco de maíz', 'Corn Sack'),
           'wok': ('Wok', 'Wok'), 'ahumador': ('Ahumador', 'Smoker'), 'fideera': ('Fideera', 'Noodle Maker'), 'natera': ('Batidor de nata', 'Cream Whipper'),
           'granizadora': ('Granizadora', 'Slush Machine'), 'caramelizadora': ('Caramelizadora', 'Caramelizer')}

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
