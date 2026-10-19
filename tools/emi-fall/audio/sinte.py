#!/usr/bin/env python3
"""Biblioteca de sintesis de Emi Survivors: osciladores sin aliasing (PolyBLEP), envolventes, filtros, reverb, eco, bateria y unos cuantos «instrumentos».
La usan musica.py (las canciones) y sonidos.py (los efectos). Todo es sintesis propia con numpy/scipy; no hay ninguna muestra de nadie.

Convenciones: SR = 44100 Hz; las notas se dan como numero MIDI (69 = La 440) o como nombre ('C#4', 'Bb3'); todo se mezcla en coma flotante y solo al final se
normaliza y se escribe a .wav (que ffmpeg pasa a .ogg)."""
import os
import re
import subprocess
import wave

import numpy as np
from scipy import signal as sg

SR = 44100
ALEATORIO = np.random.default_rng(20261016)


# ----------------------------------------------------------------------------------------------------------------------------------------- notas

_PC = {'C': 0, 'C#': 1, 'Db': 1, 'D': 2, 'D#': 3, 'Eb': 3, 'E': 4, 'F': 5, 'F#': 6, 'Gb': 6, 'G': 7, 'G#': 8, 'Ab': 8, 'A': 9, 'A#': 10, 'Bb': 10, 'B': 11}


def midi(nombre):
    """'C#4' -> 61 (C4 = 60)."""
    m = re.fullmatch(r'([A-G][#b]?)(-?\d)', nombre)
    if not m:
        raise ValueError('nota mal escrita: ' + nombre)
    return 12 * (int(m.group(2)) + 1) + _PC[m.group(1)]


def hz(m):
    return 440.0 * 2.0 ** ((np.asarray(m, dtype=float) - 69.0) / 12.0)


# ----------------------------------------------------------------------------------------------------------------------------------------- osciladores

def _fase(f, n):
    f = np.broadcast_to(np.asarray(f, dtype=float), (n,))
    dt = f / SR
    ph = np.cumsum(dt) - dt[0]
    return ph % 1.0, dt


def _blep(t, dt):
    out = np.zeros_like(t)
    m1 = t < dt
    x = t[m1] / dt[m1]
    out[m1] = x + x - x * x - 1.0
    m2 = t > 1.0 - dt
    x = (t[m2] - 1.0) / dt[m2]
    out[m2] = x * x + x + x + 1.0
    return out


def saw(f, n):
    t, dt = _fase(f, n)
    return 2.0 * t - 1.0 - _blep(t, dt)


def pulso(f, n, duty=0.5):
    t, dt = _fase(f, n)
    v = np.where(t < duty, 1.0, -1.0)
    v += _blep(t, dt)
    v -= _blep((t - duty) % 1.0, dt)
    return v - (2.0 * duty - 1.0)                 # sin componente continua


def tri(f, n):
    t, _ = _fase(f, n)
    return 4.0 * np.abs(t - 0.5) - 1.0


def seno(f, n):
    t, _ = _fase(f, n)
    return np.sin(2.0 * np.pi * t)


def ruido(n):
    return ALEATORIO.uniform(-1.0, 1.0, n)


def vibrato(f, n, prof=0.3, vel=5.5, retraso=0.0, subida=0.15):
    """Frecuencia con vibrato (prof en semitonos); empieza tras `retraso` segundos y sube en `subida`."""
    t = np.arange(n) / SR
    amp = np.clip((t - retraso) / max(subida, 1e-3), 0.0, 1.0)
    return f * 2.0 ** (prof * amp * np.sin(2.0 * np.pi * vel * t) / 12.0)


# ----------------------------------------------------------------------------------------------------------------------------------------- envolventes

def env(n, a=0.005, d=0.12, s=0.6, r=0.04):
    """Ataque lineal, caida exponencial hasta el nivel s y, al final, una bajada lineal de r segundos."""
    t = np.arange(n) / SR
    a = max(a, 1e-4)
    d = max(d, 1e-4)
    e = np.where(t < a, t / a, s + (1.0 - s) * np.exp(-(t - a) / d))
    rel = min(int(r * SR), n)
    if rel > 0:
        e[n - rel:] *= np.linspace(1.0, 0.0, rel)
    return e


def exp_caida(n, tau):
    return np.exp(-np.arange(n) / SR / max(tau, 1e-4))


