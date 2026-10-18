#!/usr/bin/env python3
"""Genera la skin de Minecraft (64x64, brazos clásicos) de «Tayan»: pelo negro con puntas turquesa,
orejas de gato, chaqueta vaquera sobre sudadera blanca, falda negra y botas altas.
Uso: python3 tayan.py <salida.png> [vista_previa.png]"""
import sys
from PIL import Image

PIEL, PIEL_S, PIEL_O = (255, 226, 207), (243, 205, 184), (232, 190, 168)
NEG, NEG_C = (27, 24, 32), (44, 40, 52)
TUR = [(18, 120, 120), (0, 170, 160), (20, 215, 195), (90, 240, 220)]  # de oscuro a claro (puntas)
VAQ, VAQ_C, VAQ_O, VAQ_P = (96, 142, 196), (132, 174, 222), (66, 102, 150), (156, 196, 236)
BLA, BLA_S, BLA_G = (244, 244, 249), (214, 214, 226), (150, 150, 165)
FALDA, FALDA_C = (30, 30, 36), (45, 45, 54)
BOTA, BOTA_C, BOTA_S = (24, 24, 28), (62, 62, 70), (12, 12, 15)
PLATA = (200, 200, 212)
ROSA, ROSA_O = (232, 150, 160), (255, 190, 196)
OJO_O, OJO, OJO_C = (24, 70, 44), (78, 214, 104), (190, 255, 205)

img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()


def caras(x0, y0, w, h, d):
    """Rectángulos (x, y, ancho, alto) de las 6 caras de una caja de la skin."""
    return {"arriba": (x0 + d, y0, w, d), "abajo": (x0 + d + w, y0, w, d), "der": (x0, y0 + d, d, h),
            "frente": (x0 + d, y0 + d, w, h), "izq": (x0 + d + w, y0 + d, d, h), "atras": (x0 + 2 * d + w, y0 + d, w, h)}


def pintar(rect, f):
    x, y, w, h = rect
    for v in range(h):
        for u in range(w):
            c = f(u, v)
            if c is not None:
                px[x + u, y + v] = (*c[:3], 255)


def lado(nombre, f):
    """Las caras laterales se pintan con k = distancia al borde delantero (0 = junto a la cara)."""
    if nombre == "der":
        return lambda u, v, W=None: f(7 - u if W is None else W - 1 - u, v)
    return f


# ---------------- cabeza (8x8x8) ----------------
def pelo_cola(v):  # degradado de las puntas: v=4..7
    return [NEG, TUR[0], TUR[1], TUR[2]][v - 4] if v >= 4 else NEG


def cabeza_base(c):
    pintar(c["arriba"], lambda u, v: NEG_C if (u + v) % 5 == 0 else NEG)
    pintar(c["abajo"], lambda u, v: PIEL_S)

    def frente(u, v):
        if v <= 2: return NEG_C if (v == 0 and u % 3 == 0) else NEG
        if u in (0, 7): return TUR[min(2, v - 3)] if v >= 3 else NEG
        if v == 3: return NEG if u in (1, 3, 4, 6) else PIEL
        if v == 4:
            if u in (1, 2, 5, 6): return OJO_O
            return PIEL
        if v == 5:
            if u in (1, 5): return OJO
            if u in (2, 6): return OJO_C
            return PIEL
        if v == 6:
            if u in (1, 6): return ROSA_O
            if u in (3, 4): return (236, 176, 170)
            return PIEL
        return PIEL_S
    pintar(c["frente"], frente)

    def costado(u, v, k=None):
        k = u if k is None else k
        if v <= 3: return NEG
        if k <= 1: return [PIEL, PIEL, PIEL_S, PIEL_S][v - 4] if k == 0 else (TUR[v - 4 - 1] if v > 4 else NEG)
        if k >= 2: return pelo_cola(v) if k >= 3 else NEG
        return PIEL
    # k = distancia al borde delantero; der: u=7 es el delantero
    pintar(c["izq"], lambda u, v: costado(u, v, u))
    pintar(c["der"], lambda u, v: costado(u, v, 7 - u))
    pintar(c["atras"], lambda u, v: pelo_cola(v) if v >= 4 else (NEG_C if (u + v) % 4 == 0 else NEG))


