"""Estadio de Circhester (Gordie, Roca): crómlech de piedras gigantes con dinteles, gradas de piedra y un altar de carbon en el centro."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 47, 32, 47
CX, CZ = 23, 26
ROCAS = ['stone'] * 5 + ['andesite'] * 3 + ['tuff'] * 2 + ['cobblestone'] + ['stone_bricks'] * 2 + ['mossy_cobblestone']


def piedra(v, x, z, ancho, alto, rnd, y0=1):
    """menhir: prisma de ancho x ancho, alto bloques, con aristas irregulares"""
    for dx in range(ancho):
        for dz in range(ancho):
            h = alto - (1 if (dx in (0, ancho - 1) and dz in (0, ancho - 1) and rnd.random() < 0.7) else 0) - (1 if rnd.random() < 0.15 else 0)
            for y in range(y0, y0 + h): v.set(x + dx, y, z + dz, rnd.choice(ROCAS))


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(181)
    for x in range(W):
        for z in range(L):
            v.set(x, 0, z, rnd.choice(['grass_block', 'grass_block', 'coarse_dirt', 'gravel', 'stone']))
    # ---- explanada circular de piedra con anillos
    disco_f(v, CX, 0, CZ, 19, anillos([(2.5, 'coal_block'), (5, 'polished_andesite'), (6, 'stone_bricks'), (9, 'smooth_stone'), (10, 'stone_bricks'), (15, 'polished_andesite'), (16, 'stone_bricks'), (19, 'gravel')]))
    for z in range(0, CZ - 17):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'stone_bricks' if (x + z) % 2 else 'polished_andesite')
    # ---- crómlech exterior: 16 menhires y dinteles entre pares (la entrada queda libre al norte)
    n = 16
    pos = []
    for k in range(n):
        a = k / n * 2 * math.pi
        x = round(CX + math.cos(a) * 17); z = round(CZ + math.sin(a) * 17)
        pos.append((k, a, x, z))
    for (k, a, x, z) in pos:
        if abs(x - CX) <= 4 and z < CZ - 12: continue
        alto = 11 + rnd.randint(-1, 2)
        piedra(v, x - 1, z - 1, 3, alto, rnd)
    for k in range(0, n, 2):
        a1, a2 = pos[k], pos[(k + 1) % n]
        if (abs(a1[2] - CX) <= 4 and a1[3] < CZ - 12) or (abs(a2[2] - CX) <= 4 and a2[3] < CZ - 12): continue
        h1 = max(y for y in range(0, 20) if v.get(a1[2], y, a1[3]) != 'air')
        for t in range(0, 11):
            x = round(a1[2] + (a2[2] - a1[2]) * t / 10); z = round(a1[3] + (a2[3] - a1[3]) * t / 10)
            for dy in (1, 2):
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        if abs(dx) + abs(dz) <= 1: v.set(x + dx, h1 + dy, z + dz, rnd.choice(['stone', 'andesite', 'tuff']))
    # ---- circulo interior de piedras bajas con musgo
    for k in range(10):
        a = k / 10 * 2 * math.pi + 0.2
        x = round(CX + math.cos(a) * 10); z = round(CZ + math.sin(a) * 10)
        if abs(x - CX) <= 3 and z < CZ - 6: continue
        piedra(v, x - 1, z - 1, 2, 4 + rnd.randint(0, 2), rnd)
    # ---- gradas bajas en un arco detras del altar (al fondo) y a los lados
    gradas_anillo(v, CX, CZ, 6, 3, 'stone_bricks', 'stone_brick', 1, hueco=lambda x, z: z < CZ - 2 or abs(x - CX) <= 2 and z < CZ + 8)
    # ---- altar central: tres escalones y un brasero de carbon
    for k, r in enumerate((4, 3, 2)):
        disco_f(v, CX, 1 + k, CZ, r, anillos([(r, 'stone_bricks' if k != 1 else 'chiseled_stone_bricks')]))
    for (dx, dz) in ((-3, 0), (3, 0), (0, 3), (0, -3)):
        v.fill(CX + dx, 2, CZ + dz, CX + dx, 4, CZ + dz, 'coal_block'); v.set(CX + dx, 5, CZ + dz, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
    v.set(CX, 4, CZ, 'coal_block')
    v.fill(CX - 1, 4, CZ - 1, CX + 1, 4, CZ + 1, 'polished_andesite')
    # ---- estatuas guardianas de roca a los lados de la entrada (apilando bloques)
    for sx in (-8, 8):
        x = CX + sx; z = 5
        v.esfera(x, 4, z, 3, 'stone'); v.esfera(x, 9, z, 2, 'andesite'); v.fill(x - 1, 1, z - 1, x + 1, 2, z + 1, 'stone_bricks')
        v.set(x - 1, 10, z - 2, 'coal_block'); v.set(x + 1, 10, z - 2, 'coal_block'); v.fill(x - 1, 8, z - 2, x + 1, 8, z - 2, 'white_concrete')
    # farolillos y minerales incrustados
    for _ in range(60):
        x = rnd.randint(1, W - 2); z = rnd.randint(1, L - 2)
        for y in range(14, 0, -1):
            b = v.get(x, y, z)
            if b != 'air' and 'stone' in b and b not in ('lantern',):
                if rnd.random() < 0.5 and v.get(x, y + 1, z) == 'air': v.set(x, y, z, rnd.choice(['coal_ore', 'iron_ore', 'copper_ore', 'gold_ore']))
                break
    for (x, z) in [(CX - 4, CZ - 15), (CX + 4, CZ - 15), (CX - 6, 6), (CX + 6, 6)]:
        v.fill(x, 1, z, x, 3, z, 'stone_brick_wall'); v.set(x, 4, z, 'lantern')
    # rocas sueltas por el terreno
    for _ in range(14):
        x = rnd.randint(2, W - 3); z = rnd.randint(2, L - 3)
        if math.hypot(x - CX, z - CZ) > 22: v.esfera(x, 1, z, rnd.randint(1, 2), rnd.choice(['stone', 'andesite', 'cobblestone', 'mossy_cobblestone']))
    return v, [(CX, 5, CZ, 180, 'emipokemon desafios galar roca')]
