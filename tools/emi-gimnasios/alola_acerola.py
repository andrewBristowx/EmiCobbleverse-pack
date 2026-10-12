"""Prueba de Acerola (Fantasma) en el supermercado abandonado Thrifty Megamart: fachada descolorida, aparcamiento con coches, pasillos con telarañas y estanterias."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 41, 28, 44
CX = 20
X0, X1 = 5, 35
Z0, Z1 = 14, 41


def coche(v, x, z, color):
    v.fill(x - 2, 1, z, x + 2, 2, z + 7, color); v.fill(x - 1, 3, z + 2, x + 1, 3, z + 5, 'light_gray_stained_glass'); v.fill(x - 1, 4, z + 2, x + 1, 4, z + 5, color)
    for (dx, dz) in ((-3, 1), (3, 1), (-3, 6), (3, 6)): v.set(x + dx, 1, z + dz, 'black_concrete')


def carrito(v, x, z):
    v.fill(x, 1, z, x + 1, 1, z + 2, 'iron_bars'); v.fill(x, 2, z, x + 1, 2, z + 2, 'iron_bars'); v.set(x, 3, z, 'iron_bars'); v.set(x + 1, 3, z, 'iron_bars')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(361)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['gray_concrete', 'gray_concrete', 'cracked_stone_bricks', 'light_gray_concrete', 'coarse_dirt']))
    for z in range(0, Z0):
        for x in range(W):
            if (x - 3) % 6 == 0: v.set(x, 0, z, 'white_concrete')
    # ---- aparcamiento: coches abandonados, carritos y hierbajos
    for (x, z, c) in [(8, 3, 'blue_concrete'), (20, 2, 'red_concrete'), (32, 4, 'gray_concrete'), (14, 9, 'light_gray_concrete')]: coche(v, x, z, c)
    for (x, z) in [(26, 8), (28, 9), (3, 10), (12, 11), (33, 11)]: carrito(v, x, z)
    for _ in range(40):
        x, z = rnd.randint(1, W - 2), rnd.randint(1, Z0 - 2)
        if v.get(x, 1, z) == 'air': v.set(x, 0, z, 'moss_block'); v.set(x, 1, z, rnd.choice(['short_grass', 'fern']))
    for (x, z) in [(2, 12), (38, 12)]: v.fill(x, 1, z, x, 8, z, 'iron_bars'); v.set(x, 9, z, 'soul_lantern')
    # ---- edificio: cuerpo gris descolorido con ventanales rotos
    v.box(X0, 1, Z0, X1, 14, Z1, 'light_gray_concrete', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 14, Z1 - 1)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'cracked_stone_bricks')
    for y in (1, 2): v.anillo(X0, Z0, X1, Z1, y, 'gray_concrete')
    for y in (11, 14): v.anillo(X0, Z0, X1, Z1, y, 'gray_concrete')
    for x in range(X0 + 1, X1):
        if x % 4:
            for y in range(4, 10):
                if rnd.random() < 0.75: v.set(x, y, Z0, 'light_gray_stained_glass')
    for z in range(Z0 + 1, Z1):
        if z % 4:
            for y in range(4, 10):
                if rnd.random() < 0.6: v.set(X0, y, z, 'light_gray_stained_glass'); v.set(X1, y, z, 'light_gray_stained_glass')
    for x in range(X0, X1 + 1, 5): v.fill(x, 1, Z0, x, 14, Z0, 'gray_concrete')
    # puerta automatica rota y cartel con bolsa de compra
    hueco_puerta(v, CX, Z0, 5, 6, 'gray_concrete', y0=2)
    v.fill(CX - 8, 12, Z0 - 1, CX + 8, 18, Z0 - 1, 'gray_concrete')
    poligono_xy(v, [(CX - 4, 12.2), (CX + 4, 12.2), (CX + 3, 17.6), (CX - 3, 17.6)], Z0 - 1, 'orange_concrete')
    for dx in (-2, 2): v.fill(CX + dx, 18, Z0 - 1, CX + dx, 19, Z0 - 1, 'orange_concrete')
    v.fill(CX - 2, 14, Z0 - 1, CX + 2, 15, Z0 - 1, 'white_concrete')
    for _ in range(25): v.set(rnd.randint(CX - 8, CX + 8), rnd.randint(12, 18), Z0 - 1, 'cracked_stone_bricks')
    v.fill(X0, 15, Z0, X1, 15, Z1, 'gray_concrete')
    for x in range(X0 + 3, X1 - 2, 8): v.fill(x, 16, 20, x + 3, 17, 24, 'light_gray_concrete'); v.set(x + 1, 18, 21, 'iron_bars')
    # ---- interior: pasillos de estanterias, cajas, telarañas y luces moribundas
    for z in range(Z0 + 5, Z1 - 8, 4):
        for x in range(X0 + 3, X1 - 2, 7):
            v.fill(x, 2, z, x + 4, 2, z, 'white_concrete'); v.fill(x, 3, z, x + 4, 3, z, 'iron_bars'); v.fill(x, 4, z, x + 4, 4, z, 'white_concrete')
            for dx in range(0, 5):
                if rnd.random() < 0.5: v.set(x + dx, 3, z, rnd.choice(['barrel[facing=up]', 'barrel[facing=up]', 'chest[facing=south]', 'white_concrete']))
    for _ in range(80):
        v.set(rnd.randint(X0 + 1, X1 - 1), rnd.randint(9, 13), rnd.randint(Z0 + 1, Z1 - 1), 'cobweb')
    for z in range(Z0 + 3, Z1 - 3, 5):
        for x in range(X0 + 4, X1 - 3, 7):
            if rnd.random() < 0.7:
                for k in range(1, 3): v.set(x, 14 - k, z, 'chain[axis=y]')
                v.set(x, 11, z, 'soul_lantern[hanging=true]')
    for (x, z) in [(10, 18), (14, 30), (26, 20), (24, 33), (8, 36)]: carrito(v, x, z)
    for x in range(X0 + 2, X0 + 14, 3):                                                      # cajas registradoras
        v.fill(x, 2, Z0 + 3, x + 1, 3, Z0 + 3, 'polished_andesite'); v.set(x, 4, Z0 + 3, 'iron_bars')
    # ---- estrado del lider junto a las neveras del fondo
    for x in range(X0 + 3, X1 - 2, 3):
        v.fill(x, 2, Z1 - 1, x + 1, 6, Z1 - 1, 'white_concrete'); v.fill(x, 3, Z1 - 1, x + 1, 5, Z1 - 1, 'light_blue_stained_glass')
    v.fill(CX - 6, 2, Z1 - 7, CX + 6, 2, Z1 - 2, 'polished_andesite'); v.fill(CX - 4, 3, Z1 - 6, CX + 4, 3, Z1 - 2, 'cracked_stone_bricks')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 8, 'stone_brick', 'north') if abs(x - CX) <= 2 else None
    for dx in (-4, 4): v.fill(CX + dx, 4, Z1 - 3, CX + dx, 6, Z1 - 3, 'iron_bars'); v.set(CX + dx, 7, Z1 - 3, 'soul_lantern')
    return v, [(CX, 4, Z1 - 5, 180, 'emipokemon desafios alola captain_acerola')]
