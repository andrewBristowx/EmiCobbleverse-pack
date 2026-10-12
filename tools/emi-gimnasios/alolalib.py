"""Piezas para las pruebas y la liga de Alola: palmeras, tikis, antorchas, cabañas con techo de paja de bambu."""
import math
from gymlib import V
from piezas2 import *


def palmera(v, x, z, alto, rnd, y0=1):
    """palmera: tronco ligeramente curvado de jungle_log y penacho de hojas"""
    dx = rnd.choice([-1, 0, 1]); dz = rnd.choice([-1, 0, 1])
    px, pz = x, z
    for k in range(alto):
        if k and k % 3 == 0: px += dx; pz += dz
        v.set(px, y0 + k, pz, 'jungle_log[axis=y]')
    top = y0 + alto
    for (ax, az) in ((1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        yy = top - (1 if abs(ax) + abs(az) >= 2 else 0)
        if v.get(px + ax, yy, pz + az) == 'air': v.set(px + ax, yy, pz + az, 'jungle_leaves')
    for (ax, az) in ((3, 0), (-3, 0), (0, 3), (0, -3)):
        if v.get(px + ax, top - 1, pz + az) == 'air': v.set(px + ax, top - 1, pz + az, 'jungle_leaves')
    v.set(px, top, pz, 'jungle_leaves'); v.set(px, top + 1, pz, 'jungle_leaves')


def tiki(v, x, z, alto, mat='mossy_stone_bricks', y0=1):
    """tiki: poste de piedra con cara (ojos, nariz, boca) mirando a -z"""
    v.fill(x - 1, y0, z - 1, x + 1, y0 + alto - 1, z + 1, mat)
    v.set(x - 1, y0 + alto - 3, z - 2, 'black_concrete'); v.set(x + 1, y0 + alto - 3, z - 2, 'black_concrete')
    v.fill(x, y0 + alto - 4, z - 2, x, y0 + alto - 3, z - 2, 'chiseled_stone_bricks'); v.fill(x - 1, y0 + alto - 5, z - 2, x + 1, y0 + alto - 5, z - 2, 'polished_andesite')
    v.fill(x - 2, y0 + alto - 1, z - 1, x + 2, y0 + alto - 1, z + 1, mat); v.set(x, y0 + alto, z, 'red_concrete')


def antorcha_tiki(v, x, z, alto=4, y0=1):
    v.fill(x, y0, z, x, y0 + alto - 1, z, 'bamboo_block'); v.set(x, y0 + alto, z, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')


def techo_bambu(v, x0, x1, z0, z1, y0, vuelo=1):
    """tejado a cuatro aguas de mosaico de bambu (paja) con borde de escalera"""
    xa, xb, za, zb = x0 - vuelo, x1 + vuelo, z0 - vuelo, z1 + vuelo
    k = 0
    while xa + k < xb - k and za + k < zb - k:
        y = y0 + k
        for x in range(xa + k, xb - k + 1): v.stairs(x, y, za + k, 'bamboo_mosaic', 'north'); v.stairs(x, y, zb - k, 'bamboo_mosaic', 'south')
        for z in range(za + k, zb - k + 1): v.stairs(xa + k, y, z, 'bamboo_mosaic', 'west'); v.stairs(xb - k, y, z, 'bamboo_mosaic', 'east')
        if xa + k + 1 <= xb - k - 1 and za + k + 1 <= zb - k - 1: v.fill(xa + k + 1, y, za + k + 1, xb - k - 1, y, zb - k - 1, 'bamboo_mosaic')
        k += 1
    return y0 + k


def flores_tropicales(v, x0, x1, z0, z1, rnd, p=0.25, y=1):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if v.get(x, y, z) == 'air' and v.get(x, y - 1, z) in ('grass_block', 'podzol', 'dirt', 'moss_block') and rnd.random() < p:
                v.set(x, y, z, rnd.choice(['poppy', 'pink_tulip', 'orange_tulip', 'allium', 'red_tulip', 'fern', 'short_grass']))
