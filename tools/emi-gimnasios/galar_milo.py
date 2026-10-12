"""Estadio de Turffield (Milo, Planta): granero-estadio rojo y blanco con un molino de viento, balas de heno y un campo de cesped."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 43, 40, 46
CX = 21
X0, X1 = 7, 37
Z0, Z1 = 12, 43


def molino(v, cx, cz, alto):
    """molino: torre troncoconica de piedra y cal, tejadillo de madera y cuatro aspas con velas de lana"""
    for y in range(1, alto + 1):
        r = 4.2 - y * 0.09
        for x in range(cx - 5, cx + 6):
            for z in range(cz - 5, cz + 6):
                d = math.hypot(x - cx, z - cz)
                if r - 1.1 < d <= r: v.set(x, y, z, 'stone_bricks' if y < 4 else 'white_concrete')
        v.set(cx, y, cz, 'air')
    cono(v, cx, alto + 1, cz, 4, 6, 'dark_oak_planks')
    hy = alto - 3; hz = cz - 4
    v.fill(cx - 1, hy - 1, hz, cx + 1, hy + 1, hz, 'stripped_oak_log[axis=z]')
    for k, ang in enumerate((20, 110, 200, 290)):
        a = math.radians(ang)
        ex, ey = cx + math.cos(a) * 11, hy + math.sin(a) * 11
        v.linea(cx, hy, hz - 1, round(ex), round(ey), hz - 1, 'oak_fence')
        for t in range(3, 11):                                                          # vela: tira de lana al lado de cada aspa
            px = cx + math.cos(a) * t; py = hy + math.sin(a) * t
            nx, ny = -math.sin(a), math.cos(a)
            for w in (1, 2):
                v.set(round(px + nx * w), round(py + ny * w), hz - 1, 'white_wool')
    v.set(cx, alto + 8, cz, 'lightning_rod[facing=up,powered=false,waterlogged=false]')
    for y in (4, 9, 14):
        for (dx, dz) in ((0, -3), (3, 0), (-3, 0)): v.set(cx + dx, y, cz + dz, 'glass_pane')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(131)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'grass_block')
    for z in range(0, Z0):
        for x in range(CX - 4, CX + 5): v.set(x, 0, z, 'dirt_path')
    # vallas y setos, un seto con forma de bola
    for x in range(1, W - 1):
        if abs(x - CX) > 6: v.set(x, 1, 2, 'oak_fence'); v.set(x, 1, 10, 'oak_leaves'); v.set(x, 2, 10, 'oak_leaves') if rnd.random() < 0.4 else None
    for z in range(2, 10):
        v.set(1, 1, z, 'oak_fence'); v.set(W - 2, 1, z, 'oak_fence')
    for (x, z) in [(CX - 12, 6), (CX + 12, 6)]: v.esfera(x, 2, z, 2, 'oak_leaves')
    for _ in range(60):
        x, z = rnd.randint(1, W - 2), rnd.randint(1, 9)
        if v.get(x, 1, z) == 'air' and abs(x - CX) > 5: v.set(x, 1, z, rnd.choice(['dandelion', 'poppy', 'short_grass', 'fern', 'oxeye_daisy']))
    molino(v, 5, 6, 20)
    # balas de heno junto a la puerta
    for (x, y, z) in [(CX + 8, 1, 6), (CX + 10, 1, 6), (CX + 9, 1, 6), (CX + 9, 2, 6)]: v.set(x, y, z, 'hay_block[axis=x]')
    # ---- granero: muros rojos con zocalo de piedra y vigas blancas
    v.box(X0, 1, Z0, X1, 10, Z1, 'red_terracotta', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 10, Z1 - 1)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'moss_block')
    for y in (1, 2): v.anillo(X0, Z0, X1, Z1, y, 'stone_bricks')
    for z in range(Z0, Z1 + 1, 5): v.fill(X0, 3, z, X0, 10, z, 'white_concrete'); v.fill(X1, 3, z, X1, 10, z, 'white_concrete')
    for x in range(X0, X1 + 1, 5): v.fill(x, 3, Z1, x, 10, Z1, 'white_concrete')
    v.anillo(X0, Z0, X1, Z1, 10, 'white_concrete')
    for z in range(Z0 + 2, Z1 - 1, 5):
        for y in (5, 6): v.set(X0, y, z, 'glass_pane'); v.set(X0, y, z + 1, 'glass_pane'); v.set(X1, y, z, 'glass_pane'); v.set(X1, y, z + 1, 'glass_pane')
    # tejado a dos aguas con buhardilla
    cum = techo_dos_aguas(v, X0 - 1, X1 + 1, Z0 - 1, Z1 + 1, 11, 'deepslate_tiles', 'deepslate_tile', 'z')
    v.fill(X0 + 1, 10, Z0 + 1, X1 - 1, 10, Z1 - 1, 'dark_oak_planks')
    # hastial de la fachada: madera con aspa y ventana del pajar
    for k in range(0, (X1 - X0 + 3) // 2 + 1):
        for zz in (Z0, Z1): v.fill(CX - (X1 - X0) // 2 - 1 + k, 11 + k, zz, CX + (X1 - X0) // 2 + 1 - k, 11 + k, zz, 'red_terracotta')
    v.aire(CX - 2, 14, Z0, CX + 2, 17, Z0)
    for dx in range(-2, 3): v.set(CX + dx, 14, Z0, 'hay_block[axis=x]'); v.set(CX + dx, 18, Z0, 'white_concrete')
    for dx in (-3, 3):
        for y in range(14, 19): v.set(CX + dx, y, Z0, 'white_concrete')
    # puerta grande con aspas
    hueco_puerta(v, CX, Z0, 9, 8, 'white_concrete', y0=2)
    for k in range(9):
        v.set(CX - 4 + k, 2 + k * 7 // 8, Z0 - 1, 'white_concrete'); v.set(CX + 4 - k, 2 + k * 7 // 8, Z0 - 1, 'white_concrete')
    for y in range(2, 10): v.set(CX - 5, y, Z0 - 1, 'white_concrete'); v.set(CX + 5, y, Z0 - 1, 'white_concrete')
    # ---- interior: gradas de madera a ambos lados (suben hacia las paredes) y campo con franjas segadas
    for i in range(4):
        for z in range(Z0 + 6, Z1 - 5):
            if i > 0:
                v.fill(X0 + 4 - i + 1, 1, z, X0 + 4 - i + 1, i, z, 'oak_planks'); v.fill(X1 - 4 + i - 1, 1, z, X1 - 4 + i - 1, i, z, 'oak_planks')
            v.stairs(X0 + 4 - i + 1, 1 + i, z, 'oak', 'east')
            v.stairs(X1 - 4 + i - 1, 1 + i, z, 'oak', 'west')
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 6, X1 - 5): v.set(x, 1, z, 'moss_block' if (z // 3) % 2 else 'grass_block')
    v.anillo(X0 + 7, Z0 + 8, X1 - 7, Z1 - 8, 1, 'white_concrete')
    disco_f(v, CX, 1, 28, 4, lambda d, a: 'white_concrete' if 3 < d <= 4 else None)
    for (x, z) in [(CX - 4, 14), (CX + 4, 14), (CX - 4, 40), (CX + 4, 40), (CX - 8, 26), (CX + 8, 26)]:
        v.fill(x, 2, z, x + 1, 2, z, 'hay_block[axis=x]'); v.set(x, 3, z, 'hay_block[axis=x]')
    # vigas y faroles colgando
    for z in range(Z0 + 3, Z1, 4):
        v.fill(X0 + 1, 9, z, X1 - 1, 9, z, 'stripped_oak_log[axis=x]')
        for x in (CX - 8, CX, CX + 8):
            for k in range(1, 3): v.set(x, 9 - k, z, 'chain[axis=y]')
            v.set(x, 6, z, 'lantern[hanging=true]')
    # ---- estrado del lider: balas de heno y un gran emblema de flor en la pared del fondo
    v.fill(CX - 6, 2, Z1 - 7, CX + 6, 2, Z1 - 1, 'hay_block[axis=x]')
    v.fill(CX - 4, 3, Z1 - 6, CX + 4, 3, Z1 - 1, 'hay_block[axis=x]')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 8, 'oak', 'north')
    elipse_xy(v, CX, 8, Z1, 5, 5, lambda d, a: 'yellow_concrete' if d < 0.28 else 'lime_concrete' if abs(math.sin(a * 4)) > 0.55 else 'green_concrete')
    for y in range(3, 6): v.set(CX, y, Z1, 'green_concrete')
    return v, [(CX, 4, Z1 - 4, 180, 'emipokemon desafios galar hierba')]
