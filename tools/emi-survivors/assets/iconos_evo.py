"""Los dibujos de las 30 evoluciones (nivel 6) y las variantes del ataque inicial de cada tipo de Pokemon.
Cada evolucion es el mismo ataque «a lo grande» (mas, mas grande o mas bonito) y lleva una estrellita dorada en la esquina de arriba a la derecha."""
from iconos_base import *
import iconos_ataques as A
from iconos_ataques2 import g_centinela


def marca_evo(d):
    """La estrella dorada de «evolucionado» (arriba a la derecha)."""
    d.pintar(estrella(19.0, 5.2, 4.6, 2.1), RAMPAS['oro'], 0.5)


def llama(d, cx, cy, s, p):
    """Una llama de escala s con la base en (cx, cy+...): el dibujo del Lanzallamas, centrado en (cx, cy)."""
    def P(x, y):
        return (cx + (x - 12) * s, cy + (y - 12) * s)
    d.pintar(union(circ(*P(12, 15.4), 6.8 * s), poli([P(5.8, 13.4), P(9.2, 8.4), P(12.6, 1.6), P(14.4, 7.4), P(18.2, 13.4)]),
                   poli([P(15.4, 11), P(19.8, 6.6), P(19.2, 14)])), p, max(0.5, 1.0 * s))
    d.plano(union(circ(*P(12, 17), 3.9 * s), poli([P(8.6, 15.6), P(11.6, 9.6), P(15.4, 15.6)])), p.light)
    d.plano(circ(*P(12, 18.2), 2.1 * s), p.hi)


def flor(d, cx, cy, r, p, centro=None):
    for i in range(5):
        a = math.radians(i * 72 - 90)
        d.esfera(cx + math.cos(a) * r * 1.35, cy + math.sin(a) * r * 1.35, r, p, brillo=False)
    d.esfera(cx, cy, r * 0.9, centro or RAMPAS['oro'], brillo=False)


def e_disparo():
    p = RAMPAS['energia']
    d = nuevo()
    for y, r, w in ((12, 4.8, 5.4), (5.2, 2.9, 3.0), (18.8, 2.9, 3.0)):
        d.pintar(cono((12.6, y), (1.8, y), w, 0.4), p, 0.7)
        d.esfera(15.2 + (0 if y == 12 else -1.6), y, r, p)
    return d


def e_orbes():
    d = nuevo()
    o = RAMPAS['oro']
    rayos = []
    for i in range(12):
        a = math.radians(i * 30)
        pa, pb = a - math.radians(7), a + math.radians(7)
        rayos.append(poli([(12 + math.cos(pa) * 7.0, 12 + math.sin(pa) * 7.0), (12 + math.cos(a) * 10.8, 12 + math.sin(a) * 10.8),
                           (12 + math.cos(pb) * 7.0, 12 + math.sin(pb) * 7.0)]))
    d.pintar(union(*rayos), o, 0.5)
    d.pintar(anillo(12, 12, 6.4, 1.6), o, 0.6)
    d.pintar(circ(12, 12, 5.6), RAMPAS['negro'], 0.8)
    d.plano(circ(12, 12, 4.9), RAMPAS['negro'].dark)
    return d


def e_pulso():
    p = RAMPAS['psychic']
    d = nuevo()
    d.pintar(anillo(12, 12, 9.4, 1.6), p, 0.5)
    d.pintar(anillo(12, 12, 6.0, 1.6), p, 0.5)
    for a in (0, 90, 180, 270):
        r = math.radians(a)
        c, s = math.cos(r), math.sin(r)
        pts = [(12 + c * 11.0 - s * 1.7, 12 + s * 11.0 + c * 1.7), (12 + c * 11.0 + s * 1.7, 12 + s * 11.0 - c * 1.7), (12 + c * 7.6, 12 + s * 7.6)]
        d.pintar(poli(pts), RAMPAS['hueso'], 0.4)
    d.pintar(circ(12, 12, 3.2), RAMPAS['negro'], 0.7)
    d.plano(circ(12, 12, 2.4), RAMPAS['negro'].dark)
    return d


