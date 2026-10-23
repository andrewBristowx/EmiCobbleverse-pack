#!/usr/bin/env python3
"""
Genera TODAS las texturas y modelos del Cafe Michialien por codigo (pixel-art de 16x16 y modelos de cubos), sin copiar nada de otros mods.
Uso:  python3 assets/generar.py            (escribe en src/main/resources/assets/emi_cafe)
      python3 assets/generar.py --hoja X.png   (ademas guarda una hoja de contacto ampliada para revisarlas)
Para cambiar una textura a mano basta con reemplazar el PNG correspondiente (los nombres son fijos) y NO volver a ejecutar este script.
"""
import json, math, os, random, sys
from PIL import Image, ImageDraw

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'emi_cafe')
T_ITEM = os.path.join(RAIZ, 'textures', 'item'); T_BLOQ = os.path.join(RAIZ, 'textures', 'block')
M_ITEM = os.path.join(RAIZ, 'models', 'item'); M_BLOQ = os.path.join(RAIZ, 'models', 'block'); BS = os.path.join(RAIZ, 'blockstates'); LANG = os.path.join(RAIZ, 'lang')
for d in (T_ITEM, T_BLOQ, M_ITEM, M_BLOQ, BS, LANG): os.makedirs(d, exist_ok=True)

def C(h, a=255):
    h = h.lstrip('#'); return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)
def mezcla(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)

# ---------------------------------------------------------------- objetos (iconos de 16x16)
def lienzo(): return Image.new('RGBA', (16, 16), (0, 0, 0, 0))

def elipse(im, cx, cy, rx, ry, base, claro, oscuro, borde, giro=0.0, luz=(-0.5, -0.6)):
    """Una forma ovalada con borde, sombra abajo a la derecha y brillo arriba a la izquierda."""
    px = im.load(); cs, sn = math.cos(giro), math.sin(giro)
    def dentro(x, y):
        dx, dy = x - cx, y - cy; u = dx * cs + dy * sn; v = -dx * sn + dy * cs
        return (u / rx) ** 2 + (v / ry) ** 2 <= 1.0
    for y in range(16):
        for x in range(16):
            if not dentro(x, y): continue
            if not (dentro(x - 1, y) and dentro(x + 1, y) and dentro(x, y - 1) and dentro(x, y + 1)): px[x, y] = borde; continue
            dx, dy = (x - cx) / rx, (y - cy) / ry
            s = dx * -luz[0] + dy * -luz[1]               # >0: lado de la sombra
            px[x, y] = oscuro if s > 0.45 else claro if s < -0.5 else base

def rect(im, x0, y0, x1, y1, col):
    d = ImageDraw.Draw(im); d.rectangle([x0, y0, x1, y1], fill=col)

def papa():
    im = lienzo()
    elipse(im, 7.6, 8.4, 6.4, 4.7, C('d9a55b'), C('efc98a'), C('a97538'), C('6b4423'), giro=-0.35)
    px = im.load()
    for (x, y) in ((5, 8), (9, 6), (10, 10), (7, 11)): px[x, y] = C('84552a')           # los «ojos» de la papa
    px[6, 6] = C('f6dfae'); px[7, 5] = C('f6dfae')
    return im

def papa_tiras():
    im = lienzo(); d = ImageDraw.Draw(im)
    pal = [(C('f3e2a0'), C('c9b060')), (C('f7e9b4'), C('d3bc6c')), (C('efdc96'), C('c4aa58'))]
    palos = [((2, 11, 12, 7), 0), ((3, 8, 13, 5), 1), ((2, 6, 11, 3), 2), ((4, 13, 14, 10), 1), ((3, 4, 9, 2), 0)]
    for (x0, y0, x1, y1), k in palos:
        d.line([x0, y0 + 1, x1, y1 + 1], fill=pal[k][1], width=2)       # sombra
        d.line([x0, y0, x1, y1], fill=pal[k][0], width=2)
    return im

def papas_fritas():
    im = lienzo(); d = ImageDraw.Draw(im); px = im.load()
    oro, oro2, oro3 = C('f8c84a'), C('ffe27a'), C('d79a22')
    for i, (x, top) in enumerate(((3, 4), (5, 2), (7, 3), (9, 1), (11, 3), (12, 5), (6, 5), (10, 5))):
        d.rectangle([x, top, x + 1, 9], fill=oro if i % 2 == 0 else oro2)
        px[x + 1, top + 1] = oro3; px[x + 1, 8] = oro3
    # el cucurucho (carton rojo con rayas blancas)
    rojo, rojo2, blanco, borde = C('e04b4b'), C('f27b70'), C('fff4ee'), C('8f2a2a')
    for y in range(8, 16):
        izq, der = 3 + (y - 8) // 3, 12 - (y - 8) // 3
        for x in range(izq, der + 1):
            c = rojo2 if x < izq + 2 else rojo
            if (y - 8) % 4 == 2: c = blanco
            px[x, y] = c
        px[izq, y] = borde; px[der, y] = borde
    for x in range(3, 13): px[x, 8] = borde
    for x in range(5, 11): px[x, 15] = borde
    return im

