"""Genera la musica y los efectos de sonido de la huida del Tyrantrum (todo sintetizado con numpy/scipy, original) y los codifica a .ogg con ffmpeg.

Uso: python3 generar_sonidos.py   (necesita numpy, scipy y ffmpeg con libvorbis)
Escribe en src/main/resources/assets/emi_huida/sounds/:
  musica_persecucion.ogg  64,0 s  (150 bpm, 40 compases)  -> DEBE ser 1280 ticks en Sonidos.duracion()
  musica_espera.ogg       32,0 s                           -> DEBE ser 640 ticks
  estrella, estrella_grande, bien, fallo, caida, pisada, victoria, derrota, listo
"""
import os, subprocess, wave
import numpy as np
from scipy.signal import butter, sosfilt, fftconvolve

SR = 44100
RNG = np.random.default_rng(11)
HERE = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'emi_huida', 'sounds')


# ------------------------------------------------------------------ utilidades
def hz(m): return 440.0 * 2 ** ((m - 69) / 12.0)

def lp(x, fc, order=2): return sosfilt(butter(order, min(fc, SR * 0.45) / (SR / 2), 'low', output='sos'), x)
def hp(x, fc, order=2): return sosfilt(butter(order, fc / (SR / 2), 'high', output='sos'), x)
def bp(x, f1, f2, order=2): return sosfilt(butter(order, [f1 / (SR / 2), min(f2, SR * 0.45) / (SR / 2)], 'band', output='sos'), x)

def tvec(n): return np.arange(n) / SR

def adsr(n, a=0.01, d=0.1, s=0.7, r=0.05):
    t = tvec(n)
    e = np.where(t < a, t / max(a, 1e-6), s + (1 - s) * np.exp(-(t - a) / max(d, 1e-6)))
    rel = min(int(r * SR), n)
    if rel > 0: e[-rel:] *= np.linspace(1, 0, rel)
    return e

def osc_fase(f, n, vib=0.0, vib_hz=5.5, vib_ini=0.0):
    """fase acumulada (en ciclos) con vibrato; f puede ser un numero o un array"""
    t = tvec(n)
    f = np.full(n, float(f)) if np.isscalar(f) else f
    if vib:
        prof = np.clip((t - vib_ini) / 0.25, 0, 1)
        f = f * (1 + vib * prof * np.sin(2 * np.pi * vib_hz * t))
    return np.cumsum(f) / SR

def saw_f(fase): return 2 * (fase % 1.0) - 1
def sine_f(fase): return np.sin(2 * np.pi * fase)

def saw(f, dur, vib=0.0, detune=(0.0,), vib_ini=0.0):
    n = int(dur * SR); x = np.zeros(n)
    for c in detune: x += saw_f(osc_fase(f * 2 ** (c / 1200.0), n, vib, 5.5, vib_ini))
    return x / len(detune)

def sinus(f, dur, vib=0.0):
    n = int(dur * SR); return sine_f(osc_fase(f, n, vib))

def ruido(dur): return RNG.uniform(-1, 1, int(dur * SR))

def reverb_ir(dur=1.6, tau=0.45):
    n = int(dur * SR); t = tvec(n)
    ir = np.stack([RNG.normal(size=n) * np.exp(-t / tau), RNG.normal(size=n) * np.exp(-t / tau)], 1)
    ir = lp(ir.T, 5200).T
    for k, (d, g) in enumerate([(0.011, .6), (0.019, .5), (0.027, .45), (0.041, .35)]):
        ir[int(d * SR), k % 2] += g * ir.std()
    ir[:int(0.004 * SR)] *= np.linspace(0, 1, int(0.004 * SR))[:, None]
    return ir / np.abs(ir).sum(0).max() * 3.0

