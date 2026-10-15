"""Musica y sonidos del nivel 3 de la huida («la huida de los michialiens»: el espacio, tierno y con prisa). Todo sintetizado con numpy/scipy (original) y
codificado a .ogg con ffmpeg.

Uso: python3 generar_sonidos_cosmos.py   (necesita numpy, scipy y ffmpeg con libvorbis; usa las utilidades de generar_sonidos.py)
Escribe en src/main/resources/assets/emi_huida/sounds/:
  cosmos_intro.ogg          10,5 s (la presentacion: un arpegio de celesta que se acelera y un redoble)    -> DEBE ser 210 ticks en Sonidos.duracion()
  cosmos.ogg                120,0 s (140 bpm, 70 compases, la persecucion; se relanza al acabar)          -> DEBE ser 2400 ticks
  musica_espera_cosmos.ogg  32,0 s  (nana espacial)                                                       -> DEBE ser 640 ticks
  horda, aplastado, aparece (efectos)
"""
import numpy as np
import generar_sonidos as g
from generar_sonidos import SR, RNG, hz, tvec, lp, hp, bp, ruido, Mezcla, con_reverb, normalizar, normalizar_rms, guardar, mono2, taiko, tom, caja, sonaja, bajo, platillo_inverso, crash, cuerdas, rumor_grave

# ------------------------------------------------------------------ instrumentos
def celesta(m, dur=0.9, vol=1.0):
    """campanita de celesta: seno y dos armonicos que se apagan rapido"""
    f = hz(m); t = tvec(int(dur * SR))
    x = (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 7) + 0.25 * np.sin(2 * np.pi * 4.01 * f * t) * np.exp(-t * 16)) * np.exp(-t * 4.2)
    return x * np.minimum(t / 0.003, 1) * vol

def chip(m, dur, duty=0.25, vib=0.004):
    """melodia de chiptune suave (onda cuadrada estrecha con vibrato y filtro)"""
    n = int(dur * SR); t = tvec(n); f = hz(m) * (1 + vib * np.sin(2 * np.pi * 5.5 * np.maximum(t - 0.12, 0)) * np.minimum(t / 0.3, 1))
    fase = np.cumsum(f) / SR
    x = np.where((fase % 1.0) < duty, 1.0, -1.0) * 0.55 + g.sine_f(fase) * 0.45
    return lp(x, 4200) * g.adsr(n, 0.006, 0.08, 0.72, min(0.06, dur * 0.3))

def pluck(m, dur=0.25):
    n = int(dur * SR); t = tvec(n)
    x = g.saw(hz(m), dur, 0.0, (-5, 5))
    return lp(x, 2600) * np.exp(-t * 11) * np.minimum(t / 0.002, 1)

def pad(ms, dur):
    x = sum(g.saw(hz(m), dur, 0.002, (-10, -3, 3, 10), 0.15) for m in ms) / len(ms)
    return lp(x, 1700) * g.adsr(len(x), min(0.6, dur * 0.45), 0.3, 0.8, min(0.5, dur * 0.3))

def kick(f0=60, dur=0.32):
    n = int(dur * SR); t = tvec(n)
    f = f0 * (1 + 2.4 * np.exp(-t * 32))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 9) + lp(ruido(dur), 800) * np.exp(-t * 70) * 0.3

def chasquido(dur=0.14):
    n = int(dur * SR); t = tvec(n)
    return (bp(ruido(dur), 1800, 9000) * 0.75 + np.sin(2 * np.pi * 240 * t) * 0.25) * np.exp(-t * 28)

def acorde(nombre):
    raiz, tipo = ACORDES[nombre]
    return raiz, (0, 3, 7) if tipo == 'm' else (0, 4, 7)

ACORDES = {'Am': (9, 'm'), 'F': (5, 'M'), 'C': (0, 'M'), 'G': (7, 'M'), 'Em': (4, 'm'), 'Dm': (2, 'm')}
# notas permitidas en la melodia sobre cada acorde (clases de nota): tonos del acorde y vecinos de la pentatonica
POOL = {'Am': (9, 0, 4, 2, 7), 'F': (5, 9, 0, 7, 2), 'C': (0, 4, 7, 2, 9), 'G': (7, 2, 11, 4, 9), 'Em': (4, 7, 11, 9, 2), 'Dm': (2, 5, 9, 0, 7)}