def burbuja():
    im = lienzo(); px = im.load(); d = ImageDraw.Draw(im)
    borde, blanco, sombra = C('7a6a9a'), C('ffffff'), C('e8e0f4')
    d.rounded_rectangle([0, 1, 15, 11], radius=4, fill=borde)
    d.rounded_rectangle([1, 2, 14, 10], radius=3, fill=blanco)
    for x in range(2, 14): px[x, 10] = sombra
    d.polygon([(3, 11), (8, 11), (4, 15)], fill=borde); d.polygon([(4, 11), (7, 11), (4, 13)], fill=blanco)
    return im

def moneda():
    im = lienzo(); px = im.load()
    elipse(im, 7.5, 7.5, 6.2, 6.2, C('ffd23f'), C('ffe98a'), C('d19a1c'), C('8b5a0b'))
    for (x, y) in ((7, 4), (8, 4), (7, 11), (8, 11), (4, 7), (4, 8), (11, 7), (11, 8)): px[x, y] = C('d19a1c')
    for (x, y) in ((7, 6), (8, 6), (6, 7), (7, 7), (8, 7), (9, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9)): px[x, y] = C('fff0a8')       # un destello en el centro
    return im

def papas_quemadas():
    im = papas_fritas(); px = im.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a and y < 9: px[x, y] = (int(r * 0.30), int(g * 0.26), int(b * 0.22), 255)
    for (x, y) in ((6, 0), (7, 1), (8, 0), (9, 1)): px[x, y] = (150, 150, 150, 200)               # humo
    return im

def baya():
    im = lienzo(); px = im.load()
    elipse(im, 7.7, 9.3, 5.4, 5.0, C('4f86e0'), C('9cc4ff'), C('2d54b0'), C('1c3470'))
    for (x, y) in ((5, 7), (6, 7), (5, 8)): px[x, y] = C('d8ebff')                                  # brillo
    for (x, y) in ((8, 4), (8, 3), (9, 2), (10, 2), (10, 3), (9, 3)): px[x, y] = C('3e9a3e')          # tallo y hoja
    px[8, 4] = C('2c6e2c'); px[8, 13] = C('1c3470'); px[7, 13] = C('1c3470')
    return im

def limonada():
    im = lienzo(); px = im.load(); d = ImageDraw.Draw(im)
    vidrio, vidrio2, liq, liq2, liq3 = C('cfe9f4'), C('9bc3d6'), C('ffe066'), C('fff3a6'), C('e6b52a')
    for y in range(4, 15):
        izq, der = 3 + (0 if y < 12 else 1), 12 - (0 if y < 12 else 1)
        for x in range(izq, der + 1): px[x, y] = liq2 if y < 6 else liq if x < der - 1 else liq3
        px[izq, y] = vidrio; px[der, y] = vidrio2
    for x in range(4, 12): px[x, 14] = vidrio2
    for x in range(3, 13): px[x, 3] = vidrio                                                      # el borde
    d.line([10, 0, 8, 8], fill=C('e04b4b'), width=1); d.line([11, 0, 9, 8], fill=C('ffffff'), width=1)   # la pajita
    elipse(im, 4.5, 3.2, 2.2, 2.0, C('4f86e0'), C('9cc4ff'), C('2d54b0'), C('1c3470'))              # una baya en el borde
    return im

def lechuga():
    im = lienzo(); px = im.load()
    elipse(im, 7.7, 8.4, 6.3, 5.9, C('6fcf5a'), C('a6ee8a'), C('3f9c3a'), C('25692a'))
    for (x, y) in ((7, 4), (7, 5), (8, 6), (8, 7), (9, 8), (6, 8), (5, 9), (8, 9), (10, 10), (7, 11)): px[x, y] = C('c8f5b0')     # nervios claros
    return im

def lechuga_picada():
    im = lienzo(); px = im.load()
    verdes = [C('6fcf5a'), C('a6ee8a'), C('4aa845'), C('8de070')]
    trozos = ((2, 9, 4, 3), (5, 11, 3, 3), (8, 10, 4, 3), (11, 9, 3, 4), (3, 6, 3, 3), (6, 7, 4, 3), (10, 6, 3, 3), (4, 3, 4, 3), (8, 3, 3, 3), (7, 13, 4, 2))
    for i, (x, y, w, h) in enumerate(trozos):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if 0 <= xx < 16 and 0 <= yy < 16: px[xx, yy] = verdes[(i + xx + yy) % 4] if (xx - x, yy - y) != (w - 1, h - 1) else C('2d7a2d')
    return im

def plato_base(im, cx=7.7, cy=8.4, rx=7.3, ry=5.4):
    elipse(im, cx, cy, rx, ry, C('fbfbff'), C('ffffff'), C('d5d8ea'), C('8a90b0'))
    elipse(im, cx, cy, rx - 3, ry - 2.4, C('f1f3fb'), C('ffffff'), C('dde0f0'), C('d5d8ea'))

def plato_sucio():
    im = lienzo(); px = im.load(); plato_base(im)
    for (x, y) in ((6, 8), (7, 8), (7, 9), (9, 9), (10, 9), (8, 10)): px[x, y] = C('9a5b32')       # manchas de salsa
    for (x, y) in ((5, 9), (11, 8), (9, 7)): px[x, y] = C('e6c46a')                                  # migas
    px[8, 9] = C('c0392b')
    return im

