#!/usr/bin/env python3
"""Rediseño de los tickets de Emipokemon: los tres que eran un cuadrado rosa de 16x16 (card_booster_ticket, sticker_gacha_ticket, type_booster_ticket) y los cuatro de modelo 3D fino (gacha_ticket, treasure_gacha_ticket, claw_ticket, emi_special_banner_ticket).
Salen en 32x32 con la misma silueta de boleto (muescas, marco dorado, perforado) y un icono temático. Uso: python3 gen_tickets.py <carpeta de salida>"""
import sys, os, math
from PIL import Image

OUT = sys.argv[1] if len(sys.argv) > 1 else '.'
os.makedirs(OUT, exist_ok=True)
ORO = (255, 205, 45, 255); ORO_D = (196, 134, 20, 255); ORO_L = (255, 245, 160, 255); CONT = (40, 24, 56, 255); BLANCO = (255, 255, 255, 255)
def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(4))
def rgba(r, g, b): return (r, g, b, 255)

def boleto(top, bottom):
    im = Image.new('RGBA', (32, 32), (0, 0, 0, 0)); P = im.load()
    x0, x1, y0, y1 = 1, 30, 5, 26
    def dentro(x, y):
        if not (x0 <= x <= x1 and y0 <= y <= y1): return False
        cy = (y0 + y1) / 2
        return all((x - cx) ** 2 + (y - cy) ** 2 > 3.2 ** 2 for cx in (x0, x1))
    veci = ((1, 0), (-1, 0), (0, 1), (0, -1))
    for y in range(32):
        for x in range(32):
            if dentro(x, y):
                P[x, y] = CONT if not all(dentro(x + dx, y + dy) for dx, dy in veci) else lerp(top, bottom, (y - y0) / (y1 - y0))
    for y in range(32):
        for x in range(32):
            if dentro(x, y) and P[x, y] != CONT and not all(dentro(x + dx, y + dy) and P[x + dx, y + dy] != CONT for dx, dy in veci):
                P[x, y] = ORO
    for y in range(32):
        for x in range(32):
            if P[x, y] == ORO and y == y0 + 1: P[x, y] = ORO_L
            if P[x, y] == ORO and y == y1 - 1: P[x, y] = ORO_D
    for y in range(y0 + 3, y1 - 2, 2): P[27, y] = BLANCO          # perforado (talon pequeño a la derecha)
    return im

def rect(P, x, y, w, h, c):
    for j in range(h):
        for i in range(w): P[x + i, y + j] = c
def px(P, pts, c):
    for x, y in pts: P[x, y] = c
def brillo(P, pts):
    for x, y in pts:
        P[x, y] = BLANCO
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if P[x + dx, y + dy][3] and P[x + dx, y + dy] not in (CONT, ORO, ORO_L, ORO_D): P[x + dx, y + dy] = (255, 240, 250, 255)

# ---------------------------------------------------------------- sobre de cartas: tres cartas en abanico, la de delante con una Poke Ball
def cartas():
    im = boleto(rgba(52, 78, 190), rgba(110, 50, 170)); P = im.load()
    for (cx, cy, dorso) in ((6, 12, rgba(20, 150, 150)), (10, 10, rgba(190, 70, 120))):
        rect(P, cx, cy, 8, 12, CONT); rect(P, cx + 1, cy + 1, 6, 10, dorso); rect(P, cx + 2, cy + 3, 4, 6, lerp(dorso, BLANCO, 0.35)); px(P, [(cx + 3, cy + 5), (cx + 4, cy + 5), (cx + 3, cy + 6), (cx + 4, cy + 6)], lerp(dorso, BLANCO, 0.7))
    cx, cy = 14, 8
    rect(P, cx, cy, 9, 15, CONT); rect(P, cx + 1, cy + 1, 7, 13, rgba(252, 244, 214))
    rect(P, cx + 1, cy + 1, 7, 2, rgba(240, 200, 60))                                   # cabecera dorada
    rect(P, cx + 2, cy + 4, 5, 5, rgba(120, 200, 250)); px(P, [(cx + 3, cy + 5), (cx + 4, cy + 5), (cx + 3, cy + 6)], rgba(210, 240, 255))
    px(P, [(cx + 4, cy + 7), (cx + 5, cy + 8), (cx + 3, cy + 8)], rgba(250, 110, 150))   # criatura rosa
    rect(P, cx + 3, cy + 10, 3, 3, CONT); rect(P, cx + 4, cy + 10, 1, 1, rgba(235, 50, 50)); rect(P, cx + 3, cy + 10, 3, 1, rgba(235, 50, 50)); px(P, [(cx + 3, cy + 11), (cx + 5, cy + 11), (cx + 4, cy + 11)], BLANCO); P[cx + 4, cy + 11] = CONT
    P[cx + 4, cy + 12] = BLANCO; P[cx + 3, cy + 12] = BLANCO; P[cx + 5, cy + 12] = BLANCO
    brillo(P, [(24, 9), (6, 8), (12, 24)])
    return im

