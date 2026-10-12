import math
from gymlib import V
from piezas import *

W, H, L = 37, 26, 40
CX, CZ = 18, 22
R = 9

def toldo(v, x0, x1, y, z, colores, largo=3):
    for x in range(x0, x1 + 1):
        c = colores[(x - x0) % len(colores)]
        for k in range(largo): v.set(x, y - k // 2 if False else y, z + k, c)
        v.stairs(x, y - 1 if False else y, z + largo, 'quartz', 'north') if False else None

def mesa_con_sillas(v, x, y, z, cola='spruce_slab[type=top]'):
    v.set(x, y, z, 'spruce_fence'); v.set(x, y + 1, z, cola)
    v.stairs(x - 1, y, z, 'spruce', 'east'); v.stairs(x + 1, y, z, 'spruce', 'west'); v.stairs(x, y, z - 1, 'spruce', 'south'); v.stairs(x, y, z + 1, 'spruce', 'north')
    v.set(x, y + 2, z, 'candle[candles=1,lit=false]')

def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    PL = 'stone_bricks'
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_stone')
    # jardín delantero
    for z in range(0, 7):
        for x in range(W):
            if abs(x - CX) > 3: v.set(x, 0, z, 'grass_block')
    for (x, z) in [(6, 3), (30, 3), (9, 5), (27, 5)]:
        v.fill(x - 1, 1, z, x + 1, 1, z, 'azalea_leaves'); v.set(x, 1, z, 'moss_block'); v.set(x, 2, z, 'flowering_azalea')
    # rotonda central de 2 plantas con cupula de cristal
    v.cilindro(CX, 1, 8, CZ, R, 'white_concrete')
    v.disco(CX, 1, CZ, R, 'smooth_quartz')
    v.cupula(CX, 9, CZ, R, 'white_stained_glass', hueco=True, ry=7)
    # nervios de la cupula
    for a in range(0, 360, 45):
        for k in range(0, 8):
            ang = math.radians(a); el = math.radians(k * 12)
            x = int(round(CX + math.cos(ang) * (R - 0.2) * math.cos(el))); z = int(round(CZ + math.sin(ang) * (R - 0.2) * math.cos(el))); y = 9 + int(round(math.sin(el) * 7))
            v.set(x, y, z, 'white_concrete')
    v.set(CX, 17, CZ, 'gold_block') if False else None
    # tres alas: verde (izquierda), roja (derecha) y azul (fondo)
    def ala(x0, x1, z0, z1, wall, roof, roof_st, glass, carpet, cara):
        v.box(x0, 1, z0, x1, 8, z1, wall)
        v.fill(x0 + 1, 1, z0 + 1, x1 - 1, 1, z1 - 1, 'smooth_quartz' if False else 'polished_andesite')
        v.aire(x0 + 1, 2, z0 + 1, x1 - 1, 8, z1 - 1)
        for y in (1, 2): v.anillo(x0, z0, x1, z1, y, 'stone_bricks')
        # tejado a dos aguas cuya cumbrera va en la direccion larga
        if (x1 - x0) <= (z1 - z0): techo_dos_aguas(v, x0 - 1, x1 + 1, z0, z1, 9, roof, roof_st, 'z')
        else: techo_dos_aguas(v, x0, x1, z0 - 1, z1 + 1, 9, roof, roof_st, 'x')
    ala(2, 9, 12, 32, 'lime_terracotta' if False else 'green_terracotta', 'green_concrete', 'mossy_cobblestone', 'lime_stained_glass', 'green_carpet', 'east')
    ala(27, 34, 12, 32, 'red_terracotta', 'red_concrete', 'brick', 'red_stained_glass', 'red_carpet', 'west')
    ala(10, 26, 31, 38, 'blue_terracotta', 'blue_concrete', 'prismarine_brick', 'light_blue_stained_glass', 'blue_carpet', 'north')
    # huecos de paso entre rotonda y alas
    v.aire(8, 2, CZ - 1, 11, 5, CZ + 1); v.aire(25, 2, CZ - 1, 28, 5, CZ + 1); v.aire(CX - 1, 2, CZ + 8, CX + 1, 5, CZ + 11)
    v.aire(8, 2, CZ - 1, 8, 5, CZ + 1)
    # ventanas decoradas en alas
    for z in range(15, 31, 4):
        for y in (4, 5): v.set(2, y, z, 'lime_stained_glass'); v.set(2, y, z + 1, 'lime_stained_glass'); v.set(34, y, z, 'red_stained_glass'); v.set(34, y, z + 1, 'red_stained_glass')
    for x in range(12, 25, 4):
        for y in (4, 5): v.set(x, y, 38, 'light_blue_stained_glass'); v.set(x + 1, y, 38, 'light_blue_stained_glass')
    # entrada: puerta de la rotonda al frente (z = CZ-R) con toldo a rayas
    zf = CZ - R
    v.aire(CX - 2, 2, zf, CX + 2, 6, zf)
    for x in (CX - 3, CX + 3): v.fill(x, 2, zf, x, 7, zf, 'dark_oak_planks')
    v.fill(CX - 3, 7, zf, CX + 3, 7, zf, 'dark_oak_planks')
    cols = ['red_concrete', 'white_concrete']
    for x in range(CX - 6, CX + 7):
        for k in range(0, 6):
            v.set(x, 9 - k // 2, zf - 1 - k, cols[(x - CX) % 2] if True else 'red_concrete')
    for x in (CX - 6, CX + 6): v.fill(x, 1, zf - 5, x, 8, zf - 5, 'oak_fence')
    # carteles: tres escudos (hoja, llama, gota) sobre la puerta
    v.fill(CX - 5, 8, zf, CX - 3, 10, zf, 'green_concrete'); v.set(CX - 4, 9, zf, 'moss_block')
    v.fill(CX - 1, 8, zf, CX + 1, 10, zf, 'white_concrete'); v.set(CX, 9, zf, 'gold_block')
    v.fill(CX + 3, 8, zf, CX + 5, 10, zf, 'red_concrete'); v.set(CX + 4, 9, zf, 'magma_block')
    # escalones y camino
    for z in range(1, zf):
        for x in range(CX - 3, CX + 4): v.set(x, 1, z, 'smooth_quartz')
    for x in range(CX - 3, CX + 4): v.stairs(x, 1, zf - 1, 'quartz', 'north')
    # interior de la rotonda: suelo de damero y fuente central
    for x in range(CX - R + 1, CX + R):
        for z in range(CZ - R + 1, CZ + R):
            if math.hypot(x - CX, z - CZ) <= R - 0.6: v.set(x, 1, z, 'black_concrete' if (x + z) % 2 else 'white_concrete')
    v.anillo_circ(CX, 1, CZ, 3, 'light_blue_concrete', 1.2) if False else None
    v.disco(CX, 1, CZ, 3, 'quartz_block'); v.disco(CX, 2, CZ, 3, 'quartz_block')
    v.disco(CX, 2, CZ, 2, 'light_blue_stained_glass'); v.set(CX, 3, CZ, 'quartz_pillar[axis=y]'); v.set(CX, 4, CZ, 'sea_lantern')
    # alfombras tricolores hacia las alas
    for x in range(8, CX - 3): v.set(x, 1, CZ, 'green_concrete')
    for x in range(CX + 4, 29): v.set(x, 1, CZ, 'red_concrete')
    for z in range(CZ + 4, 32): v.set(CX, 1, z, 'blue_concrete')
    # mesas y sillas en la rotonda
    for (dx, dz) in [(-6, -4), (6, -4), (-6, 4), (6, 4)]:
        mesa_con_sillas(v, CX + dx, 2, CZ + dz)
    # cocina en el ala azul: barra con cauldrons, hornos y estantes
    for x in range(12, 25):
        v.set(x, 2, 36, 'smoker[facing=south,lit=false]' if x % 3 == 0 else 'polished_andesite')
        v.set(x, 3, 36, 'polished_andesite_slab[type=bottom]')
    for x in (13, 17, 21): v.set(x, 4, 36, 'cauldron')
    v.fill(12, 2, 33, 24, 2, 33, 'barrel[facing=up]') if False else None
    # tarima del lider (ala azul): escenario con pupitre y tres sillas (cilan/chili/cress)
    v.fill(CX - 4, 2, 34, CX + 4, 2, 35, 'smooth_quartz')
    for x in range(CX - 4, CX + 5): v.stairs(x, 2, 33, 'quartz', 'north')
    # naves verde y roja: mesas y cocina
    for z in (16, 20, 26, 30):
        mesa_con_sillas(v, 5, 2, z); mesa_con_sillas(v, 31, 2, z)
    # lamparas colgantes
    for (dx, dz) in [(0, -3), (-5, 0), (5, 0), (0, 5)]: lampara_colgante(v, CX + dx, 12, CZ + dz, 2, 'lantern[hanging=true]')
    for z in range(15, 31, 5):
        lampara_colgante(v, 5, 11 if False else 9, z, 1, 'lantern[hanging=true]'); lampara_colgante(v, 31, 9, z, 1, 'lantern[hanging=true]')
    return v, [(CX, 4, 35, 180, 'emipokemon desafios teselia trio')]
