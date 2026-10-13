import math
from gymlib import V
from piezas import *

W, H, L = 37, 34, 40
CX, CZ = 18, 23

def cono(v, cx, y0, cz, r, h, bloque, bloque2=None, rnd=None):
    for y in range(h + 1):
        rr = r * (1 - y / h) + 0.4
        for x in range(int(cx - rr) - 1, int(cx + rr) + 2):
            for z in range(int(cz - rr) - 1, int(cz + rr) + 2):
                d = math.hypot(x - cx, z - cz)
                if d <= rr:
                    b = bloque
                    if bloque2 and rnd and rnd.random() < 0.25: b = bloque2
                    v.set(x, y0 + y, z, b)

def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(33)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'snow_block')
    for x in range(W):
        for z in range(L):
            if rnd.random() < 0.08: v.set(x, 0, z, 'packed_ice')
    # masa base: tres terrazas de nieve y hielo
    v.disco(CX, 1, CZ, 17, 'snow_block'); v.disco(CX, 2, CZ, 15, 'packed_ice'); v.disco(CX, 3, CZ, 13, 'snow_block')
    for y in range(4, 11): v.disco(CX, y, CZ, 13 - (y - 3) // 2, 'packed_ice' if y % 3 else 'blue_ice')
    # agujas: una grande en el centro y seis alrededor
    cono(v, CX, 11, CZ, 8, 22, 'blue_ice', 'packed_ice', rnd)
    for i in range(7):
        a = i * 2 * math.pi / 7 + 0.3
        x = int(round(CX + math.cos(a) * 11.5)); z = int(round(CZ + math.sin(a) * 11.5))
        cono(v, x, 4, z, 3.5 + rnd.random() * 1.5, 11 + int(rnd.random() * 9), 'packed_ice', 'ice', rnd)
    for i in range(12):
        a = i * 2 * math.pi / 12 + 0.1
        x = int(round(CX + math.cos(a) * 16)); z = int(round(CZ + math.sin(a) * 16))
        if abs(x - CX) < 5 and z < CZ: continue
        cono(v, x, 1, z, 2.2, 5 + int(rnd.random() * 4), 'ice', 'blue_ice', rnd)
    # caverna interior: cupula hueca (carcasa de hielo azul)
    v.cupula(CX, 2, CZ, 11, 'blue_ice', hueco=True, ry=10)
    # suelo de la caverna: hielo compacto con dibujo
    for x in range(CX - 11, CX + 12):
        for z in range(CZ - 11, CZ + 12):
            d = math.hypot(x - CX, z - CZ)
            if d <= 10.2:
                v.set(x, 1, z, 'blue_ice' if d < 2.5 else 'light_blue_concrete' if 4 < d < 5 else 'white_concrete' if 7 < d < 8 else 'packed_ice')
    v.anillo_circ(CX, 1, CZ, 9.5, 'cyan_concrete', 1.0) if False else None
    # copo de nieve en el centro del suelo (pixel art)
    for (dx, dz) in [(0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2), (1, 1), (-1, -1), (1, -1), (-1, 1)]: v.set(CX + dx, 1, CZ + dz, 'light_blue_concrete')
    # luces: sea lanterns incrustadas en la cascara
    for i in range(20):
        a = i * 2 * math.pi / 20; el = 20 + (i % 4) * 14
        r = 10.3
        x = int(round(CX + math.cos(a) * r * math.cos(math.radians(el)))); z = int(round(CZ + math.sin(a) * r * math.cos(math.radians(el)))); y = 2 + int(round(math.sin(math.radians(el)) * 9.5))
        if v.get(x, y, z) == 'blue_ice': v.set(x, y, z, 'sea_lantern')
    # carambanos del techo
    for _ in range(70):
        a = rnd.random() * 6.28; rr = rnd.random() * 9
        x = int(CX + math.cos(a) * rr); z = int(CZ + math.sin(a) * rr)
        for y in range(12, 3, -1):
            if v.get(x, y, z) == 'blue_ice' and v.get(x, y - 1, z) == 'air':
                for k in range(1, 2 + int(rnd.random() * 3)):
                    if v.get(x, y - k, z) == 'air': v.set(x, y - k, z, 'ice' if k % 2 else 'packed_ice')
                break
    # entrada: arco de hielo hacia el frente (-z)
    for z in range(0, CZ - 7):
        for x in range(CX - 3, CX + 4):
            for y in range(1, 7): 
                if z >= 4: v.set(x, y, z, 'air')
    for z in range(4, CZ - 7):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'packed_ice')
    for z in range(4, CZ - 9, 1):
        v.set(CX - 4, 1, z, 'blue_ice'); v.set(CX + 4, 1, z, 'blue_ice')
    for x in range(CX - 4, CX + 5):
        for y in range(1, 9):
            for z in (3, 4):
                if abs(x - CX) >= 4 or y >= 7: v.set(x, y, z, 'blue_ice')
    v.aire(CX - 2, 1, 3, CX + 2, 5, 4)
    # carambanos del arco
    for x in range(CX - 3, CX + 4, 2):
        v.set(x, 6, 3, 'ice'); v.set(x, 5, 3, 'ice') if x % 4 else None
    for z in range(0, 4):
        for x in range(CX - 3, CX + 4): v.stairs(x, 1, z, 'quartz', 'north') if z > 0 else None
    # gradas de hielo en un semicirculo y trono para el lider
    for r_, y in ((9, 2), (8, 3)):
        for x in range(CX - 10, CX + 11):
            for z in range(CZ - 3, CZ + 11):
                d = math.hypot(x - CX, z - CZ)
                if r_ - 1 < d <= r_ + 0.35 and z > CZ + 2:
                    v.fill(x, 2, z, x, y, z, 'packed_ice') if y == 3 else None
                    v.stairs(x, y, z, 'stone_brick' if False else 'quartz', 'north')
    # trono del lider: escalones y respaldo de hielo
    for x in range(CX - 4, CX + 5): v.stairs(x, 2, CZ + 6, 'quartz', 'north') if False else None
    v.fill(CX - 4, 2, CZ + 6, CX + 4, 2, CZ + 8, 'snow_block')
    v.fill(CX - 2, 3, CZ + 7, CX + 2, 3, CZ + 8, 'packed_ice')
    for x in range(CX - 4, CX + 5): v.stairs(x, 2, CZ + 5, 'quartz', 'north')
    for x in range(CX - 2, CX + 3): v.stairs(x, 3, CZ + 6, 'quartz', 'north')
    v.fill(CX - 1, 4, CZ + 8, CX + 1, 8, CZ + 8, 'blue_ice'); v.fill(CX, 9, CZ + 8, CX, 11, CZ + 8, 'blue_ice')
    v.set(CX - 2, 5, CZ + 8, 'ice'); v.set(CX + 2, 5, CZ + 8, 'ice')
    # pilares de hielo con luz en la sala
    for (dx, dz) in [(-6, -3), (6, -3), (-6, 3), (6, 3)]:
        v.fill(CX + dx, 2, CZ + dz, CX + dx, 6, CZ + dz, 'blue_ice'); v.set(CX + dx, 7, CZ + dz, 'sea_lantern')
    return v, [(CX, 4, CZ + 7, 180, 'emipokemon desafios teselia frio')]