# ---------------------------------------------------------------- gasha de cosmeticos: hoja de pegatinas con corazon, estrella y lazo
def pegatinas():
    im = boleto(rgba(255, 160, 210), rgba(176, 124, 232)); P = im.load()
    rect(P, 6, 8, 18, 15, CONT); rect(P, 7, 9, 16, 13, rgba(255, 252, 255))            # hoja blanca
    for i in range(4):                                                                  # esquina despegada
        px(P, [(22 - i, 21 - 0), (23, 21 - i)], rgba(200, 190, 230))
    px(P, [(21, 21), (22, 21), (22, 20), (21, 20), (23, 21), (23, 20), (22, 19), (23, 19)], rgba(205, 195, 235)); px(P, [(22, 22), (23, 22)], rgba(150, 130, 190))
    # corazon rojo
    px(P, [(9, 11), (10, 11), (12, 11), (13, 11), (9, 12), (10, 12), (11, 12), (12, 12), (13, 12), (10, 13), (11, 13), (12, 13), (11, 14)], rgba(235, 60, 90)); P[10, 11] = rgba(255, 150, 170); P[9, 12] = rgba(255, 130, 160)
    # estrella dorada
    px(P, [(18, 10), (17, 11), (18, 11), (19, 11), (15, 12), (16, 12), (17, 12), (18, 12), (19, 12), (20, 12), (21, 12), (16, 13), (17, 13), (18, 13), (19, 13), (20, 13), (17, 14), (19, 14), (16, 15), (20, 15)], ORO); px(P, [(18, 11), (18, 12)], ORO_L)
    # lazo azul
    px(P, [(9, 17), (10, 17), (9, 18), (10, 18), (11, 18), (11, 17), (12, 18), (13, 18), (14, 17), (14, 18), (13, 17), (9, 19), (10, 19), (13, 19), (14, 19)], rgba(70, 140, 235)); rect(P, 11, 18, 2, 1, rgba(40, 90, 190)); P[9, 17] = rgba(140, 190, 255)
    # gorro de fiesta verde
    px(P, [(18, 16), (17, 17), (18, 17), (19, 17), (16, 18), (17, 18), (18, 18), (19, 18), (20, 18), (15, 19), (16, 19), (17, 19), (18, 19), (19, 19), (20, 19), (21, 19)], rgba(80, 200, 120)); P[18, 15] = ORO; P[17, 18] = rgba(255, 240, 120); P[19, 19] = rgba(255, 240, 120)
    brillo(P, [(25, 10), (5, 22)])
    return im

# ---------------------------------------------------------------- booster de tipos: anillo de ocho tipos alrededor de una bola dorada
def tipos():
    im = boleto(rgba(28, 34, 84), rgba(70, 42, 120)); P = im.load()
    cols = [rgba(240, 84, 44), rgba(255, 150, 40), rgba(255, 224, 56), rgba(90, 200, 96), rgba(110, 230, 235), rgba(66, 138, 245), rgba(120, 78, 225), rgba(250, 110, 190)]
    cx, cy, r = 14, 15, 7.2
    for k, c in enumerate(cols):
        a = math.radians(-90 + 45 * k); x = round(cx + r * math.cos(a)); y = round(cy + r * math.sin(a))
        rect(P, x - 1, y - 1, 3, 3, c); px(P, [(x - 1, y - 1)], lerp(c, BLANCO, 0.5)); px(P, [(x + 1, y + 1)], lerp(c, CONT, 0.3))
    rect(P, cx - 2, cy - 2, 5, 5, ORO_D); rect(P, cx - 1, cy - 2, 3, 5, ORO); rect(P, cx - 2, cy - 1, 5, 3, ORO); P[cx - 1, cy - 1] = ORO_L; P[cx, cy - 1] = ORO_L; P[cx + 1, cy + 1] = ORO_D
    brillo(P, [(24, 9), (5, 8), (25, 22)])
    return im

