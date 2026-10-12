"""Gimnasio de Cyllage (Grant, Roca): un acantilado con la pared de escalada por fuera y una caverna de fosiles por dentro."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 37, 36, 42
CX, CZ = 18, 25
ZF = 8                  # cara del acantilado (fachada)


def esqueleto(v, x, y, z, largo, alto, hueso='bone_block'):
    """esqueleto de dinosaurio tumbado a lo largo de z (cabeza hacia -z): columna, costillas, patas, cola y craneo"""
    for i in range(largo):
        yy = y + alto + (1 if 2 < i < largo - 4 else 0)
        v.set(x, yy, z + i, hueso)
        if 3 <= i <= largo - 5 and i % 2 == 0:                      # costillas
            for s in (-1, 1):
                v.set(x + s, yy - 1, z + i, hueso); v.set(x + 2 * s, yy - 2, z + i, hueso)
                v.set(x + 2 * s, yy - 3, z + i, hueso)
    for s in (-1, 1):                                                # patas
        v.fill(x + 2 * s, y, z + 5, x + 2 * s, y + alto - 1, z + 5, hueso)
        v.set(x + 2 * s, y, z + 4, hueso)
    for i in range(largo - 4, largo): v.set(x, y + alto - (i - (largo - 5)) // 2, z + i, hueso)   # cola que baja
    v.fill(x - 1, y + alto, z - 3, x + 1, y + alto + 1, z - 1, hueso)                           # craneo
    v.set(x, y + alto, z - 4, hueso); v.set(x, y + alto - 1, z - 3, 'bone_block')
    v.set(x - 1, y + alto + 1, z - 2, 'black_concrete'); v.set(x + 1, y + alto + 1, z - 2, 'black_concrete')   # cuencas


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(51)
    ROCAS = ['stone'] * 8 + ['andesite'] * 4 + ['tuff'] * 2 + ['cobblestone'] * 2 + ['polished_andesite']
    # ---- suelo de grava y explanada
    for x in range(W):
        for z in range(L):
            v.set(x, 0, z, rnd.choice(['gravel', 'coarse_dirt', 'stone', 'andesite']))
    for z in range(0, ZF):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'polished_andesite')
    # ---- masa del acantilado: planta de rectangulo redondeado (superelipse) con la pared frontal recta
    alt = {}
    for x in range(2, 35):
        for z in range(ZF, 41):
            d = (abs(x - CX) / 16.5) ** 5 + (abs(z - 24.5) / 16.5) ** 5
            if d > 1.0 + rnd.random() * 0.12: continue
            h = 29 - 1.1 * max(0, abs(x - CX) - 6) - 0.75 * max(0, z - 27) + rnd.random() * 2.5
            h -= 3 * (d ** 2)
            alt[(x, z)] = int(max(4, h))
    for (x, z), h in alt.items():
        for y in range(1, h + 1): v.set(x, y, z, rnd.choice(ROCAS))
        v.set(x, h, z, rnd.choice(['stone', 'andesite', 'moss_block', 'grass_block']))
    # agujas de roca a los lados y detras
    for (x, z, r, h) in [(4, 14, 3, 20), (32, 18, 3, 24), (CX - 11, 36, 4, 22), (CX + 10, 37, 3, 19), (CX, 38, 4, 26)]:
        cono(v, x, 4, z, r, h, rnd.choice(['stone', 'andesite']))
        for k in range(h): v.set(x, 4 + k, z, 'stone') if v.get(x, 4 + k, z) == 'air' else None
    # ---- caverna (elipsoide) y tunel de entrada
    cav = (CX, 1, CZ, 12.5, 13, 13.5)
    for x in range(3, 34):
        for z in range(ZF + 1, 40):
            for y in range(1, 30):
                d = math.sqrt(((x - cav[0]) / cav[3]) ** 2 + ((y - 1) / cav[4]) ** 2 + ((z - cav[2]) / cav[5]) ** 2)
                if d < 1.0 and (y >= 1): v.set(x, y, z, 'air')
    for z in range(ZF, CZ - 8):
        for x in range(CX - 3, CX + 4):
            for y in range(1, 8 - (abs(x - CX) // 2)): v.set(x, y, z, 'air')
    for x in range(CX - 3, CX + 4): v.set(x, 0, ZF, 'polished_andesite')
    # el suelo de la caverna: roca con zona de combate de gravilla
    for x in range(3, 34):
        for z in range(ZF, 40):
            if v.get(x, 1, z) == 'air': v.set(x, 0, z, rnd.choice(['stone', 'andesite', 'gravel']))
    disco_f(v, CX, 0, CZ + 2, 10, anillos([(1.5, 'orange_terracotta'), (4.5, 'smooth_stone'), (5.5, 'orange_terracotta'), (8.5, 'smooth_stone'), (10, 'gravel')]))
    # ---- pared de escalada exterior con presas de colores y una cuerda
    colores = ['red_concrete', 'yellow_concrete', 'blue_concrete', 'lime_concrete', 'orange_concrete', 'purple_concrete', 'white_concrete', 'pink_concrete']
    for x in range(5, 32):
        for y in range(4, 27):
            if rnd.random() < 0.09 and v.get(x, y, ZF) != 'air': v.set(x, y, ZF - 1, rnd.choice(colores))
    for (x, ang) in [(CX - 7, 0), (CX + 7, 0)]:                       # lineas de ruta con presas amarillas de abajo arriba
        for y in range(4, 24, 3): v.set(x + (y % 2), y, ZF - 1, 'yellow_concrete')
    v.fill(CX - 4, 10, ZF - 1, CX - 4, 25, ZF - 1, 'chain[axis=y]'); v.fill(CX + 4, 10, ZF - 1, CX + 4, 25, ZF - 1, 'chain[axis=y]')
    # arco de entrada: marco de vigas de madera
    for y in range(1, 9): v.set(CX - 4, y, ZF - 1, 'stripped_spruce_log[axis=y]'); v.set(CX + 4, y, ZF - 1, 'stripped_spruce_log[axis=y]')
    v.fill(CX - 4, 9, ZF - 1, CX + 4, 9, ZF - 1, 'stripped_spruce_log[axis=x]')
    v.fill(CX - 4, 10, ZF - 1, CX + 4, 10, ZF - 1, 'dark_oak_planks')
    for x in range(CX - 3, CX + 4): v.set(x, 11, ZF - 1, 'spruce_slab[type=bottom]')
    # grandes rocas de la explanada y rocas con musgo
    for (x, z, r) in [(5, 3, 2), (31, 4, 3), (8, 6, 1), (28, 6, 2), (2, 7, 2)]:
        v.esfera(x, 1, z, r, rnd.choice(['stone', 'andesite', 'mossy_cobblestone']))
    # ---- interior: estalactitas, lamparas y estalagmitas
    for _ in range(90):
        x = rnd.randint(5, 31); z = rnd.randint(ZF + 3, 38)
        for y in range(28, 4, -1):
            if v.get(x, y, z) != 'air' and v.get(x, y - 1, z) == 'air' and v.get(x, y, z) not in ('lantern',):
                n = rnd.randint(1, 3)
                for k in range(1, n + 1):
                    if v.get(x, y - k, z) == 'air': v.set(x, y - k, z, 'pointed_dripstone[thickness=%s,vertical_direction=down,waterlogged=false]' % ('tip' if k == n else 'frustum'))
                break
    for (x, z) in [(CX - 6, CZ - 6), (CX + 6, CZ - 6), (CX - 9, CZ + 2), (CX + 9, CZ + 2), (CX - 4, CZ + 9), (CX + 4, CZ + 9), (CX, CZ - 8)]:
        for y in range(28, 5, -1):
            if v.get(x, y, z) != 'air' and v.get(x, y - 1, z) == 'air' and not v.get(x, y, z).startswith(('lantern', 'chain', 'pointed')):
                n = max(2, min(7, y - 9))
                for k in range(1, n + 1): v.set(x, y - k, z, 'chain[axis=y]')
                v.set(x, y - n - 1, z, 'lantern[hanging=true]')
                break
    # fosiles: dos esqueletos y vitrinas con vidrio
    esqueleto(v, CX - 9, 1, CZ - 3, 11, 3)
    esqueleto(v, CX + 9, 1, CZ - 1, 9, 2)
    for z in (CZ + 6, CZ + 9):
        v.fill(CX - 12, 1, z, CX - 11, 1, z, 'stone_bricks'); v.set(CX - 12, 2, z, 'glass'); v.set(CX - 11, 2, z, 'bone_block')
    # pared de escalada del fondo (interior) con presas y el liston del lider
    for x in range(CX - 8, CX + 9):
        for y in range(3, 22):
            if v.get(x, y, 39) != 'air' and v.get(x, y, 38) == 'air' and rnd.random() < 0.12: v.set(x, y, 38, rnd.choice(colores))
    # ---- plataforma del lider: peana de roca con escalones
    for x in range(CX - 5, CX + 6):
        for z in range(CZ + 8, CZ + 14):
            if v.get(x, 1, z) == 'air':
                v.fill(x, 1, z, x, 2, z, 'stone_bricks')
    for x in range(CX - 3, CX + 4):
        for z in range(CZ + 10, CZ + 14):
            if v.get(x, 3, z) == 'air': v.set(x, 3, z, 'polished_andesite')
    for x in range(CX - 5, CX + 6): v.stairs(x, 2, CZ + 7, 'stone_brick', 'north') if v.get(x, 2, CZ + 7) == 'air' else None
    for x in range(CX - 3, CX + 4): v.stairs(x, 3, CZ + 9, 'stone_brick', 'north') if v.get(x, 3, CZ + 9) == 'air' else None
    return v, [(CX, 4, CZ + 11, 180, 'emipokemon desafios kalos risco')]
