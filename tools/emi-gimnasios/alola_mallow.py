"""Prueba de Mallow (Planta) en la Selva Frondosa: arbol gigante con casa en las ramas, escalera de caracol, puentes de cuerda y faroles."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 43, 42, 44
CX, CZ = 21, 22
YP = 15                      # altura de la plataforma principal


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(341)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['grass_block', 'grass_block', 'podzol', 'moss_block']))
    for z in range(0, 12):
        for x in range(CX - 2, CX + 3): v.set(x, 0, z, 'dirt_path')
    # ---- el arbol: tronco de 5x5 con raices abiertas y copa enorme
    v.fill(CX - 2, 1, CZ - 2, CX + 2, 30, CZ + 2, 'jungle_log[axis=y]')
    for (dx, dz) in ((-3, -3), (3, -3), (-3, 3), (3, 3), (0, -4), (0, 4), (-4, 0), (4, 0)):
        v.fill(CX + dx, 1, CZ + dz, CX + dx, 3, CZ + dz, 'jungle_log[axis=y]'); v.set(CX + dx // 2 * 1, 1, CZ + dz // 2 * 1, 'jungle_log[axis=y]')
    for _ in range(110):
        cx = CX + rnd.randint(-13, 13); cy = rnd.randint(26, 36); cz = CZ + rnd.randint(-11, 11)
        copa(v, cx, cy, cz, rnd.randint(3, 5), rnd.randint(2, 3), rnd.randint(3, 5), rnd.choice(['jungle_leaves'] * 4 + ['oak_leaves']), rnd)
    # ramas gruesas bajo la copa
    for (dx, dz) in ((-10, 0), (10, 2), (0, -10), (1, 10), (-7, 7), (8, -8)):
        v.linea(CX, 27, CZ, CX + dx, 29, CZ + dz, 'jungle_log[axis=y]'); v.linea(CX, 26, CZ, CX + dx, 28, CZ + dz, 'jungle_log[axis=y]')
    # ---- plataforma circular de madera alrededor del tronco con barandilla
    disco_f(v, CX, YP, CZ, 10, anillos([(10, 'jungle_planks')]))
    disco_f(v, CX, YP - 1, CZ, 10, anillos([(10, 'stripped_jungle_log[axis=y]')]))
    v.fill(CX - 2, YP, CZ - 2, CX + 2, YP, CZ + 2, 'jungle_log[axis=y]')
    for k in range(40):
        a = k / 40 * 2 * math.pi
        x, z = round(CX + math.cos(a) * 10), round(CZ + math.sin(a) * 10)
        if abs(x - CX) <= 2 and z < CZ - 7: continue
        v.set(x, YP + 1, z, 'jungle_fence')
        if k % 5 == 0: v.fill(x, YP + 1, z, x, YP + 3, z, 'jungle_fence'); v.set(x, YP + 4, z, 'lantern')
    # ---- escalera de caracol alrededor del tronco (cuatro tramos)
    peld = []
    ring = [(x, CZ - 3) for x in range(CX - 3, CX + 4)] + [(CX + 3, z) for z in range(CZ - 2, CZ + 4)] + [(x, CZ + 3) for x in range(CX + 2, CX - 4, -1)] + [(CX - 3, z) for z in range(CZ + 2, CZ - 3, -1)]
    for i in range(1, YP):
        x, z = ring[(i + 2) % len(ring)]
        v.set(x, i, z, 'jungle_planks'); v.fill(x, 1, z, x, i - 1, z, 'jungle_log[axis=y]') if i > 1 else None
    # escalon de entrada y camino de acceso hasta la escalera
    for z in range(10, CZ - 4):
        for x in range(CX - 1, CX + 2): v.set(x, 0, z, 'dirt_path')
    # ---- puentes de cuerda a dos plataformas satelite con cabañas
    for (tx, tz) in [(CX - 17, CZ + 4), (CX + 17, CZ + 3)]:
        disco_f(v, tx, YP - 1, tz, 4, anillos([(4, 'jungle_planks')]))
        v.fill(tx - 1, YP, tz - 1, tx + 1, YP + 3, tz + 1, 'bamboo_planks'); v.aire(tx, YP, tz, tx, YP + 2, tz)
        techo_bambu(v, tx - 1, tx + 1, tz - 1, tz + 1, YP + 4, 1)
        for (px, pz) in ((tx - 3, tz - 3), (tx + 3, tz + 3), (tx - 3, tz + 3), (tx + 3, tz - 3)): v.fill(px, 1, pz, px, YP - 2, pz, 'jungle_log[axis=y]')
        sx = 1 if tx > CX else -1
        for i in range(0, 14):
            x = CX + sx * (10 + i)
            if abs(x - tx) <= 4: break
            zz = CZ + (tz - CZ) * i // 14
            v.set(x, YP, zz, 'jungle_slab[type=bottom]')
            v.set(x, YP + 1, zz - 1, 'jungle_fence'); v.set(x, YP + 1, zz + 1, 'jungle_fence')
    # ---- vegetacion del suelo y palmeras, linternas colgando de la copa
    for (x, z) in [(4, 4), (38, 5), (3, 20), (39, 22), (5, 38), (37, 38), (12, 6), (30, 6)]: palmera(v, x, z, rnd.randint(5, 8), rnd)
    flores_tropicales(v, 1, W - 2, 1, L - 2, rnd, 0.15)
    for k in range(8):
        a = k / 8 * 2 * math.pi + 0.3
        x, z = round(CX + math.cos(a) * 7), round(CZ + math.sin(a) * 7)
        for y in range(YP + 14, YP + 4, -1):
            if v.get(x, y, z) != 'air':
                for dy in range(1, 5): v.set(x, y - dy, z, 'chain[axis=y]')
                v.set(x, y - 5, z, 'lantern[hanging=true]'); break
    return v, [(CX, YP + 1, CZ + 7, 180, 'emipokemon desafios alola captain_mallow')]