def con_reverb(x, mezcla=0.2, dur=1.6, tau=0.45):
    ir = reverb_ir(dur, tau)
    h = np.stack([fftconvolve(x[:, c], ir[:, c], mode='full')[:len(x)] for c in range(2)], 1)
    h *= np.abs(x).max() / max(np.abs(h).max(), 1e-9)
    return x * (1 - mezcla) + h * mezcla * 1.3

class Mezcla:
    def __init__(s, seg):
        s.n = int(round(seg * SR)); s.izq = np.zeros(s.n); s.der = np.zeros(s.n)
    def poner(s, t0, x, vol=1.0, pan=0.0):
        i = int(round(t0 * SR))
        if i >= s.n or i < 0: return
        x = x[: s.n - i] * vol
        s.izq[i:i + len(x)] += x * (1 - max(0, pan)); s.der[i:i + len(x)] += x * (1 + min(0, pan))
    def estereo(s): return np.stack([s.izq, s.der], 1)

def normalizar(y, pico=0.89, saturar=True):
    if saturar: y = np.tanh(y * 1.15) / np.tanh(1.15)
    return y / max(np.abs(y).max(), 1e-9) * pico

def normalizar_rms(y, rms_db=-14.0, pico=0.8):
    """deja la mezcla a un volumen medio parecido al de la musica de las carreras (-15 dB) y sin picos que se pasen tras codificar a ogg"""
    y = y - y.mean(0)
    m = np.sqrt((y.mean(1) ** 2).mean())
    y = y * (10 ** (rms_db / 20) / max(m, 1e-9))
    y = pico * np.tanh(y / pico)
    return y

def guardar(nombre, datos):
    os.makedirs(SALIDA, exist_ok=True)
    wav = os.path.join(HERE, nombre + '.wav')
    with wave.open(wav, 'wb') as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(datos, -1, 1) * 32767).astype('<i2').tobytes())
    ogg = os.path.join(SALIDA, nombre + '.ogg')
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '3', ogg], check=True)
    os.remove(wav)
    print('%-24s %6.2f s  %5d KB' % (nombre, len(datos) / SR, os.path.getsize(ogg) // 1024))

def mono2(x, pan=0.0):
    return np.stack([x * (1 - max(0, pan)), x * (1 + min(0, pan))], 1)


# ------------------------------------------------------------------ instrumentos
def taiko(f0=62, dur=0.55):
    n = int(dur * SR); t = tvec(n)
    f = f0 * (1 + 1.6 * np.exp(-t * 22))
    cuerpo = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 7.5)
    golpe = lp(ruido(dur), 900) * np.exp(-t * 55) * 0.55
    return cuerpo + golpe

def tom(f0=140, dur=0.4):
    n = int(dur * SR); t = tvec(n)
    f = f0 * (1 + 0.9 * np.exp(-t * 30))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 10) + lp(ruido(dur), 1500) * np.exp(-t * 60) * 0.35

def caja(dur=0.18):
    n = int(dur * SR); t = tvec(n)
    return (bp(ruido(dur), 1500, 6000) * 0.8 + np.sin(2 * np.pi * 200 * t) * 0.35) * np.exp(-t * 24)

def sonaja(dur=0.06):
    t = tvec(int(dur * SR))
    return hp(ruido(dur), 6500) * np.exp(-t * 80)

def platillo_inverso(dur=1.6):
    n = int(dur * SR); t = tvec(n)
    return hp(ruido(dur), 3500) * (t / dur) ** 2.2

def crash(dur=2.2):
    n = int(dur * SR); t = tvec(n)
    return hp(ruido(dur), 2800) * np.exp(-t * 2.3) * 0.8

def bajo(m, dur):
    f = hz(m); x = saw(f, dur, detune=(-6, 6)) * 0.7 + sinus(f / 2, dur) * 0.6
    return lp(x, 650) * adsr(len(x), 0.004, 0.10, 0.65, 0.03)

