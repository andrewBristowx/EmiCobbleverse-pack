"""Gimnasio de Artazon (Brassius, Planta): museo de arte moderno blanco con un arbol-escultura de colores que sale por el techo, cuadros y una paleta gigante."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 41, 44, 46
CX = 20
X0, X1 = 6, 34
Z0, Z1 = 14, 42
COLORES = ['red_concrete', 'orange_concrete', 'yellow_concrete', 'lime_concrete', 'cyan_concrete', 'blue_concrete', 'magenta_concrete', 'white_concrete']


def cuadro(v, x, y, z, ancho, alto, rnd, eje='x'):
    """cuadro abstracto con marco de madera oscura en una pared (plano x=cte o z=cte)"""
    for i in range(-1, ancho + 1):
        for j in range(-1, alto + 1):
            borde = i in (-1, ancho) or j in (-1, alto)
            b = 'dark_oak_planks' if borde else rnd.choice(COLORES[:7] if rnd.random() < 0.8 else ['white_concrete'])
            if eje == 'x': v.set(x, y + j, z + i, b)
            else: v.set(x + i, y + j, z, b)


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(231)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_quartz')
    for z in range(0, Z0):
        for x in range(W):
            v.set(x, 0, z, 'grass_block' if abs(x - CX) > 5 else 'smooth_quartz')
    # ---- plaza: paleta de pintor gigante, pincel y esferas de color
    disco_f(v, 9, 1, 6, 5, anillos([(5, 'dark_oak_planks')]))
    for (dx, dz, c) in [(-2, -1, 'red_concrete'), (0, -3, 'yellow_concrete'), (2, -1, 'blue_concrete'), (2, 2, 'lime_concrete'), (-2, 2, 'magenta_concrete')]: disco_f(v, 9 + dx, 2, 6 + dz, 1, anillos([(1.4, c)]))
    v.set(12, 2, 9, 'air')
    v.fill(32, 1, 5, 32, 14, 5, 'oak_fence'); v.fill(32, 15, 5, 32, 17, 5, 'dark_oak_planks'); v.fill(32, 18, 5, 32, 20, 5, 'lime_concrete')
    for (x, z, c) in [(CX - 9, 8, 'red_concrete'), (CX + 9, 9, 'blue_concrete'), (CX - 12, 11, 'yellow_concrete')]: v.esfera(x, 3, z, 2, c)
    # ---- caja del museo con zocalo y grandes ventanales
    v.box(X0, 1, Z0, X1, 12, Z1, 'white_concrete', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 12, Z1 - 1)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'smooth_quartz')
    for y in (1, 2): v.anillo(X0, Z0, X1, Z1, y, 'light_gray_concrete')
    for z in range(Z0 + 3, Z1 - 2, 7):
        for dz in range(0, 4):
            for y in range(4, 11): v.set(X0, y, z + dz, 'glass'); v.set(X1, y, z + dz, 'glass')
    v.anillo(X0, Z0, X1, Z1, 12, 'black_concrete')
    for k in range(0, 8):                                                              # bloques de color en la cornisa (alegoria de pinceladas)
        x = X0 + 2 + k * 3
        v.fill(x, 13, Z0, x + 1, 14, Z0, COLORES[k]); v.fill(x, 13, Z1, x + 1, 14, Z1, COLORES[(k + 3) % 8])
    # ---- fachada: ventanal central y marco de colores
    hueco_puerta(v, CX, Z0, 7, 8, 'black_concrete', y0=2)
    for x in range(CX - 4, CX + 5):
        for y in range(10, 12): v.set(x, y, Z0, 'glass')
    cuadro(v, X0 + 3, 4, Z0, 5, 5, rnd, 'z'); cuadro(v, X1 - 8, 4, Z0, 5, 5, rnd, 'z')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, Z0 - 1, 'quartz', 'north') if abs(x - CX) <= 2 else None
    # ---- el arbol: tronco de roble oscuro, ramas y copa de hojas de colores que atraviesa el techo
    tz = 28
    v.fill(CX - 5, 12, tz - 5, CX + 5, 12, tz + 5, 'air')
    v.fill(CX - 1, 1, tz - 1, CX + 1, 20, tz + 1, 'dark_oak_log[axis=y]')
    for (dx, dz, dy) in [(-6, -2, 6), (6, 2, 7), (-3, 6, 5), (4, -6, 6), (0, 0, 9)]:
        v.linea(CX, 15, tz, CX + dx, 15 + dy, tz + dz, 'dark_oak_log[axis=y]')
    for _ in range(80):
        cx = CX + rnd.randint(-9, 9); cy = rnd.randint(19, 28); cz = tz + rnd.randint(-8, 8)
        col = rnd.choice(['lime_concrete', 'green_concrete', 'yellow_concrete', 'orange_concrete', 'lime_concrete', 'cyan_concrete'])
        v.esfera(cx, cy, cz, rnd.randint(2, 3), col)
    v.fill(CX - 1, 12, tz - 1, CX + 1, 20, tz + 1, 'dark_oak_log[axis=y]')
    # ---- interior: cuadros en las paredes, pedestales con esculturas y bancos
    for z in range(Z0 + 4, Z1 - 7, 7):
        cuadro(v, X0 + 1, 4, z, 5, 5, rnd, 'x'); cuadro(v, X1 - 1, 4, z, 5, 5, rnd, 'x')
    for z in range(Z0 + 4, Z1 - 7, 7):
        for dx in (-10, 10):
            v.fill(CX + dx, 2, z + 2, CX + dx, 3, z + 2, 'quartz_block'); v.set(CX + dx, 4, z + 2, rnd.choice(['amethyst_block', 'prismarine', 'gold_block', 'copper_block', 'lapis_block']))
        for dx in (-5, 5): v.set(CX + dx, 2, z + 4, 'smooth_quartz_slab[type=bottom]')
    for z in range(Z0 + 5, Z1 - 4, 8):
        for dx in (-6, 0, 6):
            for k in range(1, 4): v.set(CX + dx, 11 - k + 1, z, 'chain[axis=y]')
            v.set(CX + dx, 8, z, 'sea_lantern')
    for z in range(Z0 + 1, Z1):
        for x in range(CX - 2, CX + 3): v.set(x, 1, z, 'lime_concrete' if abs(x - CX) == 2 else 'white_concrete')
    # estrado del lider con caballetes
    v.fill(CX - 6, 2, Z1 - 6, CX + 6, 2, Z1 - 1, 'dark_oak_planks'); v.fill(CX - 4, 3, Z1 - 5, CX + 4, 3, Z1 - 1, 'smooth_quartz')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 7, 'dark_oak', 'north') if abs(x - CX) <= 3 else None
    for dx in (-5, 5): v.fill(CX + dx, 4, Z1 - 2, CX + dx, 7, Z1 - 2, 'oak_fence'); v.fill(CX + dx - 1, 5, Z1 - 3, CX + dx + 1, 8, Z1 - 3, 'white_concrete')
    cuadro(v, CX - 3, 5, Z1 - 1, 6, 6, rnd, 'z')
    return v, [(CX, 4, Z1 - 4, 180, 'emipokemon desafios paldea hierba')]
