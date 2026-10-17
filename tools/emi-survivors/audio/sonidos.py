#!/usr/bin/env python3
"""Sintetiza los EFECTOS DE SONIDO de Emi Survivors (todo propio, con numpy; ninguna muestra de nadie).

Uso:  python3 sonidos.py [nombre ...]         (sin argumentos: todos)
Escribe src/main/resources/assets/emi_survivors/sounds/sfx_<nombre>.ogg y regenera sounds.json con la musica y los efectos.

Todos se guardan en ESTEREO (aunque el sonido sea mono): el motor de sonido de Minecraft baja unos 4 dB a los archivos mono y asi todos suenan igual de fuerte; ningun efecto
es posicional (el servidor se los manda a cada jugador, que siempre los oye a su volumen). Cada efecto se normaliza a un volumen medio (RMS_OBJ) con un tope de pico distinto."""
import json
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sinte as S
from sinte import midi, hz

AQUI = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'emi_survivors')
SALIDA = os.path.join(RES, 'sounds')
SR = S.SR
rng = np.random.default_rng(1016)


def t_(dur):
    n = int(dur * SR)
    return np.arange(n) / SR


def mezcla(largo, *capas):
    """Suma capas [(t0, x, vol)] en un buffer mono de `largo` segundos."""
    out = np.zeros(int(largo * SR))
    for t0, x, vol in capas:
        i = int(t0 * SR)
        if i >= len(out):
            continue
        x = np.asarray(x)[:len(out) - i]
        out[i:i + len(x)] += x * vol
    return out


def barrido(f0, f1, dur, forma='exp'):
    """Frecuencia que va de f0 a f1 en `dur` segundos (exponencial o lineal)."""
    t = t_(dur)
    if forma == 'exp':
        return f0 * (f1 / f0) ** (t / dur)
    return f0 + (f1 - f0) * t / dur


def estereo(mono, ancho=0.0, retraso=0.012):
    """De mono a estereo con un poco de anchura (Haas): el canal derecho llega unos milisegundos despues."""
    n = int(retraso * SR)
    der = np.concatenate([np.zeros(n), mono])[:len(mono)] if ancho else mono
    return np.stack([mono, der * (1.0 - 0.0 * ancho)], 1)


# ----------------------------------------------------------------------------------------------------------------------------------------- partida

def gema():
    n = int(0.14 * SR)
    t = np.arange(n) / SR
    f = 1318.5 * (1.0 - 0.18 * np.exp(-t / 0.012))
    fase = 2 * np.pi * np.cumsum(f) / SR
    return S.fundido((np.sin(fase) * np.exp(-t * 26) + 0.5 * np.sin(2 * fase) * np.exp(-t * 42) + 0.18 * np.sin(3.01 * fase) * np.exp(-t * 60)), 0.0008, 0.02)


def nivel():
    notas = [('C5', 0.0, 0.5), ('E5', 0.075, 0.5), ('G5', 0.15, 0.55), ('C6', 0.225, 0.6), ('E6', 0.32, 1.0)]
    capas = []
    for nom, t0, d in notas:
        m = midi(nom)
        capas.append((t0, S.v_campana(m, 1.1, brillante=False, decay=2.0), 0.55))
        capas.append((t0, S.v_pulso(m, 0.12 if t0 < 0.3 else 0.5, 0.25, a=0.002, d=0.1, s=0.3, brillo=6000), 0.22))
    capas.append((0.32, S.v_campana(midi('E7'), 0.9, decay=3.2), 0.2))
    return estereo(mezcla(1.5, *capas), 1.0)


def carta():
    capas = [(0.0, S.v_cajita(midi('G5'), 0.5), 0.8), (0.075, S.v_cajita(midi('C6'), 0.6), 0.9), (0.0, S.maraca(0.04), 0.4)]
    return estereo(mezcla(0.6, *capas), 1.0)


