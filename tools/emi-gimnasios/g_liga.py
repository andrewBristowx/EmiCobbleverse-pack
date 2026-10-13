import math
from gymlib import V
from piezas import *

W, H, L = 81, 48, 110
CX = 40
NZ0, NZ1 = 14, 80        # nave: de la puerta al arco del campeon
CZ_CAMP = 94             # centro de la cupula del campeon
RCAMP = 15

def sala(v, x0, x1, z0, z1, alto, muro, suelo, techo, banda=None):
    v.box(x0, 1, z0, x1, alto + 1, z1, muro)
    v.fill(x0 + 1, 1, z0 + 1, x1 - 1, 1, z1 - 1, suelo)
    v.aire(x0 + 1, 2, z0 + 1, x1 - 1, alto, z1 - 1)
    v.fill(x0, alto + 1, z0, x1, alto + 1, z1, techo)
    if banda:
        v.anillo(x0, z0, x1, z1, alto, banda); v.anillo(x0, z0, x1, z1, 3, banda)

def arco(v, x, z, eje, ancho, alto, marco):
    """vano en un muro: eje='x' => el muro esta en el plano x=cte y se abre a lo largo de z"""
    h = ancho // 2
    if eje == 'x':
        v.aire(x, 2, z - h, x, alto, z + h)
        for y in range(2, alto + 1): v.set(x, y, z - h - 1, marco); v.set(x, y, z + h + 1, marco)
        v.fill(x, alto + 1, z - h - 1, x, alto + 1, z + h + 1, marco)
    else:
        v.aire(x - h, 2, z, x + h, alto, z)
        for y in range(2, alto + 1): v.set(x - h - 1, y, z, marco); v.set(x + h + 1, y, z, marco)
        v.fill(x - h - 1, alto + 1, z, x + h + 1, alto + 1, z, marco)

def torre(v, cx, cz, r, alto, muro, techo, banda):
    v.cilindro(cx, 1, alto, cz, r, muro)
    v.disco(cx, 1, cz, r, 'polished_andesite')
    v.anillo_circ(cx, alto - 1, cz, r, banda, 1.0)
    # tejado conico
    for k in range(0, r + 3):
        rr = (r + 1) * (1 - k / (r + 3)) + 0.3
        v.disco(cx, alto + 1 + k, cz, int(rr), techo)
    v.set(cx, alto + r + 4, cz, 'gold_block'); v.fill(cx, alto + r + 5, cz, cx, alto + r + 7, cz, 'lightning_rod[facing=up,powered=false,waterlogged=false]') if False else None

