import math
from gymlib import V
from piezas import *

W, H, L = 37, 36, 42
CX = 18
Z0, Z1 = 7, 40
X0, X1 = 3, 33

def tubo(v, pts, r, bloque, alt=None, paso=0.5):
    """camino grueso (esferas a lo largo de una poligonal); alt = bloque alterno cada pocos pasos"""
    n = 0
    for (a, b) in zip(pts, pts[1:]):
        d = max(abs(a[i] - b[i]) for i in range(3))
        for i in range(int(d / paso) + 1):
            t = i / max(1, d / paso)
            x, y, z = (a[k] + (b[k] - a[k]) * t for k in range(3))
            n += 1
            bl = alt if (alt and (n // 6) % 3 == 2) else bloque
            for dx in range(-int(r) - 1, int(r) + 2):
                for dy in range(-int(r) - 1, int(r) + 2):
                    for dz in range(-int(r) - 1, int(r) + 2):
                        if dx * dx + dy * dy + dz * dz <= r * r:
                            xx, yy, zz = int(round(x)) + dx, int(round(y)) + dy, int(round(z)) + dz
                            if v.get(xx, yy, zz) == 'air': v.set(xx, yy, zz, bl)

def cabeza_dragon(v, cx, y, z, nivel='bajo'):
    """cabeza de dragon mirando a -z: craneo, hocico, mandibula abierta con colmillos, cuernos dorados y ojos rojos. (cx,y,z) = parte de atras del craneo, base."""
    G, DG, ORO, ROJ, NEG, BL = 'green_concrete', 'lime_concrete', 'gold_block', 'red_concrete', 'black_concrete', 'quartz_block'
    # craneo 7 ancho x 5 alto x 6 largo
    v.fill(cx - 3, y + 1, z, cx + 3, y + 5, z + 5, G)
    # hocico alargado hacia el frente (-z), 5 de ancho, 3 de alto
    v.fill(cx - 2, y + 3, z - 6, cx + 2, y + 5, z - 1, G)
    v.fill(cx - 2, y + 5, z - 6, cx + 2, y + 5, z - 6, DG)
    # mandibula inferior abierta (mas abajo y mas corta)
    v.fill(cx - 2, y, z - 5, cx + 2, y, z + 1, DG)
    # colmillos
    for dx in (-2, 0, 2):
        v.set(cx + dx, y + 1, z - 5, BL); v.set(cx + dx, y + 2, z - 6, BL) if False else None
        v.set(cx + dx, y + 2, z - 6, BL)
    for dx in (-2, 2): v.set(cx + dx, y + 1, z - 5, BL); v.set(cx + dx, y + 2, z - 5, BL)
    # fosas nasales y ojos
    v.set(cx - 1, y + 5, z - 6, NEG); v.set(cx + 1, y + 5, z - 6, NEG)
    for s in (-1, 1):
        v.set(cx + s * 3, y + 4, z - 1, ROJ); v.set(cx + s * 3, y + 4, z, ROJ); v.set(cx + s * 3, y + 5, z - 1, NEG)
        # cuernos hacia atras y arriba
        for i in range(5): v.set(cx + s * (2 + i // 3), y + 6 + i, z + 1 + i, ORO)
        # barbillas
        for i in range(3): v.set(cx + s * 3, y - i, z - 2 - i, DG)
    # cresta de la frente
    for i in range(6): v.set(cx, y + 6, z + i, ORO if i % 2 == 0 else DG)

def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    PD, NG, VD, ORO = 'polished_blackstone_bricks', 'black_concrete', 'green_terracotta', 'gold_block'
    v.fill(0, 0, 0, W - 1, 0, L - 1, 'polished_deepslate')
    # escalera ceremonial
    for z in range(1, Z0):
        y = 1 if z > 3 else 1
        for x in range(CX - 5, CX + 6): v.stairs(x, 1, z, 'polished_blackstone_brick', 'north') if z in (2, 4, 6) else v.set(x, 1, z, 'polished_blackstone')
    # templo: tres terrazas (zigurat)
    def terraza(x0, x1, z0, z1, y0, y1, mat, banda):
        v.box(x0, y0, z0, x1, y1, z1, mat)
        v.anillo(x0, z0, x1, z1, y1, banda)
        v.anillo(x0, z0, x1, z1, y0 + 1, banda)
    terraza(X0, X1, Z0, Z1, 1, 9, PD, 'green_concrete')
    v.fill(X0 + 1, 1, Z0 + 1, X1 - 1, 1, Z1 - 1, 'polished_deepslate')
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 9, Z1 - 1)
    terraza(X0 + 3, X1 - 3, Z0 + 3, Z1 - 3, 10, 16, PD, 'gold_block')
    v.aire(X0 + 4, 10, Z0 + 4, X1 - 4, 16, Z1 - 4)
    v.fill(X0 + 4, 9, Z0 + 4, X1 - 4, 9, Z1 - 4, 'polished_deepslate')
    v.fill(X0 + 6, 17, Z0 + 6, X1 - 6, 22, Z1 - 6, PD)
    v.anillo(X0 + 6, Z0 + 6, X1 - 6, Z1 - 6, 22, 'green_concrete')
    # tejados de las terrazas: borde de escalera verde con puntas doradas
    for (x0, x1, z0, z1, y) in [(X0, X1, Z0, Z1, 10), (X0 + 3, X1 - 3, Z0 + 3, Z1 - 3, 17), (X0 + 6, X1 - 6, Z0 + 6, Z1 - 6, 23)]:
        for x in range(x0, x1 + 1):
            v.stairs(x, y, z0, 'dark_prismarine' if False else 'mud_brick' if False else 'stone_brick', 'north') if False else None
        v.anillo(x0 - 1, z0 - 1, x1 + 1, z1 + 1, y - 1, 'green_terracotta')
        for (x, z) in [(x0 - 1, z0 - 1), (x1 + 1, z0 - 1), (x0 - 1, z1 + 1), (x1 + 1, z1 + 1)]:
            v.set(x, y - 1, z, 'gold_block'); v.set(x, y, z, 'lightning_rod[facing=up,powered=false,waterlogged=false]') if False else v.set(x, y, z, 'gold_block')
    # techo del cuerpo central (lo cierra)
    v.fill(X0 + 6, 23, Z0 + 6, X1 - 6, 23, Z1 - 6, 'green_terracotta')
    # puerta: boca del dragon (7 de ancho x 6 de alto) con dientes
    v.aire(CX - 3, 2, Z0, CX + 3, 7, Z0); v.aire(CX - 3, 2, Z0 + 1, CX + 3, 7, Z0 + 2)
    for x in range(CX - 5, CX + 6):
        v.fill(x, 8, Z0, x, 8, Z0, 'gold_block') if abs(x - CX) <= 4 else None
    for x in (CX - 4, CX + 4): v.fill(x, 2, Z0, x, 8, Z0, 'gold_block')
    for dx in range(-3, 4, 2):
        v.set(CX + dx, 7, Z0, 'quartz_stairs[facing=south,half=top,shape=straight]') if False else v.set(CX + dx, 7, Z0, 'quartz_block')
        v.set(CX + dx, 6, Z0, 'quartz_block') if abs(dx) == 3 else None
        v.set(CX + dx, 2, Z0, 'quartz_block') if abs(dx) in (1, 3) else None
    # cara de dragon en la fachada alrededor de la boca: ojos y cuernos (pixel art)
    ojos = ["#      #", " ##  ## ", "  #  #  "]
    for s in (-1, 1):
        for i in range(6): v.set(CX + s * (5 + i // 2), 9 + i, Z0 - 1, 'gold_block') if i < 5 else None
        v.fill(CX + s * 7, 5, Z0 - 1, CX + s * 8, 6, Z0 - 1, 'red_concrete')
        v.fill(CX + s * 7, 4, Z0 - 1, CX + s * 8, 4, Z0 - 1, 'black_concrete')
    # cabeza del dragon sobre la puerta y alas abiertas detras
    cabeza_dragon(v, CX, 24, Z0 + 1)
    zalas = Z0 + 14
    # ala de dragon: poligono (u = distancia al cuerpo, y) con borde de salida festoneado, huesos de oro y membrana verde
    ala = [(0, 0), (6, 9), (12, 12), (18, 8), (16, 4), (13, 1), (11, -3), (7, -2), (3, -3)]
    def dentro(px, py):
        c = False
        for (x1, y1), (x2, y2) in zip(ala, ala[1:] + ala[:1]):
            if (y1 > py) != (y2 > py) and px < (x2 - x1) * (py - y1) / (y2 - y1) + x1: c = not c
        return c
    for s in (-1, 1):
        for u in range(0, 20):
            for dy in range(-4, 14):
                if dentro(u + 0.5, dy + 0.5):
                    v.set(CX + s * (5 + u), 23 + dy, zalas, 'green_concrete' if (u + dy) % 4 else 'lime_concrete')
        for (a1, a2) in (((0, 0), (6, 9)), ((6, 9), (12, 12)), ((12, 12), (18, 8)), ((12, 12), (16, 4)), ((12, 12), (11, -3)), ((12, 12), (7, -2))):
            v.linea(CX + s * (5 + a1[0]), 23 + a1[1], zalas, CX + s * (5 + a2[0]), 23 + a2[1], zalas, 'gold_block')
            v.linea(CX + s * (5 + a1[0]), 23 + a1[1], zalas + 1, CX + s * (5 + a2[0]), 23 + a2[1], zalas + 1, 'gold_block')
        v.set(CX + s * 23, 31, zalas, 'gold_block'); v.set(CX + s * 21, 27, zalas, 'gold_block')
    # cola enroscada junto a la escalera (curva baja a la derecha del frente)
    tubo(v, [(CX + 9, 2, 3), (CX + 12, 2, 6), (CX + 13, 3, 11), (CX + 12, 5, 15)], 1.5, 'green_concrete', 'lime_concrete')
    # ---- interior: sala del templo, columnas y cuatro dragones de piedra
    cz = 25
    for x in range(X0 + 1, X1):
        for z in range(Z0 + 1, Z1):
            d = math.hypot(x - CX, z - cz)
            v.set(x, 1, z, 'gold_block' if d < 1.5 else 'green_concrete' if 4.5 < d < 6 else 'black_concrete' if 7 < d < 8.5 else 'polished_blackstone' if (x + z) % 2 else 'polished_deepslate')
    for (dx, dz) in [(-8, -7), (8, -7), (-8, 7), (8, 7), (0, -9)]:
        x, z = CX + dx, cz + dz
        v.fill(x, 2, z, x, 9, z, 'stripped_warped_stem[axis=y]' if False else 'green_terracotta'); v.set(x, 2, z, 'gold_block'); v.set(x, 9, z, 'gold_block')
        v.set(x + 1, 8, z, 'lantern[hanging=true]'); v.set(x - 1, 8, z, 'lantern[hanging=true]')
    for z in range(Z0 + 2, Z1 - 1, 5):
        for x in (X0 + 1, X1 - 1):
            v.set(x, 8, z, 'sea_lantern')
        lampara_colgante(v, CX, 17, z, 4, 'lantern[hanging=true]') if False else None
    # tarima del lider
    v.fill(CX - 6, 2, Z1 - 7, CX + 6, 2, Z1 - 1, 'polished_blackstone_bricks'); v.fill(CX - 4, 3, Z1 - 5, CX + 4, 3, Z1 - 1, 'polished_blackstone')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 8, 'polished_blackstone_brick', 'north')
    for x in range(CX - 4, CX + 5): v.stairs(x, 3, Z1 - 6, 'polished_blackstone_brick', 'north')
    v.fill(CX - 5, 4, Z1 - 1, CX + 5, 11, Z1 - 1, 'green_terracotta'); v.fill(CX - 1, 6, Z1 - 1, CX + 1, 9, Z1 - 1, 'gold_block')
    # techo interior: bóveda plana con lamparas
    v.fill(X0 + 1, 10, Z0 + 1, X1 - 1, 10, Z1 - 1, 'black_concrete')
    for x in range(X0 + 4, X1 - 3, 6):
        for z in range(Z0 + 4, Z1 - 3, 6): v.set(x, 10, z, 'sea_lantern')
    return v, [(CX, 4, Z1 - 4, 180, 'emipokemon desafios teselia leyenda')]