def cabeza_capa(c):
    # orejas de gato (arriba): negras con interior rosa y pelusa blanca en la base
    def arriba(u, v):
        if v in (2, 3) and u in (0, 1, 6, 7):
            if v == 3 and u in (1, 6): return ROSA
            if v == 2 and u in (0, 7): return NEG
            if v == 2 and u in (1, 6): return NEG_C
            return (252, 244, 244) if v == 3 else None
        if v == 5 and u == 4: return NEG_C  # remolino del pelo
        return NEG if (v <= 1 or v in (4, 5, 6, 7)) else None
    pintar(c["arriba"], arriba)

    def frente(u, v):
        if v <= 2:
            if v == 2 and u in (5, 6): return TUR[3]  # pasador turquesa
            return NEG if not (v == 0 and u in (0, 7)) else None
        if u in (0, 7) and v <= 6: return TUR[min(2, v - 3)] if v >= 3 else NEG
        if v == 3: return NEG if u in (1, 3, 4, 6) else None
        return None
    pintar(c["frente"], frente)

    def costado(u, v, k):
        if v <= 3 and not (v == 0 and k <= 1): return NEG
        if v == 0: return None
        if k >= 3 and v >= 4: return pelo_cola(v)
        # oreja que sobresale un poco por arriba del lado
        return None
    pintar(c["izq"], lambda u, v: costado(u, v, u) if v > 0 else (ROSA if u in (3, 4) else (NEG if u in (2, 5) else None)))
    pintar(c["der"], lambda u, v: costado(u, v, 7 - u) if v > 0 else (ROSA if u in (3, 4) else (NEG if u in (2, 5) else None)))
    pintar(c["atras"], lambda u, v: NEG if v <= 3 else pelo_cola(v))
    # trenza turquesa del lado izquierdo del personaje (la cara «izq»)
    x, y, w, h = c["izq"]
    for v in range(4, 8):
        px[x + 1 + (v % 2), y + v] = (*TUR[1 + (v % 2)], 255)


cab = caras(0, 0, 8, 8, 8); cabeza_base(cab)
cabeza_capa(caras(32, 0, 8, 8, 8))


# ---------------- tronco (8x12x4) ----------------
def tronco_frente(u, v):
    if v == 0: return NEG if u in (2, 3, 4, 5) else VAQ_O          # gargantilla
    if v == 1 and u in (3, 4): return PLATA if u == 3 else NEG        # colgante
    if v <= 7:
        if u in (0, 1, 6, 7): return VAQ_O if v in (6, 7) else VAQ
        if v == 4 and u in (2, 3, 4, 5): return (70, 70, 84) if u != 3 else BLA   # «maron»
        if u == 3 and 1 <= v <= 6 and v != 4: return BLA_G if v % 2 else BLA_S     # cordones
        if u == 4 and 2 <= v <= 6 and v != 4: return PLATA if v == 3 else BLA_S    # cadena
        return BLA_S if u in (2, 5) else BLA
    if v == 8: return PLATA if u == 4 else NEG                         # cinturón y hebilla
    return FALDA


def tronco_costado(u, v):
    if v <= 7: return VAQ_O if v >= 6 else VAQ
    return NEG if v == 8 else FALDA


def tronco_atras(u, v):
    if v <= 7:
        if v == 0: return VAQ_C
        if u in (3, 4): return VAQ_O                     # costura central
        if v in (5, 6) and u in (0, 7): return VAQ_C
        return VAQ_O if v == 7 else VAQ
    return NEG if v == 8 else FALDA


tr = caras(16, 16, 8, 12, 4)
pintar(tr["arriba"], lambda u, v: VAQ_C); pintar(tr["abajo"], lambda u, v: FALDA)
pintar(tr["frente"], tronco_frente); pintar(tr["der"], tronco_costado); pintar(tr["izq"], tronco_costado); pintar(tr["atras"], tronco_atras)

to = caras(16, 32, 8, 12, 4)  # capa: chaqueta abierta + falda con vuelo
def capa_frente(u, v):
    if v <= 7:
        if u in (0, 1, 6, 7):
            if v == 0: return VAQ_C
            if v == 5 and u in (1, 6): return PLATA if False else VAQ_P  # costura del bolsillo
            if v == 6 and u in (1, 6): return VAQ_P
            return VAQ_C if u in (1, 6) and v < 5 else VAQ
        return None
    if v == 8: return None
    return FALDA_C if u % 2 else FALDA
def capa_costado(u, v):
    if v <= 7: return VAQ_C if v == 0 else (VAQ_P if v == 5 else VAQ)
    return None if v == 8 else (FALDA_C if u % 2 else FALDA)
def capa_atras(u, v):
    if v <= 7: return VAQ_C if v == 0 else (VAQ_O if u in (3, 4) else (VAQ_P if v == 6 and u in (0, 1, 6, 7) else VAQ))
    return None if v == 8 else (FALDA_C if u % 2 else FALDA)
pintar(to["arriba"], lambda u, v: VAQ_C); pintar(to["frente"], capa_frente)
pintar(to["der"], capa_costado); pintar(to["izq"], capa_costado); pintar(to["atras"], capa_atras)


