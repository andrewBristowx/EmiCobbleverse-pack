"""Estadio de Wyndon (Leon, Campeon de Galar): estadio redondo de cuarzo con grandes arcos, gradas, torres de focos, pantalla gigante y una estatua de Charizard."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 71, 46, 74
CX, CZ = 35, 42
R = 27                       # radio del muro exterior
RP = 11                      # radio del campo


def charizard(v, cx, z, rnd):
    """estatua de Charizard de frente (mira a -z): cuerpo naranja, vientre crema, alas abiertas con membrana azul, cuernos y llama en la cola"""
    NA, CR, AZ = 'orange_concrete', 'yellow_terracotta', 'cyan_concrete'
    v.fill(cx - 6, 1, z - 3, cx + 6, 2, z + 7, 'polished_andesite')                      # peana
    for s in (-1, 1):
        v.fill(cx + s * 2 - 1, 3, z, cx + s * 2 + 1, 6, z + 2, NA)                         # piernas
        v.fill(cx + s * 2 - 1, 3, z - 1, cx + s * 2 + 1, 3, z - 1, 'black_concrete')         # garras
    v.fill(cx - 3, 7, z, cx + 3, 14, z + 3, NA); v.fill(cx - 2, 7, z - 1, cx + 2, 13, z - 1, CR)   # torso y vientre
    for s in (-1, 1):
        v.fill(cx + s * 4, 10, z + 1, cx + s * 5, 13, z + 2, NA); v.fill(cx + s * 5, 9, z, cx + s * 5, 9, z + 2, 'black_concrete')   # brazos
    v.fill(cx - 2, 15, z, cx + 2, 18, z + 3, NA); v.fill(cx - 1, 16, z - 2, cx + 1, 17, z - 1, NA)   # cabeza y hocico
    v.set(cx - 1, 17, z - 2, 'black_concrete'); v.set(cx + 1, 17, z - 2, 'black_concrete')
    for s in (-1, 1):
        v.set(cx + s * 2, 18, z + 1, 'blue_concrete'); v.fill(cx + s * 3, 17, z + 3, cx + s * 3, 19, z + 4, NA)   # ojos y cuernos
    pts = lambda s: [(cx + s * 4, 12), (cx + s * 14, 25), (cx + s * 11, 15), (cx + s * 13, 11), (cx + s * 8, 9), (cx + s * 5, 8)]
    for s in (-1, 1):
        poligono_xy(v, pts(s), z + 4, lambda x, y: AZ if (x + y) % 5 else NA)
        v.linea(cx + s * 4, 12, z + 4, cx + s * 14, 25, z + 4, NA); v.linea(cx + s * 4, 12, z + 4, cx + s * 13, 11, z + 4, NA)
    for k in range(5): v.fill(cx - 1, 7 + k // 2, z + 4 + k, cx + 1, 8 + k // 2, z + 4 + k, NA)                  # cola
    v.fill(cx - 1, 9, z + 9, cx + 1, 10, z + 9, 'orange_concrete'); v.fill(cx, 11, z + 9, cx, 13, z + 9, 'yellow_concrete'); v.set(cx, 14, z + 9, 'white_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(211)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_quartz')
    for z in range(0, CZ - R):
        for x in range(CX - 6, CX + 7): v.set(x, 0, z, 'red_concrete' if abs(x - CX) < 3 else 'white_concrete')
    for x in range(W):
        for z in range(0, CZ - R - 1):
            if abs(x - CX) > 8 and (x + z) % 4 == 0: v.set(x, 0, z, 'light_gray_concrete')
    # ---- muro exterior con arcos de dos plantas, pilastras y cornisas doradas
    v.cilindro(CX, 1, 20, CZ, R, 'quartz_bricks')
    v.disco(CX, 0, CZ, R, 'smooth_quartz')
    n = 28
    for y in range(1, 21):
        for x in range(CX - R - 1, CX + R + 2):
            for z in range(CZ - R - 1, CZ + R + 2):
                d = math.hypot(x - CX, z - CZ)
                if R - 1.0 < d <= R + 0.35:
                    a = (math.atan2(z - CZ, x - CX) % (2 * math.pi)) / (2 * math.pi) * n
                    f = abs(a - round(a))
                    if f < 0.22 and (3 <= y <= 8 or 12 <= y <= 17):
                        puerta = abs(x - CX) <= 5 and z < CZ - 20
                        v.set(x, y, z, 'air' if puerta or y in (3, 12) else 'light_blue_stained_glass')
                    elif y in (9, 10, 18, 19, 20):
                        v.set(x, y, z, 'red_concrete' if y in (9, 18) else 'quartz_block')
    v.anillo_circ(CX, 20, CZ, R, 'gold_block', 1.0)
    for k in range(n):
        a = (k + 0.5) / n * 2 * math.pi
        x = round(CX + math.cos(a) * (R - 0.5)); z = round(CZ + math.sin(a) * (R - 0.5))
        v.fill(x, 1, z, x, 20, z, 'quartz_pillar[axis=y]')
    # ---- torres de focos en el borde
    for k in range(8):
        a = k / 8 * 2 * math.pi + math.pi / 8
        x = round(CX + math.cos(a) * (R + 4)); z = round(CZ + math.sin(a) * (R + 4))
        if 0 < x < W - 1 and 0 < z < L - 1 and not (abs(x - CX) < 12 and z < CZ - R): foco_torre(v, x, z, 28, 'iron_bars', 'sea_lantern', 'quartz_bricks')
    # ---- gradas interiores (8 filas) con pasillo hacia la puerta y cubierta anular
    gradas_anillo(v, CX, CZ, RP + 1, 9, 'quartz_bricks', 'quartz', 1, hueco=lambda x, z: abs(x - CX) <= 3 and z < CZ - RP)
    for x in range(CX - R, CX + R + 1):
        for z in range(CZ - R, CZ + R + 1):
            d = math.hypot(x - CX, z - CZ)
            if 19 < d <= R - 1: v.set(x, 20, z, 'white_concrete')
    for k in range(48):                                                         # lamparas bajo la cubierta
        a = k / 48 * 2 * math.pi
        x = round(CX + math.cos(a) * 21); z = round(CZ + math.sin(a) * 21)
        v.set(x, 19, z, 'sea_lantern')
    # ---- campo de juego: cesped con franjas, lineas y el logo de la liga en el centro
    disco_f(v, CX, 1, CZ, RP, lambda d, a: 'lime_concrete' if int(d) % 2 else 'green_concrete')
    disco_f(v, CX, 1, CZ, RP, lambda d, a: 'white_concrete' if RP - 1 < d <= RP else None)
    disco_f(v, CX, 1, CZ, 6, lambda d, a: 'red_concrete' if 4.5 < d <= 6 else 'white_concrete' if 2.5 < d <= 4.5 else None)
    v.fill(CX - 6, 1, CZ, CX + 6, 1, CZ, 'black_concrete')
    disco_f(v, CX, 1, CZ, 2, lambda d, a: 'white_concrete' if d < 1.5 else 'black_concrete')
    for k in range(-4, 5): pass
    # ---- pantalla gigante en el lado de atras
    for x in range(CX - 12, CX + 13):
        for y in range(13, 19): v.set(x, y, CZ + 22, 'black_concrete')
    for x in range(CX - 11, CX + 12):
        for y in range(14, 18):
            if (x * 7 + y * 3) % 5 < 2: v.set(x, y, CZ + 22, rnd.choice(['light_blue_concrete', 'cyan_concrete', 'white_concrete']))
    # ---- tunel de entrada: boca de piedra con la estatua de Charizard delante
    zf = CZ - R
    for x in range(CX - 4, CX + 5):
        for y in range(1, 9):
            if abs(x - CX) < 4: v.aire(x, y, zf - 1, x, y, zf + 2)
    for dx in (-5, 5): v.fill(CX + dx, 1, zf - 1, CX + dx, 12, zf + 1, 'gold_block')
    v.fill(CX - 5, 12, zf - 1, CX + 5, 13, zf + 1, 'gold_block')
    charizard(v, CX, 6, rnd)
    for dx in (-11, 11):
        v.fill(CX + dx, 1, 8, CX + dx, 12, 8, 'iron_bars'); v.fill(CX + dx + 1, 8, 8, CX + dx + 3, 12, 8, 'red_wool')
        v.set(CX + dx, 13, 8, 'gold_block')
    # ---- estrado de campeon en el circulo central
    return v, [(CX, 2, CZ, 180, 'emipokemon desafios galar champion_leon')]
