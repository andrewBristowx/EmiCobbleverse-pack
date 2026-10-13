"""Decoracion con temática Pokémon para la pista: tunel-Pokéball, Centro Pokémon, Tienda, Pikachu y Snorlax gigantes."""
import math


def tunel_pokeball(v, ms, ox, oz, k0, k1, sup_y, medio, R=None):
    """media Pokéball hueca que cubre la pista entre las muestras k0..k1: rojo arriba, banda negra y blanco abajo, con luces."""
    R = R or medio + 3.0
    n = len(ms)
    k = k0
    pasos = 0
    while k != k1:
        m = ms[k]
        nx, nz = -m['tz'], m['tx']
        y0 = sup_y(k)
        o = -R
        while o <= R:
            for h in range(0, int(R) + 2):
                d = math.hypot(o, h)
                if R - 1.7 < d <= R:
                    px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
                    f = h / R
                    b = 'red_concrete' if f > 0.62 else ('black_concrete' if f > 0.42 else 'white_concrete')
                    v.set(px, y0 + 1 + h, pz, b)
            o += 0.4
        if pasos % 12 == 6:      # luz en lo alto
            px, pz = int(round(m['x'] + ox)), int(round(m['z'] + oz))
            v.set(px, y0 + 1 + int(R) - 1, pz, 'sea_lantern')
            v.set(int(round(m['x'] + ox + nx * 3)), y0 + 1 + int(R) - 2, int(round(m['z'] + oz + nz * 3)), 'sea_lantern')
            v.set(int(round(m['x'] + ox - nx * 3)), y0 + 1 + int(R) - 2, int(round(m['z'] + oz - nz * 3)), 'sea_lantern')
        k = (k + 1) % n; pasos += 1


