#!/usr/bin/env python3
"""Compone y sintetiza la MUSICA de Emi Fall (todo propio, con numpy; ninguna muestra ni melodia de nadie). Mismo metodo que Emi Survivors (ver
tools/emi-survivors/audio/musica.py): cada cancion se escribe en «compases de 8 casillas» (nombre de nota = empieza, «-» = alarga, «.» = silencio) y antes de
sintetizar se comprueba que los tiempos fuertes son del acorde y que todo es de la escala.

Uso:  python3 musica.py [espera] [carrera] [tension] [fiesta]      (sin argumentos: todas)
Escribe src/main/resources/assets/emi_fall/sounds/musica_<nombre>.ogg y DURACIONES.txt (ticks de cada bucle; Sonido.TICKS de Sonido.java tiene que coincidir).
Cada archivo dura el bucle mas una cola que se solapa con el principio de la vuelta siguiente (el servidor la vuelve a lanzar)."""
import os
import re
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sinte as S
from sinte import midi

AQUI = os.path.dirname(os.path.abspath(__file__))
SOLO_VERIFICAR = '--verificar' in sys.argv
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'emi_fall', 'sounds')

CALIDADES = {'': (0, 4, 7), 'm': (0, 3, 7), '7': (0, 4, 7, 10), 'm7': (0, 3, 7, 10), 'maj7': (0, 4, 7, 11)}


def partes(simbolo):
    m = re.fullmatch(r'([A-G][#b]?)(.*)', simbolo)
    if not m or m.group(2) not in CALIDADES:
        raise ValueError('acorde mal escrito: ' + simbolo)
    return S._PC[m.group(1)], CALIDADES[m.group(2)]


def acorde_midi(simbolo, octava):
    raiz, iv = partes(simbolo)
    base = 12 * (octava + 1) + raiz
    return [base + i for i in iv]


def pcs_acorde(simbolo):
    raiz, iv = partes(simbolo)
    return {(raiz + i) % 12 for i in iv}


def raiz_grave(simbolo, minimo=36):
    raiz, _ = partes(simbolo)
    return minimo + (raiz - minimo) % 12


ESCALAS = {'mayor': (0, 2, 4, 5, 7, 9, 11), 'menor': (0, 2, 3, 5, 7, 8, 10)}


def pcs_escala(tonica, modo, extra=()):
    t = S._PC[tonica]
    return {(t + i) % 12 for i in ESCALAS[modo]} | set(extra)


def leer_compas(txt):
    toks = txt.split()
    n = len(toks)
    if n not in (8, 16):
        raise ValueError('un compas tiene 8 o 16 casillas, y este tiene %d: %s' % (n, txt))
    notas, i = [], 0
    while i < n:
        t = toks[i]
        if t in ('-', '.'):
            i += 1
            continue
        j = i + 1
        while j < n and toks[j] == '-':
            j += 1
        notas.append((i, j - i, midi(t)))
        i = j
    return n, notas


def verificar(nombre, melodia, progresion, escala, permitidos=()):
    avisos = 0
    for b, txt in enumerate(melodia):
        simb = progresion[b % len(progresion)]
        n, notas = leer_compas(txt)
        for (i, largo, m) in notas:
            pos = i * 8.0 / n
            if pos in (0.0, 2.0, 4.0, 6.0) and (m % 12) not in pcs_acorde(simb) and (m % 12) not in permitidos:
                print('  [%s] compas %d (%s): la nota %d (%s) cae en tiempo fuerte y no es del acorde' % (nombre, b + 1, simb, m, txt.split()[i]))
                avisos += 1
            if (m % 12) not in escala:
                print('  [%s] compas %d (%s): la nota %s no es de la escala' % (nombre, b + 1, simb, txt.split()[i]))
                avisos += 1
    return avisos


def potencia_activa(x):
    m = x.mean(axis=1) if x.ndim == 2 else x
    n = len(m) // 2048
    if n == 0:
        return 1e-12
    p = (m[:n * 2048].reshape(n, 2048) ** 2).mean(axis=1)
    activos = p[p > p.max() * 0.04]
    return float(activos.mean()) if len(activos) else 1e-12


