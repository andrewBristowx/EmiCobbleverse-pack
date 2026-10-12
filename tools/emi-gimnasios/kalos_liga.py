"""Liga de Kalos: palacio blanco y azul con vestibulo, rotonda con cupula, cuatro salas del Alto Mando (Malva, Siebold, Wikstrom, Drasna) y el teatro de Diantha."""
import math
from gymlib import V
from piezas2 import *
from ligalib import *

W, H, L = 96, 48, 90
CX = 48
AZ, BL, ORO = 'blue_concrete', 'white_concrete', 'gold_block'
X0, X1, Z0, Z1 = 6, 89, 14, 86
ROT = (48, 52, 12)          # centro x, z y radio de la rotonda
HP = 14                     # altura interior de las salas


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(121)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_quartz')
    # ---- explanada: jardines con setos, caminos y dos fuentes
    for z in range(0, Z0):
        for x in range(W):
            v.set(x, 0, z, 'grass_block' if abs(x - CX) > 6 else 'smooth_quartz')
    for x in range(CX - 6, CX + 7):
        for z in range(0, Z0): v.set(x, 0, z, 'smooth_quartz' if (x + z) % 2 else 'light_gray_concrete')
    for (fx, fz) in [(CX - 20, 7), (CX + 20, 7)]:                                          # fuentes con agua encerrada
        disco_f(v, fx, 0, fz, 4, anillos([(2.5, 'light_blue_concrete'), (4, 'stone_bricks')]))
        disco_f(v, fx, 1, fz, 3.4, lambda d, a: 'water' if d <= 2.4 else 'stone_bricks')
        v.fill(fx, 1, fz, fx, 3, fz, 'quartz_pillar[axis=y]'); v.set(fx, 4, fz, 'sea_lantern')
    for x in list(range(2, CX - 26)) + list(range(CX + 26, W - 2)):
        for z in (2, 3, 11, 12):
            if rnd.random() < 0.7: v.set(x, 1, z, rnd.choice(['poppy', 'allium', 'cornflower', 'azure_bluet', 'pink_tulip', 'oxeye_daisy']))
    for x in range(8, 40, 8):
        for (zz) in (5, 9):
            v.fill(x - 2, 1, zz, x + 2, 2, zz, 'oak_leaves'); v.fill(W - 1 - x - 2, 1, zz, W - 1 - x + 2, 2, zz, 'oak_leaves')
    # ---- cuerpo del palacio
    v.box(X0, 1, Z0, X1, HP + 2, Z1, BL, interior=None)
    for y in (1, 2, 3): v.anillo(X0, Z0, X1, Z1, y, 'quartz_bricks')
    v.anillo(X0, Z0, X1, Z1, HP + 1, ORO)
    # ---- salas: Malva, Siebold (frente), Wikstrom, Drasna (detras)
    salas = {
        'malva': (X0, 34, Z0, 38, 'red_terracotta', 'blackstone', 'polished_blackstone_bricks'),
        'siebold': (61, X1, Z0, 38, 'cyan_terracotta', 'prismarine_bricks', 'dark_prismarine'),
        'wikstrom': (X0, 34, 40, 64, 'light_gray_concrete', 'polished_andesite', 'iron_block'),
        'drasna': (61, X1, 40, 64, 'purple_terracotta', 'obsidian', 'purpur_block'),
    }
    for nombre, (xa, xb, za, zb, muro, suelo, techo) in salas.items():
        sala(v, xa, xb, za, zb, HP - 1, muro, suelo, techo)
        v.anillo(xa, za, xb, zb, HP, ORO)
    # ---- vestibulo
    sala(v, 34, 61, Z0, 40, HP - 1, BL, 'smooth_quartz', 'white_concrete')
    v.aire(35, HP + 1, Z0 + 1, 60, HP + 1, 39)
    # ---- rotonda central
    rx, rz, rr = ROT
    v.cilindro(rx, 1, HP + 1, rz, rr, BL)
    disco_f(v, rx, 1, rz, rr - 1, anillos([(2, 'gold_block'), (4, 'light_blue_concrete'), (5, 'white_concrete'), (8, 'blue_concrete'), (9, 'white_concrete'), (11, 'light_blue_concrete')]))
    v.cupula(rx, HP + 2, rz, rr, 'white_concrete', hueco=True, ry=14)
    for a in range(0, 360, 30):                                                                 # nervios dorados de la cupula
        for k in range(0, 15):
            el = math.radians(k * 6)
            x = round(rx + math.cos(math.radians(a)) * (rr - 0.3) * math.cos(el)); z = round(rz + math.sin(math.radians(a)) * (rr - 0.3) * math.cos(el)); y = HP + 2 + round(math.sin(el) * 14)
            v.set(x, y, z, ORO)
    for dx in range(-2, 3):                                                                    # oculo central
        for dz in range(-2, 3):
            if math.hypot(dx, dz) <= 2.4: v.set(rx + dx, HP + 2 + 14, rz + dz, 'light_blue_stained_glass'); v.set(rx + dx, HP + 2 + 13, rz + dz, 'light_blue_stained_glass') if math.hypot(dx, dz) <= 1.5 else None
    # ---- teatro del campeon al fondo
    sala(v, 24, 71, 66, Z1, 17, 'blue_concrete', 'red_concrete', 'white_concrete')
    v.anillo(24, 66, 71, Z1, HP + 4, ORO)
    # ---- pasillos y arcos
    arco(v, 34, 26, 'x', 7, 8, ORO); arco(v, 61, 26, 'x', 7, 8, ORO)                        # vestibulo <-> Malva / Siebold
    arco(v, 34, 52, 'x', 5, 7, ORO); arco(v, 61, 52, 'x', 5, 7, ORO)                        # rotonda <-> Wikstrom / Drasna (muro del palacio entre ambos: se abre un corredor)
    for x in range(35, 37): v.aire(x, 2, 49, x, 8, 55)
    for x in range(59, 61): v.aire(x, 2, 49, x, 8, 55)
    v.aire(46, 2, 40, 50, 8, 40)                                                               # vestibulo -> rotonda
    arco(v, CX, 40, 'z', 7, 9, ORO)
    arco(v, CX, 64, 'z', 7, 9, ORO)                                                            # rotonda -> teatro
    v.aire(CX - 3, 2, 65, CX + 3, 9, 66)
    # ---- fachada: portico con columnas, frontón y escalinata
    for x in range(CX - 12, CX + 13, 4):
        v.fill(x, 1, Z0 - 2, x, 13, Z0 - 2, 'quartz_pillar[axis=y]'); v.set(x, 14, Z0 - 2, 'chiseled_quartz_block'); v.set(x, 1, Z0 - 2, 'chiseled_quartz_block')
    v.fill(CX - 14, 15, Z0 - 3, CX + 14, 16, Z0 - 1, 'quartz_block')
    for k in range(9): v.fill(CX - 14 + k, 17 + k, Z0 - 2, CX + 14 - k, 17 + k, Z0 - 2, AZ if k % 2 else 'quartz_block')
    hueco_puerta(v, CX, Z0, 9, 10, ORO, y0=2, prof=1)
    for x in range(CX - 8, CX + 9): v.stairs(x, 1, Z0 - 3, 'quartz', 'north')
    for x in range(CX - 8, CX + 9): v.stairs(x, 0, Z0 - 4, 'quartz', 'north')
    # estandartes azules en la fachada
    for x in (CX - 18, CX - 24, CX + 18, CX + 24):
        v.fill(x, 5, Z0 - 1, x, 12, Z0 - 1, 'blue_wool'); v.set(x, 4, Z0 - 1, ORO)
    # ---- torres de las esquinas y tejados escalonados
    for (x, z) in [(X0 - 1, Z0 - 1), (X1 + 1, Z0 - 1), (X0 - 1, Z1 + 1), (X1 + 1, Z1 + 1)]:
        torre_cono(v, x, z, 4, 22, BL, AZ, ORO)
    for (xa, xb, za, zb) in [(X0, 34, Z0, 38), (61, X1, Z0, 38), (X0, 34, 40, 64), (61, X1, 40, 64)]:
        tejado_terrazas(v, xa, xb, za, zb, HP + 3, AZ, ORO, 4)
    tejado_terrazas(v, 34, 61, Z0, 40, HP + 3, AZ, ORO, 6)
    tejado_terrazas(v, 24, 71, 66, Z1, HP + 5, AZ, ORO, 8)
    # ---- decorar las salas del Alto Mando
    decorar_malva(v, rnd); decorar_siebold(v, rnd); decorar_wikstrom(v, rnd); decorar_drasna(v, rnd)
    decorar_vestibulo(v, rnd); decorar_teatro(v, rnd)
    return v, [
        (10, 4, 26, 270, 'emipokemon desafios kalos elite_malva'), (85, 4, 26, 90, 'emipokemon desafios kalos elite_siebold'),
        (10, 4, 52, 270, 'emipokemon desafios kalos elite_wikstrom'), (85, 4, 52, 90, 'emipokemon desafios kalos elite_drasna'),
        (CX, 5, 80, 180, 'emipokemon desafios kalos champion_diantha')]


