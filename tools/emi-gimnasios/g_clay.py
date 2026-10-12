import math
from gymlib import V
from piezas import *

W, H, L = 37, 28, 40
CX, CZ = 18, 24
RX, RZ = 17, 15

def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(21)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'coarse_dirt')
    # montaña: cúpula irregular
    altura = {}
    for x in range(W):
        for z in range(L):
            d = math.sqrt(((x - CX) / RX) ** 2 + ((z - CZ) / RZ) ** 2)
            if d < 1.0:
                h = 17 * (1 - d ** 2) ** 0.8 + 2.2 * math.sin(x * 0.7) * math.cos(z * 0.6) + rnd.random() * 1.2
                altura[(x, z)] = max(2, int(h))
    for (x, z), h in altura.items():
        for y in range(1, h + 1):
            r = rnd.random()
            if y > h - 1: b = 'coarse_dirt' if r < 0.45 else 'dirt' if r < 0.6 else 'terracotta' if r < 0.75 else 'andesite'
            elif y > h * 0.55: b = 'terracotta' if r < 0.3 else 'brown_terracotta' if r < 0.5 else 'stone' if r < 0.7 else 'andesite'
            else: b = 'stone' if r < 0.7 else 'andesite' if r < 0.88 else 'tuff'
            if rnd.random() < 0.035: b = rnd.choice(['iron_ore', 'copper_ore', 'gold_ore', 'coal_ore'])
            v.set(x, y, z, b)
    # caverna elipsoidal
    ry = 9; cy = 1
    for x in range(CX - 14, CX + 15):
        for z in range(CZ - 13, CZ + 14):
            for y in range(1, cy + ry + 1):
                d = math.sqrt(((x - CX) / 13.2) ** 2 + ((z - CZ) / 12.0) ** 2 + ((y - cy) / ry) ** 2)
                if d <= 1.0: v.set(x, y, z, 'air')
    # suelo de la caverna: grava, tierra y piedra con ruta de rieles
    for x in range(CX - 14, CX + 15):
        for z in range(CZ - 13, CZ + 14):
            d = math.sqrt(((x - CX) / 13.2) ** 2 + ((z - CZ) / 12.0) ** 2)
            if d <= 1.0:
                r = rnd.random()
                v.set(x, 0, z, 'gravel' if r < 0.3 else 'coarse_dirt' if r < 0.6 else 'stone')
    # tunel de entrada: desde z=CZ-15 hasta la caverna (5 ancho, 5 alto)
    zt0 = 0
    for z in range(0, CZ - 8):
        for x in range(CX - 3, CX + 4):
            for y in range(1, 7): v.set(x, y, z, 'air') if z >= 6 else None
    for z in range(6, CZ - 8):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'coarse_dirt' if (x + z) % 3 else 'gravel')
    # explanada exterior con vías
    for z in range(0, 12): v.set(CX, 1, z, 'rail[shape=north_south]')
    for z in range(0, 12):
        v.set(CX - 2, 0, z, 'stone_bricks'); v.set(CX + 2, 0, z, 'stone_bricks')
    # portal de madera de la mina (estructura de entrada)
    for x in (CX - 4, CX + 4):
        v.fill(x, 1, 5, x, 8, 5, 'stripped_spruce_log[axis=y]'); v.fill(x, 1, 6, x, 8, 6, 'stripped_spruce_log[axis=y]')
    v.fill(CX - 5, 8, 5, CX + 5, 9, 6, 'dark_oak_planks')
    v.fill(CX - 4, 7, 5, CX + 4, 7, 5, 'spruce_planks')
    for x in (CX - 3, CX - 1, CX + 1, CX + 3): v.set(x, 7, 5, 'lantern[hanging=true]')
    v.fill(CX - 6, 10, 5, CX + 6, 10, 6, 'spruce_slab[type=bottom]')
    # cartel con pico y pala (pixel art simple)
    v.fill(CX - 2, 9, 4, CX + 2, 9, 4, 'oak_wall_sign[facing=north]') if False else None
    # cabria sobre el portal (torre de madera con polea)
    for (dx, dz) in ((-2, 8), (2, 8), (-2, 12), (2, 12)):
        v.fill(CX + dx, 1, dz, CX + dx, 14, dz, 'stripped_spruce_log[axis=y]') if False else None
    # puntales y vigas dentro del tunel
    for z in range(8, CZ - 8, 4):
        for x in (CX - 3, CX + 3): v.fill(x, 1, z, x, 5, z, 'stripped_spruce_log[axis=y]')
        v.fill(CX - 3, 6, z, CX + 3, 6, z, 'dark_oak_planks')
        v.set(CX, 5, z, 'lantern[hanging=true]')
        v.set(CX, 6, z, 'dark_oak_planks')
    for z in range(8, CZ - 8): v.set(CX, 1, z, 'rail[shape=north_south]')
    # puntales en la caverna (ring de columnas de madera con vigas)
    for (dx, dz) in [(-9, -6), (9, -6), (-9, 6), (9, 6), (0, -9), (0, 9)]:
        x, z = CX + dx, CZ + dz
        v.fill(x, 1, z, x, 8, z, 'stripped_spruce_log[axis=y]')
        v.fill(x - 1, 8, z, x + 1, 8, z, 'dark_oak_planks'); v.set(x, 7, z + 1, 'lantern[hanging=false]') if False else None
        v.set(x + 1, 7, z, 'lantern[hanging=true]'); v.set(x - 1, 7, z, 'lantern[hanging=true]')
    # arena: rectangulo de rieles y minas de carbon en el centro
    for x in range(CX - 7, CX + 8):
        v.set(x, 1, CZ - 6, 'rail[shape=east_west]'); v.set(x, 1, CZ + 6, 'rail[shape=east_west]')
    for z in range(CZ - 5, CZ + 6):
        v.set(CX - 7, 1, z, 'rail[shape=north_south]'); v.set(CX + 7, 1, z, 'rail[shape=north_south]')
    v.set(CX - 7, 1, CZ - 6, 'rail[shape=south_east]'); v.set(CX + 7, 1, CZ - 6, 'rail[shape=south_west]')
    v.set(CX - 7, 1, CZ + 6, 'rail[shape=north_east]'); v.set(CX + 7, 1, CZ + 6, 'rail[shape=north_west]')
    v.disco(CX, 0, CZ, 3, 'stone_bricks'); v.disco(CX, 0, CZ, 1, 'gold_block')
    # rocas y cargas (barriles, cofres, bloques de mineral)
    for (dx, dz, b) in [(-11, -3, 'barrel[facing=up]'), (-11, -2, 'barrel[facing=up]'), (11, -4, 'barrel[facing=up]'), (11, 3, 'iron_block'), (-11, 5, 'coal_block'), (-10, 6, 'coal_block'), (10, 7, 'raw_gold_block')]:
        v.set(CX + dx, 1, CZ + dz, b)
    # saliente del lider al fondo: terraza de piedra con andamio
    for x in range(CX - 7, CX + 8):
        for z in range(CZ + 8, CZ + 12):
            for y in range(1, 4 if z < CZ + 10 else 3): v.set(x, y, z, 'stone_bricks' if y == 2 else 'stone')
    for x in range(CX - 7, CX + 8): v.stairs(x, 1, CZ + 7, 'stone_brick', 'north'); v.stairs(x, 2, CZ + 8 if False else CZ + 7, 'stone_brick', 'north') if False else None
    for x in range(CX - 5, CX + 6): v.stairs(x, 3, CZ + 9, 'stone_brick', 'north')
    for x in (CX - 6, CX + 6):
        v.fill(x, 4, CZ + 9, x, 9, CZ + 9, 'stripped_spruce_log[axis=y]'); v.set(x, 9, CZ + 10, 'lantern')
    v.fill(CX - 6, 9, CZ + 9, CX + 6, 9, CZ + 9, 'dark_oak_planks')
    v.fill(CX - 4, 4, CZ + 10, CX + 4, 4, CZ + 10, 'oak_fence'); 
    v.fill(CX - 3, 4, CZ + 11, CX + 3, 4, CZ + 11, 'barrel[facing=up]') if False else None
    # luz: antorchas y linternas colgando del techo de la caverna, ores en las paredes
    for _ in range(40):
        a = rnd.random() * 6.28; rr = rnd.random() * 10
        x = int(CX + math.cos(a) * rr); z = int(CZ + math.sin(a) * rr)
        for y in range(ry + 1, 3, -1):
            if v.get(x, y, z) != 'air' and v.get(x, y - 1, z) == 'air':
                v.set(x, y - 1, z, 'lantern[hanging=true]') if rnd.random() < 0.5 else v.set(x, y - 1, z, 'amethyst_cluster[facing=down]') if False else None
                break
    return v, [(CX, 4, CZ + 10, 180, 'emipokemon desafios teselia temblor')]
