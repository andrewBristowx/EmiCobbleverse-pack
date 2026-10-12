"""Piezas reutilizables para los gimnasios: techos, arcos, mobiliario, luces."""
import math, random
from gymlib import V

OPUESTO = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}

def techo_dos_aguas(v, x0, x1, z0, z1, y0, bloque, esc, eje='z', relleno_interior=False):
    """tejado a dos aguas con pendiente 1:1. eje='z': la cumbrera va a lo largo de z y las aguas caen hacia -x y +x. Devuelve la y de la cumbrera."""
    if eje == 'z':
        k = 0
        while x0 + k < x1 - k:
            y = y0 + k; xl, xr = x0 + k, x1 - k
            for z in range(z0, z1 + 1):
                v.stairs(xl, y, z, esc, 'west'); v.stairs(xr, y, z, esc, 'east')
                v.set(xl + 1, y, z, bloque); v.set(xr - 1, y, z, bloque)
                if relleno_interior: v.fill(xl + 1, y, z, xr - 1, y, z, bloque)
            k += 1
        y = y0 + k
        for z in range(z0, z1 + 1): v.set(x0 + k, y, z, bloque)
        return y
    k = 0
    while z0 + k < z1 - k:
        y = y0 + k; za, zb = z0 + k, z1 - k
        for x in range(x0, x1 + 1):
            v.stairs(x, y, za, esc, 'north'); v.stairs(x, y, zb, esc, 'south')
            v.set(x, y, za + 1, bloque); v.set(x, y, zb - 1, bloque)
        k += 1
    y = y0 + k
    for x in range(x0, x1 + 1): v.set(x, y, z0 + k, bloque)
    return y

def frontón(v, cx, z, y0, ancho, bloque, esc, marco=None):
    """triangulo de fachada (hastial) de la altura que de el ancho (1:1) en el plano z"""
    n = ancho // 2
    for k in range(n + 1):
        v.fill(cx - n + k, y0 + k, z, cx + n - k, y0 + k, z, bloque)
    if marco:
        for k in range(n + 1):
            v.set(cx - n + k, y0 + k, z, marco); v.set(cx + n - k, y0 + k, z, marco)
    return y0 + n

def columna(v, x, y0, y1, z, base, fuste, capitel):
    v.set(x, y0, z, base); v.fill(x, y0 + 1, z, x, y1 - 1, z, fuste); v.set(x, y1, z, capitel)

def lampara_colgante(v, x, y_techo, z, largo, luz='lantern[hanging=true]', cadena='chain[axis=y]'):
    for i in range(largo): v.set(x, y_techo - 1 - i, z, cadena)
    v.set(x, y_techo - 1 - largo, z, luz)

def arco_rect(v, cx, y0, z, ancho, alto, marco, aire=True):
    """vano rectangular centrado en cx con jambas de 'marco'"""
    h = ancho // 2
    if aire: v.aire(cx - h, y0, z, cx + h, y0 + alto - 1, z)
    for y in range(y0, y0 + alto):
        v.set(cx - h - 1, y, z, marco); v.set(cx + h + 1, y, z, marco)
    v.fill(cx - h - 1, y0 + alto, z, cx + h + 1, y0 + alto, z, marco)

def barril(v, x, y, z, tipo='barrel[facing=up]'): v.set(x, y, z, tipo)

def mesa(v, x, y, z, tablero='spruce_slab[type=top]', pata='oak_fence'):
    v.set(x, y, z, pata); v.set(x, y + 1, z, tablero)

def silla(v, x, y, z, esc, facing): v.stairs(x, y, z, esc, facing)

def estante_pared_z(v, x0, x1, y0, y1, z, estante='bookshelf'):
    v.fill(x0, y0, z, x1, y1, z, estante)

def tablero_damas(v, x0, z0, x1, z1, y, a, b, paso=1):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            v.set(x, y, z, a if ((x - x0) // paso + (z - z0) // paso) % 2 == 0 else b)

def bola_de_luz(v, x, y, z, bloque='sea_lantern'): v.set(x, y, z, bloque)

def seed(n): return random.Random(n)