def evolucion():
    n = int(0.62 * SR)
    t = np.arange(n) / SR
    f = barrido(260, 1900, 0.62)
    fase = 2 * np.pi * np.cumsum(f) / SR
    subida = (np.sin(fase) + 0.4 * np.sin(2 * fase) + 0.2 * np.sin(3 * fase)) * (t / 0.62) ** 1.6
    destello = S.pasaaltos(S.ruido(n), 4000) * (t / 0.62) ** 3 * 0.25
    capas = [(0.0, subida, 0.5), (0.0, destello, 0.5)]
    for nom in ('C5', 'E5', 'G5', 'C6'):
        capas.append((0.56, S.v_campana(midi(nom), 1.7, brillante=True, decay=1.7), 0.42))
    capas.append((0.56, S.platillo(1.2), 0.18))
    capas.append((0.58, S.v_campana(midi('G6'), 1.2, decay=2.6), 0.25))
    capas.append((0.7, S.v_campana(midi('C7'), 1.0, decay=3.0), 0.2))
    return estereo(mezcla(2.4, *capas), 1.0)


def empieza():
    capas = []
    for nom, t0, d in (('C5', 0.0, 0.1), ('E5', 0.15, 0.1), ('G5', 0.30, 0.1), ('C6', 0.45, 0.55)):
        capas.append((t0, S.v_pulso(midi(nom), d, 0.25, a=0.002, d=0.12, s=0.55, r=0.04, brillo=7000), 0.6))
        capas.append((t0, S.v_tri(midi(nom) - 12, d, d=0.2, s=0.7), 0.5))
    capas.append((0.45, S.caja(0.18, 220.0, 0.6), 0.35))
    capas.append((0.45, S.platillo(0.9), 0.14))
    capas.append((0.0, S.bombo(0.2), 0.5))
    return estereo(mezcla(1.2, *capas), 1.0)


def golpe():
    n = int(0.18 * SR)
    t = np.arange(n) / SR
    f = 105 + 150 * np.exp(-t * 38)
    cuerpo = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 20)
    golpe_medio = S.pasabanda(S.ruido(n), 450, 3200) * np.exp(-t * 55) * 1.1
    return S.fundido(cuerpo * 0.9 + golpe_medio, 0.0005, 0.03)


def caer():
    n = int(0.95 * SR)
    t = np.arange(n) / SR
    f = barrido(520, 95, 0.95) * (1.0 + 0.012 * np.sin(2 * np.pi * 7 * t))
    x = S.pasabajos(S.saw(f, n), 1500, 2) * np.exp(-t * 2.6) * (1.0 - 0.25 * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t)))
    golpe_inicial = S.bombo(0.3, 38.0, 110.0)
    return estereo(S.fundido(mezcla(1.0, (0.0, x, 0.6), (0.0, golpe_inicial, 0.8)), 0.001, 0.05), 1.0)


def revivir():
    capas = []
    for nom, t0 in (('E5', 0.0), ('G5', 0.09), ('C6', 0.18), ('E6', 0.30)):
        capas.append((t0, S.v_campana(midi(nom), 1.0, brillante=False, decay=2.6), 0.5))
        capas.append((t0, S.v_tri(midi(nom) - 12, 0.3, d=0.2, s=0.5), 0.3))
    capas.append((0.18, S.v_pad(midi('C4'), 0.4, a=0.1, r=0.4, brillo=2200), 0.18))
    return estereo(mezcla(1.4, *capas), 1.0)


def derrota():
    capas = []
    for nom, t0 in (('A4', 0.0), ('F4', 0.42), ('D4', 0.84), ('A3', 1.3)):
        capas.append((t0, S.v_saw(midi(nom), 0.9 if t0 < 1.2 else 1.6, brillo=1400, a=0.02, d=0.5, s=0.5, r=0.3), 0.32))
        capas.append((t0, S.v_campana(midi(nom) + 12, 1.6, brillante=False, decay=1.4), 0.35))
    capas.append((1.3, S.timbal(1.2, 60.0), 0.45))
    x = mezcla(3.6, *capas)
    est = estereo(x, 1.0)
    return S.reverb(est, mezcla=0.3, segundos=1.8, oscuridad=1.2, cola=0.0)[:len(est)]


