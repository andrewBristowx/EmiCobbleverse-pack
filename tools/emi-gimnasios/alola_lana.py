"""Prueba de Lana (Agua) en la Colina del Arroyuelo: laguna con nenufares, piedras para cruzar, cascada de colores y una isleta con bambu."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 41, 28, 44
CX, CZ = 20, 26
R = 13


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(321)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['grass_block', 'grass_block', 'grass_block', 'podzol']))
    for z in range(0, CZ - R - 1):
        for x in range(CX - 2, CX + 3): v.set(x, 0, z, 'dirt_path')
    # ---- laguna encerrada: fondo de arcilla, borde de piedra y agua
    disco_f(v, CX, 0, CZ, R, lambda d, a: 'clay')
    disco_f(v, CX, 1, CZ, R, lambda d, a: 'mossy_stone_bricks' if d > R - 1.2 else 'water')
    disco_f(v, CX, 2, CZ, R, lambda d, a: 'mossy_cobblestone' if d > R - 1.0 and rnd.random() < 0.5 else None)
    # isleta central y piedras para cruzar desde la entrada
    disco_f(v, CX, 1, CZ + 3, 4, anillos([(3, 'grass_block'), (4, 'mossy_cobblestone')]))
    for k in range(7):
        z = CZ - R + 2 + k * 2
        x = CX + (1 if k % 2 else -1)
        v.set(x, 1, z, 'mossy_stone_brick_slab[type=bottom]')
        v.set(x, 2, z, 'air')
    for z in range(CZ - R - 1, CZ - R + 3):
        for x in range(CX - 1, CX + 2): v.set(x, 1, z, 'mossy_stone_bricks')
    for z in range(CZ - 6, CZ):
        v.set(CX + (z % 2) * 1, 1, z, 'mossy_stone_brick_slab[type=bottom]')
    for (dx, dz) in ((0, -2), (-1, -2), (1, -2)): v.set(CX + dx, 1, CZ + 3 + dz, 'mossy_cobblestone')
    for k in range(7):                                                                    # bambu en la isleta
        a = k / 7 * 2 * math.pi
        x, z = round(CX + math.cos(a) * 3), round(CZ + 3 + math.sin(a) * 3)
        v.set(x, 1, z, 'grass_block')
        for y in range(2, 5 + k % 3): v.set(x, y, z, 'bamboo[age=1,leaves=none,stage=0]')
        # nenufares
    for _ in range(40):
        a = rnd.random() * 2 * math.pi; d = rnd.random() * (R - 3)
        x, z = round(CX + math.cos(a) * d), round(CZ + math.sin(a) * d)
        if v.get(x, 1, z) == 'water' and v.get(x, 2, z) == 'air': v.set(x, 2, z, 'lily_pad')
    for x in range(W):
        for z in range(L):
            if v.get(x, 2, z) == 'lily_pad' and v.get(x, 1, z) != 'water': v.set(x, 2, z, 'air')
    # ---- cascada de colores sobre un acantilado al fondo
    for x in range(CX - 10, CX + 11):
        h = 14 - abs(x - CX) // 3
        for z in range(37, 43):
            for y in range(1, h): v.set(x, y, z, rnd.choice(['stone', 'mossy_cobblestone', 'stone', 'tuff']))
    for x in range(CX - 3, CX + 4):
        for y in range(3, 14): v.set(x, y, 36, 'light_blue_concrete' if (y + x) % 3 else 'white_concrete')
    for x in range(CX - 3, CX + 4): v.set(x, 1, 36, 'light_blue_concrete'); v.set(x, 2, 36, 'white_concrete')
    # ---- cabaña de bambu sobre pilotes a la izquierda y el embarcadero
    for (x, z) in [(3, 12), (9, 12), (3, 19), (9, 19)]: v.fill(x, 1, z, x, 3, z, 'bamboo_block')
    v.fill(2, 4, 11, 10, 4, 20, 'bamboo_planks')
    for (x, z) in [(2, 11), (10, 11), (2, 20), (10, 20)]: v.fill(x, 5, z, x, 8, z, 'bamboo_block')
    for x in range(3, 10):
        for y in (5, 6, 7): v.set(x, y, 20, 'bamboo_planks'); v.set(x, y, 11, 'bamboo_planks') if abs(x - 6) > 1 else None
    for z in range(12, 20):
        for y in (5, 6, 7): v.set(2, y, z, 'bamboo_planks'); v.set(10, y, z, 'bamboo_planks') if z != 15 else None
    v.fill(3, 5, 12, 9, 7, 19, 'air')
    techo_bambu(v, 2, 10, 11, 20, 9, 1)
    for z in range(8, 12): v.set(6, 4, z, 'bamboo_planks')
    for z in range(5, 12, 2): v.set(5, 5, z, 'bamboo_fence')
    # palmeras y flores
    for (x, z) in [(3, 28), (4, 36), (37, 12), (37, 24), (36, 34), (30, 8), (8, 6)]: palmera(v, x, z, rnd.randint(5, 7), rnd)
    flores_tropicales(v, 1, W - 2, 1, L - 2, rnd, 0.12)
    for (x, z) in [(CX - 4, 4), (CX + 4, 4)]: antorcha_tiki(v, x, z, 4)
    return v, [(CX, 3, CZ + 3, 180, 'emipokemon desafios alola captain_lana')]
