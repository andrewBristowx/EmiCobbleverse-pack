"""Gimnasio de Alfornada (Tulip, Psiquico): pabellon-flor de cristal rosa con petalos, pasarela de moda, espejos y gemas flotantes."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 41, 38, 46
CX, CZ = 20, 27
R = 15


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(281)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_quartz')
    zf = CZ - R
    disco_f(v, CX, 0, CZ, R + 4, anillos([(R + 1, 'pink_concrete'), (R + 3, 'white_concrete'), (R + 4, 'magenta_concrete')]))
    for z in range(0, zf):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'pink_concrete' if x in (CX - 3, CX + 3) else 'white_concrete')
    # jardin de cristales y setos pastel
    for (x, z) in [(CX - 12, 5), (CX + 12, 5), (CX - 8, 9), (CX + 8, 9), (4, 14), (36, 14)]:
        v.fill(x, 1, z, x, 4, z, 'amethyst_block'); v.set(x, 5, z, 'amethyst_cluster[facing=up,waterlogged=false]')
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)): v.set(x + dx, 1, z + dz, 'amethyst_cluster[facing=up,waterlogged=false]')
    for x in list(range(2, CX - 5)) + list(range(CX + 6, W - 2)):
        for z in (2, 3):
            v.set(x, 0, z, 'grass_block')
            if rnd.random() < 0.6: v.set(x, 1, z, rnd.choice(['cherry_leaves', 'pink_tulip', 'allium', 'azure_bluet']))
    # ---- base circular y petalos: costillas de cuarzo y paneles de cristal rosa
    v.cilindro(CX, 1, 6, CZ, R, 'white_concrete')
    v.disco(CX, 0, CZ, R, 'smooth_quartz')
    v.anillo_circ(CX, 6, CZ, R, 'pink_concrete', 1.0)
    v.anillo_circ(CX, 3, CZ, R, 'magenta_concrete', 1.0)
    for k in range(16):
        a = k / 16 * 2 * math.pi + 0.2
        for y in (4, 5):
            x = round(CX + math.cos(a) * R); z = round(CZ + math.sin(a) * R)
            if not (abs(x - CX) <= 5 and z < CZ - 8): v.set(x, y, z, 'pink_stained_glass')
    v.cupula(CX, 7, CZ, R, 'pink_stained_glass', hueco=True, ry=18)
    for k in range(8):                                                                    # costillas-petalo
        a = k / 8 * 2 * math.pi + 0.2
        for i in range(0, 61):
            el = math.radians(i * 1.5)
            x = round(CX + math.cos(a) * (R - 0.3) * math.cos(el)); z = round(CZ + math.sin(a) * (R - 0.3) * math.cos(el)); y = 7 + round(math.sin(el) * 18)
            v.set(x, y, z, 'white_concrete' if i % 8 else 'magenta_concrete')
    for lat in (9, 14, 19, 24):
        for i in range(0, 721):
            a = i / 720 * 2 * math.pi
            rr = R * math.sqrt(max(0, 1 - ((lat - 7) / 18) ** 2))
            x, z = round(CX + math.cos(a) * rr), round(CZ + math.sin(a) * rr)
            if v.get(x, lat, z) == 'pink_stained_glass': v.set(x, lat, z, 'magenta_stained_glass')
    v.fill(CX, 24, CZ, CX, 31, CZ, 'amethyst_block'); v.set(CX, 32, CZ, 'end_rod[facing=up]')
    v.esfera(CX, 30, CZ, 2, 'light_blue_stained_glass')
    # ---- entrada: arco floral de cuarzo
    hueco_puerta(v, CX, zf, 5, 7, 'white_concrete', y0=1, prof=2)
    v.fill(CX - 4, 8, zf - 1, CX + 4, 9, zf - 1, 'pink_concrete')
    for dx in (-4, 4): v.fill(CX + dx, 1, zf - 1, CX + dx, 7, zf - 1, 'quartz_pillar[axis=y]')
    for k in range(5): v.set(CX - 2 + k, 10, zf - 1, 'cherry_leaves')
    # ---- interior: suelo en espiral, pasarela, asientos y espejos
    disco_f(v, CX, 1, CZ, R - 1, lambda d, a: 'white_concrete' if int(d + a * 2) % 2 == 0 else 'pink_concrete' if d > 3 else 'magenta_concrete')
    for z in range(zf + 1, CZ + 11):
        for x in range(CX - 2, CX + 3): v.set(x, 1, z, 'smooth_quartz' if abs(x - CX) < 2 else 'magenta_concrete')
        if z % 3 == 0: v.set(CX - 1, 1, z, 'sea_lantern'); v.set(CX + 1, 1, z, 'sea_lantern')
    for z in range(zf + 4, CZ + 6, 2):                                                     # asientos a los lados de la pasarela
        for dx in (-5, -4, 4, 5): v.stairs(CX + dx, 2, z, 'quartz', 'east' if dx < 0 else 'west') if abs(dx) == 4 else v.set(CX + dx, 2, z, 'pink_concrete')
    for k in range(10):
        a = k / 10 * 2 * math.pi + 0.15
        x = round(CX + math.cos(a) * (R - 1.5)); z = round(CZ + math.sin(a) * (R - 1.5))
        if abs(x - CX) <= 4 and z < CZ - 8: continue
        v.fill(x, 2, z, x, 6, z, 'glass'); v.set(x, 7, z, 'gold_block')
        v.set(x, 1, z, 'gold_block')
    for k in range(6):                                                                    # tocadores con velas
        a = k / 6 * 2 * math.pi + 0.5
        x = round(CX + math.cos(a) * 11); z = round(CZ + math.sin(a) * 11)
        if abs(x - CX) <= 4 and z < CZ - 8: continue
        v.set(x, 2, z, 'smooth_quartz'); v.set(x, 3, z, 'smooth_quartz_slab[type=bottom]'); v.set(x, 4, z, 'candle[candles=2,lit=true]')
    for k in range(7):                                                                    # gemas flotantes sobre varas del End
        a = k / 7 * 2 * math.pi
        x = round(CX + math.cos(a) * 7); z = round(CZ + math.sin(a) * 7)
        v.fill(x, 2, z, x, 5, z, 'white_concrete'); v.set(x, 6, z, 'amethyst_block'); v.set(x, 7, z, 'amethyst_cluster[facing=up,waterlogged=false]')
    # ---- escenario del lider con corona de cristales
    disco_f(v, CX, 2, CZ + 9, 4, anillos([(4, 'smooth_quartz')])); disco_f(v, CX, 3, CZ + 9, 2, anillos([(2, 'magenta_concrete')]))
    for x in range(CX - 4, CX + 5): v.stairs(x, 2, CZ + 4, 'quartz', 'north') if abs(x - CX) <= 1 else None
    for k in range(5):
        a = k / 5 * 2 * math.pi
        x = round(CX + math.cos(a) * 4.5); z = round(CZ + 9 + math.sin(a) * 3.5)
        v.fill(x, 4, z, x, 6, z, 'amethyst_block'); v.set(x, 7, z, 'amethyst_cluster[facing=up,waterlogged=false]')
    return v, [(CX, 4, CZ + 9, 180, 'emipokemon desafios paldea psiquica')]
