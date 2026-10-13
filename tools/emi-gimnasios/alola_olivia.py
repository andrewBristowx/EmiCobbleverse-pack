"""Gran prueba de Olivia (Roca) en las Ruinas de la Vida: piramide escalonada de piedra con caras talladas, musgo, una gran escalinata y un altar en la cima."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 43, 34, 44
CX, CZ = 21, 24
NIVELES = [(17, 1, 4), (13, 5, 8), (9, 9, 12), (5, 13, 16)]       # (semilado, y0, y1): cada nivel retrocede 4 y sube 4


def cara_tallada(v, x, y, z):
    """cara de piedra en relieve (ojos oscuros, nariz y boca) mirando a -z"""
    v.fill(x - 1, y, z, x + 1, y + 2, z, 'chiseled_stone_bricks')
    v.set(x - 1, y + 2, z - 1, 'black_concrete'); v.set(x + 1, y + 2, z - 1, 'black_concrete')
    v.set(x, y + 1, z - 1, 'polished_andesite'); v.fill(x - 1, y, z - 1, x + 1, y, z - 1, 'cracked_stone_bricks')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(391)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['grass_block', 'mossy_cobblestone', 'grass_block', 'podzol', 'moss_block']))
    for z in range(0, CZ - 21):
        for x in range(CX - 2, CX + 3): v.set(x, 0, z, 'mossy_stone_bricks' if (x + z) % 2 else 'stone_bricks')
    # ---- piramide escalonada
    for (s, y0, y1) in NIVELES:
        for y in range(y0, y1 + 1):
            for x in range(CX - s, CX + s + 1):
                for z in range(CZ - s, CZ + s + 1):
                    borde = abs(x - CX) == s or abs(z - CZ) == s
                    if borde or y == y1: v.set(x, y, z, rnd.choice(['stone_bricks'] * 3 + ['mossy_stone_bricks'] * 3 + ['cracked_stone_bricks', 'stone_bricks']))
                    else: v.set(x, y, z, 'cobblestone')
    # ---- gran escalinata central continua de la base a la cima (45 grados)
    for k, (s, y0, y1) in enumerate(NIVELES):
        z_ini = CZ - s - 4
        for j in range(4):
            z = z_ini + j; y = y0 + j
            for x in range(CX - 2, CX + 3):
                v.fill(x, 1, z, x, y - 1, z, 'stone_bricks') if y > 1 else None
                v.stairs(x, y, z, 'stone_brick', 'north')
    for k, (s, y0, y1) in enumerate(NIVELES):                                              # muretes laterales de la escalinata
        for j in range(4):
            z = CZ - s - 4 + j; y = y0 + j
            for x in (CX - 3, CX + 3):
                v.fill(x, 1, z, x, y, z, 'mossy_stone_bricks')
    # ---- caras talladas en los frentes de cada nivel y antorchas
    for (s, y0, y1) in NIVELES:
        for dx in (-5, 5, -9, 9, -13, 13):
            if abs(dx) < s - 1: cara_tallada(v, CX + dx, y0 + 1, CZ - s)
    for (s, y0, y1) in NIVELES:
        for (dx, dz) in ((s, s), (-s, s), (s, -s), (-s, -s)): antorcha_tiki(v, CX + dx, CZ + dz, 2, y1 + 1)
    # ---- cima: suelo con dibujo, altar y tikis
    s, y0, y1 = NIVELES[-1]
    disco_f(v, CX, y1, CZ, 5, anillos([(1.5, 'chiseled_stone_bricks'), (3.5, 'polished_andesite'), (5, 'mossy_stone_bricks')]))
    disco_f(v, CX, y1 + 1, CZ, 2, anillos([(2, 'chiseled_stone_bricks')]))
    tiki(v, CX, CZ + 4, 6, 'mossy_stone_bricks', y1 + 1)
    for dx in (-4, 4): tiki(v, CX + dx, CZ + 3, 4, 'stone_bricks', y1 + 1)
    # ---- musgo, palmeras y rocas del entorno
    for _ in range(160):
        x, z = rnd.randint(CX - 17, CX + 17), rnd.randint(CZ - 17, CZ + 17)
        for y in range(17, 0, -1):
            if v.get(x, y, z) != 'air':
                if 'stairs' not in v.get(x, y, z) and rnd.random() < 0.5: v.set(x, y, z, 'moss_block')
                break
    for (x, z) in [(3, 6), (39, 7), (3, 42), (39, 42), (10, 2), (33, 3)]: palmera(v, x, z, rnd.randint(5, 8), rnd)
    flores_tropicales(v, 1, W - 2, 1, L - 2, rnd, 0.08)
    for (x, z) in [(CX - 6, 4), (CX + 6, 4)]: antorcha_tiki(v, x, z, 4)
    return v, [(CX, y1 + 2, CZ + 1, 180, 'emipokemon desafios alola kahuna_olivia')]
