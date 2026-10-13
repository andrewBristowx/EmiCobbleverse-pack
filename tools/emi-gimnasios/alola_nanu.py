"""Gran prueba de Nanu (Siniestro) en Ciudad Malie de noche: calle con neones, una cafeteria de malasadas, la comisaria con su coche y un callejon sin salida."""
import math
from gymlib import V
from piezas2 import *
from alolalib import *
from ligalib import arco

W, H, L = 41, 28, 44
CX = 20


def edificio(v, x0, x1, z0, z1, alto, muro, rnd, neon, lado):
    """bloque de tres plantas con ventanas iluminadas; `lado` = +1 si la fachada mira hacia +x (edificio de la izquierda) o -1"""
    v.fill(x0, 1, z0, x1, alto, z1, muro)
    xf = x1 if lado > 0 else x0
    for y in range(4, alto - 1, 4):
        for z in range(z0 + 2, z1 - 1, 4):
            for dz in (0, 1): v.set(xf, y, z + dz, 'yellow_stained_glass' if rnd.random() < 0.6 else 'black_concrete'); v.set(xf, y + 1, z + dz, 'yellow_stained_glass' if rnd.random() < 0.6 else 'black_concrete')
    v.fill(x0, alto + 1, z0, x1, alto + 1, z1, 'gray_concrete')
    for z in range(z0, z1 + 1, 4): v.fill(x0, alto + 2, z, x1, alto + 2, z, 'iron_bars')
    for y in (1, 2): v.fill(xf, y, z0, xf, y, z1, 'black_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(401)
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'black_concrete')
    for z in range(0, L):
        for x in range(W):
            if x < 12 or x > 28: v.set(x, 0, z, 'light_gray_concrete' if (x + z) % 7 else 'gray_concrete')
            elif x == CX and z % 4 < 2: v.set(x, 0, z, 'yellow_concrete')
    for z in range(2, 36, 8):
        for x in range(14, 27): v.set(x, 0, z, 'white_concrete') if x % 2 == 0 else None
    # ---- edificios: cafeteria de malasadas a la izquierda, comisaria a la derecha
    edificio(v, 1, 11, 6, 40, 14, 'red_terracotta', rnd, 'pink', 1)
    edificio(v, 29, 39, 6, 40, 16, 'blue_terracotta', rnd, 'cyan', -1)
    for z in (10, 18, 26):                                                               # toldo de la cafeteria
        for k in range(3): v.fill(12, 5 - k // 2, z + k, 14, 5 - k // 2, z + k, 'red_wool' if (z // 8 + k) % 2 else 'white_wool')
    v.fill(12, 1, 8, 12, 4, 8, 'oak_fence'); v.fill(12, 1, 30, 12, 4, 30, 'oak_fence')
    for z in range(8, 31, 3): v.set(12, 5, z, 'red_wool' if (z // 3) % 2 else 'white_wool')
    # cartel de la comisaria: escudo azul y blanco y luz giratoria
    for z in range(10, 38, 8):
        v.fill(28, 6, z, 28, 8, z + 2, 'white_concrete'); v.fill(28, 7, z, 28, 7, z + 2, 'light_blue_concrete')
    v.fill(28, 1, 13, 28, 5, 14, 'blue_concrete'); v.set(28, 6, 13, 'sea_lantern'); v.set(28, 6, 14, 'red_concrete')
    # ---- coche patrulla aparcado
    x, z = 25, 12
    v.fill(x - 1, 1, z, x + 1, 2, z + 7, 'white_concrete'); v.fill(x - 1, 1, z + 1, x + 1, 1, z + 2, 'blue_concrete'); v.fill(x - 1, 1, z + 5, x + 1, 1, z + 6, 'blue_concrete')
    v.fill(x - 1, 3, z + 2, x + 1, 3, z + 5, 'light_blue_stained_glass'); v.fill(x - 1, 4, z + 2, x + 1, 4, z + 5, 'white_concrete')
    v.set(x - 1, 5, z + 3, 'red_concrete'); v.set(x + 1, 5, z + 3, 'cyan_concrete')
    for (dx, dz) in ((-2, 1), (2, 1), (-2, 6), (2, 6)): v.set(x + dx, 1, z + dz, 'black_concrete')
    # ---- mobiliario urbano: farolas, contenedores, maquina expendedora, hidrante
    for z in (6, 18, 30, 38):
        for x in (13, 27): v.fill(x, 1, z, x, 6, z, 'iron_bars'); v.fill(x - 1 if x < 20 else x, 6, z, x + 1 if x < 20 else x, 6, z, 'iron_bars'); v.set(x + (1 if x < 20 else -1), 6, z, 'gray_concrete'); v.set(x + (1 if x < 20 else -1), 5, z, 'lantern[hanging=true]')
    for (x, z) in [(13, 12), (13, 24)]: v.fill(x, 1, z, x + 1, 2, z + 1, 'green_concrete'); v.set(x, 3, z, 'iron_bars')
    v.fill(13, 1, 33, 13, 4, 34, 'red_concrete'); v.fill(13, 2, 33, 13, 3, 34, 'glass')
    v.set(26, 1, 20, 'red_concrete'); v.set(26, 2, 20, 'iron_bars')
    # ---- fondo: muro con arco iluminado, neon de luna y escalones
    v.fill(12, 1, 41, 28, 14, 43, 'gray_concrete')
    arco(v, CX, 41, 'z', 7, 8, 'black_concrete', y0=1)
    v.aire(CX - 3, 1, 41, CX + 3, 8, 43)
    for y in range(1, 9): v.set(CX - 4, y, 40, 'black_concrete'); v.set(CX + 4, y, 40, 'black_concrete')
    elipse_xy(v, CX, 11, 40, 3.5, 3.5, lambda d, a: 'pink_concrete' if d > 0.62 else 'magenta_concrete')
    for dx in range(-7, 8, 2): v.set(CX + dx, 13, 40, 'cyan_concrete')
    for x in range(14, 27): v.set(x, 7, 40, 'purple_concrete') if abs(x - CX) > 4 else None
    # ---- rincon del lider: banco, cartel y charco de neon
    v.fill(CX - 3, 1, 34, CX + 3, 1, 36, 'light_gray_concrete')
    for x in range(CX - 3, CX + 4): v.stairs(x, 1, 33, 'stone_brick', 'north') if abs(x - CX) <= 1 else None
    v.fill(CX - 2, 2, 36, CX + 2, 2, 36, 'dark_oak_slab[type=bottom]')
    v.fill(CX - 4, 1, 35, CX - 4, 6, 35, 'iron_bars'); v.fill(CX + 4, 1, 35, CX + 4, 6, 35, 'iron_bars'); v.fill(CX - 4, 7, 35, CX + 4, 7, 35, 'iron_bars')
    v.set(CX - 3, 6, 35, 'soul_lantern[hanging=true]'); v.set(CX + 3, 6, 35, 'soul_lantern[hanging=true]')
    for x in range(CX - 2, CX + 3): v.set(x, 0, 30, 'pink_concrete') if (x + 30) % 2 else v.set(x, 0, 30, 'cyan_concrete')
    return v, [(CX, 2, 35, 180, 'emipokemon desafios alola kahuna_nanu')]