# ---------------------------------------------------------------- ticket gacha (estandar): crema y oro, la carita del gato y una gema roja
def gacha():
    im = boleto(rgba(255, 245, 216), rgba(243, 205, 140)); P = im.load()
    BL = rgba(252, 250, 255); GR = rgba(205, 200, 222); MOR = rgba(92, 56, 150); ROSA = rgba(250, 150, 190)
    for x, y in ((7, 10), (8, 10), (7, 11), (8, 11), (9, 11), (15, 10), (16, 10), (16, 11), (15, 11), (14, 11)): P[x, y] = CONT          # orejas
    for x, y in ((8, 11), (15, 11)): P[x, y] = ROSA
    rect(P, 6, 12, 12, 9, CONT); rect(P, 7, 12, 10, 8, BL); rect(P, 7, 19, 10, 1, GR)
    px(P, [(9, 15), (10, 15), (9, 16), (10, 16), (13, 15), (14, 15), (13, 16), (14, 16)], MOR); px(P, [(9, 15), (13, 15)], BLANCO)
    px(P, [(11, 17), (12, 17)], ROSA); px(P, [(11, 18), (12, 18)], CONT); px(P, [(8, 17), (15, 17)], rgba(255, 190, 205))
    gx, gy = 22, 15                                                                           # gema roja con engaste dorado
    for dy in range(-4, 5):
        for dx in range(-4, 5):
            d = abs(dx) + abs(dy)
            if d <= 4: P[gx + dx, gy + dy] = ORO_D if d == 4 else (rgba(222, 36, 70) if d >= 2 else rgba(255, 90, 110))
    px(P, [(gx, gy - 3), (gx - 1, gy - 2), (gx - 1, gy - 1), (gx, gy - 2)], rgba(255, 190, 200)); px(P, [(gx - 2, gy), (gx - 1, gy)], rgba(255, 150, 165))
    px(P, [(gx, gy - 4), (gx, gy + 4), (gx - 4, gy), (gx + 4, gy)], ORO_L)
    brillo(P, [(18, 9), (4, 23)])
    return im

# ---------------------------------------------------------------- gasha de tesoros de Emi: cofre abierto con monedas y gemas
def tesoros():
    im = boleto(rgba(120, 34, 100), rgba(58, 20, 76)); P = im.load()
    MAR = rgba(150, 88, 44); MAR_L = rgba(190, 120, 62); MAR_D = rgba(96, 54, 30)
    rect(P, 9, 9, 14, 5, CONT); rect(P, 10, 10, 12, 4, MAR_D); rect(P, 11, 10, 10, 1, rgba(120, 70, 36))   # tapa abierta (por detras)
    for x in range(11, 21): P[x, 13] = ORO if x % 2 else ORO_L                                               # monedas que asoman
    px(P, [(12, 12), (15, 12), (18, 12), (13, 11), (17, 11)], ORO_L)
    rect(P, 8, 14, 16, 9, CONT); rect(P, 9, 15, 14, 7, MAR); rect(P, 9, 15, 14, 1, MAR_L)               # cuerpo
    rect(P, 12, 15, 2, 7, ORO); rect(P, 18, 15, 2, 7, ORO); rect(P, 9, 18, 14, 1, ORO_D)                   # herrajes
    rect(P, 15, 16, 2, 3, ORO_L); P[15, 17] = CONT; P[16, 17] = CONT                                        # cerradura
    px(P, [(8, 10), (23, 10)], rgba(255, 120, 200)); px(P, [(7, 9), (24, 9)], BLANCO)
    px(P, [(10, 8), (21, 8)], rgba(110, 230, 235)); px(P, [(25, 13), (6, 13)], rgba(255, 150, 210))
    brillo(P, [(15, 7), (5, 20), (26, 21)])
    px(P, [(15, 6), (14, 7), (16, 7), (15, 8)], rgba(255, 230, 240))
    return im

