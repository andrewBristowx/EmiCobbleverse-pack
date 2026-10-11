#!/usr/bin/env python3
"""Pokebolas con diseño propio para el paquete de recursos integrado `emi_pokeballs` de Emipokemon.
 - 18 pokebolas de tipo (las que usa la mecanica de Emipokemon: PokeballRules): cada una con la paleta y un dibujo de su tipo
 - Pokebola, Super Ball, Ultra Ball y Master Ball con el diseño del michialien (la bola es un cubo: tapa = ojos, base = boca con luna)
Parte de las texturas de la Poke Ball base de Cobblemon (cubo 8x8x8, textura 64x32) y de su icono (16x16) y las recolorea/dibuja.
Todas pasan a usar el modelo de la Poke Ball base (`cobblemon:poke_ball.geo`), tambien las "ancient".
Uso: python3 gen_pokeballs.py <Cobblemon.jar> <carpeta de salida (sera .../assets/cobblemon)>"""
import sys, os, io, json, zipfile
from PIL import Image

JAR, OUT = sys.argv[1], sys.argv[2]
z = zipfile.ZipFile(JAR)
def leer(p): return Image.open(io.BytesIO(z.read(p))).convert('RGBA')
BASE_ENT = leer('assets/cobblemon/textures/poke_balls/poke_ball.png')
BASE_ICO = leer('assets/cobblemon/textures/item/poke_balls/poke_ball.png')

def rgb(h): return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))
def lerp(a, b, t): return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))
def rampa(osc, med, cla):
    """funcion luminosidad (0..1) -> color con tres tonos"""
    return lambda t: lerp(osc, med, t * 2) if t < 0.5 else lerp(med, cla, (t - 0.5) * 2)
def lum(c): return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255

