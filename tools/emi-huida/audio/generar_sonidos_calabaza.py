"""Sonidos del nivel de la calabaza (Halloween). Todo sintetizado con numpy/scipy (original) y codificado a .ogg con ffmpeg.

Uso: python3 generar_sonidos_calabaza.py   (necesita numpy, scipy y ffmpeg con libvorbis; usa las utilidades de generar_sonidos.py)
Escribe en src/main/resources/assets/emi_huida/sounds/:
  musica_espera_calabaza.ogg  32,0 s (cajita de musica rota + viento + campanas)  -> DEBE ser 640 ticks en Sonidos.duracion()
  hechizo, transformacion, derrota_calabaza

La cancion de la persecucion (calabaza_intro.ogg y calabaza.ogg) NO se genera aqui: es «LA CALABAZA» que subio el dueno, cortada con ffmpeg:
  ffmpeg -i LA_CALABAZA.mp3 -t 9.23 -c:a libvorbis -q:a 4 calabaza_intro.ogg                               (el principio, hasta justo antes de que suba)
  ffmpeg -ss 9.23 -to 120.0 -i LA_CALABAZA.mp3 -af afade=t=out:st=110.4:d=0.4 -c:a libvorbis -q:a 4 calabaza.ogg   (el resto: 110,77 s = 2215 ticks)
"""
import numpy as np
import generar_sonidos as g
from generar_sonidos import SR, RNG, hz, tvec, lp, hp, bp, ruido, Mezcla, con_reverb, normalizar, normalizar_rms, guardar, campana, mono2


def caja_musica(m, dur=1.4):
    """una nota de cajita de musica un poco desafinada (por eso inquieta)"""
    f = hz(m + RNG.uniform(-0.12, 0.12))
    t = tvec(int(dur * SR))
    return (np.sin(2 * np.pi * f * t) + 0.45 * np.sin(2 * np.pi * 2.76 * f * t) * np.exp(-t * 9) + 0.2 * np.sin(2 * np.pi * 5.4 * f * t) * np.exp(-t * 18)) * np.exp(-t * 3.2) * np.minimum(t / 0.004, 1)


def musica_espera_calabaza():
    seg = 32.0; n = int(seg * SR); t = tvec(n)
    mz = Mezcla(seg)
    # nana en la menor, de tres tiempos a 90 bpm (16 compases = 32 s)
    nana = [(69, 72, 76), (81, 76, 72), (74, 77, 81), (80, 76, 71), (69, 72, 76), (81, 79, 76), (77, 74, 72), (71, 76, 80),
            (69, 72, 76), (81, 76, 72), (74, 77, 81), (80, 83, 80), (77, 81, 84), (83, 80, 76), (74, 72, 71), (69, None, None)]
    beat = 60.0 / 90.0
    for c, compas in enumerate(nana):
        for k, m in enumerate(compas):
            if m is None: continue
            mz.poner((c * 3 + k) * beat, caja_musica(m), 0.5 if k == 0 else 0.36, pan=-0.25 + 0.12 * k)
            if k == 0 and c % 2 == 0: mz.poner((c * 3 + k) * beat, caja_musica(m - 12, 2.0), 0.3, pan=0.2)       # eco grave
    # zumbido de organo grave (la + mi) que respira
    drone = g.saw(hz(33), seg, 0.001, (-8, 8)) * 0.6 + g.saw(hz(40), seg, 0.0012, (-6, 6)) * 0.35 + g.sine_f(g.osc_fase(hz(21), n)) * 0.7
    drone = lp(drone, 300) * (0.55 + 0.45 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 16.0 + 1)))
    mz.poner(0, drone, 0.2)
    # viento: ruido que sube y baja de tono
    viento = bp(ruido(seg), 250, 900) * (0.3 + 0.7 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 9.0))) + bp(ruido(seg), 900, 2200) * 0.25 * (0.5 + 0.5 * np.sin(2 * np.pi * t / 5.3 + 2)) ** 2
    mz.poner(0, viento, 0.22)
    # campana lejana (si bemol grave) cada 8 s
    for k in range(4):
        pan = RNG.uniform(-0.6, 0.6)
        mz.poner(1.0 + k * 8.0, campana(hz(46), 6.0, 0.9) * 0.9, 0.2, pan=pan); mz.poner(1.0 + k * 8.0, campana(hz(58.2), 5.0, 1.2) * 0.3, 0.2, pan=pan)
    # murcielagos y susurros: chillidos agudos muy cortos y soplos filtrados
    for _ in range(14):
        t0 = RNG.uniform(0.5, 31.0); dur = RNG.uniform(0.05, 0.12); nn = int(dur * SR); tt = tvec(nn)
        f = RNG.uniform(5500, 8500) * (1 - 0.4 * tt / dur)
        mz.poner(t0, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * tt / dur), RNG.uniform(0.05, 0.1), pan=RNG.uniform(-0.9, 0.9))
    for _ in range(6):
        t0 = RNG.uniform(1.0, 29.0); dur = RNG.uniform(0.6, 1.4); tt = tvec(int(dur * SR))
        mz.poner(t0, bp(ruido(dur), 1800, 3800) * np.sin(np.pi * tt / dur) ** 2, RNG.uniform(0.08, 0.16), pan=RNG.uniform(-0.8, 0.8))
    # crujidos de madera (un barrido grave de sierra filtrado)
    for t0 in (6.5, 14.2, 22.8, 29.0):
        d = 0.9; tt = tvec(int(d * SR)); f = 70 + 45 * np.sin(2 * np.pi * 3.1 * tt) + 30 * tt
        x = lp(g.saw_f(np.cumsum(f) / SR), 700) * np.exp(-tt * 3) * np.minimum(tt / 0.05, 1)
        mz.poner(t0, x, 0.1, pan=RNG.uniform(-0.5, 0.5))
    y = con_reverb(mz.estereo(), 0.32, 2.2, 0.8)
    f = int(0.4 * SR); y[:f] *= np.linspace(0, 1, f)[:, None]; y[-f:] *= np.linspace(1, 0, f)[:, None]
    y = np.stack([hp(y[:, 0], 30), hp(y[:, 1], 30)], 1)
    return normalizar_rms(y, -19.0, 0.7)