def e_cadena():
    d = nuevo()
    nube = RAMPAS['ghost']
    p = RAMPAS['electric']
    d.pintar(union(circ(7, 7.6, 3.6), circ(12, 5.8, 4.6), circ(17, 7.8, 3.4), rect(3.4, 7.4, 20.6, 10.8, 1.6)), nube, 0.9)
    d.pintar(poli([(13.6, 10), (8.8, 16.4), (12, 16.4), (9.8, 22.4), (16, 14.2), (12.8, 14.2), (15.4, 10)]), p, 0.8)
    d.pintar(poli([(6.4, 11), (3.6, 15.4), (5.6, 15.4), (4.4, 19), (8, 13.6), (6.4, 13.6), (7.8, 11)]), p, 0.5)
    d.pintar(poli([(19.2, 11.6), (16.6, 15), (18.2, 15), (17.2, 18), (20.4, 13.8), (19, 13.8), (20.2, 11.6)]), p, 0.5)
    return d


def e_meteoros():
    d = nuevo()
    f = RAMPAS['fire']
    m = RAMPAS['marron']
    for (x, y, r, tx, ty, w) in ((6.2, 16.8, 3.4, 15.4, 7.0, 3.6), (14.6, 19.4, 2.7, 22.0, 12.0, 3.0), (10.4, 8.8, 2.5, 16.8, 2.6, 2.8)):
        d.pintar(cono((x, y), (tx, ty), w, 0.3), f, 0.6)
        d.esfera(x, y, r, m)
    return d


def e_tajo():
    p = RAMPAS['dragon']
    d = nuevo()
    d.pintar(cono((2.6, 21.4), (21.6, 2.4), 0.6, 6.4), p, 0.9)
    d.pintar(cono((2.4, 2.8), (21.4, 21.4), 0.6, 6.4), p, 0.9)
    d.plano(cono((4.4, 19.4), (20.2, 3.8), 0.4, 3.0), RAMPAS['hueso'].hi)
    d.plano(cono((4.0, 4.4), (20.0, 19.8), 0.4, 3.0), RAMPAS['hueso'].hi)
    d.plano(destello(12, 12, 4.2, 0.36), RAMPAS['hueso'].hi)
    return d


def e_bumeran():
    p = RAMPAS['oro']
    d = nuevo()
    for (x, y) in ((12, 4.8), (19.2, 12), (12, 19.2), (4.8, 12)):
        d.pintar(circ(x, y, 3.6), RAMPAS['steel'], 0.8)
        d.plano(circ(x, y, 1.4), RAMPAS['negro'].mid)
    d.esfera(12, 12, 3.4, p)
    return d


def e_estela():
    p = RAMPAS['grass']
    d = nuevo()
    d.pintar(elip(12, 18.4, 10, 3.8), p, 0.9)
    d.plano(elip(11, 18, 6.4, 2.0), p.light)
    d.pintar(corazon(12, 9.6, 12.4), RAMPAS['rosa'], 0.9)
    d.plano(circ(9.4, 7.4, 1.2), RAMPAS['rosa'].hi)
    d.esfera(4.4, 13.4, 1.7, p)
    d.esfera(19.8, 12.6, 2.0, p)
    return d


def e_minas():
    d = nuevo()
    h = RAMPAS['steel']
    pinchos = []
    for i in range(10):
        a = math.radians(i * 36 - 90)
        pa, pb = a - math.radians(10), a + math.radians(10)
        pinchos.append(poli([(12 + math.cos(pa) * 5.0, 12 + math.sin(pa) * 5.0), (12 + math.cos(a) * 10.8, 12 + math.sin(a) * 10.8),
                             (12 + math.cos(pb) * 5.0, 12 + math.sin(pb) * 5.0)]))
    d.pintar(union(*pinchos), h, 0.5)
    d.esfera(12, 12, 5.6, RAMPAS['ground'])
    d.plano(destello(12, 12, 3.0, 0.4), RAMPAS['fire'].light)
    return d


