#!/usr/bin/env python3
"""Sintetiza los EFECTOS DE SONIDO de Emi Fall (todo propio, con numpy; ninguna muestra de nadie). Mismo metodo que tools/emi-survivors/audio/sonidos.py.

Uso:  python3 sonidos.py [nombre ...]         (sin argumentos: todos)
Escribe src/main/resources/assets/emi_fall/sounds/sfx_<nombre>.ogg y regenera sounds.json (musica.<nombre> con stream=true y sfx.<nombre>). Todos en estereo."""
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sinte as S
from sinte import midi

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'emi_fall')
SALIDA = os.path.join(RES, 'sounds')
SR = S.SR


def t_(dur):
    return np.arange(int(dur * SR)) / SR


def mezcla(largo, *capas):
    out = np.zeros(int(largo * SR))
    for t0, x, vol in capas:
        i = int(t0 * SR)
        if i >= len(out):
            continue
        x = np.asarray(x)[:len(out) - i]
        out[i:i + len(x)] += x * vol
    return out


def barrido(f0, f1, dur):
    return f0 * (f1 / f0) ** (t_(dur) / dur)


def estereo(mono):
    return np.stack([mono, mono], 1)


def nota(nom, d, tipo='pulso'):
    if tipo == 'pulso':
        return S.v_pulso(midi(nom), d, 0.25, a=0.002, d=0.12, s=0.55, r=0.04, brillo=7000)
    return S.v_cajita(midi(nom), d)


# ----------------------------------------------------------------------------------------------------------------------------------------- efectos

def cuenta():
    return estereo(mezcla(0.3, (0.0, S.v_pulso(midi('A5'), 0.2, 0.5, a=0.002, d=0.1, s=0.5, r=0.05, brillo=6000), 0.8), (0.0, S.v_tri(midi('A4'), 0.2), 0.4)))


def ya():
    capas = []
    for nom in ('C5', 'E5', 'G5', 'C6'):
        capas.append((0.0, nota(nom, 0.6), 0.5))
    capas.append((0.0, S.bombo(0.25), 0.5))
    capas.append((0.0, S.platillo(0.7), 0.15))
    return estereo(mezcla(0.9, *capas))


def recoger():
    capas = [(0.06 * k, S.v_cajita(midi(nm), 0.5), 0.8) for k, nm in enumerate(('E6', 'A6', 'C7'))]
    return estereo(mezcla(0.7, *capas))


def oro():
    capas = [(0.05 * k, S.v_cajita(midi(nm), 0.8), 0.8) for k, nm in enumerate(('C6', 'E6', 'G6', 'C7', 'E7'))]
    capas.append((0.0, S.v_campana(midi('C5'), 1.2, brillante=False, decay=2.0), 0.35))
    capas.append((0.15, S.pasabanda(S.ruido(int(0.4 * SR)), 5000, 9500) * np.exp(-t_(0.4) * 9), 0.25))
    return estereo(mezcla(1.3, *capas))


def trampa():
    n = int(0.45 * SR)
    t = np.arange(n) / SR
    f = barrido(330, 90, 0.45)
    x = S.pasabajos(S.saw(f, n), 1400, 2) * np.exp(-t * 3.0)
    return estereo(S.fundido(x + 0.4 * S.pasabanda(S.ruido(n), 300, 1500) * np.exp(-t * 16), 0.002, 0.05))


def caida():
    n = int(0.9 * SR)
    t = np.arange(n) / SR
    f = barrido(900, 110, 0.9) * (1.0 + 0.01 * np.sin(2 * np.pi * 8 * t))
    x = S.seno(f, n) * np.exp(-t * 2.2) + 0.3 * S.pasabajos(S.tri(f * 1.5, n), 1800, 1) * np.exp(-t * 3.0)
    return estereo(S.fundido(x, 0.002, 0.08))


def golpe():
    n = int(0.2 * SR)
    t = np.arange(n) / SR
    f = 100 + 150 * np.exp(-t * 36)
    cuerpo = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 18)
    return estereo(S.fundido(cuerpo * 0.9 + S.pasabanda(S.ruido(n), 450, 3200) * np.exp(-t * 55), 0.0005, 0.03))