def metal(ms, dur, vol=1.0):
    """acorde de metales: saws desafinadas, filtro algo abierto"""
    x = sum(saw(hz(m), dur, 0.002, (-9, 0, 9), 0.05) for m in ms) / len(ms)
    return lp(x, 2400) * adsr(len(x), 0.02, 0.12, 0.75, 0.06) * vol

def cuerdas(ms, dur, trem=0.0):
    x = sum(saw(hz(m), dur, 0.003, (-14, -5, 5, 14), 0.2) for m in ms) / len(ms)
    x = lp(x, 1500)
    e = adsr(len(x), min(0.5, dur * 0.4), 0.3, 0.85, min(0.5, dur * 0.3))
    if trem: e = e * (1 - trem + trem * (0.5 + 0.5 * np.sin(2 * np.pi * 11 * tvec(len(x)))))
    return x * e

def trompa(m, dur):
    x = saw(hz(m), dur, 0.006, (-8, 0, 8), 0.12) + 0.5 * saw(hz(m - 12), dur, 0.004, (0,), 0.12)
    return lp(x, 1900) * adsr(len(x), 0.03, 0.2, 0.82, min(0.08, dur * 0.3)) * 0.8

def rumor_grave(dur=1.4, f0=95, f1=38):
    n = int(dur * SR); t = tvec(n)
    f = f0 * (f1 / f0) ** (t / dur)
    x = lp(saw_f(np.cumsum(f) / SR), 220) + lp(ruido(dur), 300) * 0.5
    return x * np.sin(np.pi * t / dur) ** 0.7


# ------------------------------------------------------------------ musica de persecucion
ACORDES = {   # nombre: (nota raiz 0-11, intervalos)
    'Dm': (2, (0, 3, 7)), 'Bb': (10, (0, 4, 7)), 'Gm': (7, (0, 3, 7)), 'A': (9, (0, 4, 7)),
    'Eb': (3, (0, 4, 7)), 'C': (0, (0, 4, 7)), 'D5': (2, (0, 7, 12)), 'Eb5': (3, (0, 7, 12)),
}
PROG = (['Dm', 'Dm', 'Bb', 'Bb', 'Gm', 'Gm', 'A', 'A'] + ['Dm', 'Eb', 'Dm', 'Eb', 'Bb', 'C', 'A', 'A'] + ['D5', 'Eb5', 'D5', 'Eb5', 'Gm', 'Bb', 'A', 'A']
        + ['Dm', 'Dm', 'Bb', 'Bb', 'Gm', 'Gm', 'A', 'A'] + ['Dm', 'Eb', 'Dm', 'Eb', 'Gm', 'Gm', 'A', 'A'])
# melodia: por compas, lista de (paso de 16, largo en pasos, nota midi)
A = [[(0, 4, 62), (4, 2, 62), (6, 2, 63), (8, 4, 62), (12, 4, 57)], [(0, 4, 58), (4, 2, 57), (6, 2, 55), (8, 8, 57)],
     [(0, 4, 65), (4, 4, 62), (8, 4, 65), (12, 4, 70)], [(0, 6, 67), (6, 2, 65), (8, 8, 62)],
     [(0, 4, 67), (4, 4, 65), (8, 4, 62), (12, 4, 67)], [(0, 6, 70), (6, 2, 69), (8, 4, 67), (12, 4, 62)],
     [(0, 4, 69), (4, 2, 69), (6, 2, 70), (8, 4, 69), (12, 4, 73)], [(0, 8, 76), (8, 4, 73), (12, 4, 69)]]