class Cancion:
    def __init__(self, nombre, bpm, compases, cola=3.0):
        self.nombre, self.bpm, self.compases = nombre, bpm, compases
        self.beat = 60.0 / bpm
        self.compas = 4 * self.beat
        self.largo = compases * self.compas
        ticks = self.largo * 20.0
        if abs(ticks - round(ticks)) > 1e-6:
            raise ValueError('%s: %d compases a %d bpm duran %.4f s = %.3f ticks (tiene que ser entero)' % (nombre, compases, bpm, self.largo, ticks))
        self.ticks = int(round(ticks))
        self.cola = cola
        self.partes = {}

    def parte(self, nombre):
        if nombre not in self.partes:
            self.partes[nombre] = S.Mezcla(self.largo + self.cola)
        return self.partes[nombre]

    def poner(self, parte, compas, t, x, vol=1.0, pan=0.0):
        self.parte(parte).poner(compas * self.compas + t, x, vol, pan)

    def casilla(self, n=8):
        return self.compas / n

    def melodia(self, parte, compas, txt, voz, vol=1.0, pan=0.0, gate=0.93, transp=0):
        n, notas = leer_compas(txt)
        cs = self.compas / n
        for (i, largo, m) in notas:
            self.poner(parte, compas, i * cs, voz(m + transp, largo * cs * gate), vol, pan)

    def mezclar(self, pesos_db, eco=None, reverb=None, rms=0.13):
        ref = next(iter(pesos_db))
        p_ref = potencia_activa(self.partes[ref].estereo())
        n = self.partes[ref].n
        total = np.zeros((n, 2))
        for nombre, db in pesos_db.items():
            if nombre not in self.partes:
                continue
            est = self.partes[nombre].estereo()
            g = np.sqrt(p_ref / potencia_activa(est)) * 10 ** (db / 20.0)
            total += est * g
        if eco:
            total = S.eco(total, **eco)
        if reverb:
            total = S.reverb(total, **reverb)
        total = S.normalizar(S.sin_continua(total), rms)
        f_in, f_out = int(0.004 * S.SR), int(0.15 * S.SR)
        total[:f_in] *= np.linspace(0, 1, f_in)[:, None]
        total[-f_out:] *= np.linspace(1, 0, f_out)[:, None]
        return total


def guardar(c, total, calidad=3):
    ruta = os.path.join(SALIDA, 'musica_%s.ogg' % c.nombre)
    seg, kb = S.a_ogg(total, ruta, calidad)
    print('  musica_%s.ogg: %.1f s en total (bucle de %d ticks = %.2f s), %d KB' % (c.nombre, seg, c.ticks, c.ticks / 20.0, kb))
    return c.ticks


def arpegio_notas(tonos, patron):
    return [tonos[i] if i < len(tonos) else tonos[i - len(tonos)] + 12 for i in patron]


# ----------------------------------------------------------------------------------------------------------------------------------------- ESPERA (la sala, las explicaciones y los resultados)
# la menor, 100 bpm, 16 compases = 38,4 s = 768 ticks: caja de musica con aire de Halloween simpatico

PROG_ESPERA = ['Am', 'Am', 'F', 'E7', 'Am', 'Dm', 'E7', 'Am', 'F', 'C', 'Am', 'E7', 'Dm', 'Am', 'E7', 'Am']
MEL_ESPERA = [
    'A4 - C5 - E5 - C5 A4', 'E5 - C5 D5 C5 - A4 B4', 'A4 - C5 - F5 - C5 A4', 'B4 - G#4 - E5 - D5 B4',
    'A4 - C5 E5 A5 - E5 C5', 'D5 - F5 - A5 - F5 D5', 'G#5 - E5 - B4 - D5 E5', 'C5 - A4 - E5 - A4 -',
    'F5 - A5 - C6 - A5 F5', 'E5 - G5 - C6 - G5 E5', 'E5 - A5 - C6 - A5 E5', 'G#5 - B5 - E6 - D6 B5',
    'F5 - A5 - D6 - A5 F5', 'C6 - A5 - E5 - A5 C6', 'B5 - G#5 - E5 - G#5 B5', 'A5 - - - E5 - A4 -',
]