def e_rayo():
    p = RAMPAS['oro']
    d = nuevo()
    for ang in (0, 90, 180, 270):
        a = math.radians(ang)
        d.pintar(hoja(12 + math.cos(a) * 5.8, 12 + math.sin(a) * 5.8, 11.0, 4.6, ang + 25), p, 0.7)
    d.esfera(12, 12, 3.4, RAMPAS['electric'])
    return d


def e_lanza():
    d = nuevo()
    llama(d, 5.6, 16.6, 0.52, RAMPAS['rojo'])
    llama(d, 18.4, 16.6, 0.52, RAMPAS['rojo'])
    llama(d, 12, 12.4, 0.98, RAMPAS['fire'])
    return d


def ola(d, p, espuma):
    """Una ola grande que se curva hacia la derecha, con espuma."""
    d.pintar(union(resta(circ(10.0, 12.6, 9.8), circ(15.6, 15.8, 6.4)), rect(1.6, 17.8, 22.4, 21.8, 1.6)), p, 1.0)
    d.plano(arco(10.0, 12.6, 7.4, 196, 292, 1.5), p.light)
    for (x, y, r) in ((17.6, 9.0, 1.7), (20.4, 12.4, 1.3), (14.6, 4.2, 1.5), (6.6, 5.6, 1.4), (19.6, 7.2, 1.0)):
        d.plano(circ(x, y, r), espuma)


def e_hidro():
    d = nuevo()
    ola(d, RAMPAS['water'], RAMPAS['hueso'].hi)
    return d


def e_solar():
    p = RAMPAS['oro']
    d = nuevo()
    d.pintar(poli([(8.4, 9.6), (15.6, 9.6), (19.6, 22.4), (4.4, 22.4)]), p, 0.8)
    d.plano(poli([(10.6, 10.4), (13.4, 10.4), (14.8, 22.4), (9.2, 22.4)]), RAMPAS['hueso'].hi)
    rayos = []
    for i in range(8):
        a = math.radians(i * 45 - 90)
        pa, pb = a - math.radians(14), a + math.radians(14)
        R = 6.8 if i % 2 == 0 else 5.8
        rayos.append(poli([(12 + math.cos(pa) * 4.0, 7.0 + math.sin(pa) * 4.0), (12 + math.cos(a) * R, 7.0 + math.sin(a) * R), (12 + math.cos(pb) * 4.0, 7.0 + math.sin(pb) * 4.0)]))
    d.pintar(union(*rayos), p, 0.5)
    d.esfera(12, 7.0, 3.6, RAMPAS['electric'])
    return d


def e_hojas():
    p = RAMPAS['grass']
    d = nuevo()
    for i in range(6):
        w = 17.4 - i * 2.9
        d.pintar(elip(12 + math.sin(i * 0.9) * 1.6, 5.2 + i * 3.2, w / 2, 1.9), p if i % 2 == 0 else RAMPAS['bug'], 0.6)
    d.pintar(hoja(4.4, 9.2, 6.4, 3.0, 30), p, 0.5)
    d.pintar(hoja(19.8, 13.4, 6.4, 3.0, -25), p, 0.5)
    d.pintar(hoja(5.4, 19.6, 6.0, 2.8, 150), p, 0.5)
    return d


def e_petalos():
    p = RAMPAS['rosa']
    d = nuevo()
    for (x, y, l, a) in ((6.2, 5.2, 6.6, 35), (15.6, 4.4, 5.6, 140), (11, 11.4, 7.4, 70), (19.4, 12.6, 6.0, 20), (5.2, 16.6, 5.8, 120), (14.4, 19.2, 6.6, 50)):
        d.pintar(hoja(x, y, l, l * 0.5, a), p, 0.6)
    return d


def e_lodo():
    d = nuevo()
    ola(d, RAMPAS['marron'], RAMPAS['marron'].hi)
    return d


