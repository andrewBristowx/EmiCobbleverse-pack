#!/usr/bin/env python3
"""Herramientas para dibujar los iconos de Emi Survivors (pixel-art de 24x24 hecho por codigo).

Cada icono se dibuja 12 veces mas grande con formas simples (circulos, poligonos, trazos), se le da un volumen sencillo (borde claro arriba-izquierda, borde oscuro
abajo-derecha), se reduce a 24x24, se queda con los colores de su paleta (para que se vea como pixel-art y no como una foto borrosa) y se le pone un contorno
oscuro de 1 pixel. Los dibujos de cada icono estan en iconos_ataques.py, iconos_objetos.py; el programa que los junta en un atlas es generar_iconos.py.
"""
import math
from functools import reduce

import numpy as np
from PIL import Image, ImageChops, ImageDraw

N = 24                       # tamano final de cada icono (pixeles)
K = 12                       # supertamano
S = N * K


def hexc(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


CONTORNO = hexc('#14101e')


class Rampa:
    """Cuatro tonos de un color: oscuro, medio, claro y brillo."""
    def __init__(self, oscuro, medio, claro, brillo):
        self.dark, self.mid, self.light, self.hi = [hexc(c) for c in (oscuro, medio, claro, brillo)]


RAMPAS = {
    'normal':    Rampa('#7a7a52', '#bdbd8a', '#e0e0b8', '#fbfbe8'),
    'energia':   Rampa('#3a6cc0', '#8ad0ff', '#d4f2ff', '#ffffff'),
    'fire':      Rampa('#a1260f', '#f25a1a', '#ff9a3c', '#ffe08a'),
    'water':     Rampa('#1c4fa8', '#3b86f0', '#7fbaff', '#d9efff'),
    'grass':     Rampa('#2a7a2c', '#4fbf3f', '#96e86a', '#e2ffbf'),
    'electric':  Rampa('#b8870a', '#f5c518', '#ffe45c', '#fff8c2'),
    'ice':       Rampa('#3d8fa8', '#7fd4e8', '#b9f0f8', '#f0ffff'),
    'fighting':  Rampa('#7a1b14', '#c8352a', '#ee6a55', '#ffc2b0'),
    'poison':    Rampa('#5a1f80', '#9a45c8', '#c986ee', '#ecc9ff'),
    'ground':    Rampa('#7a5a22', '#c79a4a', '#e6c47a', '#f8e9b8'),
    'flying':    Rampa('#5b64b8', '#8f9cf0', '#bcc6ff', '#eef0ff'),
    'psychic':   Rampa('#b02a62', '#f2569a', '#ff8fc2', '#ffd6ea'),
    'bug':       Rampa('#5f7a10', '#9bc22a', '#c7e65a', '#efffb0'),
    'rock':      Rampa('#6b5a20', '#b59b40', '#d9c36e', '#f5ebb5'),
    'ghost':     Rampa('#3c2d68', '#6d55a8', '#9a82d6', '#d8cdf5'),
    'dragon':    Rampa('#3d1a9e', '#6f3df5', '#a07cff', '#dccfff'),
    'dark':      Rampa('#2c2018', '#5b4636', '#8a6f58', '#c9b199'),
    'steel':     Rampa('#5e6280', '#a3a8c4', '#cfd3e6', '#f4f5fb'),
    'fairy':     Rampa('#b8527a', '#f0a0c0', '#ffc6dc', '#fff0f6'),
    # colores de apoyo
    'oro':       Rampa('#a0740a', '#f2b81c', '#ffd95a', '#fff3b0'),
    'rojo':      Rampa('#8c1414', '#e03232', '#ff7468', '#ffc9c0'),
    'hueso':     Rampa('#8a8478', '#d8d2c0', '#f0ece0', '#ffffff'),
    'negro':     Rampa('#07040f', '#1c1030', '#3d2470', '#7a5cd0'),
    'marron':    Rampa('#4a2a10', '#8a5a2a', '#b98448', '#e2b985'),
    'cielo':     Rampa('#3c78b8', '#7ab8f0', '#b4dcff', '#f0faff'),
    'rosa':      Rampa('#b02a62', '#f2569a', '#ff8fc2', '#ffd6ea'),
    'menta':     Rampa('#2a8a6a', '#4fd6a0', '#9af0c8', '#e0fff2'),
}


# ----------------------------------------------------------------------------------------------------------------------------------------- mascaras

def _m():
    return Image.new('L', (S, S), 0)


def _p(v):
    return int(round(v * K))


def circ(cx, cy, r):
    m = _m()
    ImageDraw.Draw(m).ellipse([_p(cx - r), _p(cy - r), _p(cx + r), _p(cy + r)], fill=255)
    return m


def poli(pts):
    m = _m()
    ImageDraw.Draw(m).polygon([(_p(x), _p(y)) for x, y in pts], fill=255)
    return m


def elip(cx, cy, rx, ry, rot=0.0):
    c, s = math.cos(rot), math.sin(rot)
    pts = []
    for i in range(96):
        t = i * 2 * math.pi / 96
        x, y = rx * math.cos(t), ry * math.sin(t)
        pts.append((cx + x * c - y * s, cy + x * s + y * c))
    return poli(pts)


def trazo(pts, w):
    """Una linea quebrada con las puntas y los codos redondeados."""
    m = _m()
    d = ImageDraw.Draw(m)
    q = [(_p(x), _p(y)) for x, y in pts]
    d.line(q, fill=255, width=_p(w), joint='curve')
    r = _p(w) / 2
    for x, y in q:
        d.ellipse([x - r, y - r, x + r, y + r], fill=255)
    return m


def cono(p0, p1, w0, w1, pasos=40):
    """Un trazo que cambia de grosor de un punto al otro."""
    m = _m()
    d = ImageDraw.Draw(m)
    for i in range(pasos + 1):
        t = i / pasos
        x = p0[0] + (p1[0] - p0[0]) * t
        y = p0[1] + (p1[1] - p0[1]) * t
        r = (w0 + (w1 - w0) * t) / 2
        d.ellipse([_p(x - r), _p(y - r), _p(x + r), _p(y + r)], fill=255)
    return m


def arco(cx, cy, r, a0, a1, w0, w1=None, afilado=False, pasos=72):
    """Un trazo curvo (angulos en grados: 0 = derecha, 90 = abajo). Si {@code afilado}, las dos puntas acaban en punta."""
    if w1 is None:
        w1 = w0
    m = _m()
    d = ImageDraw.Draw(m)
    for i in range(pasos + 1):
        t = i / pasos
        a = math.radians(a0 + (a1 - a0) * t)
        x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
        w = math.sin(math.pi * t) * w0 if afilado else w0 + (w1 - w0) * t
        rr = max(0.05, w / 2)
        d.ellipse([_p(x - rr), _p(y - rr), _p(x + rr), _p(y + rr)], fill=255)
    return m


def anillo(cx, cy, r, w):
    return resta(circ(cx, cy, r + w / 2), circ(cx, cy, r - w / 2))


def estrella(cx, cy, R, r, n=5, rot=-90.0):
    pts = []
    for i in range(n * 2):
        a = math.radians(rot + i * 180.0 / n)
        rr = R if i % 2 == 0 else r
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    return poli(pts)


def destello(cx, cy, R, fino=0.26):
    """Una estrellita de cuatro puntas (brillo)."""
    pts = []
    for i in range(8):
        a = math.radians(-90 + i * 45)
        rr = R if i % 2 == 0 else R * fino
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    return poli(pts)


def rect(x0, y0, x1, y1, r=0.0):
    m = _m()
    d = ImageDraw.Draw(m)
    if r > 0:
        d.rounded_rectangle([_p(x0), _p(y0), _p(x1), _p(y1)], radius=_p(r), fill=255)
    else:
        d.rectangle([_p(x0), _p(y0), _p(x1), _p(y1)], fill=255)
    return m


def corazon(cx, cy, w):
    """Un corazon de ancho {@code w} centrado en (cx, cy)."""
    r = w / 4
    return union(circ(cx - r, cy - r * 0.55, r * 1.02), circ(cx + r, cy - r * 0.55, r * 1.02),
                 poli([(cx - w / 2 + 0.05, cy - r * 0.2), (cx + w / 2 - 0.05, cy - r * 0.2), (cx, cy + w * 0.5)]))


def gota(cx, cy, r, alto=None):
    """Una gota (punta arriba) con el centro de la parte redonda en (cx, cy)."""
    alto = alto or r * 2.1
    return union(circ(cx, cy, r), poli([(cx - r * 0.93, cy - r * 0.35), (cx + r * 0.93, cy - r * 0.35), (cx, cy - alto)]))


def hoja(cx, cy, largo, ancho, rot):
    """Una hoja (forma de limon puntiaguda) de {@code largo} centrada en (cx, cy), girada {@code rot} grados."""
    a = math.radians(rot)
    c, s = math.cos(a), math.sin(a)
    pts = []
    n = 24
    for i in range(n + 1):
        t = i / n
        x = (t - 0.5) * largo
        y = math.sin(math.pi * t) ** 0.8 * ancho / 2
        pts.append((cx + x * c - y * s, cy + x * s + y * c))
    for i in range(n, -1, -1):
        t = i / n
        x = (t - 0.5) * largo
        y = -math.sin(math.pi * t) ** 0.8 * ancho / 2
        pts.append((cx + x * c - y * s, cy + x * s + y * c))
    return poli(pts)


def union(*ms):
    return reduce(ImageChops.lighter, ms)


def inter(a, b):
    return ImageChops.multiply(a, b)


def resta(a, b):
    return ImageChops.subtract(a, b)


def mover(m, dx, dy):
    return ImageChops.offset(m, _p(dx), _p(dy))


# ----------------------------------------------------------------------------------------------------------------------------------------- el dibujo

class Dibujo:
    def __init__(self):
        self.img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
        self.colores = {CONTORNO}

    def pintar(self, mask, rampa, bisel=1.0):
        """Rellena la forma con el tono medio y le da volumen: borde claro arriba-izquierda y oscuro abajo-derecha."""
        self.colores |= {rampa.dark, rampa.mid, rampa.light, rampa.hi}
        self.img.paste(rampa.mid + (255,), mask=mask)
        if bisel > 0:
            self.img.paste(rampa.light + (255,), mask=resta(mask, mover(mask, bisel, bisel)))
            self.img.paste(rampa.dark + (255,), mask=resta(mask, mover(mask, -bisel, -bisel)))

    def plano(self, mask, color):
        """Rellena la forma con un solo color (una tupla RGB o el nombre de un tono de una rampa: ('fire', 'hi'))."""
        if isinstance(color, tuple) and len(color) == 2 and isinstance(color[0], str):
            color = getattr(RAMPAS[color[0]], color[1])
        self.colores.add(color)
        self.img.paste(color + (255,), mask=mask)

    def esfera(self, cx, cy, r, rampa, brillo=True):
        """Una bola con volumen: base con borde, una zona clara arriba-izquierda y un brillo."""
        self.pintar(circ(cx, cy, r), rampa, bisel=max(0.9, r * 0.22))
        if brillo and r >= 2.2:
            self.plano(circ(cx - r * 0.35, cy - r * 0.38, r * 0.28), rampa.hi)

    def terminar(self):
        """Lo reduce a 24x24, deja solo los colores de la paleta y le pone el contorno. Devuelve una imagen RGBA de 24x24."""
        a = np.asarray(self.img).astype(np.float32)
        al = a[..., 3:4] / 255.0
        pre = a[..., :3] * al

        def bloque(x):
            return x.reshape(N, K, N, K, x.shape[-1]).mean(axis=(1, 3))

        pre_b, al_b = bloque(pre), bloque(al)
        rgb = np.where(al_b > 1e-6, pre_b / np.maximum(al_b, 1e-6), 0.0)
        opaco = al_b[..., 0] >= 0.5
        pal = np.array(sorted(self.colores), dtype=np.float32)
        d2 = ((rgb[:, :, None, :] - pal[None, None, :, :]) ** 2).sum(axis=-1)
        cerca = pal[np.argmin(d2, axis=-1)]
        out = np.zeros((N, N, 4), dtype=np.uint8)
        out[opaco, :3] = cerca[opaco].astype(np.uint8)
        out[opaco, 3] = 255
        # contorno: los pixeles vacios que tocan (arriba, abajo, izquierda, derecha) uno lleno
        pad = np.pad(opaco, 1)
        toca = pad[:-2, 1:-1] | pad[2:, 1:-1] | pad[1:-1, :-2] | pad[1:-1, 2:]
        borde = toca & ~opaco
        out[borde, :3] = np.array(CONTORNO, dtype=np.uint8)
        out[borde, 3] = 255
        return Image.fromarray(out, 'RGBA')


def nuevo():
    return Dibujo()
