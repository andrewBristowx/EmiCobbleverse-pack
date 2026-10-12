"""Prueba de Ilima (Normal) en la Caverna Verdeante: colina de selva con una cueva de entrada con tikis, raices colgantes y un anfiteatro de piedra."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 39, 30, 42
CX, CZ = 19, 26
ZF = 9


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(311)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['grass_block', 'grass_block', 'podzol', 'coarse_dirt']))
    for z in range(0, ZF):
        for x in range(CX - 2, CX + 3): v.set(x, 0, z, 'sandstone' if (x + z) % 2 else 'smooth_sandstone')
    # ---- colina de selva con la cara frontal rocosa
    alt = {}
    for x in range(2, 37):
        for z in range(ZF, 40):
            d = (abs(x - CX) / 17.5) ** 4 + (abs(z - 25) / 16) ** 4
            if d > 1.0 + rnd.random() * 0.1: continue
            alt[(x, z)] = int(max(5, 22 - 1.0 * max(0, abs(x - CX) - 6) - 0.7 * max(0, z - 28) + rnd.random() * 2 - 4 * d))
    for (x, z), h in alt.items():
        for y in range(1, h + 1): v.set(x, y, z, rnd.choice(['stone', 'mossy_cobblestone', 'mossy_stone_bricks', 'dirt', 'stone']) if y < h - 1 else 'dirt')
        v.set(x, h, z, 'moss_block' if rnd.random() < 0.35 else 'grass_block')
        if rnd.random() < 0.12 and v.get(x, h + 1, z) == 'air': v.set(x, h + 1, z, rnd.choice(['fern', 'short_grass', 'poppy']))
    for _ in range(14):
        x, z = rnd.randint(4, 34), rnd.randint(ZF + 2, 36)
        if (x, z) in alt: palmera(v, x, z, rnd.randint(4, 6), rnd, alt[(x, z)] + 1)
    # ---- cueva: elipsoide y tunel de entrada
    for x in range(3, 36):
        for z in range(ZF + 1, 40):
            for y in range(1, 24):
                d = math.sqrt(((x - CX) / 12.5) ** 2 + ((y - 1) / 12) ** 2 + ((z - CZ) / 13) ** 2)
                if d < 1.0: v.set(x, y, z, 'air')
    for z in range(ZF, CZ - 8):
        for x in range(CX - 3, CX + 4):
            for y in range(1, 8 - abs(x - CX) // 2): v.set(x, y, z, 'air')
    for x in range(3, 36):
        for z in range(ZF, 40):
            if v.get(x, 1, z) == 'air': v.set(x, 0, z, rnd.choice(['sandstone', 'mossy_cobblestone', 'sandstone', 'gravel']))
    # entrada: arco de piedra con dos tikis
    for dx in (-5, 5): tiki(v, CX + dx, ZF - 3, 8)
    v.fill(CX - 4, 8, ZF - 1, CX + 4, 9, ZF - 1, 'mossy_stone_bricks'); v.fill(CX - 3, 10, ZF - 1, CX + 3, 10, ZF - 1, 'chiseled_stone_bricks')
    for dx in (-4, 4): v.fill(CX + dx, 1, ZF - 1, CX + dx, 7, ZF - 1, 'mossy_stone_bricks')
    for (x, z) in [(CX - 7, 4), (CX + 7, 4)]: antorcha_tiki(v, x, z, 5)
    # ---- interior: anfiteatro con suelo de piedra, estalactitas con raices y lamparas
    disco_f(v, CX, 0, CZ + 2, 9, anillos([(1.5, 'chiseled_stone_bricks'), (4, 'smooth_sandstone'), (5, 'mossy_stone_bricks'), (8, 'sandstone'), (9, 'mossy_stone_bricks')]))
    for _ in range(80):
        x, z = rnd.randint(5, 33), rnd.randint(ZF + 3, 38)
        for y in range(22, 4, -1):
            if v.get(x, y, z) != 'air' and v.get(x, y - 1, z) == 'air' and not v.get(x, y, z).startswith(('hanging', 'lantern', 'chain', 'pointed')):
                if rnd.random() < 0.5:
                    for k in range(1, rnd.randint(2, 5)):
                        if v.get(x, y - k, z) == 'air': v.set(x, y - k, z, 'hanging_roots')
                else:
                    v.set(x, y - 1, z, 'pointed_dripstone[thickness=tip,vertical_direction=down,waterlogged=false]')
                break
    for (x, z) in [(CX - 7, CZ - 4), (CX + 7, CZ - 4), (CX - 9, CZ + 5), (CX + 9, CZ + 5), (CX, CZ - 8)]:
        for y in range(22, 5, -1):
            if v.get(x, y, z) != 'air' and v.get(x, y - 1, z) == 'air' and not v.get(x, y, z).startswith(('hanging', 'lantern', 'chain', 'pointed')):
                n = max(2, min(8, y - 8))
                for k in range(1, n + 1): v.set(x, y - k, z, 'chain[axis=y]')
                v.set(x, y - n - 1, z, 'lantern[hanging=true]')
                break
    for k in range(6):
        a = k / 6 * 2 * math.pi
        x, z = round(CX + math.cos(a) * 7), round(CZ + 2 + math.sin(a) * 7)
        if abs(x - CX) <= 2 and z < CZ - 3: continue
        v.fill(x, 1, z, x, 3, z, 'mossy_stone_bricks'); v.set(x, 4, z, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
    # ---- altar del lider con el totem de Gumshoos detras
    for k in range(3):
        v.fill(CX - 5 + k, 1 + k, CZ + 8 + k, CX + 5 - k, 1 + k, CZ + 12, 'stone_bricks' if k % 2 == 0 else 'mossy_stone_bricks')
        for x in range(CX - 5 + k, CX + 6 - k): v.stairs(x, 1 + k, CZ + 7 + k, 'stone_brick', 'north')
    tiki(v, CX, CZ + 13, 9, 'stone_bricks', 1)
    for dx in (-4, 4): tiki(v, CX + dx, CZ + 11, 5, 'mossy_stone_bricks', 4)
    return v, [(CX, 4, CZ + 11, 180, 'emipokemon desafios alola captain_ilima')]
