"""Estadio de Spikemuth (Piers, Siniestro): sala de conciertos punk negra con neones rosas, una calavera gigante, altavoces y un escenario con bateria."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 43, 36, 46
CX = 21
X0, X1 = 7, 35
Z0, Z1 = 13, 43
NEON, NEON2 = 'pink_concrete', 'magenta_concrete'


def calavera(v, cx, cy, z, s=1.0, hueso='white_concrete', fondo=None):
    """calavera de frente en el plano XY: craneo, mandibula con dientes, ojos y nariz"""
    elipse_xy(v, cx, cy + 2 * s, z, 7 * s, 6.2 * s, lambda d, a: hueso)
    v.fill(cx - int(4 * s), int(cy - 4 * s), z, cx + int(4 * s), int(cy - 1 * s), z, hueso)
    for k in range(-int(3 * s), int(3 * s) + 1, 2): v.fill(cx + k, int(cy - 4 * s), z, cx + k, int(cy - 3 * s), z, 'light_gray_concrete')
    for sx in (-1, 1):
        elipse_xy(v, cx + sx * 3.2 * s, cy + 2 * s, z, 2.2 * s, 2.4 * s, lambda d, a: 'black_concrete')
    v.set(cx, int(cy + 0 * s), z, 'black_concrete'); v.set(cx - 1, int(cy - 1 * s), z, 'black_concrete'); v.set(cx + 1, int(cy - 1 * s), z, 'black_concrete')


def altavoz(v, x, z, alto, ancho=3):
    v.fill(x, 1, z, x + ancho - 1, alto, z + ancho - 1, 'black_concrete')
    for (dx, dz) in ((0, 0), (ancho - 1, 0)): pass
    for y in range(2, alto, 3):
        v.set(x + 1, y, z - 1, 'light_gray_concrete'); v.set(x + 1, y + 1, z - 1, 'light_gray_concrete'); v.set(x, y, z - 1, 'gray_concrete'); v.set(x + 2, y, z - 1, 'gray_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(191)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'gray_concrete')
    for x in range(W):
        for z in range(L):
            if rnd.random() < 0.3: v.set(x, 0, z, rnd.choice(['black_concrete', 'polished_blackstone', 'cracked_stone_bricks']))
    for z in range(0, Z0):
        for x in range(CX - 4, CX + 5): v.set(x, 0, z, 'black_concrete' if (x // 2 + z // 2) % 2 else 'purple_concrete')
    # ---- valla con alambre de puas y altavoces gigantes en las esquinas
    for x in range(1, W - 1, 2):
        if abs(x - CX) > 6: v.fill(x, 1, 2, x, 3, 2, 'iron_bars'); v.set(x, 4, 2, 'cobweb')
    for z in range(2, 12): v.fill(1, 1, z, 1, 3, z, 'iron_bars'); v.fill(W - 2, 1, z, W - 2, 3, z, 'iron_bars')
    altavoz(v, 2, 6, 9); altavoz(v, W - 5, 6, 9)
    for (x, z) in [(CX - 7, 6), (CX + 7, 6)]:                                  # barreras de cola y alfombra roja
        for k in range(0, 6): v.fill(x, 1, z + k, x, 2, z + k, 'iron_bars') if k % 2 == 0 else None
    # ---- edificio: bloque negro con zocalo, grafitis y neones
    v.box(X0, 1, Z0, X1, 18, Z1, 'black_concrete', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 18, Z1 - 1)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'black_concrete')
    for y in (1, 2, 3): v.anillo(X0, Z0, X1, Z1, y, 'polished_blackstone_bricks')
    for y in (8, 14): v.anillo(X0, Z0, X1, Z1, y, NEON)
    v.anillo(X0, Z0, X1, Z1, 18, NEON2)
    for _ in range(90):                                                          # grafitis por las paredes laterales
        z = rnd.randint(Z0 + 1, Z1 - 1); y = rnd.randint(4, 13)
        col = rnd.choice(['pink_concrete', 'purple_concrete', 'lime_concrete', 'yellow_concrete', 'cyan_concrete', 'white_concrete'])
        for d in range(rnd.randint(1, 3)): v.set(X0, y, z + d if z + d < Z1 else z, col); v.set(X1, y, z - d if z - d > Z0 else z, col)
    for z in range(Z0 + 3, Z1 - 1, 6):
        for y in (10, 11, 12): v.set(X0, y, z, 'purple_stained_glass'); v.set(X1, y, z, 'purple_stained_glass')
    # cartel luminoso: calavera gigante en la fachada
    v.fill(CX - 10, 5, Z0, CX + 10, 17, Z0, 'black_concrete')
    calavera(v, CX, 10, Z0, 1.0, 'white_concrete')
    for (dx, k) in ((-9, 13), (9, 13)):
        v.fill(CX + dx, 5, Z0, CX + dx, 5 + k - 1, Z0, NEON)
    for x in range(CX - 10, CX + 11): v.set(x, 5, Z0, NEON); v.set(x, 17, Z0, NEON)
    hueco_puerta(v, CX, Z0, 5, 4, NEON2, y0=2, prof=1)
    v.fill(CX - 3, 6, Z0 - 1, CX + 3, 6, Z0 - 1, 'purple_concrete')
    for x in range(CX - 3, CX + 4): v.set(x, 5, Z0 - 1, 'iron_bars')
    # techo con focos y cartel trasero
    v.fill(X0, 19, Z0, X1, 19, Z1, 'polished_blackstone_bricks')
    for x in range(X0 + 3, X1 - 2, 4):
        for z in (Z0 + 3, Z1 - 3): v.fill(x, 20, z, x, 21, z, 'iron_bars'); v.set(x, 22, z, 'sea_lantern')
    for x in range(X0, X1 + 1, 2): v.set(x, 20, Z0, 'iron_bars'); v.set(x, 20, Z1, 'iron_bars')
    # ---- interior: pista con franjas, barrera de seguridad y escenario
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1):
            if (z - Z0) % 6 == 0: v.set(x, 1, z, 'purple_concrete')
            elif rnd.random() < 0.06: v.set(x, 1, z, 'gravel')
    for x in range(X0 + 1, X1): v.set(x, 2, 30, 'iron_bars'); v.set(x, 3, 30, 'iron_bars')
    v.aire(CX - 1, 2, 30, CX + 1, 3, 30)
    for z in range(Z0 + 3, Z1 - 8, 4):                                          # cerchas con focos
        v.fill(X0 + 1, 17, z, X1 - 1, 17, z, 'iron_bars')
        for x in range(X0 + 4, X1 - 3, 6):
            v.set(x, 16, z, 'shroomlight'); v.set(x, 15, z, 'iron_bars')
    for z in range(Z0 + 4, Z1 - 10, 8):                                          # neones de pared
        for x in (X0 + 1, X1 - 1): v.fill(x, 4, z, x, 12, z, NEON2 if (z // 8) % 2 else NEON)
    # escenario con tarima, bateria, amplis y micro
    v.fill(CX - 12, 2, Z1 - 11, CX + 12, 2, Z1 - 1, 'polished_blackstone_bricks'); v.fill(CX - 10, 3, Z1 - 9, CX + 10, 3, Z1 - 1, 'black_concrete')
    for x in range(CX - 12, CX + 13): v.stairs(x, 2, Z1 - 12, 'polished_blackstone_brick', 'north') if abs(x - CX) <= 3 else v.set(x, 2, Z1 - 12, NEON)
    for (x, z) in [(CX - 9, Z1 - 3), (CX + 9, Z1 - 3), (CX - 6, Z1 - 3), (CX + 6, Z1 - 3)]:
        v.fill(x, 4, z, x + 1, 7, z, 'black_concrete'); v.set(x, 5, z - 1, 'light_gray_concrete'); v.set(x + 1, 5, z - 1, 'light_gray_concrete')
    for (x, z) in [(CX - 1, Z1 - 4), (CX + 1, Z1 - 4), (CX, Z1 - 3), (CX - 2, Z1 - 3), (CX + 2, Z1 - 3)]:      # bateria: bombos, platillos
        v.set(x, 4, z, 'barrel[facing=north]')
    for (x, z) in [(CX - 3, Z1 - 4), (CX + 3, Z1 - 4)]:
        v.set(x, 7, z, 'light_gray_concrete')
    v.fill(CX, 4, Z1 - 6, CX, 6, Z1 - 6, 'iron_bars'); v.set(CX, 7, Z1 - 6, 'black_concrete')
    calavera(v, CX, 11, Z1, 1.0, NEON)
    for x in range(CX - 10, CX + 11): v.set(x, 5, Z1, NEON2)
    return v, [(CX, 4, Z1 - 7, 180, 'emipokemon desafios galar siniestra')]