def e_terremoto():
    d = nuevo()
    d.plano(poli([(8.4, 11.6), (14.8, 11.6), (14.2, 14.4), (12.4, 16.8), (13.8, 22.2), (9.6, 22.2), (11.4, 17.2), (9.2, 14.6)]), RAMPAS['fire'].light)
    d.plano(poli([(10.2, 13.6), (13.2, 13.6), (12.2, 16.6), (11.4, 20.6), (10.6, 16.6)]), RAMPAS['oro'].hi)
    m = RAMPAS['marron']
    d.pintar(poli([(1.6, 11.2), (9.2, 10.4), (8.6, 14.4), (10.8, 17.4), (9.0, 22.2), (1.6, 22.2)]), m, 0.9)
    d.pintar(poli([(14.6, 14.4), (22.4, 13.2), (22.4, 22.2), (12.4, 22.2), (13.6, 17.4), (12.6, 15.8)]), m, 0.9)
    d.esfera(6.4, 5.4, 1.9, RAMPAS['fire'])
    d.esfera(14.8, 4.2, 2.3, RAMPAS['fire'])
    d.esfera(19.6, 8.2, 1.5, RAMPAS['fire'])
    return d


def e_ventisca():
    p = RAMPAS['ice']
    d = nuevo()
    d.pintar(corazon(12, 12.6, 19.6), p, 1.0)
    d.plano(trazo([(12, 7.6), (12, 17.4)], 1.0), p.hi)
    d.plano(trazo([(7.6, 12.4), (16.4, 12.4)], 1.0), p.hi)
    d.plano(trazo([(8.8, 9.2), (15.2, 15.6)], 0.8), p.hi)
    d.plano(trazo([(15.2, 9.2), (8.8, 15.6)], 0.8), p.hi)
    d.plano(circ(7.4, 8.4, 1.3), RAMPAS['hueso'].hi)
    return d


def e_torbellino():
    p = RAMPAS['flying']
    d = nuevo()
    d.pintar(elip(12, 3.6, 10.4, 2.6), RAMPAS['steel'], 0.7)
    for i in range(5, -1, -1):
        w = 19 - i * 2.9
        d.pintar(elip(12 + math.sin(i * 0.9) * 2.0, 7.0 + i * 2.9, w / 2, 2.0), p if i % 2 == 0 else RAMPAS['dragon'], 0.6)
    d.plano(circ(2.8, 14.4, 0.9), RAMPAS['hueso'].hi)
    d.plano(circ(21.2, 12.0, 0.9), RAMPAS['hueso'].hi)
    d.plano(circ(20.0, 19.6, 0.8), RAMPAS['hueso'].hi)
    return d


def e_trueno():
    p = RAMPAS['oro']
    d = nuevo()
    d.pintar(poli([(15.6, 6.6), (7.4, 15.6), (12, 15.6), (9, 22.4), (18, 12.4), (13.4, 12.4), (17.4, 6.6)]), p, 0.9)
    d.pintar(resta(elip(12, 4.0, 7.0, 2.6), elip(12, 4.0, 4.4, 1.1)), RAMPAS['oro'], 0.5)
    d.plano(trazo([(2.4, 14), (5.6, 14)], 0.9), p.light)
    d.plano(trazo([(18.6, 18), (21.6, 18)], 0.9), p.light)
    return d


def e_granizo():
    p = RAMPAS['ice']
    d = nuevo()
    for (x, y, r) in ((6.0, 8.0, 3.0), (14.6, 6.6, 2.4), (11.4, 14.4, 3.6), (19.2, 14.0, 2.6), (5.4, 18.6, 2.4), (15.4, 20.0, 2.0)):
        d.pintar(cono((x - r * 0.8, y - r * 0.8), (x - r * 1.7, y - r * 1.7), r * 0.9, 0.3), RAMPAS['hueso'], 0.3)
        d.esfera(x, y, r, p)
    return d


def e_cometa():
    p = RAMPAS['cielo']
    d = nuevo()
    for (x, y, tx, ty, R) in ((15.6, 6.6, 3.4, 18.0, 4.8), (7.4, 12.0, 1.6, 20.0, 3.0), (19.6, 16.0, 9.6, 22.0, 3.0)):
        d.pintar(cono((x, y), (tx, ty), R * 1.1, 0.3), p, 0.6)
        d.pintar(estrella(x, y, R, R * 0.45), RAMPAS['oro'], 0.5)
    return d


