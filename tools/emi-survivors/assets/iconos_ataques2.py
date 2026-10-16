"""Los dibujos de los 20 ataques nuevos de la fase 2B (ver iconos_ataques.py)."""
from iconos_base import *


def g_lanza(p=None):
    p = p or RAMPAS['fire']
    d = nuevo()
    llama = union(circ(12, 15.4, 6.8),
                  poli([(5.8, 13.4), (9.2, 8.4), (12.6, 1.6), (14.4, 7.4), (18.2, 13.4)]),
                  poli([(15.4, 11), (19.8, 6.6), (19.2, 14)]))
    d.pintar(llama, p, 1.0)
    d.plano(union(circ(12, 17, 3.9), poli([(8.6, 15.6), (11.6, 9.6), (15.4, 15.6)])), p.light)
    d.plano(circ(12, 18.2, 2.1), p.hi)
    return d


def g_hidro(p=None):
    p = p or RAMPAS['water']
    d = nuevo()
    d.pintar(gota(12, 12.2, 6.2, 11.4), p, 1.0)
    d.plano(circ(9.6, 10.6, 1.6), p.hi)
    d.plano(trazo([(8.4, 14), (9.2, 16.2)], 1.2), p.hi)
    d.esfera(19.8, 5, 1.8, p)
    d.esfera(4.4, 7.2, 1.5, p)
    d.pintar(resta(elip(12, 20.6, 9.6, 1.9), elip(12, 20.6, 6.2, 0.6)), p, 0.5)
    return d


def g_solar(p=None):
    p = p or RAMPAS['oro']
    d = nuevo()
    rayos = []
    for i in range(8):
        a = math.radians(i * 45 - 90)
        R = 10.6 if i % 2 == 0 else 8.8
        pa, pb = a - math.radians(12), a + math.radians(12)
        rayos.append(poli([(12 + math.cos(pa) * 6.2, 12 + math.sin(pa) * 6.2), (12 + math.cos(a) * R, 12 + math.sin(a) * R),
                           (12 + math.cos(pb) * 6.2, 12 + math.sin(pb) * 6.2)]))
    d.pintar(union(*rayos), p, 0.6)
    d.esfera(12, 12, 5.6, RAMPAS['electric'])
    return d


def g_hojas(p=None):
    p = p or RAMPAS['grass']
    d = nuevo()
    for ang in (-90, 30, 150):
        a = math.radians(ang)
        d.pintar(hoja(12 + math.cos(a) * 5.6, 12 + math.sin(a) * 5.6, 11.4, 4.4, ang), p, 0.8)
        d.plano(trazo([(12 + math.cos(a) * 2.0, 12 + math.sin(a) * 2.0), (12 + math.cos(a) * 9.4, 12 + math.sin(a) * 9.4)], 0.8), p.dark)
    for ang in (-30, 90, 210):
        d.plano(arco(12, 12, 10.2, ang - 22, ang + 22, 1.2), p.light)
    d.esfera(12, 12, 1.8, p, brillo=False)
    return d


def g_petalos(p=None):
    p = p or RAMPAS['rosa']
    d = nuevo()
    for i in range(5):
        a = math.radians(i * 72 - 90)
        d.esfera(12 + math.cos(a) * 4.9, 12 + math.sin(a) * 4.9, 3.6, p, brillo=False)
    d.esfera(12, 12, 2.7, RAMPAS['oro'])
    d.pintar(hoja(19.8, 4.8, 5.0, 2.6, 40), p, 0.5)
    d.pintar(hoja(4.2, 19.4, 4.6, 2.4, 140), p, 0.5)
    return d


def g_lodo(p=None):
    p = p or RAMPAS['marron']
    d = nuevo()
    d.esfera(11, 11, 6.8, p)
    for x, y0, y1, w in ((6.4, 15.2, 20.6, 2.6), (11.4, 17, 22.0, 2.8), (16, 15.2, 19.8, 2.4)):
        d.pintar(cono((x, y0), (x, y1), w, w * 0.9), p, 0.6)
        d.esfera(x, y1, w * 0.6, p, brillo=False)
    d.plano(circ(8.4, 8.2, 1.5), p.hi)
    d.plano(trazo([(15.4, 5.4), (17.6, 3.2), (20, 3.6)], 1.2), RAMPAS['hueso'].dark)
    d.plano(destello(20.6, 3.4, 3.2, 0.36), RAMPAS['electric'].light)
    return d


def g_terremoto(p=None):
    p = p or RAMPAS['ground']
    d = nuevo()
    d.pintar(poli([(1.6, 11.2), (10.4, 10.2), (9.4, 14), (11.6, 17), (9.6, 22.2), (1.6, 22.2)]), p, 0.9)
    d.pintar(poli([(13.0, 14.6), (22.4, 13.2), (22.4, 22.2), (11.2, 22.2), (13.4, 17.4), (11.8, 15.6)]), p, 0.9)
    d.esfera(6.6, 5.6, 1.9, RAMPAS['rock'])
    d.esfera(14.6, 4.4, 2.3, RAMPAS['rock'])
    d.esfera(19.8, 8.4, 1.5, RAMPAS['rock'])
    return d