# ------------------------------------------------------------------ motivos 8x8 (X = acento)
M = {
 'fire': ['...X....', '..XX..X.', '..XXX.XX', '.XXXXXXX', '.XXXXXXX', '.XXXXXXX', '..XXXXX.', '...XXX..'],
 'water': ['........', '.XX..XX.', 'X..XX..X', '........', '.XX..XX.', 'X..XX..X', '........', '........'],
 'grass': ['....XXX.', '...XXXXX', '..XXXXXX', '.XXXXXX.', 'XXXXXX..', '.XXX....', 'X.X.....', '........'],
 'electric': ['....XX..', '...XX...', '..XX....', '.XXXXX..', '...XX...', '..XX....', '.XX.....', '.X......'],
 'ice': ['...XX...', '.X.XX.X.', '..XXXX..', 'XXXXXXXX', 'XXXXXXXX', '..XXXX..', '.X.XX.X.', '...XX...'],
 'fighting': ['.XXXXXX.', 'XXXXXXXX', 'XXXXXXXX', '.XXXXXXX', '.XXXXXX.', '..XXXX..', '..XXXX..', '........'],
 'poison': ['.XX..XX.', '.XX..XX.', '........', '...XX...', '..XXXX..', '..XXXX..', '...XX...', '........'],
 'ground': ['X.......', '.X..X...', '..XX.X..', '...X.XX.', '.XX..X..', '...XX..X', '..X..XX.', '.X......'],
 'flying': ['.....XX.', '...XXXX.', '..XXXXX.', '.XXXXX..', 'XXXXX...', '.XX.....', 'X.......', '........'],
 'psychic': ['..XXXX..', '.X....X.', 'X..XX..X', 'X.XXXX.X', 'X..XX..X', '.X....X.', '..XXXX..', '........'],
 'bug': ['..XXXX..', '.X....X.', 'X..XX..X', 'X.X..X.X', 'X..XX..X', '.X....X.', '..XXXX..', '........'],
 'rock': ['..XXX...', '.XXXXX..', '.XXXXXX.', '..XXXX..', '.....XX.', '.XXX.XXX', 'XXXXXXXX', '.XXXXXX.'],
 'ghost': ['..XXXX..', '.XXXXXX.', '.X.XX.X.', '.XXXXXX.', '.XXXXXX.', '.XXXXXX.', '.X.XX.X.', '........'],
 'dragon': ['.X.X.X.X', 'X.X.X.X.', '.X.X.X.X', 'X.X.X.X.', '.X.X.X.X', 'X.X.X.X.', '.X.X.X.X', 'X.X.X.X.'],
 'dark': ['..XXX...', '.XX.....', 'XX......', 'XX......', 'XX......', '.XX.....', '..XXX...', '........'],
 'steel': ['..X..X..', '.XXXXXX.', 'XX.XX.XX', '.XXXXXX.', '.XXXXXX.', 'XX.XX.XX', '.XXXXXX.', '..X..X..'],
 'fairy': ['...XX...', '...XX...', '.XXXXXX.', 'XXXXXXXX', '.XXXXXX.', '..XXXX..', '.XX..XX.', '.X....X.'],
}
# tipo: (bola de Cobblemon que usa Emipokemon, tapa oscuro/medio/claro, acento, base o None = blanca)
TIPOS = {
 'fire': ('safari_ball', '#9c1c10', '#e8481e', '#ff9a4a', '#ffd23c', None),
 'water': ('dive_ball', '#14449c', '#2f7be0', '#7fc0ff', '#e6f6ff', None),
 'grass': ('ancient_verdant_ball', '#124a1c', '#2e8c3c', '#6fc860', '#e4ff9c', None),
 'electric': ('ancient_citrine_ball', '#b88a10', '#f2cc22', '#fff27a', '#4a3410', None),
 'ice': ('ancient_azure_ball', '#1c6890', '#3fb0da', '#9fe8fa', '#ffffff', None),
 'fighting': ('ancient_slate_ball', '#7a2a0c', '#c85a22', '#ec9a5c', '#fff4d6', None),
 'poison': ('ancient_leaden_ball', '#5a1c7c', '#9a48c2', '#d296ec', '#9cf06a', None),
 'ground': ('sport_ball', '#6a4418', '#b0803c', '#dcb06c', '#4a2c0c', None),
 'flying': ('ancient_wing_ball', '#2c5aa8', '#5a98e8', '#a8d0ff', '#ffffff', None),
 'psychic': ('ancient_roseate_ball', '#a8285e', '#ea5fa4', '#ffaad0', '#ffe8f6', None),
 'bug': ('net_ball', '#62780c', '#98be28', '#d0ee6a', '#2c3c08', None),
 'rock': ('ancient_jet_ball', '#6a5e4c', '#9a8c76', '#cbbfa6', '#3a3226', None),
 'ghost': ('ancient_ivory_ball', '#341c5c', '#5a3a98', '#9a78d0', '#f0e6ff', '#c8bde0'),
 'dragon': ('slate_ball', '#1c2678', '#3c4cc0', '#8090f0', '#ffd24a', None),
 'dark': ('dusk_ball', '#14101c', '#2e2840', '#5a5078', '#ffd84a', '#5a5470'),
 'steel': ('ancient_gigaton_ball', '#68748c', '#a6b2c6', '#e0e8f4', '#3c4658', '#c6cede'),
 'fairy': ('ancient_feather_ball', '#c8588c', '#f6a0cc', '#ffd8ee', '#ffffff', None),
}

