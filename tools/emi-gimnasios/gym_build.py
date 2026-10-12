"""Plantilla de gimnasio 27x20x26 (misma escala que los de Cobbleverse) con tema por tipo."""
from gymlib import V

W, H, L = 27, 21, 26
X0, X1 = 1, 25          # cuerpo del edificio
Z0, Z1 = 4, 25          # fachada (z=4) y fondo (z=25); delante, la explanada z=0..3
SUELO = 1               # capa del suelo del edificio (se camina en y=2)
CX = 13

def plantilla(T):
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    wall, trim, base, acc, acc2, pil = T['wall'], T['trim'], T['base'], T['acc'], T['acc2'], T['pillar']
    # --- explanada y cimientos
    v.fill(0, 0, 0, W - 1, 0, L - 1, base)
    v.fill(X0, SUELO, Z0, X1, SUELO, Z1, T['floor_edge'])
    # --- muros (y=2..11), con zocalo
    for (x0, z0, x1, z1) in [(X0, Z0, X1, Z1)]:
        v.box(x0, SUELO + 1, z0, x1, 11, z1, wall)
    v.fill(X0, 2, Z0, X1, 3, Z0, trim); v.fill(X0, 2, Z1, X1, 3, Z1, trim)
    v.fill(X0, 2, Z0, X0, 3, Z1, trim); v.fill(X1, 2, Z0, X1, 3, Z1, trim)
    # banda de color del tipo
    for y in (7,):
        v.anillo(X0, Z0, X1, Z1, y, acc)
    for y in (6, 8): v.anillo(X0, Z0, X1, Z1, y, acc2)
    # cornisa
    v.anillo(X0, Z0, X1, Z1, 11, trim)
    # pilares de esquina y a lo largo de los laterales
    for (x, z) in [(X0, Z0), (X1, Z0), (X0, Z1), (X1, Z1)]:
        v.fill(x, 2, z, x, 12, z, pil)
    for z in range(Z0 + 5, Z1 - 2, 5):
        v.fill(X0, 2, z, X0, 11, z, pil); v.fill(X1, 2, z, X1, 11, z, pil)
    # ventanas laterales y trasera (entre pilares)
    for z in range(Z0 + 2, Z1 - 1, 5):
        for dz in (0, 1, 2):
            if Z0 + 4 < z + dz < Z1 - 1 or True:
                pass
    for zc in (Z0 + 3, Z0 + 8, Z0 + 13, Z0 + 18):
        for dz in (-1, 0, 1):
            zz = zc + dz
            if Z0 + 1 <= zz <= Z1 - 1 and (zz - Z0) % 5 != 0:
                for y in (9, 10):
                    v.set(X0, y, zz, T['glass']); v.set(X1, y, zz, T['glass'])
    for xc in (CX - 8, CX - 4, CX, CX + 4, CX + 8):
        for dx in (-1, 0, 1):
            for y in (9, 10): v.set(xc + dx, y, Z1, T['glass'])
    # --- techo escalonado: capas con borde en escalera
    roof, rst = T['roof'], T['roof_stairs']
    k = 0
    while True:
        i = 2 * k
        xa, xb, za, zb = X0 + i, X1 - i, Z0 + i, Z1 - i
        if xb - xa < 2 or zb - za < 2: break
        y = 12 + k
        v.fill(xa, y, za, xb, y, zb, roof)
        # borde en escalera (bajo hacia fuera) salvo en las esquinas
        for x in range(xa + 1, xb):
            v.stairs(x, y, za, rst, 'north'); v.stairs(x, y, zb, rst, 'south')
        for z in range(za + 1, zb):
            v.stairs(xa, y, z, rst, 'west'); v.stairs(xb, y, z, rst, 'east')
        k += 1
    v.techo_k = k
    # cielo interior: techo plano a y=12 (bajo la primera capa)
    # --- interior
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 11, Z1 - 1)
    v.fill(X0, 12, Z0, X1, 12, Z1, roof)
    v.fill(X0 + 1, 11, Z0 + 1, X1 - 1, 11, Z1 - 1, T['ceiling'])
    # luces de techo en rejilla
    for x in range(X0 + 3, X1 - 2, 4):
        for z in range(Z0 + 4, Z1 - 2, 4):
            v.set(x, 11, z, T['lamp'])
    # --- fachada: porche con 4 columnas, puerta en arco, panel con el emblema
    puerta(v, T)
    # --- decoracion interior comun
    interior(v, T)
    if T.get('props'): T['props'](v, T)
    if T.get('crown'): T['crown'](v, T)
    return v