def g_ventisca(p=None):
    p = p or RAMPAS['ice']
    d = nuevo()
    brazos = []
    for i in range(6):
        a = math.radians(i * 60 - 90)
        ca, sa = math.cos(a), math.sin(a)
        brazos.append(trazo([(12, 12), (12 + ca * 9.4, 12 + sa * 9.4)], 2.0))
        mx, my = 12 + ca * 6.0, 12 + sa * 6.0
        for off in (-52, 52):
            b = a + math.radians(off)
            brazos.append(trazo([(mx, my), (mx + math.cos(b) * 3.4, my + math.sin(b) * 3.4)], 1.5))
    d.pintar(union(*brazos), p, 0.5)
    d.esfera(12, 12, 2.7, p)
    return d


def g_torbellino(p=None):
    p = p or RAMPAS['cielo']
    d = nuevo()
    for i in range(5, -1, -1):
        w = 19 - i * 3.0
        x = 12 + math.sin(i * 0.9) * 2.0
        d.pintar(elip(x, 4.2 + i * 3.4, w / 2, 2.1), p if i % 2 == 0 else RAMPAS['flying'], 0.6)
    d.plano(circ(3.2, 17.4, 0.9), p.hi)
    d.plano(circ(20.6, 8.2, 0.9), p.hi)
    d.plano(circ(19.4, 16.4, 0.8), p.hi)
    return d


def g_trueno(p=None):
    p = p or RAMPAS['electric']
    d = nuevo()
    nube = RAMPAS['steel']
    d.pintar(union(circ(7.4, 9.2, 4.2), circ(12.4, 7.2, 5.4), circ(17.6, 9.6, 4.0), rect(3.6, 9.2, 21.6, 13.4, 2.0)), nube, 0.9)
    d.pintar(poli([(14.4, 11.6), (8.6, 18.2), (12.2, 18.2), (9.8, 22.4), (16.8, 15.4), (13, 15.4), (15.6, 11.6)]), p, 0.8)
    return d


def g_granizo(p=None):
    p = p or RAMPAS['ice']
    d = nuevo()
    for (x, y, r) in ((7.6, 8.8, 3.4), (16.4, 11.4, 4.2), (9.4, 18.2, 3.0)):
        d.pintar(cono((x - r * 0.8, y - r * 0.8), (x - r * 1.9, y - r * 1.9), r * 0.9, 0.3), RAMPAS['hueso'], 0.4)
        d.esfera(x, y, r, p)
    return d


def g_cometa(p=None):
    p = p or RAMPAS['cielo']
    d = nuevo()
    d.pintar(cono((15, 9), (2.4, 21.6), 6.2, 0.4), p, 0.9)
    d.plano(cono((14, 10), (5, 18.5), 2.4, 0.3), p.hi)
    d.pintar(estrella(15.4, 8.8, 6.6, 2.9), RAMPAS['oro'], 0.8)
    d.plano(destello(4.6, 6.4, 2.8, 0.36), RAMPAS['oro'].hi)
    d.plano(destello(20.2, 19.4, 2.8, 0.36), RAMPAS['oro'].hi)
    return d


def g_garra(p=None):
    p = p or RAMPAS['ghost']
    d = nuevo()
    for i in range(3):
        x0, y0, x1, y1 = 8.6 + i * 5.2, 1.8, 3.0 + i * 5.2, 22.2
        mx, my = (x0 + x1) / 2 + 1.8, (y0 + y1) / 2
        d.pintar(union(cono((x0, y0), (mx, my), 0.4, 3.8), cono((mx, my), (x1, y1), 3.8, 0.4)), p, 0.7)
        d.plano(cono((x0 + 0.3, y0 + 2.4), (mx + 0.2, my - 1), 0.3, 1.1), p.hi)
    return d


def g_agujero(p=None):
    p = p or RAMPAS['negro']
    d = nuevo()
    ps = RAMPAS['psychic']
    anillo_el = resta(elip(12, 12, 11.0, 4.8, -0.45), elip(12, 12, 7.0, 2.2, -0.45))
    d.pintar(anillo_el, ps, 0.7)
    d.pintar(circ(12, 12, 5.6), p, 0.8)
    d.plano(circ(12, 12, 4.8), p.dark)
    d.pintar(inter(anillo_el, rect(0, 12.4, 24, 24)), ps, 0.7)
    d.plano(destello(4.4, 4.6, 2.8, 0.36), RAMPAS['hueso'].hi)
    d.plano(destello(19.8, 19.8, 2.8, 0.36), RAMPAS['hueso'].hi)
    return d


