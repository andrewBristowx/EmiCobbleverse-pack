#!/usr/bin/env python3
"""Compone y sintetiza la MUSICA de Emi Survivors (todo propio, chiptune/cajita de musica hecho con numpy; no hay ninguna muestra ni melodia de nadie).

Uso:  python3 musica.py [sala] [pradera] [bosque] [cosmos] [jefe]      (sin argumentos: todas)
Escribe src/main/resources/assets/emi_survivors/sounds/musica_<nombre>.ogg y vuelve a escribir DURACIONES.txt con la duracion de cada bucle en ticks.

Como suenan en el juego: el SERVIDOR las manda con playSoundToPlayer y las vuelve a lanzar cuando pasan `ticks` ticks (el cliente no sabe repetir un sonido que
viene del servidor). Por eso cada archivo dura el bucle (`ticks` * 50 ms exactos) MAS una cola (la reverb y las notas que se apagan) que se solapa con el principio de la
siguiente vuelta: asi el corte del bucle no se nota. Las duraciones de Musica.TICKS (Musica.java) DEBEN ser las de DURACIONES.txt.

Cada cancion se escribe con una notacion de «compases de 8 (o 16) casillas»: cada casilla es una corchea (o semicorchea); un nombre de nota empieza una nota, «-» la alarga
y «.» es un silencio. Antes de sintetizar se comprueba que las notas de los tiempos fuertes son del acorde y que todas son de la escala (si no, avisa)."""
import os
import re
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sinte as S
from sinte import midi

AQUI = os.path.dirname(os.path.abspath(__file__))
SOLO_VERIFICAR = '--verificar' in sys.argv
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'emi_survivors', 'sounds')

# ----------------------------------------------------------------------------------------------------------------------------------------- teoria

CALIDADES = {'': (0, 4, 7), 'm': (0, 3, 7), '7': (0, 4, 7, 10), 'm7': (0, 3, 7, 10), 'maj7': (0, 4, 7, 11), '6': (0, 4, 7, 9), 'm6': (0, 3, 7, 9), 'dim': (0, 3, 6),
             'sus2': (0, 2, 7), 'add9': (0, 4, 7, 14), 'm7b5': (0, 3, 6, 10)}


def partes(simbolo):
    m = re.fullmatch(r'([A-G][#b]?)(.*)', simbolo)
    if not m or m.group(2) not in CALIDADES:
        raise ValueError('acorde mal escrito: ' + simbolo)
    return S._PC[m.group(1)], CALIDADES[m.group(2)]


def acorde_midi(simbolo, octava):
    """Las notas MIDI del acorde con la fundamental en la octava dada (C4 = 60)."""
    raiz, iv = partes(simbolo)
    base = 12 * (octava + 1) + raiz
    return [base + i for i in iv]


def pcs_acorde(simbolo):
    raiz, iv = partes(simbolo)
    return {(raiz + i) % 12 for i in iv}


def raiz_grave(simbolo, minimo=36):
    """La fundamental del acorde en el rango del bajo (minimo..minimo+11)."""
    raiz, _ = partes(simbolo)
    m = minimo + (raiz - minimo) % 12
    return m


ESCALAS = {'mayor': (0, 2, 4, 5, 7, 9, 11), 'menor': (0, 2, 3, 5, 7, 8, 10)}


def pcs_escala(tonica, modo, extra=()):
    """Las clases de nota (0..11, do = 0) de la escala; `extra` son clases de nota sueltas que tambien se permiten (p. ej. el sol sostenido de la menor armonica)."""
    t = S._PC[tonica]
    return {(t + i) % 12 for i in ESCALAS[modo]} | set(extra)


# ----------------------------------------------------------------------------------------------------------------------------------------- las armonias (un acorde por compas, 16 compases que se repiten)

PROG_SALA = ['C', 'G', 'Am', 'Em', 'F', 'C', 'F', 'G', 'C', 'G', 'Am', 'Em', 'F', 'C', 'Dm', 'G']
PROG_PRADERA = ['G', 'D', 'Em', 'C', 'G', 'D', 'C', 'D', 'Em', 'C', 'G', 'D', 'Em', 'C', 'D', 'D']
PROG_BOSQUE = ['Am', 'Am', 'F', 'E7', 'Am', 'Dm', 'E7', 'Am', 'F', 'G', 'Am', 'E7', 'Dm', 'Am', 'E7', 'E7']
PROG_COSMOS = ['Cmaj7', 'Am7', 'Fmaj7', 'G6', 'Cmaj7', 'Em7', 'Fmaj7', 'G6', 'Am7', 'Fmaj7', 'Cmaj7', 'G6', 'Am7', 'Dm7', 'Fmaj7', 'G6']
PROG_JEFE = ['Dm', 'Dm', 'Bb', 'C', 'Dm', 'Gm', 'A7', 'A7', 'Bb', 'F', 'Gm', 'A7', 'Dm', 'Bb', 'A7', 'A7']

