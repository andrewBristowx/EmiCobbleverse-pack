"""Estadio de Hulbury (Nessa, Agua): almacen del puerto con tejado de bóveda a rayas, faro, un barco en dique seco y una piscina con muelles."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 45, 40, 50
CX = 22
X0, X1 = 7, 37
Z0, Z1 = 14, 47
R = 15.5            # radio de la boveda (ancho 31)
YS = 9              # arranque de la boveda


def ancla(v, cx, cy, z):
    v.fill(cx, cy - 4, z, cx, cy + 4, z, 'iron_block')
    v.fill(cx - 2, cy + 2, z, cx + 2, cy + 2, z, 'iron_block')
    v.set(cx, cy + 5, z, 'iron_block'); v.set(cx - 1, cy + 5, z, 'iron_block'); v.set(cx + 1, cy + 5, z, 'iron_block'); v.set(cx, cy + 6, z, 'iron_block')
    for k in range(0, 4):
        v.set(cx - 1 - k, cy - 4 + k, z, 'iron_block'); v.set(cx + 1 + k, cy - 4 + k, z, 'iron_block')
    v.set(cx - 5, cy, z, 'iron_block'); v.set(cx + 5, cy, z, 'iron_block')
    v.set(cx - 5, cy - 1, z, 'iron_block'); v.set(cx + 5, cy - 1, z, 'iron_block')


def barco(v, x, z, largo):
    """barco en dique seco con la proa hacia -z: casco de roble oscuro, cubierta, mastil y velas"""
    for i in range(largo):
        a = 3 if 2 < i < largo - 3 else 2
        v.fill(x - a, 2, z + i, x + a, 2, z + i, 'dark_oak_planks')
        v.fill(x - a, 3, z + i, x - a, 4, z + i, 'dark_oak_planks'); v.fill(x + a, 3, z + i, x + a, 4, z + i, 'dark_oak_planks')
        v.fill(x - a + 1, 3, z + i, x + a - 1, 3, z + i, 'spruce_planks')
    v.fill(x - 1, 1, z + 3, x + 1, 1, z + largo - 4, 'dark_oak_planks')
    v.fill(x, 4, z + largo // 2, x, 18, z + largo // 2, 'stripped_spruce_log[axis=y]')
    for y in range(7, 17): v.fill(x - 3, y, z + largo // 2 - 1, x + 3, y, z + largo // 2 - 1, 'white_wool') if y % 5 else None
    v.fill(x, 4, z + 1, x, 6, z, 'dark_oak_planks'); v.set(x, 7, z, 'lantern')
    for k in range(1, 5): v.set(x, 5 + k, z - 1, 'dark_oak_planks') if k == 1 else None


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(141)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_stone')
    for x in range(W):
        for z in range(L):
            if rnd.random() < 0.25: v.set(x, 0, z, 'stone_bricks')
    for z in range(0, Z0):
        for x in range(CX - 4, CX + 5): v.set(x, 0, z, 'dark_oak_planks' if (x + z) % 3 else 'spruce_planks')
    # bolardos, cajas, redes y boyas en el muelle
    for x in range(2, W - 2, 5):
        if abs(x - CX) > 6: v.fill(x, 1, 3, x, 2, 3, 'iron_bars'); v.set(x, 3, 3, 'red_concrete')
    for (x, z) in [(10, 9), (11, 9), (10, 10), (34, 9), (35, 9), (34, 11)]: v.set(x, 1, z, 'barrel[facing=up]')
    v.set(10, 2, 9, 'barrel[facing=up]')
    for x in range(2, 9): v.set(x, 1, 6, 'cobweb') if rnd.random() < 0.5 else None
    barco(v, 10, 2, 11)
    for (dx, dz) in ((-2, 2), (2, 2), (-2, 8), (2, 8)): v.set(10 + dx, 1, 2 + dz, 'oak_log[axis=y]')
    # ---- almacen con boveda a rayas
    v.box(X0, 1, Z0, X1, YS, Z1, 'white_concrete', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, YS, Z1 - 1)
    for z in range(Z0, Z1 + 1):
        for i in range(0, 1201):
            a = math.pi * i / 1200
            x = round(CX + math.cos(a) * R); y = round(YS + math.sin(a) * R * 0.95)
            v.set(x, y, z, 'blue_concrete' if (z // 2) % 2 else 'white_concrete')
    for z in (Z0, Z1):
        for x in range(X0, X1 + 1):
            h = YS + math.sqrt(max(0, 1 - ((x - CX) / R) ** 2)) * R * 0.95
            for y in range(YS, int(h) + 1): v.set(x, y, z, 'white_concrete' if (x // 2) % 2 else 'light_blue_concrete')
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'prismarine_bricks')
    for y in (1, 2): v.anillo(X0, Z0, X1, Z1, y, 'stone_bricks')
    for y in (4, 5): v.anillo(X0, Z0, X1, Z1, y, 'blue_concrete')
    for z in range(Z0 + 3, Z1 - 1, 5):                                    # ojos de buey
        for x in (X0, X1):
            elipse_xy(v, 0, 0, 0, 1, 1, lambda d, a: None)
            v.set(x, 6, z, 'glass'); v.set(x, 7, z, 'glass'); v.set(x, 6, z + 1, 'glass'); v.set(x, 7, z + 1, 'glass')
    # puerta en arco y ancla sobre ella, ojos de buey en la fachada
    hueco_puerta(v, CX, Z0, 7, 7, 'blue_concrete', y0=2)
    ancla(v, CX, 15, Z0 - 1)
    for dx in (-8, 8):
        for (cx2, cy2) in ((CX + dx, 7),):
            elipse_xy(v, cx2, cy2, Z0, 2, 2, lambda d, a: 'light_gray_concrete' if d > 0.7 else 'glass')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, Z0 - 1, 'stone_brick', 'north') if abs(x - CX) <= 2 else None
    # ---- faro con rayas rojas y blancas
    fx, fz = 37, 7
    for y in range(1, 27):
        r = 4.3 - y * 0.05
        for x in range(fx - 5, fx + 6):
            for z in range(fz - 5, fz + 6):
                d = math.hypot(x - fx, z - fz)
                if r - 1.1 < d <= r: v.set(x, y, z, 'red_concrete' if (y // 4) % 2 == 0 else 'white_concrete')
    v.disco(fx, 1, fz, 4, 'stone_bricks')
    v.cilindro(fx, 27, 29, fz, 3, 'glass'); v.disco(fx, 26, fz, 4, 'iron_block'); v.disco(fx, 30, fz, 3, 'iron_block')
    v.set(fx, 28, fz, 'sea_lantern'); v.set(fx, 27, fz, 'sea_lantern'); v.set(fx, 29, fz, 'sea_lantern')
    cono(v, fx, 31, fz, 4, 5, 'red_concrete')
    for y in (6, 12, 18):
        for (dx, dz) in ((0, 3), (3, 0), (-3, 0), (0, -3)): v.set(fx + dx, y, fz + dz, 'glass_pane')
    # ---- interior: piscina con muelles de madera
    for z in range(Z0 + 3, Z1 - 2):
        for x in range(X0 + 4, X1 - 3):
            if abs(x - CX) > 1: v.set(x, 1, z, 'water')
    v.anillo(X0 + 3, Z0 + 2, X1 - 3, Z1 - 1, 1, 'prismarine_bricks')
    for z in range(Z0 + 2, Z1 - 1):
        v.set(CX, 1, z, 'oak_planks'); v.set(CX - 1, 1, z, 'spruce_planks'); v.set(CX + 1, 1, z, 'spruce_planks')
    for z in range(Z0 + 9, Z1 - 2, 8):                                         # muelles laterales
        for s in (-1, 1):
            for k in range(2, 9): v.set(CX + s * k, 1, z, 'oak_planks'); v.set(CX + s * k, 1, z + 1, 'oak_planks')
            v.fill(CX + s * 8, 2, z, CX + s * 8, 3, z, 'oak_fence'); v.set(CX + s * 8, 4, z, 'lantern')
    for z in range(Z0 + 2, Z1 - 1, 3):                                         # postes del muelle central
        for s in (-2, 2): v.fill(CX + s, 2, z, CX + s, 3, z, 'oak_fence')
    for x in range(X0 + 4, X1 - 3, 4):                                         # cuerdas de farolillos
        for z in (Z0 + 8, Z0 + 20):
            for k in range(1, 4): v.set(x, YS + 6 - k, z, 'chain[axis=y]') if v.get(x, YS + 6, z) != 'air' else None
    for z in range(Z0 + 5, Z1 - 2, 6):
        for x in (CX - 9, CX + 9):
            top = YS + int(math.sqrt(max(0, 1 - ((x - CX) / (R - 1)) ** 2)) * (R - 1) * 0.95)
            for k in range(1, 3): v.set(x, top - k, z, 'chain[axis=y]')
            v.set(x, top - 3, z, 'lantern[hanging=true]')
    # ---- gradas de madera a los lados (suben hacia las paredes)
    for z in range(Z0 + 6, Z1 - 6):
        for i in range(3):
            xl, xr = X0 + 3 - i, X1 - 3 + i
            if i: v.fill(xl, 1, z, xl, i, z, 'oak_planks'); v.fill(xr, 1, z, xr, i, z, 'oak_planks')
            v.stairs(xl, 1 + i, z, 'oak', 'east'); v.stairs(xr, 1 + i, z, 'oak', 'west')
    for z in range(Z1 - 8, Z1):
        v.fill(CX - 5, 1, z, CX + 5, 1, z, 'spruce_planks')
    v.fill(CX - 4, 2, Z1 - 6, CX + 4, 2, Z1 - 1, 'dark_oak_planks')
    for x in range(CX - 4, CX + 5): v.stairs(x, 2, Z1 - 7, 'dark_oak', 'north')
    for dx in (-5, 5): v.fill(CX + dx, 2, Z1 - 6, CX + dx, 5, Z1 - 6, 'oak_fence'); v.set(CX + dx, 6, Z1 - 6, 'lantern')
    # ola de fondo (pixel art de la pared interior)
    for x in range(CX - 8, CX + 9):
        h = 5 + int(3 * math.sin((x - CX) * 0.7)) + 2
        for y in range(4, 4 + h): v.set(x, y, Z1, 'blue_concrete' if y < 4 + h - 1 else 'white_concrete')
    return v, [(CX, 3, Z1 - 4, 180, 'emipokemon desafios galar agua')]