def jefe_derrotado():
    capas = []
    # tres golpes de «¡ta-ta-TAAA!» con acorde de do mayor y luego un acorde largo con brillos
    for t0, d, notas in ((0.0, 0.14, ('C4', 'E4', 'G4')), (0.18, 0.14, ('D4', 'F4', 'A4')), (0.36, 1.5, ('E4', 'G4', 'C5', 'E5'))):
        for nom in notas:
            capas.append((t0, S.v_saw(midi(nom), d, brillo=3600, a=0.01, d=0.3, s=0.6, r=0.1), 0.2))
            capas.append((t0, S.v_pulso(midi(nom) + 12, d, 0.25, brillo=5000, a=0.008, d=0.3, s=0.5), 0.1))
    capas.append((0.36, S.timbal(1.4, 80.0), 0.6))
    capas.append((0.36, S.platillo(1.6), 0.28))
    for k, nom in enumerate(('G5', 'C6', 'E6', 'G6', 'C7')):
        capas.append((0.55 + 0.1 * k, S.v_campana(midi(nom), 1.4, decay=2.2), 0.3))
    est = estereo(mezcla(3.2, *capas), 1.0)
    return S.reverb(est, mezcla=0.25, segundos=1.6, cola=0.0)[:len(est)]


# ----------------------------------------------------------------------------------------------------------------------------------------- objetos

def baya():
    n = int(0.1 * SR)
    t = np.arange(n) / SR
    f = 280 + 420 * (t / 0.1) ** 1.2
    glup = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 22) * 0.8
    capas = [(0.0, glup, 0.7), (0.1, S.v_cajita(midi('E6'), 0.5), 0.7), (0.19, S.v_cajita(midi('A6'), 0.6), 0.8)]
    return mezcla(0.9, *capas)


def iman():
    n = int(0.55 * SR)
    t = np.arange(n) / SR
    f = barrido(180, 1500, 0.55)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * (0.55 + 0.45 * np.sin(2 * np.pi * 21 * t)) * np.minimum(t / 0.05, 1.0) * np.exp(-np.maximum(t - 0.35, 0) * 12)
    brillos = [(0.28 + 0.05 * k, S.v_campana(midi(('A6', 'C#7', 'E7', 'A7')[k]), 0.5, decay=4.0), 0.25) for k in range(4)]
    return S.fundido(mezcla(0.9, (0.0, x, 0.6), *brillos), 0.002, 0.03)


def bomba():
    n = int(0.9 * SR)
    t = np.arange(n) / SR
    f = 28 + 90 * np.exp(-t * 14)
    cuerpo = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 4.5)
    fcs = 2600 * np.exp(-t * 7) + 160
    estruendo = S.barrido_pasabajos(S.ruido(n), fcs) * np.exp(-t * 4.2) * 1.4
    chasquido = S.pasaaltos(S.ruido(n), 2000) * np.exp(-t * 90) * 0.5
    medio = S.pasabanda(S.ruido(n), 150, 900) * np.exp(-t * 7.5) * 1.6
    return S.fundido(S.saturar(cuerpo * 0.8 + estruendo + chasquido + medio, 1.4), 0.0005, 0.06)


def caramelo():
    capas = []
    for k, nom in enumerate(('E6', 'G6', 'B6', 'E7', 'G7')):
        capas.append((0.06 * k, S.v_cajita(midi(nom), 0.7), 0.8))
    capas.append((0.0, S.v_campana(midi('E5'), 1.0, brillante=False, decay=2.4), 0.3))
    return estereo(mezcla(1.0, *capas), 1.0)


