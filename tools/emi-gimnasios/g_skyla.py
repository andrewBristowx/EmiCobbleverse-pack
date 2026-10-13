import math
from gymlib import V
from piezas import *

W, H, L = 37, 28, 46
CX = 18
ZF, ZB = 14, 45     # hangar: de la boca (z=14) al fondo
R = 11

def avion(v, x, y, z, largo=11, ala=15):
    """avioneta de bloques: fuselaje a lo largo de z (morro hacia -z)"""
    B, G, A = 'white_concrete', 'light_gray_concrete', 'light_blue_concrete'
    v.fill(x - 1, y, z, x + 1, y + 2, z + largo, B)
    v.fill(x, y + 3, z + 2, x, y + 3, z + largo - 2, B) if False else None
    for dz in range(0, 3): v.fill(x - 1 + (dz == 2), y + dz, z - 1 - 0, x + 1 - (dz == 2), y + dz, z - 1, B) if False else None
    v.fill(x, y + 1, z - 1, x, y + 1, z - 1, G); v.set(x, y + 2, z + 2, 'light_blue_stained_glass'); v.fill(x - 1, y + 2, z + 1, x + 1, y + 2, z + 3, 'light_blue_stained_glass')
    v.fill(x - ala // 2, y + 1, z + 4, x + ala // 2, y + 1, z + 7, B)               # alas
    v.fill(x - ala // 2, y + 1, z + 4, x - ala // 2, y + 1, z + 7, A); v.fill(x + ala // 2, y + 1, z + 4, x + ala // 2, y + 1, z + 7, A)
    v.fill(x - 3, y + 1, z + largo - 1, x + 3, y + 1, z + largo, B)                 # cola horizontal
    v.fill(x, y + 3, z + largo - 3, x, y + 5, z + largo, A)                         # deriva
    v.fill(x, y + 1, z - 1, x, y + 1, z - 1, 'iron_bars') ; v.fill(x - 1, y, z - 1, x + 1, y, z - 1, 'iron_bars') if False else None
    v.set(x, y + 1, z - 1, 'iron_bars'); v.set(x, y, z - 1, 'iron_bars'); v.set(x, y + 2, z - 1, 'iron_bars')   # helice
    v.set(x - 1, y - 1, z + 2, 'black_concrete'); v.set(x + 1, y - 1, z + 2, 'black_concrete'); v.set(x, y - 1, z + largo - 1, 'black_concrete')

def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    ASF, BL, AZ = 'gray_concrete', 'white_concrete', 'light_blue_concrete'
    v.fill(0, 0, 0, W - 1, 0, L - 1, ASF)
    # pista: franja de aterrizaje con rayas blancas y luces laterales
    for z in range(0, ZF):
        v.fill(CX - 6, 0, z, CX + 6, 0, z, 'black_concrete')
        if z % 4 < 2: v.set(CX, 0, z, BL)
        v.set(CX - 6, 0, z, BL); v.set(CX + 6, 0, z, BL)
        if z % 3 == 0: v.set(CX - 7, 0, z, 'sea_lantern'); v.set(CX + 7, 0, z, 'sea_lantern')
    # el suelo del hangar y plataforma
    v.fill(CX - R, 1, ZF, CX + R, 1, ZB, 'polished_andesite')
    # bóveda semicircular (cilindro hacia z): carcasa blanca con nervios de cuarzo cada 5
    for z in range(ZF, ZB + 1):
        nervio = (z - ZF) % 5 == 0 or z == ZB
        for x in range(CX - R - 1, CX + R + 2):
            for y in range(2, R + 2):
                d = math.hypot(x - CX, y - 1)
                if R - 1.0 < d <= R + 0.35:
                    v.set(x, y, z, 'quartz_block' if nervio else (AZ if (y > 7) else BL))
    # faja de color y ventanales en el arco
    for z in range(ZF + 2, ZB - 1, 5):
        for a in range(35, 146, 10):
            x = int(round(CX + math.cos(math.radians(a)) * R)); y = 1 + int(round(math.sin(math.radians(a)) * R))
            v.set(x, y, z, 'light_blue_stained_glass'); v.set(x, y, z + 1, 'light_blue_stained_glass'); v.set(x, y, z + 2, 'light_blue_stained_glass')
    # pared del fondo
    for x in range(CX - R, CX + R + 1):
        for y in range(2, R + 1):
            if math.hypot(x - CX, y - 1) <= R - 0.7: v.set(x, y, ZB, BL)
    # boca del hangar: marco y puertas correderas abiertas a los lados
    for x in range(CX - R - 1, CX + R + 2):
        for y in range(2, R + 2):
            d = math.hypot(x - CX, y - 1)
            if R - 2.0 < d <= R + 0.35: v.set(x, y, ZF, 'quartz_block' if d > R - 1 else 'light_gray_concrete')
    for s in (-1, 1):
        v.fill(CX + s * (R - 3), 2, ZF - 1, CX + s * (R - 1), 9, ZF - 1, 'gray_concrete')
        for y in range(2, 10, 2): v.fill(CX + s * (R - 3), y, ZF - 1, CX + s * (R - 1), y, ZF - 1, 'light_gray_concrete')
    # rotulo SKYLA / pixel art de ala sobre la boca
    ala = [" #   #   # ", "  ##  # ## ", "   ########", "    ###### ", "     ####  "]
    v.pixelart_z(CX - 5, 14, ZF - 1, ["  #     #  ", " ###   ### ", "###########", " #########  "], {'#': 'light_blue_concrete'})
    # torre de control a un lado
    tx, tz = CX + 16, ZF + 6
    v.fill(tx - 1, 1, tz - 1, tx + 1, 14, tz + 1, 'white_concrete')
    v.fill(tx - 2, 15, tz - 2, tx + 2, 15, tz + 2, 'light_gray_concrete')
    v.fill(tx - 2, 16, tz - 2, tx + 2, 18, tz + 2, 'light_blue_stained_glass'); v.aire(tx - 1, 16, tz - 1, tx + 1, 18, tz + 1)
    v.fill(tx - 3, 19, tz - 3, tx + 3, 19, tz + 3, 'gray_concrete')
    v.fill(tx, 20, tz, tx, 24, tz, 'iron_bars'); v.set(tx, 25, tz, 'sea_lantern')
    for y in range(2, 14, 4): v.set(tx, y, tz - 2, 'sea_lantern') if False else None
    # manga de viento
    v.fill(CX - 14, 1, ZF + 3, CX - 14, 9, ZF + 3, 'iron_bars')
    for i, c in enumerate(['red_concrete', 'white_concrete', 'red_concrete', 'white_concrete']):
        v.set(CX - 15 - i, 9 - (i // 2), ZF + 3, c)
    # interior: circulo de lanzamiento y flechas
    cz = ZF + 14
    for x in range(CX - 9, CX + 10):
        for z in range(ZF + 3, ZB - 2):
            d = math.hypot(x - CX, z - cz)
            if d < 2.5: v.set(x, 1, z, 'yellow_concrete')
            elif 5 < d < 6.5: v.set(x, 1, z, AZ)
            elif 8 < d < 9.2: v.set(x, 1, z, BL)
    for z in range(ZF + 2, ZB - 3):
        if (z - ZF) % 3 == 0: v.set(CX, 1, z, 'blue_concrete')
    # avion expuesto, a la izquierda del fondo (pegado a la pared)
    avion(v, CX + 6, 2, ZB - 15, 11, 9)
    # estructura de ventiladores colgantes y luces
    for z in range(ZF + 4, ZB - 2, 6):
        for dx in (-5, 0, 5): lampara_colgante(v, CX + dx, 10 if dx == 0 else 8, z, 2, 'sea_lantern')
    # plataforma de control del lider (fondo) con una cabina de cristal
    v.fill(CX - 6, 2, ZB - 6, CX + 3, 2, ZB - 1, 'light_gray_concrete')
    v.fill(CX - 5, 3, ZB - 4, CX + 2, 3, ZB - 1, 'white_concrete')
    for x in range(CX - 6, CX + 4): v.stairs(x, 2, ZB - 7, 'polished_andesite', 'north') if False else v.stairs(x, 2, ZB - 7, 'quartz', 'north')
    for x in range(CX - 5, CX + 3): v.stairs(x, 3, ZB - 5, 'quartz', 'north')
    v.fill(CX - 5, 4, ZB - 1, CX + 2, 5, ZB - 1, 'blue_concrete')
    v.fill(CX - 4, 4, ZB - 2, CX + 1, 4, ZB - 2, 'light_gray_concrete')
    v.set(CX - 2, 5, ZB - 2, 'sea_lantern'); v.set(CX, 5, ZB - 2, 'sea_lantern')
    # rotor de ventilador en la pared del fondo (aspas de barras de hierro)
    for a in range(0, 360, 45):
        for r in range(2, 6): v.set(CX - 1 + int(round(math.cos(math.radians(a)) * r)), 7 + int(round(math.sin(math.radians(a)) * r)), ZB - 1, 'iron_bars')
    v.set(CX - 1, 7, ZB - 1, 'blue_concrete')
    return v, [(CX - 1, 4, ZB - 3, 180, 'emipokemon desafios teselia reactor')]