def fundido(x, entrada=0.003, salida=0.01):
    """Entrada y salida cortas para que ningun sonido empiece o acabe con un chasquido."""
    x = np.array(x, dtype=float)
    a, b = int(entrada * SR), int(salida * SR)
    col = (slice(None), None) if x.ndim == 2 else (slice(None),)
    if a > 0 and len(x) > a:
        x[:a] *= np.linspace(0.0, 1.0, a)[col]
    if b > 0 and len(x) > b:
        x[-b:] *= np.linspace(1.0, 0.0, b)[col]
    return x


# ----------------------------------------------------------------------------------------------------------------------------------------- filtros

def pasabajos(x, fc, orden=2):
    fc = min(fc, SR * 0.49)
    return sg.sosfilt(sg.butter(orden, fc / (SR / 2), 'low', output='sos'), x)


def pasaaltos(x, fc, orden=2):
    return sg.sosfilt(sg.butter(orden, fc / (SR / 2), 'high', output='sos'), x)


def pasabanda(x, f1, f2, orden=2):
    return sg.sosfilt(sg.butter(orden, [f1 / (SR / 2), min(f2, SR * 0.49) / (SR / 2)], 'band', output='sos'), x)


def barrido_pasabajos(x, fcs, bloque=256, orden=2):
    """Pasabajos con la frecuencia de corte cambiando en el tiempo (fcs: una frecuencia por muestra; se actualiza cada `bloque`)."""
    x = np.asarray(x, dtype=float)
    out = np.zeros_like(x)
    zi = None
    for i in range(0, len(x), bloque):
        fc = float(np.clip(fcs[min(i, len(fcs) - 1)], 40.0, SR * 0.45))
        sos = sg.butter(orden, fc / (SR / 2), 'low', output='sos')
        if zi is None or zi.shape[0] != sos.shape[0]:
            zi = np.zeros((sos.shape[0], 2))
        out[i:i + bloque], zi = sg.sosfilt(sos, x[i:i + bloque], zi=zi)
    return out


def barrido_pasabanda(x, fcs, ancho=0.5, bloque=256):
    """Pasabanda de ancho relativo `ancho` (una octava = 1) que sigue las frecuencias centrales fcs (una por muestra)."""
    x = np.asarray(x, dtype=float)
    out = np.zeros_like(x)
    zi = None
    for i in range(0, len(x), bloque):
        fc = float(np.clip(fcs[min(i, len(fcs) - 1)], 60.0, SR * 0.4))
        f1, f2 = fc / 2 ** (ancho / 2), fc * 2 ** (ancho / 2)
        sos = sg.butter(2, [f1 / (SR / 2), f2 / (SR / 2)], 'band', output='sos')
        if zi is None or zi.shape[0] != sos.shape[0]:
            zi = np.zeros((sos.shape[0], 2))
        out[i:i + bloque], zi = sg.sosfilt(sos, x[i:i + bloque], zi=zi)
    return out


def saturar(x, g=2.0):
    return np.tanh(x * g) / np.tanh(g)


# ----------------------------------------------------------------------------------------------------------------------------------------- instrumentos de una nota

def v_pulso(m, dur, duty=0.5, a=0.004, d=0.15, s=0.55, r=0.03, vib=0.0, brillo=9000.0):
    n = int(dur * SR)
    f = hz(m) if np.isscalar(m) else m
    if vib:
        f = vibrato(f, n, prof=vib, vel=5.5, retraso=0.12)
    return pasabajos(pulso(f, n, duty), brillo, 1) * env(n, a, d, s, r)


def v_tri(m, dur, a=0.005, d=0.3, s=0.75, r=0.04):
    n = int(dur * SR)
    return tri(hz(m), n) * env(n, a, d, s, r)


def v_saw(m, dur, a=0.005, d=0.2, s=0.5, r=0.04, brillo=5000.0):
    n = int(dur * SR)
    return pasabajos(saw(hz(m), n), brillo, 2) * env(n, a, d, s, r)


def v_seno(m, dur, a=0.005, d=0.4, s=0.7, r=0.05):
    n = int(dur * SR)
    return seno(hz(m), n) * env(n, a, d, s, r)


def v_pad(m, dur, a=0.25, r=0.4, brillo=1800.0, abre=0.0):
    """Tres sierras desafinadas con pasabajos (un colchon): `abre` > 0 hace que el filtro se abra durante la nota."""
    n = int(dur * SR + r * SR)
    x = (saw(hz(m) * 2 ** (-0.07 / 12), n) + saw(hz(m), n) + saw(hz(m) * 2 ** (0.07 / 12), n)) / 3.0
    if abre:
        fcs = brillo * (1.0 + abre * np.minimum(np.arange(n) / SR / max(dur, 0.1), 1.0))
        x = barrido_pasabajos(x, fcs)
    else:
        x = pasabajos(x, brillo, 2)
    e = env(n, a, 1.0, 1.0, r)
    return x * e


