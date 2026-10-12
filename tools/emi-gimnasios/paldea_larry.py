"""Gimnasio de Medali (Larry, Normal): edificio de oficinas gris con muro cortina, parada de bus, oficina diaria con cubiculos y maquinas expendedoras."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 39, 30, 42
CX = 19
X0, X1 = 5, 33
Z0, Z1 = 13, 39


def emblema_normal(v, cx, cy, z, r):
    elipse_xy(v, cx, cy, z, r, r, lambda d, a: 'light_gray_concrete' if d > 0.8 else 'white_concrete' if d > 0.35 else 'gray_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(261)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'gray_concrete')
    for z in range(0, Z0):
        for x in range(W): v.set(x, 0, z, 'light_gray_concrete' if (x + z) % 2 else 'gray_concrete')
    for z in range(0, Z0 - 1):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'white_concrete' if x in (CX - 3, CX + 3) else 'black_concrete')
    # ---- parada de autobus, bicis y papeleras en la acera
    v.fill(3, 1, 4, 3, 4, 4, 'iron_bars'); v.fill(9, 1, 4, 9, 4, 4, 'iron_bars'); v.fill(3, 5, 3, 9, 5, 6, 'glass'); v.set(4, 1, 5, 'oak_slab[type=bottom]'); v.fill(4, 1, 5, 8, 1, 5, 'oak_slab[type=bottom]')
    v.fill(30, 1, 5, 31, 2, 5, 'iron_bars'); v.fill(34, 1, 6, 34, 2, 6, 'iron_bars')
    for (x, z) in [(CX - 7, 6), (CX + 7, 6), (CX + 9, 9)]:
        v.fill(x, 1, z, x, 2, z, 'gray_concrete'); v.set(x, 3, z, 'light_gray_concrete')
    for (x, z) in [(14, 4), (24, 4)]:
        arbol(v, x, 1, z, 3, 2, 'oak_leaves', 'oak_log[axis=y]', rnd)
    # ---- edificio: cuerpo gris con muro cortina de cristal y pilares
    v.box(X0, 1, Z0, X1, 18, Z1, 'gray_concrete', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 17, Z1 - 1)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'gray_concrete')
    for x in range(X0, X1 + 1, 4): v.fill(x, 1, Z0, x, 18, Z0, 'light_gray_concrete'); v.fill(x, 1, Z1, x, 18, Z1, 'light_gray_concrete')
    for z in range(Z0, Z1 + 1, 4): v.fill(X0, 1, z, X0, 18, z, 'light_gray_concrete'); v.fill(X1, 1, z, X1, 18, z, 'light_gray_concrete')
    for y0 in (3, 10):
        for x in range(X0 + 1, X1):
            if (x - X0) % 4: v.fill(x, y0, Z0, x, y0 + 4, Z0, 'light_blue_stained_glass'); v.fill(x, y0, Z1, x, y0 + 4, Z1, 'light_blue_stained_glass')
        for z in range(Z0 + 1, Z1):
            if (z - Z0) % 4: v.fill(X0, y0, z, X0, y0 + 4, z, 'light_blue_stained_glass'); v.fill(X1, y0, z, X1, y0 + 4, z, 'light_blue_stained_glass')
    v.anillo(X0, Z0, X1, Z1, 18, 'black_concrete'); v.anillo(X0, Z0, X1, Z1, 8, 'white_concrete')
    # puerta giratoria (cuatro paneles de cristal) y marquesina
    v.aire(CX - 2, 2, Z0, CX + 2, 6, Z0)
    for dx in (-3, 3): v.fill(CX + dx, 2, Z0, CX + dx, 7, Z0, 'black_concrete')
    v.fill(CX - 3, 7, Z0, CX + 3, 7, Z0, 'black_concrete')
    v.fill(CX - 5, 8, Z0 - 3, CX + 5, 8, Z0 - 1, 'black_concrete')
    for dx in (-5, 5): v.fill(CX + dx, 1, Z0 - 2, CX + dx, 7, Z0 - 2, 'iron_bars')
    emblema_normal(v, CX, 13, Z0, 4)
    # azotea: unidades de aire acondicionado, deposito de agua, mastil con bandera
    for (x, z) in [(10, 20), (10, 30), (28, 20), (28, 30)]:
        v.fill(x - 1, 19, z - 1, x + 1, 20, z + 1, 'light_gray_concrete'); v.set(x, 21, z, 'iron_bars'); v.set(x - 1, 20, z - 2, 'gray_concrete')
    v.cilindro(CX, 19, 24, 33, 2, 'oak_planks'); v.disco(CX, 25, 33, 2, 'oak_planks')
    v.fill(CX + 10, 19, 17, CX + 10, 28, 17, 'iron_bars'); v.fill(CX + 11, 26, 17, CX + 13, 28, 17, 'white_concrete'); v.set(CX + 10, 29, 17, 'sea_lantern')
    # ---- interior: recepcion, cubiculos, plantas, expendedoras y mesa del lider
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1): v.set(x, 1, z, 'gray_concrete')
    for z in range(Z0 + 1, Z1):
        for x in range(CX - 2, CX + 3): v.set(x, 1, z, 'light_gray_concrete')
    for x in range(CX - 4, CX + 5): v.set(x, 2, Z0 + 4, 'smooth_quartz'); v.set(x, 3, Z0 + 4, 'smooth_quartz_slab[type=bottom]')
    v.aire(CX - 1, 2, Z0 + 4, CX + 1, 3, Z0 + 4)
    for fila in range(4):
        z = Z0 + 9 + fila * 5
        for lado in (-1, 1):
            for k in range(2):
                x = CX + lado * (6 + k * 6)
                v.fill(x - 2, 2, z - 2, x + 2, 4, z - 2, 'white_concrete'); v.fill(x - 2, 2, z - 2, x - 2, 4, z + 1, 'white_concrete')
                v.fill(x - 1, 2, z, x + 1, 2, z, 'oak_slab[type=top]')
                v.set(x, 3, z, 'iron_bars'); v.set(x + 1, 2, z + 1, 'dark_oak_stairs[facing=north,half=bottom,shape=straight]')
    for z in range(Z0 + 3, Z1 - 5, 6):
        v.set(X0 + 1, 2, z, 'potted_fern'); v.set(X1 - 1, 2, z, 'potted_fern')
    for z in range(Z0 + 2, Z1 - 3, 5):
        for x in range(X0 + 3, X1 - 2, 5): v.set(x, 17, z, 'sea_lantern')
    for z in (Z0 + 8, Z1 - 10):
        v.fill(X0 + 1, 2, z, X0 + 1, 4, z, 'red_concrete'); v.set(X0 + 1, 3, z + 1, 'glass'); v.fill(X0 + 1, 2, z + 1, X0 + 1, 4, z + 1, 'blue_concrete')
    for x in range(X0 + 2, X0 + 6): v.set(x, 12, Z1 - 1, 'white_concrete')
    elipse_xy(v, X1 - 6, 11, Z1 - 1, 3, 3, lambda d, a: 'black_concrete' if d > 0.85 else 'white_concrete')
    v.set(X1 - 6, 11, Z1 - 1, 'black_concrete')
    # estrado y mesa de despacho del lider
    v.fill(CX - 6, 2, Z1 - 7, CX + 6, 2, Z1 - 1, 'light_gray_concrete')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 8, 'stone_brick', 'north') if abs(x - CX) <= 2 else None
    v.fill(CX - 3, 3, Z1 - 3, CX + 3, 3, Z1 - 3, 'dark_oak_planks'); v.fill(CX - 3, 4, Z1 - 3, CX + 3, 4, Z1 - 3, 'dark_oak_slab[type=bottom]')
    v.set(CX - 2, 5, Z1 - 3, 'lantern'); v.set(CX + 2, 5, Z1 - 3, 'potted_fern')
    emblema_normal(v, CX, 9, Z1, 5)
    return v, [(CX, 3, Z1 - 5, 180, 'emipokemon desafios paldea normal')]
