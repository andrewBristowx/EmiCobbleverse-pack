"""Liga de Paldea: palacio con las cinco bases del Team Star (Giacomo, Mela, Atticus, Ortega, Eri), atrio con estrella, sala de trofeos y la cupula de la campeona Geeta."""
import math
from gymlib import V
from piezas2 import *
from ligalib import *

W, H, L = 84, 46, 120
CX = 42
AZ, BL = 'blue_concrete', 'white_concrete'
X0, X1, Z0, Z1 = 6, 78, 14, 86
HP = 13
GZ, GR = 101, 15            # centro z y radio de la cupula de Geeta


def estrella_suelo(v, cx, cz, y, R, a, b):
    poligono_xz(v, estrella(cx, cz, R), y, a)
    poligono_xz(v, estrella(cx, cz, R * 0.55, R * 0.23), y, b)


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(301)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'smooth_quartz')
    for z in range(0, Z0):
        for x in range(W): v.set(x, 0, z, 'grass_block' if abs(x - CX) > 8 else 'smooth_quartz')
    for x in range(CX - 8, CX + 9):
        for z in range(0, Z0): v.set(x, 0, z, 'smooth_quartz' if (x + z) % 2 else 'light_gray_concrete')
    poligono_xz(v, estrella(CX, 7, 6), 0, 'yellow_concrete')
    for x in range(3, W - 3, 5):
        if abs(x - CX) > 12:
            v.fill(x, 1, 3, x + 2, 2, 3, 'oak_leaves'); v.set(x + 1, 1, 9, rnd.choice(['poppy', 'cornflower', 'allium', 'azure_bluet', 'pink_tulip']))
    # ---- cuerpo del palacio y torres de esquina
    v.box(X0, 1, Z0, X1, HP + 2, Z1, BL, interior=None)
    for y in (1, 2, 3): v.anillo(X0, Z0, X1, Z1, y, 'quartz_bricks')
    v.anillo(X0, Z0, X1, Z1, HP + 1, 'yellow_concrete')
    for (x, z) in [(X0 - 1, Z0 - 1), (X1 + 1, Z0 - 1), (X0 - 1, Z1 + 1), (X1 + 1, Z1 + 1)]: torre_cono(v, x, z, 4, 20, BL, AZ, 'yellow_concrete', 'gold_block')
    # ---- salas del Team Star y sala de trofeos
    salas = {
        'giacomo': (X0, 30, Z0, 38, 'gray_concrete', 'black_concrete', 'polished_blackstone_bricks'),
        'mela': (54, X1, Z0, 38, 'red_terracotta', 'blackstone', 'red_nether_bricks'),
        'atticus': (X0, 30, 38, 62, 'purple_terracotta', 'polished_deepslate', 'deepslate_tiles'),
        'ortega': (54, X1, 38, 62, 'pink_terracotta', 'smooth_quartz', 'pink_concrete'),
        'eri': (X0, 30, 62, Z1, 'orange_terracotta', 'sandstone', 'smooth_sandstone'),
        'trofeos': (54, X1, 62, Z1, 'white_concrete', 'polished_andesite', 'quartz_bricks'),
    }
    for nombre, (xa, xb, za, zb, muro, suelo, techo) in salas.items():
        sala(v, xa, xb, za, zb, HP - 1, muro, suelo, techo)
        v.anillo(xa, za, xb, zb, HP, 'yellow_concrete')
    # ---- pasillo central: vestibulo, atrio y corredor (muros compartidos con las salas)
    sala(v, 30, 54, Z0, 38, HP - 1, BL, 'smooth_quartz', 'white_concrete')
    sala(v, 30, 54, 62, Z1, HP - 1, BL, 'smooth_quartz', 'white_concrete')
    rx, rz, rr = CX, 50, 11
    v.cilindro(rx, 1, HP + 1, rz, rr, BL)
    v.aire(rx - 11, 2, 38, rx + 11, HP, 38)
    disco_f(v, rx, 1, rz, rr - 1, anillos([(11, 'smooth_quartz')]))
    estrella_suelo(v, rx, rz, 1, 9, 'yellow_concrete', 'blue_concrete')
    v.cupula(rx, HP + 2, rz, rr, 'light_blue_stained_glass', hueco=True, ry=10)
    for k in range(8):
        for i in range(0, 11):
            el = math.radians(i * 9)
            x = round(rx + math.cos(k * math.pi / 4) * (rr - 0.3) * math.cos(el)); z = round(rz + math.sin(k * math.pi / 4) * (rr - 0.3) * math.cos(el)); y = HP + 2 + round(math.sin(el) * 10)
            v.set(x, y, z, 'white_concrete')
    # ---- vanos
    arco(v, 30, 26, 'x', 7, 8, 'yellow_concrete'); arco(v, 54, 26, 'x', 7, 8, 'yellow_concrete')
    arco(v, 30, 50, 'x', 7, 8, 'yellow_concrete'); arco(v, 54, 50, 'x', 7, 8, 'yellow_concrete')
    arco(v, 30, 74, 'x', 7, 8, 'yellow_concrete'); arco(v, 54, 74, 'x', 7, 8, 'yellow_concrete')
    v.aire(CX - 3, 2, 38, CX + 3, 9, 38); v.aire(CX - 3, 2, 62, CX + 3, 9, 62)
    arco(v, CX, 38, 'z', 7, 9, 'yellow_concrete'); arco(v, CX, 62, 'z', 7, 9, 'yellow_concrete')
    for x in range(30, 32): v.aire(x, 2, 47, x, 8, 53)
    for x in range(53, 55): v.aire(x, 2, 47, x, 8, 53)
    # ---- fachada: portico, estrella dorada en el frontón y escalinata
    for x in range(CX - 12, CX + 13, 4):
        v.fill(x, 1, Z0 - 2, x, 13, Z0 - 2, 'quartz_pillar[axis=y]'); v.set(x, 14, Z0 - 2, 'chiseled_quartz_block')
    v.fill(CX - 14, 15, Z0 - 3, CX + 14, 16, Z0 - 1, 'quartz_block')
    for k in range(9): v.fill(CX - 14 + k, 17 + k, Z0 - 2, CX + 14 - k, 17 + k, Z0 - 2, AZ if k % 2 else 'quartz_block')
    poligono_xy(v, estrella(CX, 21, 5), Z0 - 2, 'yellow_concrete')
    hueco_puerta(v, CX, Z0, 9, 10, 'yellow_concrete', y0=2, prof=1)
    for x in range(CX - 8, CX + 9): v.stairs(x, 1, Z0 - 3, 'quartz', 'north'); v.stairs(x, 0, Z0 - 4, 'quartz', 'north')
    for x in (CX - 18, CX - 24, CX + 18, CX + 24): v.fill(x, 5, Z0 - 1, x, 12, Z0 - 1, 'blue_wool'); v.set(x, 4, Z0 - 1, 'gold_block')
    # ---- cupula de Geeta
    sala_circular(v, CX, GZ, GR, HP + 3, BL, lambda d, a: 'polished_deepslate' if int(d) % 2 else 'deepslate_tiles')
    v.cupula(CX, HP + 5, GZ, GR, 'white_concrete', hueco=True, ry=18)
    for k in range(12):
        for i in range(0, 20):
            el = math.radians(i * 4.5)
            x = round(CX + math.cos(k * math.pi / 6) * (GR - 0.3) * math.cos(el)); z = round(GZ + math.sin(k * math.pi / 6) * (GR - 0.3) * math.cos(el)); y = HP + 5 + round(math.sin(el) * 18)
            v.set(x, y, z, 'yellow_concrete')
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if math.hypot(dx, dz) <= 2.4: v.set(CX + dx, HP + 5 + 18, GZ + dz, 'light_blue_stained_glass')
    v.aire(CX - 4, 2, Z1, CX + 4, 10, Z1 + 1)
    arco(v, CX, Z1, 'z', 7, 9, 'yellow_concrete')
    # ---- decoracion
    decorar_giacomo(v, rnd); decorar_mela(v, rnd); decorar_atticus(v, rnd); decorar_ortega(v, rnd); decorar_eri(v, rnd)
    decorar_trofeos(v, rnd); decorar_geeta(v, rnd); decorar_pasillos(v, rnd)
    # ---- tejados de las salas
    for (xa, xb, za, zb) in [(X0, 30, Z0, 38), (54, X1, Z0, 38), (X0, 30, 38, 62), (54, X1, 38, 62), (X0, 30, 62, Z1), (54, X1, 62, Z1)]:
        tejado_terrazas(v, xa, xb, za, zb, HP + 3, AZ, 'yellow_concrete', 4)
    tejado_terrazas(v, 30, 54, Z0, 38, HP + 3, AZ, 'yellow_concrete', 5)
    tejado_terrazas(v, 30, 54, 62, Z1, HP + 3, AZ, 'yellow_concrete', 5)
    return v, [
        (9, 4, 26, 270, 'emipokemon desafios paldea elite_giacomo'), (75, 4, 26, 90, 'emipokemon desafios paldea elite_mela'),
        (9, 4, 50, 270, 'emipokemon desafios paldea elite_atticus'), (75, 4, 50, 90, 'emipokemon desafios paldea elite_ortega'),
        (18, 2, 74, 270, 'emipokemon desafios paldea elite_eri'), (CX, 5, 110, 180, 'emipokemon desafios paldea champion_geeta')]


