import math
from gymlib import V
from piezas import *

W, H, L = 35, 34, 38
CX, CZ, R = 17, 23, 14

def hex_color(x, z, a, b, c, tam=3.0):
    """celda hexagonal (rejilla de hexagonos de radio ~tam) -> uno de tres bloques"""
    q = (math.sqrt(3) / 3 * (x - CX) - (z - CZ) / 3) / tam
    r = ((2 / 3) * (z - CZ)) / tam
    # redondeo axial
    xx, zz = q, r; yy = -xx - zz
    rx, ry, rz = round(xx), round(yy), round(zz)
    dx, dy, dz = abs(rx - xx), abs(ry - yy), abs(rz - zz)
    if dx > dy and dx > dz: rx = -ry - rz
    elif dy > dz: ry = -rx - rz
    else: rz = -rx - ry
    return [a, b, c][(rx - rz) % 3]

def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    # explanada y senda
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'moss_block')
    for z in range(0, CZ - R + 1):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'mossy_stone_bricks' if (x + z) % 3 else 'stone_bricks')
    v.disco(CX, 0, CZ, R + 2, 'mossy_stone_bricks')
    # suelo hexagonal (interior) y cilindro
    for x in range(CX - R, CX + R + 1):
        for z in range(CZ - R, CZ + R + 1):
            if math.hypot(x - CX, z - CZ) <= R + 0.35: v.set(x, 1, z, hex_color(x, z, 'yellow_terracotta', 'orange_terracotta', 'honeycomb_block'))
    v.cilindro(CX, 2, 11, CZ, R, 'orange_terracotta')
    # banda de colores del muro
    v.anillo_circ(CX, 6, CZ, R, 'yellow_concrete', grosor=1.0)
    v.anillo_circ(CX, 7, CZ, R, 'green_concrete', grosor=1.0)
    v.anillo_circ(CX, 11, CZ, R, 'black_concrete', grosor=1.0)
    # pilares: 12 columnas de madera de acacia oscura alrededor
    for i in range(12):
        a = i * math.pi / 6
        x = int(round(CX + math.cos(a) * R)); z = int(round(CZ + math.sin(a) * R))
        v.fill(x, 2, z, x, 11, z, 'stripped_acacia_log[axis=y]')
    # cupula de panal con ventanas hexagonales de cristal naranja
    v.cupula(CX, 12, CZ, R, 'honeycomb_block', hueco=True, ry=13)
    rnd = seed(4)
    for (x, y, z), b in list(v.g.items()):
        if y >= 12 and b == 'honeycomb_block':
            h = math.sqrt(((x - CX) / R) ** 2 + ((z - CZ) / R) ** 2)
            if hex_color(x * 1.0, z * 1.0 + y * 1.3, 'a', 'b', 'c', 2.6) == 'a' and 0.35 < h < 0.95 and y < 12 + 10:
                v.g[(x, y, z)] = 'orange_stained_glass'
    # anillos de la cupula en verde (hojas)
    for yy in (12, 17, 21):
        for (x, y, z), b in list(v.g.items()):
            if y == yy and b == 'honeycomb_block': v.g[(x, y, z)] = 'green_terracotta' if yy != 17 else 'yellow_concrete'
    # antenas de insecto en la cima: dos arcos negros con luz
    top = 12 + 13
    for s in (-1, 1):
        for i in range(10):
            xx = CX + s * (1 + i // 2); yy = top + i * 0.9; v.set(xx, int(yy), CZ, 'black_concrete')
        v.set(CX + s * 6, top + 9, CZ, 'shroomlight'); v.set(CX + s * 6, top + 8, CZ, 'black_concrete')
    v.set(CX, top, CZ, 'honeycomb_block'); v.set(CX, top + 1, CZ, 'black_concrete')
    # nidos de abeja decorativos en la fachada (a ambos lados de la entrada) y matorrales
    for dx in (-8, 8):
        for dy in (3, 5, 7): v.set(CX + dx, dy, CZ - R - 1, 'bee_nest[facing=north,honey_level=0]')
        v.fill(CX + dx - 1, 1, CZ - R - 2, CX + dx + 1, 1, CZ - R - 2, 'azalea_leaves')
        v.set(CX + dx, 2, CZ - R - 2, 'flowering_azalea')
    # entrada hexagonal 5x6
    for y in range(2, 8):
        ancho = 2 if y < 6 else (2 if y == 6 else 1)
        for x in range(CX - ancho, CX + ancho + 1):
            for z in range(CZ - R - 1, CZ - R + 3): v.set(x, y, z, 'air')
    for y in range(1, 9):
        a = 3 if y < 6 else 2 if y == 6 else 1
        for z in range(CZ - R - 1, CZ - R + 1):
            v.set(CX - a, y, z, 'stripped_acacia_log[axis=y]') if y < 6 else v.set(CX - a, y, z, 'black_concrete')
            v.set(CX + a, y, z, 'stripped_acacia_log[axis=y]') if y < 6 else v.set(CX + a, y, z, 'black_concrete')
    v.fill(CX - 1, 7, CZ - R, CX + 1, 8, CZ - R, 'black_concrete') if False else None
    for z in range(CZ - R - 3, CZ - R + 1):
        for x in range(CX - 3, CX + 4): v.stairs(x, 1, CZ - R - 3 + (CZ - R - z) * 0, 'mossy_stone_brick', 'north') if False else None
    for i, z in enumerate((CZ - R - 3, CZ - R - 2, CZ - R - 1)):
        for x in range(CX - 3, CX + 4): v.stairs(x, 1, z, 'mossy_stone_brick', 'north')
    # interior: gradas circulares (dos escalones) con sillas
    for r_, y in ((R - 1, 2), (R - 2, 3)):
        for x in range(CX - r_ - 1, CX + r_ + 2):
            for z in range(CZ - r_ - 1, CZ + r_ + 2):
                d = math.hypot(x - CX, z - CZ)
                if r_ - 1 < d <= r_ + 0.35:
                    ang = math.atan2(z - CZ, x - CX)
                    # abrir un pasillo hacia la puerta y hacia el lider
                    if abs(math.degrees(ang) + 90) < 14: continue
                    f = 'east' if abs(x - CX) >= abs(z - CZ) and x < CX else 'west' if abs(x - CX) >= abs(z - CZ) else 'south' if z < CZ else 'north'
                    v.fill(x, 2, z, x, y - 1, z, 'stripped_acacia_log[axis=y]') if y > 2 else None
                    v.stairs(x, y, z, 'acacia', f)
    # arena: anillo de hojas en el suelo y centro con flor
    v.anillo_circ(CX, 1, CZ, 8, 'black_concrete', grosor=1.0)
    v.anillo_circ(CX, 1, CZ, 4, 'green_concrete', grosor=1.0)
    v.disco(CX, 1, CZ, 2, 'honey_block') if False else v.disco(CX, 1, CZ, 2, 'yellow_concrete')
    # lamparas de gota colgantes del interior de la cupula
    for (dx, dz) in [(0, 0), (6, 0), (-6, 0), (0, 6), (0, -6), (4, 4), (-4, 4), (4, -4), (-4, -4)]:
        lampara_colgante(v, CX + dx, 22 - (abs(dx) + abs(dz)) // 3, CZ + dz, 4 + (abs(dx) + abs(dz)) // 3, 'glowstone')
    # podio del lider (sala trasera, redonda)
    v.disco(CX, 2, CZ + 9, 4, 'stripped_acacia_log[axis=y]')
    v.disco(CX, 3, CZ + 9, 2, 'honeycomb_block')
    for i in range(3): v.set(CX - 1 + i, 4, CZ + 11, 'acacia_slab[type=top]') if False else None
    v.fill(CX - 2, 4, CZ + 11, CX + 2, 5, CZ + 11, 'acacia_planks')
    v.set(CX, 6, CZ + 11, 'beehive[facing=north]')
    # luces en el muro
    for i in range(12):
        a = i * math.pi / 6 + math.pi / 12
        x = int(round(CX + math.cos(a) * (R - 1))); z = int(round(CZ + math.sin(a) * (R - 1)))
        if math.hypot(x - CX, z - (CZ - R)) > 3: v.set(x, 8, z, 'shroomlight')
    return v, [(CX, 4, CZ + 9, 180, 'emipokemon desafios teselia insecto')]
