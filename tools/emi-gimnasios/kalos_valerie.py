"""Gimnasio de Laverre (Valerie, Hada): castillo de cuento rosa y blanco entre setas gigantes, con salon de baile."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 41, 38, 44
CX = 20
X0, X1 = 8, 32
Z0, Z1 = 11, 40


def corazon(v, cx, cy, z, s, bloque):
    for x in range(int(cx - 1.4 * s), int(cx + 1.4 * s) + 2):
        for y in range(int(cy - 1.2 * s), int(cy + 1.4 * s) + 2):
            u = (x - cx) / s; w = (y - cy) / s
            if (u * u + w * w - 1) ** 3 - u * u * w ** 3 <= 0: v.set(x, y, z, bloque)


def torre(v, cx, cz, r, h):
    v.cilindro(cx, 1, h, cz, r, 'white_concrete')
    v.disco(cx, 0, cz, r, 'smooth_quartz')
    v.anillo_circ(cx, h - 2, cz, r, 'pink_concrete', 1.0)
    v.anillo_circ(cx, 6, cz, r, 'pink_concrete', 1.0)
    for y in (9, 15, 21):
        if y < h - 3:
            for (dx, dz) in ((r, 0), (-r, 0), (0, r), (0, -r)): v.set(cx + dx, y, cz + dz, 'pink_stained_glass'); v.set(cx + dx, y + 1, cz + dz, 'pink_stained_glass')
    cono(v, cx, h + 1, cz, r + 2, r * 3 + 2, 'pink_concrete')
    top = h + 1 + r * 3 + 2
    v.fill(cx, top, cz, cx, top + 2, cz, 'oak_fence')
    v.set(cx + 1, top + 2, cz, 'pink_wool'); v.set(cx + 1, top + 1, cz, 'pink_wool'); v.set(cx + 2, top + 2, cz, 'pink_wool')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(91)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'grass_block')
    for z in range(0, Z0):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'smooth_quartz' if (x + z) % 2 else 'pink_concrete')
    # ---- bosque de setas gigantes alrededor
    for (x, z, alto, r) in [(3, 3, 8, 4), (36, 4, 10, 5), (6, 12, 6, 3), (34, 14, 7, 3), (3, 24, 9, 4), (37, 26, 8, 4), (5, 36, 10, 5), (35, 38, 7, 3), (12, 4, 4, 3), (28, 5, 5, 3)]:
        seta(v, x, 1, z, alto, r, 'mushroom_stem', rnd.choice(['red_mushroom_block', 'red_mushroom_block', 'brown_mushroom_block']), 'mushroom_stem', rnd)
    for x in range(W):
        for z in range(L):
            if v.get(x, 1, z) == 'air' and abs(x - CX) > 4 and rnd.random() < 0.15 and not (X0 - 3 <= x <= X1 + 3 and Z0 - 3 <= z <= Z1 + 3):
                v.set(x, 1, z, rnd.choice(['pink_tulip', 'allium', 'lily_of_the_valley', 'cornflower', 'azure_bluet']))
    # ---- cuerpo del castillo
    v.box(X0, 1, Z0, X1, 20, Z1, 'white_concrete', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 19, Z1 - 1)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'smooth_quartz')
    for y in (1, 2): v.anillo(X0, Z0, X1, Z1, y, 'pink_terracotta')
    v.anillo(X0, Z0, X1, Z1, 10, 'pink_concrete'); v.anillo(X0, Z0, X1, Z1, 20, 'pink_concrete')
    for k in range(X1 - X0 + 1):                                                         # almenas
        if k % 2 == 0:
            v.set(X0 + k, 21, Z0, 'white_concrete'); v.set(X0 + k, 21, Z1, 'white_concrete')
    for k in range(Z1 - Z0 + 1):
        if k % 2 == 0: v.set(X0, 21, Z0 + k, 'white_concrete'); v.set(X1, 21, Z0 + k, 'white_concrete')
    for (x, z) in [(X0, Z0), (X1, Z0), (X0, Z1), (X1, Z1)]: torre(v, x, z, 4, 26)
    # ventanas de arco con cristal rosa
    for z in range(Z0 + 7, Z1 - 5, 6):
        for y in (4, 5, 6, 13, 14, 15):
            for x in (X0, X1): v.set(x, y, z, 'pink_stained_glass'); v.set(x, y, z + 1, 'pink_stained_glass')
    # ---- fachada: portalon con corazon y balcon
    hueco_puerta(v, CX, Z0, 7, 9, 'pink_concrete', y0=1, prof=1)
    for dx in (-4, 4):
        for y in range(1, 11): v.set(CX + dx, y, Z0, 'quartz_pillar[axis=y]')
    corazon(v, CX, 16, Z0, 3.2, 'pink_concrete')
    v.fill(CX - 4, 11, Z0 - 1, CX + 4, 11, Z0 - 1, 'smooth_quartz_slab[type=bottom]')
    for x in range(CX - 4, CX + 5): v.set(x, 12, Z0 - 1, 'iron_bars')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, Z0 - 1, 'quartz', 'north') if abs(x - CX) <= 2 else None
    for dx in (-5, 5):
        v.fill(CX + dx, 1, Z0 - 2, CX + dx, 5, Z0 - 2, 'quartz_pillar[axis=y]'); v.set(CX + dx, 6, Z0 - 2, 'pink_concrete')
        v.set(CX + dx, 7, Z0 - 2, 'lantern')
    # tejado central bajo con cresta rosa
    for k in range(6):
        v.fill(X0 + 3 + k, 21 + k, Z0 + 3, X1 - 3 - k, 21 + k, Z1 - 3, 'pink_concrete') if X0 + 3 + k < X1 - 3 - k else None
    # ---- interior: salon de baile
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1):
            v.set(x, 1, z, 'white_concrete' if (x + z) % 2 else 'pink_concrete')
    for z in range(Z0 + 1, Z1):
        for x in range(CX - 2, CX + 3): v.set(x, 1, z, 'pink_wool' if abs(x - CX) < 2 else 'magenta_concrete')
    for z in range(Z0 + 4, Z1, 5):
        for x in (X0 + 3, X1 - 3):
            v.fill(x, 2, z, x, 9, z, 'quartz_pillar[axis=y]'); v.set(x, 10, z, 'chiseled_quartz_block'); v.set(x, 2, z, 'pink_concrete')
    for z in (Z0 + 8, Z0 + 16, Z0 + 24):                                                    # candelabros
        for x in (CX - 6, CX, CX + 6):
            for k in range(3): v.set(x, 19 - k, z, 'chain[axis=y]')
            v.set(x, 16, z, 'sea_lantern')
            for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)): v.set(x + dx, 16, z + dz, 'pink_stained_glass')
    for z in range(Z0 + 3, Z1 - 1, 5):                                                       # cortinas rosas
        for x in (X0 + 1, X1 - 1):
            v.fill(x, 4, z, x, 8, z, 'pink_wool'); v.fill(x, 4, z + 2, x, 8, z + 2, 'pink_wool')
    for (x, z) in [(X0 + 2, Z0 + 3), (X1 - 2, Z0 + 3), (X0 + 2, Z1 - 3), (X1 - 2, Z1 - 3), (X0 + 2, 25), (X1 - 2, 25)]:   # rincones de setas
        v.set(x, 1, z, 'podzol'); v.set(x, 2, z, rnd.choice(['red_mushroom', 'brown_mushroom']))
    # ---- estrado del lider con trono y corazon de fondo
    disco_f(v, CX, 2, Z1 - 5, 6, anillos([(6, 'white_concrete')]))
    disco_f(v, CX, 3, Z1 - 5, 4, anillos([(4, 'pink_concrete')]))
    for x in range(CX - 7, CX + 8): v.stairs(x, 2, Z1 - 11, 'quartz', 'north') if abs(x - CX) <= 3 else None
    corazon(v, CX, 11, Z1, 4.5, 'pink_concrete')
    for dx in (-3, 3):
        v.fill(CX + dx, 4, Z1 - 1, CX + dx, 8, Z1 - 1, 'quartz_pillar[axis=y]'); v.set(CX + dx, 9, Z1 - 1, 'chiseled_quartz_block')
    return v, [(CX, 4, Z1 - 5, 180, 'emipokemon desafios kalos hada')]