def espera():
    c = Cancion('espera', 100, 16)
    escala = pcs_escala('A', 'menor', extra=(8,))
    avisos = verificar('espera', MEL_ESPERA, PROG_ESPERA, escala)
    if SOLO_VERIFICAR:
        return avisos
    cs = c.casilla(8)
    rng = np.random.default_rng(11)
    for k in range(16):
        simb = PROG_ESPERA[k]
        tonos3 = acorde_midi(simb, 3)
        c.melodia('lead', k, MEL_ESPERA[k], lambda m, d: S.v_cajita(m, 1.0), 1.0, 0.0, gate=1.0)
        if k >= 8:
            c.melodia('lead2', k, MEL_ESPERA[k], lambda m, d: S.v_campana(m + 12, 1.2, brillante=False, decay=2.4), 1.0, 0.35, gate=1.0)
        pat = arpegio_notas(tonos3, [0, 1, 2, 3, 2, 1, 2, 1])
        for slot, nota in enumerate(pat):
            c.poner('arp', k, slot * cs, S.v_pizz(nota + 12, 0.45, brillo=1800), 0.8 if slot % 2 == 0 else 0.5, -0.3 if slot % 2 else 0.3)
        for j, nota in enumerate(tonos3):
            c.poner('pad', k, 0.0, S.v_pad(nota, c.compas, a=0.5, r=0.7, brillo=1000, abre=0.3), 1.0, (-0.6, 0.0, 0.6)[j % 3])
        r = raiz_grave(simb, 36)
        c.poner('bajo', k, 0.0, S.v_tri(r, cs * 3.2, a=0.01, d=0.4, s=0.6), 1.0)
        c.poner('bajo', k, 4 * cs, S.v_tri(r + 7, cs * 2.8, a=0.01, d=0.4, s=0.5), 0.75)
        if k % 2 == 0:
            c.poner('bombo', k, 0.0, S.bombo(0.3, 50.0, 110.0), 0.7)
        c.poner('madera', k, 2 * cs, S.madera(0.07, 900.0), 0.6, 0.4)
        c.poner('madera', k, 6 * cs, S.madera(0.07, 700.0), 0.5, -0.4)
        if k in (3, 7, 11, 15):        # un «uuuh» de fantasma al final de cada frase
            c.poner('fantasma', k, 3 * cs, S.v_fantasma(midi('E4'), 2.2, a=0.5, r=0.9, prof=0.5, desde=midi('A3')), 1.0, rng.uniform(-0.6, 0.6))
        for _ in range(2):
            c.poner('destellos', k, rng.uniform(0, c.compas * 0.9), S.v_campana(int(rng.choice([midi('E6'), midi('A6'), midi('C7'), midi('B6')])), 1.0, decay=4.0), rng.uniform(0.5, 1.0), rng.uniform(-0.8, 0.8))
    pesos = {'lead': 0.0, 'lead2': -10.0, 'arp': -8.5, 'pad': -8.0, 'bajo': -4.5, 'bombo': -11.0, 'madera': -17.0, 'fantasma': -12.0, 'destellos': -12.0}
    total = c.mezclar(pesos, eco=dict(retraso=0.6, retroalim=0.5, mezcla=0.3, rebotes=4, filtro=3200.0), reverb=dict(mezcla=0.34, segundos=3.0, oscuridad=0.9), rms=0.105)
    return guardar(c, total)


# ----------------------------------------------------------------------------------------------------------------------------------------- CARRERA (rapida, para las rondas de correr)
# re menor, 150 bpm, 16 compases = 25,6 s = 512 ticks: chiptune de persecucion

PROG_CARRERA = ['Dm', 'Dm', 'Bb', 'C', 'Dm', 'Gm', 'A7', 'A7', 'Bb', 'F', 'Gm', 'A7', 'Dm', 'Bb', 'A7', 'Dm']
MEL_CARRERA = [
    'D5 - F5 A5 D6 - A5 F5', 'F5 - A5 - D6 - F6 D6', 'D6 - Bb5 - F5 - Bb5 D6', 'E6 - C6 - G5 - C6 E6',
    'D6 - A5 - F5 A5 D6 -', 'G5 - Bb5 - D6 - Bb5 G5', 'A5 - C#6 - E6 - G6 E6', 'A5 - E6 - C#6 - A5 -',
    'Bb5 - D6 - F6 - D6 Bb5', 'A5 - C6 - F6 - C6 A5', 'Bb5 - D6 - G6 - D6 Bb5', 'C#6 - E6 - A6 - E6 C#6',
    'D6 - F6 - A6 - F6 D6', 'Bb5 - D6 - F6 - D6 F6', 'E6 - C#6 - A5 - E5 G5', 'D5 - - - A4 - D5 -',
]