def tarima_x(v, x0, z, lado, ancho, mat, esc):
    """estrado pegado a la pared x0 (la sala se abre hacia +x si lado=+1 o -x si lado=-1): 2 peldaños; el NPC en y=4 a 2 bloques de la pared"""
    for dz in range(-ancho, ancho + 1):
        v.fill(x0, 2, z + dz, x0 + lado * 5, 2, z + dz, mat)
        v.fill(x0, 3, z + dz, x0 + lado * 3, 3, z + dz, mat)
        v.stairs(x0 + lado * 6, 2, z + dz, esc, 'east' if lado > 0 else 'west')
        v.stairs(x0 + lado * 4, 3, z + dz, esc, 'east' if lado > 0 else 'west')


def decorar_malva(v, rnd):
    """sala de fuego: basalto, estanques de lava encerrados, braseros y estandartes rojos"""
    x0, x1, z0, z1 = 7, 33, 15, 37
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            if rnd.random() < 0.15: v.set(x, 1, z, 'magma_block')
    for z in (19, 33):
        for x in (14, 22):
            v.fill(x - 1, 1, z - 1, x + 1, 2, z + 1, 'blackstone'); v.set(x, 1, z, 'lava'); v.set(x, 2, z, 'air')
            v.fill(x - 1, 3, z - 1, x + 1, 3, z + 1, 'air')
    for z in range(z0 + 2, z1, 4):
        for x in (x0 + 1, x1 - 1):
            v.fill(x, 2, z, x, 5, z, 'polished_blackstone_bricks'); v.set(x, 6, z, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
            v.set(x, 7, z, 'air')
    for x in range(x0 + 3, x1 - 2, 6):
        for z in (z0 + 3, z1 - 3):
            for k in range(1, 4): v.set(x, HP - k + 1, z, 'chain[axis=y]')
            v.set(x, HP - 3, z, 'shroomlight')
    for z in range(z0 + 3, z1, 5):
        v.fill(x1 - 1, 5, z, x1 - 1, 11, z, 'red_wool'); v.set(x1 - 1, 12, z, ORO)
    tarima_x(v, x0 + 1, 26, 1, 5, 'red_nether_bricks', 'red_nether_brick')
    for z in (22, 30): v.fill(x0 + 2, 4, z, x0 + 2, 8, z, 'red_nether_bricks'); v.set(x0 + 2, 9, z, 'shroomlight')
    v.fill(x0 + 1, 4, 24, x0 + 1, 12, 28, 'red_concrete')


def decorar_siebold(v, rnd):
    """sala de agua: acuarios de cristal con agua, suelo de prismarina y mesas de cocina"""
    x0, x1, z0, z1 = 62, 88, 15, 37
    for z in range(z0 + 1, z1, 1):
        for x in range(x0 + 1, x1):
            if rnd.random() < 0.18: v.set(x, 1, z, 'sea_lantern')
    for z in (18, 32):                                              # acuarios en la pared de los lados (el centro queda libre para el estrado)
        for x in (x1 - 2,):
            v.fill(x - 1, 2, z, x + 1, 9, z + 4, 'glass')
            v.fill(x - 1, 2, z, x + 1, 2, z + 4, 'prismarine_bricks')
            v.fill(x, 3, z + 1, x, 8, z + 3, 'water')
            v.set(x, 3, z + 2, 'prismarine'); v.set(x, 4, z + 2, 'sea_lantern')
    for z in range(z0 + 3, z1 - 2, 7):
        for x in (x0 + 3,):
            v.fill(x - 1, 2, z, x + 1, 9, z + 4, 'glass'); v.fill(x - 1, 2, z, x + 1, 2, z + 4, 'prismarine_bricks')
            v.fill(x, 3, z + 1, x, 8, z + 3, 'water')
    for (x, z) in [(70, 20), (70, 32), (78, 20), (78, 32)]:           # mesas de comedor
        v.set(x, 2, z, 'oak_fence')
        v.fill(x - 1, 3, z - 1, x + 1, 3, z + 1, 'smooth_quartz_slab[type=bottom]'); v.set(x, 3, z, 'smooth_quartz'); v.set(x, 4, z, 'candle[candles=1,lit=true]')
    for x in range(x0 + 3, x1 - 2, 6):
        for z in (z0 + 3, z1 - 3):
            for k in range(1, 4): v.set(x, HP - k + 1, z, 'chain[axis=y]')
            v.set(x, HP - 3, z, 'sea_lantern')
    tarima_x(v, x1 - 1, 26, -1, 5, 'prismarine_bricks', 'prismarine_brick')
    v.fill(x1 - 1, 4, 24, x1 - 1, 12, 28, 'dark_prismarine')


def decorar_wikstrom(v, rnd):
    """sala de acero: armaduras, estandartes grises, suelo de ajedrez y una espada gigante"""
    x0, x1, z0, z1 = 7, 33, 41, 63
    for z in range(z0 + 1, z1):
        for x in range(x0 + 1, x1): v.set(x, 1, z, 'polished_andesite' if (x + z) % 2 else 'gray_concrete')
    for z in range(z0 + 2, z1, 4):
        for x in (x0 + 2, x1 - 2):
            v.fill(x, 2, z, x, 3, z, 'iron_block'); v.fill(x, 4, z, x, 5, z, 'iron_block'); v.set(x, 6, z, 'iron_block')
            v.fill(x + (1 if x < 20 else -1), 3, z, x + (1 if x < 20 else -1), 5, z, 'iron_bars')
    for x in range(x0 + 4, x1 - 3, 6):
        for z in (z0 + 4, z1 - 4):
            for k in range(1, 4): v.set(x, HP - k + 1, z, 'chain[axis=y]')
            v.set(x, HP - 3, z, 'lantern[hanging=true]')
    for z in range(z0 + 3, z1, 5):
        v.fill(x1 - 1, 5, z, x1 - 1, 11, z, 'gray_wool'); v.set(x1 - 1, 12, z, 'iron_block')
    tarima_x(v, x0 + 1, 52, 1, 5, 'smooth_stone', 'stone_brick')
    # espada gigante en la pared del fondo (vertical, punta arriba)
    for y in range(6, 14): v.set(x0 + 1, y, 52, 'iron_block'); v.set(x0 + 1, y, 51, 'light_gray_concrete') if y > 8 else None


def decorar_drasna(v, rnd):
    """sala de dragon: obsidiana y purpur, un tesoro de oro, cristales de amatista y una cabeza de dragon"""
    x0, x1, z0, z1 = 62, 88, 41, 63
    for z in range(z0 + 1, z1):
        for x in range(x0 + 1, x1):
            if rnd.random() < 0.1: v.set(x, 1, z, 'crying_obsidian')
    for _ in range(26):
        x = rnd.randint(x0 + 2, x1 - 4); z = rnd.randint(z0 + 2, z1 - 2)
        if abs(z - 52) < 5 and x > x1 - 12: continue
        v.set(x, 2, z, 'amethyst_cluster[facing=up,waterlogged=false]')
    for (x, z) in [(70, 45), (70, 59), (80, 45), (80, 59)]:                                  # montones de oro
        for dx in range(-1, 2):
            for dz in range(-1, 2): v.set(x + dx, 2, z + dz, ORO)
        v.set(x, 3, z, ORO)
    for z in range(z0 + 3, z1, 5):
        v.fill(x1 - 1, 5, z, x1 - 1, 11, z, 'purple_wool'); v.set(x1 - 1, 12, z, ORO)
    for x in range(x0 + 4, x1 - 3, 6):
        for z in (z0 + 4, z1 - 4):
            for k in range(1, 4): v.set(x, HP - k + 1, z, 'chain[axis=y]')
            v.set(x, HP - 3, z, 'soul_lantern[hanging=true]')
    tarima_x(v, x1 - 1, 52, -1, 5, 'purpur_block', 'purpur')
    for (x, z) in [(66, 47), (66, 57)]:                                                         # cabezas de dragon sobre pilares
        v.fill(x, 2, z, x, 4, z, 'obsidian'); v.set(x, 5, z, 'dragon_head[rotation=4]')


def decorar_vestibulo(v, rnd):
    for z in range(16, 40):
        for x in range(42, 55): v.set(x, 1, z, 'light_blue_concrete' if abs(x - CX) < 3 else 'white_concrete')
        v.set(CX - 4, 1, z, ORO); v.set(CX + 3, 1, z, ORO)
    for z in range(18, 39, 5):
        for x in (37, 58):
            v.fill(x, 2, z, x, 12, z, 'quartz_pillar[axis=y]'); v.set(x, 13, z, 'chiseled_quartz_block')
        for dx in (-5, 5):
            for k in range(1, 5): v.set(CX + dx, HP - k + 1, z, 'chain[axis=y]')
            v.set(CX + dx, HP - 4, z, 'sea_lantern')


def decorar_teatro(v, rnd):
    """teatro de Diantha: escenario, telon rojo, filas de butacas, palcos y arana de luces"""
    # escenario al fondo (z 80..85), butacas hacia delante
    for fila, z in enumerate(range(68, 78, 2)):
        for x in range(26, 70):
            if abs(x - CX) in (0, 1): continue
            v.stairs(x, 2 + fila // 3, z, 'brick', 'south')
            v.fill(x, 1, z, x, 1 + fila // 3, z, 'red_concrete') if fila // 3 > 0 else None
    for z in range(68, 78): v.set(CX, 1, z, 'gold_block') if z % 2 == 0 else v.set(CX, 1, z, 'white_concrete')
    # escenario elevado con alfombra
    v.fill(30, 2, 79, 65, 2, 85, 'smooth_quartz'); v.fill(34, 3, 80, 61, 3, 85, 'smooth_quartz')
    for x in range(30, 66): v.stairs(x, 2, 78, 'quartz', 'north') if abs(x - CX) <= 3 else v.set(x, 2, 78, 'blue_concrete')
    for x in range(34, 62): v.stairs(x, 3, 79, 'quartz', 'north')
    # telon rojo a los lados y arco de proscenio
    for dx in list(range(-17, -9)) + list(range(10, 18)):
        h = 13 - abs(abs(dx) - 14) // 2
        v.fill(CX + dx, 4, 85, CX + dx, h, 85, 'red_wool')
    v.fill(26, 15, 79, 69, 17, 79, 'blue_concrete'); v.fill(26, 14, 79, 69, 14, 79, ORO)
    for x in range(26, 70, 3): v.set(x, 18, 79, ORO)
    for (dx, y0) in ((-8, 10), (8, 10)):
        v.fill(CX + dx, 4, 84, CX + dx, 12, 84, 'quartz_pillar[axis=y]')
    # decorado de fondo: panel con la Torre Prismatica (pixel)
    for dy in range(5, 14):
        w = max(1, 6 - (dy - 5) // 2)
        v.fill(CX - w, dy, 85, CX + w, dy, 85, 'light_blue_concrete' if dy % 2 else 'white_concrete')
    v.fill(CX, 14, 85, CX, 17, 85, ORO)
    # palcos laterales
    for z in range(68, 78, 3):
        v.fill(25, 5, z, 25, 5, z + 1, 'quartz_slab[type=bottom]'); v.set(25, 6, z, 'iron_bars'); v.set(25, 6, z + 1, 'iron_bars')
        v.fill(70, 5, z, 70, 5, z + 1, 'quartz_slab[type=bottom]'); v.set(70, 6, z, 'iron_bars'); v.set(70, 6, z + 1, 'iron_bars')
    # aranas de luces
    for (x, z) in [(36, 72), (60, 72), (48, 72), (40, 82), (56, 82)]:
        for k in range(1, 6): v.set(x, 18 - k + 1, z, 'chain[axis=y]')
        v.set(x, 13, z, 'sea_lantern')
        for (ax, az) in ((1, 0), (-1, 0), (0, 1), (0, -1)): v.set(x + ax, 13, z + az, 'glowstone')
