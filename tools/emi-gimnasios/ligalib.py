"""Piezas para las ligas (Alto Mando y campeon): salas con techo, vanos en muros, torres, tarimas y teatros."""
import math
from gymlib import V
from piezas2 import *

ORO = 'gold_block'


def sala(v, x0, x1, z0, z1, alto, muro, suelo, techo=None, banda=None, y0=1):
    """sala cerrada: muros y techo de `muro`/`techo`, suelo `suelo` en y0, interior de aire hasta y0+alto"""
    v.box(x0, y0, z0, x1, y0 + alto + 1, z1, muro)
    v.fill(x0 + 1, y0, z0 + 1, x1 - 1, y0, z1 - 1, suelo)
    v.aire(x0 + 1, y0 + 1, z0 + 1, x1 - 1, y0 + alto, z1 - 1)
    if techo: v.fill(x0, y0 + alto + 1, z0, x1, y0 + alto + 1, z1, techo)
    if banda:
        v.anillo(x0, z0, x1, z1, y0 + alto, banda); v.anillo(x0, z0, x1, z1, y0 + 2, banda)


def arco(v, x, z, eje, ancho, alto, marco, y0=2):
    """vano en un muro: eje='x' => el muro esta en el plano x=cte y se abre a lo largo de z; eje='z' => muro en z=cte, se abre a lo largo de x"""
    h = ancho // 2
    if eje == 'x':
        v.aire(x, y0, z - h, x, y0 + alto - 1, z + h)
        for y in range(y0, y0 + alto): v.set(x, y, z - h - 1, marco); v.set(x, y, z + h + 1, marco)
        v.fill(x, y0 + alto, z - h - 1, x, y0 + alto, z + h + 1, marco)
    else:
        v.aire(x - h, y0, z, x + h, y0 + alto - 1, z)
        for y in range(y0, y0 + alto): v.set(x - h - 1, y, z, marco); v.set(x + h + 1, y, z, marco)
        v.fill(x - h - 1, y0 + alto, z, x + h + 1, y0 + alto, z, marco)


def torre_cono(v, cx, cz, r, alto, muro, techo, banda, remate=ORO):
    v.cilindro(cx, 1, alto, cz, r, muro)
    v.disco(cx, 1, cz, r, 'polished_andesite')
    v.anillo_circ(cx, alto - 1, cz, r, banda, 1.0)
    cono(v, cx, alto + 1, cz, r + 1, r * 2 + 2, techo)
    v.set(cx, alto + r * 2 + 3, cz, remate)


def tejado_terrazas(v, x0, x1, z0, z1, y, bloque, borde, pasos=3):
    for k in range(pasos):
        if x0 + 1 + 2 * k >= x1 - 1 - 2 * k or z0 + 1 + 2 * k >= z1 - 1 - 2 * k: break
        v.fill(x0 + 1 + k * 2, y + k, z0 + 1 + k * 2, x1 - 1 - k * 2, y + k, z1 - 1 - k * 2, bloque)
        v.anillo(x0 + 1 + k * 2, z0 + 1 + k * 2, x1 - 1 - k * 2, z1 - 1 - k * 2, y + k, borde)


def tarima(v, cx, z, ancho, mat, esc, y=2, sentido='north'):
    """estrado de 2 alturas con peldaños frontales; el NPC va en y+2 (mirando hacia -z si sentido='north')"""
    v.fill(cx - ancho, y, z - 2, cx + ancho, y, z + 1, mat)
    for x in range(cx - ancho, cx + ancho + 1): v.stairs(x, y, z - 3, esc, sentido)
    v.fill(cx - ancho + 2, y + 1, z - 1, cx + ancho - 2, y + 1, z + 1, mat)
    for x in range(cx - ancho + 2, cx + ancho - 1): v.stairs(x, y + 1, z - 2, esc, sentido)


def sala_circular(v, cx, cz, r, alto, muro, suelo_f, cupula_bloque=None, ry=None):
    """sala redonda (rotonda) con suelo dibujado por la funcion suelo_f(d, ang) y cupula opcional"""
    v.cilindro(cx, 1, alto + 1, cz, r, muro)
    disco_f(v, cx, 1, cz, r - 1, suelo_f)
    if cupula_bloque: v.cupula(cx, alto + 2, cz, r, cupula_bloque, hueco=True, ry=ry or r)


def tarima_lado(v, x0, z, lado, ancho, mat, esc):
    """estrado pegado a la pared x0 que se abre hacia +x (lado=1) o -x (lado=-1): dos alturas; el NPC va en la grada alta (y=4) a 2-3 bloques de la pared"""
    for dz in range(-ancho, ancho + 1):
        v.fill(x0, 2, z + dz, x0 + lado * 5, 2, z + dz, mat); v.fill(x0, 3, z + dz, x0 + lado * 3, 3, z + dz, mat)
        v.stairs(x0 + lado * 6, 2, z + dz, esc, 'east' if lado > 0 else 'west'); v.stairs(x0 + lado * 4, 3, z + dz, esc, 'east' if lado > 0 else 'west')


def luces_colgantes(v, xa, xb, za, zb, hp, luz='sea_lantern', paso=6):
    if luz in ('lantern', 'soul_lantern'): luz += '[hanging=true]'
    for x in range(xa + 4, xb - 2, paso):
        for z in range(za + 4, zb - 2, paso):
            for k in range(1, 3): v.set(x, hp - k + 1, z, 'chain[axis=y]')
            v.set(x, hp - 2, z, luz)