def puerta(v, T):
    wall, pil, trim, tm = T['wall'], T['pillar'], T['trim'], T['trim_mat']
    # puerta 5x5 en arco
    v.aire(CX - 2, 2, Z0, CX + 2, 6, Z0)
    for x in (CX - 3, CX + 3): v.fill(x, 2, Z0, x, 11, Z0, pil)
    v.fill(CX - 2, 7, Z0, CX + 2, 7, Z0, trim)
    v.stairs(CX - 2, 6, Z0, tm, 'east', 'top'); v.stairs(CX + 2, 6, Z0, tm, 'west', 'top')
    # frontón con el emblema (sobresale por encima del tejado)
    v.fill(CX - 7, 8, Z0, CX + 7, 11, Z0, wall)
    v.fill(CX - 6, 12, Z0, CX + 6, 20, Z0 + 1, wall)
    v.fill(CX - 5, 13, Z0, CX + 5, 19, Z0, trim)
    v.fill(CX - 4, 14, Z0 - 1, CX + 4, 18, Z0 - 1, 'black_concrete')
    v.fill(CX - 4, 14, Z0, CX + 4, 18, Z0, 'black_concrete')
    v.pixelart_z(CX - 4, 14, Z0 - 1, T['emblem'], T['emblem_pal'])
    for i in range(3):          # escalones laterales del frontón
        for sx in (-1, 1):
            v.stairs(CX + sx * (7 + i), 12, Z0, T['roof_stairs'], 'east' if sx == -1 else 'west') if False else None
    v.fill(CX - 7, 20, Z0, CX + 7, 20, Z0 + 1, T['roof'])
    for x in range(CX - 7, CX + 8):
        v.stairs(x, 21, Z0, T['roof_stairs'], 'north') if v.dentro(x, 21, Z0) else None
    # columnas decorativas a ambos lados de la puerta
    for x in (CX - 6, CX + 6):
        v.fill(x, 2, Z0 - 1, x, 10, Z0 - 1, pil)
        v.set(x, 11, Z0 - 1, trim)
    # banderas del tipo junto al porche
    for x in (CX - 9, CX + 9):
        v.fill(x - 1, 4, Z0 - 1, x + 1, 9, Z0 - 1, T['acc'])
        v.fill(x - 1, 3, Z0 - 1, x + 1, 3, Z0 - 1, T['acc2'])
        v.set(x, 6, Z0 - 1, T['lamp'])
    # escalones y alfombra de entrada
    for z in (3, 2, 1):
        for x in range(CX - 4, CX + 5): v.stairs(x, 1, z, tm, 'north')
    v.aire(CX - 2, 2, 1, CX + 2, 6, 3)
    for z in range(1, Z0): v.set(CX, 1, z, T['carpet']) if False else None

def interior(v, T):
    trim, tm = T['trim'], T['trim_mat']
    # alfombra desde la puerta hasta la arena
    for z in range(Z0 + 1, Z0 + 8):
        for x in range(CX - 1, CX + 2): v.set(x, SUELO, z, T['carpet'])
    # arena: suelo con dibujo
    xa, xb, za, zb = CX - 7, CX + 7, Z0 + 8, Z1 - 5
    for x in range(xa, xb + 1):
        for z in range(za, zb + 1):
            v.set(x, SUELO, z, T['floor_a'] if (x + z) % 2 == 0 else T['floor_b'])
    v.anillo(xa - 1, za - 1, xb + 1, zb + 1, SUELO, T['floor_line'])
    v.anillo(xa, za, xb, zb, SUELO, T['floor_line'])
    zc = (za + zb) // 2
    for x in range(xa + 3, xb - 2): v.set(x, SUELO, zc, T['floor_line'])
    for z in range(za + 3, zb - 2): v.set(CX, SUELO, z, T['floor_line'])
    v.fill(CX - 1, SUELO, zc - 1, CX + 1, SUELO, zc + 1, T['floor_line'])
    # gradas de espectadores a los lados (3 escalones hacia la pared)
    for xs, d, cara in ((X0 + 4, -1, 'east'), (X1 - 4, 1, 'west')):
        for t in range(3):
            x = xs + d * t
            for z in range(Z0 + 8, Z1 - 3):
                v.fill(x, SUELO + 1, z, x, SUELO + 1 + t, z, trim)
                v.stairs(x, SUELO + 1 + t, z, tm, cara)
    # podio del lider al fondo (dos escalones)
    v.fill(CX - 6, SUELO + 1, Z1 - 3, CX + 6, SUELO + 1, Z1 - 1, trim)
    v.fill(CX - 4, SUELO + 2, Z1 - 2, CX + 4, SUELO + 2, Z1 - 1, trim)
    for x in range(CX - 6, CX + 7): v.stairs(x, SUELO + 1, Z1 - 4, tm, 'north')
    for x in range(CX - 4, CX + 5): v.stairs(x, SUELO + 2, Z1 - 3, tm, 'north')
    # estandartes del tipo en la pared del fondo
    for dx in (-6, 0, 6):
        v.fill(CX + dx - 1, 5, Z1 - 1, CX + dx + 1, 10, Z1 - 1, T['acc'])
        v.fill(CX + dx - 1, 4, Z1 - 1, CX + dx + 1, 4, Z1 - 1, T['acc2'])
        v.set(CX + dx, 4, Z1 - 1, T['lamp'])