# motivos para los iconos 16x16 (7 columnas x 5 filas, se colocan en x 4..10, y 2..6 de la tapa)
MI = {
 'fire': ['..X....', '.XX.X..', '.XXXXX.', 'XXXXXXX', '.XXXXX.'],
 'water': ['...X...', '..XXX..', '.XXXXX.', '.XXXXX.', '..XXX..'],
 'grass': ['....XXX', '..XXXXX', '.XXXXX.', 'XXXXX..', 'X.X....'],
 'electric': ['...XXX.', '..XXX..', '.XXXXX.', '...XX..', '..XX...'],
 'ice': ['X..X..X', '.X.X.X.', '..XXX..', '.X.X.X.', 'X..X..X'],
 'fighting': ['.XXXXX.', 'XXXXXXX', 'XXXXXXX', '.XXXXX.', '..XXX..'],
 'poison': ['.X...X.', 'XXX.XXX', '.X.XXX.', '...XXX.', '....X..'],
 'ground': ['X......', '.XX....', '...XX..', '.....XX', '......X'],
 'flying': ['....XXX', '..XXXX.', 'XXXXX..', '.XXX...', '..X....'],
 'psychic': ['.XXXXX.', 'X.....X', 'X.XXX.X', 'X.....X', '.XXXXX.'],
 'bug': ['..XXX..', '.X...X.', 'X..X..X', '.X...X.', '..XXX..'],
 'rock': ['..XXX..', '.XXXXX.', 'XXXXXXX', '.X...XX', 'XXX.XXX'],
 'ghost': ['..XXX..', '.XXXXX.', '.X.X.X.', '.XXXXX.', '.X.X.X.'],
 'dragon': ['X.....X', 'XX...XX', '.XX.XX.', '..XXX..', '...X...'],
 'dark': ['..XXX..', '.XX....', '.XX....', '.XX....', '..XXX..'],
 'steel': ['.X.X.X.', 'XXXXXXX', '.XX.XX.', 'XXXXXXX', '.X.X.X.'],
 'fairy': ['...X...', '...X...', 'XXXXXXX', '..XXX..', '.XX.XX.'],
}
# michialien: (bola, tapa, acento, base, adorno)
MICHI = {
 'poke_ball': ('#a81e1e', '#e0443c', '#ff8a78', '#ffffff', None),
 'great_ball': ('#1c3e98', '#3a78e0', '#8cc0ff', '#ffffff', 'lazo'),
 'ultra_ball': ('#16162a', '#34344e', '#62628a', '#ffffff', 'corona'),
 'master_ball': ('#5a1c9c', '#9650d6', '#d6a0ff', '#ffffff', 'aureola'),
}
MORADO = (110, 62, 176); MORADO_O = (70, 36, 120); BRILLO = (236, 214, 255); NEGRO = (28, 22, 40); ROSA = (255, 150, 196); ORO = (255, 208, 52); ORO_D = (200, 140, 20)

# caras de la textura 64x32 (x, y, ancho, alto): la tapa (caja en 0,0) y la base (caja en 32,0), cada una con su capa exterior inflada (y+20)
def caras(ox):
    d = {}
    for cap, dy in (('in', 0), ('out', 20)):
        d[('top', cap)] = (ox + 8, dy, 8, 8); d[('bottom', cap)] = (ox + 16, dy, 8, 8)
        for k, dx in (('east', 0), ('north', 8), ('west', 16), ('south', 24)):
            d[(k, cap)] = (ox + dx, dy + 8, 8, 4)
    return d
TAPA, BASE = caras(0), caras(32)

def recolor(im, rects, ramp, solo=lambda c: True):
    P = im.load(); lums = []
    for (x, y, w, h) in rects:
        for j in range(h):
            for i in range(w):
                c = P[x + i, y + j]
                if c[3] and lum(c) > 0.3 and solo(c): lums.append(lum(c))
    if not lums: return
    lo, hi = min(lums), max(lums)
    for (x, y, w, h) in rects:
        for j in range(h):
            for i in range(w):
                c = P[x + i, y + j]
                if c[3] and lum(c) > 0.3 and solo(c):
                    P[x + i, y + j] = ramp((lum(c) - lo) / (hi - lo + 1e-6)) + (255,)

def pintar(im, caras_, motivo, color, f0=0, f1=8, c0=0, c1=8):
    """dibuja el motivo (lista de 8 cadenas) en la cara, recortado a filas f0..f1; en las caras de lado solo caben las filas f0..f0+3"""
    P = im.load(); x, y, w, h = caras_
    for j in range(h):
        for i in range(w):
            try: ch = motivo[f0 + j][i]
            except IndexError: continue
            if ch == 'X': P[x + i, y + j] = tuple(color) + (255,)