def plato_limpio():
    im = lienzo(); px = im.load(); plato_base(im)
    for (x, y) in ((4, 8), (5, 7), (11, 12)): px[x, y] = C('ffffff')                                  # brillo
    px[12, 6] = C('fff7a0'); px[13, 5] = C('fff7a0'); px[12, 4] = C('fff7a0'); px[11, 5] = C('fff7a0')   # destello de limpio
    return im

def ensalada():
    im = lienzo(); px = im.load(); d = ImageDraw.Draw(im)
    elipse(im, 8, 7.5, 6.2, 4.6, C('7fd96a'), C('b4f09a'), C('4aa845'), C('2c6e2c'))
    for (x, y) in ((5, 6), (9, 4), (11, 7), (7, 8)): px[x, y] = C('d6fbc2')
    for (cx, cy) in ((6, 5), (10, 8)): elipse(im, cx, cy, 1.6, 1.5, C('4f86e0'), C('9cc4ff'), C('2d54b0'), C('1c3470'))       # bayas encima
    # el cuenco
    for y in range(8, 14):
        izq, der = 2 + (y - 8) // 2, 13 - (y - 8) // 2
        for x in range(izq, der + 1): px[x, y] = C('f7c6d9') if x < der - 1 else C('d99ab5')
        px[izq, y] = C('8d4f6a'); px[der, y] = C('8d4f6a')
    for x in range(5, 11): px[x, 14] = C('8d4f6a')
    return im

def _cara(cuerpo, claro, oscuro, borde, extra):
    im = lienzo(); px = im.load()
    elipse(im, 8, 10, 6, 5, cuerpo, claro, oscuro, borde)
    for (x, y) in ((5, 9), (5, 10), (10, 9), (10, 10)): px[x, y] = C('1d1d2b')                          # ojos
    px[5, 9] = C('ffffff'); px[10, 9] = C('ffffff')
    for x in range(7, 9): px[x, 12] = C('b04a4a')
    extra(im, px)
    # el gorro de cocinero
    for y in range(1, 5):
        for x in range(4, 12): px[x, y] = C('ffffff') if (x + y) % 5 else C('eef0ff')
    for x in range(3, 13): px[x, 5] = C('d5d8ea')
    px[3, 2] = C('ffffff'); px[12, 2] = C('ffffff'); px[3, 3] = C('eef0ff'); px[12, 3] = C('eef0ff')
    return im

def cocinero_charmander():
    def extra(im, px):
        for (x, y) in ((12, 13), (13, 12), (13, 13), (12, 12)): px[x, y] = C('ffb21e')
        px[13, 12] = C('ff5a1e'); px[12, 12] = C('ffe26a')
        for x in range(6, 10): px[x, 11] = C('ffd9a0')                                                 # el hocico claro
    return _cara(C('f4833d'), C('ffb170'), C('c95a1f'), C('7a3210'), extra)

def cocinero_squirtle():
    def extra(im, px):
        for (x, y) in ((2, 13), (3, 14), (4, 14), (11, 14), (12, 14), (13, 13)): px[x, y] = C('c68b4e')   # el caparazon asoma
        for x in range(6, 10): px[x, 11] = C('cfeaf7')
    return _cara(C('6bb8e6'), C('a9dcf7'), C('3f8ab8'), C('1f4f75'), extra)

def cocinero_bulbasaur():
    def extra(im, px):
        for (x, y) in ((3, 8), (4, 7), (12, 8), (11, 7), (6, 12), (9, 13)): px[x, y] = C('3f9a73')       # manchas oscuras
        px[5, 9] = C('d63a3a'); px[5, 10] = C('d63a3a'); px[10, 9] = C('d63a3a'); px[10, 10] = C('d63a3a')   # ojos rojos
        px[5, 9] = C('ffffff'); px[10, 9] = C('ffffff')
    return _cara(C('7fcdb0'), C('b4ecd4'), C('4fa588'), C('2a6a52'), extra)

def campana():
    im = lienzo(); px = im.load()
    elipse(im, 8, 8, 6, 4.4, C('ffd23f'), C('ffee9a'), C('d19a1c'), C('8b5a0b'))
    for x in range(1, 15): px[x, 12] = C('8b5a0b'); px[x, 13] = C('d19a1c')
    px[8, 3] = C('8b5a0b'); px[7, 3] = C('d19a1c'); px[8, 2] = C('8b5a0b'); px[7, 2] = C('ffd23f')
    return im

def puerta():
    im = lienzo(); px = im.load(); d = ImageDraw.Draw(im)
    d.rectangle([3, 1, 12, 15], fill=C('8a5a34')); d.rectangle([4, 2, 11, 14], fill=C('b07a45'))
    d.rectangle([5, 3, 10, 7], fill=C('c48a52')); d.rectangle([5, 9, 10, 13], fill=C('c48a52'))
    px[10, 8] = C('ffd23f'); px[10, 9] = C('d19a1c')
    d.rectangle([2, 15, 13, 15], fill=C('5e3a20'))
    return im

