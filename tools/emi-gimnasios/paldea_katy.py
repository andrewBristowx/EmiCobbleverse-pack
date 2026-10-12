"""Gimnasio de Cortondo (Katy, Bicho): pasteleria con forma de tarta de tres pisos, con glaseado, fresas, velas y un atrio con mesas de cafe."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 41, 40, 44
CX, CZ = 20, 25
PISOS = [(15, 1, 9), (11, 10, 17), (7, 18, 24)]       # (radio, y0, y1)


def mariquita(v, cx, cy, z, r):
    elipse_xy(v, cx, cy, z, r, r * 0.85, lambda d, a: 'black_concrete' if d > 0.9 else 'red_concrete')
    v.fill(cx, int(cy - r * 0.85), z, cx, int(cy + r * 0.85), z, 'black_concrete')
    for (dx, dy) in ((-3, 1), (3, 1), (-2, -2), (2, -2), (-4, -1), (4, -1)):
        v.set(cx + dx, cy + dy, z, 'black_concrete'); v.set(cx + dx + (1 if dx < 0 else -1), cy + dy, z, 'black_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(221)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'pink_terracotta')
    for x in range(W):
        for z in range(L):
            if (x // 2 + z // 2) % 2: v.set(x, 0, z, 'white_terracotta')
    # ---- los tres pisos de la tarta: bizcocho, glaseado rosa y nata blanca
    for (r, y0, y1) in PISOS:
        v.cilindro(CX, y0, y1, CZ, r, 'yellow_terracotta' if r > 7 else 'orange_terracotta')
        for y in range(y0, y1 + 1):
            if (y - y0) % 4 == 3: v.anillo_circ(CX, y, CZ, r, 'brown_terracotta', 1.0)
        v.anillo_circ(CX, y0 + 1, CZ, r, 'pink_concrete', 1.2)
        v.disco(CX, y1, CZ, r, 'white_concrete'); v.anillo_circ(CX, y1, CZ, r + 1, 'pink_concrete', 1.4)
        for k in range(rnd.randint(14, 20)):                                              # chorreones de nata
            a = rnd.random() * 2 * math.pi
            x = round(CX + math.cos(a) * (r + 1)); z = round(CZ + math.sin(a) * (r + 1))
            for dy in range(rnd.randint(1, 4)): v.set(x, y1 - dy, z, 'white_concrete')
        for k in range(10):                                                                # fresas en el borde
            a = k / 10 * 2 * math.pi + y0
            x = round(CX + math.cos(a) * (r - 0.5)); z = round(CZ + math.sin(a) * (r - 0.5))
            v.set(x, y1 + 1, z, 'red_concrete'); v.set(x, y1 + 2, z, 'lime_concrete') if rnd.random() < 0.5 else None
    v.disco(CX, 25, CZ, 7, 'pink_concrete')
    for k in range(8):                                                                      # velas encendidas en la cima
        a = k / 8 * 2 * math.pi
        x = round(CX + math.cos(a) * 4); z = round(CZ + math.sin(a) * 4)
        v.set(x, 26, z, 'candle[candles=2,lit=true]')
    v.set(CX, 26, CZ, 'candle[candles=4,lit=true]')
    # ---- sala redonda de la planta baja: suelo de anillos de colores y claraboya rosa en el centro del techo
    disco_f(v, CX, 1, CZ, 14, anillos([(2, 'yellow_concrete'), (5, 'pink_concrete'), (8, 'white_concrete'), (11, 'pink_concrete'), (14, 'white_concrete')]))
    disco_f(v, CX, 9, CZ, 5, lambda d, a: 'pink_stained_glass')
    # ---- puerta: arco de caramelo con mariquita gigante encima (en la pared de la fachada plana)
    zf = CZ - PISOS[0][0]
    v.fill(CX - 8, 1, zf - 2, CX + 8, 16, zf - 2, 'white_concrete')
    for y in range(1, 17): v.set(CX - 8, y, zf - 2, 'pink_concrete'); v.set(CX + 8, y, zf - 2, 'pink_concrete')
    v.aire(CX - 2, 1, zf - 2, CX + 2, 7, zf - 1)
    hueco_puerta(v, CX, zf, 5, 7, 'red_concrete', y0=1, prof=1)
    v.fill(CX - 3, 8, zf - 2, CX + 3, 8, zf - 2, 'red_concrete')
    mariquita(v, CX, 12, zf - 2, 4)
    for dx in (-3, 3): v.fill(CX + dx, 1, zf - 2, CX + dx, 7, zf - 2, 'red_concrete')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, zf - 3, 'quartz', 'north') if abs(x - CX) <= 2 else None
    # setos y flores de azucar: piruletas a los lados
    for (x, z) in [(CX - 12, 5), (CX + 12, 5), (CX - 15, 11), (CX + 15, 11), (4, 20), (36, 20)]:
        v.fill(x, 1, z, x, 7, z, 'white_concrete'); v.esfera(x, 9, z, 2, rnd.choice(['pink_concrete', 'magenta_concrete', 'red_concrete', 'orange_concrete']))
        v.set(x, 9, z, 'white_concrete')
    # ---- interior: mesas de cafe, mostrador de cristal, panales colgantes y el estrado
    for k in range(6):
        a = k / 6 * 2 * math.pi + 0.5
        x = round(CX + math.cos(a) * 10); z = round(CZ + math.sin(a) * 10)
        if math.hypot(x - CX, z - (CZ - 13)) < 4: continue
        v.set(x, 3, z, 'quartz_slab[type=bottom]'); v.set(x, 4, z, 'candle[candles=1,lit=true]')
        for (dx, dz) in ((2, 0), (-2, 0), (0, 2), (0, -2)): v.stairs(x + dx, 2, z + dz, 'quartz', 'north')
    for k in range(10):
        a = k / 10 * 2 * math.pi
        x = round(CX + math.cos(a) * 9); z = round(CZ + math.sin(a) * 9)
        largo = 2 + k % 3
        for dy in range(1, largo + 1): v.set(x, 9 - dy, z, 'chain[axis=y]')
        v.set(x, 9 - largo - 1, z, 'shroomlight')
    for dx in (-4, 4):
        for dy in range(1, 3): v.set(CX + dx, 9 - dy, CZ, 'chain[axis=y]')
        v.set(CX + dx, 6, CZ, 'honeycomb_block')
    # estrado del lider: tarta pequeña
    disco_f(v, CX, 2, CZ + 7, 4, anillos([(4, 'white_concrete')])); disco_f(v, CX, 3, CZ + 7, 3, anillos([(3, 'pink_concrete')]))
    for x in range(CX - 4, CX + 5): v.stairs(x, 2, CZ + 2, 'quartz', 'north') if abs(x - CX) <= 1 else None
    v.fill(CX - 4, 3, CZ + 11, CX + 4, 9, CZ + 11, 'pink_concrete'); mariquita(v, CX, 7, CZ + 11, 3)
    return v, [(CX, 4, CZ + 7, 180, 'emipokemon desafios paldea bicho')]