def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    PL, QZ, ORO, AZ = 'polished_andesite', 'smooth_quartz', 'gold_block', 'blue_concrete'
    v.fill(0, 0, 0, W - 1, 0, L - 1, PL)
    # ---- nave central: muros hasta y=13 y boveda de cañon
    NX0, NX1 = 29, 51
    v.fill(NX0, 1, NZ0, NX1, 1, NZ1, 'polished_blackstone')
    for z in range(NZ0, NZ1 + 1):
        nervio = (z - NZ0) % 6 == 0
        for x in (NX0, NX1): v.fill(x, 2, z, x, 13, z, QZ if nervio else 'white_concrete')
        for x in range(NX0, NX1 + 1):
            for y in range(14, 27):
                d = math.hypot(x - CX, y - 14)
                if 10.0 < d <= 11.4: v.set(x, y, z, 'quartz_block' if nervio else AZ)
    # suelo de la nave con alfombra y damero
    for x in range(NX0 + 1, NX1):
        for z in range(NZ0, NZ1 + 1):
            v.set(x, 1, z, 'red_concrete' if abs(x - CX) <= 2 else ('polished_blackstone' if (x + z) % 2 else 'polished_deepslate'))
    for x in (CX - 3, CX + 3):
        for z in range(NZ0, NZ1 + 1): v.set(x, 1, z, ORO)
    # pilares con estatuas de armadura (columnas) a lo largo de la nave
    for z in range(NZ0 + 6, NZ1 - 4, 6):
        for x in (NX0 + 2, NX1 - 2):
            v.fill(x, 2, z, x, 9, z, 'quartz_pillar[axis=y]'); v.set(x, 10, z, 'chiseled_quartz_block'); v.set(x, 2, z, 'chiseled_quartz_block')
            v.set(x + (1 if x < CX else -1), 8, z, 'lantern[hanging=false]') if False else None
        lampara_colgante(v, CX, 24, z, 6, 'lantern[hanging=true]'); lampara_colgante(v, CX - 5, 22, z, 3, 'lantern[hanging=true]'); lampara_colgante(v, CX + 5, 22, z, 3, 'lantern[hanging=true]')
    # ---- puerta principal (frente de la nave) y plaza con escalinata
    FZ = NZ0
    v.aire(CX - 5, 2, FZ, CX + 5, 11, FZ)
    for x in (CX - 6, CX + 6): v.fill(x, 2, FZ, x, 13, FZ, ORO)
    v.fill(CX - 6, 12, FZ, CX + 6, 13, FZ, ORO)
    for dx in range(-6, 7): v.stairs(CX + dx, 14, FZ, 'quartz', 'north')
    for z in range(0, FZ):
        anch = 14 + (FZ - z) // 2
        for x in range(CX - anch, CX + anch + 1):
            v.stairs(x, 1, z, 'quartz', 'north') if z in (FZ - 1, FZ - 3, FZ - 5, FZ - 7, FZ - 9) else v.set(x, 1, z, QZ if z < FZ - 9 else 'smooth_quartz')
    v.aire(CX - 5, 2, 0, CX + 5, 11, FZ - 1)
    # frontón sobre la puerta: escudo de la liga (pixel art 11 x 9)
    escudo = ["    ###    ", "   #####   ", "  ##   ##  ", " ##  #  ## ", " ##  #  ## ", "  ## # ##  ", "   #####   ", "    ###    "]
    v.fill(CX - 8, 14, FZ, CX + 8, 24, FZ, 'blue_concrete')
    v.fill(CX - 7, 15, FZ, CX + 7, 22, FZ, 'white_concrete')
    v.pixelart_z(CX - 5, 14, FZ - 1, escudo, {'#': 'gold_block'})
    # ---- cuatro camaras laterales (alto 12)
    camaras = {
        'shauntal': (6, 28, 16, 40),  'marshal': (6, 28, 48, 72),
        'grimsley': (52, 74, 16, 40), 'caitlin': (52, 74, 48, 72),
    }
    estilos = {
        'shauntal': ('purple_terracotta', 'deepslate_tiles', 'black_concrete', 'purple_concrete'),
        'marshal': ('white_concrete', 'smooth_sandstone', 'stripped_birch_log[axis=y]', 'red_concrete'),
        'grimsley': ('gray_concrete', 'polished_blackstone', 'black_concrete', 'red_concrete'),
        'caitlin': ('white_concrete', 'quartz_block', 'light_blue_stained_glass', 'magenta_concrete'),
    }
    for nombre, (x0, x1, z0, z1) in camaras.items():
        muro, suelo, techo, banda = estilos[nombre]
        # los muros compartidos con la nave (x=29 / x=51) ya existen; la camara llega hasta ellos
        xa, xb = (x0, 29) if x1 < 40 else (51, x1)
        sala(v, xa, xb, z0, z1, 12, muro, suelo, techo, banda)
        # puerta de la camara a la nave
        zc = (z0 + z1) // 2
        px = 29 if xb == 29 else 51
        arco(v, px, zc, 'x', 7, 8, ORO)
        # nervios y contrafuertes exteriores
        cara = x0 if px == 29 else x1
        for z in range(z0, z1 + 1, 6): v.fill(cara, 2, z, cara, 13, z, 'quartz_pillar[axis=y]') if False else v.fill(cara, 2, z, cara, 13, z, QZ)
        # luz de techo
        for x in range(xa + 3, xb - 2, 5):
            for z in range(z0 + 3, z1 - 2, 5): v.set(x, 12, z, 'sea_lantern' if nombre != 'shauntal' else 'soul_lantern[hanging=true]')
    # ---- decoracion de cada camara
    decorar_shauntal(v, 17, 38); decorar_marshal(v, 17, 70); decorar_grimsley(v, 63, 38); decorar_caitlin(v, 63, 70)
    # ---- arco al salon del campeon y cupula
    v.aire(CX - 5, 2, NZ1, CX + 5, 12, NZ1)
    for x in (CX - 6, CX + 6): v.fill(x, 2, NZ1, x, 13, NZ1, ORO)
    v.fill(CX - 6, 13, NZ1, CX + 6, 13, NZ1, ORO)
    salon_campeon(v)
    # ---- torres decorativas en las esquinas de la fachada y atras
    for (x, z) in [(4, 4), (76, 4)]: torre(v, x, z, 4, 20, 'white_concrete', 'blue_concrete', 'gold_block')
    for (x, z) in [(14, 12), (66, 12)]: torre(v, x, z, 3, 16, 'white_concrete', 'blue_concrete', 'gold_block')
    # tejados de las camaras: terrazas azules con borde de oro
    for nombre, (x0, x1, z0, z1) in camaras.items():
        xa, xb = (x0, 29) if x1 < 40 else (51, x1)
        for k in range(3):
            v.fill(xa + 1 + k * 2, 14 + k, z0 + 1 + k * 2, xb - 1 - k * 2, 14 + k, z1 - 1 - k * 2, AZ)
            v.anillo(xa + 1 + k * 2, z0 + 1 + k * 2, xb - 1 - k * 2, z1 - 1 - k * 2, 14 + k, ORO if k == 2 else 'quartz_block')
    v.fill(30, 14, 12, 50, 14, 13, AZ) if False else None
    return v, [
        (17, 4, 38, 180, 'emipokemon desafios teselia elite_shauntal'), (17, 4, 70, 180, 'emipokemon desafios teselia elite_marshal'),
        (63, 4, 38, 180, 'emipokemon desafios teselia elite_grimsley'), (63, 4, 70, 180, 'emipokemon desafios teselia elite_caitlin'),
        (CX, 5, CZ_CAMP + 10, 180, 'emipokemon desafios teselia champion_alder')]

