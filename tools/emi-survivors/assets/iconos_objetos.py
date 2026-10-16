"""Los dibujos de los 16 objetos (mejoras pasivas), las 7 mejoras permanentes del menu y las tarjetas sueltas (curar y experiencia)."""
from iconos_base import *


def g_vida(p=None):                                    # Corazon Michi: un corazon con orejas de gato
    p = p or RAMPAS['rojo']
    d = nuevo()
    d.pintar(poli([(3.6, 9.4), (3.6, 2.4), (9.6, 6.2)]), p, 0.7)
    d.pintar(poli([(20.4, 9.4), (20.4, 2.4), (14.4, 6.2)]), p, 0.7)
    d.pintar(corazon(12, 13.4, 19.4), p, 1.0)
    d.plano(circ(8.2, 10.6, 1.7), p.hi)
    d.plano(poli([(5.4, 8), (5.4, 4.8), (8, 6.6)]), RAMPAS['fairy'].light)
    d.plano(poli([(18.6, 8), (18.6, 4.8), (16, 6.6)]), RAMPAS['fairy'].light)
    return d


def g_regen(p=None):                                   # Hierba Curativa
    p = p or RAMPAS['grass']
    d = nuevo()
    d.pintar(trazo([(12, 22.2), (12, 9)], 1.8), RAMPAS['grass'], 0.5)
    d.pintar(hoja(7.0, 13.4, 9.6, 4.6, -35), p, 0.8)
    d.pintar(hoja(17.0, 11.6, 9.6, 4.6, 35), p, 0.8)
    d.pintar(hoja(12, 6.2, 9.0, 4.2, -90), p, 0.8)
    d.plano(rect(18.2, 15.4, 19.6, 21.4), RAMPAS['hueso'].hi)                       # un «+» de curar
    d.plano(rect(15.9, 17.7, 21.9, 19.1), RAMPAS['hueso'].hi)
    return d


def g_vel(p=None):                                     # Zapatos Veloces
    p = p or RAMPAS['rojo']
    d = nuevo()
    h = RAMPAS['hueso']
    d.plano(cono((1.6, 11), (6.8, 11), 0.5, 1.6), h.light)                          # rayas de velocidad
    d.plano(cono((1.0, 15), (6.4, 15), 0.5, 1.6), h.light)
    d.pintar(poli([(6.4, 16.4), (7.2, 6.2), (12.4, 5.4), (13.6, 10.2), (21.6, 14), (21.6, 17.4)]), p, 0.9)
    d.pintar(rect(5.6, 16.6, 22.4, 20.6, 1.6), h, 0.8)
    d.plano(trazo([(9.4, 9.2), (12.6, 11.6)], 0.9), h.hi)
    d.plano(trazo([(9.2, 12.2), (12.4, 14.2)], 0.9), h.hi)
    d.pintar(hoja(6.2, 5.4, 7.4, 3.2, -140), h, 0.5)                               # una alita
    return d


def g_dano(p=None):                                    # Garras Afiladas: una garra curva
    p = p or RAMPAS['hueso']
    d = nuevo()
    d.pintar(arco(18.4, 19.4, 15.6, 188, 288, 6.8, 0.8), p, 0.9)
    d.plano(arco(18.4, 19.4, 15.6, 205, 270, 1.4, 0.6), p.hi)
    d.plano(destello(19.6, 18.6, 3.2, 0.36), RAMPAS['rojo'].mid)
    d.plano(destello(4.8, 6.6, 2.6, 0.36), RAMPAS['hueso'].hi)
    return d


def g_recarga(p=None):                                 # Reloj de Arena
    p = p or RAMPAS['cielo']
    d = nuevo()
    m = RAMPAS['marron']
    oro = RAMPAS['oro']
    d.pintar(union(poli([(6.6, 5), (17.4, 5), (12.6, 11.6)]), poli([(11.4, 12.4), (17.4, 19), (6.6, 19)])), p, 0.7)
    d.plano(poli([(8.8, 6.0), (15.2, 6.0), (12, 9.4)]), oro.mid)
    d.plano(poli([(12, 14.6), (16.0, 18.2), (8.0, 18.2)]), oro.mid)
    d.plano(rect(11.5, 10.6, 12.5, 14.8), oro.light)
    d.pintar(rect(4.6, 2.2, 19.4, 5.2, 0.8), m, 0.7)
    d.pintar(rect(4.6, 18.8, 19.4, 21.8, 0.8), m, 0.7)
    return d