# la forma de la cancion: 70 compases = 120 s a 140 bpm. 0 = estrofa, 1 = puente (menos bateria), 2 = estribillo (todo)
SECCIONES = [('A', 0), ('A', 0), ('B', 1), ('A', 0), ('C', 2), ('B', 1), ('A', 0), ('C', 2), ('F', 1)]
PROG = {'A': ['Am', 'F', 'C', 'G', 'Am', 'F', 'C', 'G'], 'B': ['F', 'G', 'Am', 'Am', 'F', 'G', 'C', 'C'], 'C': ['Am', 'F', 'C', 'G', 'F', 'G', 'Am', 'G'], 'F': ['Am', 'F', 'C', 'G', 'Am', 'Am']}
RITMOS = [(0, 3, 6, 8, 10, 12, 14), (0, 2, 4, 6, 8, 12), (0, 4, 8, 10, 12, 14), (0, 2, 4, 6, 8, 10, 12, 14), (0, 3, 6, 8, 11, 14), (0, 6, 8, 12)]

def nota_cerca(pool, previa, lo=69, hi=88):
    """la nota del pool mas cercana (con un pasito aleatorio) a la anterior"""
    cand = [m for m in range(lo, hi + 1) if m % 12 in pool]
    cand.sort(key=lambda m: abs(m - previa) + RNG.uniform(0, 4.5))
    return cand[0]