def carrera():
    c = Cancion('carrera', 150, 16)
    escala = pcs_escala('D', 'menor', extra=(1,))
    if SOLO_VERIFICAR:
        return verificar('carrera', MEL_CARRERA, PROG_CARRERA, escala)
    cs8 = c.casilla(8)
    cs16 = c.casilla(16)
    for k in range(16):
        simb = PROG_CARRERA[k]
        tonos4 = acorde_midi(simb, 4)
        c.melodia('lead', k, MEL_CARRERA[k], lambda m, d: S.v_pulso(m, d, 0.25, a=0.003, d=0.12, s=0.6, r=0.03, vib=0.12, brillo=7000), 1.0, 0.0, gate=0.9)
        if k >= 8:
            c.melodia('lead2', k, MEL_CARRERA[k], lambda m, d: S.v_pulso(m - 12, d, 0.5, a=0.003, d=0.1, s=0.5, r=0.03, brillo=4000), 1.0, 0.3, gate=0.85)
        pat = arpegio_notas(tonos4, [0, 1, 2, 1] * 4)
        for slot, nota in enumerate(pat):
            c.poner('arp', k, slot * cs16, S.v_pulso(nota, cs16 * 0.8, 0.125, a=0.002, d=0.05, s=0.3, r=0.01, brillo=5000), 0.9 if slot % 4 == 0 else 0.55, -0.4 if slot % 2 else 0.4)
        r = raiz_grave(simb, 33)
        for slot in range(8):
            c.poner('bajo', k, slot * cs8, S.v_bajo_saw(r + (12 if slot % 2 else 0), cs8 * 0.85, brillo=800), 1.0 if slot % 2 == 0 else 0.7)
        for beat in range(4):
            c.poner('bombo', k, beat * 2 * cs8, S.bombo(0.25, 52.0, 140.0), 1.0)
        for beat in (1, 3):
            c.poner('caja', k, beat * 2 * cs8, S.caja(0.18, 200.0, 0.55), 0.9)
        for slot in range(8):
            c.poner('hat', k, slot * cs8 + cs8 * 0.5, S.hihat(False), 0.5)
        if k % 4 == 3:
            c.poner('tom', k, 6 * cs8, S.tom(0.3, 140.0, 80.0), 0.9)
            c.poner('tom', k, 7 * cs8, S.tom(0.3, 110.0, 62.0), 0.9)
    pesos = {'lead': 0.0, 'lead2': -9.0, 'arp': -10.5, 'bajo': -4.0, 'bombo': -6.0, 'caja': -9.0, 'hat': -17.0, 'tom': -11.0}
    total = c.mezclar(pesos, eco=dict(retraso=0.2, retroalim=0.3, mezcla=0.18, rebotes=3, filtro=3500.0), reverb=dict(mezcla=0.16, segundos=1.6, oscuridad=0.8), rms=0.115)
    return guardar(c, total)


# ----------------------------------------------------------------------------------------------------------------------------------------- TENSION (cristales, muros, bombas)
# mi menor, 120 bpm, 16 compases = 32 s = 640 ticks: latido de contrabajo y notas sueltas

PROG_TENSION = ['Em', 'Em', 'C', 'B7', 'Em', 'Am', 'B7', 'Em', 'C', 'D', 'Em', 'B7', 'Am', 'Em', 'B7', 'Em']
MEL_TENSION = [
    'E5 - G5 - B5 - G5 E5', 'B4 - E5 - G5 - E5 D#5', 'E5 - G5 - C6 - G5 E5', 'D#5 - F#5 - B5 - A5 F#5',
    'G5 - B5 - E6 - B5 G5', 'A5 - C6 - E6 - C6 A5', 'F#5 - A5 - D#6 - B5 F#5', 'E5 - - - B4 - E5 -',
    'G5 - E5 - C5 - E5 G5', 'A5 - F#5 - D5 - F#5 A5', 'B5 - G5 - E5 - G5 B5', 'A5 - F#5 - D#5 - F#5 A5',
    'C6 - A5 - E5 - A5 C6', 'B5 - G5 - E5 - G5 E5', 'D#6 - B5 - F#5 - B5 D#6', 'E6 - - - B5 - E5 -',
]


