"""Gimnasio de Coumarine (Ramos, Planta): invernadero victoriano de cobre y cristal con un arbol enorme dentro."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 37, 32, 42
CX = 18
X0, X1 = 3, 33
Z0, Z1 = 7, 39
R = 15.5            # radio de la boveda
YS = 11             # arranque de la boveda


def boveda(v, z0, z1, vidrio, costilla, cada=5):
    """boveda de cañon de cristal a lo largo de z con costillas de cobre"""
    for i in range(0, 1201):
        a = math.pi * i / 1200
        x = CX + math.cos(a) * R; y = YS + math.sin(a) * R * 0.95
        xi, yi = round(x), round(y)
        for z in range(z0, z1 + 1):
            if v.get(xi, yi, z) == 'air': v.set(xi, yi, z, vidrio)
    for z in range(z0, z1 + 1, cada):
        for i in range(0, 1201):
            a = math.pi * i / 1200
            v.set(round(CX + math.cos(a) * R), round(YS + math.sin(a) * R * 0.95), z, costilla)
            v.set(round(CX + math.cos(a) * (R - 0.8)), round(YS + math.sin(a) * (R - 0.8) * 0.95), z, costilla)


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(71)
    COB, VID, PIE = 'waxed_oxidized_copper', 'glass', 'stone_bricks'
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'mossy_stone_bricks')
    for z in range(0, Z0):
        for x in range(W):
            v.set(x, 0, z, 'polished_andesite' if abs(x - CX) <= 3 else 'grass_block')
    # jardin delantero: setos y flores
    for x in list(range(1, CX - 4)) + list(range(CX + 5, W - 1)):
        for z in range(1, Z0 - 1):
            if rnd.random() < 0.45: v.set(x, 1, z, rnd.choice(['poppy', 'cornflower', 'allium', 'pink_tulip', 'fern', 'azure_bluet', 'oxeye_daisy']))
    for (x, z) in [(3, 2), (33, 2), (8, 4), (28, 4)]:
        arbol(v, x, 1, z, 4, 2, 'oak_leaves', 'oak_log[axis=y]', rnd)
    # ---- zocalo, muros de cristal y columnas de cobre
    for y in range(1, YS + 1):
        for (x, z) in [(X0, None), (X1, None)]:
            v.fill(x, y, Z0, x, y, Z1, PIE if y <= 3 else VID)
        v.fill(X0, y, Z1, X1, y, Z1, PIE if y <= 3 else VID)
        v.fill(X0, y, Z0, X1, y, Z0, PIE if y <= 3 else VID)
    for z in range(Z0, Z1 + 1, 5):
        v.fill(X0, 1, z, X0, YS, z, COB); v.fill(X1, 1, z, X1, YS, z, COB)
    for x in range(X0, X1 + 1, 5):
        v.fill(x, 1, Z0, x, YS, Z0, COB); v.fill(x, 1, Z1, x, YS, Z1, COB)
    v.anillo(X0, Z0, X1, Z1, YS, 'waxed_oxidized_cut_copper')
    boveda(v, Z0, Z1, VID, COB)
    # frontones de cristal en ambos extremos de la boveda
    for z in (Z0, Z1):
        for x in range(X0, X1 + 1):
            h = YS + math.sqrt(max(0, 1 - ((x - CX) / R) ** 2)) * R * 0.95
            for y in range(YS + 1, int(h)): v.set(x, y, z, VID)
        for x in range(X0, X1 + 1, 3):
            h = YS + math.sqrt(max(0, 1 - ((x - CX) / R) ** 2)) * R * 0.95
            for y in range(YS + 1, int(h) + 1): v.set(x, y, z, COB)
    # puerta principal
    hueco_puerta(v, CX, Z0, 5, 7, COB, y0=1, prof=1)
    for k in range(3): v.set(CX - 2 + k * 2, 9, Z0, 'waxed_oxidized_cut_copper')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, Z0 - 1, 'stone_brick', 'north') if abs(x - CX) <= 2 else None
    # ---- interior: suelo con sendero y parterres
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1):
            v.set(x, 1, z, 'moss_block' if rnd.random() < 0.65 else 'grass_block')
        for x in range(CX - 2, CX + 3): v.set(x, 1, z, 'mossy_stone_bricks' if (z + x) % 2 else 'stone_bricks')
    for z in range(Z0 + 2, Z1 - 2, 4):                      # parterres a ambos lados
        for s in (-1, 1):
            xa = CX + s * 8
            v.fill(min(xa, xa + s * 4), 1, z, max(xa, xa + s * 4), 1, z + 2, 'dirt')
            for x in range(min(xa, xa + s * 4), max(xa, xa + s * 4) + 1):
                for zz in range(z, z + 3):
                    flor = rnd.choice(['flowering_azalea', 'azalea', 'poppy', 'dandelion', 'pink_tulip', 'red_tulip', 'cornflower', 'allium', 'blue_orchid', 'oxeye_daisy'])
                    v.set(x, 2, zz, flor)
            v.anillo(min(xa, xa + s * 4) - 1, z - 1, max(xa, xa + s * 4) + 1, z + 3, 1, 'stone_bricks')
    # ---- el arbol gigante al fondo
    tz = 33
    v.fill(CX - 1, 1, tz - 1, CX + 1, 14, tz + 1, 'oak_log[axis=y]')
    for (dx, dz) in ((-2, 0), (2, 0), (0, -2), (0, 2)):         # raices
        v.fill(CX + dx, 1, tz + dz, CX + dx, 2, tz + dz, 'oak_log[axis=y]')
    for (dx, dz) in ((-3, -1), (3, 1), (-1, 3), (1, -3)): v.set(CX + dx, 1, tz + dz, 'oak_log[axis=x]' if dx else 'oak_log[axis=z]')
    for (dx, dz, dy) in [(-9, -3, 6), (9, -2, 7), (-6, 6, 5), (7, 5, 6), (0, -8, 6)]:
        v.linea(CX, 12, tz, CX + dx, 12 + dy, tz + dz, 'oak_log[axis=y]')
    for _ in range(60):
        copa(v, CX + rnd.randint(-8, 8), rnd.randint(17, 21), tz + rnd.randint(-5, 5), rnd.randint(3, 5), rnd.randint(2, 3), rnd.randint(3, 5),
             rnd.choice(['oak_leaves', 'oak_leaves', 'azalea_leaves', 'flowering_azalea_leaves', 'cherry_leaves']), rnd)
    copa(v, CX, 18, tz, 11, 5, 8, 'oak_leaves', rnd)
    # las hojas no deben salir por el cristal
    for (x, y, z), b in list(v.g.items()):
        if 'leaves' in b and X0 < x < X1 and Z0 < z < Z1:
            dentro = (y <= YS) or (((x - CX) / (R - 1.6)) ** 2 + ((y - YS) / ((R - 1.6) * 0.95)) ** 2 < 1)
            if not dentro: del v.g[(x, y, z)]
    # cresta de cobre en la cumbrera
    ycum = int(YS + R * 0.95) + 1
    for z in range(Z0, Z1 + 1): v.set(CX, ycum, z, 'waxed_copper_block') if v.get(CX, ycum - 1, z) != 'air' else None
    # ---- estrado del lider delante del tronco
    disco_f(v, CX, 2, 27, 5, anillos([(2.5, 'stripped_oak_log[axis=y]'), (5, 'oak_planks')]))
    disco_f(v, CX, 1, 27, 6, anillos([(6, 'oak_planks')]))
    for k in range(-5, 6): v.stairs(CX + k, 2, 21, 'oak', 'north') if abs(k) <= 3 else None
    # ---- luces: faroles colgando de las costillas y flores colgantes
    for z in range(Z0 + 5, Z1, 5):
        for dx in (-9, -4, 4, 9):
            top = YS + math.sqrt(max(0, 1 - ((dx) / (R - 0.8)) ** 2)) * (R - 0.8) * 0.95
            y = int(top) - 1
            if v.get(CX + dx, y + 1, z) != 'air':
                lampara_colgante(v, CX + dx, y + 1, z, 3, 'lantern[hanging=true]')
                for k in range(1, 3): v.set(CX + dx + 1, y + 1 - k, z, 'flowering_azalea_leaves') if v.get(CX + dx + 1, y + 1 - k, z) == 'air' else None
    return v, [(CX, 3, 27, 180, 'emipokemon desafios kalos planta')]
