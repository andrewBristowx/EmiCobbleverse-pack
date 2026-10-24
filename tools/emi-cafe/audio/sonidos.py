#!/usr/bin/env python3
"""Efectos de sonido del Cafe Michialien, sintetizados con la biblioteca de sinte.py (nada de muestras de nadie).

Uso:  python3 sonidos.py
Escribe ../src/main/resources/assets/emi_cafe/sounds/sfx_<nombre>.ogg y ../src/main/resources/assets/emi_cafe/sounds.json.
Los efectos «de mundo» (sonando en un sitio: cobrar, campana, listo, quemado, chisporroteo, lavar) son MONO, para que Minecraft los atenue con la distancia;
los que oye solo un jugador (subir, evento, megusta, abrir, cerrar) son estereo."""
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sinte as S
from sinte import midi

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'emi_cafe')
SALIDA = os.path.join(RAIZ, 'sounds')


def pegar(partes, largo):
    """Mezcla [(t0, señal, vol, pan)] en un buffer estereo de `largo` segundos."""
    m = S.Mezcla(largo)
    for t0, x, vol, pan in partes:
        m.poner(t0, x, vol, pan)
    return m.estereo()


def cobrar():
    return pegar([(0.0, S.v_campana(midi('E6'), 0.5, True, 5.0), 0.8, -0.1), (0.07, S.v_campana(midi('B6'), 0.7, True, 4.5), 0.8, 0.1), (0.0, S.pasaaltos(S.ruido(int(0.02 * S.SR)), 4000) * 0.3, 1.0, 0.0)], 0.9)


def campana():
    return pegar([(0.0, S.v_campana(midi('G6'), 1.0, True, 2.4), 0.9, 0.0), (0.16, S.v_campana(midi('C7'), 1.2, True, 2.2), 0.8, 0.0)], 1.5)


def campanilla():
    """La campanilla que suena sobre la puerta de una tienda cuando entra alguien: tres tintineos agudos y rapidos, el ultimo mas largo."""
    return pegar([(0.0, S.v_campana(midi('G7'), 0.45, True, 6.0), 0.7, 0.0), (0.06, S.v_campana(midi('E7'), 0.5, True, 5.5), 0.75, 0.0), (0.13, S.v_campana(midi('C8'), 0.9, True, 4.2), 0.8, 0.0),
                  (0.0, S.pasaaltos(S.ruido(int(0.012 * S.SR)), 5000) * 0.25, 1.0, 0.0)], 1.3)


def listo():
    return pegar([(0.0, S.v_cajita(midi('C6'), 0.5), 0.9, 0.0), (0.09, S.v_cajita(midi('G6'), 0.7), 0.8, 0.0)], 0.9)


def quemado():
    n = int(0.9 * S.SR)
    t = np.arange(n) / S.SR
    zumbido = S.pasabajos(S.saw(np.linspace(180, 90, n), n), 700, 2) * np.exp(-t * 3.5)
    chisporroteo = S.pasabanda(S.ruido(n), 1500, 6000) * np.exp(-t * 4.0) * (np.random.default_rng(3).random(n) > 0.9) * 2.5
    return pegar([(0.0, S.fundido(zumbido * 0.6 + chisporroteo * 0.4), 1.0, 0.0)], 1.0)


def chisporroteo():
    n = int(1.0 * S.SR)
    t = np.arange(n) / S.SR
    base = S.pasaaltos(S.ruido(n), 2500, 2) * np.exp(-t * 2.2) * 0.4
    pops = (np.random.default_rng(7).random(n) > 0.985) * S.ruido(n) * np.exp(-t * 2.0)
    return pegar([(0.0, S.fundido(S.pasabanda(base + pops, 1800, 9000)), 1.0, 0.0)], 1.1)


def lavar():
    n = int(0.8 * S.SR)
    t = np.arange(n) / S.SR
    agua = S.pasabanda(S.ruido(n), 500, 3500) * np.sin(np.pi * np.minimum(t / 0.8, 1.0)) * 0.7
    burbujas = np.zeros(n)
    rng = np.random.default_rng(5)
    for _ in range(9):
        i = int(rng.uniform(0.05, 0.7) * S.SR)
        f = rng.uniform(500, 1400)
        m = min(int(0.08 * S.SR), n - i)
        burbujas[i:i + m] += np.sin(2 * np.pi * f * (1 + 2 * np.arange(m) / S.SR) * np.arange(m) / S.SR) * np.exp(-np.arange(m) / S.SR * 40) * 0.4
    return pegar([(0.0, S.fundido(agua + burbujas), 1.0, 0.0)], 0.9)


