"""Estadio de Motostoke (Kabu, Fuego): acerería de piedra negra con chimeneas humeantes, tuberias, un foso de lava y una llama gigante en la fachada."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 43, 46, 46
CX, CZ = 21, 27
R = 15


def llama(v, cx, y0, z, escala=1.0):
    """llama gigante en el plano XY: exterior naranja, interior amarillo, nucleo blanco"""
    def f(d, a):
        return 'white_concrete' if d < 0.25 else 'yellow_concrete' if d < 0.55 else 'orange_concrete'
    elipse_xy(v, cx, y0 + 5 * escala, z, 4.2 * escala, 5.2 * escala, f)
    for k in range(0, 9):
        w = max(1, int((9 - k) * 0.5 * escala))
        v.fill(cx - w, int(y0 + 9 * escala + k), z, cx + w, int(y0 + 9 * escala + k), z, 'orange_concrete' if k > 2 else 'yellow_concrete')
    for (dx, k) in ((-3, 5), (3, 6), (-5, 3), (5, 4)):
        for j in range(k): v.set(cx + dx, int(y0 + 8 * escala + j), z, 'orange_concrete')


def chimenea(v, x, z, alto):
    v.cilindro(x, 1, alto, z, 2, 'bricks')
    v.disco(x, alto, z, 2, 'black_concrete')
    v.anillo_circ(x, alto - 1, z, 2, 'gray_concrete', 1.0)
    for y in (5, 11, 17): v.anillo_circ(x, y, z, 2, 'iron_block', 1.0)
    v.set(x, alto + 1, z, 'hay_block[axis=y]'); v.set(x, alto + 2, z, 'campfire[lit=true,facing=north,signal_fire=true,waterlogged=false]')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(151)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'gray_concrete')
    for x in range(W):
        for z in range(L):
            if rnd.random() < 0.2: v.set(x, 0, z, rnd.choice(['blackstone', 'polished_blackstone', 'deepslate']))
    zf = CZ - R
    for z in range(0, zf):                                                       # camino con franjas de peligro
        for x in range(CX - 4, CX + 5):
            v.set(x, 0, z, 'yellow_concrete' if abs(x - CX) == 4 and z % 2 == 0 else 'black_concrete')
    # ---- cilindro de la acereria con bandas naranja y rendijas de fuego
    v.cilindro(CX, 1, 20, CZ, R, 'polished_blackstone_bricks')
    v.disco(CX, 0, CZ, R, 'blackstone')
    for y in (4, 10, 16): v.anillo_circ(CX, y, CZ, R, 'orange_concrete', 1.0)
    for k in range(16):
        a = k / 16 * 2 * math.pi + 0.2
        for y in range(6, 9):
            x = round(CX + math.cos(a) * R); z = round(CZ + math.sin(a) * R)
            if not (abs(x - CX) <= 6 and z < CZ - 8): v.set(x, y, z, 'orange_stained_glass')
    for k in range(16):
        a = k / 16 * 2 * math.pi
        x = round(CX + math.cos(a) * (R + 0.6)); z = round(CZ + math.sin(a) * (R + 0.6))
        if not (abs(x - CX) <= 7 and z < CZ - 8): v.fill(x, 1, z, x, 19, z, 'gray_concrete') if k % 2 == 0 else None
    v.anillo_circ(CX, 20, CZ, R, 'iron_block', 2.0)
    # techo: anillo de chapa y claraboya central de cristal tintado
    for x in range(CX - R, CX + R + 1):
        for z in range(CZ - R, CZ + R + 1):
            d = math.hypot(x - CX, z - CZ)
            if 7 < d <= R - 0.5: v.set(x, 21, z, 'black_concrete')
            elif d <= 7: v.set(x, 21, z, 'orange_stained_glass') if d > 5 else None
    # ---- chimeneas, tuberias y depositos
    for (x, z, h) in [(5, 8, 32), (37, 8, 34), (5, 44, 30), (37, 44, 33)]:
        chimenea(v, x, z, h)
    for (x, z) in [(5, 25), (37, 25)]:                                           # depositos cilindricos
        v.cilindro(x, 1, 12, z, 3, 'iron_block'); v.disco(x, 13, z, 3, 'iron_block')
        for y in (4, 8): v.anillo_circ(x, y, z, 3, 'orange_concrete', 1.0)
    for z in range(10, 42, 3):
        pass
    v.fill(5, 15, 8, 5, 15, 25, 'copper_block'); v.fill(5, 14, 25, 5, 15, 25, 'copper_block')
    v.fill(37, 15, 8, 37, 15, 25, 'copper_block')
    v.fill(5, 15, 8, 10, 15, 8, 'copper_block'); v.fill(32, 15, 8, 37, 15, 8, 'copper_block')
    for x in range(10, 33): pass
    # ---- fachada plana con la gran llama y puerta de acero
    v.fill(CX - 9, 1, zf - 2, CX + 9, 26, zf - 2, 'polished_blackstone_bricks')
    v.fill(CX - 9, 27, zf - 2, CX + 9, 27, zf - 2, 'orange_concrete')
    for y in range(1, 27, 5): v.fill(CX - 9, y, zf - 2, CX + 9, y, zf - 2, 'gray_concrete')
    v.aire(CX - 3, 1, zf - 2, CX + 3, 7, zf - 1)
    hueco_puerta(v, CX, zf, 7, 7, 'iron_block', y0=1, prof=1)
    for x in range(CX - 3, CX + 4): v.set(x, 8, zf - 2, 'iron_block')
    for dx in (-4, 4): v.fill(CX + dx, 1, zf - 2, CX + dx, 7, zf - 2, 'yellow_concrete');
    llama(v, CX, 12, zf - 2, 1.4)
    for y in range(1, 27): v.set(CX - 9, y, zf - 3, 'iron_block'); v.set(CX + 9, y, zf - 3, 'iron_block')
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, zf - 3, 'polished_blackstone_brick', 'north') if abs(x - CX) <= 2 else None
    # ---- interior: foso de lava circular, plataforma central y gradas de piedra negra
    gradas_anillo(v, CX, CZ, 11, 4, 'blackstone', 'polished_blackstone_brick', 1, hueco=lambda x, z: abs(x - CX) <= 3 and z < CZ - 8)
    disco_f(v, CX, 1, CZ, 10, anillos([(7.5, 'polished_blackstone_bricks'), (8.5, 'lava'), (10, 'blackstone')]))
    disco_f(v, CX, 1, CZ, 7, lambda d, a: 'orange_terracotta' if int(d) % 2 == 0 and d > 3 else 'magma_block' if d < 1.5 else 'polished_blackstone')
    disco_f(v, CX, 2, CZ, 10, lambda d, a: 'iron_bars' if 9.2 < d <= 10 and not (abs(math.cos(a)) < 0.35 and math.sin(a) < 0) else None)
    for z in range(CZ - 11, CZ - 6):                                             # puente de entrada sobre la lava
        for x in range(CX - 1, CX + 2): v.set(x, 1, z, 'polished_blackstone_bricks')
        v.set(CX - 2, 2, z, 'iron_bars') if z > CZ - 10 else None; v.set(CX + 2, 2, z, 'iron_bars') if z > CZ - 10 else None
    for x in range(CX - 4, CX + 5): pass
    for k in range(8):                                                           # braseros en el borde del foso
        a = k / 8 * 2 * math.pi + math.pi / 8
        x = round(CX + math.cos(a) * 10.5); z = round(CZ + math.sin(a) * 10.5)
        if v.get(x, 2, z) == 'air': v.fill(x, 2, z, x, 3, z, 'polished_blackstone_bricks'); v.set(x, 4, z, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
    # pasarela alta con farolillos y cadenas
    for k in range(12):
        a = k / 12 * 2 * math.pi
        x = round(CX + math.cos(a) * 12); z = round(CZ + math.sin(a) * 12)
        for y in range(15, 20): v.set(x, y, z, 'chain[axis=y]') if v.get(x, 20, z) != 'air' else None
        v.set(x, 14, z, 'shroomlight') if v.get(x, 15, z) == 'chain[axis=y]' else None
    # ---- estrado del lider en el centro de la arena
    for x in range(CX - 2, CX + 3):
        for z in range(CZ - 2, CZ + 3): v.set(x, 2, z, 'polished_blackstone_bricks')
    v.set(CX, 2, CZ, 'magma_block')
    return v, [(CX, 3, CZ, 180, 'emipokemon desafios galar fuego')]
