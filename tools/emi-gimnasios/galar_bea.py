"""Estadio de Stow-on-Side (Bea, Lucha): dojo de madera y cal con techo de tejas, torii rojo, estatuas de luchadores y un ring de piedra."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 45, 36, 48
CX = 22
X0, X1 = 9, 35
Z0, Z1 = 15, 45


def techo_pagoda(v, x0, x1, z0, z1, y0, bloque, esc, vuelo=2):
    """tejado a cuatro aguas con alero largo: capas que se estrechan de 1 en 1 (esquinas vueltas con losas)"""
    xa, xb, za, zb = x0 - vuelo, x1 + vuelo, z0 - vuelo, z1 + vuelo
    k = 0
    while xa + k < xb - k and za + k < zb - k:
        y = y0 + k
        for x in range(xa + k, xb - k + 1):
            v.stairs(x, y, za + k, esc, 'north'); v.stairs(x, y, zb - k, esc, 'south')
        for z in range(za + k, zb - k + 1):
            v.stairs(xa + k, y, z, esc, 'west'); v.stairs(xb - k, y, z, esc, 'east')
        if xa + k + 1 <= xb - k - 1 and za + k + 1 <= zb - k - 1: v.fill(xa + k + 1, y, za + k + 1, xb - k - 1, y, zb - k - 1, bloque)
        k += 1
    for (x, z) in ((xa, za), (xb, za), (xa, zb), (xb, zb)): v.set(x, y0 + 1, z, 'deepslate_tile_slab[type=bottom]')   # esquinas vueltas
    return y0 + k


def estatua_luchador(v, x, z, mirando='south'):
    """estatua de piedra de un luchador con el puño en alto (6x14)"""
    v.fill(x - 2, 1, z - 2, x + 2, 2, z + 2, 'stone_bricks')
    v.fill(x - 1, 3, z, x + 1, 5, z, 'stone'); v.fill(x - 2, 6, z, x + 2, 9, z, 'stone')           # piernas y torso
    v.fill(x - 1, 10, z, x + 1, 12, z, 'stone'); v.set(x - 1, 11, z + (1 if mirando == 'south' else -1), 'polished_andesite'); v.set(x + 1, 11, z + (1 if mirando == 'south' else -1), 'polished_andesite')
    v.fill(x + 3, 6, z, x + 3, 12, z, 'stone'); v.fill(x + 3, 13, z - 1, x + 4, 14, z, 'polished_andesite')   # brazo en alto con puño
    v.fill(x - 3, 6, z, x - 3, 8, z, 'stone'); v.fill(x - 4, 8, z, x - 3, 9, z + (1 if mirando == 'south' else -1), 'polished_andesite')
    v.set(x, 14, z, 'red_concrete')                                                                    # cinta de la cabeza


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(161)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'grass_block')
    for x in range(W):
        for z in range(L):
            if rnd.random() < 0.15: v.set(x, 0, z, 'coarse_dirt')
    for z in range(0, Z0):
        for x in range(CX - 4, CX + 5): v.set(x, 0, z, 'stone_bricks' if (x + z) % 3 else 'polished_andesite')
    # ---- torii rojo y linternas de piedra
    for dx in (-6, 6):
        v.fill(CX + dx - 1, 1, 7, CX + dx, 12, 8, 'red_concrete')
    v.fill(CX - 9, 12, 6, CX + 9, 13, 9, 'black_concrete'); v.fill(CX - 8, 14, 7, CX + 8, 14, 8, 'black_concrete')
    v.fill(CX - 7, 10, 7, CX + 7, 10, 8, 'red_concrete')
    for dx in (-10, 10): v.set(CX + dx, 14, 7, 'black_concrete'); v.set(CX + dx, 13, 7, 'black_concrete')
    for dx in (-4, 4):
        for z in (4, 11):
            v.fill(CX + dx, 1, z, CX + dx, 3, z, 'stone_brick_wall'); v.set(CX + dx, 4, z, 'lantern')
    for z in range(1, 15, 2): v.set(CX - 5, 1, z, 'stone_brick_slab[type=bottom]'); v.set(CX + 5, 1, z, 'stone_brick_slab[type=bottom]')
    estatua_luchador(v, CX - 15, 9, 'south'); estatua_luchador(v, CX + 15, 9, 'south')
    # arbolitos de bonsai y rocas
    for (x, z) in [(3, 4), (41, 4), (4, 24), (40, 24), (4, 40), (40, 40)]:
        v.fill(x, 1, z, x, 3, z, 'dark_oak_log[axis=y]'); copa(v, x, 5, z, 3, 2, 3, 'cherry_leaves', rnd)
    # ---- dojo: base de piedra, muros blancos con vigas oscuras, ventanas de papel
    v.box(X0, 1, Z0, X1, 11, Z1, 'white_terracotta', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 11, Z1 - 1)
    v.fill(X0, 1, Z0, X1, 3, Z1, 'stone_bricks'); v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 11, Z1 - 1)
    for x in range(X0, X1 + 1, 4): v.fill(x, 4, Z0, x, 11, Z0, 'dark_oak_log[axis=y]'); v.fill(x, 4, Z1, x, 11, Z1, 'dark_oak_log[axis=y]')
    for z in range(Z0, Z1 + 1, 5): v.fill(X0, 4, z, X0, 11, z, 'dark_oak_log[axis=y]'); v.fill(X1, 4, z, X1, 11, z, 'dark_oak_log[axis=y]')
    for y in (4, 8, 11): v.anillo(X0, Z0, X1, Z1, y, 'dark_oak_planks')
    for z in range(Z0 + 1, Z1):
        if (z - Z0) % 5 in (1, 2, 3):
            for y in (5, 6, 7): v.set(X0, y, z, 'white_stained_glass_pane'); v.set(X1, y, z, 'white_stained_glass_pane')
    for x in range(X0 + 1, X1):
        if (x - X0) % 4 in (1, 2, 3):
            for y in (5, 6, 7): v.set(x, y, Z1, 'white_stained_glass_pane')
    # tejados de dos plantas
    y1 = techo_pagoda(v, X0, X1, Z0, Z1, 12, 'deepslate_tiles', 'deepslate_tile', 2)
    v.box(X0 + 6, 13, Z0 + 7, X1 - 6, 20, Z1 - 7, 'white_terracotta', interior=None)
    v.aire(X0 + 7, 14, Z0 + 8, X1 - 7, 19, Z1 - 8)
    for x in range(X0 + 6, X1 - 5, 4): v.fill(x, 13, Z0 + 7, x, 20, Z0 + 7, 'dark_oak_log[axis=y]')
    y2 = techo_pagoda(v, X0 + 6, X1 - 6, Z0 + 7, Z1 - 7, 21, 'deepslate_tiles', 'deepslate_tile', 2)
    v.fill(CX, y2, (Z0 + Z1) // 2, CX, y2 + 3, (Z0 + Z1) // 2, 'gold_block')
    v.set(CX, y2 + 4, (Z0 + Z1) // 2, 'lightning_rod[facing=up,powered=false,waterlogged=false]')
    # ---- puerta de doble hoja con escalones
    hueco_puerta(v, CX, Z0, 7, 7, 'dark_oak_log[axis=y]', y0=2)
    for x in range(CX - 3, CX + 4): v.stairs(x, 1, Z0 - 1, 'stone_brick', 'north')
    # ---- interior: suelo de tatami y madera, columnas de bambu, ring de piedra central
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1): v.set(x, 1, z, 'dark_oak_planks' if (x + z) % 2 else 'spruce_planks')
    for z in range(Z0 + 2, Z1 - 1, 2):
        for x in range(X0 + 2, X1 - 1, 4):
            if (z // 2) % 2 and abs(x - CX) > 6: v.set(x, 1, z, 'green_terracotta')
    for z in range(Z0 + 4, Z1, 6):
        for x in (X0 + 3, X1 - 3): v.fill(x, 2, z, x, 10, z, 'bamboo_block'); v.set(x, 11, z, 'dark_oak_log[axis=x]')
    for z in range(Z0 + 5, Z1, 6):
        for x in (X0 + 8, X1 - 8):
            for k in range(1, 4): v.set(x, 11 - k, z, 'chain[axis=y]')
            v.set(x, 7, z, 'lantern[hanging=true]')
    cz = 30
    v.fill(CX - 7, 2, cz - 7, CX + 7, 2, cz + 7, 'smooth_stone')
    v.anillo(CX - 7, cz - 7, CX + 7, cz + 7, 2, 'red_concrete'); v.anillo(CX - 6, cz - 6, CX + 6, cz + 6, 2, 'white_concrete')
    disco_f(v, CX, 2, cz, 4, anillos([(1.2, 'red_concrete'), (3.5, 'smooth_stone'), (4, 'red_concrete')]))
    for x in range(CX - 2, CX + 3): v.stairs(x, 1, cz - 8, 'stone_brick', 'north')
    for (dx, dz) in ((-7, -7), (7, -7), (-7, 7), (7, 7)): v.fill(CX + dx, 3, cz + dz, CX + dx, 5, cz + dz, 'dark_oak_fence'); v.set(CX + dx, 6, cz + dz, 'lantern')
    # sacos de arena (makiwara) y tambores a los lados
    for (x, z) in [(X0 + 5, Z0 + 5), (X1 - 5, Z0 + 5), (X0 + 5, Z0 + 11), (X1 - 5, Z0 + 11)]:
        v.fill(x, 2, z, x, 5, z, 'stripped_oak_log[axis=y]'); v.set(x + 1, 3, z, 'hay_block[axis=y]')
    for (x, z) in [(X0 + 4, Z1 - 4), (X1 - 4, Z1 - 4)]:
        v.set(x, 2, z, 'barrel[facing=up]'); v.set(x + 1, 2, z, 'barrel[facing=up]')
    # estrado del lider al fondo con rollo y bandera
    v.fill(CX - 5, 2, Z1 - 6, CX + 5, 2, Z1 - 1, 'stripped_dark_oak_log[axis=x]'); v.fill(CX - 3, 3, Z1 - 5, CX + 3, 3, Z1 - 1, 'dark_oak_planks')
    for x in range(CX - 5, CX + 6): v.stairs(x, 2, Z1 - 7, 'dark_oak', 'north')
    for x in range(CX - 3, CX + 4): v.stairs(x, 3, Z1 - 6, 'dark_oak', 'north')
    v.fill(CX - 2, 6, Z1 - 1, CX + 2, 10, Z1 - 1, 'red_concrete'); elipse_xy(v, CX, 8, Z1 - 1, 1.4, 1.4, lambda d, a: 'white_concrete')
    return v, [(CX, 4, Z1 - 4, 180, 'emipokemon desafios galar lucha')]