def g_area(p=None):                                    # Lupa Estelar
    p = p or RAMPAS['oro']
    d = nuevo()
    d.pintar(trazo([(16.4, 16.4), (21.4, 21.4)], 3.6), RAMPAS['marron'], 0.8)
    d.plano(circ(10.2, 10.2, 7.6), RAMPAS['cielo'].light)
    d.pintar(anillo(10.2, 10.2, 7.6, 2.2), p, 0.8)
    d.plano(arco(10.2, 10.2, 5.0, 200, 260, 1.2), RAMPAS['cielo'].hi)
    d.pintar(estrella(10.4, 10.8, 3.6, 1.6), RAMPAS['oro'], 0.4)
    return d


def g_iman(p=None):                                    # Iman de Estrellas
    p = p or RAMPAS['rojo']
    d = nuevo()
    herradura = union(rect(3.6, 3.2, 9.4, 11.4), rect(14.6, 3.2, 20.4, 11.4),
                      inter(resta(circ(12, 11.4, 8.4), circ(12, 11.4, 3.0)), rect(0, 11.4, 24, 24)))
    azul = inter(herradura, rect(12, 0, 24, 24))
    d.pintar(herradura, p, 0.9)
    d.pintar(azul, RAMPAS['water'], 0.9)
    d.plano(rect(3.6, 3.2, 9.4, 6.6), RAMPAS['hueso'].light)
    d.plano(rect(14.6, 3.2, 20.4, 6.6), RAMPAS['hueso'].light)
    d.pintar(estrella(12, 5.8, 3.2, 1.4), RAMPAS['oro'], 0.4)
    return d


def g_xp(p=None):                                      # Sombrero de Sabio
    p = p or RAMPAS['dragon']
    d = nuevo()
    d.pintar(union(poli([(11.4, 2.4), (5.6, 17), (18.4, 17)]), poli([(11, 3.6), (15.8, 1.6), (13.6, 6.8)])), p, 0.9)
    d.pintar(elip(12, 17.6, 10.4, 3.0), p, 0.9)
    d.plano(rect(7, 14.2, 17, 16.2), RAMPAS['oro'].mid)
    d.pintar(estrella(11.4, 10, 2.8, 1.2), RAMPAS['oro'], 0.4)
    return d


def g_armadura(p=None):                                # Escudo Lunar
    p = p or RAMPAS['steel']
    d = nuevo()
    escudo = union(rect(3.6, 3, 20.4, 11.6), poli([(3.6, 10.6), (20.4, 10.6), (12, 22.4)]))
    d.pintar(escudo, p, 0.9)
    d.pintar(union(rect(5.6, 5, 18.4, 11.4), poli([(5.6, 10.4), (18.4, 10.4), (12, 19.6)])), RAMPAS['water'], 0.7)
    d.pintar(resta(circ(11.4, 11, 4.6), circ(13.6, 9.8, 4.0)), RAMPAS['oro'], 0.6)
    return d


def g_critico(p=None):                                 # Periscopio (Lente Alcance)
    p = p or RAMPAS['steel']
    d = nuevo()
    d.plano(circ(12, 12, 8.6), RAMPAS['hueso'].light)
    d.pintar(anillo(12, 12, 8.6, 2.4), p, 0.8)
    r = RAMPAS['rojo']
    d.plano(rect(11.3, 4.4, 12.7, 19.6), r.mid)
    d.plano(rect(4.4, 11.3, 19.6, 12.7), r.mid)
    d.plano(circ(12, 12, 2.4), RAMPAS['hueso'].light)
    d.plano(anillo(12, 12, 2.4, 1.1), r.mid)
    d.plano(circ(12, 12, 0.9), r.dark)
    return d