def fx_hechizo():
    """el hechizo del michi brujita: un barrido brillante que sube con campanitas en escala menor armonica y un golpe de chispas al final"""
    dur = 2.2; n = int(dur * SR); t = tvec(n)
    f = 300 * (3600 / 300) ** np.minimum(t / 1.7, 1)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.5 + np.sin(2 * np.pi * np.cumsum(f * 1.5) / SR) * 0.25
    x *= (t / 1.7).clip(0, 1) ** 1.5 * np.exp(-np.maximum(t - 1.7, 0) * 6) * (0.75 + 0.25 * np.sin(2 * np.pi * 14 * t))
    mz = Mezcla(dur)
    mz.poner(0, x, 0.55)
    mz.poner(0, hp(ruido(dur), 2500) * (t / 1.7).clip(0, 1) ** 2 * np.exp(-np.maximum(t - 1.7, 0) * 8), 0.22)
    for k, m in enumerate((81, 83, 84, 87, 88, 91, 92, 95)): mz.poner(0.15 + k * 0.2, campana(hz(m), 0.6, 6.0), 0.3)
    mz.poner(1.7, campana(hz(100), 0.5, 8.0), 0.35)
    return normalizar(con_reverb(mz.estereo(), 0.22, 1.0, 0.3), 0.85, False)


def fx_transformacion():
    """la calabaza se convierte en Pumpkaboo: golpe grave, estallido y una lluvia de campanitas hacia abajo"""
    dur = 2.4; mz = Mezcla(dur)
    mz.poner(0.0, g.taiko(46, 1.0), 0.9)
    tt = tvec(int(0.9 * SR))
    mz.poner(0.0, lp(ruido(0.9), 2500) * np.exp(-tt * 5), 0.5)
    mz.poner(0.0, hp(ruido(0.5), 4000) * np.exp(-tvec(int(0.5 * SR)) * 9), 0.3)
    mz.poner(0.0, g.rumor_grave(1.6, 110, 40), 0.35)
    for k, m in enumerate((96, 93, 91, 88, 84, 81, 76)): mz.poner(0.12 + k * 0.1, campana(hz(m), 0.7, 5.0), 0.28)
    x = mz.estereo()
    return normalizar(con_reverb(x, 0.3, 1.4, 0.4), 0.9, True)


def fx_derrota_calabaza():
    """«uuuuuooooo» de fantasma que baja de tono (te traga la calabaza)"""
    dur = 2.8; n = int(dur * SR); t = tvec(n)
    f = 560 * (170 / 560) ** (t / dur) * (1 + 0.025 * np.sin(2 * np.pi * 5.5 * t))
    fase = np.cumsum(f) / SR
    x = (g.saw_f(fase) * 0.5 + g.saw_f(fase * 1.004) * 0.4 + np.sin(2 * np.pi * fase) * 0.5)
    x = lp(x, 1400) * np.sin(np.pi * np.minimum(t / dur, 1)) ** 0.7
    mz = Mezcla(dur)
    mz.poner(0, x, 0.6)
    mz.poner(0, bp(ruido(dur), 500, 1800) * np.sin(np.pi * t / dur) ** 2, 0.15)
    mz.poner(0.0, campana(hz(45), dur, 1.0), 0.18)
    return normalizar(con_reverb(mz.estereo(), 0.35, 1.6, 0.6), 0.85, False)


def main():
    guardar('musica_espera_calabaza', musica_espera_calabaza())
    for nombre, fx in (('hechizo', fx_hechizo), ('transformacion', fx_transformacion), ('derrota_calabaza', fx_derrota_calabaza)):
        guardar(nombre, fx())


if __name__ == '__main__':
    main()
