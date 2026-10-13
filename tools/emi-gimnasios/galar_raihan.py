"""Estadio de Hammerlocke (Raihan, Dragon): fortaleza de piedra con torres, cabezas de dragon en la puerta, patio-estadio y un torreon con el trono."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 49, 44, 50
CX = 24
X0, X1 = 7, 41
Z0, Z1 = 12, 46


def cabeza_dragon(v, x, y, z):
    """cabeza de dragon de piedra mirando a -z: cuello curvo, craneo con cuernos, mandibula abierta con colmillos y ojos rojos"""
    for k, (dy, dz) in enumerate([(0, 6), (1, 5), (2, 4), (3, 3), (4, 2)]):
        v.fill(x - 1, y + dy, z + dz, x + 1, y + dy + 1, z + dz + 1, 'stone_bricks')
    v.fill(x - 2, y + 4, z - 3, x + 2, y + 6, z + 1, 'polished_andesite')                      # craneo
    v.fill(x - 1, y + 4, z - 5, x + 1, y + 5, z - 3, 'polished_andesite')                      # hocico
    v.fill(x - 1, y + 2, z - 4, x + 1, y + 2, z - 1, 'stone_bricks')                           # mandibula inferior abierta
    for dx in (-1, 1): v.set(x + dx, y + 3, z - 4, 'white_concrete'); v.set(x + dx, y + 3, z - 2, 'white_concrete')   # colmillos
    for dx in (-2, 2): v.set(x + dx, y + 6, z - 2, 'red_concrete')                             # ojos
    for dx in (-2, 2):                                                                          # cuernos
        v.set(x + dx, y + 7, z, 'bone_block'); v.set(x + dx, y + 8, z + 1, 'bone_block'); v.set(x + dx * 3 // 2, y + 9, z + 2, 'bone_block')
    v.set(x, y + 6, z - 1, 'bone_block'); v.set(x, y + 7, z, 'bone_block')


def torre(v, x, z, h, rnd):
    v.box(x - 3, 1, z - 3, x + 3, h, z + 3, 'stone_bricks', interior=None)
    v.aire(x - 2, 2, z - 2, x + 2, h, z + 2)
    for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3)): v.fill(x + dx, 1, z + dz, x + dx, h, z + dz, 'chiseled_stone_bricks')
    for y in (6, 12): v.anillo(x - 3, z - 3, x + 3, z + 3, y, 'polished_andesite')
    v.anillo(x - 4, z - 4, x + 4, z + 4, h, 'stone_brick_slab[type=bottom]')
    v.fill(x - 4, h, z - 4, x + 4, h, z + 4, 'stone_bricks')
    for k in range(-4, 5):                                                                   # almenas
        if k % 2 == 0:
            for (a, b) in ((k, -4), (k, 4), (-4, k), (4, k)): v.set(x + a, h + 1, z + b, 'stone_bricks')
    for y in (8, 9, 15, 16):
        for (dx, dz) in ((0, -3), (0, 3), (-3, 0), (3, 0)): v.set(x + dx, y, z + dz, 'iron_bars')
    for dx in (0,): v.fill(x, h + 1, z, x, h + 5, z, 'oak_fence'); v.fill(x + 1, h + 4, z, x + 2, h + 5, z, 'red_wool')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(201)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['coarse_dirt', 'gravel', 'stone', 'andesite', 'coarse_dirt']))
    for z in range(0, Z0 - 3):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'stone_bricks' if (x + z) % 3 else 'cracked_stone_bricks')
    # ---- muralla: muros de piedra con paseo de ronda y almenas
    for (xa, za, xb, zb) in ((X0, Z0, X1, Z0), (X0, Z1, X1, Z1), (X0, Z0, X0, Z1), (X1, Z0, X1, Z1)):
        for y in range(1, 13):
            for x in range(xa, xb + 1):
                for z in range(za, zb + 1): v.set(x, y, z, rnd.choice(['stone_bricks'] * 4 + ['cracked_stone_bricks', 'mossy_stone_bricks']))
    v.aire(X0 + 1, 1, Z0 + 1, X1 - 1, 12, Z1 - 1)
    for y in (4, 8): v.anillo(X0, Z0, X1, Z1, y, 'polished_andesite')
    v.anillo(X0 - 1, Z0 - 1, X1 + 1, Z1 + 1, 12, 'stone_brick_slab[type=bottom]')
    for k in range(X0, X1 + 1):
        if k % 2 == 0: v.set(k, 13, Z0 - 1, 'stone_bricks'); v.set(k, 13, Z1 + 1, 'stone_bricks')
    for k in range(Z0, Z1 + 1):
        if k % 2 == 0: v.set(X0 - 1, 13, k, 'stone_bricks'); v.set(X1 + 1, 13, k, 'stone_bricks')
    for (x, z) in [(X0, Z0), (X1, Z0), (X0, Z1), (X1, Z1)]: torre(v, x, z, 22, rnd)
    # ---- puerta con rastrillo y dragones de piedra
    hueco_puerta(v, CX, Z0, 7, 8, 'chiseled_stone_bricks', y0=1, prof=1)
    for dz in range(1, 3):
        for x in range(CX - 3, CX + 4): v.aire(x, 1, Z0 + dz, x, 8, Z0 + dz)
    for x in range(CX - 3, CX + 4):
        for y in range(7, 9): v.set(x, y, Z0, 'iron_bars')
    for dx in (-8, 8):
        cabeza_dragon(v, CX + dx, 10, Z0 - 2)
    for x in (CX - 5, CX + 5): v.fill(x, 1, Z0 - 1, x, 11, Z0 - 1, 'chiseled_stone_bricks')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, Z0 - 2, 'stone_brick', 'north')
    for dx in (-3, 3):
        v.fill(CX + dx, 10, Z0 - 1, CX + dx, 18, Z0 - 1, 'red_wool'); v.set(CX + dx, 19, Z0 - 1, 'gold_block')
        v.fill(CX + dx, 1, Z0 - 3, CX + dx, 2, Z0 - 3, 'stone_brick_wall'); v.set(CX + dx, 3, Z0 - 3, 'lantern')
    # ---- patio de arena con gradas laterales
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1): v.set(x, 0, z, 'stone_bricks')
    disco_f(v, CX, 0, 30, 9, anillos([(1.5, 'red_concrete'), (4, 'polished_andesite'), (5, 'chiseled_stone_bricks'), (7, 'stone_bricks'), (8, 'red_terracotta'), (9, 'polished_andesite')]))
    for i in range(4):
        for z in range(Z0 + 4, Z1 - 4):
            xl, xr = X0 + 5 - i, X1 - 5 + i
            if i: v.fill(xl, 1, z, xl, i, z, 'stone_bricks'); v.fill(xr, 1, z, xr, i, z, 'stone_bricks')
            v.stairs(xl, 1 + i, z, 'stone_brick', 'east'); v.stairs(xr, 1 + i, z, 'stone_brick', 'west')
    for z in range(Z0 + 5, Z1 - 4, 6):
        for xx in (X0 + 1, X1 - 1):
            v.fill(xx, 6, z, xx, 11, z, 'red_wool'); v.set(xx, 12, z, 'gold_block')
    for (x, z) in [(CX - 10, Z0 + 3), (CX + 10, Z0 + 3), (CX - 10, Z1 - 3), (CX + 10, Z1 - 3)]:
        v.fill(x, 1, z, x, 3, z, 'stone_brick_wall'); v.set(x, 4, z, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
    # ---- torreon central con el trono y la cabeza de dragon
    v.box(CX - 8, 1, 38, CX + 8, 26, Z1, 'stone_bricks', interior=None)
    v.aire(CX - 7, 2, 39, CX + 7, 24, Z1 - 1)
    v.fill(CX - 7, 1, 39, CX + 7, 1, Z1 - 1, 'polished_andesite')
    for y in (8, 16): v.anillo(CX - 8, 38, CX + 8, Z1, y, 'chiseled_stone_bricks')
    cum = techo_dos_aguas(v, CX - 9, CX + 9, 37, Z1 + 1, 27, 'deepslate_tiles', 'deepslate_tile', 'z')
    hueco_puerta(v, CX, 38, 7, 9, 'chiseled_stone_bricks', y0=2, prof=1)
    for x in range(CX - 7, CX + 8):
        for y in range(4, 22): v.set(x, y, Z1, 'red_wool') if abs(x - CX) in (6, 7) or (abs(x - CX) < 3 and 8 < y < 20) else None
    elipse_xy(v, CX, 14, Z1, 4, 5, lambda d, a: 'black_concrete' if d > 0.75 else 'red_concrete')
    v.fill(CX - 1, 12, Z1, CX + 1, 16, Z1, 'gold_block')
    for k in range(3):
        v.fill(CX - 6 + k, 2 + k, 41 + k, CX + 6 - k, 2 + k, Z1 - 1, 'polished_andesite')
        for x in range(CX - 6 + k, CX + 7 - k): v.stairs(x, 2 + k, 40 + k, 'stone_brick', 'north')
    for dx in (-5, 5): v.fill(CX + dx, 6, 42, CX + dx, 9, 42, 'stone_brick_wall'); v.set(CX + dx, 10, 42, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
    return v, [(CX, 5, 44, 180, 'emipokemon desafios galar dragon')]