def tarima_lado(v, x0, z, lado, ancho, mat, esc):
    for dz in range(-ancho, ancho + 1):
        v.fill(x0, 2, z + dz, x0 + lado * 5, 2, z + dz, mat); v.fill(x0, 3, z + dz, x0 + lado * 3, 3, z + dz, mat)
        v.stairs(x0 + lado * 6, 2, z + dz, esc, 'east' if lado > 0 else 'west'); v.stairs(x0 + lado * 4, 3, z + dz, esc, 'east' if lado > 0 else 'west')


def luces_sala(v, xa, xb, za, zb, luz='sea_lantern'):
    if luz in ('lantern', 'soul_lantern'): luz += '[hanging=true]'
    for x in range(xa + 4, xb - 2, 6):
        for z in range(za + 4, zb - 2, 6):
            for k in range(1, 3): v.set(x, HP - k + 1, z, 'chain[axis=y]')
            v.set(x, HP - 2, z, luz)


def coche(v, x, z, color, largo=9):
    """coche visto de lado (largo en z, morro hacia -z): carroceria, cristales, ruedas y faros"""
    v.fill(x - 2, 3, z, x + 2, 3, z + largo - 1, color); v.fill(x - 2, 4, z + 1, x + 2, 4, z + largo - 2, color)
    v.fill(x - 1, 5, z + 2, x + 1, 5, z + largo - 4, 'light_blue_stained_glass'); v.fill(x - 1, 6, z + 3, x + 1, 6, z + largo - 5, color)
    for (dx, dz) in ((-3, 1), (3, 1), (-3, largo - 3), (3, largo - 3)): v.fill(x + dx, 2, z + dz, x + dx, 3, z + dz + 1, 'black_concrete')
    v.set(x - 2, 4, z - 1, 'yellow_concrete'); v.set(x + 2, 4, z - 1, 'yellow_concrete'); v.set(x, 3, z - 1, 'iron_bars')