def reloj():
    im = lienzo(); px = im.load(); d = ImageDraw.Draw(im)
    elipse(im, 7.7, 8, 6.4, 6.4, C('fbfbff'), C('ffffff'), C('d5d8ea'), C('4a4f70'))
    d.line([8, 8, 8, 3], fill=C('2b2f4a')); d.line([8, 8, 11, 9], fill=C('e04b4b'))
    px[8, 8] = C('2b2f4a')
    for (x, y) in ((8, 2), (13, 8), (8, 13), (2, 8)): px[x, y] = C('4a4f70')
    return im

def corazon():
    im = lienzo(); px = im.load(); d = ImageDraw.Draw(im)
    for (cx, cy) in ((5, 5), (10, 5)): elipse(im, cx, cy, 3.4, 3.2, C('f2556b'), C('ff9aa8'), C('c22a43'), C('7a1428'))
    d.polygon([(2, 7), (13, 7), (7.5, 14)], fill=C('f2556b'))
    d.line([2, 7, 7, 14], fill=C('7a1428')); d.line([13, 7, 8, 14], fill=C('7a1428'))
    px[4, 4] = C('ffd0d8'); px[5, 4] = C('ffd0d8'); px[4, 5] = C('ffd0d8')
    return im

def mesas_icono():
    im = lienzo(); px = im.load(); d = ImageDraw.Draw(im)
    d.rectangle([1, 4, 14, 7], fill=C('e2495c')); d.rectangle([1, 4, 14, 4], fill=C('b8283c'))
    for x in range(1, 15, 4): d.rectangle([x, 5, x + 1, 6], fill=C('fff6f2'))
    d.rectangle([7, 8, 8, 14], fill=C('8a5a34')); d.rectangle([4, 14, 11, 15], fill=C('5e3a20'))
    return im

def pila_platos():
    im = lienzo()
    for k, cy in enumerate((12, 9, 6)): elipse(im, 8, cy, 6, 2.6, C('fbfbff'), C('ffffff'), C('c9cde4'), C('7a80a0'))
    return im

def megafono():
    im = lienzo(); px = im.load(); d = ImageDraw.Draw(im)
    d.polygon([(2, 6), (7, 6), (13, 2), (13, 13), (7, 10), (2, 10)], fill=C('e04b4b'), outline=C('7a1428'))
    d.rectangle([3, 7, 6, 9], fill=C('f27b70')); d.rectangle([4, 10, 6, 14], fill=C('8a5a34'))
    d.line([14, 5, 15, 4], fill=C('ffd23f')); d.line([14, 7, 15, 7], fill=C('ffd23f')); d.line([14, 10, 15, 11], fill=C('ffd23f'))
    return im

def libro():
    im = lienzo(); d = ImageDraw.Draw(im); px = im.load()
    d.rectangle([2, 2, 13, 14], fill=C('b8283c'), outline=C('6a1020')); d.rectangle([3, 3, 4, 13], fill=C('8a1c2c'))
    d.rectangle([6, 4, 11, 12], fill=C('fff6e0'))
    for y in (6, 8, 10): d.line([7, y, 10, y], fill=C('b08a5a'))
    return im

def invitar():
    im = lienzo(); px = im.load(); d = ImageDraw.Draw(im)
    for (cx, col) in ((5, C('f4b183')), (11, C('9ad0f5'))):
        elipse(im, cx, 5, 2.6, 2.6, col, mezcla(col, (255, 255, 255), 0.4), mezcla(col, (0, 0, 0), 0.25), C('3a2a40'))
        d.rectangle([cx - 3, 9, cx + 2, 14], fill=col)
    d.rectangle([7, 11, 9, 11], fill=C('ffffff')); d.rectangle([8, 10, 8, 12], fill=C('ffffff'))
    return im

def cerrado():
    im = lienzo(); d = ImageDraw.Draw(im)
    d.rectangle([1, 3, 14, 12], fill=C('e04b4b'), outline=C('7a1428')); d.rectangle([2, 4, 13, 11], fill=C('f27b70'))
    d.line([4, 5, 11, 10], fill=C('ffffff'), width=1); d.line([4, 10, 11, 5], fill=C('ffffff'), width=1)
    d.line([8, 0, 8, 3], fill=C('8a5a34'))
    return im

OBJETOS = {'papa': papa, 'papa_tiras': papa_tiras, 'papas_fritas': papas_fritas, 'burbuja': burbuja, 'moneda': moneda,
            'papas_quemadas': papas_quemadas, 'baya': baya, 'limonada': limonada, 'lechuga': lechuga, 'lechuga_picada': lechuga_picada, 'ensalada': ensalada,
            'plato_sucio': plato_sucio, 'plato_limpio': plato_limpio,
            'cocinero_charmander': cocinero_charmander, 'cocinero_squirtle': cocinero_squirtle, 'cocinero_bulbasaur': cocinero_bulbasaur,
            'campana': campana, 'puerta': puerta, 'reloj': reloj, 'corazon': corazon, 'mesas_icono': mesas_icono, 'pila_platos': pila_platos, 'megafono': megafono,
            'libro': libro, 'invitar': invitar, 'cerrado': cerrado}

# ---------------------------------------------------------------- losetas de los bloques (16x16)
def loseta(f, sem):
    im = Image.new('RGBA', (16, 16)); rnd = random.Random(sem); f(im, im.load(), rnd); return im