def tarima(v, cx, z, ancho, mat, esc, y=2):
    v.fill(cx - ancho, y, z - 2, cx + ancho, y, z + 1, mat)
    for x in range(cx - ancho, cx + ancho + 1): v.stairs(x, y, z - 3, esc, 'north')
    v.fill(cx - ancho + 2, y + 1, z - 1, cx + ancho - 2, y + 1, z + 1, mat)
    for x in range(cx - ancho + 2, cx + ancho - 1): v.stairs(x, y + 1, z - 2, esc, 'north')

def decorar_shauntal(v, cx, z):
    # biblioteca encantada: estanterias, velas, telarañas, calaveras
    for x in range(8, 28, 1): 
        if abs(x - cx) > 4: v.fill(x, 2, 39, x, 9, 39, 'bookshelf')
    for x in range(7, 28, 4):
        v.fill(x, 2, 17, x, 8, 17, 'bookshelf'); v.fill(x, 2, 18, x, 3, 18, 'bookshelf')
    for (x, zz) in [(8, 17), (27, 17), (8, 38), (27, 38)]:
        v.set(x, 11, zz, 'cobweb'); v.set(x, 10, zz, 'cobweb')
    for (dx, dz) in [(-6, -10), (6, -10), (-6, -2), (6, -2)]:
        v.fill(cx + dx, 2, z + dz, cx + dx, 3, z + dz, 'purple_concrete'); v.set(cx + dx, 4, z + dz, 'candle[candles=3,lit=true]')
    # circulo ritual en el suelo
    for x in range(8, 28):
        for zz in range(18, 38):
            d = math.hypot(x - cx, zz - 28)
            if 6.5 < d < 7.5 or d < 1.5: v.set(x, 1, zz, 'purple_concrete')
            elif 3.5 < d < 4.5: v.set(x, 1, zz, 'black_concrete')
    for a in range(0, 360, 60): v.set(int(round(cx + math.cos(math.radians(a)) * 7)), 2, int(round(28 + math.sin(math.radians(a)) * 7)), 'skeleton_skull[rotation=0]')
    tarima(v, cx, z - 1, 5, 'purple_terracotta', 'purple_glazed_terracotta' if False else 'deepslate_tile')
    v.fill(cx - 2, 4, z + 1, cx + 2, 4, z + 1, 'dark_oak_slab[type=bottom]')