def pixeles(im, caras_, pts, color):
    P = im.load(); x, y, w, h = caras_
    for i, j in pts:
        if 0 <= i < w and 0 <= j < h: P[x + i, y + j] = tuple(color) + (255,)

def textura_tipo(t):
    bola, osc, med, cla, acento, base = TIPOS[t]
    im = BASE_ENT.copy()
    recolor(im, [TAPA[k] for k in TAPA], rampa(rgb(osc), rgb(med), rgb(cla)), solo=lambda c: c[0] > c[1] * 1.5)
    if base: recolor(im, [BASE[k] for k in BASE], rampa(tuple(int(v * .55) for v in rgb(base)), rgb(base), (255, 255, 255)), solo=lambda c: abs(c[0] - c[1]) < 25)
    mo = M[t]; col = rgb(acento)
    for cap in ('in', 'out'):
        pintar(im, TAPA[('top', cap)], mo, col)
        for k in ('north', 'east', 'west', 'south'):
            pintar(im, TAPA[(k, cap)], mo, col, f0=2)           # 3 filas visibles encima de la banda negra; se usan las filas 2..4 del dibujo
    # boton: del color del acento con un punto claro
    P = im.load(); P[0, 0] = (255, 255, 255, 255); P[1, 0] = tuple(min(255, v + 40) for v in col) + (255,); P[0, 1] = tuple(col) + (255,); P[1, 1] = tuple(int(v * .7) for v in col) + (255,)
    return im

def michi_cara(im, color_o, color_m, adorno):
    """ojos enfadados morados en la tapa, boca oscura con luna en la base, antenas en lo alto, orejas a los lados"""
    for cap in ('in', 'out'):
        n = TAPA[('north', cap)]
        pixeles(im, n, [(1, 0), (2, 0), (5, 0), (6, 0)], NEGRO)                          # cejas
        pixeles(im, n, [(1, 1), (2, 1), (5, 1), (6, 1), (1, 2), (2, 2), (5, 2), (6, 2)], MORADO)   # ojos
        pixeles(im, n, [(1, 1), (5, 1)], BRILLO)
        b = BASE[('north', cap)]
        pixeles(im, b, [(2, 1), (3, 1), (4, 1), (5, 1), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2), (2, 3), (3, 3), (4, 3), (5, 3)], NEGRO)
        pixeles(im, b, [(2, 2), (2, 3), (3, 3)], MORADO)                                 # luna creciente
        t = TAPA[('top', cap)]
        pixeles(im, t, [(1, 1), (2, 1), (1, 2), (2, 2), (5, 1), (6, 1), (5, 2), (6, 2)], MORADO)  # bolitas de las antenas
        pixeles(im, t, [(1, 1), (5, 1)], BRILLO); pixeles(im, t, [(2, 2), (6, 2)], MORADO_O)
        pixeles(im, t, [(1, 3), (5, 3)], (200, 196, 215))                                # tallos
        for k in ('east', 'west'):
            pixeles(im, TAPA[(k, cap)], [(3, 0), (4, 0), (3, 1), (4, 1)], MORADO)       # orejas moradas
        if adorno == 'lazo':
            pixeles(im, t, [(2, 5), (3, 5), (4, 5), (5, 5), (2, 6), (3, 6), (4, 6), (5, 6), (3, 5), (4, 6)], ROSA); pixeles(im, t, [(3, 5), (4, 5)], (255, 220, 235))
            pixeles(im, t, [(3, 6), (4, 6)], (230, 90, 150))
        if adorno == 'corona':
            pixeles(im, t, [(1, 6), (2, 6), (3, 6), (4, 6), (5, 6), (6, 6), (1, 5), (3, 5), (4, 5), (6, 5), (3, 4), (4, 4)], ORO); pixeles(im, t, [(1, 6), (4, 5)], (255, 244, 160))
            pixeles(im, t, [(2, 6), (5, 6)], (230, 60, 90))
        if adorno == 'aureola':
            pixeles(im, t, [(i, 0) for i in range(8)] + [(i, 7) for i in range(8)] + [(0, j) for j in range(8)] + [(7, j) for j in range(8)], ORO)
            pixeles(im, t, [(0, 0), (7, 0), (0, 7), (7, 7)], (255, 244, 160))
        if adorno == 'aureola':
            for k in ('north', 'east', 'west', 'south'):
                pixeles(im, TAPA[(k, cap)], [(0, 0), (7, 0)], ORO)
    P = im.load()
    P[0, 0] = (255, 255, 255, 255); P[1, 0] = (255, 190, 215, 255); P[0, 1] = (255, 150, 196, 255); P[1, 1] = (214, 100, 150, 255)     # boton = nariz rosa