def v_organo(m, dur, a=0.03, r=0.12, trem=0.0):
    """Organo de tubos sencillo: armonicos 1, 2, 3, 4, 6 y 8 (con un poco de temblor)."""
    n = int(dur * SR)
    f = hz(m)
    x = np.zeros(n)
    for k, g in ((1, 1.0), (2, 0.55), (3, 0.35), (4, 0.25), (6, 0.12), (8, 0.07)):
        if f * k < SR * 0.45:
            x += g * seno(f * k, n)
    if trem:
        x *= 1.0 - trem * (0.5 + 0.5 * np.sin(2 * np.pi * 5.0 * np.arange(n) / SR))
    return x / 2.4 * env(n, a, 1.0, 1.0, r)


def v_campana(m, dur, brillante=True, decay=2.8):
    """Campana de cristal: parciales no armonicos con caidas distintas."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = hz(m)
    parciales = ((1.0, 1.0, 1.0), (2.756, 0.55, 1.6), (5.404, 0.3, 2.6), (8.933, 0.15, 3.8)) if brillante else ((1.0, 1.0, 1.0), (2.0, 0.4, 1.6), (3.01, 0.2, 2.4))
    x = np.zeros(n)
    for r_, a_, d_ in parciales:
        if f * r_ < SR * 0.45:
            x += a_ * np.sin(2 * np.pi * f * r_ * t) * np.exp(-t * decay * d_)
    return fundido(x / 1.8, 0.001, 0.02)


def v_cajita(m, dur=0.9):
    """Caja de musica: seno con un segundo armonico que cae mas deprisa."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = hz(m)
    x = np.sin(2 * np.pi * f * t) * np.exp(-t * 5.0) + 0.35 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 9.0) + 0.12 * np.sin(2 * np.pi * 4.01 * f * t) * np.exp(-t * 16.0)
    return fundido(x, 0.001, 0.02)


def v_pizz(m, dur=0.35, brillo=2600.0):
    """Cuerda pellizcada (bajo o arpegio): triangulo + armonico, cae rapido."""
    n = int(dur * SR)
    f = hz(m)
    x = tri(f, n) + 0.4 * seno(f * 2, n) + 0.15 * seno(f * 3, n)
    return fundido(pasabajos(x, brillo, 2) * env(n, 0.002, 0.09, 0.0, 0.03), 0.001, 0.01)


def v_clave(m, dur=0.3):
    """Clavicembalo: pulso estrecho con un chasquido al empezar y caida rapida."""
    n = int(dur * SR)
    f = hz(m)
    x = 0.7 * pulso(f, n, 0.13) + 0.5 * saw(f, n)
    x = pasabajos(x, 5200, 2) * env(n, 0.001, 0.07, 0.04, 0.05)
    click = ruido(int(0.004 * SR)) * 0.5
    x[:len(click)] += click * np.linspace(1, 0, len(click))
    return fundido(x, 0.0005, 0.01)


def v_fantasma(m, dur, a=0.12, r=0.25, prof=0.45, desde=None):
    """Voz fantasmal (theremin): seno con vibrato lento y profundo y, si se da `desde`, un deslizamiento desde esa nota."""
    n = int(dur * SR)
    f = np.full(n, float(hz(m)))
    if desde is not None:
        k = min(n, int(0.09 * SR))
        f[:k] = hz(desde) * (hz(m) / hz(desde)) ** (np.arange(k) / k)
    f = vibrato(f, n, prof=prof, vel=5.0, retraso=0.18, subida=0.3)
    x = seno(f, n) + 0.18 * seno(f * 2, n)
    return x * env(n, a, 1.0, 1.0, r)


def v_bajo_saw(m, dur, brillo=900.0, a=0.003, d=0.12, s=0.7, r=0.02):
    n = int(dur * SR)
    return pasabajos(saw(hz(m), n) + 0.6 * pulso(hz(m) / 2, n, 0.5), brillo, 2) * env(n, a, d, s, r)


# ----------------------------------------------------------------------------------------------------------------------------------------- bateria

def bombo(dur=0.3, grave=44.0, ataque=150.0, caida=18.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = grave + (ataque - grave) * np.exp(-t * caida)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 11.0)
    clic = ruido(int(0.003 * SR)) * 0.3
    x[:len(clic)] += clic
    return fundido(x, 0.0005, 0.015)