def g_concha(p=None):                                  # Campana Concha
    p = p or RAMPAS['rosa']
    d = nuevo()
    h = RAMPAS['hueso']
    d.pintar(inter(circ(12, 17.6, 10), rect(0, 0, 24, 17.6)), h, 0.9)
    d.plano(poli([(9.4, 17.4), (14.6, 17.4), (12.8, 21.6), (11.2, 21.6)]), h.dark)
    for i in range(1, 5):
        a = math.radians(180 + i * 36)
        d.plano(trazo([(12, 17.6), (12 + math.cos(a) * 9.4, 17.6 + math.sin(a) * 9.4)], 0.9), p.mid)
    d.plano(arco(12, 17.6, 7.0, 192, 348, 0.9), p.light)
    d.pintar(anillo(12, 3.8, 2.3, 1.5), RAMPAS['oro'], 0.5)
    d.pintar(trazo([(12, 6.2), (12, 8.4)], 1.4), RAMPAS['oro'], 0.4)
    return d


def g_focus(p=None):                                   # Banda Focus: una cinta roja anudada
    p = p or RAMPAS['rojo']
    d = nuevo()
    d.pintar(union(cono((18, 12), (21.6, 21.6), 3.2, 1.4), cono((18, 12), (14.6, 21.8), 3.2, 1.4)), p, 0.6)
    d.pintar(rect(1.6, 7.6, 18, 15.6, 1.6), p, 0.9)
    d.esfera(18, 11.6, 3.6, p)
    d.pintar(estrella(8.6, 11.8, 3.2, 1.4), RAMPAS['oro'], 0.4)
    return d


def g_moneda(p=None):                                  # Moneda Amuleto
    p = p or RAMPAS['oro']
    d = nuevo()
    d.pintar(circ(12, 12, 9.6), p, 1.0)
    d.pintar(anillo(12, 12, 7.2, 1.2), RAMPAS['oro'], 0.5)
    d.pintar(resta(circ(11.2, 12, 4.4), circ(13.4, 10.8, 3.8)), RAMPAS['marron'], 0.4)
    d.plano(destello(17.6, 6.4, 3.0, 0.36), p.hi)
    return d


def g_casco(p=None):                                   # Casco Dentado
    p = p or RAMPAS['steel']
    d = nuevo()
    d.pintar(inter(circ(12, 14.8, 9.0), rect(0, 0, 24, 16)), p, 0.9)
    d.pintar(rect(2.6, 14.6, 21.4, 18.6, 1.2), p, 0.8)
    d.plano(rect(7, 15.6, 17, 17.0), RAMPAS['negro'].mid)
    d.plano(rect(6, 18.6, 18, 21.6, 1.0), p.dark)
    h = RAMPAS['hueso']
    d.pintar(poli([(5.8, 8.8), (4.4, 3.6), (9.2, 6.4)]), h, 0.5)
    d.pintar(poli([(9.6, 6.2), (12, 1.4), (14.4, 6.2)]), h, 0.5)
    d.pintar(poli([(14.8, 6.4), (19.6, 3.6), (18.2, 8.8)]), h, 0.5)
    return d


def g_experto(p=None):                                 # Cinta Experto
    p = p or RAMPAS['dragon']
    d = nuevo()
    d.pintar(rect(1.6, 7.6, 22.4, 16.4, 1.4), p, 0.9)
    d.plano(rect(2.4, 9.6, 21.6, 10.4), p.light)
    d.plano(rect(2.4, 13.6, 21.6, 14.4), p.dark)
    d.pintar(rect(8.4, 5.6, 15.6, 18.4, 1.2), RAMPAS['oro'], 0.8)
    d.plano(rect(10.2, 8.4, 13.8, 15.6), p.dark)
    d.plano(destello(19.6, 4.4, 2.8, 0.36), RAMPAS['oro'].hi)
    return d


def g_baya(p=None):                                    # Baya Zidra
    p = p or RAMPAS['electric']
    d = nuevo()
    d.pintar(hoja(17.2, 6.0, 8.6, 3.8, -28), RAMPAS['grass'], 0.6)
    d.pintar(trazo([(12, 7.6), (12.6, 4.4)], 1.4), RAMPAS['marron'], 0.4)
    d.esfera(12, 14.4, 7.8, p)
    d.plano(circ(12, 7.8, 1.3), p.dark)
    return d


# ----------------------------------------------------------------------------------------------------------------------------------------- mejoras permanentes

