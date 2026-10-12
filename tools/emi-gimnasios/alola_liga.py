"""Liga de Alola en el Monte Lanakila: templo nevado con tejados de bambu, vestibulo, rotonda, cuatro salas del Alto Mando (Hala, Olivia, Acerola, Kahili) y la cupula del campeon Kukui."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *
from ligalib import *

W, H, L = 90, 48, 98
CX = 45
X0, X1, Z0, Z1 = 6, 83, 14, 66
HP = 13
KZ, KR = 80, 15                  # centro z y radio de la cupula de Kukui
ROJO, AMA = 'red_concrete', 'yellow_concrete'


def emblema_alola(v, cx, cz, y, R):
    """sol (amarillo) y luna (morado) en el suelo: dos mitades de un disco con rayos"""
    disco_f(v, cx, y, cz, R, lambda d, a: AMA if (math.cos(a) > 0 and d > R * 0.35) or d < R * 0.2 else 'purple_concrete' if d > R * 0.35 else 'white_concrete')
    for k in range(8):
        a = k / 8 * 2 * math.pi
        for t in range(int(R) + 1, int(R) + 3): v.set(round(cx + math.cos(a) * t), y, round(cz + math.sin(a) * t), AMA if math.cos(a) > 0 else 'purple_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(421)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'snow_block')
    for x in range(W):
        for z in range(L):
            if rnd.random() < 0.12: v.set(x, 0, z, 'packed_ice')
            elif rnd.random() < 0.18 and not (X0 - 3 <= x <= X1 + 3 and Z0 - 3 <= z <= 96): v.set(x, 1, z, 'snow[layers=%d]' % rnd.randint(1, 4))
    for x in range(CX - 7, CX + 8):
        for z in range(0, Z0): v.set(x, 0, z, 'stone_bricks' if (x + z) % 2 else 'smooth_stone'); v.set(x, 1, z, 'air')
    for (x, z) in [(10, 5), (80, 6), (4, 30), (86, 34), (5, 58), (85, 60)]:
        v.fill(x, 1, z, x, 4, z, 'spruce_log[axis=y]'); copa(v, x, 7, z, 3, 4, 3, 'spruce_leaves', rnd); v.set(x, 11, z, 'snow_block')
    # ---- cuerpo del templo: base de piedra, vigas rojas y muros claros
    v.box(X0, 1, Z0, X1, HP + 2, Z1, 'stone_bricks', interior=None)
    for y in (1, 2, 3): v.anillo(X0, Z0, X1, Z1, y, 'polished_andesite')
    for x in range(X0, X1 + 1, 4):
        v.fill(x, 4, Z0, x, HP, Z0, ROJO); v.fill(x, 4, Z1, x, HP, Z1, ROJO)
    for (x, z) in [(X0 - 1, Z0 - 1), (X1 + 1, Z0 - 1), (X0 - 1, Z1 + 1), (X1 + 1, Z1 + 1)]:
        pass
    # ---- salas del Alto Mando
    salas = {
        'hala': (X0, 32, Z0, 38, 'white_terracotta', 'spruce_planks', 'dark_oak_planks'),
        'olivia': (57, X1, Z0, 38, 'stone_bricks', 'stone', 'deepslate_tiles'),
        'acerola': (X0, 32, 40, Z1, 'purple_terracotta', 'polished_blackstone', 'blackstone'),
        'kahili': (57, X1, 40, Z1, 'light_blue_concrete', 'white_concrete', 'white_concrete'),
    }
    for nombre, (xa, xb, za, zb, muro, suelo, techo) in salas.items():
        sala(v, xa, xb, za, zb, HP - 1, muro, suelo, techo)
        v.anillo(xa, za, xb, zb, HP, AMA)
    sala(v, 32, 57, Z0, 38, HP - 1, 'stone_bricks', 'polished_andesite', 'dark_oak_planks')
    # rotonda central
    rx, rz, rr = CX, 52, 12
    v.cilindro(rx, 1, HP + 1, rz, rr, 'stone_bricks')
    v.aire(rx - 11, 2, 38, rx + 11, HP, 38)
    emblema_alola(v, rx, rz, 1, 9)
    disco_f(v, rx, 1, rz, rr - 1, lambda d, a: 'smooth_sandstone' if d > 9.8 else None)
    v.cupula(rx, HP + 2, rz, rr, 'white_concrete', hueco=True, ry=11)
    for k in range(8):
        for i in range(0, 12):
            el = math.radians(i * 7.5)
            x = round(rx + math.cos(k * math.pi / 4) * (rr - 0.3) * math.cos(el)); z = round(rz + math.sin(k * math.pi / 4) * (rr - 0.3) * math.cos(el)); y = HP + 2 + round(math.sin(el) * 11)
            v.set(x, y, z, AMA if k % 2 else 'purple_concrete')
    v.fill(rx - 1, HP + 2 + 11, rz - 1, rx + 1, HP + 2 + 11, rz + 1, 'light_blue_stained_glass')
    # ---- vanos
    arco(v, 32, 26, 'x', 7, 8, AMA); arco(v, 57, 26, 'x', 7, 8, AMA)
    for x in range(32, 34): v.aire(x, 2, 49, x, 8, 55)
    for x in range(56, 58): v.aire(x, 2, 49, x, 8, 55)
    arco(v, 32, 52, 'x', 7, 8, AMA); arco(v, 57, 52, 'x', 7, 8, AMA)
    v.aire(CX - 3, 2, 38, CX + 3, 9, 38); arco(v, CX, 38, 'z', 7, 9, AMA)
    v.aire(CX - 3, 2, 64, CX + 3, 9, 66)
    # ---- fachada: porche de madera con tikis y escalinata
    v.fill(CX - 10, 1, Z0 - 4, CX + 10, 1, Z0 - 1, 'stone_bricks')
    for x in range(CX - 10, CX + 11, 5):
        v.fill(x, 2, Z0 - 3, x, 10, Z0 - 3, 'jungle_log[axis=y]')
    techo_bambu(v, CX - 10, CX + 10, Z0 - 4, Z0 - 1, 11, 1)
    hueco_puerta(v, CX, Z0, 7, 8, ROJO, y0=2, prof=1)
    for x in range(CX - 7, CX + 8): v.stairs(x, 1, Z0 - 5, 'stone_brick', 'north')
    for dx in (-13, 13): tiki(v, CX + dx, 6, 12, 'mossy_stone_bricks')
    for dx in (-16, -8, 8, 16): antorcha_tiki(v, CX + dx, 4, 4)
    # ---- tejados de bambu para cada sala, vestibulo y cupulas
    for (xa, xb, za, zb) in [(X0, 32, Z0, 38), (57, X1, Z0, 38), (X0, 32, 40, Z1), (57, X1, 40, Z1), (32, 57, Z0, 38)]:
        techo_bambu(v, xa, xb, za, zb, HP + 3, 2)
    # ---- sala de la campeona: cupula con sol y luna
    sala_circular(v, CX, KZ, KR, HP + 3, 'stone_bricks', lambda d, a: 'polished_andesite' if int(d) % 2 else 'stone_bricks')
    v.cupula(CX, HP + 5, KZ, KR, 'white_concrete', hueco=True, ry=17)
    for k in range(12):
        for i in range(0, 20):
            el = math.radians(i * 4.5)
            x = round(CX + math.cos(k * math.pi / 6) * (KR - 0.3) * math.cos(el)); z = round(KZ + math.sin(k * math.pi / 6) * (KR - 0.3) * math.cos(el)); y = HP + 5 + round(math.sin(el) * 17)
            v.set(x, y, z, AMA if k % 2 else 'purple_concrete')
    v.fill(CX - 1, HP + 5 + 17, KZ - 1, CX + 1, HP + 5 + 17, KZ + 1, 'light_blue_stained_glass')
    v.aire(CX - 4, 2, Z1, CX + 4, 10, Z1 + 1); arco(v, CX, Z1, 'z', 7, 9, AMA)
    decorar(v, rnd)
    return v, [
        (10, 4, 26, 270, 'emipokemon desafios alola elite_hala'), (80, 4, 26, 90, 'emipokemon desafios alola elite_olivia'),
        (10, 4, 52, 270, 'emipokemon desafios alola elite_acerola'), (80, 4, 52, 90, 'emipokemon desafios alola elite_kahili'),
        (CX, 5, KZ + 10, 180, 'emipokemon desafios alola champion_kukui')]


def decorar(v, rnd):
    # Hala: sala de lucha con ring de paja y gradas de madera
    for z in range(15, 38):
        for x in range(7, 32): v.set(x, 1, z, 'spruce_planks' if (x + z) % 2 else 'dark_oak_planks')
    disco_f(v, 20, 1, 26, 7, anillos([(1.5, 'red_concrete'), (6, 'sandstone'), (7, 'red_concrete')]))
    for z in range(17, 37, 5):
        v.fill(7, 5, z, 7, 11, z, 'red_wool'); v.set(7, 12, z, AMA)
    luces_colgantes(v, 6, 32, 14, 38, HP, 'lantern')
    tarima_lado(v, 7, 26, 1, 4, 'dark_oak_planks', 'dark_oak')
    # Olivia: cueva de roca con cristales y fosiles
    for z in range(15, 38):
        for x in range(58, 83):
            if rnd.random() < 0.12: v.set(x, 1, z, rnd.choice(['cobblestone', 'andesite', 'tuff']))
    for _ in range(18):
        x, z = rnd.randint(59, 80), rnd.randint(16, 36)
        if not (x > 76 and 22 < z < 30): v.set(x, 2, z, 'amethyst_cluster[facing=up,waterlogged=false]')
    for (x, z) in [(62, 18), (62, 34), (70, 18), (70, 34)]:
        v.fill(x, 2, z, x + 1, 5, z + 1, 'stone_bricks'); v.set(x, 6, z, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
    for k in range(7): v.set(66 + k, 2, 26, 'bone_block'); v.set(66 + k, 3, 25 + (k % 2) * 2, 'bone_block')
    luces_colgantes(v, 57, 83, 14, 38, HP, 'shroomlight')
    tarima_lado(v, 82, 26, -1, 4, 'stone_bricks', 'stone_brick')
    # Acerola: sala de fantasmas con velas y telarañas
    for z in range(41, 64):
        for x in range(7, 32):
            if rnd.random() < 0.08: v.set(x, 1, z, 'crying_obsidian')
    for _ in range(70): v.set(rnd.randint(7, 31), rnd.randint(8, 12), rnd.randint(41, 63), 'cobweb')
    for (x, z) in [(10, 44), (10, 60), (18, 44), (18, 60), (26, 50)]:
        v.fill(x, 2, z, x, 3, z, 'polished_blackstone_bricks'); v.set(x, 4, z, 'candle[candles=4,lit=true]')
    luces_colgantes(v, 6, 32, 40, Z1, HP, 'soul_lantern')
    tarima_lado(v, 7, 52, 1, 4, 'purple_concrete', 'purpur')
    # Kahili: campo de golf en el cielo: nubes, banderines y hoyos
    for z in range(41, 64):
        for x in range(58, 83):
            v.set(x, 1, z, 'lime_concrete' if rnd.random() < 0.7 else 'green_concrete')
    for (x, z) in [(64, 46), (70, 58), (76, 46)]:
        v.set(x, 1, z, 'black_concrete'); v.fill(x, 2, z, x, 6, z, 'iron_bars'); v.fill(x + 1, 5, z, x + 2, 6, z, 'red_wool')
    for _ in range(12):
        x, z, y = rnd.randint(60, 80), rnd.randint(42, 62), rnd.randint(7, 11)
        v.fill(x, y, z, x + 3, y, z + 2, 'white_concrete'); v.fill(x + 1, y + 1, z + 1, x + 2, y + 1, z + 1, 'white_concrete')
    luces_colgantes(v, 57, 83, 40, Z1, HP, 'sea_lantern')
    tarima_lado(v, 82, 52, -1, 4, 'white_concrete', 'quartz')
    # vestibulo y rotonda
    for z in range(15, 38):
        for x in range(CX - 3, CX + 4): v.set(x, 1, z, 'red_concrete' if abs(x - CX) < 2 else 'yellow_concrete')
    for z in range(18, 38, 5):
        for x in (34, 55): v.fill(x, 2, z, x, 11, z, 'jungle_log[axis=y]'); v.set(x, 12, z, 'chiseled_stone_bricks')
    luces_colgantes(v, 32, 57, 14, 38, HP, 'lantern')
    # sala de Kukui: suelo con sol y luna, columnas y estrado
    emblema_alola(v, CX, KZ, 1, 11)
    for k in range(12):
        a = k / 12 * 2 * math.pi
        x, z = round(CX + math.cos(a) * 12.5), round(KZ + math.sin(a) * 12.5)
        if abs(x - CX) <= 4 and z < KZ - 9: continue
        v.fill(x, 2, z, x, 10, z, 'jungle_log[axis=y]'); v.set(x, 11, z, 'chiseled_stone_bricks')
    for k in range(3):
        v.fill(CX - 6 + k, 2 + k, KZ + 6 + k, CX + 6 - k, 2 + k, KZ + KR - 3, 'stone_bricks' if k % 2 == 0 else 'polished_andesite')
        for x in range(CX - 6 + k, CX + 7 - k): v.stairs(x, 2 + k, KZ + 5 + k, 'stone_brick', 'north')
    elipse_xy(v, CX, 12, KZ + 12, 4, 4, lambda d, a: AMA if d < 0.5 else 'purple_concrete')
    for dx in (-5, 5): tiki(v, CX + dx, KZ + 10, 7, 'mossy_stone_bricks', 5)
