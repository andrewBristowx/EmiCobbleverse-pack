"""Los dibujos de los 30 ataques (nivel 1-5). Cada funcion recibe la rampa de colores con la que se pinta (por defecto la suya; para el ataque inicial de cada tipo
de Pokemon se pasa la rampa del tipo) y devuelve un {@code Dibujo}. Todo cabe en 24x24 con 1 pixel de margen para el contorno."""
from iconos_base import *


def g_disparo(p=None):
    p = p or RAMPAS['energia']
    d = nuevo()
    d.pintar(cono((11, 12), (1.8, 12), 6.4, 0.4), p, 0.9)                       # la cola, de punta a punta
    d.plano(cono((10.5, 12), (4.5, 12), 2.2, 0.3), p.hi)
    d.esfera(15.6, 12, 6.0, p)
    return d


def g_orbes(p=None):
    p = p or RAMPAS['ghost']
    d = nuevo()
    d.plano(anillo(12, 12, 8.6, 1.0), p.dark)
    for ang in (-90, 30, 150):
        a = math.radians(ang)
        d.esfera(12 + math.cos(a) * 8.6, 12 + math.sin(a) * 8.6, 3.0, p)
    d.pintar(resta(circ(12, 12, 3.9), circ(13.7, 10.8, 3.4)), RAMPAS['oro'], 0.6)    # la luna del centro
    return d


def g_pulso(p=None):
    p = p or RAMPAS['psychic']
    d = nuevo()
    d.pintar(anillo(12, 12, 9.6, 1.7), p, 0.5)
    d.pintar(anillo(12, 12, 6.2, 1.9), p, 0.6)
    d.esfera(12, 12, 2.8, p)
    return d


def g_cadena(p=None):
    p = p or RAMPAS['electric']
    d = nuevo()
    d.pintar(poli([(16.5, 1.8), (7.6, 13), (12.4, 13), (9.2, 22.4), (18.8, 9.2), (13.6, 9.2)]), p, 0.9)
    d.plano(destello(4.6, 6.4, 3.6, 0.36), p.light)
    d.plano(destello(19.6, 18.2, 3.2, 0.36), p.light)
    return d


def g_meteoros(p=None):
    d = nuevo()
    f = RAMPAS['fire']
    d.pintar(poli([(11, 10.5), (22.4, 1.8), (17.4, 9), (22.6, 9.8), (15.2, 16.8)]), f, 0.8)
    d.plano(poli([(12.5, 12), (19.6, 5), (16.6, 11.4)]), f.light)
    d.esfera(9.5, 15.2, 5.8, RAMPAS['marron'])
    d.plano(circ(7.4, 17.2, 1.2), RAMPAS['marron'].dark)
    d.plano(circ(11.6, 18.0, 0.9), RAMPAS['marron'].dark)
    d.plano(circ(7.8, 12.6, 0.8), RAMPAS['marron'].dark)
    d.esfera(18.6, 18.4, 2.2, f)
    return d


def g_tajo(p=None):
    p = p or RAMPAS['fighting']
    d = nuevo()
    d.pintar(cono((2.6, 21.4), (21.6, 2.4), 0.6, 7.0), p, 0.9)
    d.plano(cono((4.4, 19.4), (20.2, 3.8), 0.4, 3.4), RAMPAS['hueso'].hi)
    d.plano(destello(19.4, 19.2, 3.4, 0.36), RAMPAS['hueso'].hi)
    d.plano(destello(5.0, 5.6, 3.0, 0.36), p.light)
    return d


def g_bumeran(p=None):
    punta = RAMPAS['rojo'].mid if p is None else p.dark                           # las puntas rojas del bumeran amarillo; en los demas, un tono oscuro de su color
    p = p or RAMPAS['oro']
    d = nuevo()
    d.plano(arco(11, 13, 10.6, 18, 78, 1.3), RAMPAS['hueso'].light)              # lo que gira al volar
    d.plano(arco(11, 13, 10.6, 202, 262, 1.3), RAMPAS['hueso'].light)
    d.pintar(arco(17.0, 17.0, 12.4, 176, 278, 5.8, 5.8), p, 1.0)
    for a0, a1 in ((178, 192), (262, 276)):
        d.plano(arco(17.0, 17.0, 12.4, a0, a1, 5.8, 5.8), punta)
    d.plano(arco(17.0, 17.0, 12.4, 206, 250, 1.3), p.hi)
    return d


def g_estela(p=None):
    p = p or RAMPAS['poison']
    d = nuevo()
    d.pintar(elip(12, 17.5, 10, 4.4), p, 0.9)
    d.plano(elip(11, 17, 6.6, 2.4), p.light)
    d.esfera(7.6, 11.4, 2.5, p)
    d.esfera(14.4, 8.8, 3.3, p)
    d.esfera(18.6, 13.4, 1.8, p)
    d.esfera(11.4, 4, 1.7, p)
    return d


def g_minas(p=None):
    p = p or RAMPAS['steel']
    d = nuevo()
    d.pintar(elip(12, 20.6, 10.2, 2.7), RAMPAS['marron'], 0.7)
    d.pintar(poli([(9.4, 20.5), (12.6, 2.4), (15.8, 20.5)]), p, 0.9)
    d.pintar(poli([(4.2, 20.8), (6.6, 8.2), (9.8, 20.8)]), p, 0.8)
    d.pintar(poli([(14.4, 20.8), (17.8, 7.2), (20, 20.8)]), p, 0.8)
    d.plano(circ(12.6, 4.2, 0.8), p.hi)
    return d


def g_rayo(p=None):
    p = p or RAMPAS['electric']
    d = nuevo()
    d.plano(arco(12, 12, 9.8, 205, 285, 1.5), p.light)
    d.plano(arco(12, 12, 9.8, 25, 105, 1.5), p.light)
    d.plano(poli([(10.6, 1.4), (14.8, 3.6), (11.2, 5.8)]), p.light)
    d.plano(poli([(13.4, 22.6), (9.2, 20.4), (12.8, 18.2)]), p.light)
    d.pintar(cono((12, 12), (21.4, 5), 4.2, 0.9), p, 0.7)
    d.pintar(cono((12, 12), (2.6, 19), 4.2, 0.9), p, 0.7)
    d.esfera(12, 12, 3.3, RAMPAS['water'])
    return d


ATAQUES_1 = {
    'disparo': g_disparo, 'orbes': g_orbes, 'pulso': g_pulso, 'cadena': g_cadena, 'meteoros': g_meteoros,
    'tajo': g_tajo, 'bumeran': g_bumeran, 'estela': g_estela, 'minas': g_minas, 'rayo': g_rayo,
}

from iconos_ataques2 import ATAQUES_2

ATAQUES = {**ATAQUES_1, **ATAQUES_2}