B = [[(0, 2, 62), (2, 2, 65), (4, 2, 69), (6, 2, 65), (8, 2, 62), (10, 2, 65), (12, 2, 69), (14, 2, 74)],
     [(0, 2, 63), (2, 2, 67), (4, 2, 70), (6, 2, 67), (8, 2, 63), (10, 2, 67), (12, 2, 70), (14, 2, 75)],
     [(0, 2, 62), (2, 2, 65), (4, 2, 69), (6, 2, 65), (8, 4, 74), (12, 2, 72), (14, 2, 70)],
     [(0, 2, 63), (2, 2, 67), (4, 2, 70), (6, 2, 67), (8, 4, 75), (12, 2, 74), (14, 2, 70)],
     [(0, 4, 70), (4, 4, 74), (8, 4, 77), (12, 4, 74)], [(0, 4, 72), (4, 4, 76), (8, 4, 79), (12, 4, 76)],
     [(0, 4, 73), (4, 2, 76), (6, 2, 73), (8, 4, 69), (12, 4, 73)], [(0, 8, 76), (8, 8, 69)]]
C = [[(0, 8, 74), (8, 4, 72), (12, 4, 74)], [(0, 8, 75), (8, 4, 74), (12, 4, 75)], [(0, 8, 74), (8, 4, 77), (12, 4, 74)], [(0, 6, 75), (6, 2, 74), (8, 8, 70)],
     [(0, 4, 79), (4, 4, 74), (8, 4, 70), (12, 4, 74)], [(0, 4, 77), (4, 4, 74), (8, 4, 70), (12, 4, 74)], [(0, 4, 76), (4, 4, 73), (8, 4, 69), (12, 4, 73)], [(0, 16, 76)]]
E = B[:4] + [[(0, 4, 67), (4, 4, 70), (8, 4, 74), (12, 4, 70)], [(0, 8, 67), (8, 8, 62)],
             [(0, 2, 69), (2, 2, 73), (4, 2, 76), (6, 2, 73), (8, 4, 69), (12, 4, 73)], [(0, 16, 73)]]
MELODIA = A + B + C + A + E

def musica_persecucion():
    bpm = 150; paso = 60.0 / bpm / 4; compas = paso * 16; ncomp = 40
    mz = Mezcla(ncomp * compas)
    for b in range(ncomp):
        t0 = b * compas
        nombre = PROG[b]; pc, iv = ACORDES[nombre]
        trozo = b // 8                                   # 0 A, 1 B, 2 C, 3 A', 4 E
        intens = [1, 2, 3, 2, 3][trozo]
        raiz = 36 + pc
        if raiz > 46: raiz -= 12
        # --- bajo: galope «dum . dum-dum» en cada pulso
        for q in range(4):
            for k, (off, m) in enumerate([(0, raiz), (2, raiz), (3, raiz + (7 if (b + q) % 2 else 0))]):
                if intens == 1 and k == 2 and q % 2: continue
                mz.poner(t0 + (q * 4 + off) * paso, bajo(m, paso * (1.6 if off < 2 else 0.9)), 0.34 + 0.03 * intens)
        # --- bateria
        kicks = {1: [0, 8], 2: [0, 6, 8, 10], 3: [0, 3, 6, 8, 10, 12]}[intens]
        for s_ in kicks: mz.poner(t0 + s_ * paso, taiko(58), 0.62 if s_ in (0, 8) else 0.38)
        for s_ in ((4, 12) if intens > 1 or b % 8 >= 4 else ()):
            mz.poner(t0 + s_ * paso, tom(120 if s_ == 4 else 150), 0.5); 
            if intens >= 2: mz.poner(t0 + s_ * paso, caja(), 0.5)
        if b % 4 == 3:                                    # relleno de toms al final de cada 4 compases
            for k, f in enumerate((210, 180, 150, 120)): mz.poner(t0 + (12 + k) * paso, tom(f), 0.5, pan=(-0.4, -0.15, 0.15, 0.4)[k])
        if intens >= 2:
            for s_ in range(16): mz.poner(t0 + s_ * paso, sonaja(), 0.16 + 0.07 * (s_ % 2) + 0.04 * intens, pan=0.3 if s_ % 2 else -0.3)
        if b % 8 == 0: mz.poner(t0, crash(), 0.34 + 0.06 * intens); mz.poner(t0, rumor_grave(), 0.16)
        if b % 8 == 7: mz.poner(t0 + 8 * paso, platillo_inverso(8 * paso), 0.28)
        # --- cuerdas (colchon) y metales (golpes)
        tonos = [60 + pc + i for i in iv]
        mz.poner(t0, cuerdas(tonos, compas, trem=0.0 if intens == 1 else 0.55), 0.30 + 0.05 * intens, pan=-0.2)
        mz.poner(t0, cuerdas([m + 12 for m in tonos[:2]], compas, trem=0.55 if intens > 1 else 0.0), 0.18 + 0.04 * intens, pan=0.25)
        stabs = {1: [0], 2: [0, 6, 10], 3: [0, 3, 6, 10, 12]}[intens]
        for s_ in stabs:
            mz.poner(t0 + s_ * paso, metal([48 + pc, 55 + pc, 60 + pc] if iv[1] != 4 and iv[1] != 7 else [48 + pc, 52 + pc, 55 + pc], paso * 1.5), 0.55 + 0.07 * intens)
        # --- melodia (trompas)
        oct_ = 12 if (trozo == 3 and b % 8 >= 4) else 0
        for (s_, largo, m) in MELODIA[b]:
            mz.poner(t0 + s_ * paso, trompa(m + oct_, largo * paso * 0.97), 0.85 + 0.08 * (trozo == 2), pan=0.12)
            if trozo in (2, 4):
                mz.poner(t0 + s_ * paso, trompa(m - 12, largo * paso * 0.97), 0.42, pan=-0.12)
    y = con_reverb(mz.estereo(), 0.22, 1.7, 0.5)
    y = np.stack([hp(y[:, 0], 38), hp(y[:, 1], 38)], 1)
    return normalizar_rms(y, -14.5, 0.8)