# para analizar.py: nombre -> (pulsos por minuto, progresion)
ARMONIA = {'sala': (96, PROG_SALA), 'pradera': (128, PROG_PRADERA), 'bosque': (120, PROG_BOSQUE), 'cosmos': (100, PROG_COSMOS), 'jefe': (150, PROG_JEFE)}


# ----------------------------------------------------------------------------------------------------------------------------------------- notacion

def leer_compas(txt):
    """«G4 - B4 D5 - B4 G5 -» -> (casillas, [(casilla, largo, midi)])."""
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
    """Avisa de las notas de tiempo fuerte que no son del acorde y de las que no son de la escala. Devuelve el numero de avisos."""
    avisos = 0
    for b, txt in enumerate(melodia):
        simb = progresion[b % len(progresion)]
        n, notas = leer_compas(txt)
        for (i, largo, m) in notas:
            pos = i * 8.0 / n                       # en corcheas desde el principio del compas
            if pos in (0.0, 2.0, 4.0, 6.0) and (m % 12) not in pcs_acorde(simb) and (m % 12) not in permitidos:
                print('  [%s] compas %d (%s): la nota %d (%s) cae en tiempo fuerte y no es del acorde' % (nombre, b + 1, simb, m, txt.split()[i]))
                avisos += 1
            if (m % 12) not in escala:
                print('  [%s] compas %d (%s): la nota %s no es de la escala' % (nombre, b + 1, simb, txt.split()[i]))
                avisos += 1
    return avisos


# ----------------------------------------------------------------------------------------------------------------------------------------- la cancion

def potencia_activa(x):
    """Potencia media de los trozos (de 2048 muestras) en los que suena algo: sirve para igualar el volumen de cada parte aunque unas toquen siempre y otras de vez en cuando."""
    m = x.mean(axis=1) if x.ndim == 2 else x
    n = len(m) // 2048
    if n == 0:
        return 1e-12
    p = (m[:n * 2048].reshape(n, 2048) ** 2).mean(axis=1)
    umbral = p.max() * 0.04
    activos = p[p > umbral]
    return float(activos.mean()) if len(activos) else 1e-12


class Cancion:
    def __init__(self, nombre, bpm, compases, cola=3.0):
        self.nombre, self.bpm, self.compases = nombre, bpm, compases
        self.beat = 60.0 / bpm
        self.compas = 4 * self.beat
        self.largo = compases * self.compas
        ticks = self.largo * 20.0
        if abs(ticks - round(ticks)) > 1e-6:
            raise ValueError('%s: %d compases a %d bpm duran %.4f s = %.3f ticks (tiene que ser un numero entero de ticks)' % (nombre, compases, bpm, self.largo, ticks))
        self.ticks = int(round(ticks))
        self.cola = cola
        self.partes = {}

    def parte(self, nombre):
        if nombre not in self.partes:
            self.partes[nombre] = S.Mezcla(self.largo + self.cola)
        return self.partes[nombre]

    def poner(self, parte, compas, t, x, vol=1.0, pan=0.0):
        """Pone el sonido x en el compas (0..) a t segundos de su principio."""
        self.parte(parte).poner(compas * self.compas + t, x, vol, pan)

    def casilla(self, n=8):
        return self.compas / n

    def melodia(self, parte, compas, txt, voz, vol=1.0, pan=0.0, gate=0.93, transp=0):
        """Toca un compas escrito en la notacion de casillas con la funcion `voz(midi, segundos)`."""
        n, notas = leer_compas(txt)
        cs = self.compas / n
        for (i, largo, m) in notas:
            self.poner(parte, compas, i * cs, voz(m + transp, largo * cs * gate), vol, pan)

    def mezclar(self, pesos_db, eco=None, reverb=None, rms=0.13, referencia=None):
        """Junta las partes igualando su volumen al de la `referencia` (la primera si no se dice) mas los decibelios de `pesos_db`; luego eco, reverb y volumen final."""
        ref = referencia or next(iter(pesos_db))
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
        # sin chasquidos: entrada suave y la cola se apaga del todo
        f_in, f_out = int(0.004 * S.SR), int(0.15 * S.SR)
        total[:f_in] *= np.linspace(0, 1, f_in)[:, None]
        total[-f_out:] *= np.linspace(1, 0, f_out)[:, None]
        return total


