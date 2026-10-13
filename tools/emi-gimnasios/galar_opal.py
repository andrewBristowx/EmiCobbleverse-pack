"""Estadio de Ballonlea (Opal, Hada): mansion tudor de vigas negras con torre de sombrero de bruja, bola de cristal gigante y setas rosas."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 45, 42, 48
CX = 22
X0, X1 = 11, 33
Z0, Z1 = 14, 43


def sombrero(v, cx, cz, r, y0, alto):
    """cono de bruja con la punta doblada hacia un lado"""
    for k in range(alto):
        rr = max(0.5, (r + 1) * (1 - k / alto))
        off = int((k / alto) ** 2.2 * 5)
        for x in range(int(cx - rr) - 1, int(cx + rr) + 2):
            for z in range(int(cz - rr) - 1, int(cz + rr) + 2):
                if math.hypot(x - cx, z - cz) <= rr + 0.2: v.set(x + off, y0 + k, z, 'purple_concrete' if k % 6 != 5 else 'magenta_concrete')
    v.set(cx + 5, y0 + alto, cz, 'end_rod[facing=up]')
    v.disco(cx, y0 - 1, cz, r + 3, 'magenta_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(171)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'grass_block')
    for x in range(W):
        for z in range(L):
            if rnd.random() < 0.2: v.set(x, 0, z, 'podzol')
    for z in range(0, Z0):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'stone_bricks' if (x + z) % 2 else 'mossy_stone_bricks')
    # ---- bosque de setas rosas y moradas
    for (x, z, alto, r) in [(4, 4, 9, 4), (40, 5, 11, 5), (5, 20, 7, 3), (39, 22, 8, 4), (4, 38, 10, 4), (40, 40, 9, 4), (14, 4, 5, 3), (30, 4, 6, 3)]:
        seta(v, x, 1, z, alto, r, 'white_concrete', rnd.choice(['magenta_concrete', 'purple_concrete', 'pink_concrete']), 'white_concrete', rnd)
    for _ in range(50):
        x, z = rnd.randint(1, W - 2), rnd.randint(1, Z0 - 3)
        if v.get(x, 1, z) == 'air' and abs(x - CX) > 5 and v.get(x, 0, z) == 'podzol': v.set(x, 1, z, rnd.choice(['red_mushroom', 'brown_mushroom']))
    # ---- casa tudor: muros blancos con vigas negras
    v.box(X0, 1, Z0, X1, 12, Z1, 'white_concrete', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 12, Z1 - 1)
    v.fill(X0, 1, Z0, X1, 3, Z1, 'stone_bricks'); v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 12, Z1 - 1)
    for x in range(X0, X1 + 1, 4):
        for (zz) in (Z0, Z1): v.fill(x, 4, zz, x, 12, zz, 'dark_oak_log[axis=y]')
    for z in range(Z0, Z1 + 1, 5):
        for xx in (X0, X1): v.fill(xx, 4, z, xx, 12, z, 'dark_oak_log[axis=y]')
    for y in (4, 8, 12):
        v.anillo(X0, Z0, X1, Z1, y, 'dark_oak_log[axis=x]')
    for x in range(X0, X1, 4):                                                                       # diagonales de la fachada
        for k in range(4): v.set(x + k, 5 + k, Z0, 'dark_oak_log[axis=y]') if k < 3 else None
    for z in range(Z0 + 2, Z1 - 1, 5):
        for y in (6, 7, 10): v.set(X0, y, z, 'glass_pane'); v.set(X0, y, z + 1, 'glass_pane'); v.set(X1, y, z, 'glass_pane'); v.set(X1, y, z + 1, 'glass_pane')
    # tejado a dos aguas morado con buhardillas
    cum = techo_dos_aguas(v, X0 - 1, X1 + 1, Z0 - 1, Z1 + 1, 13, 'purple_concrete', 'purpur', 'z')
    v.fill(X0 + 1, 12, Z0 + 1, X1 - 1, 12, Z1 - 1, 'dark_oak_planks')
    for k in range(0, (X1 - X0 + 3) // 2 + 1):
        for zz in (Z0, Z1): v.fill(CX - (X1 - X0) // 2 - 1 + k, 13 + k, zz, CX + (X1 - X0) // 2 + 1 - k, 13 + k, zz, 'white_concrete')
    for k in range(5):
        for zz in (Z0, Z1): v.set(CX - 4 + k, 14 + k, zz, 'dark_oak_log[axis=y]'); v.set(CX + 4 - k, 14 + k, zz, 'dark_oak_log[axis=y]')
    # ---- bola de cristal sobre el tejado
    cz2 = (Z0 + Z1) // 2
    v.fill(CX - 2, cum + 1, cz2 - 2, CX + 2, cum + 1, cz2 + 2, 'amethyst_block')
    v.esfera(CX, cum + 6, cz2, 5, 'light_blue_stained_glass', hueco=True)
    v.esfera(CX, cum + 6, cz2, 2, 'sea_lantern')
    for a in range(0, 360, 45):
        x = round(CX + math.cos(math.radians(a)) * 6); z = round(cz2 + math.sin(math.radians(a)) * 6)
        if v.get(x, cum, z) != 'air': v.set(x, cum + 1, z, 'amethyst_cluster[facing=up,waterlogged=false]')
    # ---- torres con sombrero de bruja
    for (x, z, r, h, hat) in [(X0 - 1, Z0 - 1, 4, 20, 16), (X1 + 1, Z1 + 1, 3, 17, 13)]:
        v.cilindro(x, 1, h, z, r, 'white_concrete'); v.disco(x, 0, z, r, 'stone_bricks')
        for y in range(4, h, 4): v.anillo_circ(x, y, z, r, 'dark_oak_planks', 1.0)
        for y in (8, 14):
            for (dx, dz) in ((r, 0), (-r, 0), (0, r), (0, -r)): v.set(x + dx, y, z + dz, 'pink_stained_glass'); v.set(x + dx, y + 1, z + dz, 'pink_stained_glass')
        sombrero(v, x, z, r, h + 1, hat)
    # ---- puerta
    hueco_puerta(v, CX, Z0, 5, 7, 'dark_oak_log[axis=y]', y0=2)
    v.fill(CX - 4, 2, Z0 - 2, CX + 4, 2, Z0 - 1, 'stone_bricks')
    for x in range(CX - 3, CX + 4): v.stairs(x, 1, Z0 - 2, 'stone_brick', 'north') if abs(x - CX) <= 2 else None
    for dx in (-3, 3): v.fill(CX + dx, 3, Z0 - 2, CX + dx, 6, Z0 - 2, 'dark_oak_fence'); v.set(CX + dx, 7, Z0 - 2, 'lantern')
    # ---- interior: salon con suelo de ajedrez, cortinas, estanterias y arana de luces
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1): v.set(x, 1, z, 'black_concrete' if (x + z) % 2 else 'white_concrete')
    for z in range(Z0 + 1, Z1):
        for x in range(CX - 2, CX + 3): v.set(x, 1, z, 'purple_concrete' if abs(x - CX) < 2 else 'magenta_concrete')
    for z in range(Z0 + 3, Z1 - 5, 4):
        for xx in (X0 + 1, X1 - 1): v.fill(xx, 2, z, xx, 7, z + 1, 'bookshelf')
    for z in range(Z0 + 4, Z1, 5):
        for xx in (X0 + 1, X1 - 1):
            pass
    for z in range(Z0 + 5, Z1 - 4, 8):
        for dx in (-5, 5):
            for k in range(1, 5): v.set(CX + dx, 12 - k + 1, z, 'chain[axis=y]')
            v.set(CX + dx, 8, z, 'sea_lantern')
            for (ax, az) in ((1, 0), (-1, 0), (0, 1), (0, -1)): pass
        v.set(CX, 8, z, 'end_rod[facing=down]')
    for (x, z) in [(X0 + 2, Z0 + 2), (X1 - 2, Z0 + 2), (X0 + 2, Z1 - 2), (X1 - 2, Z1 - 2)]:
        v.fill(x, 2, z, x, 5, z, 'dark_oak_fence'); v.set(x, 6, z, 'lantern')
    # mesa redonda con bola de cristal y asientos
    disco_f(v, CX, 2, 24, 3, anillos([(3, 'dark_oak_planks')]))
    for a in range(0, 360, 60):
        x = round(CX + math.cos(math.radians(a)) * 5); z = round(24 + math.sin(math.radians(a)) * 5)
        v.set(x, 2, z, 'purple_concrete')
    # estrado del lider con trono morado y arco de luz
    v.fill(CX - 6, 2, Z1 - 7, CX + 6, 2, Z1 - 1, 'amethyst_block'); v.fill(CX - 4, 3, Z1 - 6, CX + 4, 3, Z1 - 1, 'purple_concrete')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 8, 'purpur', 'north')
    for x in range(CX - 4, CX + 5): v.stairs(x, 3, Z1 - 7, 'purpur', 'north')
    for dx in range(-3, 4):
        h = 6 - abs(dx) // 2
        for y in range(5, 5 + h): v.set(CX + dx, y, Z1 - 1, 'purple_concrete' if abs(dx) > 1 else 'magenta_concrete')
    elipse_xy(v, CX, 10, Z1, 2.5, 2.5, lambda d, a: 'light_blue_stained_glass' if d > 0.4 else 'sea_lantern')
    return v, [(CX, 4, Z1 - 4, 180, 'emipokemon desafios galar hada')]
