"""Gran prueba de Hala (Lucha) en las Ruinas del Conflicto: ring circular de piedra para luchar, cuatro tikis, arco de ruinas y cabañas de paja."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 43, 30, 44
CX, CZ = 21, 25


def cabana(v, x0, z0, ancho, fondo, rnd):
    v.fill(x0, 1, z0, x0 + ancho, 5, z0 + fondo, 'bamboo_planks'); v.aire(x0 + 1, 2, z0 + 1, x0 + ancho - 1, 5, z0 + fondo - 1)
    for (dx, dz) in ((0, 0), (ancho, 0), (0, fondo), (ancho, fondo)): v.fill(x0 + dx, 1, z0 + dz, x0 + dx, 5, z0 + dz, 'bamboo_block')
    v.aire(x0 + ancho // 2, 2, z0, x0 + ancho // 2, 4, z0)
    techo_bambu(v, x0, x0 + ancho, z0, z0 + fondo, 6, 1)


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(381)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['grass_block', 'grass_block', 'podzol', 'coarse_dirt']))
    for z in range(0, 10):
        for x in range(CX - 2, CX + 3): v.set(x, 0, z, 'stone_bricks' if (x + z) % 3 else 'mossy_stone_bricks')
    flores_tropicales(v, 1, W - 2, 1, L - 2, rnd, 0.12)
    # ---- arco de ruinas a la entrada
    for dx in (-5, 5): v.fill(CX + dx - 1, 1, 8, CX + dx + 1, 12, 9, 'mossy_stone_bricks')
    for x in range(CX - 6, CX + 7):
        if abs(x - CX) > 3 or rnd.random() < 0.8: v.fill(x, 12, 8, x, 13, 9, 'mossy_stone_bricks' if x % 3 else 'cracked_stone_bricks')
    for _ in range(14): v.set(CX + rnd.randint(-5, 5), 13, rnd.choice([8, 9]), 'air')
    v.set(CX - 6, 14, 8, 'chiseled_stone_bricks'); v.set(CX + 6, 14, 8, 'chiseled_stone_bricks')
    # ---- plataforma circular elevada con anillos de paja y escalinata
    disco_f(v, CX, 1, CZ, 12, anillos([(12, 'stone_bricks')]))
    disco_f(v, CX, 2, CZ, 11, anillos([(11, 'mossy_stone_bricks')]))
    disco_f(v, CX, 3, CZ, 10, anillos([(1.5, 'red_concrete'), (7.5, 'sandstone'), (8.5, 'red_concrete'), (10, 'smooth_sandstone')]))
    for k, z in enumerate((CZ - 14, CZ - 13, CZ - 12)):
        for x in range(CX - 3, CX + 4):
            v.fill(x, 0, z, x, k, z, 'stone_bricks'); v.stairs(x, 1 + k, z, 'stone_brick', 'north')
    for z in range(10, CZ - 14):
        for x in range(CX - 2, CX + 3): v.set(x, 0, z, 'stone_bricks' if (x + z) % 3 else 'mossy_stone_bricks')
    # ---- cuatro tikis en los puntos cardinales
    for (dx, dz) in ((0, -15), (15, 0), (-15, 0), (0, 15)):
        x, z = CX + dx, CZ + dz
        if 0 < x < W - 1 and 3 < z < L - 3: tiki(v, x, z, 11, 'mossy_stone_bricks')
    for k in range(12):
        a = k / 12 * 2 * math.pi + 0.13
        x, z = round(CX + math.cos(a) * 12.5), round(CZ + math.sin(a) * 12.5)
        if abs(x - CX) <= 3 and z < CZ - 10: continue
        antorcha_tiki(v, x, z, 3, 1)
    # ---- cabañas de paja a los lados y plataforma del lider
    cabana(v, 3, 12, 8, 7, rnd); cabana(v, 32, 12, 8, 7, rnd)
    cabana(v, 3, 30, 8, 7, rnd); cabana(v, 32, 30, 8, 7, rnd)
    disco_f(v, CX, 4, CZ + 4, 3, anillos([(3, 'polished_andesite')]))
    for x in range(CX - 3, CX + 4): v.stairs(x, 4, CZ + 1, 'stone_brick', 'north') if abs(x - CX) <= 1 else None
    for (x, z) in [(6, 4), (36, 5), (5, 40), (38, 40), (21, 41), (12, 40), (30, 39)]: palmera(v, x, z, rnd.randint(5, 8), rnd)
    return v, [(CX, 5, CZ + 4, 180, 'emipokemon desafios alola kahuna_hala')]