def tension():
    c = Cancion('tension', 120, 16)
    escala = pcs_escala('E', 'menor', extra=(3, 2, 6))        # re sostenido (armonica), re natural y fa sostenido
    if SOLO_VERIFICAR:
        return verificar('tension', MEL_TENSION, PROG_TENSION, escala)
    cs = c.casilla(8)
    rng = np.random.default_rng(23)
    for k in range(16):
        simb = PROG_TENSION[k]
        tonos3 = acorde_midi(simb, 3)
        c.melodia('lead', k, MEL_TENSION[k], lambda m, d: S.v_clave(m, 0.45), 1.0, 0.0, gate=1.0)
        if k >= 8:
            c.melodia('lead2', k, MEL_TENSION[k], lambda m, d: S.v_fantasma(m - 12, d * 1.2, a=0.1, r=0.3, prof=0.35), 1.0, -0.3, gate=1.0)
        for j, nota in enumerate(tonos3):
            c.poner('pad', k, 0.0, S.v_pad(nota, c.compas, a=0.6, r=0.7, brillo=900, abre=0.6), 1.0, (-0.7, 0.0, 0.7)[j % 3])
        r = raiz_grave(simb, 33)
        # latido: dos pulsos graves y un hueco
        c.poner('bajo', k, 0.0, S.v_pizz(r, 0.5, brillo=900), 1.0)
        c.poner('bajo', k, 1 * cs, S.v_pizz(r, 0.4, brillo=900), 0.7)
        c.poner('bajo', k, 4 * cs, S.v_pizz(r, 0.5, brillo=900), 0.9)
        c.poner('bajo', k, 5 * cs, S.v_pizz(r, 0.4, brillo=900), 0.6)
        c.poner('bombo', k, 0.0, S.bombo(0.35, 46.0, 120.0), 0.9)
        c.poner('bombo', k, 4 * cs, S.bombo(0.35, 46.0, 120.0), 0.7)
        for slot in (2, 6):
            c.poner('madera', k, slot * cs, S.madera(0.06, 1200.0), 0.5, 0.5 if slot == 2 else -0.5)
        if k % 4 == 3:
            c.poner('platillo', k, 0.0, S.platillo(1.6), 0.5)
        if k % 2 == 1:
            c.poner('fantasma', k, rng.uniform(0, 3 * cs), S.v_fantasma(midi('B3'), 2.0, a=0.6, r=0.8, prof=0.6, desde=midi('E3')), 1.0, rng.uniform(-0.8, 0.8))
    pesos = {'lead': 0.0, 'lead2': -9.0, 'pad': -8.5, 'bajo': -3.5, 'bombo': -8.0, 'madera': -16.0, 'platillo': -22.0, 'fantasma': -12.0}
    total = c.mezclar(pesos, eco=dict(retraso=0.375, retroalim=0.45, mezcla=0.28, rebotes=4, filtro=3000.0), reverb=dict(mezcla=0.36, segundos=3.2, oscuridad=1.0), rms=0.105)
    return guardar(c, total)


# ----------------------------------------------------------------------------------------------------------------------------------------- FIESTA (caramelos, pilla-pilla, suelo embrujado)
# do mayor, 128 bpm, 16 compases = 30 s = 600 ticks: «oom-pah» saltarin

PROG_FIESTA = ['C', 'G', 'Am', 'F', 'C', 'G', 'F', 'G', 'C', 'Am', 'Dm', 'G', 'C', 'F', 'G', 'C']
MEL_FIESTA = [
    'C5 - E5 G5 C6 - G5 E5', 'B4 - D5 - G5 - D5 B4', 'C5 - E5 - A5 - E5 C5', 'A4 - C5 - F5 - A5 C6',
    'E5 - G5 - C6 - E6 C6', 'D6 - B5 - G5 - B5 D6', 'C6 - A5 - F5 - A5 C6', 'D5 - G5 - B5 - D6 -',
    'G5 - E5 - C5 - E5 G5', 'A5 - E5 - C5 - E5 A5', 'F5 - A5 - D6 - A5 F5', 'G5 - B5 - D6 - B5 G5',
    'C6 - E6 - G6 - E6 C6', 'A5 - C6 - F6 - C6 A5', 'B5 - D6 - G6 - D6 B5', 'C6 - - - G5 - C5 -',
]