def e_garra():
    p = RAMPAS['dark']
    d = nuevo()
    for i in range(4):
        x0, y0, x1, y1 = 5.6 + i * 4.4, 1.8, 2.2 + i * 4.4, 22.2
        mx, my = (x0 + x1) / 2 + 1.6, (y0 + y1) / 2
        d.pintar(union(cono((x0, y0), (mx, my), 0.4, 3.2), cono((mx, my), (x1, y1), 3.2, 0.4)), RAMPAS['ghost'], 0.6)
        d.plano(cono((x0 + 0.3, y0 + 3), (mx + 0.2, my - 1), 0.3, 1.0), RAMPAS['rojo'].light)
    return d


def e_agujero():
    p = RAMPAS['negro']
    d = nuevo()
    ps = RAMPAS['hueso']
    anillo_el = resta(elip(12, 12, 11.2, 5.0, -0.45), elip(12, 12, 6.8, 2.2, -0.45))
    d.pintar(anillo_el, RAMPAS['psychic'], 0.7)
    d.pintar(circ(12, 12, 5.6), p, 0.8)
    d.plano(circ(12, 12, 4.8), p.dark)
    d.pintar(inter(anillo_el, rect(0, 12.4, 24, 24)), ps, 0.7)
    d.plano(anillo(12, 12, 6.2, 0.9), RAMPAS['psychic'].light)
    d.plano(destello(4.4, 4.6, 2.8, 0.36), RAMPAS['hueso'].hi)
    d.plano(destello(19.8, 19.8, 2.8, 0.36), RAMPAS['hueso'].hi)
    return d


def e_centinela():
    d = g_centinela()
    d.pintar(poli([(7.4, 8.4), (7.8, 2.6), (10.4, 5.6), (12, 1.8), (13.6, 5.6), (16.2, 2.6), (16.6, 8.4)]), RAMPAS['oro'], 0.6)
    d.plano(circ(12, 5.8, 1.0), RAMPAS['rojo'].mid)
    return d


def e_aroma():
    d = nuevo()
    g = RAMPAS['grass']
    d.pintar(elip(12, 20.2, 10.4, 2.8), g, 0.8)
    for (x, y, p) in ((6.0, 13.6, RAMPAS['rosa']), (12, 8.8, RAMPAS['rojo']), (18.0, 13.2, RAMPAS['oro'])):
        d.plano(trazo([(x, y + 1), (x, 19)], 1.0), g.dark)
        flor(d, x, y, 1.9, p, RAMPAS['hueso'] if p is not RAMPAS['oro'] else RAMPAS['marron'])
    return d


def e_pokebola():
    d = nuevo()
    r, w = RAMPAS['dragon'], RAMPAS['hueso']
    d.pintar(circ(12, 12, 9.4), w, 1.0)
    d.pintar(inter(circ(12, 12, 9.4), rect(0, 0, 24, 12)), r, 1.0)
    d.plano(rect(2.6, 10.8, 21.4, 13.2), (24, 16, 32))
    d.pintar(circ(12, 12, 3.6), w, 0.6)
    d.plano(anillo(12, 12, 3.6, 1.0), (24, 16, 32))
    d.plano(circ(12, 12, 1.5), w.hi)
    d.plano(trazo([(8.2, 9.2), (8.2, 4.6), (12, 7.8), (15.8, 4.6), (15.8, 9.2)], 1.3), RAMPAS['psychic'].light)
    d.esfera(4.8, 6.2, 1.9, RAMPAS['psychic'], brillo=False)
    d.esfera(19.2, 6.2, 1.9, RAMPAS['psychic'], brillo=False)
    return d