def boo():
    x = mezcla(1.6, (0.0, S.v_fantasma(midi('A3'), 1.4, a=0.25, r=0.6, prof=0.6, desde=midi('E3')), 0.9), (0.0, S.v_fantasma(midi('E4'), 1.4, a=0.3, r=0.6, prof=0.5, desde=midi('B3')), 0.4),
               (0.1, S.viento(1.2, 0.35, 1100), 1.0))
    return estereo(x)


def cristal():
    n = int(0.5 * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    rng = np.random.default_rng(3)
    for k in range(9):
        i = int(rng.uniform(0, 0.22) * SR)
        f = rng.uniform(2500, 7500)
        m = n - i
        tt = np.arange(m) / SR
        x[i:] += np.sin(2 * np.pi * f * tt) * np.exp(-tt * rng.uniform(30, 70)) * rng.uniform(0.3, 0.9)
    x += S.pasaaltos(S.ruido(n), 3500) * np.exp(-t * 28) * 0.6
    return estereo(S.fundido(x, 0.0005, 0.03))


def baldosa():
    n = int(0.35 * SR)
    t = np.arange(n) / SR
    x = S.pasabanda(S.ruido(n), 250, 2200) * np.exp(-t * 14) + 0.5 * np.sin(2 * np.pi * np.cumsum(120 * np.exp(-t * 7) + 50) / SR) * np.exp(-t * 12)
    return estereo(S.fundido(x, 0.0005, 0.04))


def maullido():
    n = int(0.55 * SR)
    t = np.arange(n) / SR
    f0 = 520 + 330 * np.sin(np.pi * np.minimum(t / 0.5, 1.0)) ** 1.2
    f0 = f0 * (1.0 + 0.02 * np.sin(2 * np.pi * 7 * t))
    base = S.saw(f0, n)
    form = S.pasabanda(base, 700, 1300, 2) * 1.3 + S.pasabanda(base, 1800, 3000, 2) * 0.5
    e = np.minimum(t / 0.05, 1.0) * np.exp(-np.maximum(t - 0.3, 0) * 7.0)
    return estereo(S.fundido(form * e, 0.004, 0.05))


def pop():
    n = int(0.25 * SR)
    t = np.arange(n) / SR
    f = 700 * np.exp(-t * 18) + 140
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 20) + S.pasabanda(S.ruido(n), 800, 4000) * np.exp(-t * 60) * 0.7
    return estereo(S.fundido(x, 0.0005, 0.03))


def muro():
    n = int(0.9 * SR)
    t = np.arange(n) / SR
    fcs = 300 * (4000 / 300) ** (np.minimum(t / 0.5, 1.0))
    x = S.barrido_pasabanda(S.ruido(n), fcs, 1.2) * np.sin(np.pi * np.minimum(t / 0.9, 1.0)) ** 1.5
    return estereo(S.fundido(x * 1.4, 0.01, 0.05))


def puntos():
    capas = [(0.09 * k, nota(nm, 0.3), 0.7) for k, nm in enumerate(('C5', 'E5', 'G5'))] + [(0.27, nota('C6', 0.5), 0.8), (0.27, S.v_campana(midi('C6'), 1.0, decay=3.0), 0.4)]
    return estereo(mezcla(1.0, *capas))


def tic():
    return estereo(mezcla(0.12, (0.0, S.madera(0.08, 1500.0), 1.0)))


def reaparece():
    capas = [(0.07 * k, S.v_cajita(midi(nm), 0.5), 0.8) for k, nm in enumerate(('C5', 'G5', 'C6', 'G6'))]
    return estereo(mezcla(0.8, *capas))


def ronda_fin():
    capas = []
    for t0, nm, d in ((0.0, 'G5', 0.14), (0.15, 'G5', 0.14), (0.30, 'G5', 0.14), (0.45, 'C6', 0.8)):
        capas.append((t0, nota(nm, d), 0.7))
        capas.append((t0, S.v_tri(midi(nm) - 12, d), 0.4))
    capas.append((0.45, S.platillo(1.1), 0.12))
    capas.append((0.45, S.caja(0.2, 210.0, 0.6), 0.3))
    return estereo(mezcla(1.4, *capas))