def textura_michi(b):
    osc, med, cla, base, adorno = MICHI[b]
    im = BASE_ENT.copy()
    recolor(im, [TAPA[k] for k in TAPA], rampa(rgb(osc), rgb(med), rgb(cla)), solo=lambda c: c[0] > c[1] * 1.5)
    michi_cara(im, rgb(med), rgb(base), adorno)
    if b == 'great_ball':                                                                # franja roja a los lados, como la Super Ball original
        for cap in ('in', 'out'):
            for k in ('east', 'west'): pixeles(im, TAPA[(k, cap)], [(0, 1), (1, 1), (6, 1), (7, 1), (0, 2), (7, 2)], (226, 52, 52))
    if b == 'ultra_ball':
        for cap in ('in', 'out'):
            for k in ('east', 'west', 'north', 'south'): pixeles(im, TAPA[(k, cap)], [(0, 1), (7, 1), (0, 2), (7, 2)], (250, 200, 40))
    if b == 'master_ball':
        for cap in ('in', 'out'):
            for k in ('east', 'west', 'south'): pixeles(im, TAPA[(k, cap)], [(2, 1), (5, 1), (3, 2), (4, 2)], (255, 130, 190))
        P = im.load(); P[0, 0] = (255, 244, 160, 255)
    return im

# ------------------------------------------------------------------ iconos 16x16: la bola base recoloreada con un recorte del motivo / la carita
def icono_base(tapa_ramp, base_ramp):
    ic = BASE_ICO.copy(); P = ic.load(); lids = []; bots = []
    for y in range(16):
        for x in range(16):
            c = P[x, y]
            if not c[3]: continue
            if c[0] > c[1] * 1.5 and lum(c) > 0.2: lids.append(lum(c))
            elif abs(c[0] - c[1]) < 25 and lum(c) > 0.45: bots.append(lum(c))
    lo, hi = min(lids), max(lids); blo, bhi = min(bots), max(bots)
    for y in range(16):
        for x in range(16):
            c = P[x, y]
            if not c[3]: continue
            if c[0] > c[1] * 1.5 and lum(c) > 0.2: P[x, y] = tapa_ramp((lum(c) - lo) / (hi - lo + 1e-6)) + (255,)
            elif base_ramp and abs(c[0] - c[1]) < 25 and lum(c) > 0.45: P[x, y] = base_ramp((lum(c) - blo) / (bhi - blo + 1e-6)) + (255,)
    return ic