def g_fuerza(p=None):                                  # una pesa
    p = p or RAMPAS['steel']
    d = nuevo()
    d.pintar(rect(7, 10.4, 17, 13.6, 0.8), RAMPAS['marron'], 0.6)
    for x0, x1, y0, y1 in ((2.4, 6.6, 6.2, 17.8), (17.4, 21.6, 6.2, 17.8)):
        d.pintar(rect(x0, y0, x1, y1, 1.4), RAMPAS['rojo'], 0.8)
    d.pintar(rect(0.6, 8.6, 3, 15.4, 0.8), p, 0.6)
    d.pintar(rect(21, 8.6, 23.4, 15.4, 0.8), p, 0.6)
    return d


def g_agilidad(p=None):                                # un ala
    p = p or RAMPAS['cielo']
    d = nuevo()
    h = RAMPAS['hueso']
    for i, (x, y, l, rot) in enumerate(((13.4, 15.6, 14.0, -62), (11.4, 11.4, 13.6, -52), (10.6, 7.6, 12.0, -42))):
        d.pintar(hoja(x, y, l, 4.6, rot), h if i != 1 else p, 0.7)
    d.pintar(poli([(4.4, 20.4), (7.6, 12.4), (11.4, 15)]), h, 0.6)
    return d


def g_sabiduria(p=None):                               # un libro con una estrella
    p = p or RAMPAS['dragon']
    d = nuevo()
    d.pintar(rect(3.6, 3, 19.4, 20.4, 1.2), p, 0.9)
    d.pintar(rect(5.2, 4.4, 20.4, 21.6, 1.0), RAMPAS['hueso'], 0.6)
    d.pintar(rect(3.6, 3, 7, 20.4, 0.8), p, 0.7)
    d.pintar(estrella(13.2, 12.6, 4.6, 2.0), RAMPAS['oro'], 0.5)
    return d


def g_suerte(p=None):                                  # un trebol de cuatro hojas
    p = p or RAMPAS['grass']
    d = nuevo()
    d.pintar(trazo([(12, 12), (9, 22)], 1.6), p, 0.4)
    for dx, dy in ((-3.6, -3.6), (3.6, -3.6), (-3.6, 3.6), (3.6, 3.6)):
        d.esfera(12 + dx, 11.4 + dy, 4.1, p, brillo=False)
    d.plano(circ(12, 11.4, 1.4), p.dark)
    return d


# ----------------------------------------------------------------------------------------------------------------------------------------- tarjetas sueltas

def g_curar(p=None):                                   # Pastel Michi: un pastelito con cereza
    p = p or RAMPAS['hueso']
    d = nuevo()
    d.pintar(poli([(4.4, 13), (19.6, 13), (17.6, 21.4), (6.4, 21.4)]), RAMPAS['marron'], 0.8)
    d.plano(rect(5.4, 15.6, 18.6, 16.4), RAMPAS['marron'].light)
    d.pintar(union(circ(8, 11.6, 3.8), circ(12, 9.4, 4.8), circ(16, 11.6, 3.8), rect(4.4, 10.4, 19.6, 14, 1.0)), RAMPAS['fairy'], 0.9)
    d.esfera(12, 4.6, 2.4, RAMPAS['rojo'])
    return d


def g_estrella_xp(p=None):                             # Estrella Fugaz
    p = p or RAMPAS['oro']
    d = nuevo()
    d.pintar(estrella(12, 12.6, 10, 4.4), p, 1.0)
    d.plano(destello(4.6, 4.6, 2.8, 0.36), p.hi)
    d.plano(destello(19.8, 4.4, 2.2, 0.36), p.hi)
    d.plano(destello(19.4, 20.2, 2.4, 0.36), p.hi)
    return d


OBJETOS = {
    'vida': g_vida, 'regen': g_regen, 'vel': g_vel, 'dano': g_dano, 'recarga': g_recarga, 'area': g_area, 'iman': g_iman, 'xp': g_xp, 'armadura': g_armadura,
    'critico': g_critico, 'concha': g_concha, 'focus': g_focus, 'moneda': g_moneda, 'casco': g_casco, 'experto': g_experto, 'baya': g_baya,
}

# las mejoras permanentes que reusan el dibujo de un objeto (el id de la mejora -> el id del objeto) y las que tienen el suyo
MEJORAS_USAN = {'vigor': 'vida', 'iman': 'iman', 'recuperacion': 'regen'}
MEJORAS_PROPIAS = {'fuerza': g_fuerza, 'agilidad': g_agilidad, 'sabiduria': g_sabiduria, 'suerte': g_suerte}
OTROS = {'curar': g_curar, 'xp': g_estrella_xp}
