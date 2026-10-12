"""Prueba de Kiawe (Fuego) en el Parque del Volcan Wela: caldera de basalto con foso de lava, puente, tikis encendidos y humo."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 43, 32, 46
CX, CZ = 21, 26


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(331)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['basalt[axis=y]', 'blackstone', 'coarse_dirt', 'gravel', 'smooth_basalt']))
    for z in range(0, 5):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'polished_basalt[axis=y]')
    # ---- caldera: anillo de basalto (cresta a 16 bloques) con pendiente exterior y pared interior; el suelo del anillo interior queda a nivel
    for x in range(1, W - 1):
        for z in range(3, L - 1):
            d = math.hypot(x - CX, (z - CZ) * 1.0)
            if d < 9.5 or d > 20.5: continue
            if d <= 13: h = 16 - abs(d - 11.5) * 0.8
            else: h = 15 - (d - 13) * 1.9
            h = int(h + rnd.random() * 1.5)
            for y in range(1, h + 1):
                v.set(x, y, z, rnd.choice(['basalt[axis=y]', 'blackstone', 'smooth_basalt', 'basalt[axis=y]', 'tuff', 'magma_block' if (y > h - 2 and rnd.random() < 0.06) else 'blackstone']))
    # hendiduras de lava en la ladera (solo decorativas: bloque de magma brillante)
    for _ in range(25):
        a = rnd.random() * 2 * math.pi; d = rnd.uniform(13, 18)
        x, z = round(CX + math.cos(a) * d), round(CZ + math.sin(a) * d)
        for y in range(25, 0, -1):
            if v.get(x, y, z) != 'air': v.set(x, y, z, 'magma_block'); break
    # ---- entrada: corte frontal en el anillo con escalones y tikis
    for z in range(3, CZ - 8):
        for x in range(CX - 3, CX + 4):
            for y in range(1, 22): v.set(x, y, z, 'air')
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'polished_basalt[axis=y]')
    for (x, z) in [(CX - 5, 6), (CX + 5, 6)]: tiki(v, x, z, 8, 'blackstone')
    for z in range(8, CZ - 9, 3):
        for dx in (-4, 4): antorcha_tiki(v, CX + dx, z, 4)
    # ---- arena: suelo de basalto con anillos, foso de lava y puente
    disco_f(v, CX, 0, CZ, 9, anillos([(3, 'polished_basalt[axis=y]'), (4.5, 'magma_block'), (5.0, 'blackstone'), (6.5, 'smooth_basalt'), (8, 'polished_blackstone'), (9, 'blackstone')]))
    disco_f(v, CX, 0, CZ, 6.5, lambda d, a: 'lava' if 5.0 < d <= 6.5 else None)            # foso de lava a nivel del suelo, bordeado de piedra por todos lados
    disco_f(v, CX, 1, CZ, 7.5, lambda d, a: 'blackstone' if 6.5 < d <= 7.5 and rnd.random() < 0.15 else None)
    for z in range(CZ - 8, CZ - 3):
        for x in range(CX - 1, CX + 2): v.set(x, 0, z, 'polished_blackstone_bricks'); v.set(x, 1, z, 'polished_blackstone_bricks')
        for x in (CX - 2, CX + 2): v.set(x, 1, z, 'polished_blackstone_brick_wall')
    # antorchas encendidas alrededor de la arena y plataforma del lider
    for k in range(10):
        a = k / 10 * 2 * math.pi + 0.15
        x, z = round(CX + math.cos(a) * 8.5), round(CZ + math.sin(a) * 8.5)
        if abs(x - CX) <= 2 and z < CZ - 5: continue
        antorcha_tiki(v, x, z, 3)
    for r, y in ((3.5, 1), (2.5, 2)):
        disco_f(v, CX, y, CZ, int(r), anillos([(r, 'polished_basalt[axis=y]' if y == 1 else 'chiseled_polished_blackstone')]))
    for (x, z) in [(CX - 7, CZ + 10), (CX + 7, CZ + 10)]: tiki(v, x, z, 7, 'blackstone', 4)
    # humo: hogueras de señal sobre heno en la cresta
    for k in range(6):
        a = k / 6 * 2 * math.pi + 0.5
        x, z = round(CX + math.cos(a) * 12), round(CZ + math.sin(a) * 12)
        for y in range(24, 0, -1):
            if v.get(x, y, z) != 'air' and v.get(x, y + 1, z) == 'air':
                v.set(x, y + 1, z, 'hay_block[axis=y]'); v.set(x, y + 2, z, 'campfire[lit=true,facing=north,signal_fire=true,waterlogged=false]'); break
    return v, [(CX, 3, CZ, 180, 'emipokemon desafios alola captain_kiawe')]