def focus():
    n = int(0.8 * SR)
    t = np.arange(n) / SR
    metal = S.pasabanda(S.ruido(n), 3800, 9000) * np.exp(-t * 14) * 0.6
    f = barrido(500, 2200, 0.25)
    chirp = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t[:len(f)] * 6) * 0.5
    anillo = (np.sin(2 * np.pi * 523.25 * t) + 0.6 * np.sin(2 * np.pi * 784.0 * t) + 0.3 * np.sin(2 * np.pi * 1318.5 * t)) * np.exp(-t * 4.0) * 0.4
    return S.fundido(mezcla(0.8, (0.0, metal, 0.8), (0.0, chirp, 0.8), (0.12, anillo, 0.7)), 0.001, 0.05)


# ----------------------------------------------------------------------------------------------------------------------------------------- jefes (mono)

def jefe_aparece():
    n = int(2.6 * SR)
    t = np.arange(n) / SR
    f1 = 55.0 * (1.0 + 0.02 * np.sin(2 * np.pi * 0.7 * t))
    f2 = 82.4 * (1.0 + 0.02 * np.sin(2 * np.pi * 0.9 * t))
    dron = (S.saw(f1, n) + 0.8 * S.saw(f2, n) + 0.5 * S.saw(f1 * 2.01, n))
    dron = S.barrido_pasabajos(dron, 250 + 900 * (t / 2.6) ** 1.5) * (np.minimum(t / 1.4, 1.0) ** 1.3) * np.exp(-np.maximum(t - 2.0, 0) * 3.0)
    impacto = bomba()
    rugido = jefe_rugido()
    return S.fundido(mezcla(2.8, (0.0, impacto, 0.9), (0.15, dron, 0.5), (0.7, rugido[:int(1.5 * SR)], 0.35)), 0.001, 0.15)


def jefe_rugido():
    n = int(1.3 * SR)
    t = np.arange(n) / SR
    f = 96.0 * (1.0 + 0.35 * np.exp(-t * 3.0)) * (1.0 + 0.03 * np.sin(2 * np.pi * 6.5 * t))
    x = S.saw(f, n) + 0.6 * S.pulso(f * 0.5, n, 0.3)
    x = S.saturar(x * (0.6 + 0.4 * np.sin(2 * np.pi * 34 * t) ** 2), 3.0)
    # formantes: la voz se «abre» y se «cierra»
    f1 = 450 + 500 * np.sin(np.pi * np.minimum(t / 1.3, 1.0))
    voz = S.barrido_pasabanda(x, f1, 1.2) * 1.4 + S.barrido_pasabanda(x, f1 * 2.3, 1.5) * 0.7
    aire = S.pasabanda(S.ruido(n), 700, 3500) * 0.18
    e = np.minimum(t / 0.06, 1.0) * np.exp(-np.maximum(t - 0.45, 0) * 2.4)
    return S.fundido((voz + aire) * e, 0.002, 0.1)


def jefe_impacto():
    n = int(0.75 * SR)
    t = np.arange(n) / SR
    f = 36 + 120 * np.exp(-t * 20)
    cuerpo = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 6.5)
    estruendo = S.barrido_pasabajos(S.ruido(n), 2200 * np.exp(-t * 9) + 140) * np.exp(-t * 6.5) * 1.3
    grieta = S.pasaaltos(S.ruido(n), 1800) * np.exp(-t * 120) * 0.7
    medio = S.pasabanda(S.ruido(n), 150, 1000) * np.exp(-t * 9) * 1.5
    return S.fundido(S.saturar(cuerpo * 0.8 + estruendo + grieta + medio, 1.5), 0.0005, 0.05)


def jefe_carga():
    n = int(1.0 * SR)
    t = np.arange(n) / SR
    f = barrido(48, 150, 1.0)
    x = S.saw(f, n) + 0.5 * S.saw(f * 1.5, n)
    x = S.saturar(S.pasabajos(x, 1200, 2) * (0.55 + 0.45 * np.sin(2 * np.pi * 22 * t)), 2.0)
    ruido = S.barrido_pasabajos(S.ruido(n), 200 + 2400 * (t / 1.0) ** 2) * (t / 1.0) ** 1.4 * 0.7
    e = np.minimum(t / 0.08, 1.0) * np.exp(-np.maximum(t - 0.8, 0) * 12)
    return S.fundido((x + ruido) * e, 0.003, 0.05)