def icono_tipo(t):
    bola, osc, med, cla, acento, base = TIPOS[t]
    ic = icono_base(rampa(rgb(osc), rgb(med), rgb(cla)), (rampa(tuple(int(v * .55) for v in rgb(base)), rgb(base), (255, 255, 255)) if base else None))
    P = ic.load(); mo = MI[t]; col = rgb(acento); osc_c = tuple(int(v * .45) for v in rgb(osc)) if lum(rgb(acento)) > lum(rgb(med)) else rgb(cla)
    pts = [(4 + i, 2 + j) for j in range(5) for i in range(7) if mo[j][i] == 'X']
    S = set(pts)
    for (x, y) in pts:                                                                    # contorno oscuro de 1 px para que se lea el dibujo
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in S and 0 <= q[0] < 16 and 0 <= q[1] < 16 and P[q][3] and P[q][0] > P[q][1] * 1.0 + 0 and lum(P[q]) > 0.2 and q[1] <= 7: P[q] = osc_c + (255,)
    for (x, y) in pts: P[x, y] = tuple(col) + (255,)
    return ic

def icono_michi(b):
    osc, med, cla, base, adorno = MICHI[b]
    ic = icono_base(rampa(rgb(osc), rgb(med), rgb(cla)), None); P = ic.load()
    def px(pts, c):
        for x, y in pts: 
            if P[x, y][3]: P[x, y] = tuple(c) + (255,)
    px([(4, 4), (5, 4), (9, 4), (10, 4), (4, 5), (5, 5), (9, 5), (10, 5)], MORADO); px([(4, 4), (9, 4)], BRILLO); px([(3, 3), (4, 3), (10, 3), (11, 3)], NEGRO)
    px([(7, 6), (8, 6)], ROSA)                                                           # nariz (donde estaba el boton)
    px([(6, 12), (7, 12), (8, 12), (9, 12), (6, 13), (7, 13), (8, 13)], NEGRO); px([(6, 12), (6, 13)], MORADO)
    px([(4, 1), (5, 1), (10, 1), (11, 1), (4, 2), (11, 2)], MORADO); px([(4, 1), (10, 1)], BRILLO)  # antenas
    if adorno == 'lazo': px([(1, 5), (2, 4), (2, 5), (1, 6), (2, 6), (13, 4), (13, 5), (14, 5), (13, 6), (14, 6)], ROSA)
    if adorno == 'corona': px([(6, 0), (8, 0), (10, 0), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1)], ORO)
    if adorno == 'aureola': px([(5, 0), (6, 0), (7, 0), (8, 0), (9, 0), (10, 0)], ORO)
    return ic

# ------------------------------------------------------------------ salida
def guardar(im, ruta):
    os.makedirs(os.path.dirname(ruta), exist_ok=True); im.save(ruta)

variaciones = []
bolas = [(b, textura_michi(b), icono_michi(b)) for b in MICHI] + [(TIPOS[t][0], textura_tipo(t), icono_tipo(t)) for t in TIPOS]
for nombre, ent, ico in bolas:
    guardar(ent, f'{OUT}/textures/poke_balls/{nombre}.png')
    guardar(ent, f'{OUT}/textures/item/poke_balls/models/{nombre}.png')
    guardar(ico, f'{OUT}/textures/item/poke_balls/{nombre}.png')
    # todas con el modelo de la Poke Ball base (las "ancient" tenian orejas/alas propias)
    os.makedirs(f'{OUT}/bedrock/poke_balls/variations', exist_ok=True); os.makedirs(f'{OUT}/models/item', exist_ok=True)
    json.dump({'pokeball': f'cobblemon:{nombre}', 'order': 0, 'variations': [{'aspects': [], 'poser': 'cobblemon:poke_ball', 'model': 'cobblemon:poke_ball.geo',
               'texture': f'cobblemon:textures/poke_balls/{nombre}.png', 'layers': []}]}, open(f'{OUT}/bedrock/poke_balls/variations/0_{nombre}_base.json', 'w'))
    if nombre != 'poke_ball':   # `poke_ball_model` es el modelo base (padre de todos): reescribirlo con padre = si mismo rompe TODAS las bolas en la mano
        json.dump({'parent': 'cobblemon:item/poke_ball_model', 'textures': {'layer0': f'cobblemon:item/poke_balls/models/{nombre}'}}, open(f'{OUT}/models/item/{nombre}_model.json', 'w'))
print(len(bolas), 'pokebolas en', OUT)