# ------------------------------------------------------------------ musica de espera (selva inquietante, bucle de 32 s)
def musica_espera():
    seg = 32.0; n = int(seg * SR); t = tvec(n)
    mz = Mezcla(seg)
    # zumbido grave (D1 + A1) con filtro que respira
    drone = (saw(hz(26), seg, 0.001, (-7, 7)) * 0.8 + saw(hz(33), seg, 0.0015, (-9, 9)) * 0.45 + sine_f(osc_fase(hz(14), n)) * 0.7)
    lfo = 0.5 + 0.5 * np.sin(2 * np.pi * t / 16.0)
    drone = lp(drone, 260) * (0.6 + 0.4 * lfo)
    mz.poner(0, drone, 0.22)
    # latido lento (60 bpm) y un redoble lejano cada 8 s
    for k in range(32): mz.poner(k * 1.0, taiko(52, 0.6), 0.2 if k % 2 == 0 else 0.11)
    for k in range(4): 
        for q in range(6): mz.poner(k * 8.0 + 6.0 + q * 0.09, tom(100 + q * 6), 0.18)
    # crujidos de selva: pajaros (barridos agudos), insectos (ruido filtrado con trino) y ramas
    for k in range(26):
        t0 = RNG.uniform(0.3, 31.0)
        f0 = RNG.uniform(1800, 3600); dur = RNG.uniform(0.12, 0.30)
        nn = int(dur * SR); tt = tvec(nn)
        f = f0 * (1 + 0.45 * np.sin(2 * np.pi * RNG.uniform(6, 14) * tt) * np.exp(-tt * 6) + RNG.uniform(-0.3, 0.3) * tt / dur)
        pio = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 14) * (0.2 + 0.8 * (np.sin(np.pi * tt / dur)))
        mz.poner(t0, pio, RNG.uniform(0.18, 0.4), pan=RNG.uniform(-0.8, 0.8))
    insectos = bp(ruido(seg), 4200, 7000) * (0.5 + 0.5 * np.sin(2 * np.pi * 17 * t)) ** 3 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 7.0))
    mz.poner(0, insectos, 0.16)
    mz.poner(0, lp(ruido(seg), 1400) * (0.35 + 0.35 * np.sin(2 * np.pi * t / 5.3 + 1)), 0.12)
    # amenaza: dos gruñidos graves lejanos
    for t0 in (11.0, 25.5):
        mz.poner(t0, rumor_grave(2.2, 80, 34), 0.16)
    # pocas notas sueltas de cuerdas oscuras (D menor)
    for t0, m in ((2.0, 50), (9.0, 53), (17.0, 45), (23.0, 50), (28.0, 57)):
        mz.poner(t0, cuerdas([m, m + 7, m + 12], 4.5), 0.34, pan=RNG.uniform(-0.4, 0.4))
    y = con_reverb(mz.estereo(), 0.3, 2.0, 0.7)
    f = int(0.4 * SR); y[:f] *= np.linspace(0, 1, f)[:, None]; y[-f:] *= np.linspace(1, 0, f)[:, None]
    y = np.stack([hp(y[:, 0], 30), hp(y[:, 1], 30)], 1)
    return normalizar_rms(y, -19.0, 0.7)


