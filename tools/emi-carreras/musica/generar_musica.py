"""Genera la musica de fondo de las carreras (chiptune original, sintetizada con numpy) y la codifica a .ogg con ffmpeg.

Uso: python3 generar_musica.py   (necesita numpy y ffmpeg con libvorbis)
Escribe src/main/resources/assets/emi_carreras/sounds/musica_circuito.ogg (51,2 s) y musica_volcan.ogg (40,0 s).
Las duraciones DEBEN coincidir con DURACION_MUSICA de Carrera.java (en ticks: 1024 y 800): el servidor relanza la pista cuando acaba.
"""
import os, subprocess, wave
import numpy as np

SR = 44100
RNG = np.random.default_rng(7)
HERE = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'emi_carreras', 'sounds')


def hz(m): return 440.0 * 2 ** ((m - 69) / 12.0)

def env(n, a=0.004, d=0.12, s=0.55, r=0.03):
    t = np.arange(n) / SR
    e = np.where(t < a, t / a, s + (1 - s) * np.exp(-(t - a) / d))
    rel = int(r * SR)
    if n > rel: e[-rel:] *= np.linspace(1, 0, rel)
    return e

def pulse(f, dur, duty=0.5, vib=0.0, a=0.004, d=0.12, s=0.55):
    n = int(dur * SR)
    t = np.arange(n) / SR
    ph = (f * t + (vib * np.sin(2 * np.pi * 5.5 * t) * 0.01 * f * t if vib else 0)) % 1.0
    return np.where(ph < duty, 1.0, -1.0) * env(n, a, d, s)

def tri(f, dur, a=0.004, d=0.3, s=0.7):
    n = int(dur * SR); t = np.arange(n) / SR
    return (2 * np.abs(2 * ((f * t) % 1.0) - 1) - 1) * env(n, a, d, s)

def saw(f, dur, a=0.004, d=0.2, s=0.5):
    n = int(dur * SR); t = np.arange(n) / SR
    return (2 * ((f * t) % 1.0) - 1) * env(n, a, d, s)

def kick(dur=0.22):
    n = int(dur * SR); t = np.arange(n) / SR
    f = 110 * np.exp(-t * 18) + 42
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 14)

def snare(dur=0.16):
    n = int(dur * SR); t = np.arange(n) / SR
    return (RNG.uniform(-1, 1, n) * 0.8 + np.sin(2 * np.pi * 190 * t) * 0.4) * np.exp(-t * 26)

def hat(dur=0.05, abierto=False):
    if abierto: dur = 0.14
    n = int(dur * SR); t = np.arange(n) / SR
    x = RNG.uniform(-1, 1, n)
    x = np.diff(np.concatenate([[0], x]))             # filtro paso alto burdo
    return x * np.exp(-t * (30 if abierto else 90))

def retumbo(dur):
    n = int(dur * SR); t = np.arange(n) / SR
    x = RNG.uniform(-1, 1, n)
    k = np.ones(400) / 400
    return np.convolve(x, k, mode='same') * np.exp(-t * 2.0)

class Mezcla:
    def __init__(s, seg):
        s.n = int(seg * SR)
        s.izq = np.zeros(s.n); s.der = np.zeros(s.n)
    def poner(s, t0, x, vol=1.0, pan=0.0):
        i = int(t0 * SR)
        if i >= s.n: return
        x = x[: s.n - i] * vol
        s.izq[i:i + len(x)] += x * (1 - max(0, pan)); s.der[i:i + len(x)] += x * (1 + min(0, pan))
    def fin(s):
        m = max(np.abs(s.izq).max(), np.abs(s.der).max(), 1e-6)
        out = np.stack([s.izq, s.der], 1) / m * 0.85
        f = int(0.02 * SR); out[-f:] *= np.linspace(1, 0, f)[:, None]; out[:f] *= np.linspace(0, 1, f)[:, None]
        return out

ACORDES = {'C': (60, 64, 67), 'G': (55, 59, 62), 'Am': (57, 60, 64), 'F': (53, 57, 60), 'Dm': (62, 65, 69), 'Em': (64, 67, 71),
           'E': (52, 56, 59), 'E7': (52, 56, 59), 'Bdim': (59, 62, 65)}