def victoria():
    capas = []
    melodia = (('C5', 0.0, 0.18), ('E5', 0.2, 0.18), ('G5', 0.4, 0.18), ('C6', 0.6, 0.35), ('G5', 1.0, 0.18), ('C6', 1.2, 0.18), ('E6', 1.4, 1.0))
    for nm, t0, d in melodia:
        capas.append((t0, nota(nm, d), 0.7))
        capas.append((t0, S.v_tri(midi(nm) - 12, d), 0.45))
        capas.append((t0, S.v_campana(midi(nm) + 12, 1.0, decay=3.5), 0.25))
    for t0 in (0.0, 0.4, 0.6, 1.0, 1.4):
        capas.append((t0, S.bombo(0.25), 0.5))
    capas.append((1.4, S.platillo(1.6), 0.2))
    return estereo(mezcla(2.8, *capas))


def intro():
    """El golpe que abre cada ronda (cuando sale el titulo con el nombre del juego)."""
    capas = [(0.0, S.timbal(0.8, 80.0), 0.8), (0.0, S.v_pad(midi('A3'), 0.6, a=0.02, r=0.5, brillo=1500), 0.3), (0.12, S.v_campana(midi('E6'), 1.0, decay=3.0), 0.5), (0.0, S.platillo(0.8), 0.1)]
    return estereo(mezcla(1.2, *capas))


RMS_BASE = -19.0
PICO_MAX = 0.7
RMS_OBJ = {'tic': -26.0, 'cuenta': -21.0, 'recoger': -24.0, 'golpe': -22.0, 'pop': -22.0, 'cristal': -22.0, 'maullido': -20.0}

EFECTOS = {
    'cuenta': (cuenta, 0.6), 'ya': (ya, 0.8), 'recoger': (recoger, 0.55), 'oro': (oro, 0.7), 'trampa': (trampa, 0.7), 'caida': (caida, 0.8), 'golpe': (golpe, 0.65),
    'boo': (boo, 0.7), 'cristal': (cristal, 0.7), 'baldosa': (baldosa, 0.7), 'maullido': (maullido, 0.7), 'pop': (pop, 0.7), 'muro': (muro, 0.7), 'puntos': (puntos, 0.7),
    'tic': (tic, 0.5), 'reaparece': (reaparece, 0.6), 'ronda_fin': (ronda_fin, 0.8), 'victoria': (victoria, 0.85), 'intro': (intro, 0.85),
}

MUSICA = ['espera', 'carrera', 'tension', 'fiesta']


def escribir_json():
    d = {}
    for nombre in MUSICA:
        d['musica.' + nombre] = {'sounds': [{'name': 'emi_fall:musica_' + nombre, 'stream': True}]}
    for nombre in EFECTOS:
        d['sfx.' + nombre] = {'sounds': ['emi_fall:sfx_' + nombre]}
    with open(os.path.join(RES, 'sounds.json'), 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=2, ensure_ascii=False)
        f.write('\n')


def main():
    pedidos = [a for a in sys.argv[1:] if not a.startswith('--')] or list(EFECTOS)
    total_kb = 0
    for nombre in pedidos:
        fn, pico = EFECTOS[nombre]
        pico = min(pico, PICO_MAX)
        x = np.asarray(fn(), dtype=float)
        x = S.sin_continua(x, 25.0)
        m = x.mean(axis=1) if x.ndim == 2 else x
        rms = np.sqrt((m ** 2).mean() + 1e-12)
        g = min(10 ** (RMS_OBJ.get(nombre, RMS_BASE) / 20.0) / rms, pico / max(np.abs(x).max(), 1e-9))
        x = x * g
        seg, kb = S.a_ogg(S.fundido(x, 0.001, min(0.03, len(x) / S.SR * 0.2)), os.path.join(SALIDA, 'sfx_%s.ogg' % nombre), calidad=4)
        total_kb += kb
        print('  sfx_%-12s %4.2f s  %3d KB' % (nombre, seg, kb))
    escribir_json()
    print('  %d efectos, %d KB; sounds.json escrito' % (len(pedidos), total_kb))


if __name__ == '__main__':
    main()
