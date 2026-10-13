"""Gimnasio de Cascarrafa (Kofu, Agua): patio morisco de azulejos con galerias de arcos, un canal de agua, puestos de mercado y un cangrejo gigante."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 47, 32, 50
CX = 23
X0, X1 = 5, 41
Z0, Z1 = 12, 47
AZUL, BLANCO = 'light_blue_glazed_terracotta', 'white_glazed_terracotta'


def cangrejo(v, cx, y, z):
    """cangrejo gigante de frente: caparazon naranja, ojos sobre tallos, dos pinzas en alto y patas laterales"""
    O, R = 'orange_concrete', 'red_concrete'
    v.fill(cx - 5, y, z - 2, cx + 5, y + 3, z + 2, O)
    v.fill(cx - 4, y + 4, z - 1, cx + 4, y + 4, z + 1, R)
    for s in (-1, 1):
        v.fill(cx + s * 2, y + 5, z, cx + s * 2, y + 6, z, 'white_concrete'); v.set(cx + s * 2, y + 7, z, 'black_concrete')      # ojos
        for k in range(3): v.fill(cx + s * (6 + k), y + k, z - 1, cx + s * (6 + k), y + k, z + 1, O)                               # patas
        v.fill(cx + s * 6, y + 3, z - 1, cx + s * 6, y + 6, z + 1, O)                                                                # brazo
        v.fill(cx + s * 5, y + 7, z - 1, cx + s * 8, y + 9, z + 1, R)                                                                # pinza
        v.fill(cx + s * 6, y + 10, z - 1, cx + s * 6, y + 11, z + 1, R); v.set(cx + s * 7, y + 8, z - 1, 'black_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(251)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_sandstone')
    for z in range(0, Z0):
        for x in range(W): v.set(x, 0, z, 'sandstone' if (x + z) % 5 else 'smooth_sandstone')
    for x in range(CX - 4, CX + 5):
        for z in range(0, Z0): v.set(x, 0, z, AZUL if (x + z) % 2 else BLANCO)
    for (x, z) in [(6, 5), (40, 5), (10, 9), (36, 9)]:                                                # naranjos y tiestos
        v.fill(x, 1, z, x, 3, z, 'jungle_log[axis=y]'); copa(v, x, 5, z, 3, 2, 3, 'oak_leaves', rnd)
        for _ in range(5): v.set(x + rnd.randint(-2, 2), 4 + rnd.randint(0, 2), z + rnd.randint(-2, 2), 'orange_concrete') if v.get(x, 5, z) != 'air' else None
    # ---- muralla con galeria de arcos de herradura
    v.box(X0, 1, Z0, X1, 12, Z1, 'sandstone', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 12, Z1 - 1)
    for y in (1, 2): v.anillo(X0, Z0, X1, Z1, y, 'smooth_sandstone')
    for y in (9, 12): v.anillo(X0, Z0, X1, Z1, y, 'cut_sandstone')
    for k in range(X0 + 2, X1 - 1, 4):
        for dz in (Z1,):
            for y in range(4, 8): v.aire(k, y, dz, k + 1, y, dz)
            v.set(k, 8, dz, 'air'); v.set(k + 1, 8, dz, 'air')
    # galeria cubierta pegada a los muros interiores (anchura 5) con tejado de tejas y arcos hacia el patio
    for (xa, xb, za, zb) in [(X0 + 1, X1 - 1, Z0 + 1, Z0 + 5), (X0 + 1, X1 - 1, Z1 - 5, Z1 - 1), (X0 + 1, X0 + 5, Z0 + 1, Z1 - 1), (X1 - 5, X1 - 1, Z0 + 1, Z1 - 1)]:
        v.fill(xa, 8, za, xb, 8, zb, 'orange_terracotta')
    v.fill(X0 + 6, 8, Z0 + 6, X1 - 6, 8, Z1 - 6, 'air')
    for x in range(X0 + 6, X1 - 5, 4):
        for (zz) in (Z0 + 5, Z1 - 5):
            v.fill(x, 2, zz, x, 7, zz, 'smooth_sandstone'); v.set(x, 2, zz, 'chiseled_sandstone')
    for z in range(Z0 + 6, Z1 - 5, 4):
        for xx in (X0 + 5, X1 - 5): v.fill(xx, 2, z, xx, 7, z, 'smooth_sandstone'); v.set(xx, 2, z, 'chiseled_sandstone')
    v.fill(X0 + 6, 8, Z0 + 5, X1 - 6, 8, Z0 + 5, 'orange_terracotta'); v.fill(X0 + 6, 8, Z1 - 5, X1 - 6, 8, Z1 - 5, 'orange_terracotta')
    # ---- puerta de herradura con azulejos
    hueco_puerta(v, CX, Z0, 5, 7, 'blue_terracotta', y0=2)
    for dx in (-3, 3):
        for y in range(2, 11): v.set(CX + dx, y, Z0 - 1, AZUL if y % 2 else BLANCO)
    v.fill(CX - 3, 11, Z0 - 1, CX + 3, 11, Z0 - 1, 'blue_terracotta')
    # torre-minarete con cupula de cebolla azul
    for (tx, tz) in [(X0 - 1, Z0 - 1), (X1 + 1, Z0 - 1)]:
        v.box(tx - 2, 1, tz - 2, tx + 2, 22, tz + 2, 'sandstone', interior=None)
        for y in (6, 12, 18): v.anillo(tx - 2, tz - 2, tx + 2, tz + 2, y, AZUL)
        for y in (9, 15): v.set(tx, y, tz - 2, 'air'); v.set(tx, y + 1, tz - 2, 'air')
        v.esfera(tx, 25, tz, 3, 'blue_terracotta'); v.fill(tx, 28, tz, tx, 30, tz, 'yellow_concrete')
    # ---- patio: suelo de azulejos con estrella, canal central de agua encerrado y fuentes
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1):
            v.set(x, 1, z, AZUL if (x // 2 + z // 2) % 2 else BLANCO)
    for z in range(Z0 + 6, Z1 - 6):
        for x in range(CX - 3, CX + 4):
            v.set(x, 1, z, 'water' if abs(x - CX) < 3 else 'blue_terracotta')
    for x in range(CX - 3, CX + 4):
        v.set(x, 1, Z0 + 5, 'blue_terracotta'); v.set(x, 1, Z1 - 6, 'blue_terracotta')
    for z in range(Z0 + 6, Z1 - 6, 6):
        for x in (CX - 3, CX + 3): v.fill(x, 2, z, x, 2, z, 'smooth_sandstone_slab[type=bottom]')
    for (fx, fz) in [(CX - 11, Z0 + 10), (CX + 11, Z0 + 10), (CX - 11, Z1 - 12), (CX + 11, Z1 - 12)]:                      # fuentes
        disco_f(v, fx, 1, fz, 2, anillos([(2, 'blue_terracotta')])); v.set(fx, 1, fz, 'water')
    # puestos de mercado con toldos a rayas
    for (x, z) in [(X0 + 7, Z0 + 12), (X0 + 7, Z0 + 20), (X1 - 9, Z0 + 12), (X1 - 9, Z0 + 20), (X0 + 7, Z0 + 28), (X1 - 9, Z0 + 28)]:
        for dx, dz in ((0, 0), (2, 0), (0, 2), (2, 2)): v.fill(x + dx, 2, z + dz, x + dx, 4, z + dz, 'oak_fence')
        for i in range(3):
            for j in range(3): v.set(x + i, 5, z + j, 'red_wool' if (i + j) % 2 == 0 else 'white_wool')
        v.set(x + 1, 2, z + 1, 'barrel[facing=up]')
    # ---- fondo: escenario con el cangrejo gigante
    v.fill(CX - 8, 2, Z1 - 9, CX + 8, 2, Z1 - 1, 'blue_terracotta'); v.fill(CX - 5, 3, Z1 - 8, CX + 5, 3, Z1 - 1, 'smooth_sandstone')
    for x in range(CX - 8, CX + 9): v.stairs(x, 2, Z1 - 10, 'sandstone', 'north') if abs(x - CX) <= 2 else None
    cangrejo(v, CX, 4, Z1 - 2)
    for dx in (-6, 6): v.fill(CX + dx, 4, Z1 - 7, CX + dx, 7, Z1 - 7, 'oak_fence'); v.set(CX + dx, 8, Z1 - 7, 'lantern')
    return v, [(CX, 4, Z1 - 5, 180, 'emipokemon desafios paldea agua')]