def pista(nombre, bpm, compases, prog, motivos, lead_ond, bajo_ond, duty_arp, lead_oct=0, bateria='alegre', arps_vol=0.16):
    beat = 60.0 / bpm
    total = compases * 4 * beat
    mz = Mezcla(total)
    for b in range(compases):
        acorde = ACORDES[prog[b % len(prog)]]
        raiz = acorde[0]
        t0 = b * 4 * beat
        # bajo: corcheas con salto de octava
        for k in range(8):
            nota = raiz - 12 + (12 if k % 4 == 3 else 0) if raiz > 52 else raiz + (12 if k % 4 == 3 else 0)
            while nota > 52: nota -= 12
            while nota < 33: nota += 12
            x = (tri if bajo_ond == 'tri' else pulse)(hz(nota), beat * 0.48, *(() if bajo_ond == 'tri' else (0.5,)))
            mz.poner(t0 + k * beat / 2, x, 0.55)
        # arpegio de semicorcheas
        for k in range(16):
            nota = acorde[k % 3] + 12 + (12 if (k // 3) % 2 else 0)
            mz.poner(t0 + k * beat / 4, pulse(hz(nota), beat / 4 * 0.9, duty_arp, d=0.05, s=0.3), arps_vol, pan=0.5 if k % 2 else -0.5)
        # melodia
        mot = motivos[b % len(motivos)]
        for (off, largo, grado) in mot:
            oct_ = 12 * (grado // 3) + lead_oct
            nota = acorde[grado % 3] + 12 + oct_
            if lead_ond == 'pulse': x = pulse(hz(nota), largo * beat / 2 * 0.95, 0.25, vib=1.0, d=0.2, s=0.6)
            else: x = saw(hz(nota), largo * beat / 2 * 0.95)
            mz.poner(t0 + off * beat / 2, x, 0.38)
        # bateria
        for k in range(4):
            if bateria == 'alegre':
                if k in (0, 2): mz.poner(t0 + k * beat, kick(), 0.9)
                if k in (1, 3): mz.poner(t0 + k * beat, snare(), 0.55)
                mz.poner(t0 + k * beat, hat(), 0.22); mz.poner(t0 + k * beat + beat / 2, hat(abierto=(k == 3)), 0.2)
            else:   # volcan: bombo doble y semicorcheas de hi-hat
                mz.poner(t0 + k * beat, kick(), 1.0)
                if k in (0, 2): mz.poner(t0 + k * beat + beat * 0.75, kick(0.18), 0.7)
                if k in (1, 3): mz.poner(t0 + k * beat, snare(), 0.7)
                for q in range(4): mz.poner(t0 + k * beat + q * beat / 4, hat(), 0.16 + 0.06 * (q % 2))
        if bateria != 'alegre' and b % 4 == 0: mz.poner(t0, retumbo(beat * 3), 0.5)
    out = mz.fin()
    return out, total

def guardar(nombre, datos):
    os.makedirs(SALIDA, exist_ok=True)
    wav = os.path.join(HERE, nombre + '.wav')
    with wave.open(wav, 'wb') as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(datos, -1, 1) * 32767).astype('<i2').tobytes())
    ogg = os.path.join(SALIDA, nombre + '.ogg')
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '3', ogg], check=True)
    os.remove(wav)
    print(nombre, '%.1f s' % (len(datos) / SR), os.path.getsize(ogg) // 1024, 'KB')

def main():
    # Circuito Pokeball: alegre, do mayor, 150 bpm, 32 compases = 51,2 s
    prog_c = ['C', 'G', 'Am', 'F', 'C', 'G', 'F', 'G', 'F', 'C', 'G', 'Am', 'F', 'G', 'C', 'C']
    mot_c = [
        [(0, 1, 2), (1, 1, 1), (2, 2, 2), (4, 1, 3), (5, 1, 2), (6, 2, 1)],
        [(0, 2, 1), (2, 1, 2), (3, 1, 3), (4, 2, 4), (6, 1, 3), (7, 1, 2)],
        [(0, 1, 3), (1, 1, 2), (2, 1, 1), (3, 1, 2), (4, 2, 3), (6, 2, 2)],
        [(0, 2, 4), (2, 1, 3), (3, 1, 2), (4, 1, 1), (5, 1, 2), (6, 2, 3)],
    ]
    a, t = pista('circuito', 150, 32, prog_c, mot_c, 'pulse', 'tri', 0.125, bateria='alegre')
    assert abs(t - 51.2) < 1e-6, t
    guardar('musica_circuito', a)
    # Volcan: oscura y rapida, la menor, 168 bpm, 28 compases = 40,0 s
    prog_v = ['Am', 'F', 'G', 'E'] * 2 + ['Dm', 'Am', 'F', 'E'] * 2 + ['Am', 'F', 'G', 'E'] * 2 + ['Am', 'F', 'E', 'E']
    mot_v = [
        [(0, 1, 3), (1, 1, 3), (2, 1, 2), (3, 1, 3), (4, 2, 1), (6, 1, 2), (7, 1, 1)],
        [(0, 2, 2), (2, 1, 3), (3, 1, 4), (4, 1, 3), (5, 1, 2), (6, 2, 1)],
        [(0, 1, 4), (1, 1, 3), (2, 2, 2), (4, 1, 3), (5, 1, 4), (6, 2, 5)],
        [(0, 1, 2), (1, 1, 3), (2, 1, 2), (3, 1, 1), (4, 2, 2), (6, 1, 1), (7, 1, 0)],
    ]
    a, t = pista('volcan', 168, 28, prog_v, mot_v, 'saw', 'pulse', 0.25, bateria='fuerte', arps_vol=0.12)
    assert abs(t - 40.0) < 1e-6, t
    guardar('musica_volcan', a)

if __name__ == '__main__':
    main()
