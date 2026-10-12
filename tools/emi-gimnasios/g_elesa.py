import math
from gymlib import V
from piezas import *

W, H, L = 35, 34, 38
CX = 17
X0, X1 = 2, 32
Z0, Z1 = 7, 37

def anillo_vertical_xy(v, cx, cy, z, r, bloque, grosor=1.0):
    for x in range(cx - r - 1, cx + r + 2):
        for y in range(cy - r - 1, cy + r + 2):
            d = math.hypot(x - cx, y - cy)
            if r - grosor < d <= r + 0.35: v.set(x, y, z, bloque)

def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    NEG, AMA, CIA, COB = 'black_concrete', 'yellow_concrete', 'cyan_concrete', 'waxed_copper_block'
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'polished_blackstone')
    # franjas de neon en el suelo de la explanada
    for z in range(0, Z0 - 1):
        v.fill(CX - 4, 0, z, CX - 4, 0, z, AMA); v.fill(CX + 4, 0, z, CX + 4, 0, z, AMA)
        v.fill(CX - 2, 0, z, CX + 2, 0, z, 'gray_concrete')
    # cuerpo: muros negros con tiras verticales de neon
    v.box(X0, 1, Z0, X1, 8, Z1, NEG)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'gray_concrete')
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 8, Z1 - 1)
    for x in range(X0 + 2, X1, 4):
        for y in range(2, 9):
            v.set(x, y, Z0, 'sea_lantern' if y % 2 == 0 else CIA); v.set(x, y, Z1, 'sea_lantern' if y % 2 == 0 else CIA)
    for z in range(Z0 + 3, Z1 - 1, 4):
        for y in range(2, 9):
            v.set(X0, y, z, 'sea_lantern' if y % 2 == 0 else AMA); v.set(X1, y, z, 'sea_lantern' if y % 2 == 0 else AMA)
    for y in (2, 8): v.anillo(X0, Z0, X1, Z1, y, AMA)
    # piramide de cristal sobre el cuerpo: capas que se estrechan 2 bloques por cada altura
    k = 0
    while True:
        xa, xb, za, zb = X0 + 2 * k, X1 - 2 * k, Z0 + 2 * k, Z1 - 2 * k
        y = 9 + k
        if xb - xa < 2 or zb - za < 2: break
        v.anillo(xa, za, xb, zb, y, 'cyan_stained_glass')
        if xb - xa > 3 and zb - za > 3: v.anillo(xa + 1, za + 1, xb - 1, zb - 1, y, 'cyan_stained_glass')
        # nervios de cobre en cada 6 bloques y en las esquinas
        v.set(xa, y, za, COB); v.set(xa, y, zb, COB); v.set(xb, y, za, COB); v.set(xb, y, zb, COB)
        for x in range(xa, xb + 1):
            if (x - X0) % 6 == 0: v.set(x, y, za, COB); v.set(x, y, zb, COB)
        for z in range(za, zb + 1):
            if (z - Z0) % 6 == 0: v.set(xa, y, z, COB); v.set(xb, y, z, COB)
        k += 1
    top = 9 + k
    v.fill(CX - 1, top, (Z0 + Z1) // 2 - 1, CX + 1, top, (Z0 + Z1) // 2 + 1, COB)
    for i in range(1, 8): v.set(CX, top + i, (Z0 + Z1) // 2, 'lightning_rod[facing=up,powered=false,waterlogged=false]' if i > 2 else COB)
    # nervio de las aristas hasta la cima (cobre)
    # entrada: puerta 5x5 bajo un aro de neon
    v.aire(CX - 2, 2, Z0, CX + 2, 6, Z0)
    for x in (CX - 3, CX + 3): v.fill(x, 2, Z0, x, 7, Z0, COB)
    v.fill(CX - 3, 7, Z0, CX + 3, 7, Z0, COB)
    v.fill(CX - 2, 8, Z0, CX + 2, 8, Z0, AMA) if False else None
    for z in (4, 5):
        v.fill(CX - 8, 1, z, CX - 8, 12, z, NEG); v.fill(CX + 8, 1, z, CX + 8, 12, z, NEG)
    anillo_vertical_xy(v, CX, 11, 5, 8, AMA, 1.5)
    anillo_vertical_xy(v, CX, 11, 5, 5, CIA, 1.0)
    for a in range(0, 360, 30):
        x = int(round(CX + math.cos(math.radians(a)) * 6.5)); y = int(round(11 + math.sin(math.radians(a)) * 6.5)); v.set(x, y, 4, 'sea_lantern')
    # rayo en el centro del aro (pixel art 7x9)
    bolt = ["   ##  ", "  ##   ", " ####  ", "  ###  ", "   ##  ", "  ##   ", " #     "]
    v.pixelart_z(CX - 3, 8, 5, [r for r in bolt], {'#': AMA})
    # escalones de entrada
    for z in (3, 2, 1):
        for x in range(CX - 3, CX + 4): v.stairs(x, 1, z, 'polished_blackstone_brick', 'north')
    v.aire(CX - 2, 2, 1, CX + 2, 6, 6)
    # ---- interior: escenario circular concentrico en el suelo
    cz = 24
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            d = math.hypot(x - CX, z - cz)
            col = 'gray_concrete'
            if d < 3: col = AMA
            elif d < 5: col = NEG
            elif d < 6.5: col = CIA
            elif d < 8: col = NEG
            elif d < 9.5: col = AMA
            v.set(x, 1, z, col)
    # bobinas Tesla en las 4 esquinas de la sala
    for (x, z) in [(X0 + 3, Z0 + 4), (X1 - 3, Z0 + 4), (X0 + 3, Z1 - 3), (X1 - 3, Z1 - 3)]:
        v.fill(x, 2, z, x, 3, z, COB); v.set(x, 4, z, 'waxed_cut_copper'); v.set(x, 5, z, 'copper_bulb[lit=true,powered=false]')
        for i in range(6, 11): v.set(x, i, z, 'lightning_rod[facing=up,powered=false,waterlogged=false]')
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            v.set(x + dx, 2, z + dz, NEG); v.set(x + dx, 3, z + dz, AMA) if False else None
    # focos colgantes bajo la piramide
    for (dx, dz) in [(-8, -6), (8, -6), (-8, 6), (8, 6), (0, 0)]:
        lampara_colgante(v, CX + dx, 12, cz + dz, 3, 'sea_lantern', 'iron_chain[axis=y]' if False else 'chain[axis=y]')
    # escenario del lider (parte trasera) con altavoces
    v.fill(CX - 6, 2, Z1 - 5, CX + 6, 2, Z1 - 1, NEG)
    v.fill(CX - 4, 3, Z1 - 3, CX + 4, 3, Z1 - 1, 'gray_concrete')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 6, 'polished_blackstone_brick', 'north')
    for x in range(CX - 4, CX + 5): v.stairs(x, 3, Z1 - 4, 'polished_blackstone_brick', 'north')
    for s in (-1, 1):
        for i in range(3): v.set(CX + s * 7, 2 + i, Z1 - 2, 'note_block')
        v.set(CX + s * 7, 5, Z1 - 2, 'sea_lantern')
    v.set(CX, 4, Z1 - 1, 'jukebox')
    v.fill(CX - 2, 4, Z1 - 2, CX + 2, 4, Z1 - 2, 'waxed_cut_copper_slab[type=bottom]')
    # gradas laterales (dos filas)
    for (xs, d, cara) in ((X0 + 1, 1, 'east'), (X1 - 1, -1, 'west')):
        for t in range(2):
            x = xs + d * t
            for z in range(Z0 + 5, Z1 - 7):
                v.fill(x, 2, z, x, 2 + t, z, NEG); v.stairs(x, 2 + t, z, 'polished_blackstone_brick', cara)
    return v, [(CX, 4, Z1 - 3, 180, 'emipokemon desafios teselia rayo')]
