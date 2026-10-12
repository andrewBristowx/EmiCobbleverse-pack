from gymlib import V
from piezas import *

W, H, L = 37, 27, 38
CX = 18
Z0 = 6          # fachada
X0, X1 = 2, 34  # cuerpo
Z1 = 37

def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    PL, PIL, CRM, SUE = 'stone_bricks', 'quartz_pillar[axis=y]', 'quartz_block', 'smooth_sandstone'
    v.fill(0, 0, 0, W - 1, 0, L - 1, PL)
    v.fill(X0, 1, Z0, X1, 1, Z1, 'polished_andesite')
    # ---- muros: naves laterales (y=2..8) y nave central (y=2..12)
    v.box(X0, 2, Z0, X1, 8, Z1, SUE)
    v.box(11, 2, Z0, 25, 12, Z1, SUE, interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 8, Z1 - 1)
    v.aire(12, 2, Z0 + 1, 24, 12, Z1 - 1)
    # zocalo de ladrillo y cornisas
    for y in (2, 3):
        v.fill(X0, y, Z0, X1, y, Z0, 'bricks'); v.fill(X0, y, Z1, X1, y, Z1, 'bricks')
        v.fill(X0, y, Z0, X0, y, Z1, 'bricks'); v.fill(X1, y, Z0, X1, y, Z1, 'bricks')
    v.anillo(X0, Z0, X1, Z1, 8, CRM)
    # pilastras exteriores cada 4 bloques en los muros laterales
    for z in range(Z0 + 4, Z1, 4):
        v.fill(X0, 2, z, X0, 8, z, PIL); v.fill(X1, 2, z, X1, 8, z, PIL)
    # ventanas altas de las naves laterales (entre pilastras)
    for z in range(Z0 + 2, Z1 - 1, 4):
        for dz in (0, 1):
            for y in (4, 5, 6, 7):
                v.set(X0, y, z + dz, 'glass_pane'); v.set(X1, y, z + dz, 'glass_pane')
    # muro trasero con ventanal
    for dx in range(-4, 5):
        for y in (4, 5, 6, 7, 8, 9, 10): v.set(CX + dx, y, Z1, 'light_blue_stained_glass' if abs(dx) < 4 else SUE)
    # ---- arcos entre la nave central y las laterales
    for z in range(Z0 + 5, Z1 - 3, 4):
        for x in (11, 25):
            v.aire(x, 2, z, x, 6, z + 2)
            v.stairs(x, 7, z, 'quartz', 'east' if x == 11 else 'west', 'top')
    for z in range(Z0 + 3, Z1, 4):
        for x in (11, 25): v.fill(x, 2, z, x, 8, z, PIL)
    # ventanas altas de la nave central (claristorio)
    for z in range(Z0 + 3, Z1 - 1, 3):
        for x in (11, 25):
            for y in (10, 11): v.set(x, y, z, 'glass_pane'); v.set(x, y, z + 1, 'glass_pane')
    # ---- techos
    v.fill(X0, 9, Z0, 10, 9, Z1, 'deepslate_tiles'); v.fill(26, 9, Z0, X1, 9, Z1, 'deepslate_tiles')
    v.anillo(X0, Z0, X1, Z1, 9, 'deepslate_tile_slab[type=bottom]') if False else None
    for x in (X0, X1):
        v.fill(x, 9, Z0, x, 9, Z1, 'deepslate_tile_wall') if False else None
    ridge = techo_dos_aguas(v, 11, 25, Z0, Z1, 13, 'deepslate_tiles', 'deepslate_tile', 'z')
    v.fill(11, 13, Z0, 25, 13, Z1, 'deepslate_tiles') if False else None
    v.fill(12, 12, Z0 + 1, 24, 12, Z1 - 1, 'air')
    # hastial trasero
    frontón(v, CX, Z1, 13, 14, SUE, 'quartz') 
    frontón(v, CX, Z0, 13, 14, SUE, 'quartz')
    # ---- fachada: porche con 6 columnas, entablamento y frontón
    for x in (CX - 8, CX - 4, CX + 4, CX + 8):
        v.fill(x, 1, 4, x, 11, 4, PIL)
        v.set(x, 12, 4, 'chiseled_quartz_block'); v.set(x, 1, 4, 'chiseled_quartz_block')
    v.fill(CX - 10, 12, 3, CX + 10, 13, 5, CRM)
    v.fill(CX - 10, 14, 4, CX + 10, 14, 4, 'smooth_sandstone_slab[type=bottom]') if False else None
    for k in range(10):
        v.fill(CX - 10 + k, 14 + k, 4, CX + 10 - k, 14 + k, 4, 'quartz_block' if k % 2 == 0 else CRM)
    v.fill(CX - 6, 16, 4, CX + 6, 20, 4, SUE) if False else None
    # rosetón en el frontón: circulo de cristal con un faro de luz
    v.esfera(CX, 18, 4, 2, 'light_blue_stained_glass') if False else None
    for dx, dy in [(0, 0), (-1, 0), (1, 0), (0, 1), (0, -1), (-1, 1), (1, 1), (-1, -1), (1, -1)]:
        v.set(CX + dx, 17 + dy, 4, 'light_blue_stained_glass')
    for dx, dy in [(0, 2), (0, -2), (2, 0), (-2, 0)]: v.set(CX + dx, 17 + dy, 4, 'gold_block')
    # puerta doble 5x6 y escalinata
    v.aire(CX - 2, 2, Z0, CX + 2, 7, Z0)
    for x in (CX - 3, CX + 3): v.fill(x, 2, Z0, x, 8, Z0, PIL)
    v.fill(CX - 3, 8, Z0, CX + 3, 8, Z0, 'chiseled_quartz_block')
    for z, y in ((3, 1), (2, 1), (1, 1)):
        for x in range(CX - 11, CX + 12): v.stairs(x, y, z, 'quartz', 'north') if z != 3 else v.stairs(x, y, z, 'quartz', 'north')
    for x in range(CX - 11, CX + 12): v.fill(x, 1, 4, x, 1, 5, 'smooth_quartz') if abs(x - CX) <= 10 else None
    # estandartes laterales
    for x in (CX - 14, CX + 14):
        v.fill(x - 1, 3, Z0 - 1, x + 1, 9, Z0 - 1, 'orange_terracotta' if False else 'white_concrete')
        v.fill(x - 1, 3, Z0 - 1, x + 1, 3, Z0 - 1, 'orange_concrete')
        v.set(x, 6, Z0 - 1, 'lantern')
    # ---- interior
    # suelo de la nave: damero de cuarzo y abedul; alfombra central roja
    tablero_damas(v, 12, Z0 + 1, 24, Z1 - 1, 1, 'smooth_quartz', 'stripped_birch_log[axis=y]' if False else 'birch_planks')
    for z in range(Z0 + 1, 30): v.fill(CX - 1, 1, z, CX + 1, 1, z, 'red_concrete')
    v.anillo(12, 14, 24, 30, 1, 'orange_concrete')       # recuadro de combate
    v.fill(CX - 2, 1, 21, CX + 2, 1, 23, 'orange_concrete'); v.fill(CX - 1, 1, 22, CX + 1, 1, 22, 'white_concrete')
    # naves laterales: suelo oscuro y vitrinas
    for (xa, xb) in ((X0 + 1, 10), (26, X1 - 1)):
        v.fill(xa, 1, Z0 + 1, xb, 1, Z1 - 1, 'polished_andesite')
        for z in range(Z0 + 3, Z1 - 3, 4):
            cx2 = (xa + xb) // 2
            v.fill(cx2 - 1, 2, z, cx2 + 1, 2, z + 1, 'smooth_stone')
            v.fill(cx2 - 1, 3, z, cx2 + 1, 5, z + 1, 'glass')
            v.fill(cx2 - 1, 6, z, cx2 + 1, 6, z + 1, 'smooth_stone')
            v.aire(cx2, 3, z, cx2, 5, z)
            v.set(cx2, 3, z, 'bone_block'); v.set(cx2, 4, z, 'skeleton_skull[rotation=0]'); 
        # estanterias en el muro exterior
        xm = X0 + 1 if xa == X0 + 1 else X1 - 1
        for z in range(Z0 + 5, Z1 - 4, 4):
            v.fill(xm, 2, z, xm, 4, z + 1, 'bookshelf')
    # fosil gigante (costillar y cola de bloques de hueso) tras el podio, sobre las naves
    for z in range(8, 33, 3):
        r = 3
        for a in range(0, 181, 15):
            import math
            x = int(round(CX + math.cos(math.radians(a)) * 0)) if False else 0
    # lamparas del techo de las naves laterales y de la nave central
    for z in range(Z0 + 3, Z1 - 1, 4):
        for xa in (X0 + 5, 30): v.set(xa, 8, z, 'sea_lantern')
        lampara_colgante(v, CX, ridge, z, 4, 'lantern[hanging=true]')
        lampara_colgante(v, CX - 4, 14 + 3, z, 2, 'lantern[hanging=true]') if False else None
    # podio del lider al fondo
    v.fill(CX - 6, 2, Z1 - 5, CX + 6, 2, Z1 - 1, 'smooth_quartz')
    v.fill(CX - 4, 3, Z1 - 3, CX + 4, 3, Z1 - 1, 'smooth_quartz')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 6, 'quartz', 'north')
    for x in range(CX - 4, CX + 5): v.stairs(x, 3, Z1 - 4, 'quartz', 'north')
    # escritorio del lider y estantes
    v.fill(CX - 2, 4, Z1 - 2, CX + 2, 4, Z1 - 2, 'dark_oak_slab[type=bottom]')
    v.set(CX, 5, Z1 - 2, 'lectern[facing=north]')
    # banderas de la pared trasera
    for dx in (-7, 7):
        v.fill(CX + dx - 1, 4, Z1 - 1, CX + dx + 1, 10, Z1 - 1, 'orange_concrete'); v.fill(CX + dx, 6, Z1 - 1, CX + dx, 8, Z1 - 1, 'white_concrete')
    return v, [(CX, 4, Z1 - 3, 180, 'emipokemon desafios teselia basica')]