def musica_cosmos():
    bpm = 140; paso = 60.0 / bpm / 4; compas = paso * 16
    prog = [(sec, tipo, c) for sec, tipo in SECCIONES for c in PROG[sec]]
    ncomp = len(prog)
    assert ncomp == 70, ncomp
    mz = Mezcla(ncomp * compas)
    previa = 81
    for b, (sec, tipo, nombre) in enumerate(prog):
        t0 = b * compas
        pc, iv = acorde(nombre)
        raiz = 33 + pc if pc >= 5 else 45 + pc
        if raiz > 45: raiz -= 12
        # --- bajo: corcheas con salto de octava
        for k in range(8):
            m = raiz + (12 if k % 4 == 3 else 0) + (7 if k % 8 == 6 else 0)
            mz.poner(t0 + k * 2 * paso, bajo(m, paso * 1.7), 0.36 + 0.04 * tipo)
        # --- bateria
        if tipo >= 1:
            for q in range(4): mz.poner(t0 + q * 4 * paso, kick(), 0.8 if tipo == 2 else 0.6)
        else:
            for q in (0, 2): mz.poner(t0 + q * 4 * paso, kick(), 0.55)
        for s_ in (4, 12): mz.poner(t0 + s_ * paso, chasquido(), 0.5 if tipo != 1 else 0.3)
        if tipo >= 1:
            for s_ in range(16): mz.poner(t0 + s_ * paso, sonaja(), 0.12 + 0.06 * (s_ % 2) + 0.03 * tipo, pan=0.3 if s_ % 2 else -0.3)
        if tipo == 2 and b % 4 == 3:
            for k in range(4): mz.poner(t0 + (12 + k) * paso, tom(200 - 18 * k), 0.38, pan=(-0.3, -0.1, 0.1, 0.3)[k])
        if sec != 'F' and b % 8 == 0 and tipo >= 1: mz.poner(t0, crash(), 0.22 + 0.05 * tipo)
        # --- colchon de cuerdas suaves
        tonos = [57 + pc + i for i in iv]
        mz.poner(t0, pad(tonos, compas), 0.26 + 0.05 * tipo, pan=-0.2)
        # --- arpegio de celesta (16 pasos) sobre los tonos del acorde: la nota cuqui
        for k in range(16):
            m = 69 + (pc - 9) % 12 + iv[(k * 2 + (1 if k % 4 == 3 else 0)) % 3] + (12 if (k // 4) % 2 else 0)
            while m > 91: m -= 12
            mz.poner(t0 + k * paso, celesta(m, 0.7), 0.22 + 0.08 * (k % 4 == 0) + 0.04 * tipo, pan=-0.35 + 0.7 * ((k % 8) / 7.0))
        # --- melodia: ritmo elegido por el compas y notas del pool del acorde
        ritmo = RITMOS[(b + (3 if tipo == 2 else 0)) % len(RITMOS)]
        for i, s_ in enumerate(ritmo):
            sig = ritmo[i + 1] if i + 1 < len(ritmo) else 16
            largo = (sig - s_) * (0.9 if sig - s_ <= 2 else 0.8)
            if tipo == 1 and i % 2: continue
            previa = nota_cerca(POOL[nombre], previa + (2 if tipo == 2 and i == 0 else 0), 72 if tipo == 2 else 69, 91 if tipo == 2 else 86)
            mz.poner(t0 + s_ * paso, chip(previa, largo * paso), 0.5 + 0.12 * (tipo == 2), pan=0.12)
            if tipo == 2: mz.poner(t0 + s_ * paso, chip(previa - 12, largo * paso, 0.5), 0.2, pan=-0.12)
    y = con_reverb(mz.estereo(), 0.2, 1.6, 0.45)
    y = np.stack([hp(y[:, 0], 40), hp(y[:, 1], 40)], 1)
    y = normalizar_rms(y, -16.5, 0.72)
    f = int(0.05 * SR); y[-f:] *= np.linspace(1, 0.0, f)[:, None]          # el bucle suena a «un compas mas»: sin corte seco
    return y

def musica_cosmos_intro():
    seg = 10.5; n = int(seg * SR); t = tvec(n)
    mz = Mezcla(seg)
    # arpegio de celesta en La menor que sube y se acelera (de corcheas a semifusas) hasta el redoble
    notas = [57, 60, 64, 67, 69, 72, 76, 79, 81, 84, 88, 91]
    tt = 0.0; k = 0
    while tt < seg - 0.5:
        frac = tt / seg
        m = notas[min(len(notas) - 1, int(frac * len(notas) * 1.0) + (k % 3 == 2) * 0)]
        m2 = m + (0, 12)[k % 2] if m < 80 else m
        mz.poner(tt, celesta(min(m2, 93), 1.0), 0.24 + 0.2 * frac, pan=-0.4 + 0.8 * ((k % 6) / 5.0))
        tt += 0.34 * (1 - 0.8 * frac ** 1.3) + 0.045
        k += 1
    # colchon que crece y la nota grave de La
    mz.poner(0, pad([45, 52, 57, 60], seg), 0.4)
    mz.poner(0, lp(g.saw(hz(33), seg, 0.0012, (-8, 8)), 240) * (0.3 + 0.7 * (t / seg) ** 1.4), 0.4)
    # latido de bombo que se acelera
    tb = 1.0
    while tb < seg - 0.4:
        mz.poner(tb, kick(52, 0.35), 0.35 + 0.35 * tb / seg)
        tb += 0.9 - 0.6 * (tb / seg)
    # brillos aleatorios
    for _ in range(26):
        t0 = RNG.uniform(0.2, seg - 1.0)
        mz.poner(t0, celesta(RNG.choice([88, 91, 93, 84]), 0.6), RNG.uniform(0.08, 0.2), pan=RNG.uniform(-0.8, 0.8))
    # redoble de caja y platillo inverso en los ultimos 2,5 s
    tr = seg - 2.5
    while tr < seg - 0.08:
        mz.poner(tr, chasquido(0.1), 0.18 + 0.4 * (tr - (seg - 2.5)) / 2.5)
        tr += 0.2 * (1 - 0.7 * (tr - (seg - 2.5)) / 2.5) + 0.03
    mz.poner(seg - 2.6, platillo_inverso(2.5), 0.4)
    y = con_reverb(mz.estereo(), 0.25, 1.7, 0.5)
    f = int(0.5 * SR); y[:f] *= np.linspace(0, 1, f)[:, None]
    y = np.stack([hp(y[:, 0], 35), hp(y[:, 1], 35)], 1)
    return normalizar_rms(y, -17.0, 0.72)

def musica_espera_cosmos():
    seg = 32.0; n = int(seg * SR); t = tvec(n)
    mz = Mezcla(seg)
    # nana espacial en Do mayor (pentatonica), cuatro tiempos a 60 bpm (8 compases = 32 s): celesta lenta con eco
    nana = [(72, 76, 79, 76), (74, 77, 81, 77), (76, 79, 83, 79), (72, 79, 84, 79), (77, 81, 84, 81), (76, 79, 83, 79), (74, 77, 81, 79), (72, 76, 79, 72)]
    for c, compas in enumerate(nana):
        for k, m in enumerate(compas):
            tt = (c * 4 + k) * 1.0
            mz.poner(tt, celesta(m, 2.2), 0.42 if k == 0 else 0.3, pan=-0.3 + 0.2 * k)
            if k == 0: mz.poner(tt, celesta(m - 12, 3.0), 0.26, pan=0.2)
        mz.poner(c * 4.0, pad([48 + (0 if c % 4 in (0, 3) else 2 if c % 4 == 1 else 4), 55, 60, 64], 4.0), 0.3)
    drone = g.saw(hz(36), seg, 0.001, (-6, 6)) * 0.5 + g.sine_f(g.osc_fase(hz(24), n)) * 0.7
    mz.poner(0, lp(drone, 260) * (0.6 + 0.4 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 16.0))), 0.2)
    for _ in range(30):
        mz.poner(RNG.uniform(0.2, 31.0), celesta(RNG.choice([84, 88, 91, 93]), 1.0), RNG.uniform(0.06, 0.16), pan=RNG.uniform(-0.8, 0.8))
    y = con_reverb(mz.estereo(), 0.35, 2.2, 0.8)
    f = int(0.4 * SR); y[:f] *= np.linspace(0, 1, f)[:, None]; y[-f:] *= np.linspace(1, 0, f)[:, None]
    y = np.stack([hp(y[:, 0], 30), hp(y[:, 1], 30)], 1)
    return normalizar_rms(y, -20.0, 0.7)

