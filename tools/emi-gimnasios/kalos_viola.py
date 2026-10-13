"""Gimnasio de Santalune (Viola, Bicho): estudio fotografico parisino de fachada blanca, tejado mansarda y una mariposa gigante en la torre central."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 37, 34, 40
CX = 18
X0, X1 = 3, 33          # cuerpo
Z0, Z1 = 6, 37          # fachada y fondo


def mariposa(v, cx, cy, z, escala=1.0, plano='xy'):
    """mariposa (alas naranja/amarillo con borde negro y ojo azul) en la pared z, centrada en (cx, cy)"""
    def ala(d, a):
        if d > 0.82: return 'black_concrete'
        if d > 0.55: return 'orange_concrete'
        if d > 0.28: return 'yellow_concrete'
        return 'light_blue_concrete'
    for s in (-1, 1):
        elipse_xy(v, cx + s * 6.2 * escala, cy + 3.2 * escala, z, 5.8 * escala, 4.6 * escala, ala)
        elipse_xy(v, cx + s * 4.2 * escala, cy - 3.0 * escala, z, 3.8 * escala, 3.1 * escala, ala)
    v.fill(cx, int(cy - 5 * escala), z, cx, int(cy + 5 * escala), z, 'black_concrete')
    v.set(cx, int(cy + 6 * escala), z, 'black_concrete')
    for s in (-1, 1):
        v.set(cx + s, int(cy + 7 * escala), z, 'black_concrete'); v.set(cx + 2 * s, int(cy + 8 * escala), z, 'black_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(41)
    PIE, CAL, ZOC, TEJ = 'smooth_quartz', 'calcite', 'stone_bricks', 'deepslate_tiles'
    v.fill(0, 0, 0, W - 1, 0, L - 1, ZOC)
    # ---- jardin delantero con camino, setos y flores
    for z in range(0, Z0):
        for x in range(W):
            v.set(x, 0, z, 'polished_andesite' if abs(x - CX) <= 2 else 'grass_block')
    for z in range(0, Z0 - 1):
        for x in (CX - 4, CX + 4): v.set(x, 0, z, 'smooth_stone')
    flores = ['poppy', 'dandelion', 'azure_bluet', 'cornflower', 'pink_tulip', 'allium', 'orange_tulip', 'oxeye_daisy']
    for x in list(range(1, CX - 5)) + list(range(CX + 6, W - 1)):
        for z in range(0, Z0 - 1):
            if rnd.random() < 0.5: v.set(x, 1, z, rnd.choice(flores))
    for dx in (-1, 1):                       # setos a los lados de la entrada, con una mariposa de lana encima
        for x in range(CX + dx * 6, CX + dx * 12, dx):
            v.fill(x, 1, 1, x, 2, 1, 'oak_leaves'); v.set(x, 3, 1, 'oak_leaves') if rnd.random() < 0.5 else None
    for (x, z) in [(2, 2), (34, 2), (2, 4), (34, 4)]:   # arbolitos de esquina
        arbol(v, x, 1, z, 4, 2, 'cherry_leaves', 'cherry_log[axis=y]', rnd)
    # ---- cuerpo: muros de calcita, zocalo de ladrillo
    v.box(X0, 1, Z0, X1, 14, Z1, CAL, interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 14, Z1 - 1)
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'moss_block')
    for y in (1, 2): v.anillo(X0, Z0, X1, Z1, y, ZOC)
    v.anillo(X0, Z0, X1, Z1, 9, PIE)         # cornisa entre plantas
    v.anillo(X0, Z0, X1, Z1, 14, PIE)
    # pilastras de cuarzo exteriores cada 5 bloques
    for z in range(Z0 + 4, Z1, 5):
        v.fill(X0, 3, z, X0, 13, z, 'quartz_pillar[axis=y]'); v.fill(X1, 3, z, X1, 13, z, 'quartz_pillar[axis=y]')
    # ventanas laterales altas (dos plantas)
    for z in range(Z0 + 2, Z1 - 1, 5):
        for dz in (0, 1):
            for y in (4, 5, 6, 7, 11, 12, 13):
                v.set(X0, y, z + dz, 'glass_pane'); v.set(X1, y, z + dz, 'glass_pane')
    # ---- fachada: puerta en arco, ventanales con balcon de barrotes
    hueco_puerta(v, CX, Z0, 5, 7, PIE, y0=2)
    for dx in (-1, 1):
        for cxv in (CX + dx * 7, CX + dx * 12):
            for y in range(3, 9): v.fill(cxv - 1, y, Z0, cxv + 1, y, Z0, 'glass_pane')
            for y in range(10, 14): v.fill(cxv - 1, y, Z0, cxv + 1, y, Z0, 'glass_pane')
            v.fill(cxv - 2, 3, Z0, cxv - 2, 8, Z0, PIE); v.fill(cxv + 2, 3, Z0, cxv + 2, 8, Z0, PIE)
            v.fill(cxv - 2, 10, Z0, cxv - 2, 13, Z0, PIE); v.fill(cxv + 2, 10, Z0, cxv + 2, 13, Z0, PIE)
            # balcon de la planta alta
            v.fill(cxv - 2, 9, Z0 - 1, cxv + 2, 9, Z0 - 1, 'smooth_stone_slab[type=bottom]')
            v.fill(cxv - 2, 10, Z0 - 1, cxv + 2, 10, Z0 - 1, 'iron_bars')
    # ---- torre central con la mariposa gigante
    v.box(6, 15, Z0, 30, 32, Z0 + 5, CAL, interior=None)
    v.fill(7, 15, Z0 + 1, 29, 32, Z0 + 4, 'air')
    v.fill(6, 15, Z0, 30, 15, Z0, PIE)
    v.fill(6, 32, Z0, 30, 32, Z0, PIE)
    v.fill(5, 33, Z0 - 1, 31, 33, Z0 + 6, 'deepslate_tile_slab[type=bottom]')
    mariposa(v, CX, 23, Z0, 1.0)
    # ---- tejado mansarda del cuerpo (pendiente fuerte hacia dentro y cubierta plana con claraboya)
    for k in range(0, 4):
        y = 15 + k
        for x in range(X0 + k, X1 - k + 1):
            v.stairs(x, y, Z0 + k, 'deepslate_tile', 'north') if not (6 <= x <= 30) else None
            v.stairs(x, y, Z1 - k, 'deepslate_tile', 'south')
        for z in range(Z0 + k, Z1 - k + 1):
            v.stairs(X0 + k, y, z, 'deepslate_tile', 'west'); v.stairs(X1 - k, y, z, 'deepslate_tile', 'east')
    v.fill(X0 + 4, 18, Z0 + 4, X1 - 4, 18, Z1 - 4, TEJ)
    v.fill(X0 + 5, 19, Z0 + 5, X1 - 5, 19, Z1 - 5, 'air')
    # claraboya central de cristal (el estudio recibe luz de arriba)
    v.fill(CX - 6, 18, 18, CX + 6, 18, 30, 'light_blue_stained_glass')
    v.fill(CX - 6, 14, 18, CX + 6, 17, 30, 'air')
    # buhardillas en la mansarda lateral
    for z in (14, 22, 30):
        for x in (X0 + 1, X1 - 1):
            v.set(x, 17, z, 'glass_pane'); v.set(x, 16, z, 'glass_pane')
    # ---- interior: suelo con camino de cuarzo y bordes rosas
    for z in range(Z0 + 1, Z1):
        v.fill(CX - 2, 1, z, CX + 2, 1, z, 'smooth_quartz')
        v.set(CX - 3, 1, z, 'pink_concrete'); v.set(CX + 3, 1, z, 'pink_concrete')
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1):
            if abs(x - CX) > 3 and rnd.random() < 0.12: v.set(x, 1, z, 'lime_concrete')
    # columnas de cuarzo con telarañas entre ellas
    for z in (12, 18, 24, 30):
        for x in (9, 27):
            v.fill(x, 2, z, x, 13, z, 'quartz_pillar[axis=y]'); v.set(x, 14, z, 'chiseled_quartz_block'); v.set(x, 2, z, 'chiseled_quartz_block')
    for z in (12, 18, 24, 30):
        for x in (9, 27):
            for dy in (0, 1, 2):
                if z + 6 <= 30:
                    for zz in range(z + 1, z + 6): v.set(x, 13 - dy, zz, 'cobweb') if rnd.random() < 0.5 else None
    # capullos colgantes (lana blanca con telarana) y lamparas
    for (x, z, largo) in [(13, 14, 4), (23, 14, 6), (14, 21, 3), (22, 26, 5), (12, 32, 4), (24, 32, 3)]:
        for dy in range(largo): v.set(x, 13 - dy, z, 'chain[axis=y]')
        v.fill(x, 13 - largo - 2, z, x, 13 - largo, z, 'white_wool')
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)): v.set(x + dx, 13 - largo - 1, z + dz, 'cobweb')
    for (x, z) in [(CX - 5, 10), (CX + 5, 10), (CX - 5, 22), (CX + 5, 22), (CX - 5, 33), (CX + 5, 33)]:
        lampara_colgante(v, x, 14, z, 3, 'lantern[hanging=true]')
    # equipo de fotografia: focos de estudio y camaras en tripode a los lados de la entrada
    for (x, z) in [(6, 10), (30, 10), (6, 20), (30, 20), (6, 30), (30, 30)]:
        foco_pie(v, x, 2, z)
    for (x, z, dirx) in [(7, 25, 1), (29, 25, -1)]:
        for (dx, dz) in ((-1, -1), (1, -1), (0, 1)): v.fill(x + dx, 2, z + dz, x + dx, 3, z + dz, 'iron_bars')
        v.fill(x - 1, 4, z - 1, x + 1, 5, z + 1, 'black_concrete'); v.set(x + dirx * 2, 5, z, 'glass'); v.set(x + dirx * 2, 4, z, 'black_concrete')
    # macetas con flores a lo largo de las paredes
    for z in range(10, 36, 6):
        v.set(X0 + 1, 2, z, 'potted_poppy'); v.set(X1 - 1, 2, z, 'potted_cornflower')
    # ---- escenario del lider: estrado semicircular con la mariposa de fondo
    for k, (r, b) in enumerate([(6, 'white_concrete'), (4, 'moss_block')]):
        for x in range(CX - r, CX + r + 1):
            for z in range(Z1 - 8 + 1, Z1):
                if math.hypot(x - CX, (z - (Z1 - 1)) * 0.9) <= r + 0.3: v.fill(x, 2 + k, z, x, 2 + k, z, b)
    for x in range(CX - 7, CX + 8): v.stairs(x, 2, Z1 - 9, 'quartz', 'north') if math.hypot(x - CX, 8 * 0.9) <= 7.3 else None
    mariposa(v, CX, 9, Z1, 0.8)
    # ---- NPC
    return v, [(CX, 4, Z1 - 4, 180, 'emipokemon desafios kalos bicho')]