def guardar(c, total, calidad=3):
    ruta = os.path.join(SALIDA, 'musica_%s.ogg' % c.nombre)
    seg, kb = S.a_ogg(total, ruta, calidad)
    print('  musica_%s.ogg: %.1f s en total (bucle de %d ticks = %.2f s), %d KB' % (c.nombre, seg, c.ticks, c.ticks / 20.0, kb))
    return c.ticks


# ----------------------------------------------------------------------------------------------------------------------------------------- patrones comunes

def arpegio_notas(tonos, patron):
    """Los indices del patron sobre los tonos del acorde (el indice 3 en un acorde de tres notas es la fundamental una octava mas arriba)."""
    out = []
    for i in patron:
        out.append(tonos[i] if i < len(tonos) else tonos[i - len(tonos)] + 12)
    return out


def chirrido(f0=5200.0, f1=3200.0, dur=0.09):
    n = int(dur * S.SR)
    t = np.arange(n) / S.SR
    f = f0 * (f1 / f0) ** (t / dur)
    return S.fundido(np.sin(2 * np.pi * np.cumsum(f) / S.SR) * np.exp(-t * 18.0), 0.002, 0.01)


def subida(dur, fmin=300.0, fmax=7000.0, volumen=1.0):
    """Un barrido de ruido que sube y crece (para anunciar el siguiente tramo)."""
    n = int(dur * S.SR)
    t = np.arange(n) / S.SR
    fcs = fmin * (fmax / fmin) ** (t / dur)
    x = S.barrido_pasabanda(S.ruido(n), fcs, 1.6)
    return x * (t / dur) ** 2.2 * volumen


# ----------------------------------------------------------------------------------------------------------------------------------------- 1. PRADERA (sol mayor, 128 bpm, 32 compases = 60 s)

def pradera():
    c = Cancion('pradera', 128, 32)
    prog = PROG_PRADERA
    mel = [
        'G4 - B4 D5 - B4 G5 -', 'F#5 - A5 - F#5 D5 - A4', 'G5 - E5 G5 B5 - G5 E5', 'E5 - G5 - E5 D5 C5 -',                     # A
        'B4 - D5 - G5 - B5 -', 'A5 - F#5 - D5 - F#5 A5', 'G5 - E5 - C5 E5 G5 -', 'F#5 A5 F#5 D5 A4 D5 F#5 -',                  # A'
        'E5 - - G5 B5 - - G5', 'C6 - - G5 E5 - G5 -', 'D5 - G5 - B5 - D6 -', 'A5 - F#5 D5 A5 - - -',                            # B
        'B5 - G5 E5 G5 - B5 -', 'C6 - G5 A5 G5 - E5 -', 'F#5 - A5 - D6 - A5 -', 'F#5 - D5 - A4 - D5 F#5',                       # B'
    ]
    avisos = verificar('pradera', mel, prog, pcs_escala('G', 'mayor'))
    if avisos:
        print('  (%d avisos en pradera)' % avisos)
    if SOLO_VERIFICAR:
        return 0
    cs = c.casilla(8)
    for k in range(32):
        simb, ronda = prog[k % 16], k // 16
        tonos4 = acorde_midi(simb, 4)
        tonos3 = acorde_midi(simb, 3)
        # melodia (pulso estrecho con vibrato); en la segunda vuelta se dobla una octava abajo con triangulo
        c.melodia('lead', k, mel[k % 16], lambda m, d: S.v_pulso(m, d, 0.25, vib=0.25, brillo=7500, d=0.3, s=0.72), 1.0, 0.0)
        if ronda == 1:
            c.melodia('lead2', k, mel[k % 16], lambda m, d: S.v_tri(m, d, d=0.4, s=0.8), 1.0, 0.0, transp=-12)
        # bajo de triangulo que rebota
        r = raiz_grave(simb, 36)
        for slot, nota in ((0, r), (2, r + 12), (3, r), (4, r + 7), (6, r + 12), (7, r + 7)):
            c.poner('bajo', k, slot * cs, S.v_tri(nota, cs * 0.9, d=0.2, s=0.7), 1.0, 0.0)
        # arpegio de semicorcheas (pulso 12,5 %), de lado a lado
        pat = arpegio_notas([tonos4[0], tonos4[1], tonos4[2]], [0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 0, 1, 2, 1])
        for q, nota in enumerate(pat):
            c.poner('arp', k, q * cs / 2, S.v_pulso(nota, cs / 2 * 0.9, 0.125, a=0.002, d=0.05, s=0.2, brillo=6000), 1.0, -0.45 if q % 2 else 0.45)
        # colchon de acorde
        for j, nota in enumerate(tonos3):
            c.poner('pad', k, 0.0, S.v_pad(nota, c.compas * 0.98, a=0.08, r=0.2, brillo=1500), 1.0, (-0.5, 0.0, 0.5)[j % 3])
        # bateria
        for slot in (0, 4):
            c.poner('bombo', k, slot * cs, S.bombo(), 1.0)
        if k % 4 in (1, 3):
            c.poner('bombo', k, 7 * cs, S.bombo(0.2), 0.7)
        for slot in (2, 6):
            c.poner('caja', k, slot * cs, S.caja(), 1.0)
        if k % 4 == 3:                                         # relleno: redoble de semicorcheas en el ultimo tiempo
            for q in range(4):
                c.poner('caja', k, 6 * cs + q * cs / 2, S.caja(0.1, brillo=0.8), 0.55 + 0.12 * q)
        for slot in range(8):
            c.poner('hats', k, slot * cs, S.hihat(abierto=(slot == 7 and k % 2 == 1)), 0.9 if slot % 2 == 0 else 0.55, 0.25 if slot % 2 else -0.25)
        if ronda == 1:
            for q in range(16):
                c.poner('extra', k, q * cs / 2, S.maraca(), 0.5 + 0.3 * (q % 2), 0.4 if q % 2 else -0.4)
        # campanitas: arriba cada ocho compases y una cascada al final
        if k % 8 == 0:
            c.poner('campanas', k, 0.0, S.v_campana(tonos4[0] + 24, 1.6), 1.0, 0.3)
        if k % 8 == 7:
            for j, nota in enumerate((tonos4[2] + 24, tonos4[1] + 24, tonos4[0] + 24)):
                c.poner('campanas', k, (5 + j) * cs, S.v_campana(nota, 1.2), 0.9 - 0.15 * j, -0.4 + 0.4 * j)
    for k in (0, 8, 16, 24):
        c.poner('extra', k, 0.0, S.platillo(1.4), 0.7)
    pesos = {'lead': 0.0, 'lead2': -9.0, 'bajo': -3.0, 'arp': -9.5, 'pad': -11.0, 'bombo': -5.0, 'caja': -7.0, 'hats': -15.0, 'extra': -14.0, 'campanas': -9.0}
    total = c.mezclar(pesos, eco=dict(retraso=0.3516, retroalim=0.5, mezcla=0.22, rebotes=3), reverb=dict(mezcla=0.2, segundos=1.6, oscuridad=1.0), rms=0.125)
    return guardar(c, total)