# ------------------------------------------------------------------ efectos
def miau(f0=620, dur=0.42):
    """un maullido sintetizado: saw con formantes que sube y baja"""
    n = int(dur * SR); t = tvec(n)
    f = f0 * (1 + 0.55 * np.sin(np.pi * np.minimum(t / dur, 1)) - 0.25 * (t / dur))
    x = g.saw_f(np.cumsum(f) / SR)
    x = bp(x, 700, 1600) * 1.2 + bp(x, 2200, 3200) * 0.4
    return x * np.sin(np.pi * t / dur) ** 0.6 * np.minimum(t / 0.02, 1)

def fx_horda():
    seg = 3.6; n = int(seg * SR); t = tvec(n)
    mz = Mezcla(seg)
    # estampida: pisadas de muchas patas (golpes sordos a unos 7 por segundo con tonos y momentos al azar)
    for k in range(70):
        t0 = k / 70.0 * (seg - 0.5) + RNG.uniform(0, 0.05)
        amp = 0.35 * np.sin(np.pi * t0 / seg) ** 0.7
        mz.poner(t0, taiko(RNG.uniform(70, 120), 0.18), amp, pan=RNG.uniform(-0.6, 0.6))
    mz.poner(0, lp(ruido(seg), 380) * (np.sin(np.pi * t / seg) ** 1.2), 0.5)
    for k in range(9):
        mz.poner(RNG.uniform(0.2, seg - 0.8), miau(RNG.uniform(520, 900), RNG.uniform(0.3, 0.5)), RNG.uniform(0.13, 0.26), pan=RNG.uniform(-0.8, 0.8))
    y = con_reverb(mz.estereo(), 0.2, 1.2, 0.35)
    y = np.stack([hp(y[:, 0], 40), hp(y[:, 1], 40)], 1)
    return normalizar(y, 0.7)

def fx_aplastado():
    seg = 1.3; n = int(seg * SR); t = tvec(n)
    mz = Mezcla(seg)
    # «boing-plaf»: un seno que cae, el golpe y un chirrido de goma al desinflarse
    f = 700 * np.exp(-t * 7) + 70
    mz.poner(0, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 5) * 0.8, 1.0)
    mz.poner(0, lp(ruido(0.2), 1400) * np.exp(-tvec(int(0.2 * SR)) * 22), 0.8)
    mz.poner(0.18, lp(g.saw_f(np.cumsum(np.linspace(1500, 380, int(0.7 * SR))) / SR), 3200) * np.exp(-tvec(int(0.7 * SR)) * 4), 0.28)
    mz.poner(0.55, celesta(60, 0.7), 0.2)
    y = mz.estereo()
    return normalizar(y, 0.7)

def fx_aparece():
    seg = 2.4; mz = Mezcla(seg)
    pent = [72, 74, 76, 79, 81, 84, 86, 88, 91, 93]
    for k, m in enumerate(pent):
        mz.poner(k * 0.07, celesta(m, 1.2), 0.3 + 0.03 * k, pan=-0.6 + 0.12 * k)
    for k, m in enumerate((84, 88, 91, 96)):
        mz.poner(0.95 + k * 0.03, celesta(m, 1.4), 0.28, pan=0.0)
    tt = tvec(int(1.6 * SR))
    mz.poner(0.05, hp(ruido(1.6), 5000) * np.sin(np.pi * tt / 1.6) ** 2, 0.15)
    mz.poner(0.9, pad([60, 64, 67, 72], 1.4), 0.4)
    y = con_reverb(mz.estereo(), 0.3, 1.6, 0.6)
    return normalizar(y, 0.7)

def main():
    guardar('cosmos_intro', musica_cosmos_intro())
    guardar('cosmos', musica_cosmos())
    guardar('musica_espera_cosmos', musica_espera_cosmos())
    for nombre, fx in (('horda', fx_horda), ('aplastado', fx_aplastado), ('aparece', fx_aparece)):
        guardar(nombre, fx())

if __name__ == '__main__':
    main()
