"""Gimnasio de Montenevera (Ryme, Fantasma): mansion gotica embrujada con cementerio, arboles muertos y un escenario de rap con luces moradas."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 45, 44, 50
CX = 22
X0, X1 = 9, 35
Z0, Z1 = 16, 47


def arbol_muerto(v, x, z, alto, rnd):
    v.fill(x, 1, z, x, alto, z, 'dark_oak_log[axis=y]')
    for k in range(4):
        dx, dz = rnd.choice([(-1, 0), (1, 0), (0, 1), (0, -1)]); y = alto - 1 - k
        for i in range(1, 3 + k % 2): v.set(x + dx * i, y + i // 2, z + dz * i, 'dark_oak_log[axis=%s]' % ('x' if dx else 'z'))


def lapida(v, x, z, rnd):
    t = rnd.randint(0, 2)
    if t == 0: v.fill(x, 1, z, x, 3, z, 'stone_bricks'); v.set(x, 4, z, 'stone_brick_slab[type=bottom]')
    elif t == 1: v.fill(x, 1, z, x, 4, z, 'cobblestone_wall'); v.fill(x - 1, 3, z, x + 1, 3, z, 'cobblestone_wall')
    else: v.fill(x, 1, z, x + 1, 2, z, 'mossy_cobblestone'); v.set(x, 3, z, 'mossy_stone_brick_slab[type=bottom]')
    v.set(x, 0, z + 1, 'coarse_dirt')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(271)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['coarse_dirt', 'podzol', 'gravel', 'dirt']))
    for z in range(0, Z0):
        for x in range(CX - 2, CX + 3): v.set(x, 0, z, 'cracked_stone_bricks' if (x + z) % 2 else 'stone_bricks')
    # ---- cementerio con valla de hierro, lapidas y arboles muertos
    for x in range(1, W - 1, 2):
        if abs(x - CX) > 3: v.fill(x, 1, 1, x, 3, 1, 'iron_bars'); v.set(x, 4, 1, 'stone_brick_wall')
    for z in range(1, Z0): v.fill(1, 1, z, 1, 3, z, 'iron_bars'); v.fill(W - 2, 1, z, W - 2, 3, z, 'iron_bars')
    for dx in (-3, 3): v.fill(CX + dx, 1, 1, CX + dx, 5, 1, 'stone_bricks'); v.set(CX + dx, 6, 1, 'soul_lantern')
    for _ in range(26):
        x, z = rnd.randint(3, W - 4), rnd.randint(3, Z0 - 3)
        if abs(x - CX) > 4 and v.get(x, 1, z) == 'air': lapida(v, x, z, rnd)
    for (x, z, a) in [(5, 6, 9), (39, 8, 10), (6, 24, 8), (38, 26, 9), (5, 40, 10), (39, 42, 8)]: arbol_muerto(v, x, z, a, rnd)
    # ---- mansion: muros oscuros, ventanas ojivales, tejado de pizarra empinado y torres
    v.box(X0, 1, Z0, X1, 14, Z1, 'deepslate_bricks', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 14, Z1 - 1)
    for y in (1, 2): v.anillo(X0, Z0, X1, Z1, y, 'polished_blackstone_bricks')
    for x in range(X0, X1 + 1, 4): v.fill(x, 3, Z0, x, 14, Z0, 'polished_deepslate'); v.fill(x, 3, Z1, x, 14, Z1, 'polished_deepslate')
    for z in range(Z0, Z1 + 1, 4): v.fill(X0, 3, z, X0, 14, z, 'polished_deepslate'); v.fill(X1, 3, z, X1, 14, z, 'polished_deepslate')
    for y in (7, 14): v.anillo(X0, Z0, X1, Z1, y, 'blackstone')
    for z in range(Z0 + 2, Z1 - 1, 4):
        for y in (4, 5, 6, 9, 10, 11): v.set(X0, y, z, 'purple_stained_glass'); v.set(X0, y, z + 1, 'purple_stained_glass'); v.set(X1, y, z, 'purple_stained_glass'); v.set(X1, y, z + 1, 'purple_stained_glass')
    cum = techo_dos_aguas(v, X0 - 1, X1 + 1, Z0 - 1, Z1 + 1, 15, 'deepslate_tiles', 'deepslate_tile', 'z')
    v.fill(X0 + 1, 14, Z0 + 1, X1 - 1, 14, Z1 - 1, 'blackstone')
    for k in range(0, (X1 - X0 + 3) // 2 + 1):
        for zz in (Z0, Z1): v.fill(CX - (X1 - X0) // 2 - 1 + k, 15 + k, zz, CX + (X1 - X0) // 2 + 1 - k, 15 + k, zz, 'deepslate_bricks')
    for (x, z) in [(X0 - 1, Z0 - 1), (X1 + 1, Z0 - 1), (X1 + 1, Z1 + 1)]:
        v.cilindro(x, 1, 26, z, 3, 'deepslate_bricks'); v.disco(x, 0, z, 3, 'polished_blackstone_bricks')
        for y in (9, 15, 21): v.set(x, y, z - 3, 'purple_stained_glass'); v.set(x, y + 1, z - 3, 'purple_stained_glass')
        cono(v, x, 27, z, 4, 11, 'deepslate_tiles'); v.fill(x, 38, z, x, 41, z, 'iron_bars')
    for dx in (-2, 2): v.fill(CX + dx, 1, Z0 - 3, CX + dx, 6, Z0 - 3, 'polished_blackstone_bricks'); v.set(CX + dx, 7, Z0 - 3, 'soul_lantern')
    hueco_puerta(v, CX, Z0, 5, 7, 'polished_blackstone_bricks', y0=2)
    v.fill(CX - 3, 9, Z0, CX + 3, 9, Z0, 'polished_blackstone_bricks')
    elipse_xy(v, CX, 13, Z0, 2.5, 2.5, lambda d, a: 'purple_stained_glass' if d < 0.7 else 'polished_blackstone_bricks')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, Z0 - 1, 'stone_brick', 'north') if abs(x - CX) <= 2 else None
    for _ in range(40):                                                                   # telaranas en las esquinas
        v.set(rnd.choice([X0 + 1, X1 - 1]), rnd.randint(11, 13), rnd.randint(Z0 + 1, Z1 - 1), 'cobweb')
    # ---- interior: salon con suelo de ajedrez oscuro, candelabros y velas moradas
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1): v.set(x, 1, z, 'polished_blackstone' if (x + z) % 2 else 'blackstone')
    for z in range(Z0 + 1, Z1):
        for x in range(CX - 1, CX + 2): v.set(x, 1, z, 'purple_concrete')
    for z in range(Z0 + 4, Z1 - 8, 5):
        for x in (X0 + 3, X1 - 3):
            v.fill(x, 2, z, x, 13, z, 'polished_deepslate'); v.set(x, 2, z, 'chiseled_deepslate'); v.set(x, 13, z, 'chiseled_deepslate')
            v.set(x + (1 if x < CX else -1), 2, z, 'candle[candles=3,lit=true]')
    for z in range(Z0 + 6, Z1 - 9, 6):
        for dx in (-6, 0, 6):
            for k in range(1, 5): v.set(CX + dx, 14 - k, z, 'chain[axis=y]')
            v.set(CX + dx, 9, z, 'soul_lantern[hanging=true]')
            for (ax, az) in ((1, 0), (-1, 0), (0, 1), (0, -1)): v.set(CX + dx + ax, 10, z + az, 'cobweb') if rnd.random() < 0.5 else None
    for (x, z) in [(X0 + 2, Z0 + 3), (X1 - 2, Z0 + 3), (X0 + 2, Z1 - 3), (X1 - 2, Z1 - 3)]:      # ataudes de decoracion
        v.fill(x - 1, 2, z - 1, x + 1, 2, z + 1, 'dark_oak_planks'); v.fill(x, 3, z - 1, x, 3, z + 1, 'dark_oak_slab[type=bottom]')
    # ---- escenario de rap: tarima, altavoces, DJ, micro y luces
    v.fill(CX - 9, 2, Z1 - 11, CX + 9, 2, Z1 - 1, 'polished_blackstone_bricks'); v.fill(CX - 7, 3, Z1 - 10, CX + 7, 3, Z1 - 1, 'black_concrete')
    for x in range(CX - 9, CX + 10): v.stairs(x, 2, Z1 - 12, 'polished_blackstone_brick', 'north') if abs(x - CX) <= 2 else v.set(x, 2, Z1 - 12, 'purple_concrete')
    for dx in (-7, 7):
        v.fill(CX + dx - 1, 4, Z1 - 2, CX + dx + 1, 8, Z1 - 2, 'black_concrete'); v.set(CX + dx, 5, Z1 - 3, 'light_gray_concrete'); v.set(CX + dx, 7, Z1 - 3, 'light_gray_concrete')
    v.fill(CX - 3, 4, Z1 - 3, CX + 3, 4, Z1 - 3, 'quartz_slab[type=bottom]'); v.fill(CX - 3, 4, Z1 - 2, CX + 3, 4, Z1 - 2, 'black_concrete')
    for dx in (-2, 2): v.set(CX + dx, 5, Z1 - 3, 'iron_bars'); v.set(CX + dx, 6, Z1 - 3, 'light_gray_concrete')
    v.fill(CX, 4, Z1 - 7, CX, 6, Z1 - 7, 'iron_bars'); v.set(CX, 7, Z1 - 7, 'black_concrete')
    elipse_xy(v, CX, 13, Z1, 4, 5, lambda d, a: 'purple_concrete' if d > 0.7 else 'black_concrete')
    for (a, b) in ((-2, 12), (2, 12)): v.fill(CX + a, b, Z1, CX + a, b + 1, Z1, 'white_concrete')
    v.fill(CX - 2, 9, Z1, CX + 2, 9, Z1, 'white_concrete'); v.set(CX, 10, Z1, 'white_concrete')
    for x in range(CX - 9, CX + 10, 3): v.set(x, 13, Z1 - 3, 'purple_stained_glass'); v.set(x, 12, Z1 - 3, 'sea_lantern')
    return v, [(CX, 4, Z1 - 6, 180, 'emipokemon desafios paldea fantasma')]