# ---------------- piernas (4x12x4) ----------------
def pierna(c, ov, izquierda):
    def f(u, v, cara):
        sombra = cara in ("der", "izq")
        piel = PIEL_S if sombra else PIEL
        if ov:
            if v <= 1: return FALDA_C if (u + v) % 2 else FALDA
            if v == 7: return BOTA_C
            if v >= 8: return BOTA_S if v == 11 else BOTA
            if izquierda and v in (3, 4) and cara != "atras" and not (cara == "izq"):
                return NEG if not (v == 3 and u == 1 and cara == "frente") else PLATA   # liga con anilla
            return None
        if v <= 1: return FALDA
        if v <= 6:
            if izquierda and v in (3, 4): return NEG if not (v == 3 and u == 1 and cara == "frente") else PLATA
            return piel if v < 6 else PIEL_O
        if v == 7: return BOTA_C
        return BOTA_S if v == 11 else BOTA
    for cara in ("der", "frente", "izq", "atras"):
        pintar(c[cara], lambda u, v, cara=cara: f(u, v, cara))
    pintar(c["arriba"], lambda u, v: FALDA if not ov else None)
    pintar(c["abajo"], lambda u, v: BOTA_S if not ov else BOTA_S)


pierna(caras(0, 16, 4, 12, 4), False, False); pierna(caras(0, 32, 4, 12, 4), True, False)
pierna(caras(16, 48, 4, 12, 4), False, True); pierna(caras(0, 48, 4, 12, 4), True, True)

# cola turquesa que asoma por detrás, justo debajo de la falda (una columna en cada pierna, junto a la unión)
for (x, y) in ((12, 36), (15, 48)):
    for v, col in zip(range(2, 10), [NEG, NEG, NEG, NEG, TUR[0], TUR[1], TUR[2], TUR[3]]):
        px[x, y + v] = (*col, 255)


# ---------------- brazos (4x12x4): chaqueta vaquera y manos ----------------
def brazo(c, ov):
    def f(u, v, cara):
        if v <= 8:
            if ov:
                if v == 8: return VAQ_O
                if v == 0: return VAQ_C
                return VAQ_P if (cara == "frente" and u == 1 and v in (2, 3, 4)) else VAQ
            return VAQ_O if v == 8 else VAQ
        # manos
        sombra = cara in ("der", "izq")
        if ov: return None
        return PIEL_O if v == 11 and not sombra else (PIEL_S if sombra else PIEL)
    for cara in ("der", "frente", "izq", "atras"):
        pintar(c[cara], lambda u, v, cara=cara: f(u, v, cara))
    pintar(c["arriba"], lambda u, v: VAQ_C)
    pintar(c["abajo"], lambda u, v: None if ov else PIEL_S)


brazo(caras(40, 16, 4, 12, 4), False); brazo(caras(40, 32, 4, 12, 4), True)
brazo(caras(32, 48, 4, 12, 4), False); brazo(caras(48, 48, 4, 12, 4), True)

img.save(sys.argv[1])

# ---------------- vista previa plana (delante y detrás, con capas) ----------------
if len(sys.argv) > 2:
    S = 10
    def recorte(x, y, w, h): return img.crop((x, y, x + w, y + h))
    def figura(cara):
        f = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
        def poner(x, y, w, h, dx, dy):
            f.alpha_composite(recorte(x, y, w, h), (dx, dy))
        if cara == "frente":
            for capa in (0, 1):
                o = 32 if capa == 0 else 0
                poner(8 + 32 * capa, 8, 8, 8, 4, 0)
                poner(20, 20 + 16 * capa, 8, 12, 4, 8)
                poner(44, 20 + 16 * capa, 4, 12, 0, 8)
                poner(36 if capa == 0 else 52, 52, 4, 12, 12, 8)
                poner(4, 20 + 16 * capa, 4, 12, 4, 20)
                poner(20 if capa == 0 else 4, 52, 4, 12, 8, 20)
        else:
            for capa in (0, 1):
                poner(24 + 32 * capa, 8, 8, 8, 4, 0)
                poner(32, 20 + 16 * capa, 8, 12, 4, 8)
                poner(52, 20 + 16 * capa, 4, 12, 12, 8)
                poner(44 if capa == 0 else 60, 52, 4, 12, 0, 8)
                poner(12, 20 + 16 * capa, 4, 12, 8, 20)
                poner(28 if capa == 0 else 12, 52, 4, 12, 4, 20)
        return f
    pv = Image.new("RGBA", (40 * S, 36 * S), (58, 64, 78, 255))
    for i, cara in enumerate(("frente", "atras")):
        pv.alpha_composite(figura(cara).resize((16 * S, 32 * S), Image.NEAREST), ((3 + i * 19) * S // 1, 2 * S))
    pv.save(sys.argv[2])
