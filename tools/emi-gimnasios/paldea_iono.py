"""Gimnasio de Levincia (Iono, Electrico): rascacielos de neon con pantalla gigante, estudio de directo con aros de luz y pantallas flotantes."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 39, 46, 44
CX = 19
X0, X1 = 6, 32
Z0, Z1 = 12, 40
NEON = ['cyan_concrete', 'magenta_concrete', 'yellow_concrete', 'lime_concrete', 'pink_concrete', 'light_blue_concrete']


def cara_iono(v, cx, cy, z):
    """pantalla con la cara de Iono: pelo amarillo y cian a ambos lados, ojos con estrellas y sonrisa (pixel art)"""
    elipse_xy(v, cx, cy, z, 8, 9, lambda d, a: 'black_concrete')
    elipse_xy(v, cx, cy - 1, z, 5.5, 6.5, lambda d, a: 'white_concrete' if d < 0.95 else 'pink_concrete')
    for s in (-1, 1):
        v.fill(cx + s * 6, cy - 3, z, cx + s * 7, cy + 8, z, 'yellow_concrete')                 # coletas
        v.fill(cx + s * 7, cy - 5, z, cx + s * 7, cy - 3, z, 'cyan_concrete')
        v.fill(cx + s * 2, cy + 1, z, cx + s * 3, cy + 2, z, 'black_concrete'); v.set(cx + s * 2, cy + 2, z, 'white_concrete')   # ojos
        v.set(cx + s * 4, cy - 3, z, 'pink_concrete')                                            # mejillas
    v.fill(cx - 3, cy - 4, z, cx + 3, cy - 4, z, 'red_concrete'); v.set(cx - 4, cy - 3, z, 'red_concrete'); v.set(cx + 4, cy - 3, z, 'red_concrete')   # sonrisa
    v.fill(cx - 5, cy + 5, z, cx + 5, cy + 6, z, 'yellow_concrete')                              # flequillo


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(241)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'gray_concrete')
    for z in range(0, Z0):
        for x in range(W): v.set(x, 0, z, 'black_concrete' if (x // 3 + z // 3) % 2 else 'gray_concrete')
    for x in range(CX - 4, CX + 5):
        for z in range(0, Z0): v.set(x, 0, z, 'cyan_concrete' if (x + z) % 2 else 'light_blue_concrete')
    # farolas de neon y carteles
    for (x, z) in [(3, 4), (35, 4), (3, 9), (35, 9)]:
        v.fill(x, 1, z, x, 7, z, 'iron_bars'); v.set(x, 8, z, rnd.choice(NEON)); v.set(x, 9, z, 'sea_lantern')
    # ---- torre: tres cuerpos escalonados negros con aros de neon
    for (xa, xb, za, zb, y0, y1) in [(X0, X1, Z0, Z1, 1, 14), (X0 + 3, X1 - 3, Z0 + 3, Z1 - 3, 15, 28), (X0 + 7, X1 - 7, Z0 + 7, Z1 - 7, 29, 38)]:
        v.box(xa, y0, za, xb, y1, zb, 'black_concrete', interior=None)
        if y0 == 1: v.aire(xa + 1, 2, za + 1, xb - 1, y1 - 1, zb - 1)
        for k, y in enumerate(range(y0 + 3, y1, 4)):
            v.anillo(xa, za, xb, zb, y, NEON[(k + y0) % 6])
        v.anillo(xa - 1, za - 1, xb + 1, zb + 1, y1, 'gray_concrete')
    for y in range(40, 45): v.fill(CX, y, 26, CX, y, 26, 'iron_bars')
    v.set(CX, 45, 26, 'sea_lantern')
    for (dx, c) in ((-3, 'cyan_concrete'), (3, 'magenta_concrete')): v.fill(CX + dx, 39, 26, CX + dx, 41, 26, c)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'black_concrete')
    # ---- fachada: pantalla gigante con la cara y entrada de cristal
    v.fill(CX - 10, 15, Z0 + 2, CX + 10, 28, Z0 + 2, 'black_concrete')
    cara_iono(v, CX, 21, Z0 + 2)
    for x in range(CX - 10, CX + 11): v.set(x, 14, Z0 + 2, 'cyan_concrete'); v.set(x, 29, Z0 + 2, 'magenta_concrete')
    hueco_puerta(v, CX, Z0, 7, 8, 'cyan_concrete', y0=2)
    for x in range(CX - 3, CX + 4):
        for y in range(10, 13): v.set(x, y, Z0, 'cyan_stained_glass')
    for z in range(Z0 + 4, Z1 - 2, 6):
        for y in (5, 6, 7): v.set(X0, y, z, 'cyan_stained_glass'); v.set(X0, y, z + 1, 'cyan_stained_glass'); v.set(X1, y, z, 'magenta_stained_glass'); v.set(X1, y, z + 1, 'magenta_stained_glass')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, Z0 - 1, 'quartz', 'north') if abs(x - CX) <= 2 else None
    # ---- interior: pista de luz, aros, camaras y pantallas flotantes
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1):
            if (x // 2 + z // 2) % 2: v.set(x, 1, z, 'gray_concrete')
    for z in range(Z0 + 1, Z1):
        v.set(CX - 3, 1, z, 'cyan_concrete'); v.set(CX + 3, 1, z, 'magenta_concrete')
    for k in range(5):                                                                    # arcos de neon a lo largo del pasillo
        z = Z0 + 4 + k * 5
        for y in range(2, 11):
            r = 9 - 0
            for x in (CX - 9, CX + 9): v.set(x, y, z, NEON[k % 6])
        v.fill(CX - 9, 10, z, CX + 9, 10, z, NEON[k % 6])
    for z in range(Z0 + 3, Z1 - 7, 4):                                                      # cables en el suelo
        for x in range(X0 + 2, X1 - 1, 3):
            if rnd.random() < 0.5: v.set(x, 1, z, 'black_concrete')
    for (x, z) in [(CX - 6, 18), (CX + 6, 18), (CX - 6, 26), (CX + 6, 26)]:                # aros de luz sobre tripodes
        v.fill(x, 2, z, x, 4, z, 'iron_bars')
        for a in range(0, 360, 20):
            v.set(round(x + math.cos(math.radians(a)) * 2), round(6 + math.sin(math.radians(a)) * 2), z, 'sea_lantern')
        v.set(x, 6, z, 'black_concrete')
    for k in range(7):                                                                    # pantallas flotantes (cristal y neon)
        x = CX + rnd.randint(-8, 8); z = rnd.randint(Z0 + 5, Z1 - 12); y = rnd.randint(6, 9)
        v.fill(x, y, z, x + 2, y + 1, z, 'black_concrete'); v.fill(x, y + 2, z, x + 2, y + 2, z, rnd.choice(NEON))
        for dx in range(3): v.set(x + dx, y, z, rnd.choice(NEON)) if rnd.random() < 0.5 else None
    # ---- escenario con el sillon de streaming y un marco de pantallas
    v.fill(CX - 8, 2, Z1 - 9, CX + 8, 2, Z1 - 1, 'black_concrete'); v.fill(CX - 5, 3, Z1 - 8, CX + 5, 3, Z1 - 1, 'polished_blackstone')
    for x in range(CX - 8, CX + 9): v.stairs(x, 2, Z1 - 10, 'polished_blackstone_brick', 'north') if abs(x - CX) <= 2 else v.set(x, 2, Z1 - 10, NEON[x % 6])
    for x in range(CX - 5, CX + 6): v.stairs(x, 3, Z1 - 9, 'polished_blackstone_brick', 'north') if abs(x - CX) <= 1 else v.set(x, 3, Z1 - 9, NEON[(x + 2) % 6])
    cara_iono(v, CX, 14, Z1)
    for x in range(CX - 10, CX + 11): v.set(x, 4, Z1, 'magenta_concrete'); v.set(x, 24, Z1, 'cyan_concrete')
    for dx in (-7, 7):
        v.fill(CX + dx, 4, Z1 - 1, CX + dx, 9, Z1 - 1, 'black_concrete'); v.set(CX + dx, 10, Z1 - 1, 'sea_lantern')
    return v, [(CX, 4, Z1 - 5, 180, 'emipokemon desafios paldea electrica')]