# ------------------------------------------------------------------ efectos
def campana(f, dur, d=3.5):
    t = tvec(int(dur * SR))
    return (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * 2 * f * t) + 0.12 * np.sin(2 * np.pi * 3.01 * f * t)) * np.exp(-t * d)

def fx_estrella():
    mz = Mezcla(0.5)
    for k, m in enumerate((88, 93)): mz.poner(k * 0.055, campana(hz(m), 0.4, 7.0), 0.7)
    mz.poner(0.0, campana(hz(100), 0.3, 12.0), 0.25)
    return normalizar(con_reverb(mz.estereo(), 0.12, 0.5, 0.15), 0.8, False)

def fx_estrella_grande():
    mz = Mezcla(0.9)
    for k, m in enumerate((88, 92, 95, 100)): mz.poner(k * 0.06, campana(hz(m), 0.8, 4.0), 0.6 - 0.04 * k)
    mz.poner(0.2, campana(hz(107), 0.5, 7.0), 0.22)
    mz.poner(0.0, hp(ruido(0.5), 5000) * np.exp(-tvec(int(0.5 * SR)) * 8), 0.15)
    return normalizar(con_reverb(mz.estereo(), 0.18, 0.7, 0.2), 0.85, False)

def fx_bien():
    mz = Mezcla(0.6)
    mz.poner(0.0, campana(hz(84), 0.5, 6.0), 0.7); mz.poner(0.09, campana(hz(91), 0.5, 6.0), 0.7)
    mz.poner(0.0, hp(ruido(0.12), 3500) * np.exp(-tvec(int(0.12 * SR)) * 30), 0.2)
    return normalizar(con_reverb(mz.estereo(), 0.1, 0.4, 0.12), 0.7, False)

def fx_fallo():
    dur = 0.65; n = int(dur * SR); t = tvec(n)
    f = 300 * (110 / 300) ** (t / 0.45); f = np.where(t < 0.45, f, 110)
    x = (saw_f(np.cumsum(f) / SR) * 0.6 + np.sin(2 * np.pi * np.cumsum(f * 1.06) / SR) * 0.4)
    x = lp(x, 900) * np.exp(-t * 4.5) * np.minimum(t / 0.01, 1)
    golpe = lp(ruido(0.2), 500) * np.exp(-tvec(int(0.2 * SR)) * 28) * 1.2
    x[:len(golpe)] += golpe
    return normalizar(mono2(x), 0.85, False)