def e_burbujas():
    p = RAMPAS['water']
    cl = RAMPAS['cielo']
    d = nuevo()
    d.pintar(circ(11.4, 12.6, 9.8), p, 1.0)
    d.plano(circ(11.4, 12.6, 8.4), cl.light)
    d.plano(arco(11.4, 12.6, 6.8, 195, 280, 1.5), cl.hi)
    d.plano(circ(15.8, 17.4, 1.2), cl.hi)
    d.pintar(estrella(11.4, 12.8, 3.4, 1.5), RAMPAS['oro'], 0.4)
    d.esfera(20.2, 5.2, 2.2, p)
    d.esfera(21, 19.6, 1.7, p)
    return d


def e_latigo():
    p = RAMPAS['grass']
    d = nuevo()
    pts = [(3.4, 21), (6.6, 14.2), (11, 11.2), (16, 11.8), (19.6, 8.2), (19.2, 3.8)]
    ws = [5.0, 4.6, 4.0, 3.4, 2.6, 1.8]
    d.pintar(union(*[cono(pts[i], pts[i + 1], ws[i], ws[i + 1], 14) for i in range(len(pts) - 1)]), p, 0.7)
    r = RAMPAS['rojo']
    for pol in ([(4.4, 15.8), (1.4, 14.4), (4.8, 12.8)], [(8.6, 11.2), (7.6, 7.8), (11.2, 9.2)], [(13.4, 12.8), (14.8, 16.4), (17, 12.6)],
                [(20.6, 10.6), (23, 8), (21.2, 6.2)]):
        d.pintar(poli(pol), r, 0.4)
    d.plano(circ(19.3, 4.0, 0.9), p.hi)
    return d


def e_lunar():
    p = RAMPAS['hueso']
    d = nuevo()
    d.pintar(circ(11.6, 12.4, 9.6), p, 1.0)
    for (x, y, r) in ((8.6, 9.4, 2.0), (15.2, 14.6, 2.6), (9.4, 16.6, 1.4)):
        d.plano(circ(x, y, r), p.dark)
        d.plano(circ(x - 0.3, y - 0.3, r * 0.6), p.mid)
    d.plano(destello(20.2, 18.8, 2.8, 0.36), RAMPAS['oro'].hi)
    return d


EVOLUCIONES = {
    'disparo': e_disparo, 'orbes': e_orbes, 'pulso': e_pulso, 'cadena': e_cadena, 'meteoros': e_meteoros, 'tajo': e_tajo, 'bumeran': e_bumeran,
    'estela': e_estela, 'minas': e_minas, 'rayo': e_rayo, 'lanza': e_lanza, 'hidro': e_hidro, 'solar': e_solar, 'hojas': e_hojas, 'petalos': e_petalos,
    'lodo': e_lodo, 'terremoto': e_terremoto, 'ventisca': e_ventisca, 'torbellino': e_torbellino, 'trueno': e_trueno, 'granizo': e_granizo,
    'cometa': e_cometa, 'garra': e_garra, 'agujero': e_agujero, 'centinela': e_centinela, 'aroma': e_aroma, 'pokebola': e_pokebola,
    'burbujas': e_burbujas, 'latigo': e_latigo, 'lunar': e_lunar,
}

# el ataque con el que empieza cada tipo de Pokemon: (el ataque, la rampa de colores del tipo)
INICIALES = {
    'fire': ('disparo', 'fire'), 'water': ('disparo', 'water'), 'ice': ('disparo', 'ice'), 'ghost': ('disparo', 'ghost'), 'normal': ('disparo', 'normal'),
    'electric': ('cadena', 'electric'), 'grass': ('orbes', 'grass'), 'fighting': ('tajo', 'fighting'), 'bug': ('tajo', 'bug'), 'dragon': ('tajo', 'dragon'),
    'dark': ('tajo', 'dark'), 'poison': ('estela', 'poison'), 'ground': ('pulso', 'ground'), 'psychic': ('pulso', 'psychic'), 'fairy': ('pulso', 'fairy'),
    'flying': ('bumeran', 'flying'), 'steel': ('bumeran', 'steel'), 'rock': ('meteoros', 'rock'),
}
