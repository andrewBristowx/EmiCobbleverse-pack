"""Gran prueba de Hapu (Tierra) en el Cañon de Poni: arena entre mesas de terracota en capas, un arco natural, cactus y un castillo de arena (Palossand)."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *

W, H, L = 45, 34, 46
CX, CZ = 22, 27
CAPAS = ['terracotta', 'orange_terracotta', 'yellow_terracotta', 'red_terracotta', 'brown_terracotta', 'white_terracotta', 'orange_terracotta', 'terracotta', 'yellow_terracotta', 'red_terracotta']


def castillo_arena(v, cx, z, rnd):
    """castillo de arena con almenas y torres (Palossand): arenisca lisa abajo, arena encima, ojos oscuros en la puerta"""
    v.fill(cx - 5, 1, z - 3, cx + 5, 6, z + 3, 'smooth_sandstone'); v.fill(cx - 4, 7, z - 2, cx + 4, 9, z + 2, 'sandstone')
    for x in range(cx - 5, cx + 6, 2): v.set(x, 7, z - 3, 'smooth_sandstone'); v.set(x, 7, z + 3, 'smooth_sandstone')
    for (dx, dz) in ((-6, -4), (6, -4), (-6, 4), (6, 4)):
        v.fill(cx + dx - 1, 1, z + dz - 1, cx + dx + 1, 10, z + dz + 1, 'smooth_sandstone')
        v.fill(cx + dx - 1, 11, z + dz - 1, cx + dx + 1, 12, z + dz + 1, 'sandstone'); v.set(cx + dx, 13, z + dz, 'sandstone')
    v.aire(cx - 1, 1, z - 3, cx + 1, 3, z - 3); v.set(cx - 2, 3, z - 3, 'black_concrete'); v.set(cx + 2, 3, z - 3, 'black_concrete')
    v.fill(cx - 3, 5, z - 3, cx + 3, 5, z - 3, 'sandstone'); v.set(cx - 1, 6, z - 3, 'black_concrete'); v.set(cx + 1, 6, z - 3, 'black_concrete')
    v.fill(cx - 3, 10, z - 2, cx + 3, 10, z + 2, 'sand')
    v.set(cx, 11, z, 'sand'); v.set(cx, 12, z, 'sandstone')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(411)
    for x in range(W):
        for z in range(L): v.set(x, 0, z, rnd.choice(['red_sand', 'red_sand', 'sand', 'red_sandstone']))
    for z in range(0, 10):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'smooth_red_sandstone' if (x + z) % 2 else 'red_sandstone')
    # ---- paredes del cañon en capas de terracota: izquierda, derecha y fondo; el frente queda abierto
    for x in range(W):
        for z in range(10, L):
            dx = abs(x - CX); dz = z - CZ
            d = max(dx / 16.5, dz / 17.0 if dz > 0 else 0)
            if d < 1.0: continue
            h = 16 + min(14, (d - 1.0) * 14) + rnd.random() * 3
            if z < 14 and dx < 22: h = 8 + rnd.random() * 2
            for y in range(1, int(h) + 1): v.set(x, y, z, CAPAS[(y // 3) % len(CAPAS)])
    # arco natural a la entrada
    for dx in (-8, 8): v.fill(CX + dx - 3, 1, 12, CX + dx + 3, 15, 16, 'terracotta')
    for y in range(1, 22):
        for x in range(CX - 8 - 3, CX + 8 + 4):
            for z in range(12, 17):
                if y >= 16 and (abs(x - CX) < 11 - (y - 15) // 2): v.set(x, y, z, CAPAS[(y // 3) % len(CAPAS)])
    v.aire(CX - 5, 1, 12, CX + 5, 15, 16)
    for (x, y, z) in [(CX - 6, 14, 13), (CX + 6, 14, 14), (CX - 5, 15, 12), (CX + 5, 15, 12)]: v.set(x, y, z, 'orange_terracotta')
    # ---- arena: suelo de arena con anillos de terracota
    disco_f(v, CX, 0, CZ, 13, anillos([(1.5, 'orange_terracotta'), (5, 'red_sand'), (6, 'terracotta'), (9, 'sand'), (10, 'red_terracotta'), (13, 'red_sand')]))
    # ---- cactus, matorrales y huesos en la arena
    for _ in range(26):
        x, z = rnd.randint(2, W - 3), rnd.randint(6, L - 3)
        if v.get(x, 1, z) == 'air' and v.get(x, 0, z) in ('red_sand', 'sand'):
            libre = all(v.get(x + dx, y, z + dz) == 'air' for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)) for y in (1, 2, 3, 4))
            if rnd.random() < 0.55 and libre: v.fill(x, 1, z, x, rnd.randint(2, 4), z, 'cactus')
            else: v.set(x, 1, z, 'dead_bush')
    for (x, z) in [(CX - 7, CZ - 3), (CX + 8, CZ - 6), (CX - 9, CZ + 4)]: v.set(x, 1, z, 'bone_block'); v.set(x + 1, 1, z, 'bone_block')
    # ---- castillo de arena al fondo y altar con escalones
    castillo_arena(v, CX, CZ + 12, rnd)
    for k in range(3):
        v.fill(CX - 5 + k, 1 + k, CZ + 2 + k, CX + 5 - k, 1 + k, CZ + 6, 'smooth_red_sandstone' if k % 2 == 0 else 'red_sandstone')
        for x in range(CX - 5 + k, CX + 6 - k): v.stairs(x, 1 + k, CZ + 1 + k, 'red_sandstone', 'north')
    for (x, z) in [(CX - 11, CZ - 2), (CX + 11, CZ - 2), (CX - 8, CZ + 8), (CX + 8, CZ + 8)]:
        v.fill(x, 1, z, x, 3, z, 'red_sandstone_wall'); v.set(x, 4, z, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
    return v, [(CX, 4, CZ + 5, 180, 'emipokemon desafios alola kahuna_hapu')]