def t_madera(base, oscuro, claro):
    def f(im, px, rnd):
        for y in range(16):
            for x in range(16):
                c = base
                if y % 4 == 3: c = oscuro                                   # la junta entre tablas
                elif rnd.random() < 0.18: c = mezcla(base, claro, 0.7)
                elif rnd.random() < 0.12: c = mezcla(base, oscuro, 0.45)
                px[x, y] = c
        for (x, y) in ((3, 1), (11, 5), (6, 9), (13, 13)): px[x, y] = oscuro                     # clavos
    return f

def t_metal(base, oscuro, claro):
    def f(im, px, rnd):
        for y in range(16):
            for x in range(16):
                c = mezcla(base, claro, 0.5) if x % 5 == 1 else mezcla(base, oscuro, 0.35) if x % 7 == 4 else base
                if x in (0, 15) or y in (0, 15): c = oscuro
                px[x, y] = c
        for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)): px[x, y] = claro
    return f

def t_aceite(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = mezcla(C('f0b830'), C('ffd870'), 0.5 + 0.5 * math.sin((x + y) * 0.9) * 0.4)
    for _ in range(9):
        x, y = rnd.randrange(1, 14), rnd.randrange(1, 14); px[x, y] = C('fff0b0'); px[x + 1, y] = C('ffe08a')

def t_papas(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('6e4a2a')
    for _ in range(11):
        cx, cy = rnd.randrange(1, 15), rnd.randrange(1, 15)
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 4 and 0 <= cx + dx < 16 and 0 <= cy + dy < 16:
                    px[cx + dx, cy + dy] = C('efc98a') if dx + dy < -1 else C('a97538') if dx + dy > 1 else C('d9a55b')

def t_tabla(im, px, rnd):
    for y in range(16):
        for x in range(16):
            c = C('ebcf9c')
            if (x + (y // 5) * 3) % 6 == 0: c = C('d3b07a')
            if rnd.random() < 0.08: c = C('f4dfb4')
            px[x, y] = c
    for x in range(16): px[x, 0] = C('c9a46c'); px[x, 15] = C('c9a46c')
    for y in range(16): px[0, y] = C('c9a46c'); px[15, y] = C('c9a46c')

def t_mantel(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('e2495c') if ((x // 4) + (y // 4)) % 2 == 0 else C('fff6f2')
    for x in range(16): px[x, 0] = C('b8283c'); px[x, 15] = C('b8283c')

def t_mueble(im, px, rnd):                                                   # un mueble rosa pastel con un panel hundido
    for y in range(16):
        for x in range(16):
            c = C('f8d3df')
            if x in (0, 15) or y in (0, 15): c = C('e9a9be')
            elif x in (2, 13) or y in (2, 13): c = C('e9a9be') if (2 <= x <= 13 and 2 <= y <= 13) else c
            px[x, y] = c
    px[8, 6] = C('b07a8a'); px[8, 7] = C('b07a8a')                           # el tirador

def t_encimera(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('fbf7f6') if rnd.random() > 0.12 else C('e6dde0')
    for x in range(16): px[x, 0] = C('d9ced2'); px[x, 15] = C('d9ced2')
    for y in range(16): px[0, y] = C('d9ced2'); px[15, y] = C('d9ced2')

def t_cubo(im, px, rnd):
    for y in range(16):
        for x in range(16):
            c = C('8aa89c') if x % 4 != 0 else C('6f8c80')
            if y in (0, 15): c = C('587064')
            px[x, y] = c

def t_pared_rosa(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('f6c4d4') if rnd.random() > 0.1 else C('f0b5c8')

def t_bayas(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('4a3566')
    for _ in range(12):
        cx, cy = rnd.randrange(1, 15), rnd.randrange(1, 15)
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                if dx * dx + dy * dy <= 4 and 0 <= cx + dx < 16 and 0 <= cy + dy < 16:
                    px[cx + dx, cy + dy] = C('9cc4ff') if dx + dy < -1 else C('2d54b0') if dx + dy > 1 else C('4f86e0')

def t_lechugas(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('2f7a35')
    for _ in range(11):
        cx, cy = rnd.randrange(1, 15), rnd.randrange(1, 15)
        for dy in range(-2, 3):
            for dx in range(-3, 4):
                if (dx / 3) ** 2 + (dy / 2) ** 2 <= 1 and 0 <= cx + dx < 16 and 0 <= cy + dy < 16:
                    px[cx + dx, cy + dy] = C('a6ee8a') if dy < 0 else C('4aa845') if dy > 0 else C('6fcf5a')

def t_cristal(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('d6eef8') if (x + y) % 7 else C('f2fbff')
    for x in range(16): px[x, 0] = C('9bc3d6'); px[x, 15] = C('9bc3d6')
    for y in range(16): px[0, y] = C('9bc3d6'); px[15, y] = C('9bc3d6')

def t_limonada(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = mezcla(C('ffe066'), C('fff3a6'), 0.5 + 0.5 * math.sin(x * 0.8 + y * 0.5) * 0.5)
    for _ in range(6): px[rnd.randrange(1, 15), rnd.randrange(1, 15)] = C('ffffff')

def t_agua(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = mezcla(C('5aa8ee'), C('a8d8ff'), 0.5 + 0.5 * math.sin(x * 0.7 + y * 0.9) * 0.5)
    for _ in range(5): px[rnd.randrange(1, 15), rnd.randrange(1, 15)] = C('ffffff')

def t_bandeja(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('f4f1ea')
    for x in range(16): px[x, 0] = C('b9b3a4'); px[x, 15] = C('b9b3a4')
    for y in range(16): px[0, y] = C('b9b3a4'); px[15, y] = C('b9b3a4')
    elipse_t = lambda cx, cy, r, c: [px.__setitem__((cx + dx, cy + dy), c) for dy in range(-r, r + 1) for dx in range(-r, r + 1) if dx * dx + dy * dy <= r * r and r > 0]
    elipse_t(8, 8, 5, C('ffffff')); elipse_t(8, 8, 3, C('e8ebf6'))
    for dx in range(-5, 6):
        if dx * dx <= 25: px[8 + dx, 8 + (1 if dx == 0 else 0)] = px[8 + dx, 8 + (1 if dx == 0 else 0)]

def t_registradora(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('f0b4c8')
    for x in range(16): px[x, 0] = C('c9728f'); px[x, 15] = C('c9728f')
    for y in range(16): px[0, y] = C('c9728f'); px[15, y] = C('c9728f')
    for fila in range(3):
        for col in range(4):
            x0, y0 = 2 + col * 3, 8 + fila * 2 + (1 if fila else 0)
            px[x0, y0] = C('ffd23f') if (fila + col) % 3 else C('ffffff'); px[x0 + 1, y0] = px[x0, y0]
    for x in range(2, 14):
        for y in range(2, 6): px[x, y] = C('1c3a2a')
    for x in range(3, 8): px[x, 3] = C('7cf0a0')
    for x in range(9, 13): px[x, 4] = C('7cf0a0')

def t_registradora_top(im, px, rnd):
    for y in range(16):
        for x in range(16): px[x, y] = C('e89ab4')
    for x in range(16): px[x, 0] = C('c9728f'); px[x, 15] = C('c9728f')
    for y in range(16): px[0, y] = C('c9728f'); px[15, y] = C('c9728f')
    for x in range(3, 13):
        for y in range(3, 8): px[x, y] = C('1c3a2a')
    for x in range(4, 9): px[x, 4] = C('7cf0a0')
    for x in range(4, 12): px[x, 6] = C('4fbf78')
    for x in range(3, 13): px[x, 11] = C('ffd23f')

TILES = {
    'madera': t_madera(C('c48a52'), C('8f5e36'), C('dba570')), 'madera_oscura': t_madera(C('8a5a34'), C('5e3a20'), C('a3733f')),
    'metal': t_metal(C('c4cad4'), C('7c8494'), C('eef2f8')), 'metal_oscuro': t_metal(C('767e8e'), C('4a505e'), C('9aa2b2')),
    'aceite': t_aceite, 'papas': t_papas, 'tabla': t_tabla, 'mantel': t_mantel, 'mueble': t_mueble, 'encimera': t_encimera, 'cubo': t_cubo,
    'bayas': t_bayas, 'lechugas': t_lechugas, 'cristal': t_cristal, 'limonada_t': t_limonada, 'agua': t_agua, 'bandeja': t_bandeja, 'registradora': t_registradora, 'registradora_top': t_registradora_top,
}

# ---------------------------------------------------------------- modelos de los bloques
def caras(tex):
    """tex: un nombre para todas las caras o un dict {up, down, lados} (los nombres de textura sin «#»)."""
    if isinstance(tex, str): tex = {'up': tex, 'down': tex, 'lados': tex}
    f = {}
    for k in ('north', 'south', 'east', 'west'): f[k] = {'texture': '#' + tex['lados']}
    f['up'] = {'texture': '#' + tex['up']}; f['down'] = {'texture': '#' + tex['down']}
    return f

def caja(a, b, tex):
    return {'from': list(a), 'to': list(b), 'faces': caras(tex)}

def base():                                                                  # el mueble sobre el que va cada estacion
    return caja((0, 0, 0), (16, 10, 16), {'up': 'encimera', 'down': 'mueble', 'lados': 'mueble'})

TEXTURAS_COMUNES = ['madera', 'madera_oscura', 'metal', 'metal_oscuro', 'aceite', 'papas', 'tabla', 'mantel', 'mueble', 'encimera', 'cubo']

def modelo(elementos, usa):
    return {'parent': 'minecraft:block/block', 'textures': dict({'particle': 'emi_cafe:block/' + usa[0]}, **{u: 'emi_cafe:block/' + u for u in usa}), 'elements': elementos,
            'display': {'gui': {'rotation': [30, 225, 0], 'scale': [0.6, 0.6, 0.6]}, 'fixed': {'scale': [0.5, 0.5, 0.5]}}}

MODELOS = {
    'caja_papas': modelo([base(), caja((2, 10, 2), (14, 15, 14), {'up': 'papas', 'down': 'madera', 'lados': 'madera'}),
                          caja((2, 10, 2), (4, 16, 4), 'madera_oscura'), caja((12, 10, 2), (14, 16, 4), 'madera_oscura'),
                          caja((2, 10, 12), (4, 16, 14), 'madera_oscura'), caja((12, 10, 12), (14, 16, 14), 'madera_oscura')], ['mueble', 'encimera', 'madera', 'madera_oscura', 'papas']),
    'tabla_cortar': modelo([base(), caja((2, 10, 3), (14, 11, 13), 'tabla'), caja((9, 11, 4), (10, 12, 10), 'metal'), caja((9, 11, 10), (10, 12, 12), 'madera_oscura')],
                           ['mueble', 'encimera', 'tabla', 'metal', 'madera_oscura']),
    'freidora': modelo([base(), caja((2, 10, 2), (14, 14, 14), 'metal'), caja((2, 14, 2), (14, 15, 3), 'metal_oscuro'), caja((2, 14, 13), (14, 15, 14), 'metal_oscuro'),
                        caja((2, 14, 3), (3, 15, 13), 'metal_oscuro'), caja((13, 14, 3), (14, 15, 13), 'metal_oscuro'),
                        caja((3, 13.5, 3), (13, 14.5, 13), 'aceite'), caja((14, 11, 6), (16, 13, 10), 'madera_oscura'), caja((3, 15, 7), (13, 16, 9), 'metal_oscuro')],
                       ['mueble', 'encimera', 'metal', 'metal_oscuro', 'aceite', 'madera_oscura']),
    'basura': modelo([caja((3, 0, 3), (13, 11, 13), 'cubo'), caja((2, 11, 2), (14, 13, 14), 'metal_oscuro'), caja((6, 13, 6), (10, 14, 10), 'metal')], ['cubo', 'metal_oscuro', 'metal']),
    'mesa': modelo([caja((0, 12, 0), (16, 14, 16), {'up': 'mantel', 'down': 'madera', 'lados': 'madera'}), caja((6, 0, 6), (10, 12, 10), 'madera_oscura'), caja((3, 0, 3), (13, 1, 13), 'madera_oscura')],
                   ['madera', 'madera_oscura', 'mantel']),
    'encimera': modelo([base()], ['mueble', 'encimera']),
    'caja_bayas': modelo([base(), caja((2, 10, 2), (14, 15, 14), {'up': 'bayas', 'down': 'madera', 'lados': 'madera'}),
                          caja((2, 10, 2), (4, 16, 4), 'madera_oscura'), caja((12, 10, 2), (14, 16, 4), 'madera_oscura'),
                          caja((2, 10, 12), (4, 16, 14), 'madera_oscura'), caja((12, 10, 12), (14, 16, 14), 'madera_oscura')], ['mueble', 'encimera', 'madera', 'madera_oscura', 'bayas']),
    'caja_lechuga': modelo([base(), caja((2, 10, 2), (14, 15, 14), {'up': 'lechugas', 'down': 'madera', 'lados': 'madera'}),
                            caja((2, 10, 2), (4, 16, 4), 'madera_oscura'), caja((12, 10, 2), (14, 16, 4), 'madera_oscura'),
                            caja((2, 10, 12), (4, 16, 14), 'madera_oscura'), caja((12, 10, 12), (14, 16, 14), 'madera_oscura')], ['mueble', 'encimera', 'madera', 'madera_oscura', 'lechugas']),
    'exprimidor': modelo([base(), caja((4, 10, 4), (12, 11, 12), 'metal_oscuro'), caja((5, 11, 5), (11, 16, 11), 'cristal'), caja((6, 11, 6), (10, 14, 10), 'limonada_t'),
                          caja((5, 16, 5), (11, 17, 11), 'metal'), caja((7, 17, 7), (9, 19, 9), 'madera_oscura'), caja((11, 12, 7), (13, 14, 9), 'metal')],
                         ['mueble', 'encimera', 'metal', 'metal_oscuro', 'cristal', 'limonada_t', 'madera_oscura']),
    'emplatar': modelo([base(), caja((1, 10, 1), (15, 11, 15), 'bandeja'), caja((1, 11, 1), (15, 12, 2), 'metal'), caja((1, 11, 14), (15, 12, 15), 'metal')],
                       ['mueble', 'encimera', 'bandeja', 'metal']),
    'fregadero': modelo([base(), caja((1, 10, 1), (15, 13, 15), 'metal'), caja((3, 12, 3), (13, 13.5, 13), 'agua'), caja((7, 13, 12), (9, 18, 14), 'metal_oscuro'), caja((7, 17, 8), (9, 19, 14), 'metal_oscuro')],
                        ['mueble', 'encimera', 'metal', 'metal_oscuro', 'agua']),
    'caja_registradora': modelo([base(), caja((2, 10, 3), (14, 15, 13), {'up': 'registradora_top', 'down': 'registradora', 'lados': 'registradora'}), caja((3, 15, 5), (13, 18, 8), 'registradora_top')],
                                ['mueble', 'encimera', 'registradora', 'registradora_top']),
}

NOMBRES_ES = {'papa': 'Papa', 'papa_tiras': 'Papa en tiras', 'papas_fritas': 'Papas fritas', 'burbuja': 'Burbuja de pedido', 'moneda': 'Moneda del Café',
              'caja_papas': 'Caja de papas', 'tabla_cortar': 'Tabla de cortar', 'freidora': 'Freidora', 'basura': 'Cubo de basura', 'mesa': 'Mesa del café', 'encimera': 'Encimera',
              'papas_quemadas': 'Papas quemadas', 'baya': 'Baya', 'limonada': 'Limonada de bayas', 'lechuga': 'Lechuga', 'lechuga_picada': 'Lechuga picada', 'ensalada': 'Ensalada de bayas',
              'plato_sucio': 'Plato sucio', 'plato_limpio': 'Plato limpio', 'cocinero_charmander': 'Charmander cocinero', 'cocinero_squirtle': 'Squirtle cocinero', 'cocinero_bulbasaur': 'Bulbasaur cocinero',
              'campana': 'Campana', 'puerta': 'Puerta', 'reloj': 'Reloj', 'corazon': 'Corazón', 'mesas_icono': 'Mesas', 'pila_platos': 'Pila de platos', 'megafono': 'Megáfono', 'libro': 'Recetario',
              'invitar': 'Invitar', 'cerrado': 'Cerrado',
              'caja_bayas': 'Caja de bayas', 'caja_lechuga': 'Caja de lechugas', 'exprimidor': 'Exprimidor', 'emplatar': 'Mesa de emplatar', 'fregadero': 'Fregadero', 'caja_registradora': 'Caja registradora'}
NOMBRES_EN = {'papa': 'Potato', 'papa_tiras': 'Potato Strips', 'papas_fritas': 'French Fries', 'burbuja': 'Order Bubble', 'moneda': 'Cafe Coin',
              'caja_papas': 'Potato Crate', 'tabla_cortar': 'Cutting Board', 'freidora': 'Fryer', 'basura': 'Trash Bin', 'mesa': 'Cafe Table', 'encimera': 'Counter',
              'papas_quemadas': 'Burnt Fries', 'baya': 'Berry', 'limonada': 'Berry Lemonade', 'lechuga': 'Lettuce', 'lechuga_picada': 'Chopped Lettuce', 'ensalada': 'Berry Salad',
              'plato_sucio': 'Dirty Plate', 'plato_limpio': 'Clean Plate', 'cocinero_charmander': 'Chef Charmander', 'cocinero_squirtle': 'Chef Squirtle', 'cocinero_bulbasaur': 'Chef Bulbasaur',
              'campana': 'Bell', 'puerta': 'Door', 'reloj': 'Clock', 'corazon': 'Heart', 'mesas_icono': 'Tables', 'pila_platos': 'Plate Stack', 'megafono': 'Megaphone', 'libro': 'Recipe Book',
              'invitar': 'Invite', 'cerrado': 'Closed',
              'caja_bayas': 'Berry Crate', 'caja_lechuga': 'Lettuce Crate', 'exprimidor': 'Juicer', 'emplatar': 'Plating Table', 'fregadero': 'Sink', 'caja_registradora': 'Cash Register'}

def escribir():
    hoja = []
    for n, f in OBJETOS.items():
        im = f(); im.save(os.path.join(T_ITEM, n + '.png')); hoja.append(im)
        json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'emi_cafe:item/' + n}}, open(os.path.join(M_ITEM, n + '.json'), 'w'), indent=2)
    for i, (n, f) in enumerate(TILES.items()):
        im = loseta(f, i + 1) if n not in ('aceite', 'papas', 'tabla', 'mantel', 'mueble', 'encimera', 'cubo') else loseta(f, i + 1)
        im.save(os.path.join(T_BLOQ, n + '.png')); hoja.append(im)
    for n, m in MODELOS.items():
        json.dump(m, open(os.path.join(M_BLOQ, n + '.json'), 'w'), indent=2)
        json.dump({'variants': {'': {'model': 'emi_cafe:block/' + n}}}, open(os.path.join(BS, n + '.json'), 'w'), indent=2)
        json.dump({'parent': 'emi_cafe:block/' + n}, open(os.path.join(M_ITEM, n + '.json'), 'w'), indent=2)
    clave = lambda tabla: {('item.emi_cafe.' + n if n in OBJETOS else 'block.emi_cafe.' + n): v for n, v in tabla.items()}
    json.dump(clave(NOMBRES_ES), open(os.path.join(LANG, 'es_es.json'), 'w'), indent=2, ensure_ascii=False)
    json.dump(clave(NOMBRES_EN), open(os.path.join(LANG, 'en_us.json'), 'w'), indent=2, ensure_ascii=False)
    return hoja

if __name__ == '__main__':
    hoja = escribir()
    if '--hoja' in sys.argv:
        salida = sys.argv[sys.argv.index('--hoja') + 1]
        esc = 10; cols = 8; filas = (len(hoja) + cols - 1) // cols
        im = Image.new('RGBA', (cols * 16 * esc + (cols + 1) * 8, filas * 16 * esc + (filas + 1) * 8), (70, 60, 90, 255))
        for i, t in enumerate(hoja):
            x, y = 8 + (i % cols) * (16 * esc + 8), 8 + (i // cols) * (16 * esc + 8)
            im.paste(t.resize((16 * esc, 16 * esc), Image.NEAREST), (x, y), t.resize((16 * esc, 16 * esc), Image.NEAREST))
        im.save(salida); print('hoja:', salida)
    print('texturas y modelos escritos en', os.path.normpath(RAIZ))
