#!/usr/bin/env python3
"""Compone y sintetiza la MUSICA del Cafe Michialien (todo propio, con numpy; ninguna muestra ni melodia de nadie). Mismo metodo que Emi Fall (ver tools/emi-fall/audio/musica.py):
cada compas son 8 casillas (nombre de nota = empieza, «-» = alarga, «.» = silencio) y antes de sintetizar se comprueba que los tiempos fuertes son del acorde.

Uso:  python3 musica.py [cerrado] [abierto]      (sin argumentos: las dos)
Escribe ../src/main/resources/assets/emi_cafe/sounds/musica_<nombre>.ogg y DURACIONES.txt (ticks de cada bucle; Musica.TICKS de Musica.java tiene que coincidir)."""
import os
import re
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sinte as S
from sinte import midi

AQUI = os.path.dirname(os.path.abspath(__file__))
SOLO_VERIFICAR = '--verificar' in sys.argv
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'emi_cafe', 'sounds')

CALIDADES = {'': (0, 4, 7), 'm': (0, 3, 7), '7': (0, 4, 7, 10), 'm7': (0, 3, 7, 10), 'maj7': (0, 4, 7, 11)}
ESCALAS = {'mayor': (0, 2, 4, 5, 7, 9, 11)}


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


def verificar(nombre, melodia, progresion, escala):
    avisos = 0
    for b, txt in enumerate(melodia):
        simb = progresion[b % len(progresion)]
        n, notas = leer_compas(txt)
        for (i, largo, m) in notas:
            pos = i * 8.0 / n
            if pos in (0.0, 2.0, 4.0, 6.0) and (m % 12) not in pcs_acorde(simb):
                print('  [%s] compas %d (%s): la nota %s cae en tiempo fuerte y no es del acorde' % (nombre, b + 1, simb, txt.split()[i]))
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

    def mezclar(self, pesos_db, eco=None, reverb=None, rms=0.12):
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


# ----------------------------------------------------------------------------------------------------------------------------------------- CERRADO (preparar el cafe, construir, comprar)
# Do mayor, 96 bpm, 16 compases = 40 s = 800 ticks: bossa tranquila con piano electrico, bajo pellizcado y escobillas

PROG_CERRADO = ['Cmaj7', 'Am7', 'Dm7', 'G7', 'Cmaj7', 'Am7', 'Fmaj7', 'G7', 'Em7', 'Am7', 'Dm7', 'G7', 'Fmaj7', 'Em7', 'Dm7', 'G7']
MEL_CERRADO = [
    'E5 - G5 - E5 D5 C5 -', 'C5 - E5 - C5 B4 A4 -', 'D5 - F5 - A5 - F5 E5', 'D5 - B4 - G4 A4 B4 -',
    'E5 - G5 - B5 - G5 E5', 'A5 - G5 - E5 - C5 D5', 'A5 - C6 - A5 G5 F5 -', 'D5 - F5 - D5 - B4 C5',
    'G5 - B5 - G5 - E5 D5', 'E5 - A5 - G5 - E5 C5', 'F5 - A5 - F5 - D5 E5', 'G5 - F5 - D5 - B4 D5',
    'C6 - A5 - F5 - A5 C6', 'B5 - G5 - E5 - G5 B5', 'A5 - F5 - D5 - F5 A5', 'B4 - D5 - G5 - - -',
]


def cerrado():
    c = Cancion('cerrado', 96, 16)
    escala = pcs_escala('C', 'mayor')
    avisos = verificar('cerrado', MEL_CERRADO, PROG_CERRADO, escala)
    if SOLO_VERIFICAR:
        return avisos
    cs = c.casilla(8)
    for k in range(16):
        simb = PROG_CERRADO[k]
        tonos = acorde_midi(simb, 4)
        c.melodia('lead', k, MEL_CERRADO[k], lambda m, d: S.v_campana(m, 1.1, brillante=False, decay=2.1), 1.0, 0.1, gate=1.0)
        # el piano electrico acompaña con dos golpes por compas (a contratiempo, como una bossa)
        for slot in (1, 4):
            for j, nota in enumerate(tonos[:3]):
                c.poner('piano', k, slot * cs, S.v_seno(nota - 12, cs * 2.2, a=0.01, d=0.5, s=0.3, r=0.1), 0.7, (-0.4, 0.0, 0.4)[j])
        r = raiz_grave(simb, 36)
        c.poner('bajo', k, 0.0, S.v_pizz(r, 0.6, brillo=1400), 1.0)
        c.poner('bajo', k, 3 * cs, S.v_pizz(r + 7, 0.45, brillo=1400), 0.7)
        c.poner('bajo', k, 4 * cs, S.v_pizz(r + 12 if r < 43 else r, 0.5, brillo=1400), 0.85)
        c.poner('bajo', k, 7 * cs, S.v_pizz(r + 7, 0.3, brillo=1400), 0.55)
        for slot in range(8):                                   # escobillas suaves
            c.poner('bateria', k, slot * cs, S.hihat(False), 0.35 if slot % 2 == 0 else 0.2, 0.3)
        c.poner('bateria', k, 0.0, S.bombo(0.25, 48.0, 100.0), 0.45)
        c.poner('bateria', k, 4 * cs, S.bombo(0.25, 48.0, 100.0), 0.35)
        c.poner('bateria', k, 2 * cs, S.madera(0.08, 760.0), 0.5, -0.2)
        c.poner('bateria', k, 6 * cs, S.madera(0.08, 640.0), 0.5, 0.2)
        for j, nota in enumerate(tonos):
            c.poner('pad', k, 0.0, S.v_pad(nota - 12, c.compas, a=0.6, r=0.8, brillo=900, abre=0.2), 1.0, (-0.6, 0.0, 0.6)[j % 3])
    total = c.mezclar({'lead': 0, 'piano': -9, 'bajo': -3, 'bateria': -9, 'pad': -15}, eco=dict(retraso=0.375, retroalim=0.4, mezcla=0.18), reverb=dict(mezcla=0.2, segundos=1.8))
    return guardar(c, total)