def g_centinela(p=None):
    p = p or RAMPAS['hueso']
    d = nuevo()
    rosa = RAMPAS['fairy']
    d.pintar(poli([(4.2, 13.4), (4.8, 4.8), (11.4, 9.4)]), p, 0.8)
    d.pintar(poli([(19.8, 13.4), (19.2, 4.8), (12.6, 9.4)]), p, 0.8)
    d.plano(poli([(6.2, 11), (6.6, 7.2), (9.4, 9.6)]), rosa.light)
    d.plano(poli([(17.8, 11), (17.4, 7.2), (14.6, 9.6)]), rosa.light)
    d.pintar(elip(12, 15.4, 8.6, 6.8), p, 0.9)
    for x in (8.4, 15.6):
        d.plano(elip(x, 15, 1.6, 2.1), (40, 28, 60))
        d.plano(circ(x - 0.4, 14.1, 0.6), (255, 255, 255))
    d.plano(poli([(11, 17.6), (13, 17.6), (12, 18.8)]), rosa.mid)
    d.plano(trazo([(12, 9.2), (12, 5)], 0.9), p.dark)
    d.esfera(12, 3.6, 2.0, RAMPAS['menta'])
    return d


def g_aroma(p=None):
    p = p or RAMPAS['menta']
    d = nuevo()
    for x in (6.6, 12, 17.4):
        d.pintar(trazo([(x + math.sin(i * 0.9) * 1.4, 8.4 - i * 1.35) for i in range(6)], 1.5), p, 0.4)
    f = RAMPAS['rojo']
    for i in range(5):
        a = math.radians(i * 72 - 90)
        d.esfera(12 + math.cos(a) * 3.4, 15.6 + math.sin(a) * 3.4, 3.0, f, brillo=False)
    d.esfera(12, 15.6, 2.0, RAMPAS['oro'])
    return d


def g_pokebola(p=None):
    d = nuevo()
    r, w = RAMPAS['rojo'], RAMPAS['hueso']
    d.pintar(circ(12, 12, 9.4), w, 1.0)
    d.pintar(inter(circ(12, 12, 9.4), rect(0, 0, 24, 12)), r, 1.0)
    d.plano(rect(2.6, 10.8, 21.4, 13.2), (24, 16, 32))
    d.pintar(circ(12, 12, 3.6), w, 0.6)
    d.plano(anillo(12, 12, 3.6, 1.0), (24, 16, 32))
    d.plano(circ(12, 12, 1.5), w.hi)
    return d


def g_burbujas(p=None):
    p = p or RAMPAS['water']
    d = nuevo()
    cl = RAMPAS['cielo']
    for (x, y, r) in ((9.6, 13.6, 6.8), (18.2, 6.4, 3.6), (18.4, 18.4, 2.8)):
        d.pintar(circ(x, y, r), p, 0.9)
        d.plano(circ(x, y, r - 1.3), cl.light)
        d.plano(arco(x, y, r - 2.3, 200, 275, 1.3), cl.hi)
        d.plano(circ(x + r * 0.36, y + r * 0.36, max(0.5, r * 0.12)), cl.hi)
    return d


def g_latigo(p=None):
    p = p or RAMPAS['grass']
    d = nuevo()
    pts = [(3.4, 21), (6.6, 14.2), (11, 11.2), (16, 11.8), (19.6, 8.2), (19.2, 3.8)]
    ws = [4.6, 4.2, 3.6, 3.0, 2.2, 1.6]
    d.pintar(union(*[cono(pts[i], pts[i + 1], ws[i], ws[i + 1], 14) for i in range(len(pts) - 1)]), p, 0.7)
    d.pintar(hoja(9.2, 18.0, 6.4, 3.2, -48), p, 0.5)
    d.plano(poli([(11.4, 9.6), (12.8, 6.6), (13.8, 10.4)]), p.dark)
    d.plano(circ(19.3, 4.0, 0.9), p.hi)
    return d


def g_lunar(p=None):
    p = p or RAMPAS['oro']
    d = nuevo()
    d.pintar(resta(circ(11.4, 12, 9.4), circ(15.6, 9.8, 7.8)), p, 1.0)
    d.plano(destello(18.4, 17.4, 3.6, 0.36), p.hi)
    d.plano(circ(20.4, 7.4, 1.0), p.hi)
    return d


ATAQUES_2 = {
    'lanza': g_lanza, 'hidro': g_hidro, 'solar': g_solar, 'hojas': g_hojas, 'petalos': g_petalos, 'lodo': g_lodo, 'terremoto': g_terremoto,
    'ventisca': g_ventisca, 'torbellino': g_torbellino, 'trueno': g_trueno, 'granizo': g_granizo, 'cometa': g_cometa, 'garra': g_garra,
    'agujero': g_agujero, 'centinela': g_centinela, 'aroma': g_aroma, 'pokebola': g_pokebola, 'burbujas': g_burbujas, 'latigo': g_latigo, 'lunar': g_lunar,
}
