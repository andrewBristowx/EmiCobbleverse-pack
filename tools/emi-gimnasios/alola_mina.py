"""Prueba de Mina (Hada) en la Aldea Marinera: barco-restaurante varado con velas rosas, embarcaderos de madera, farolillos y flores de hada."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 41, 34, 46
CX = 20
Z0, Z1 = 8, 40      # casco del barco: de la proa (-z) a la popa (+z)


def ancho_casco(z):
    t = (z - Z0) / (Z1 - Z0)
    if t < 0.28: return 3 + int(t / 0.28 * 7)
    if t > 0.85: return 10 - int((t - 0.85) / 0.15 * 3)
    return 10


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(371)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['sand', 'sand', 'smooth_sandstone', 'sandstone']))
    for z in range(Z0 + 6, Z1 - 1):
        for x in range(CX - 12, CX + 13):
            if abs(x - CX) > ancho_casco(z) and rnd.random() < 0.3: v.set(x, 0, z, rnd.choice(['pink_terracotta', 'sandstone']))
    # ---- casco: costados de roble oscuro, cubierta y borda
    for z in range(Z0, Z1 + 1):
        a = ancho_casco(z)
        for x in range(CX - a, CX + a + 1):
            if abs(x - CX) in (a, a - 1) or z in (Z0, Z0 + 1, Z1):
                for y in range(1, 7): v.set(x, y, z, 'dark_oak_planks' if y % 3 else 'stripped_dark_oak_log[axis=y]')
            if abs(x - CX) < a - 1: v.set(x, 1, z, 'dark_oak_planks')
        for x in range(CX - a + 2, CX + a - 1): v.set(x, 4, z, 'spruce_planks')
        v.set(CX - a, 7, z, 'dark_oak_fence'); v.set(CX + a, 7, z, 'dark_oak_fence')
    for z in range(Z0, Z1 + 1):
        a = ancho_casco(z)
        for x in range(CX - a + 1, CX + a): v.aire(x, 5, z, x, 6, z) if abs(x - CX) < a - 1 and z > Z0 + 1 else None
    # proa elevada y mascaron de proa en forma de estrella marina
    for z in range(Z0, Z0 + 6): v.set(CX, 7 + (5 - (z - Z0)) // 2, z, 'dark_oak_planks')
    elipse_xy(v, CX, 11, Z0 - 1, 2.5, 2.5, lambda d, a: 'pink_concrete' if d > 0.5 else 'white_concrete')
    # ---- mastiles y velas rosas
    for (mz, alto) in [(Z0 + 10, 24), (Z0 + 20, 28), (Z0 + 28, 22)]:
        v.fill(CX, 5, mz, CX, alto, mz, 'stripped_spruce_log[axis=y]')
        for y in range(8, alto - 2):
            for dx in range(-5, 6):
                if abs(dx) + (alto - y) // 2 < 7 and dx != 0: v.set(CX + dx, y, mz + 1, 'pink_wool' if (y + dx) % 5 else 'white_wool')
        v.fill(CX - 6, alto - 3, mz + 1, CX + 6, alto - 3, mz + 1, 'stripped_spruce_log[axis=x]'); v.set(CX, alto + 1, mz, 'lantern')
    # ---- castillo de popa con el estrado del lider y la rueda del timon
    zp = Z1 - 8
    v.fill(CX - 8, 5, zp, CX + 8, 5, Z1 - 1, 'spruce_planks'); v.fill(CX - 8, 6, zp, CX + 8, 6, zp, 'dark_oak_planks')
    for x in range(CX - 8, CX + 9): v.stairs(x, 5, zp - 1, 'spruce', 'north') if abs(x - CX) <= 3 else None
    for dx in (-8, 8): v.fill(CX + dx, 6, zp, CX + dx, 10, zp, 'dark_oak_fence'); v.set(CX + dx, 11, zp, 'lantern')
    elipse_xy(v, CX, 9, Z1 - 1, 2.5, 2.5, lambda d, a: 'oak_planks' if 0.6 < d else None)
    # ---- mesas del restaurante en cubierta con velas
    for (x, z) in [(CX - 5, Z0 + 12), (CX + 5, Z0 + 12), (CX - 5, Z0 + 18), (CX + 5, Z0 + 18), (CX - 5, Z0 + 24), (CX + 5, Z0 + 24)]:
        v.set(x, 5, z, 'oak_fence'); v.set(x, 6, z, 'pink_concrete'); v.set(x, 7, z, 'candle[candles=2,lit=true]')
        for (dx, dz) in ((1, 0), (-1, 0)): v.stairs(x + dx, 5, z + dz, 'dark_oak', 'north')
    # ---- rampa de entrada por la proa (se abre un hueco en la borda) y embarcaderos con farolillos
    for x in range(CX - 1, CX + 2):
        for z in (Z0, Z0 + 1): v.aire(x, 5, z, x, 6, z)
        for k in range(4):
            v.fill(x, 0, 3 + k, x, k, 3 + k, 'dark_oak_planks'); v.stairs(x, 1 + k, 3 + k, 'dark_oak', 'north')
        v.fill(x, 0, 7, x, 4, 7, 'dark_oak_planks')
    for z in range(1, 8, 2):
        for dx in (-2, 2): v.fill(CX + dx, 1, z, CX + dx, 3, z, 'bamboo_block'); v.set(CX + dx, 4, z, 'lantern') if z % 4 == 1 else None
    for (x, z) in [(CX - 17, 20), (CX + 17, 24)]:                                           # pequeños muelles y balsas con farolillos
        v.fill(x - 2, 1, z - 3, x + 2, 1, z + 3, 'dark_oak_planks')
        for (dx, dz) in ((-2, -3), (2, 3)): v.fill(x + dx, 2, z + dz, x + dx, 4, z + dz, 'bamboo_block'); v.set(x + dx, 5, z + dz, 'lantern')
    for _ in range(14):
        x, z = rnd.randint(1, W - 2), rnd.randint(1, L - 2)
        if v.get(x, 1, z) == 'air' and v.get(x, 0, z) in ('sand', 'sandstone', 'smooth_sandstone'): v.set(x, 0, z, 'grass_block'); v.set(x, 1, z, rnd.choice(['pink_tulip', 'allium', 'azure_bluet', 'cornflower']))
    for (x, z) in [(3, 14), (37, 12), (4, 36), (36, 38)]: palmera(v, x, z, rnd.randint(5, 7), rnd)
    return v, [(CX, 6, Z1 - 4, 180, 'emipokemon desafios alola captain_mina')]