def subir():
    notas = ['C5', 'E5', 'G5', 'C6', 'E6']
    partes = [(0.1 * i, S.v_pulso(midi(nota), 0.35 if i < 4 else 1.0, duty=0.25, a=0.003, d=0.2, s=0.5, r=0.08, brillo=6000), 0.7, -0.3 + 0.15 * i) for i, nota in enumerate(notas)]
    partes += [(0.4, S.v_campana(midi('C7'), 1.2, True, 2.4), 0.6, 0.0)]
    return pegar(partes, 1.9)


def evento():
    return pegar([(0.0, S.v_campana(midi('E5'), 0.8, False, 2.4), 0.8, -0.3), (0.2, S.v_campana(midi('A5'), 0.8, False, 2.4), 0.8, 0.0), (0.4, S.v_campana(midi('C#6'), 1.3, False, 2.0), 0.9, 0.3)], 1.8)


def megusta():
    partes = [(0.07 * i, S.v_cajita(midi(nota), 0.6), 0.8, -0.5 + 0.2 * i) for i, nota in enumerate(['E6', 'G6', 'B6', 'E7'])]
    partes += [(0.35, S.pasabanda(S.ruido(int(0.12 * S.SR)), 900, 2500) * np.exp(-np.arange(int(0.12 * S.SR)) / S.SR * 28) * 0.5, 1.0, 0.0)]
    return pegar(partes, 1.2)


def abrir():
    return pegar([(0.0, S.v_campana(midi('G5'), 1.2, True, 2.0), 0.9, -0.2), (0.14, S.v_campana(midi('D6'), 1.2, True, 2.0), 0.9, 0.0), (0.28, S.v_campana(midi('G6'), 1.6, True, 1.8), 1.0, 0.2)], 2.0)


def cerrar():
    return pegar([(0.0, S.v_campana(midi('G6'), 1.0, True, 2.2), 0.9, 0.2), (0.18, S.v_campana(midi('D6'), 1.0, True, 2.2), 0.8, 0.0), (0.36, S.v_campana(midi('G5'), 1.6, True, 1.8), 0.9, -0.2)], 2.0)


EFECTOS = {'cobrar': (cobrar, True), 'campana': (campana, True), 'campanilla': (campanilla, True), 'listo': (listo, True), 'quemado': (quemado, True), 'chisporroteo': (chisporroteo, True), 'lavar': (lavar, True),
           'subir': (subir, False), 'evento': (evento, False), 'megusta': (megusta, False), 'abrir': (abrir, False), 'cerrar': (cerrar, False)}


def main():
    os.makedirs(SALIDA, exist_ok=True)
    sj = {}
    solo = sys.argv[1:]                                   # python3 sonidos.py campanilla = solo regenera ese (los demas se quedan como estan)
    for nombre, (f, mono) in EFECTOS.items():
        sj['sfx.' + nombre] = {'sounds': [{'name': 'emi_cafe:sfx_' + nombre}]}
        if solo and nombre not in solo: continue
        x = f()
        x = S.normalizar(S.sin_continua(x), 0.12, 0.85)
        x = S.fundido(x, 0.002, 0.03)
        seg, kb = S.a_ogg(x, os.path.join(SALIDA, 'sfx_%s.ogg' % nombre), 4, mono=mono)
        print('  sfx_%s.ogg: %.2f s, %d KB%s' % (nombre, seg, kb, ' (mono)' if mono else ''))
    for pista in ('cerrado', 'abierto'):
        sj['musica.' + pista] = {'sounds': [{'name': 'emi_cafe:musica_' + pista, 'stream': True}]}
    with open(os.path.join(RAIZ, 'sounds.json'), 'w') as f:
        json.dump(sj, f, indent=2)
        f.write('\n')


if __name__ == '__main__':
    main()
