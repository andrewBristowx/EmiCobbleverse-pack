"""Gimnasio de Shalour (Korrina, Lucha): coliseo de arenisca con ring de boxeo, sacos colgantes y una rampa de patinaje."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 39, 28, 42
CX, CZ = 19, 25
R = 15


def puno(v, cx, y0, z):
    """emblema: disco rojo con un puño blanco (palma, cuatro dedos y pulgar)"""
    elipse_xy(v, cx, y0 + 6, z, 8, 8, lambda d, a: 'red_concrete' if d > 0.86 else 'blue_concrete')
    v.fill(cx - 4, y0 + 1, z, cx + 3, y0 + 7, z, 'white_concrete')
    for i in range(4): v.fill(cx - 4 + i * 2, y0 + 8, z, cx - 3 + i * 2, y0 + 10, z, 'white_concrete')
    v.fill(cx - 7, y0 + 2, z, cx - 5, y0 + 5, z, 'white_concrete')
    for x in range(cx - 4, cx + 4, 2): v.set(x, y0 + 8, z, 'light_gray_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(61)
    ARE, ARE2, ROJO = 'sandstone', 'smooth_sandstone', 'red_terracotta'
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_sandstone')
    for z in range(0, CZ - R + 1):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'polished_andesite')
    # ---- muro del coliseo con arcadas en dos plantas
    v.cilindro(CX, 1, 17, CZ, R, ARE)
    v.disco(CX, 0, CZ, R, 'smooth_sandstone')
    n = 20
    for y in range(1, 18):
        for x in range(CX - R - 1, CX + R + 2):
            for z in range(CZ - R - 1, CZ + R + 2):
                d = math.hypot(x - CX, z - CZ)
                if R - 1.0 < d <= R + 0.35:
                    a = (math.atan2(z - CZ, x - CX) % (2 * math.pi)) / (2 * math.pi) * n
                    f = abs(a - round(a))
                    if f < 0.25 and ((3 <= y <= 7) or (10 <= y <= 14)):
                        v.set(x, y, z, 'air')
                    elif y in (8, 9, 15, 16, 17) or (y == 2 and f < 0.4):
                        v.set(x, y, z, ARE2 if y not in (9, 16) else 'chiseled_sandstone')
    # pilastras entre arcos
    for k in range(n):
        a = (k + 0.5) / n * 2 * math.pi
        x = round(CX + math.cos(a) * (R - 0.4)); z = round(CZ + math.sin(a) * (R - 0.4))
        v.fill(x, 1, z, x, 17, z, 'cut_sandstone'); v.set(x, 8, z, 'chiseled_sandstone'); v.set(x, 15, z, 'chiseled_sandstone')
    # almenas
    for k in range(n * 2):
        a = k / (n * 2) * 2 * math.pi
        x = round(CX + math.cos(a) * (R - 0.5)); z = round(CZ + math.sin(a) * (R - 0.5))
        if k % 2 == 0: v.set(x, 18, z, ARE2)
    # ---- cubierta anular (de r=10 a r=R) con tejas rojas; el centro queda a cielo abierto
    for x in range(CX - R - 1, CX + R + 2):
        for z in range(CZ - R - 1, CZ + R + 2):
            d = math.hypot(x - CX, z - CZ)
            if 9.5 < d <= R - 0.5: v.set(x, 17, z, 'red_terracotta')
            elif 8.5 < d <= 9.5: v.set(x, 17, z, 'terracotta')
    # ---- puerta principal y emblema del puño
    zf = CZ - R
    hueco_puerta(v, CX, zf, 5, 6, 'red_terracotta', y0=1, prof=2)
    # fachada: panel plano delante del coliseo
    v.fill(CX - 9, 1, zf - 2, CX + 9, 22, zf - 2, 'smooth_sandstone')
    v.fill(CX - 9, 1, zf - 1, CX + 9, 22, zf - 1, 'air')
    v.aire(CX - 2, 1, zf - 2, CX + 2, 6, zf - 2)
    for y in range(1, 8): v.set(CX - 3, y, zf - 2, 'red_terracotta'); v.set(CX + 3, y, zf - 2, 'red_terracotta')
    v.fill(CX - 3, 8, zf - 2, CX + 3, 8, zf - 2, 'red_terracotta')
    v.fill(CX - 10, 23, zf - 2, CX + 10, 23, zf - 2, 'red_terracotta')
    puno(v, CX, 10, zf - 2)
    v.fill(CX - 9, 1, zf - 2, CX - 9, 22, zf - 2, 'cut_sandstone'); v.fill(CX + 9, 1, zf - 2, CX + 9, 22, zf - 2, 'cut_sandstone')
    for y in range(1, 23):
        v.set(CX - 3, y, zf - 3, 'air')
    # ---- interior: gradas que suben hacia fuera (de r=11 a r=14), con un pasillo libre hacia la puerta
    gradas_anillo(v, CX, CZ, 11, 4, 'smooth_sandstone', 'sandstone', 1, hueco=lambda x, z: abs(x - CX) <= 3 and z < CZ - 9)
    # suelo de la arena con dibujo y el ring de boxeo
    disco_f(v, CX, 1, CZ, 10, anillos([(1.5, 'red_terracotta'), (5.5, 'smooth_sandstone'), (6.5, 'red_terracotta'), (10, 'terracotta')]))
    v.fill(CX - 6, 2, CZ - 6, CX + 6, 2, CZ + 6, 'blue_concrete')
    v.anillo(CX - 6, CZ - 6, CX + 6, CZ + 6, 2, 'white_concrete')
    v.anillo(CX - 5, CZ - 5, CX + 5, CZ + 5, 2, 'red_concrete')
    for (x, z) in [(CX - 6, CZ - 6), (CX + 6, CZ - 6), (CX - 6, CZ + 6), (CX + 6, CZ + 6)]:
        v.fill(x, 3, z, x, 6, z, 'red_concrete'); v.set(x, 7, z, 'white_concrete')
    for y in (4, 5, 6):
        for x in range(CX - 5, CX + 6): v.set(x, y, CZ + 6, 'iron_bars'); v.set(x, y, CZ - 6, 'iron_bars') if abs(x - CX) > 1 else None
        for z in range(CZ - 5, CZ + 6): v.set(CX - 6, y, z, 'iron_bars'); v.set(CX + 6, y, z, 'iron_bars')
    v.fill(CX - 1, 3, CZ - 6, CX + 1, 6, CZ - 6, 'air')
    # escalones para subir al ring
    for x in range(CX - 1, CX + 2): v.stairs(x, 2, CZ - 7, 'quartz', 'north')
    # sacos de boxeo colgando de la cubierta, banderas y focos
    for k in range(8):
        a = k / 8 * 2 * math.pi + 0.2
        x = round(CX + math.cos(a) * 10.8); z = round(CZ + math.sin(a) * 10.8)
        for y in range(14, 17): v.set(x, y, z, 'chain[axis=y]')
        v.fill(x, 11, z, x, 13, z, 'brown_terracotta'); v.set(x, 14, z, 'chain[axis=y]')
    # ---- rampa de patinaje (cuarto de tuberia doble) a la izquierda de la entrada
    for x in range(2, 12):
        h = int(round(((x - 6.5) / 4.5) ** 2 * 4.5))
        for z in range(1, 8):
            v.fill(x, 1, z, x, max(1, h), z, 'smooth_stone' if h >= 1 else 'light_gray_concrete')
    for z in range(1, 8):
        v.set(1, 6, z, 'iron_bars'); v.set(12, 6, z, 'iron_bars')
    # ---- entrada del NPC: ring central
    return v, [(CX, 3, CZ, 180, 'emipokemon desafios kalos pelea')]