def fiesta():
    c = Cancion('fiesta', 128, 16)
    escala = pcs_escala('C', 'mayor')
    if SOLO_VERIFICAR:
        return verificar('fiesta', MEL_FIESTA, PROG_FIESTA, escala)
    cs = c.casilla(8)
    for k in range(16):
        simb = PROG_FIESTA[k]
        tonos3 = acorde_midi(simb, 3)
        tonos4 = acorde_midi(simb, 4)
        c.melodia('lead', k, MEL_FIESTA[k], lambda m, d: S.v_pulso(m, d, 0.25, a=0.003, d=0.1, s=0.55, r=0.03, vib=0.1, brillo=6500), 1.0, 0.0, gate=0.88)
        if k >= 8:
            c.melodia('lead2', k, MEL_FIESTA[k], lambda m, d: S.v_campana(m + 12, 0.9, brillante=True, decay=3.0), 1.0, 0.3, gate=1.0)
        # oom-pah: bajo en 1 y 3, acorde en 2 y 4
        r = raiz_grave(simb, 36)
        for slot, nota in ((0, r), (4, r + 7)):
            c.poner('bajo', k, slot * cs, S.v_pizz(nota, 0.4, brillo=1500), 1.0)
        for slot in (2, 6):
            for nota in tonos4:
                c.poner('pah', k, slot * cs, S.v_pulso(nota, cs * 0.7, 0.5, a=0.002, d=0.05, s=0.3, r=0.02, brillo=3500), 0.6, 0.2 if slot == 2 else -0.2)
        c.poner('bombo', k, 0.0, S.bombo(0.25, 50.0, 130.0), 0.9)
        c.poner('bombo', k, 4 * cs, S.bombo(0.25, 50.0, 130.0), 0.8)
        for slot in (2, 6):
            c.poner('caja', k, slot * cs, S.caja(0.15, 210.0, 0.5), 0.8)
        for slot in range(8):
            c.poner('maraca', k, slot * cs + cs * 0.5, S.maraca(), 0.5, 0.3 if slot % 2 else -0.3)
    pesos = {'lead': 0.0, 'lead2': -10.0, 'bajo': -4.0, 'pah': -10.0, 'bombo': -7.0, 'caja': -9.5, 'maraca': -20.0}
    total = c.mezclar(pesos, eco=dict(retraso=0.234375, retroalim=0.3, mezcla=0.18, rebotes=3, filtro=3500.0), reverb=dict(mezcla=0.18, segundos=1.8, oscuridad=0.8), rms=0.11)
    return guardar(c, total)


PISTAS = {'espera': espera, 'carrera': carrera, 'tension': tension, 'fiesta': fiesta}


def main():
    pedidas = [a for a in sys.argv[1:] if not a.startswith('--')] or list(PISTAS)
    duraciones = {}
    ruta_dur = os.path.join(AQUI, 'DURACIONES.txt')
    if os.path.exists(ruta_dur):
        for linea in open(ruta_dur, encoding='utf-8'):
            if '=' in linea:
                k, v = linea.split('=')
                duraciones[k.strip()] = int(v)
    for nombre in pedidas:
        print('== ' + nombre)
        r = PISTAS[nombre]()
        if not SOLO_VERIFICAR:
            duraciones[nombre] = r
        elif r:
            print('  %d avisos' % r)
    if SOLO_VERIFICAR:
        return
    with open(ruta_dur, 'w', encoding='utf-8') as f:
        f.write('# duracion de cada bucle de musica en ticks (Sonido.TICKS de Sonido.java tiene que coincidir)\n')
        for k in sorted(duraciones):
            f.write('%s=%d\n' % (k, duraciones[k]))


if __name__ == '__main__':
    main()