def caja(dur=0.2, tono=185.0, cuerpo=0.5, brillo=1.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    r = pasabanda(ruido(n), 1200, 7500) * np.exp(-t * 22.0) * brillo
    c = np.sin(2 * np.pi * tono * t) * np.exp(-t * 30.0) * cuerpo
    return fundido(r + c, 0.0005, 0.02)


def hihat(abierto=False, dur=None):
    dur = dur or (0.17 if abierto else 0.05)
    n = int(dur * SR)
    t = np.arange(n) / SR
    return fundido(pasaaltos(ruido(n), 7000, 2) * np.exp(-t * (16.0 if abierto else 80.0)), 0.0005, 0.005) * 0.8


def tom(dur=0.35, f0=110.0, f1=62.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f1 + (f0 - f1) * np.exp(-t * 14.0)
    return fundido(np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 8.0) + 0.1 * ruido(n) * np.exp(-t * 60.0), 0.0005, 0.02)


def palmada(dur=0.22):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for k, off in enumerate((0.0, 0.011, 0.024)):
        i = int(off * SR)
        r = ruido(n - i)
        x[i:] += pasabanda(r, 900, 3200) * np.exp(-np.arange(n - i) / SR * (60.0 if k < 2 else 18.0))
    return fundido(x * 0.8, 0.0005, 0.02)


def platillo(dur=1.4):
    n = int(dur * SR)
    t = np.arange(n) / SR
    return fundido(pasaaltos(ruido(n), 3200, 2) * np.exp(-t * 3.2), 0.0005, 0.05) * 0.7


def madera(dur=0.08, f=820.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    return fundido((np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.4 * t)) * np.exp(-t * 55.0), 0.0003, 0.01)


def maraca(dur=0.07):
    n = int(dur * SR)
    t = np.arange(n) / SR
    return fundido(pasabanda(ruido(n), 4500, 9500) * np.minimum(t / 0.012, 1.0) * np.exp(-t * 38.0), 0.002, 0.01)


def timbal(dur=0.9, f=88.0):
    n = int(dur * SR)
    t = np.arange(n) / SR
    fr = f * (1.0 + 0.18 * np.exp(-t * 20.0))
    return fundido((np.sin(2 * np.pi * np.cumsum(fr) / SR) + 0.35 * np.sin(2 * np.pi * np.cumsum(fr * 1.5) / SR)) * np.exp(-t * 4.2) + 0.25 * pasabajos(ruido(n), 700) * np.exp(-t * 40.0), 0.0005, 0.04)


def viento(dur, volumen=1.0, centro=900.0, mov=0.5):
    """Soplo de viento: ruido filtrado con la frecuencia subiendo y bajando despacio."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    fcs = centro * (1.0 + mov * np.sin(2 * np.pi * t / max(dur, 0.5) * 1.3 + 1.0))
    x = barrido_pasabanda(ruido(n), fcs, 1.3)
    e = np.sin(np.pi * np.minimum(t / dur, 1.0)) ** 1.5
    return x * e * volumen


# ----------------------------------------------------------------------------------------------------------------------------------------- mezcla y efectos

class Mezcla:
    """Un buffer estereo en el que se van poniendo sonidos en un instante (segundos) con volumen y balance (-1 izquierda, 1 derecha)."""

    def __init__(self, segundos):
        self.n = int(round(segundos * SR))
        self.izq = np.zeros(self.n)
        self.der = np.zeros(self.n)

    def poner(self, t0, x, vol=1.0, pan=0.0):
        i = int(round(t0 * SR))
        if i >= self.n or i + len(x) <= 0:
            return
        if i < 0:
            x = x[-i:]
            i = 0
        x = np.asarray(x)[:self.n - i] * vol
        ang = (pan + 1.0) * np.pi / 4.0
        self.izq[i:i + len(x)] += x * np.cos(ang)
        self.der[i:i + len(x)] += x * np.sin(ang)

    def estereo(self):
        return np.stack([self.izq, self.der], 1)


def eco(est, retraso, retroalim=0.45, mezcla=0.3, rebotes=4, filtro=3500.0):
    """Eco ping-pong sobre una señal estereo (n, 2): cada rebote cambia de lado, se retrasa `retraso` s, pierde brillo y suena `retroalim` veces mas bajo que el anterior
    (el primero suena `mezcla` veces el original)."""
    n = len(est)
    d = int(retraso * SR)
    out = est.copy()
    ci, cd = est[:, 0], est[:, 1]
    for k in range(rebotes):
        if d >= n:
            break
        ni, nd = np.zeros(n), np.zeros(n)
        ni[d:] = cd[:n - d]
        nd[d:] = ci[:n - d]
        ci, cd = pasabajos(ni, filtro, 1), pasabajos(nd, filtro, 1)
        amp = mezcla * retroalim ** k
        out[:, 0] += ci * amp
        out[:, 1] += cd * amp
    return out


def respuesta_sala(segundos=2.2, oscuridad=1.0, semilla=5):
    """Respuesta al impulso sintetica de una sala (ruido que se apaga mas deprisa en los agudos), estereo."""
    rng = np.random.default_rng(semilla)
    n = int(segundos * SR)
    t = np.arange(n) / SR
    canales = []
    for c in range(2):
        r = rng.uniform(-1.0, 1.0, n)
        grave = pasabajos(r, 600, 2) * np.exp(-t * (3.0 / segundos) * 2.2)
        medio = pasabanda(r, 600, 3500, 2) * np.exp(-t * (3.0 / segundos) * 3.3 * oscuridad)
        agudo = pasaaltos(r, 3500, 2) * np.exp(-t * (3.0 / segundos) * 6.5 * oscuridad)
        ir = grave * 1.0 + medio * 0.8 + agudo * 0.35
        ir[:int(0.012 * SR)] *= np.linspace(0, 1, int(0.012 * SR))               # sin chasquido al principio
        canales.append(ir)
    ir = np.stack(canales, 1)
    return ir / np.sqrt((ir ** 2).sum() / 2.0)


def reverb(est, mezcla=0.2, segundos=2.2, oscuridad=1.0, cola=None):
    """Reverb por convolucion. Devuelve la señal (con `cola` segundos mas para que la reverb se apague)."""
    cola = segundos if cola is None else cola
    n_extra = int(cola * SR)
    seco = np.vstack([est, np.zeros((n_extra, 2))])
    ir = respuesta_sala(segundos, oscuridad)
    humedo = np.zeros_like(seco)
    for c in range(2):
        conv = sg.fftconvolve(est[:, c], ir[:, c], mode='full')[:len(seco)]
        humedo[:len(conv), c] = conv
    # se deja la proporcion `mezcla` de humedo; el nivel del humedo se iguala al del seco antes de mezclar
    rms_s, rms_h = np.sqrt((est ** 2).mean() + 1e-12), np.sqrt((humedo[:len(est)] ** 2).mean() + 1e-12)
    humedo *= rms_s / rms_h
    return seco * (1.0 - mezcla * 0.55) + humedo * mezcla


def sin_continua(est, fc=18.0):
    """Quita la componente continua y los graves por debajo de `fc` Hz (no se oyen y gastan margen y dan chasquidos)."""
    b, a = sg.butter(1, fc / (SR / 2), 'high')
    return sg.lfilter(b, a, est, axis=0)


def limitar(est, techo=0.93, rodilla=0.6):
    """Limitador de rodilla suave: por debajo de `rodilla` no toca nada; por encima, tanh hacia `techo` (nunca sube el volumen, solo recorta los picos)."""
    a = np.abs(est)
    exceso = np.maximum(a - rodilla, 0.0)
    comprimido = rodilla + (techo - rodilla) * np.tanh(exceso / (techo - rodilla))
    return np.sign(est) * np.where(a > rodilla, comprimido, a)


def normalizar(est, rms_objetivo=0.13, techo=0.9):
    """Sube o baja a un volumen medio razonable para musica y recorta los picos con el limitador."""
    rms = np.sqrt((est ** 2).mean() + 1e-12)
    est = est * (rms_objetivo / rms)
    return limitar(est, techo)


def a_ogg(est, ruta_ogg, calidad=3, mono=False):
    """Escribe el .wav (16 bits) y lo codifica a .ogg (Vorbis) con ffmpeg; devuelve (segundos, KB)."""
    est = np.asarray(est)
    if est.ndim == 1:
        est = est[:, None]
    if mono and est.shape[1] == 2:
        est = est.mean(axis=1, keepdims=True)
    wav = ruta_ogg[:-4] + '.wav'
    os.makedirs(os.path.dirname(ruta_ogg), exist_ok=True)
    with wave.open(wav, 'wb') as w:
        w.setnchannels(est.shape[1])
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((np.clip(est, -1, 1) * 32767).astype('<i2').tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', str(calidad), ruta_ogg], check=True)
    os.remove(wav)
    return len(est) / SR, os.path.getsize(ruta_ogg) // 1024