def jefe_zarpazo():
    n = int(0.5 * SR)
    t = np.arange(n) / SR
    fcs = 700 * (6500 / 700) ** np.minimum(t / 0.2, 1.0)
    silbido = S.barrido_pasabanda(S.ruido(n), fcs, 1.2) * np.exp(-t * 11) * 1.6
    golpe_ = np.sin(2 * np.pi * np.cumsum(70 + 90 * np.exp(-t * 35)) / SR) * np.exp(-t * 20)
    cuchilla = S.pasaaltos(S.ruido(n), 3500) * np.exp(-t * 55) * 0.5
    return S.fundido(silbido + golpe_ * 0.7 + cuchilla, 0.002, 0.05)


def jefe_aviso():
    capas = []
    for k, f in enumerate((660.0, 440.0, 660.0, 440.0)):
        n = int(0.11 * SR)
        t = np.arange(n) / SR
        x = (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * f * 3 * t)) * np.minimum(t / 0.006, 1.0) * np.exp(-np.maximum(t - 0.08, 0) * 90)
        capas.append((k * 0.12, x, 0.8))
    return S.fundido(mezcla(0.6, *capas), 0.001, 0.03)


def jefe_rayo():
    n = int(0.9 * SR)
    t = np.arange(n) / SR
    f = barrido(160, 2400, 0.7)
    cargando = S.saw(np.concatenate([f, np.full(n - len(f), 2400.0)]), n) * (0.6 + 0.4 * np.sin(2 * np.pi * 31 * t))
    cargando = S.pasabajos(cargando, 3500, 2) * np.minimum(t / 0.7, 1.0) ** 1.7 * (t < 0.72)
    chispas = (rng.random(n) < 0.004) * S.ruido(n) * 2.0
    chispas = S.pasaaltos(chispas, 2500) * (t < 0.72)
    zap = S.pasaaltos(S.ruido(n), 1500) * np.exp(-np.maximum(t - 0.72, 0) * 24) * (t >= 0.72) * 0.9
    return S.fundido(cargando * 1.5 + chispas * 0.7 + zap, 0.002, 0.08)


def jefe_invoca():
    capas = []
    for k, nom in enumerate(('E5', 'B4', 'G4', 'E4')):
        capas.append((0.12 * k, S.v_campana(midi(nom), 1.2, brillante=True, decay=2.0), 0.55))
    n = int(1.3 * SR)
    t = np.arange(n) / SR
    dron = S.barrido_pasabajos(S.saw(65.4, n) + S.saw(98.0, n), 160 + 800 * np.sin(np.pi * np.minimum(t / 1.3, 1.0))) * np.sin(np.pi * np.minimum(t / 1.3, 1.0)) * 0.7
    brillo = S.viento(1.3, 0.5, 3200.0, 0.4)
    return S.fundido(mezcla(1.5, (0.0, dron, 0.6), (0.0, brillo, 0.7), *capas), 0.002, 0.15)


# ----------------------------------------------------------------------------------------------------------------------------------------- mareas y ataques (mono)

def marea_cuerno():
    capas = []
    for nom, t0, d in (('F2', 0.0, 0.7), ('C3', 0.72, 1.4)):
        m = midi(nom)
        n = int((d + 0.2) * SR)
        f = vibrato_cuerno(hz(m), n)
        x = S.pasabajos(S.saw(f, n) + 0.5 * S.pulso(f * 2, n, 0.35) + 0.25 * S.saw(f * 3.0, n), 1500, 2)
        capas.append((t0, x * S.env(n, 0.12, 1.0, 1.0, 0.2), 0.7))
    return S.fundido(mezcla(2.4, *capas), 0.01, 0.1)