# ----------------------------------------------------------------------------------------------------------------------------------------- 2. BOSQUE EMBRUJADO (la menor, 120 bpm, 32 compases = 64 s)

def bosque():
    c = Cancion('bosque', 120, 32)
    prog = PROG_BOSQUE
    mel = [
        'E5 - - - C5 - A4 C5', 'E5 - A5 - E5 - C5 -', 'F5 - A5 - C6 - A5 -', 'G#5 - B5 - G#5 - E5 -',                           # A
        'A5 - C6 - E6 - C6 -', 'D6 - A5 - F5 - A5 -', 'B5 - G#5 - B5 - D6 -', 'C6 - A5 - E5 - - -',                             # A'
        'A5 - C6 - F6 - C6 -', 'B5 - D6 - G6 - D6 -', 'E6 - C6 - A5 - C6 -', 'B5 - G#5 - E5 - G#5 B5',                          # B
        'A5 - F5 - D5 - F5 -', 'C6 - E6 - C6 - A5 -', 'G#5 - B5 - D6 - B5 -', 'E5 - G#5 - B5 - G#5 -',                          # B'
    ]
    avisos = verificar('bosque', mel, prog, pcs_escala('A', 'menor', extra=(8,)))
    if avisos:
        print('  (%d avisos en bosque)' % avisos)
    if SOLO_VERIFICAR:
        return 0
    cs = c.casilla(8)
    rng = np.random.default_rng(31)
    for k in range(32):
        simb, ronda = prog[k % 16], k // 16
        tonos3 = acorde_midi(simb, 3)
        tonos4 = acorde_midi(simb, 4)
        # la voz fantasma
        prev = [None]

        def voz(m, d, _p=prev):
            x = S.v_fantasma(m, d, a=0.08, r=0.2, prof=0.45, desde=_p[0] if (_p[0] is not None and abs(_p[0] - m) >= 4) else None)
            _p[0] = m
            return x
        c.melodia('lead', k, mel[k % 16], voz, 1.0, 0.0)
        if ronda == 1:
            c.melodia('lead2', k, mel[k % 16], lambda m, d: S.v_clave(m, min(d, 0.5)), 1.0, 0.15)
        # clavicembalo: corcheas que suben y bajan por el acorde (una vez por compas incluye la septima)
        pat = arpegio_notas(tonos4, [0, 1, 2, 1, 0, 1, 3 if len(tonos4) > 3 else 2, 1])
        for slot, nota in enumerate(pat):
            c.poner('arp', k, slot * cs, S.v_clave(nota, cs * 1.4), 0.9 if slot % 2 == 0 else 0.6, -0.35 if slot % 2 else 0.35)
        # bajo pellizcado a los tiempos 1 y 3
        r = raiz_grave(simb, 33)
        c.poner('bajo', k, 0.0, S.v_pizz(r, 0.5), 1.0)
        c.poner('bajo', k, 4 * cs, S.v_pizz(r + 7 if simb in ('Am', 'Dm', 'F') else r, 0.5), 0.8)
        # organo con temblor
        for j, nota in enumerate(tonos3):
            c.poner('pad', k, 0.0, S.v_organo(nota, c.compas * 0.97, trem=0.3), 1.0, (-0.45, 0.0, 0.45)[j % 3])
        # corazon: dos latidos por compas (fuerte-suave) y el tic-tac de un reloj
        for base in (0, 4):
            c.poner('latido', k, base * cs, S.timbal(0.7, 70.0), 1.0)
            c.poner('latido', k, (base + 0.75) * cs, S.timbal(0.5, 66.0), 0.55)
        c.poner('reloj', k, 2 * cs, S.madera(0.07, 880.0), 0.8, 0.5)
        c.poner('reloj', k, 6 * cs, S.madera(0.07, 640.0), 0.7, -0.5)
        # viento cada cuatro compases, campana grave cada ocho, murcielagos de vez en cuando
        if k % 4 == 0:
            c.poner('ambiente', k, 0.0, S.viento(4 * c.compas, 1.0, 700.0 + 300 * (k % 8 == 4)), 1.0, -0.5 if (k // 4) % 2 == 0 else 0.5)
        if k % 8 == 0:
            c.poner('campana', k, 0.0, S.v_campana(45, 5.0, decay=1.1), 1.0, -0.2)
            c.poner('campana', k, 0.0, S.v_campana(52, 4.0, decay=1.4), 0.5, 0.2)
        if k in (5, 13, 21, 29):
            for j in range(3):
                c.poner('ambiente', k, (5 + 0.27 * j) * cs, chirrido(5400 - 300 * j, 3300 - 200 * j), 0.5, rng.uniform(-0.7, 0.7))
        if ronda == 1:                                         # segunda vuelta: coro de «aah» una octava arriba (colchon con filtro cerrado)
            for j, nota in enumerate(tonos4):
                c.poner('coro', k, 0.0, S.v_pad(nota + 12, c.compas * 0.98, a=0.35, r=0.5, brillo=1100), 1.0, (-0.6, -0.2, 0.2, 0.6)[j % 4])
    pesos = {'lead': 0.0, 'lead2': -9.0, 'arp': -8.0, 'bajo': -4.0, 'pad': -10.0, 'latido': -5.5, 'reloj': -13.0, 'ambiente': -14.0, 'campana': -9.0, 'coro': -12.0}
    total = c.mezclar(pesos, eco=dict(retraso=0.375, retroalim=0.5, mezcla=0.3, rebotes=4, filtro=2600.0), reverb=dict(mezcla=0.3, segundos=2.8, oscuridad=1.3), rms=0.115)
    return guardar(c, total)


# ----------------------------------------------------------------------------------------------------------------------------------------- 3. COSMOS MICHIALIEN (do mayor, 100 bpm, 32 compases = 76,8 s)

def cosmos():
    c = Cancion('cosmos', 100, 32)
    prog = PROG_COSMOS
    mel = [
        'E5 - - - G5 - B5 -', 'A5 - - - G5 - E5 -', 'C6 - - A5 F5 - A5 -', 'G5 - B5 - D6 - E6 -',
        'E6 - - D6 C6 - B5 -', 'G5 - B5 - D6 - B5 -', 'A5 - C6 - E6 - C6 -', 'D6 - B5 - G5 - - -',
        'C6 - E6 - A6 - E6 -', 'F6 - E6 - C6 - A5 -', 'G6 - E6 - C6 - E6 -', 'D6 - B5 - G5 - B5 D6',
        'E6 - C6 - A5 - C6 -', 'F6 - D6 - A5 - D6 -', 'C6 - A5 - F5 - A5 C6', 'D6 - B5 - G5 - B5 -',
    ]
    avisos = verificar('cosmos', mel, prog, pcs_escala('C', 'mayor'))
    if avisos:
        print('  (%d avisos en cosmos)' % avisos)
    if SOLO_VERIFICAR:
        return 0
    cs = c.casilla(8)
    rng = np.random.default_rng(77)
    pentatonica = [midi(n) for n in ('C6', 'D6', 'E6', 'G6', 'A6', 'C7')]
    for k in range(32):
        simb, ronda = prog[k % 16], k // 16
        tonos3 = acorde_midi(simb, 3)
        tonos4 = acorde_midi(simb, 4)
        # melodia de campana de cristal (en la segunda vuelta, con una caja de musica una octava arriba en las notas largas)
        c.melodia('lead', k, mel[k % 16], lambda m, d: S.v_campana(m, max(d, 0.5) + 0.6, brillante=False, decay=1.5), 1.0, 0.0, gate=1.0)
        if ronda == 1:
            c.melodia('lead2', k, mel[k % 16], lambda m, d: S.v_cajita(m + 12, 0.8), 1.0, 0.3)
        # arpegio de semicorcheas de caja de musica que sube y baja por cuatro tonos (con eco ping-pong de toda la mezcla)
        pat = arpegio_notas(tonos4[:4] if len(tonos4) >= 4 else tonos4 + [tonos4[0] + 12], [0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 0, 1, 2, 1])
        for q, nota in enumerate(pat):
            c.poner('arp', k, q * cs / 2, S.v_cajita(nota + 12, 0.7), 0.8 if q % 4 == 0 else 0.5, -0.5 if q % 2 else 0.5)
        # colchon que se abre
        for j, nota in enumerate(tonos3 + [tonos3[0] + 12]):
            c.poner('pad', k, 0.0, S.v_pad(nota, c.compas, a=0.7, r=0.9, brillo=1300, abre=0.9), 1.0, (-0.7, -0.25, 0.25, 0.7)[j % 4])
        # bajo seno con un poco de saturacion
        r = raiz_grave(simb, 36)
        c.poner('bajo', k, 0.0, S.saturar(S.v_seno(r, c.compas * 0.55, a=0.01, d=0.5, s=0.6), 1.6), 1.0)
        c.poner('bajo', k, 5 * cs, S.saturar(S.v_seno(r + 7, cs * 1.6, a=0.01, d=0.3, s=0.5), 1.6), 0.7)
        # bateria suave
        for slot in (0, 4):
            c.poner('bombo', k, slot * cs, S.bombo(0.28, 48.0, 120.0), 0.85)
        for slot in (2, 6):
            c.poner('palmada', k, slot * cs, S.palmada(), 0.55)
        for slot in (1, 3, 5, 7):
            c.poner('hats', k, slot * cs, S.hihat(abierto=(slot == 3 or slot == 7)), 0.5, 0.3 if slot % 4 == 1 else -0.3)
        for q in range(16):
            c.poner('extra', k, q * cs / 2, S.maraca(), 0.28 + 0.14 * (q % 2), 0.4 if q % 2 else -0.4)
        # destellos al azar
        for _ in range(3):
            c.poner('destellos', k, rng.uniform(0, c.compas * 0.9), S.v_campana(int(rng.choice(pentatonica)), 1.2, decay=3.5), rng.uniform(0.5, 1.0), rng.uniform(-0.8, 0.8))
        if k % 8 == 7:
            c.poner('subida', k, c.compas * 0.35, subida(c.compas * 0.65, 400.0, 8000.0), 1.0, 0.0)
        if k % 8 == 0 and k > 0:
            c.poner('subida', k, 0.0, S.platillo(1.8), 0.6)
    pesos = {'lead': 0.0, 'lead2': -9.0, 'arp': -9.5, 'pad': -7.5, 'bajo': -3.5, 'bombo': -6.5, 'palmada': -12.0, 'hats': -17.0, 'extra': -19.0, 'destellos': -13.0, 'subida': -17.0}
    total = c.mezclar(pesos, eco=dict(retraso=0.45, retroalim=0.55, mezcla=0.42, rebotes=5, filtro=3800.0), reverb=dict(mezcla=0.32, segundos=3.2, oscuridad=0.8), rms=0.12)
    return guardar(c, total)


# ----------------------------------------------------------------------------------------------------------------------------------------- 4. JEFE (re menor, 150 bpm, 32 compases = 51,2 s)

def jefe():
    c = Cancion('jefe', 150, 32)
    prog = PROG_JEFE
    mel = [
        'A5 - D6 - A5 F5 D5 -', 'F5 - A5 - D6 - F6 -', 'D6 - Bb5 - F5 - Bb5 D6', 'E6 - C6 - G5 - C6 E6',
        'D6 - F6 - A6 - F6 -', 'G5 - Bb5 - D6 - Bb5 -', 'E6 - C#6 - A5 - C#6 E6', 'G6 - E6 - C#6 - A5 -',
        'F6 - D6 - Bb5 - D6 -', 'A5 - C6 - F6 - C6 -', 'D6 - Bb5 - G5 - Bb5 -', 'C#6 - E6 - G6 - E6 -',
        'A5 - D6 - F6 - A6 -', 'Bb5 - D6 - F6 - D6 -', 'A5 - C#6 - E6 - G6 -', 'E6 - C#6 - A5 - E5 A5',
    ]
    avisos = verificar('jefe', mel, prog, pcs_escala('D', 'menor', extra=(1,)))
    if avisos:
        print('  (%d avisos en jefe)' % avisos)
    if SOLO_VERIFICAR:
        return 0
    cs = c.casilla(8)
    for k in range(32):
        simb, ronda = prog[k % 16], k // 16
        tonos3 = acorde_midi(simb, 3)
        tonos4 = acorde_midi(simb, 4)
        # melodia de sierra cortante (en la segunda vuelta, con un pulso una octava abajo para dar cuerpo)
        c.melodia('lead', k, mel[k % 16], lambda m, d: S.v_saw(m, d, brillo=4800, a=0.003, d=0.25, s=0.62), 1.0, 0.0, gate=0.9)
        if ronda == 1:
            c.melodia('lead2', k, mel[k % 16], lambda m, d: S.v_pulso(m, d, 0.5, brillo=2500, a=0.004, d=0.2, s=0.6), 1.0, 0.0, gate=0.9, transp=-12)
        # bajo galopante: corcheas con salto de octava y una semicorchea extra al final
        r = raiz_grave(simb, 33)
        for slot in range(8):
            nota = r + (12 if slot in (3, 7) else 0)
            c.poner('bajo', k, slot * cs, S.v_bajo_saw(nota, cs * 0.82, brillo=800), 1.0 if slot % 2 == 0 else 0.8)
        # arpegio rapido de pulso
        pat = arpegio_notas(tonos4[:3], [0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 0, 2, 1, 2])
        for q, nota in enumerate(pat):
            c.poner('arp', k, q * cs / 2, S.v_pulso(nota, cs / 2 * 0.85, 0.25, a=0.002, d=0.04, s=0.3, brillo=5000), 1.0, -0.5 if q % 2 else 0.5)
        # colchon grave
        for j, nota in enumerate(tonos3):
            c.poner('pad', k, 0.0, S.v_pad(nota - 12, c.compas * 0.98, a=0.05, r=0.2, brillo=700), 1.0, (-0.4, 0.0, 0.4)[j % 3])
        # bateria
        for slot in (0, 2, 4, 6):
            c.poner('bombo', k, slot * cs, S.bombo(0.25, 46.0, 170.0), 1.0 if slot in (0, 4) else 0.8)
        if k % 4 == 3:
            c.poner('bombo', k, 7 * cs, S.bombo(0.18), 0.8)
        for slot in (2, 6):
            c.poner('caja', k, slot * cs, S.caja(0.2, 200.0, 0.7), 1.0)
        c.poner('caja', k, 5 * cs, S.caja(0.08, 200.0, 0.4), 0.3)
        for q in range(16):
            c.poner('hats', k, q * cs / 2, S.hihat(abierto=(q % 8 == 7)), 0.8 if q % 2 == 0 else 0.5, 0.2 if q % 2 else -0.2)
        if k % 4 == 0:
            c.poner('platillo', k, 0.0, S.platillo(1.6), 0.9)
        if k % 4 == 3:                                         # relleno de timbales que bajan
            for j, (f0, f1) in enumerate(((190, 120), (160, 100), (130, 85), (110, 70))):
                c.poner('timbales', k, (4 + j) * cs, S.tom(0.3, f0, f1), 1.0, 0.5 - 0.33 * j)
        if k % 8 == 0:
            c.poner('timbales', k, 0.0, S.timbal(1.0, 70.0), 1.0)
        if k % 8 == 7:
            c.poner('subida', k, c.compas * 0.5, subida(c.compas * 0.5, 300.0, 9000.0), 1.0, 0.0)
    pesos = {'lead': 0.0, 'lead2': -9.0, 'bajo': -3.0, 'arp': -9.0, 'pad': -11.0, 'bombo': -4.0, 'caja': -6.0, 'hats': -13.0, 'platillo': -12.0, 'timbales': -6.0, 'subida': -15.0}
    total = c.mezclar(pesos, eco=dict(retraso=0.2, retroalim=0.4, mezcla=0.14, rebotes=3), reverb=dict(mezcla=0.14, segundos=1.3, oscuridad=1.0), rms=0.135)
    return guardar(c, total)


# ----------------------------------------------------------------------------------------------------------------------------------------- 5. SALA (do mayor, 96 bpm, 32 compases = 80 s)

def sala():
    c = Cancion('sala', 96, 32)
    prog = PROG_SALA
    mel = [
        'E5 - - D5 C5 - - E5', 'D5 - - B4 G4 - - B4', 'C5 - - E5 A5 - - E5', 'G5 - - E5 B4 - - E5',
        'A5 - - G5 F5 - - A5', 'G5 - - E5 C5 - E5 G5', 'A5 - - F5 C6 - A5 F5', 'B5 - - A5 G5 - - D5',
        'G5 - - E5 C6 - - G5', 'B5 - - G5 D6 - - B5', 'C6 - - A5 E6 - - C6', 'B5 - - G5 E6 - - B5',
        'A5 - C6 - F6 - C6 A5', 'G5 - E5 - C5 - E5 G5', 'A5 - F5 - D5 - F5 A5', 'D5 - B4 - G4 - B4 D5',
    ]
    avisos = verificar('sala', mel, prog, pcs_escala('C', 'mayor'))
    if avisos:
        print('  (%d avisos en sala)' % avisos)
    if SOLO_VERIFICAR:
        return 0
    cs = c.casilla(8)
    rng = np.random.default_rng(5)
    for k in range(32):
        simb, ronda = prog[k % 16], k // 16
        tonos3 = acorde_midi(simb, 3)
        tonos4 = acorde_midi(simb, 4)
        # melodia de caja de musica (la segunda vuelta con la campanita de apoyo una octava arriba)
        c.melodia('lead', k, mel[k % 16], lambda m, d: S.v_cajita(m, 1.0), 1.0, 0.0, gate=1.0)
        if ronda == 1:
            c.melodia('lead2', k, mel[k % 16], lambda m, d: S.v_campana(m + 12, 1.2, brillante=False, decay=2.4), 1.0, 0.35, gate=1.0)
        # arpegio suave de cuerda pellizcada (corcheas)
        pat = arpegio_notas(tonos3, [0, 1, 2, 3, 2, 1, 2, 1])
        for slot, nota in enumerate(pat):
            c.poner('arp', k, slot * cs, S.v_pizz(nota + 12, 0.45, brillo=1800), 0.8 if slot % 2 == 0 else 0.5, -0.3 if slot % 2 else 0.3)
        # colchon
        for j, nota in enumerate(tonos3):
            c.poner('pad', k, 0.0, S.v_pad(nota, c.compas, a=0.5, r=0.7, brillo=1100, abre=0.3), 1.0, (-0.6, 0.0, 0.6)[j % 3])
        # bajo de triangulo
        r = raiz_grave(simb, 36)
        c.poner('bajo', k, 0.0, S.v_tri(r, cs * 3.2, a=0.01, d=0.4, s=0.6), 1.0)
        c.poner('bajo', k, 4 * cs, S.v_tri(r + 7, cs * 2.8, a=0.01, d=0.4, s=0.5), 0.75)
        # percusion muy suave: un bombo tenue, un clic de madera y una maraca a contratiempo
        if k % 2 == 0:
            c.poner('bombo', k, 0.0, S.bombo(0.3, 50.0, 110.0), 0.7)
        c.poner('madera', k, 2 * cs, S.madera(0.07, 900.0), 0.6, 0.4)
        c.poner('madera', k, 6 * cs, S.madera(0.07, 700.0), 0.5, -0.4)
        for slot in (1, 3, 5, 7):
            c.poner('extra', k, slot * cs, S.maraca(), 0.6, 0.3 if slot % 4 == 1 else -0.3)
        # estrellitas al azar
        for _ in range(2):
            c.poner('destellos', k, rng.uniform(0, c.compas * 0.9), S.v_campana(int(rng.choice([midi('E6'), midi('G6'), midi('C7'), midi('D7')])), 1.0, decay=4.0), rng.uniform(0.5, 1.0), rng.uniform(-0.8, 0.8))
    pesos = {'lead': 0.0, 'lead2': -10.0, 'arp': -8.5, 'pad': -8.0, 'bajo': -4.5, 'bombo': -11.0, 'madera': -17.0, 'extra': -22.0, 'destellos': -12.0}
    total = c.mezclar(pesos, eco=dict(retraso=0.46875, retroalim=0.5, mezcla=0.3, rebotes=4, filtro=3200.0), reverb=dict(mezcla=0.34, segundos=3.0, oscuridad=0.9), rms=0.105)
    return guardar(c, total)


PISTAS = {'sala': sala, 'pradera': pradera, 'bosque': bosque, 'cosmos': cosmos, 'jefe': jefe}


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
        duraciones[nombre] = PISTAS[nombre]()
    if SOLO_VERIFICAR:
        return
    with open(ruta_dur, 'w', encoding='utf-8') as f:
        f.write('# duracion de cada bucle de musica en ticks (Musica.TICKS de Musica.java tiene que coincidir)\n')
        for k in sorted(duraciones):
            f.write('%s=%d\n' % (k, duraciones[k]))


if __name__ == '__main__':
    main()