def edificio(v, x0, z0, ancho, fondo, alto, paredes, techo, cartel_col, nombre=''):
    """caja con puerta al norte (-z), ventanas de cristal y techo plano con borde"""
    y = 1
    v.fill(x0 - 1, 0, z0 - 1, x0 + ancho, 0, z0 + fondo, 'smooth_stone')
    v.box(x0, y, z0, x0 + ancho - 1, y + alto - 1, z0 + fondo - 1, paredes)
    v.fill(x0 - 1, y + alto, z0 - 1, x0 + ancho, y + alto, z0 + fondo, techo)
    v.fill(x0 - 1, y + alto + 1, z0 - 1, x0 + ancho, y + alto + 1, z0 - 1, techo); v.fill(x0 - 1, y + alto + 1, z0 + fondo, x0 + ancho, y + alto + 1, z0 + fondo, techo)
    v.fill(x0 - 1, y + alto + 1, z0 - 1, x0 - 1, y + alto + 1, z0 + fondo, techo); v.fill(x0 + ancho, y + alto + 1, z0 - 1, x0 + ancho, y + alto + 1, z0 + fondo, techo)
    cx = x0 + ancho // 2
    v.aire(cx - 1, y, z0, cx + 1, y + 2, z0)                                  # puerta
    for x in range(x0 + 2, x0 + ancho - 2, 4):
        if abs(x - cx) > 2: v.fill(x, y + 2, z0, x + 1, y + 3, z0, 'light_blue_stained_glass')
    v.fill(x0 + 1, y + alto - 1, z0, x0 + ancho - 2, y + alto - 1, z0, cartel_col)
    v.fill(cx - 1, y + alto - 2, z0 - 1, cx + 1, y + alto - 2, z0 - 1, cartel_col)
    # luz interior
    v.set(cx, y + alto - 1, z0 + fondo // 2, 'sea_lantern')


def pokeball_cartel(v, cx, cy, z, r):
    """Pokéball plana (en un muro mirando a -z)"""
    for x in range(cx - r, cx + r + 1):
        for yy in range(cy - r, cy + r + 1):
            d = math.hypot(x - cx, yy - cy)
            if d > r + 0.3: continue
            if d < 1.6: b = 'white_concrete'
            elif abs(yy - cy) <= 0: b = 'black_concrete'
            else: b = 'red_concrete' if yy > cy else 'white_concrete'
            v.set(x, yy, z, b)
    v.set(cx, cy, z, 'light_gray_concrete')


def pikachu(v, cx, cy, z):
    """Pikachu de pixel-art de ~26 de alto sobre un bastidor (se ve por delante y por detras)"""
    def el(ex, ey, rx, ry, b):
        for x in range(int(ex - rx) - 1, int(ex + rx) + 2):
            for yy in range(int(ey - ry) - 1, int(ey + ry) + 2):
                if math.hypot((x - ex) / rx, (yy - ey) / ry) <= 1.0:
                    for dz in (0, 1): v.set(x, yy, z + dz, b)
    el(cx, cy - 8, 6.5, 7.5, 'yellow_concrete')                      # cuerpo
    el(cx - 9, cy - 8, 2, 5, 'yellow_concrete'); el(cx + 9, cy - 8, 2, 5, 'yellow_concrete')   # brazos
    el(cx, cy, 10, 8, 'yellow_concrete')                             # cabeza
    for sg in (-1, 1):
        for h in range(0, 13):                                       # orejas inclinadas
            ex = cx + sg * (6 + h * 0.45)
            for dz in (0, 1):
                v.set(int(round(ex)), cy + 6 + h, z + dz, 'black_concrete' if h >= 9 else 'yellow_concrete')
                v.set(int(round(ex)) + sg, cy + 6 + h, z + dz, 'black_concrete' if h >= 9 else 'yellow_concrete')
        el(cx + sg * 7.5, cy - 2, 2.3, 2.3, 'red_concrete')          # mejillas
        for dz in (0, 1):
            for ex2, ey2 in ((cx + sg * 4, cy + 2), (cx + sg * 4, cy + 3), (cx + sg * 5, cy + 2), (cx + sg * 5, cy + 3)): v.set(ex2, ey2, z + dz, 'black_concrete')
            v.set(cx + sg * 4, cy + 3, z + dz, 'white_concrete')
    for dz in (0, 1):
        for ex2 in (cx - 1, cx, cx + 1): v.set(ex2, cy - 1, z + dz, 'black_concrete')
        v.set(cx, cy + 0, z + dz, 'black_concrete')
    # cola en zigzag
    for i, (dx, dy) in enumerate([(9, -12), (11, -9), (13, -9), (13, -6), (15, -6), (15, -3), (18, -3), (18, 3), (14, 3)]):
        for dz in (0, 1): v.fill(cx + dx, cy + dy, z + dz, cx + dx + 2, cy + dy + 1, z + dz, 'orange_concrete' if i < 6 else 'yellow_concrete')
    # bastidor trasero y base
    v.fill(cx - 6, 1, z + 2, cx - 5, cy - 14, z + 2, 'stone_bricks'); v.fill(cx + 5, 1, z + 2, cx + 6, cy - 14, z + 2, 'stone_bricks')
    v.fill(cx - 12, 1, z - 2, cx + 12, 1, z + 4, 'smooth_stone')


def snorlax(v, cx, cz, rnd):
    """Snorlax durmiendo, grande, de cara al norte (-z)"""
    def elip(ex, ey, ez, rx, ry, rz, b, inv=None):
        for x in range(int(ex - rx) - 1, int(ex + rx) + 2):
            for yy in range(int(ey - ry) - 1, int(ey + ry) + 2):
                for z in range(int(ez - rz) - 1, int(ez + rz) + 2):
                    if ((x - ex) / rx) ** 2 + ((yy - ey) / ry) ** 2 + ((z - ez) / rz) ** 2 <= 1.0:
                        v.set(x, yy, z, inv(x, yy, z) if inv else b)
    base = 1
    elip(cx, base + 9, cz, 17, 11, 13, 'cyan_terracotta')                                   # cuerpo
    # barriga crema
    elip(cx, base + 8, cz - 5, 11, 8, 9, 'cyan_terracotta', lambda x, yy, z: 'smooth_sandstone' if z < cz - 5 + 2 and ((x - cx) / 11) ** 2 + ((yy - (base + 8)) / 8) ** 2 < 0.85 else 'cyan_terracotta')
    # cabeza
    elip(cx, base + 17, cz - 7, 9, 7, 7, 'cyan_terracotta')
    elip(cx, base + 15, cz - 12, 6.5, 4.5, 3, 'smooth_sandstone')                           # hocico crema
    for sg in (-1, 1):
        elip(cx + sg * 7, base + 23, cz - 6, 2.2, 2.5, 2, 'black_concrete')                  # orejas
        elip(cx + sg * 14, base + 4, cz - 6, 4, 4, 4, 'cyan_terracotta')                     # brazos
        elip(cx + sg * 14, base + 3, cz - 9, 3, 3, 2, 'smooth_sandstone')                    # manos
        elip(cx + sg * 8, base + 2, cz - 12, 4, 2.5, 4, 'smooth_sandstone')                  # pies
        for dx in (-3, -2, -1, 0, 1, 2, 3):                                                    # ojos cerrados
            pass
        v.fill(cx + sg * 4 - 1, base + 18, cz - 14, cx + sg * 4 + 1, base + 18, cz - 14, 'black_concrete')
    v.fill(cx - 2, base + 14, cz - 15, cx + 2, base + 14, cz - 15, 'black_concrete')           # boca
    v.set(cx - 2, base + 13, cz - 15, 'white_concrete'); v.set(cx + 2, base + 13, cz - 15, 'white_concrete')
    v.set(cx, base + 16, cz - 15, 'black_concrete')
    # mechon de pelo
    for dz in (-1, 0, 1): v.set(cx, base + 24, cz - 6 + dz, 'black_concrete')
