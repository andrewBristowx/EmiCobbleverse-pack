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

OBJETOS = {'papa': papa, 'papa_tiras': papa_tiras, 'papas_fritas': papas_fritas, 'burbuja': burbuja, 'moneda': moneda}

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

TILES = {
    'madera': t_madera(C('c48a52'), C('8f5e36'), C('dba570')), 'madera_oscura': t_madera(C('8a5a34'), C('5e3a20'), C('a3733f')),
    'metal': t_metal(C('c4cad4'), C('7c8494'), C('eef2f8')), 'metal_oscuro': t_metal(C('767e8e'), C('4a505e'), C('9aa2b2')),
    'aceite': t_aceite, 'papas': t_papas, 'tabla': t_tabla, 'mantel': t_mantel, 'mueble': t_mueble, 'encimera': t_encimera, 'cubo': t_cubo,
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
}

NOMBRES_ES = {'papa': 'Papa', 'papa_tiras': 'Papa en tiras', 'papas_fritas': 'Papas fritas', 'burbuja': 'Burbuja de pedido', 'moneda': 'Moneda del Café',
              'caja_papas': 'Caja de papas', 'tabla_cortar': 'Tabla de cortar', 'freidora': 'Freidora', 'basura': 'Cubo de basura', 'mesa': 'Mesa del café', 'encimera': 'Encimera'}
NOMBRES_EN = {'papa': 'Potato', 'papa_tiras': 'Potato Strips', 'papas_fritas': 'French Fries', 'burbuja': 'Order Bubble', 'moneda': 'Cafe Coin',
              'caja_papas': 'Potato Crate', 'tabla_cortar': 'Cutting Board', 'freidora': 'Fryer', 'basura': 'Trash Bin', 'mesa': 'Cafe Table', 'encimera': 'Counter'}

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