def decorar_marshal(v, cx, z):
    # dojo: tatami, columnas de madera, sacos de arena, estandartes rojos
    for x in range(7, 28):
        for zz in range(49, 72): v.set(x, 1, zz, 'smooth_sandstone' if (x // 3 + zz // 3) % 2 else 'birch_planks')
    for x in range(11, 24):
        for zz in range(53, 68):
            if x in (11, 23) or zz in (53, 67): v.set(x, 1, zz, 'red_concrete')
    v.fill(14, 1, 56, 20, 1, 64, 'white_concrete'); v.fill(15, 1, 57, 19, 1, 63, 'light_gray_concrete')
    for zz in range(50, 72, 4):
        for x in (7, 27): v.fill(x, 2, zz, x, 10, zz, 'stripped_birch_log[axis=y]')
        v.fill(7, 10, zz, 27, 10, zz, 'stripped_birch_log[axis=x]') if False else None
    for x in range(8, 27, 6):
        lampara_colgante(v, x, 12, 55, 2, 'lantern[hanging=true]'); lampara_colgante(v, x, 12, 65, 2, 'lantern[hanging=true]')
    for (x, zz) in [(9, 51), (25, 51), (9, 69), (25, 69)]:
        v.set(x, 11, zz, 'chain[axis=y]'); v.set(x, 10, zz, 'chain[axis=y]'); v.set(x, 9, zz, 'hay_block[axis=y]'); v.set(x, 8, zz, 'hay_block[axis=y]')
    for x in range(8, 27, 3):
        v.fill(x, 5, 72, x, 9, 72, 'red_concrete') if abs(x - cx) > 3 else None
    for x in range(10, 25, 2): v.set(x, 3, 71, 'target')  if False else None
    tarima(v, cx, z - 1, 4, 'birch_planks', 'birch')

def decorar_grimsley(v, cx, z):
    # salon oscuro: barra, mesas de cartas, lamparas
    for x in range(53, 74):
        for zz in range(17, 40): v.set(x, 1, zz, 'black_concrete' if (x + zz) % 2 else 'gray_concrete')
    for x in range(60, 67): 
        for zz in range(20, 36): v.set(x, 1, zz, 'red_concrete') if (x in (60, 66) or zz in (20, 35)) else None
    for x in range(54, 73):
        v.set(x, 2, 18, 'barrel[facing=up]' if x % 2 else 'polished_blackstone'); v.set(x, 3, 18, 'polished_blackstone_slab[type=bottom]')
    for x in (56, 60, 66, 70): v.set(x, 4, 18, 'brewing_stand')
    for (dx, dz) in [(-8, -6), (8, -6), (-8, 6), (8, 6)]:
        x, zz = cx + dx, 28 + dz
        v.set(x, 2, zz, 'spruce_fence'); v.set(x, 3, zz, 'spruce_slab[type=top]'); v.set(x, 4, zz, 'green_carpet')
        v.stairs(x - 1, 2, zz, 'dark_oak', 'east'); v.stairs(x + 1, 2, zz, 'dark_oak', 'west'); v.stairs(x, 2, zz - 1, 'dark_oak', 'south'); v.stairs(x, 2, zz + 1, 'dark_oak', 'north')
    for x in range(55, 72, 4):
        for zz in (22, 34): lampara_colgante(v, x, 12, zz, 3, 'lantern[hanging=true]')
    v.fill(54, 2, 39, 72, 8, 39, 'red_concrete'); v.fill(56, 4, 39, 70, 4, 39, 'black_concrete')
    tarima(v, cx, z - 1, 5, 'polished_blackstone', 'polished_blackstone_brick')
    v.set(cx, 4, z + 1, 'jukebox')

def decorar_caitlin(v, cx, z):
    # sala onirica: suelo claro con estrella, cristales de amatista, varas del end
    for x in range(53, 74):
        for zz in range(49, 72): v.set(x, 1, zz, 'white_concrete' if (x + zz) % 3 else 'light_blue_concrete')
    for x in range(53, 74):
        for zz in range(49, 72):
            d = math.hypot(x - cx, zz - 60)
            if d < 2 or 5.5 < d < 6.5: v.set(x, 1, zz, 'magenta_concrete')
            elif 8.5 < d < 9.2: v.set(x, 1, zz, 'purple_concrete')
    for (dx, dz) in [(-8, -8), (8, -8), (-8, 8), (8, 8), (0, -10), (0, 10)]:
        x, zz = cx + dx, 60 + dz
        v.fill(x, 2, zz, x, 7, zz, 'amethyst_block'); v.set(x, 8, zz, 'amethyst_cluster[facing=up]'); v.set(x + 1, 2, zz, 'amethyst_cluster[facing=east]'); v.set(x - 1, 2, zz, 'amethyst_cluster[facing=west]')
        v.set(x, 9, zz, 'end_rod[facing=up]')
    for x in range(55, 72, 5):
        for zz in (52, 60, 68): lampara_colgante(v, x, 12, zz, 4, 'sea_lantern', 'end_rod[facing=up]' if False else 'chain[axis=y]')
    v.fill(54, 2, 72, 72, 10, 72, 'white_stained_glass'); v.fill(56, 3, 72, 70, 9, 72, 'light_blue_stained_glass')
    tarima(v, cx, z - 1, 5, 'quartz_block', 'quartz')

def salon_campeon(v):
    cz = CZ_CAMP; R = RCAMP
    ORO = 'gold_block'
    v.cilindro(CX, 1, 14, cz, R, 'white_concrete')
    v.disco(CX, 1, cz, R, 'smooth_quartz')
    v.cupula(CX, 15, cz, R, 'light_blue_stained_glass', hueco=True, ry=13)
    for a in range(0, 360, 30):
        for k in range(0, 14):
            ang = math.radians(a); el = math.radians(k * 6.9)
            x = int(round(CX + math.cos(ang) * (R - 0.3) * math.cos(el))); z = int(round(cz + math.sin(ang) * (R - 0.3) * math.cos(el))); y = 15 + int(round(math.sin(el) * 13))
            v.set(x, y, z, 'quartz_block' if k % 2 else ORO)
    v.set(CX, 28, cz, ORO)
    # suelo: estrella de oro y anillos
    for x in range(CX - R, CX + R + 1):
        for z in range(cz - R, cz + R + 1):
            d = math.hypot(x - CX, z - cz)
            if d <= R - 0.6:
                col = 'smooth_quartz'
                if d < 2: col = ORO
                elif 5.5 < d < 6.5: col = 'blue_concrete'
                elif 9.5 < d < 10.5: col = ORO
                elif (x + z) % 2 == 0 and d > 11: col = 'white_concrete'
                v.set(x, 1, z, col)
    # columnas en circulo
    for i in range(10):
        a = i * math.pi / 5 + 0.3
        x = int(round(CX + math.cos(a) * (R - 2))); z = int(round(cz + math.sin(a) * (R - 2)))
        if z < cz - R + 5 and abs(x - CX) < 6: continue
        v.fill(x, 2, z, x, 13, z, 'quartz_pillar[axis=y]'); v.set(x, 14, z, ORO); v.set(x, 2, z, 'chiseled_quartz_block')
    # puerta del salon: ya hay un arco en la nave. Trono del campeon al fondo
    v.fill(CX - 7, 2, cz + 8, CX + 7, 2, cz + 12, 'smooth_quartz')
    for x in range(CX - 7, CX + 8): v.stairs(x, 2, cz + 7, 'quartz', 'north')
    v.fill(CX - 4, 3, cz + 10, CX + 4, 3, cz + 12, 'smooth_quartz')
    for x in range(CX - 4, CX + 5): v.stairs(x, 3, cz + 9, 'quartz', 'north')
    v.fill(CX - 2, 4, cz + 12, CX + 2, 12, cz + 12, 'blue_concrete'); v.fill(CX - 1, 6, cz + 12, CX + 1, 10, cz + 12, ORO)
    for dx in (-3, 3): v.fill(CX + dx, 4, cz + 11, CX + dx, 8, cz + 11, ORO)
    # lamparas colgantes
    for (dx, dz) in [(0, 0), (6, 0), (-6, 0), (0, 6), (0, -6)]:
        lampara_colgante(v, CX + dx, 26 - (abs(dx) + abs(dz)) // 2, cz + dz, 6, 'lantern[hanging=true]')