def decorar_giacomo(v, rnd):
    for z in range(15, 38):
        for x in range(7, 30):
            if rnd.random() < 0.08: v.set(x, 1, z, 'gray_concrete')
    for x in range(7, 30): v.set(x, 1, 20, 'yellow_concrete') if x % 3 else None
    coche(v, 20, 16, 'black_concrete'); coche(v, 24, 27, 'magenta_concrete')
    for (x, z) in [(8, 17), (8, 21), (9, 33), (8, 35)]:
        for k in range(3): v.fill(x, 2 + k, z, x + 1, 2 + k, z + 1, 'black_concrete')
    for z in range(17, 37, 5):
        for k in range(1, 3): v.set(28, HP - k + 1, z, 'chain[axis=y]')
        v.set(28, HP - 2, z, 'sea_lantern')
        v.fill(7, 5, z, 7, 8, z, 'magenta_concrete')
    tarima_lado(v, 7, 26, 1, 4, 'polished_blackstone_bricks', 'polished_blackstone_brick')


def decorar_mela(v, rnd):
    for z in range(15, 38):
        for x in range(55, 78):
            if rnd.random() < 0.12: v.set(x, 1, z, 'magma_block')
    for (x, z) in [(60, 20), (60, 32), (68, 20), (68, 32)]:
        v.fill(x - 1, 1, z - 1, x + 1, 2, z + 1, 'blackstone'); v.set(x, 2, z, 'lava'); v.fill(x - 1, 3, z - 1, x + 1, 3, z + 1, 'air')
    for z in range(17, 37, 4):
        v.fill(55, 2, z, 55, 4, z, 'polished_blackstone_bricks'); v.set(55, 5, z, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
        v.fill(77, 5, z, 77, 11, z, 'orange_wool')
    luces_sala(v, 54, 78, 14, 38, 'shroomlight')
    tarima_lado(v, 77, 26, -1, 4, 'red_nether_bricks', 'red_nether_brick')


def decorar_atticus(v, rnd):
    for z in range(39, 62):
        for x in range(7, 30):
            if rnd.random() < 0.1: v.set(x, 1, z, 'slime_block')
    for (x, z) in [(10, 42), (10, 58), (18, 42), (18, 58), (24, 46), (24, 54)]:                 # cubas de veneno de cristal
        v.fill(x - 1, 2, z - 1, x + 1, 6, z + 1, 'lime_stained_glass'); v.fill(x, 2, z, x, 5, z, 'lime_concrete'); v.fill(x - 1, 2, z - 1, x + 1, 2, z + 1, 'polished_deepslate')
        v.set(x, 7, z, 'verdant_froglight')
    for z in range(41, 61, 4): v.fill(7, 5, z, 7, 10, z, 'purple_wool'); v.set(7, 11, z, 'lime_concrete')
    luces_sala(v, 6, 30, 38, 62, 'verdant_froglight')
    tarima_lado(v, 7, 50, 1, 4, 'purple_concrete', 'purpur')


def decorar_ortega(v, rnd):
    for z in range(39, 62):
        for x in range(55, 78): v.set(x, 1, z, 'pink_concrete' if (x + z) % 2 else 'white_concrete')
    for z in range(41, 61, 5):
        v.fill(77, 3, z, 77, 9, z + 2, 'glass'); v.fill(77, 2, z, 77, 2, z + 2, 'gold_block'); v.fill(77, 10, z, 77, 10, z + 2, 'gold_block')
    for (x, z) in [(60, 44), (60, 56), (70, 44), (70, 56)]:
        v.set(x, 2, z, 'smooth_quartz'); v.set(x, 3, z, 'smooth_quartz_slab[type=bottom]'); v.set(x, 4, z, 'candle[candles=3,lit=true]')
        for (dx, dz) in ((1, 0), (-1, 0)): v.stairs(x + dx, 2, z + dz, 'quartz', 'north')
    luces_sala(v, 54, 78, 38, 62, 'sea_lantern')
    poligono_xz(v, estrella(66, 50, 4), 1, 'magenta_concrete')
    tarima_lado(v, 77, 50, -1, 4, 'pink_concrete', 'quartz')


def decorar_eri(v, rnd):
    for z in range(63, 85):
        for x in range(7, 30): v.set(x, 1, z, 'sandstone' if rnd.random() < 0.5 else 'smooth_sandstone')
    disco_f(v, 18, 1, 74, 8, anillos([(2, 'red_concrete'), (7, 'sand'), (8, 'red_concrete')]))
    for k in range(16):
        a = k / 16 * 2 * math.pi
        x = round(18 + math.cos(a) * 8); z = round(74 + math.sin(a) * 8)
        if x > 23 and abs(z - 74) < 4: continue
        v.fill(x, 3, z, x, 7, z, 'iron_bars')
    for z in range(65, 84, 6):
        for x in (8, 28):
            for k in range(1, 4): v.set(x, HP - k + 1, z, 'chain[axis=y]')
            v.fill(x, 8, z, x, 10, z, 'brown_terracotta')
    for (x, z) in [(9, 66), (9, 82), (28, 66), (28, 82)]:
        v.fill(x, 2, z, x + 1, 3, z + 1, 'hay_block[axis=y]')
    luces_sala(v, 6, 30, 62, 86, 'lantern')


def decorar_trofeos(v, rnd):
    for z in range(63, 85):
        for x in range(55, 78): v.set(x, 1, z, 'polished_andesite' if (x + z) % 2 else 'smooth_quartz')
    estrella_suelo(v, 66, 74, 1, 7, 'yellow_concrete', 'blue_concrete')
    for k in range(10):
        a = k / 10 * 2 * math.pi
        x = round(66 + math.cos(a) * 9); z = round(74 + math.sin(a) * 9)
        v.fill(x, 2, z, x, 3, z, 'quartz_block'); v.set(x, 4, z, 'glass'); v.set(x, 5, z, 'glass')
        v.set(x, 4, z, rnd.choice(['gold_block', 'diamond_block', 'emerald_block', 'lapis_block', 'amethyst_block']))
    for z in range(66, 84, 6):
        v.fill(77, 4, z, 77, 9, z, 'blue_wool'); v.set(77, 10, z, 'gold_block')
    luces_sala(v, 54, 78, 62, 86, 'sea_lantern')


def decorar_pasillos(v, rnd):
    for z in range(15, 38):
        for x in range(CX - 3, CX + 4): v.set(x, 1, z, 'blue_concrete' if abs(x - CX) < 2 else 'yellow_concrete')
    for z in range(63, 86):
        for x in range(CX - 3, CX + 4): v.set(x, 1, z, 'blue_concrete' if abs(x - CX) < 2 else 'yellow_concrete')
    for z in list(range(18, 38, 5)) + list(range(66, 86, 5)):
        for x in (32, 52): v.fill(x, 2, z, x, 11, z, 'quartz_pillar[axis=y]'); v.set(x, 12, z, 'chiseled_quartz_block')
        for dx in (-5, 5):
            for k in range(1, 4): v.set(CX + dx, HP - k + 1, z, 'chain[axis=y]')
            v.set(CX + dx, HP - 3, z, 'sea_lantern')


def decorar_geeta(v, rnd):
    estrella_suelo(v, CX, GZ, 1, 10, 'yellow_concrete', 'blue_concrete')
    for k in range(12):
        a = k / 12 * 2 * math.pi
        x = round(CX + math.cos(a) * 12); z = round(GZ + math.sin(a) * 12)
        if abs(x - CX) <= 4 and z < GZ - 8: continue
        v.fill(x, 2, z, x, 10, z, 'quartz_pillar[axis=y]'); v.set(x, 11, z, 'chiseled_quartz_block'); v.set(x, 2, z, 'chiseled_quartz_block')
    for k in range(8):
        a = k / 8 * 2 * math.pi + 0.2
        x = round(CX + math.cos(a) * 6); z = round(GZ + math.sin(a) * 6)
        for dy in range(1, 4): v.set(x, HP + 4 - dy, z, 'chain[axis=y]')
        v.set(x, HP + 1, z, 'sea_lantern')
    # trono de la campeona: estrado de tres peldaños con respaldo estrella
    for k in range(3):
        v.fill(CX - 6 + k, 2 + k, 106 + k, CX + 6 - k, 2 + k, GZ + GR - 3, 'polished_deepslate' if k % 2 == 0 else 'yellow_concrete')
        for x in range(CX - 6 + k, CX + 7 - k): v.stairs(x, 2 + k, 105 + k, 'quartz', 'north')
    v.fill(CX - 2, 5, GZ + 11, CX + 2, 14, GZ + 11, 'blue_concrete')
    poligono_xy(v, estrella(CX, 11, 3.5), GZ + 10, 'yellow_concrete')
    for dx in (-5, 5): v.fill(CX + dx, 5, GZ + 9, CX + dx, 10, GZ + 9, 'quartz_pillar[axis=y]'); v.set(CX + dx, 11, GZ + 9, 'sea_lantern')