def fx_caida():
    dur = 1.5; n = int(dur * SR); t = tvec(n)
    f = np.where(t < 1.0, 1300 * (180 / 1300) ** (t / 1.0), 180)
    silbido = np.sin(2 * np.pi * np.cumsum(f) / SR) * (1 - np.clip((t - 0.8) / 0.2, 0, 1)) * 0.5
    silbido *= 1 + 0.25 * np.sin(2 * np.pi * 9 * t)
    plop = (lp(ruido(0.5), 380) * np.exp(-tvec(int(0.5 * SR)) * 9)) * 1.4
    chisporroteo = hp(ruido(0.6), 2500) * np.exp(-tvec(int(0.6 * SR)) * 6) * (RNG.uniform(0, 1, int(0.6 * SR)) > 0.7) * 0.3
    x = silbido
    i = int(0.95 * SR); x[i:i + len(plop)] += plop[: n - i]; x[i:i + len(chisporroteo)] += chisporroteo[: n - i]
    return normalizar(mono2(x), 0.85, False)

def fx_pisada():
    dur = 0.7; n = int(dur * SR); t = tvec(n)
    f = 66 * (1 + 1.3 * np.exp(-t * 20))
    x = (np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 6.5) * 0.8 + lp(ruido(dur), 260) * np.exp(-t * 11) * 0.9 + lp(ruido(dur), 90) * np.exp(-t * 4) * 0.8
         + bp(ruido(dur), 180, 900) * np.exp(-t * 16) * 1.1 + np.sin(2 * np.pi * 240 * t) * np.exp(-t * 30) * 0.5)
    x *= np.minimum(t / 0.004, 1)
    return normalizar(mono2(x), 0.9, True)

def fx_victoria():
    mz = Mezcla(3.2)
    # timbales y fanfarria en D mayor: D F# A D, acorde final
    for k in range(6): mz.poner(k * 0.06, tom(110 + 10 * k), 0.4)
    mz.poner(0.4, taiko(58), 0.9)
    for k, ms in enumerate(([62, 66, 69], [66, 69, 74], [69, 74, 78])):
        mz.poner(0.4 + k * 0.28, metal(ms, 0.45), 0.7)
    mz.poner(1.3, metal([62, 66, 69, 74], 1.7), 0.9)
    mz.poner(1.3, cuerdas([50, 57, 62, 66], 1.9), 0.4)
    mz.poner(1.3, crash(2.0), 0.35)
    for k, m in enumerate((93, 97, 100)): mz.poner(1.4 + k * 0.1, campana(hz(m), 1.6, 3.0), 0.25)
    return normalizar(con_reverb(mz.estereo(), 0.25, 1.4, 0.5), 0.88)

def fx_derrota():
    mz = Mezcla(2.6)
    mz.poner(0.0, taiko(48, 1.0), 1.0)
    for k, ms in enumerate(([57, 60, 64], [56, 59, 62], [53, 56, 59])):
        mz.poner(0.25 + k * 0.45, metal(ms, 0.9), 0.7)
    mz.poner(0.2, rumor_grave(2.2, 90, 30), 0.7)
    mz.poner(1.6, cuerdas([41, 48, 51, 60], 1.0), 0.5)
    return normalizar(con_reverb(mz.estereo(), 0.25, 1.6, 0.6), 0.88)

def fx_listo():
    mz = Mezcla(0.5)
    mz.poner(0.0, campana(hz(79), 0.45, 5.5), 0.7); mz.poner(0.07, campana(hz(86), 0.45, 5.5), 0.55)
    return normalizar(con_reverb(mz.estereo(), 0.1, 0.4, 0.12), 0.7, False)


def main():
    guardar('musica_persecucion', musica_persecucion())
    guardar('musica_espera', musica_espera())
    for nombre, fx in (('estrella', fx_estrella), ('estrella_grande', fx_estrella_grande), ('bien', fx_bien), ('fallo', fx_fallo), ('caida', fx_caida),
                       ('pisada', fx_pisada), ('victoria', fx_victoria), ('derrota', fx_derrota), ('listo', fx_listo)):
        guardar(nombre, fx())

if __name__ == '__main__':
    main()
