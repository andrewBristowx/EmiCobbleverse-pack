"""Gimnasio de Anistar (Olympia, Psiquico): templo-cupula estrellado con un reloj de sol gigante delante."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 37, 36, 42
CX, CZ = 18, 27
R = 15


def luna(v, cx, cy, z, r, bloque):
    """creciente de luna en el plano XY (mira al +x)"""
    for x in range(cx - r, cx + r + 1):
        for y in range(cy - r, cy + r + 1):
            if math.hypot(x - cx, y - cy) <= r + 0.3 and math.hypot(x - (cx + r * 0.45), y - cy) > r * 0.9: v.set(x, y, z, bloque)


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(101)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_basalt')
    # ---- plaza con el reloj de sol: anillos, marcas horarias y gnomon
    cs = 5
    disco_f(v, CX, 0, cs, 8, anillos([(1.5, 'amethyst_block'), (6, 'smooth_quartz'), (7, 'purple_concrete'), (8, 'smooth_quartz')]))
    for k in range(24):
        a = k * math.pi / 12
        x = round(CX + math.cos(a) * 6.5); z = round(cs + math.sin(a) * 6.5)
        v.set(x, 0, z, 'black_concrete')
    for z in range(2, 9):
        h = 1 + int((z - 2) * 1.5)
        v.fill(CX, 1, z, CX, h, z, 'quartz_block'); v.set(CX, h + 1, z, 'quartz_slab[type=bottom]') if z == 8 else None
    v.fill(CX, 1, 1, CX, 2, 1, 'chiseled_quartz_block')
    for z in range(0, CZ - R + 1):
        for x in range(CX - 2, CX + 3): v.set(x, 0, z, 'smooth_quartz' if (x + z) % 2 else 'polished_blackstone') if z > 8 else None
    # ---- templo: base de cuarzo, dos bandas de amatista y cupula negra con estrellas
    v.cilindro(CX, 1, 9, CZ, R, 'quartz_bricks')
    v.disco(CX, 0, CZ, R, 'polished_blackstone')
    v.anillo_circ(CX, 9, CZ, R, 'amethyst_block', 1.3)
    v.anillo_circ(CX, 4, CZ, R, 'purple_concrete', 1.0)
    for k in range(12):                                                                   # pilastras
        a = k / 12 * 2 * math.pi + 0.13
        x = round(CX + math.cos(a) * (R - 0.4)); z = round(CZ + math.sin(a) * (R - 0.4))
        v.fill(x, 1, z, x, 9, z, 'quartz_pillar[axis=y]'); v.set(x, 9, z, 'chiseled_quartz_block')
    v.cupula(CX, 10, CZ, R, 'black_concrete', hueco=True, ry=17)
    for (x, y, z), b in list(v.g.items()):                                                   # estrellas e hilos de cristal en la cascara
        if b == 'black_concrete' and y > 10:
            q = rnd.random()
            if q < 0.035: v.g[(x, y, z)] = 'sea_lantern'
            elif q < 0.08: v.g[(x, y, z)] = 'purple_concrete'
    for lat in (13, 17, 21):
        for i in range(0, 721):
            a = i / 720 * 2 * math.pi
            rr = R * math.sqrt(max(0, 1 - ((lat - 10) / 17) ** 2))
            x, z = round(CX + math.cos(a) * rr), round(CZ + math.sin(a) * rr)
            if v.get(x, lat, z) != 'air' and 'sea' not in v.get(x, lat, z): v.set(x, lat, z, 'amethyst_block')
    # aguja de cristal en la cima
    v.fill(CX, 26, CZ, CX, 33, CZ, 'amethyst_block'); v.fill(CX - 1, 26, CZ - 1, CX + 1, 26, CZ + 1, 'amethyst_block')
    v.set(CX, 34, CZ, 'end_rod[facing=up]')
    # ---- entrada: portico de cuarzo con creciente de luna
    zf = CZ - R
    hueco_puerta(v, CX, zf, 5, 7, 'quartz_pillar[axis=y]', y0=1, prof=2)
    v.fill(CX - 4, 8, zf - 1, CX + 4, 8, zf - 1, 'quartz_block'); v.fill(CX - 3, 9, zf - 1, CX + 3, 9, zf - 1, 'amethyst_block')
    for dx in (-4, 4): v.fill(CX + dx, 1, zf - 1, CX + dx, 7, zf - 1, 'quartz_pillar[axis=y]')
    luna(v, CX, 13, zf - 1, 3, 'white_concrete')
    # pilares-faro a los lados de la plaza con cristales
    for (x, z) in [(CX - 11, 4), (CX + 11, 4), (CX - 15, 15), (CX + 15, 15)]:
        v.fill(x, 1, z, x, 6, z, 'purple_concrete'); v.set(x, 7, z, 'chiseled_quartz_block'); v.set(x, 8, z, 'amethyst_block')
        v.set(x, 9, z, 'amethyst_cluster[facing=up,waterlogged=false]'); v.set(x, 0, z, 'smooth_quartz')
    # ---- interior: suelo con anillos, constelaciones y pilares de cristal
    disco_f(v, CX, 1, CZ, 14, anillos([(2, 'amethyst_block'), (4, 'black_concrete'), (5, 'purple_concrete'), (8, 'black_concrete'), (9, 'smooth_quartz'), (13, 'black_concrete'), (14, 'purple_concrete')]))
    for _ in range(70):
        a = rnd.random() * 2 * math.pi; d = rnd.random() * 13
        x, z = round(CX + math.cos(a) * d), round(CZ + math.sin(a) * d)
        if v.get(x, 1, z) == 'black_concrete': v.set(x, 1, z, 'white_concrete')
    for k in range(6):                                                                      # pilares con cristal
        a = k / 6 * 2 * math.pi + 0.5
        x, z = round(CX + math.cos(a) * 9.5), round(CZ + math.sin(a) * 9.5)
        if math.hypot(x - CX, z - (CZ + 9)) < 4: continue
        v.fill(x, 2, z, x, 6, z, 'purple_concrete'); v.set(x, 7, z, 'amethyst_block'); v.set(x, 8, z, 'amethyst_cluster[facing=up,waterlogged=false]')
        v.set(x, 1, z, 'smooth_quartz')
    for k in range(5):                                                                      # esferas flotantes de luz
        a = k / 5 * 2 * math.pi + 1.0
        x, z = round(CX + math.cos(a) * 5), round(CZ + math.sin(a) * 5)
        v.esfera(x, 12 + (k % 2) * 3, z, 1, 'sea_lantern')
    # ---- estrado del lider con creciente de luna a su espalda
    disco_f(v, CX, 2, CZ + 8, 5, anillos([(5, 'smooth_quartz')]))
    disco_f(v, CX, 3, CZ + 8, 3, anillos([(3, 'amethyst_block')]))
    for x in range(CX - 5, CX + 6): v.stairs(x, 2, CZ + 3, 'quartz', 'north') if abs(x - CX) <= 2 else None
    luna(v, CX, 13, CZ + R - 1, 5, 'white_concrete')
    for dx in (-5, 5): v.fill(CX + dx, 2, CZ + 12, CX + dx, 7, CZ + 12, 'quartz_pillar[axis=y]'); v.set(CX + dx, 8, CZ + 12, 'sea_lantern')
    return v, [(CX, 4, CZ + 8, 180, 'emipokemon desafios kalos psiquica')]