# ---------------------------------------------------------------- ticket de la garra: garra de tres dedos agarrando una bola dorada
def garra():
    im = boleto(rgba(176, 30, 108), rgba(96, 22, 96)); P = im.load()
    G = rgba(206, 212, 224); GD = rgba(130, 138, 160); GL = rgba(240, 244, 252)
    rect(P, 15, 7, 2, 6, G); P[15, 7] = GL; px(P, [(16, 8), (16, 9), (16, 10), (16, 11), (16, 12)], GD)
    rect(P, 12, 12, 8, 2, GD); rect(P, 13, 12, 6, 1, G); px(P, [(13, 12), (14, 12)], GL)
    for x, y in ((12, 14), (11, 15), (11, 16), (12, 17), (13, 17)): P[x, y] = G                                  # dedo izquierdo
    for x, y in ((19, 14), (20, 15), (20, 16), (19, 17), (18, 17)): P[x, y] = G                                  # dedo derecho
    for x, y in ((15, 14), (16, 14), (15, 15), (16, 15), (15, 16), (16, 16)): P[x, y] = G                        # dedo central
    px(P, [(11, 15), (11, 16), (15, 14)], GL); px(P, [(20, 15), (20, 16), (16, 16)], GD)
    cx, cy = 15.5, 20
    for y in range(16, 24):
        for x in range(10, 22):
            d = (x - cx) ** 2 + ((y - cy) * 1.1) ** 2
            if d <= 12.5: P[x, y] = ORO if d > 4.5 else ORO_L
            if 10.5 < d <= 12.5: P[x, y] = ORO_D
    px(P, [(14, 19), (15, 19)], BLANCO)
    px(P, [(7, 10), (8, 11), (24, 9), (23, 10), (6, 21), (25, 20)], rgba(60, 220, 210)); brillo(P, [(24, 22), (6, 8)])
    return im

# ---------------------------------------------------------------- banner especial de Emi: corazon grande con estrella dorada y banda plateada
def especial():
    im = boleto(rgba(232, 204, 255), rgba(188, 140, 232)); P = im.load()
    CO = rgba(240, 70, 130); COL = rgba(255, 150, 190); COD = rgba(190, 40, 100)
    filas = ['..XXX...XXX..', '.XXXXX.XXXXX.', 'XXXXXXXXXXXXX', 'XXXXXXXXXXXXX', 'XXXXXXXXXXXXX', '.XXXXXXXXXXX.', '..XXXXXXXXX..', '...XXXXXXX...', '....XXXXX....', '.....XXX.....', '......X......']
    ox, oy = 9, 8
    for j, f in enumerate(filas):
        for i, ch in enumerate(f):
            if ch == 'X': P[ox + i, oy + j] = COD if (j >= 6 and i > 6) or j == 10 else CO
    px(P, [(ox + 2, oy), (ox + 3, oy), (ox + 1, oy + 1), (ox + 2, oy + 1), (ox + 1, oy + 2)], COL); px(P, [(ox + 2, oy + 2)], BLANCO)
    for x in range(8, 25): P[x, 20] = rgba(215, 218, 232); P[x, 21] = rgba(150, 156, 184)                      # banda plateada
    px(P, [(11, 20), (12, 20), (20, 20), (21, 20)], rgba(250, 150, 190))
    px(P, [(24, 9), (23, 10), (24, 10), (25, 10), (24, 11)], ORO); P[24, 10] = ORO_L
    px(P, [(7, 12), (6, 13), (7, 14), (8, 13)], ORO)
    brillo(P, [(6, 9), (25, 17), (5, 21)])
    return im

for nombre, f in (('card_booster_ticket', cartas), ('sticker_gacha_ticket', pegatinas), ('type_booster_ticket', tipos),
                  ('gacha_ticket', gacha), ('treasure_gacha_ticket', tesoros), ('claw_ticket', garra), ('emi_special_banner_ticket', especial)):
    f().save(f'{OUT}/{nombre}.png')
print('7 tickets 32x32 en', OUT)