# ----------------------------------------------------------------------------------------------------------------------------------------- ABIERTO (con clientes)
# Sol mayor, 120 bpm, 16 compases = 32 s = 640 ticks: pop alegre y con prisa, lead brillante, arpegios y bateria

PROG_ABIERTO = ['G', 'Em', 'C', 'D', 'G', 'Em', 'C', 'D', 'Em', 'C', 'G', 'D', 'C', 'G', 'Am7', 'D7']
MEL_ABIERTO = [
    'D5 - G5 - B5 - G5 D5', 'E5 - G5 - B5 - G5 E5', 'E5 - G5 - C6 - G5 E5', 'D5 - F#5 - A5 - F#5 D5',
    'B5 - G5 A5 B5 - D6 -', 'G5 - B5 - G5 F#5 E5 -', 'C6 - G5 A5 G5 - E5 -', 'F#5 - A5 - D6 - A5 F#5',
    'E5 - G5 - B5 - G5 B5', 'C6 - G5 - E5 - G5 C6', 'D5 - G5 - B5 - D6 B5', 'A5 - F#5 - D5 - F#5 A5',
    'E6 - C6 - G5 - C6 E6', 'D6 - B5 - G5 - B5 D6', 'E5 - A5 - C6 - A5 G5', 'A5 - F#5 - D5 - - -',
]


def abierto():
    c = Cancion('abierto', 120, 16)
    escala = pcs_escala('G', 'mayor')
    avisos = verificar('abierto', MEL_ABIERTO, PROG_ABIERTO, escala)
    if SOLO_VERIFICAR:
        return avisos
    cs = c.casilla(8)
    for k in range(16):
        simb = PROG_ABIERTO[k]
        tonos = acorde_midi(simb, 3)
        c.melodia('lead', k, MEL_ABIERTO[k], lambda m, d: S.v_pulso(m, d + 0.05, duty=0.3, a=0.004, d=0.18, s=0.45, r=0.05, vib=0.12, brillo=5200), 0.9, 0.0, gate=0.95)
        if k >= 8:
            c.melodia('lead2', k, MEL_ABIERTO[k], lambda m, d: S.v_campana(m + 12, 0.8, brillante=True, decay=3.0), 0.7, 0.4, gate=1.0)
        pat = arpegio_notas(tonos, [0, 1, 2, 1, 0, 1, 2, 1])
        for slot, nota in enumerate(pat):
            c.poner('arp', k, slot * cs, S.v_pizz(nota + 12, 0.3, brillo=2600), 0.7 if slot % 2 == 0 else 0.45, -0.4 if slot % 2 else 0.4)
        r = raiz_grave(simb, 36)
        for slot in (0, 2, 4, 6):
            c.poner('bajo', k, slot * cs, S.v_bajo_saw(r if slot % 4 == 0 else r + 7, cs * 1.6, brillo=800), 1.0 if slot % 4 == 0 else 0.7)
        for slot in (0, 4):
            c.poner('bateria', k, slot * cs, S.bombo(0.28, 52.0, 130.0), 0.85)
        for slot in (2, 6):
            c.poner('bateria', k, slot * cs, S.palmada(0.2), 0.6)
        for slot in range(8):
            c.poner('bateria', k, slot * cs, S.hihat(slot == 7), 0.4 if slot % 2 == 0 else 0.25, 0.25)
        if k % 4 == 3:
            c.poner('bateria', k, 7 * cs, S.maraca(0.09), 0.6)
        for j, nota in enumerate(tonos):
            c.poner('pad', k, 0.0, S.v_pad(nota, c.compas, a=0.3, r=0.5, brillo=1200), 1.0, (-0.6, 0.0, 0.6)[j % 3])
    total = c.mezclar({'lead': 0, 'lead2': -9, 'arp': -7, 'bajo': -4, 'bateria': -8, 'pad': -15}, eco=dict(retraso=0.25, retroalim=0.35, mezcla=0.15), reverb=dict(mezcla=0.12, segundos=1.2))
    return guardar(c, total)


if __name__ == '__main__':
    nombres = [a for a in sys.argv[1:] if not a.startswith('--')] or ['cerrado', 'abierto']
    fun = {'cerrado': cerrado, 'abierto': abierto}
    res = {}
    for n in nombres:
        print('Componiendo', n)
        res[n] = fun[n]()
    if SOLO_VERIFICAR:
        print('avisos:', sum(res.values()))
    else:
        ruta = os.path.join(AQUI, 'DURACIONES.txt')
        previo = {}
        if os.path.exists(ruta):
            for l in open(ruta):
                if '=' in l and not l.startswith('#'):
                    k, v = l.strip().split('=')
                    previo[k] = int(v)
        previo.update(res)
        with open(ruta, 'w') as f:
            f.write('# duracion de cada bucle de musica en ticks (Musica.TICKS de Musica.java tiene que coincidir)\n')
            for k in sorted(previo):
                f.write('%s=%d\n' % (k, previo[k]))