def vibrato_cuerno(f, n):
    t = np.arange(n) / SR
    return f * (1.0 + 0.006 * np.sin(2 * np.pi * 5.2 * t) * np.minimum(t / 0.5, 1.0))


def marea_estampida():
    n = int(1.6 * SR)
    t = np.arange(n) / SR
    rumor = S.pasabajos(S.ruido(n), 220, 3) * np.minimum(t / 0.3, 1.0) * np.exp(-np.maximum(t - 1.1, 0) * 6) * 4.0
    capas = [(0.0, rumor, 0.8)]
    for k in range(10):
        capas.append((0.04 + 0.145 * k + rng.uniform(-0.01, 0.01), S.tom(0.25, 95.0 - 2 * k, 52.0), 0.25 + 0.04 * (k % 2)))
    return S.fundido(mezcla(1.7, *capas), 0.005, 0.1)


def trueno():
    n = int(1.4 * SR)
    t = np.arange(n) / SR
    grieta = S.pasaaltos(S.ruido(n), 1500) * np.exp(-t * 55) * 1.0
    modulo = 0.65 + 0.35 * np.sin(2 * np.pi * 3.1 * t + 1.0) * np.sin(2 * np.pi * 1.7 * t)
    estruendo = S.barrido_pasabajos(S.ruido(n), 900 * np.exp(-t * 2.2) + 90) * np.exp(-t * 2.4) * modulo * 9.0
    return S.fundido(grieta * 0.45 + estruendo, 0.001, 0.12)


def brillo():
    n = int(0.6 * SR)
    t = np.arange(n) / SR
    f = barrido(700, 2600, 0.45)
    f = np.concatenate([f, np.full(n - len(f), 2600.0)])
    x = (np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.6 * np.sin(2 * np.pi * np.cumsum(f * 1.005) / SR)) * np.minimum(t / 0.04, 1.0) * np.exp(-np.maximum(t - 0.3, 0) * 9)
    return S.fundido(x * 0.6 + S.pasaaltos(S.ruido(n), 5000) * np.exp(-t * 14) * 0.15, 0.002, 0.04)


def temblor():
    n = int(1.0 * SR)
    t = np.arange(n) / SR
    cuerpo = np.sin(2 * np.pi * np.cumsum(30 + 40 * np.exp(-t * 8)) / SR) * np.exp(-t * 3.5)
    rocas = S.pasabajos((rng.random(n) < 0.0025) * S.ruido(n) * 4.0, 900) * np.exp(-t * 2.0)
    estruendo = S.pasabajos(S.ruido(n), 260, 3) * np.exp(-t * 3.0) * 3.0
    medio = S.pasabanda(S.ruido(n), 160, 1100) * np.exp(-t * 4.0) * (0.5 + 0.5 * np.sin(2 * np.pi * 11 * t) ** 2) * 1.3
    return S.fundido(S.saturar(cuerpo * 0.8 + estruendo + rocas + medio, 1.4), 0.001, 0.1)


def explosion_suave():
    n = int(0.55 * SR)
    t = np.arange(n) / SR
    f = 46 + 110 * np.exp(-t * 22)
    cuerpo = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 8)
    estruendo = S.barrido_pasabajos(S.ruido(n), 2400 * np.exp(-t * 12) + 200) * np.exp(-t * 8) * 1.0
    medio = S.pasabanda(S.ruido(n), 150, 900) * np.exp(-t * 12) * 1.4
    return S.fundido(S.saturar(cuerpo * 0.8 + estruendo + medio, 1.3), 0.0005, 0.05)


# ----------------------------------------------------------------------------------------------------------------------------------------- menu (cliente)

def menu_clic():
    n = int(0.07 * SR)
    t = np.arange(n) / SR
    x = (np.sin(2 * np.pi * 980 * t) + 0.4 * np.sin(2 * np.pi * 1960 * t)) * np.exp(-t * 70) + S.pasaaltos(S.ruido(n), 3000) * np.exp(-t * 300) * 0.4
    return S.fundido(x, 0.0005, 0.01)


