"""Gimnasio de Lumiose (Clemont, Electrico): taller de inventor con engranaje gigante, bobinas Tesla, un robot y chimeneas."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 37, 36, 42
CX = 18
X0, X1 = 4, 32
Z0, Z1 = 8, 39


def engranaje(v, cx, cy, z, r, dientes, mat, hueco, centro, giro=0.0):
    """engranaje en el plano XY: cuerpo de radio r con `dientes` dientes, 6 radios y un agujero con luz en el centro"""
    for x in range(int(cx - r) - 3, int(cx + r) + 4):
        for y in range(int(cy - r) - 3, int(cy + r) + 4):
            d = math.hypot(x - cx, y - cy)
            a = math.atan2(y - cy, x - cx) + giro
            diente = (math.sin(a * dientes) > 0.1)
            if d <= r - 0.5 or (d <= r + 1.5 and diente and d > r - 1):
                if d < 2.2: v.set(x, y, z, centro)
                elif d < r - 3.2 and abs(math.sin(a * 3)) < 0.88: v.set(x, y, z, hueco)
                else: v.set(x, y, z, mat)


def bobina(v, x, y, z, alto):
    v.fill(x, y, z, x, y + alto - 1, z, 'waxed_copper_block')
    v.set(x, y + alto, z, 'copper_bulb[lit=true,powered=false]')
    v.set(x, y + alto + 1, z, 'lightning_rod[facing=up,powered=false,waterlogged=false]')
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)): v.set(x + dx, y, z + dz, 'waxed_cut_copper')


def robot(v, cx, y, z):
    """Clembot: torso de hierro, cabeza amarilla con visor azul, brazos con pinzas y mochila con rayo"""
    v.fill(cx - 2, y + 4, z - 1, cx + 2, y + 8, z + 1, 'iron_block')
    v.fill(cx - 3, y + 4, z - 1, cx - 3, y + 8, z, 'light_gray_concrete'); v.fill(cx + 3, y + 4, z - 1, cx + 3, y + 8, z, 'light_gray_concrete')
    v.fill(cx - 1, y, z, cx - 1, y + 3, z, 'gray_concrete'); v.fill(cx + 1, y, z, cx + 1, y + 3, z, 'gray_concrete')
    v.fill(cx - 2, y, z - 1, cx - 1, y, z, 'black_concrete'); v.fill(cx + 1, y, z - 1, cx + 2, y, z, 'black_concrete')
    v.fill(cx - 1, y + 9, z - 1, cx + 1, y + 11, z + 1, 'yellow_concrete')
    v.fill(cx - 1, y + 10, z - 2, cx + 1, y + 10, z - 2, 'cyan_concrete')
    v.set(cx, y + 12, z, 'lightning_rod[facing=up,powered=false,waterlogged=false]')
    v.fill(cx - 4, y + 3, z - 1, cx - 4, y + 4, z - 1, 'iron_block'); v.fill(cx + 4, y + 3, z - 1, cx + 4, y + 4, z - 1, 'iron_block')
    v.set(cx, y + 6, z - 2, 'yellow_concrete'); v.set(cx, y + 7, z - 2, 'yellow_concrete'); v.set(cx - 1, y + 5, z - 2, 'yellow_concrete'); v.set(cx + 1, y + 5, z - 2, 'yellow_concrete')
    v.fill(cx - 2, y + 5, z + 2, cx + 2, y + 8, z + 2, 'copper_block')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(81)
    LAD, HIE, COB, AMA = 'bricks', 'iron_block', 'waxed_copper_block', 'yellow_concrete'
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_stone')
    for z in range(0, Z0):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'yellow_concrete' if (z % 2 == 0 and abs(x - CX) == 3) else 'gray_concrete')
    # ---- nave del taller
    v.box(X0, 1, Z0, X1, 17, Z1, LAD, interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 17, Z1 - 1)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'smooth_stone')
    for y in (4, 10, 16): v.anillo(X0, Z0, X1, Z1, y, 'waxed_cut_copper')            # bandas de cobre
    for x in range(X0 + 2, X1, 4):                                                    # pilares de hierro
        v.fill(x, 1, Z0, x, 17, Z0, 'polished_blackstone'); v.fill(x, 1, Z1, x, 17, Z1, 'polished_blackstone')
    for z in range(Z0 + 2, Z1, 4):
        v.fill(X0, 1, z, X0, 17, z, 'polished_blackstone'); v.fill(X1, 1, z, X1, 17, z, 'polished_blackstone')
    for z in range(Z0 + 3, Z1 - 1, 4):                                               # ventanas con rejilla
        for y in (6, 7, 8): v.set(X0, y, z, 'iron_bars'); v.set(X1, y, z, 'iron_bars')
        for y in (12, 13, 14): v.set(X0, y, z, 'glass_pane'); v.set(X1, y, z, 'glass_pane')
    # cubierta plana con claraboyas y tuberias
    v.fill(X0, 17, Z0, X1, 17, Z1, 'polished_blackstone_bricks')
    for z in range(Z0 + 4, Z1 - 3, 7):
        v.fill(CX - 6, 17, z, CX + 6, 17, z + 2, 'glass')
    # ---- chimeneas y bobinas Tesla en el tejado
    for (x, z, h) in [(7, 12, 10), (29, 12, 12), (7, 33, 11), (29, 33, 9)]:
        v.cilindro(x, 18, 18 + h, z, 2, 'bricks'); v.disco(x, 18 + h, z, 2, 'black_concrete')
        v.anillo_circ(x, 17 + h, z, 2, 'iron_block', 1.0)
    for (x, z) in [(CX - 8, 20), (CX + 8, 20), (CX - 8, 30), (CX + 8, 30)]: bobina(v, x, 18, z, 4)
    # ---- torre de la fachada con el engranaje gigante
    v.box(CX - 8, 18, Z0, CX + 8, 31, Z0 + 6, LAD, interior=None)
    v.fill(CX - 7, 18, Z0 + 1, CX + 7, 31, Z0 + 5, 'air')
    v.fill(CX - 9, 32, Z0 - 1, CX + 9, 32, Z0 + 7, 'waxed_cut_copper')
    v.fill(CX - 8, 19, Z0, CX + 8, 31, Z0, 'smooth_stone')
    engranaje(v, CX, 25, Z0, 6.5, 10, 'iron_block', 'light_gray_concrete', 'yellow_concrete')
    bobina(v, CX - 8, 33, Z0 + 1, 3); bobina(v, CX + 8, 33, Z0 + 1, 3)
    for dx in range(-3, 4): v.set(CX + dx, 33, Z0 + 3, 'lightning_rod[facing=up,powered=false,waterlogged=false]') if abs(dx) % 3 == 0 else None
    # entrada: puerta de taller con chapa
    hueco_puerta(v, CX, Z0, 5, 7, 'iron_block', y0=1, prof=1)
    for dx in (-2, 2): v.fill(CX + dx, 1, Z0 - 1, CX + dx, 3, Z0 - 1, 'yellow_concrete')
    v.fill(CX - 2, 8, Z0 - 1, CX + 2, 8, Z0 - 1, 'yellow_concrete')
    # ---- interior: suelo con franjas de peligro y un engranaje en el centro
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1):
            if (x + z) % 2 == 0 and rnd.random() < 0.3: v.set(x, 1, z, 'polished_andesite')
    for z in range(Z0 + 1, Z1):
        v.set(CX - 3, 1, z, 'yellow_concrete'); v.set(CX + 3, 1, z, 'yellow_concrete')
    disco_f(v, CX, 1, 25, 9, lambda d, a: ('yellow_concrete' if 8 < d <= 9 else 'black_concrete' if 6 < d <= 7 else 'light_gray_concrete' if d <= 2 else 'waxed_cut_copper') if (d > 7.5 or int(a * 6 / math.pi + 20) % 2 == 0 or d < 3) else 'gray_concrete')
    for k in range(10):
        a = k / 10 * 2 * math.pi
        x = round(CX + math.cos(a) * 9.8); z = round(25 + math.sin(a) * 9.8); v.set(x, 1, z, 'yellow_concrete')
    # tuberias del techo y focos
    for z in range(Z0 + 2, Z1 - 1, 6):
        v.fill(X0 + 1, 16, z, X1 - 1, 16, z, 'waxed_oxidized_copper')
        for x in range(X0 + 4, X1 - 3, 6):
            for k in range(1, 3): v.set(x, 16 - k, z, 'chain[axis=y]')
            v.set(x, 13, z, 'sea_lantern')
    # bancos de trabajo con herramientas, bobinas y el robot
    for (x, z) in [(7, 12), (7, 17), (29, 12), (29, 17), (7, 33), (29, 33)]:
        v.fill(x - 1, 2, z, x + 1, 2, z + 1, 'polished_blackstone')
        v.set(x, 3, z, rnd.choice(['anvil[facing=north]', 'crafting_table', 'smithing_table', 'grindstone[face=floor,facing=north]', 'cauldron']))
        v.set(x - 1, 3, z + 1, 'lantern') ; v.set(x + 1, 3, z, 'copper_bulb[lit=true,powered=false]')
    for (x, z) in [(CX - 12, 22), (CX + 12, 22), (CX - 12, 28), (CX + 12, 28)]: bobina(v, x, 2, z, 5)
    robot(v, CX - 12, 2, 36)
    robot(v, CX + 12, 2, 36)
    # ---- estrado del lider: plataforma de rejilla con barandilla amarilla
    v.fill(CX - 6, 2, Z1 - 8, CX + 6, 2, Z1 - 1, 'polished_blackstone_bricks')
    v.fill(CX - 4, 3, Z1 - 7, CX + 4, 3, Z1 - 1, 'iron_block')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 9, 'stone_brick', 'north') if abs(x - CX) <= 2 else v.set(x, 2, Z1 - 9, 'yellow_concrete')
    for x in range(CX - 4, CX + 5): v.stairs(x, 3, Z1 - 8, 'polished_blackstone_brick', 'north') if abs(x - CX) <= 1 else v.set(x, 3, Z1 - 8, 'iron_bars')
    # Pantalla/cuadro de mandos del fondo: lamparas
    for dx in range(-5, 6, 2):
        for dy in (6, 8): v.set(CX + dx, dy, Z1 - 1, 'sea_lantern' if (dx + dy) % 4 else 'yellow_concrete')
    return v, [(CX, 4, Z1 - 4, 180, 'emipokemon desafios kalos voltaje')]
