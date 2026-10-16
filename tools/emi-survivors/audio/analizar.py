#!/usr/bin/env python3
"""Revisa los .ogg que genera musica.py / sonidos.py SIN oirlos (la maquina de pruebas no tiene sonido): volumen, picos, chasquidos, la cola del bucle, si las notas
caen donde tienen que caer (comparando la nota que suena en cada compas con los acordes de la cancion) y un espectrograma en PNG para verlo.

Uso:  python3 analizar.py musica_pradera [...]        (nombres de archivo sin .ogg, dentro de assets/emi_survivors/sounds)
      python3 analizar.py --espectro DIR musica_sala  (ademas guarda el espectrograma en DIR/<nombre>.png)
      python3 analizar.py --todo                      (todos los .ogg de la carpeta, solo el resumen)"""
import os
import subprocess
import sys

import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, AQUI)
import sinte as S

SONIDOS = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'emi_survivors', 'sounds')


def leer(nombre):
    ruta = os.path.join(SONIDOS, nombre + '.ogg')
    canales = int(subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'stream=channels', '-of', 'csv=p=0', ruta], capture_output=True, text=True).stdout.strip() or 2)
    r = subprocess.run(['ffmpeg', '-v', 'error', '-i', ruta, '-f', 'f32le', '-ac', str(canales), '-ar', str(S.SR), '-'], capture_output=True, check=True)
    x = np.frombuffer(r.stdout, dtype='<f4').reshape(-1, canales).astype(np.float64)
    if canales == 1:
        x = np.repeat(x, 2, axis=1)                 # para el analisis, mono = el mismo canal dos veces (sin bajar 3 dB)
    return x, canales, os.path.getsize(ruta) // 1024


def db(v):
    return 20 * np.log10(max(v, 1e-9))


def resumen(nombre, x, canales, kb):
    m = x.mean(axis=1)
    pico = np.abs(x).max()
    rms = np.sqrt((m ** 2).mean())
    saturadas = int((np.abs(x) > 0.985).sum())
    dc = abs(m.mean())
    fin = np.sqrt((m[-int(0.1 * S.SR):] ** 2).mean())
    ini = np.sqrt((m[:int(0.1 * S.SR)] ** 2).mean())
    # centroide espectral medio y reparto de energia por bandas
    n = 1 << 15
    pot = np.zeros(n // 2 + 1)
    mp = np.pad(m, (0, max(0, n - len(m))))
    hop = max(n, len(mp) // 40)
    for i in range(0, max(1, len(mp) - n + 1), hop):
        pot += np.abs(np.fft.rfft(mp[i:i + n] * np.hanning(n))) ** 2
    f = np.fft.rfftfreq(n, 1 / S.SR)
    centroide = float((f * pot).sum() / pot.sum())
    bandas = [(20, 120), (120, 500), (500, 2000), (2000, 6000), (6000, 20000)]
    reparto = ['%d%%' % round(100 * pot[(f >= a) & (f < b)].sum() / pot.sum()) for a, b in bandas]
    print('%-22s %5.1f s  %3s  pico %5.1f dB  rms %5.1f dB  chasquidos(>0,985): %d  dc %.4f  inicio %.4f fin %.5f  centroide %4.0f Hz  [<120|<500|<2k|<6k|>6k: %s]  %d KB' %
          (nombre, len(x) / S.SR, 'mono' if canales == 1 else 'est.', db(pico), db(rms), saturadas, dc, ini, fin, centroide, ' '.join(reparto), kb))


def cromas(m, t0, t1):
    """Energia de cada una de las 12 clases de nota entre t0 y t1 segundos (con una ventana larga para afinar los graves)."""
    a, b = int(t0 * S.SR), min(len(m), int(t1 * S.SR))
    n = 1 << 16
    seg = m[a:b]
    if len(seg) < 1024:
        return np.zeros(12)
    seg = np.pad(seg * np.hanning(len(seg)), (0, max(0, n - len(seg))))[:n]
    esp = np.abs(np.fft.rfft(seg)) ** 2
    f = np.fft.rfftfreq(n, 1 / S.SR)
    ok = (f >= 55) & (f <= 2000)
    midi = 69 + 12 * np.log2(f[ok] / 440.0)
    clase = np.round(midi).astype(int) % 12
    c = np.zeros(12)
    np.add.at(c, clase, esp[ok])
    return c / max(c.sum(), 1e-12)


def comprobar_armonia(nombre, m, bpm, progresion, pcs_de_acorde):
    """Para cada compas, cuanta energia de las notas del acorde hay (1 = todo). Un valor bajo en un compas indica una nota fuera de sitio o un acorde mal sonado."""
    compas = 4 * 60.0 / bpm
    resultados = []
    for k in range(len(progresion) * 2):
        simb = progresion[k % len(progresion)]
        c = cromas(m, k * compas, (k + 1) * compas)
        propios = sum(c[p] for p in pcs_de_acorde(simb))
        resultados.append(propios)
    print('   armonia: %s del acorde en cada compas (minimo %.0f %% en el compas %d)' % ('%.0f%% de media' % (100 * np.mean(resultados)), 100 * min(resultados), 1 + int(np.argmin(resultados))))
    return resultados


def espectrograma(x, ruta, ancho=1100, alto=420):
    m = x.mean(axis=1)
    n_fft = 4096
    m = np.pad(m, (0, max(0, n_fft + 1 - len(m))))
    pasos = np.linspace(0, len(m) - n_fft, ancho).astype(int)
    ventana = np.hanning(n_fft)
    f = np.fft.rfftfreq(n_fft, 1 / S.SR)
    ejes = np.geomspace(40, 16000, alto)
    img = np.zeros((alto, ancho))
    for j, i in enumerate(pasos):
        esp = np.abs(np.fft.rfft(m[i:i + n_fft] * ventana))
        img[:, j] = np.interp(ejes, f, esp)
    img = 20 * np.log10(img + 1e-7)
    img = np.clip((img - (img.max() - 80)) / 80, 0, 1)[::-1]
    # una paleta de negro a violeta, naranja y blanco
    pal = np.array([[0, 0, 0], [30, 8, 60], [120, 20, 110], [220, 60, 60], [255, 170, 40], [255, 255, 220]], dtype=float)
    pos = img * (len(pal) - 1)
    lo = np.floor(pos).astype(int).clip(0, len(pal) - 2)
    fr = (pos - lo)[..., None]
    rgb = pal[lo] * (1 - fr) + pal[lo + 1] * fr
    Image.fromarray(rgb.astype(np.uint8)).save(ruta)


def main():
    args = sys.argv[1:]
    dir_png = None
    if '--espectro' in args:
        i = args.index('--espectro')
        dir_png = args[i + 1]
        del args[i:i + 2]
        os.makedirs(dir_png, exist_ok=True)
    armonia = '--sin-armonia' not in args
    args = [a for a in args if not a.startswith('--')] or None
    if args is None or '--todo' in sys.argv:
        args = sorted(f[:-4] for f in os.listdir(SONIDOS) if f.endswith('.ogg'))
    for nombre in args:
        x, canales, kb = leer(nombre)
        resumen(nombre, x, canales, kb)
        if dir_png:
            espectrograma(x, os.path.join(dir_png, nombre + '.png'))
        if armonia and nombre.startswith('musica_'):
            import musica as M
            info = M.ARMONIA.get(nombre[len('musica_'):])
            if info:
                comprobar_armonia(nombre, x.mean(axis=1), info[0], info[1], M.pcs_acorde)


if __name__ == '__main__':
    main()
