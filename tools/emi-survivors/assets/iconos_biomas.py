#!/usr/bin/env python3
"""Los iconos de las tres arenas (biomas) del menu de Emi Survivors: la pradera (una colina con una flor y el sol), el bosque embrujado (una calabaza encendida
bajo la luna) y el cosmos (un planeta con anillo y estrellas). Mismas herramientas que el resto (iconos_base.py); claves del atlas: b_pradera, b_bosque, b_cosmos."""
import math

from iconos_base import *


def _girar(pts, cx, cy, grados):
    a = math.radians(grados)
    c, s = math.cos(a), math.sin(a)
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for x, y in pts]


def g_pradera():
    d = nuevo()
    # el sol, detras de la colina
    d.esfera(17.4, 7.2, 4.0, RAMPAS['oro'])
    for a in range(0, 360, 45):
        r0, r1 = 5.6, 7.0
        x0, y0 = 17.4 + math.cos(math.radians(a)) * r0, 7.2 + math.sin(math.radians(a)) * r0
        x1, y1 = 17.4 + math.cos(math.radians(a)) * r1, 7.2 + math.sin(math.radians(a)) * r1
        if y1 < 14:
            d.plano(trazo([(x0, y0), (x1, y1)], 1.2), RAMPAS['oro'].light)
    # la colina (una cupula de hierba) y una segunda mas lejana
    d.pintar(inter(elip(17.0, 25.5, 11.5, 8.6), rect(0, 0, 24, 24)), RAMPAS['menta'], 0.8)
    d.plano(trazo([(14.2, 19.2), (18.6, 18.6)], 0.9), RAMPAS['menta'].hi)
    d.pintar(inter(elip(6.0, 25.0, 9.2, 10.4), rect(0, 0, 24, 24)), RAMPAS['grass'], 0.8)
    d.plano(trazo([(2.4, 18.4), (5.6, 16.6)], 0.9), RAMPAS['grass'].hi)
    # una flor
    d.pintar(trazo([(8.0, 22.6), (8.0, 14.4)], 1.5), RAMPAS['grass'], 0.4)
    d.pintar(hoja(10.2, 19.0, 3.8, 1.8, -30), RAMPAS['grass'], 0.4)
    for a in range(0, 360, 72):
        px, py = 8.0 + math.cos(math.radians(a - 90)) * 2.7, 12.4 + math.sin(math.radians(a - 90)) * 2.7
        d.esfera(px, py, 1.9, RAMPAS['fairy'], brillo=False)
    d.esfera(8.0, 12.4, 1.6, RAMPAS['oro'], brillo=False)
    return d


def g_bosque():
    d = nuevo()
    # la luna creciente, arriba a la izquierda
    luna = resta(circ(5.6, 5.4, 4.0), circ(7.4, 4.2, 3.4))
    d.pintar(luna, RAMPAS['hueso'], 0.6)
    d.plano(destello(19.6, 4.6, 2.2, 0.34), RAMPAS['oro'].hi)
    d.plano(destello(2.6, 14.2, 1.6, 0.34), RAMPAS['oro'].hi)
    # el rabito y la calabaza (tres gajos)
    d.pintar(poli([(11.0, 7.6), (13.4, 7.6), (14.6, 4.6), (12.6, 4.2)]), RAMPAS['grass'], 0.5)
    d.pintar(elip(7.4, 15.0, 4.8, 6.4), RAMPAS['fire'], 0.8)
    d.pintar(elip(16.6, 15.0, 4.8, 6.4), RAMPAS['fire'], 0.8)
    d.pintar(elip(12.0, 15.2, 5.6, 7.4), RAMPAS['fire'], 0.9)
    # la cara encendida
    luz = RAMPAS['oro'].hi
    d.plano(poli([(8.2, 12.2), (10.6, 12.2), (9.4, 9.8)]), luz)
    d.plano(poli([(13.4, 12.2), (15.8, 12.2), (14.6, 9.8)]), luz)
    boca = poli([(7.6, 16.0), (9.0, 15.2), (10.4, 16.6), (12.0, 15.2), (13.6, 16.6), (15.0, 15.2), (16.4, 16.0), (15.4, 19.0), (12.0, 20.0), (8.6, 19.0)])
    d.plano(boca, luz)
    d.plano(poli([(10.2, 17.4), (11.4, 17.4), (10.8, 19.2)]), RAMPAS['fire'].dark)
    d.plano(poli([(12.8, 17.4), (14.0, 17.4), (13.4, 19.2)]), RAMPAS['fire'].dark)
    return d


def g_cosmos():
    d = nuevo()
    ang = -24.0
    cx, cy = 12.0, 12.5
    anillo_m = resta(elip(cx, cy, 11.4, 3.7, math.radians(ang)), elip(cx, cy, 8.2, 2.1, math.radians(ang)))
    # el lado de atras del anillo (la mitad de arriba, girada con el anillo), el planeta y el lado de delante
    delante = poli(_girar([(-4, cy), (28, cy), (28, 30), (-4, 30)], cx, cy, ang))
    atras = poli(_girar([(-4, -6), (28, -6), (28, cy), (-4, cy)], cx, cy, ang))
    d.pintar(inter(anillo_m, atras), RAMPAS['oro'], 0.5)
    d.esfera(cx, cy, 6.6, RAMPAS['psychic'])
    d.plano(trazo([(8.6, 9.0), (13.2, 7.4)], 0.9), RAMPAS['psychic'].light)
    d.plano(trazo([(7.6, 14.6), (15.6, 12.2)], 0.9), RAMPAS['psychic'].dark)
    d.pintar(inter(anillo_m, delante), RAMPAS['oro'], 0.5)
    # estrellitas
    d.plano(destello(4.0, 4.4, 2.8, 0.32), RAMPAS['dragon'].hi)
    d.plano(destello(20.4, 4.0, 2.0, 0.32), RAMPAS['fairy'].hi)
    d.plano(destello(19.6, 20.6, 2.6, 0.32), RAMPAS['oro'].hi)
    d.plano(circ(3.6, 19.6, 0.9), RAMPAS['fairy'].hi)
    return d


BIOMAS = {'pradera': g_pradera, 'bosque': g_bosque, 'cosmos': g_cosmos}
