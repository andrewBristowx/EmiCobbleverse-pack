"""Prueba de Sophocles (Electrico) en el Observatorio Hokulani: colina con camino en espiral, cupula blanca con rendija, telescopio gigante y antenas parabolicas."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 39, 36, 42
CX, CZ = 19, 24
R = 11


def parabolica(v, x, y, z, r, giro=0):
    """antena parabolica: plato blanco inclinado hacia el cielo y soporte"""
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            if d <= r + 0.3: v.set(x + dx, y + int(d * d / (r * 1.6)), z + dz, 'white_concrete' if d > 1 else 'iron_block')
    v.fill(x, y - 3, z, x, y - 1, z, 'iron_block'); v.fill(x, y, z, x, y + int(r * r / 1.6) + 1, z, 'iron_bars')
    v.set(x, y + int(r * r / 1.6) + 2, z, 'sea_lantern')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(351)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['grass_block', 'grass_block', 'coarse_dirt']))
    for z in range(0, CZ - R - 2):
        for x in range(CX - 2, CX + 3): v.set(x, 0, z, 'smooth_stone' if (x + z) % 2 else 'stone_bricks')
    flores_tropicales(v, 1, W - 2, 1, L - 2, rnd, 0.1)
    for (x, z) in [(4, 5), (34, 7), (4, 32), (35, 34), (30, 4)]: palmera(v, x, z, rnd.randint(5, 7), rnd)
    # ---- observatorio: base cilindrica blanca y cupula con rendija
    v.cilindro(CX, 1, 9, CZ, R, 'white_concrete')
    v.disco(CX, 0, CZ, R, 'smooth_quartz')
    for y in (3, 6, 9): v.anillo_circ(CX, y, CZ, R, 'light_gray_concrete', 1.0)
    for k in range(12):
        a = k / 12 * 2 * math.pi
        x, z = round(CX + math.cos(a) * R), round(CZ + math.sin(a) * R)
        for y in (4, 5): v.set(x, y, z, 'light_blue_stained_glass')
    v.cupula(CX, 10, CZ, R, 'white_concrete', hueco=True, ry=11)
    for a in range(0, 360, 30):
        for i in range(0, 12):
            el = math.radians(i * 7.5)
            x = round(CX + math.cos(math.radians(a)) * (R - 0.3) * math.cos(el)); z = round(CZ + math.sin(math.radians(a)) * (R - 0.3) * math.cos(el)); y = 10 + round(math.sin(el) * 11)
            v.set(x, y, z, 'light_gray_concrete')
    # rendija de observacion hacia el frente (-z): hueco vertical en la cupula
    for z in range(CZ - R - 2, CZ + 1):
        for x in range(CX - 2, CX + 3):
            for y in range(11, 22):
                if v.get(x, y, z) not in ('air',) and abs(x - CX) <= 1: v.set(x, y, z, 'air')
    # ---- puerta, rampa de entrada y camino en espiral
    hueco_puerta(v, CX, CZ - R, 5, 6, 'light_gray_concrete', y0=1, prof=2)
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, CZ - R - 2, 'stone_brick', 'north') if abs(x - CX) <= 2 else None
    for (x, z) in [(CX - 4, CZ - R - 2), (CX + 4, CZ - R - 2)]: v.fill(x, 1, z, x, 3, z, 'iron_bars'); v.set(x, 4, z, 'sea_lantern')
    # ---- antenas y panel de datos
    parabolica(v, 5, 4, 14, 4); parabolica(v, 33, 4, 16, 4); parabolica(v, 6, 4, 34, 3)
    v.fill(CX + 14, 1, CZ - 8, CX + 14, 18, CZ - 8, 'iron_bars'); v.fill(CX + 13, 14, CZ - 8, CX + 15, 14, CZ - 8, 'iron_bars'); v.set(CX + 14, 19, CZ - 8, 'sea_lantern')
    # ---- interior: suelo con mapa estelar, telescopio y consolas
    disco_f(v, CX, 1, CZ, R - 1, anillos([(1.5, 'yellow_concrete'), (4, 'blue_concrete'), (5, 'light_blue_concrete'), (8, 'blue_concrete'), (9, 'white_concrete'), (11, 'blue_concrete')]))
    for _ in range(60):
        a = rnd.random() * 2 * math.pi; d = rnd.random() * 9.5
        x, z = round(CX + math.cos(a) * d), round(CZ + math.sin(a) * d)
        if v.get(x, 1, z) in ('blue_concrete', 'light_blue_concrete'): v.set(x, 1, z, 'white_concrete')
    for k in range(24):                                                                    # estrellas en la cupula interior
        a = rnd.random() * 2 * math.pi; el = rnd.random() * 1.2
        x = round(CX + math.cos(a) * (R - 1.3) * math.cos(el)); z = round(CZ + math.sin(a) * (R - 1.3) * math.cos(el)); y = 10 + round(math.sin(el) * (R - 1.3))
        if v.get(x, y, z) == 'air': v.set(x, y, z, 'sea_lantern')
    # telescopio: soporte central y tubo diagonal hacia la rendija
    v.fill(CX - 1, 2, CZ + 3, CX + 1, 4, CZ + 5, 'iron_block'); v.fill(CX, 5, CZ + 4, CX, 6, CZ + 4, 'iron_block')
    for t in range(0, 12):
        v.fill(CX - 1, 7 + t, CZ + 4 - t, CX, 8 + t, CZ + 5 - t, 'smooth_quartz' if t % 4 else 'light_gray_concrete')
    v.set(CX - 1, 8 + 12, CZ - 8, 'iron_block'); v.set(CX, 8 + 12, CZ - 8, 'iron_block')
    for (dx, dz) in ((-8, 4), (8, 4), (-7, -2), (7, -2)):                                     # consolas con pantallas
        v.fill(CX + dx, 2, CZ + dz, CX + dx + 1, 3, CZ + dz + 1, 'polished_andesite'); v.fill(CX + dx, 4, CZ + dz, CX + dx + 1, 4, CZ + dz, 'light_blue_concrete')
        v.set(CX + dx, 3, CZ + dz - 1, 'sea_lantern')
    # estrado del lider detras del telescopio
    disco_f(v, CX, 2, CZ + 7, 3, anillos([(3, 'smooth_quartz')]))
    for x in range(CX - 3, CX + 4): v.stairs(x, 2, CZ + 4, 'quartz', 'north') if abs(x - CX) <= 1 else None
    return v, [(CX, 3, CZ + 7, 180, 'emipokemon desafios alola captain_sophocles')]