def menu_comprar():
    capas = [(0.0, S.v_campana(midi('E6'), 0.6, decay=4.0), 0.8), (0.09, S.v_campana(midi('A6'), 0.9, decay=3.0), 0.9), (0.09, S.pasabanda(S.ruido(int(0.2 * SR)), 5000, 9000) * np.exp(-t_(0.2) * 20), 0.25)]
    return estereo(mezcla(1.0, *capas), 1.0)


# volumen medio objetivo de cada efecto (dB respecto al maximo digital); los que no estan aqui van a RMS_BASE. El pico nunca pasa del que dice EFECTOS.
RMS_BASE = -19.0
PICO_MAX = 0.7          # ningun efecto pasa de este pico: varios a la vez (y la musica) no deben saturar la salida
RMS_OBJ = {'gema': -25.0, 'golpe': -22.0, 'menu_clic': -26.0, 'carta': -22.0, 'baya': -22.0, 'jefe_zarpazo': -20.0, 'trueno': -18.0, 'jefe_rayo': -20.0, 'evolucion': -22.0}

# nombre -> (funcion, pico)
EFECTOS = {
    'gema': (gema, 0.50), 'nivel': (nivel, 0.80), 'carta': (carta, 0.75), 'evolucion': (evolucion, 0.85), 'empieza': (empieza, 0.80), 'golpe': (golpe, 0.65), 'caer': (caer, 0.85),
    'revivir': (revivir, 0.8), 'derrota': (derrota, 0.85), 'jefe_derrotado': (jefe_derrotado, 0.85),
    'baya': (baya, 0.7), 'iman': (iman, 0.7), 'bomba': (bomba, 0.9), 'caramelo': (caramelo, 0.7), 'focus': (focus, 0.8),
    'jefe_aparece': (jefe_aparece, 0.9), 'jefe_rugido': (jefe_rugido, 0.9), 'jefe_impacto': (jefe_impacto, 0.9), 'jefe_carga': (jefe_carga, 0.85), 'jefe_zarpazo': (jefe_zarpazo, 0.85),
    'jefe_aviso': (jefe_aviso, 0.8), 'jefe_rayo': (jefe_rayo, 0.8), 'jefe_invoca': (jefe_invoca, 0.8),
    'marea_cuerno': (marea_cuerno, 0.85), 'marea_estampida': (marea_estampida, 0.85),
    'trueno': (trueno, 0.85), 'brillo': (brillo, 0.6), 'temblor': (temblor, 0.85), 'explosion_suave': (explosion_suave, 0.8),
    'menu_clic': (menu_clic, 0.55), 'menu_comprar': (menu_comprar, 0.7),
}

MUSICA = ['sala', 'pradera', 'bosque', 'cosmos', 'jefe']


def escribir_json():
    """sounds.json: un evento por cada archivo (musica.<nombre> con stream=true y sfx.<nombre>), con subtitulos apagados."""
    d = {}
    for nombre in MUSICA:
        d['musica.' + nombre] = {'sounds': [{'name': 'emi_survivors:musica_' + nombre, 'stream': True}]}
    for nombre in EFECTOS:
        d['sfx.' + nombre] = {'sounds': ['emi_survivors:sfx_' + nombre]}
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
        if x.ndim == 1:
            x = np.stack([x, x], 1)             # todos estereo: asi suenan igual de fuerte (el motor de sonido baja unos 4 dB a los mono, y ningun efecto es posicional)
        seg, kb = S.a_ogg(S.fundido(x, 0.001, min(0.03, len(x) / S.SR * 0.2)), os.path.join(SALIDA, 'sfx_%s.ogg' % nombre), calidad=4)
        total_kb += kb
        print('  sfx_%-16s %4.2f s  %3d KB' % (nombre, seg, kb))
    escribir_json()
    print('  %d efectos, %d KB; sounds.json escrito' % (len(pedidos), total_kb))


if __name__ == '__main__':
    main()
