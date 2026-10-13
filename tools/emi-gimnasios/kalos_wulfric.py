"""Gimnasio de Snowbelle (Wulfric, Hielo): catedral gotica de nieve y hielo con torres, rosetón y un Avalugg de hielo."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 37, 40, 46
CX = 18
X0, X1 = 6, 30
Z0, Z1 = 9, 43


def rosetón(v, cx, cy, z, r):
    def f(d, a):
        if d > 0.88: return 'quartz_block'
        if d < 0.2: return 'light_blue_stained_glass'
        if abs(math.sin(a * 3)) < 0.22 or d < 0.28 or (0.55 < d < 0.62): return 'quartz_block'
        return 'blue_stained_glass' if int(d * 6) % 2 else 'light_blue_stained_glass'
    elipse_xy(v, cx, cy, z, r, r, f)


def avalugg(v, x, z, rnd):
    """Avalugg: cubo de hielo con la cara, patas, y puas de hielo en el lomo (mira al -z)"""
    v.fill(x - 3, 1, z, x + 3, 5, z + 4, 'packed_ice')
    v.fill(x - 3, 6, z + 1, x + 3, 6, z + 3, 'blue_ice')
    v.fill(x - 2, 2, z - 1, x + 2, 4, z - 1, 'packed_ice')                   # hocico
    v.set(x - 2, 4, z - 2, 'sea_lantern'); v.set(x + 2, 4, z - 2, 'sea_lantern')
    v.fill(x - 1, 2, z - 2, x + 1, 2, z - 2, 'white_concrete')               # dientes
    for k in range(10):
        a, b = rnd.randint(-3, 3), rnd.randint(1, 3)
        v.fill(x + a, 7, z + b, x + a, 7 + rnd.randint(1, 3), z + b, rnd.choice(['ice', 'packed_ice', 'blue_ice']))
    for (dx, dz) in ((-3, 0), (3, 0), (-3, 4), (3, 4)): v.fill(x + dx, 1, z + dz, x + dx, 1, z + dz, 'blue_ice')


def abeto(v, x, z, alto):
    for k in range(alto):
        r = max(0, (alto - k) // 3 + (1 if k < alto - 2 else 0))
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) + abs(dz) <= r + 1 and v.get(x + dx, 1 + k + 2, z + dz) == 'air': v.set(x + dx, 1 + k + 2, z + dz, 'spruce_leaves')
    v.fill(x, 1, z, x, 3, z, 'spruce_log[axis=y]')
    v.set(x, 1 + alto + 2, z, 'snow_block')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(111)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'snow_block')
    for x in range(W):
        for z in range(L):
            if rnd.random() < 0.1: v.set(x, 0, z, 'packed_ice')
            elif rnd.random() < 0.18 and not (X0 - 2 <= x <= X1 + 2 and Z0 - 2 <= z <= Z1 + 2): v.set(x, 1, z, 'snow[layers=%d]' % rnd.randint(1, 3))
    for z in range(0, Z0):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'smooth_stone' if (x + z) % 2 else 'stone_bricks'); v.set(x, 1, z, 'air')
    avalugg(v, 32, 3, rnd)
    abeto(v, 3, 3, 9); abeto(v, 14, 4, 6); abeto(v, 3, 20, 8); abeto(v, 33, 22, 10); abeto(v, 3, 36, 12); abeto(v, 33, 38, 9); abeto(v, 12, 2, 7)
    # ---- nave
    v.box(X0, 1, Z0, X1, 14, Z1, 'quartz_bricks', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 20, Z1 - 1)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'packed_ice')
    for y in (1, 2): v.anillo(X0, Z0, X1, Z1, y, 'stone_bricks')
    v.anillo(X0, Z0, X1, Z1, 14, 'snow_block')
    # contrafuertes y ventanales alargados
    for z in range(Z0 + 3, Z1 - 1, 5):
        for x in (X0, X1):
            s = -1 if x == X0 else 1
            v.fill(x + s, 1, z, x + s, 9, z, 'quartz_bricks'); v.fill(x + 2 * s, 1, z, x + 2 * s, 5, z, 'quartz_bricks'); v.set(x + s, 10, z, 'snow_block')
            for y in range(4, 12): v.set(x, y, z + 2, 'light_blue_stained_glass') if z + 2 < Z1 - 1 else None
    # tejado a dos aguas
    cum = techo_dos_aguas(v, X0 - 1, X1 + 1, Z0 - 1, Z1 + 1, 15, 'light_blue_concrete', 'quartz', 'z')
    v.fill(X0 + 1, 14, Z0 + 1, X1 - 1, 14, Z1 - 1, 'quartz_bricks')
    v.fill(CX, cum + 1, Z0 - 1, CX, cum + 1, Z1 + 1, 'snow_block')
    # hastiales con rosetones
    frontón(v, CX, Z0, 15, X1 - X0 + 2, 'quartz_bricks', 'quartz')
    frontón(v, CX, Z1, 15, X1 - X0 + 2, 'quartz_bricks', 'quartz')
    rosetón(v, CX, 22, Z0, 5)
    rosetón(v, CX, 22, Z1, 5)
    # torres de fachada con agujas
    for x in (X0 + 1, X1 - 1):
        v.box(x - 2, 1, Z0 - 2, x + 2, 24, Z0 + 2, 'quartz_bricks', interior=None)
        for y in (6, 12, 18): v.anillo(x - 2, Z0 - 2, x + 2, Z0 + 2, y, 'snow_block')
        for y in (8, 9, 15, 16, 20, 21):
            for (dx, dz) in ((0, -2), (-2, 0), (2, 0)): v.set(x + dx, y, Z0 + dz, 'blue_stained_glass')
        cono(v, x, 25, Z0, 4, 14, 'light_blue_concrete')
        v.set(x, 38, Z0, 'quartz_block')
    # portal ojival
    zf = Z0
    hueco_puerta(v, CX, zf, 5, 8, 'blue_ice', y0=1, prof=1)
    v.fill(CX - 1, 9, zf, CX + 1, 9, zf, 'air'); v.fill(CX - 3, 9, zf, CX - 3, 9, zf, 'blue_ice'); v.fill(CX + 3, 9, zf, CX + 3, 9, zf, 'blue_ice')
    v.fill(CX - 2, 10, zf, CX + 2, 10, zf, 'blue_ice'); v.fill(CX - 1, 11, zf, CX + 1, 11, zf, 'blue_ice'); v.set(CX, 12, zf, 'blue_ice')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, zf - 1, 'quartz', 'north') if abs(x - CX) <= 2 else None
    # ---- interior: suelo con lineas de hielo azul, arcadas de columnas de hielo
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1): v.set(x, 1, z, 'packed_ice' if rnd.random() < 0.8 else 'blue_ice')
        for x in range(CX - 1, CX + 2): v.set(x, 1, z, 'light_blue_concrete')
    for z in range(Z0 + 5, Z1 - 3, 5):
        for x in (X0 + 4, X1 - 4):
            v.fill(x, 2, z, x, 13, z, 'blue_ice'); v.set(x, 14, z, 'quartz_block'); v.set(x, 2, z, 'quartz_block')
        for x in range(X0 + 4, X1 - 3): v.set(x, 13, z, 'blue_ice') if abs(x - CX) > 1 else None
    for z in range(Z0 + 7, Z1 - 3, 5):                                                            # arañas de hielo
        for dx in (-5, 5):
            for k in range(1, 4): v.set(CX + dx, 14 - k, z, 'chain[axis=y]')
            v.set(CX + dx, 10, z, 'sea_lantern')
            for (ax, az) in ((1, 0), (-1, 0), (0, 1), (0, -1)): v.set(CX + dx + ax, 10, z + az, 'ice')
            v.set(CX + dx, 9, z, 'ice')
    for z in (Z0 + 8, Z0 + 20):                                                                    # estatuas de hielo laterales
        for x in (X0 + 2, X1 - 2): v.fill(x, 2, z, x, 5, z, 'packed_ice'); v.set(x, 6, z, 'blue_ice')
    # altar de hielo del lider con rosetón detras
    for k in range(3):
        v.fill(CX - 7 + k, 1 + k, Z1 - 9 + k, CX + 7 - k, 1 + k, Z1 - 1, 'quartz_block' if k % 2 == 0 else 'blue_ice')
        for x in range(CX - 7 + k, CX + 8 - k): v.stairs(x, 1 + k, Z1 - 10 + k, 'quartz', 'north')
    for dx in (-5, 5): v.fill(CX + dx, 4, Z1 - 2, CX + dx, 10, Z1 - 2, 'blue_ice'); v.set(CX + dx, 11, Z1 - 2, 'sea_lantern')
    return v, [(CX, 4, Z1 - 5, 180, 'emipokemon desafios kalos iceberg')]
