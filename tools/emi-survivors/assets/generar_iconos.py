#!/usr/bin/env python3
"""Dibuja todos los iconos de Emi Survivors (ataques, evoluciones, ataques iniciales por tipo, objetos y mejoras) y los junta en un atlas de 24x24 por icono.

Uso:  python3 generar_iconos.py                 -> escribe src/main/resources/assets/emi_survivors/textures/iconos.png y .../iconos.json
      python3 generar_iconos.py --hojas DIR     -> ademas guarda hojas de contacto ampliadas en DIR (para revisar a ojo)
      python3 generar_iconos.py --solo clave1,clave2   -> solo esas claves (para probar)
Las claves: a_<ataque>, e_<ataque> (evolucion), i_<tipo> (ataque inicial de ese tipo), p_<objeto>, m_<mejora permanente>, x_<otros>.
Los dibujos son propios (hechos por codigo); del juego PokéSurvivor solo se tomo como referencia la idea de la interfaz (ranuras con icono, numero y puntos de nivel)."""
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image, ImageDraw, ImageFont

from iconos_base import *
import iconos_ataques
import iconos_objetos
import iconos_evo

AQUI = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'emi_survivors')
COLUMNAS = 16


def todos():
    """Todos los iconos: {clave: Dibujo}, en el orden del atlas."""
    r = {}
    for k, f in iconos_ataques.ATAQUES.items():
        r['a_' + k] = f()
    for k, f in iconos_evo.EVOLUCIONES.items():
        d = f()
        iconos_evo.marca_evo(d)
        r['e_' + k] = d
    for t, (a, rampa) in iconos_evo.INICIALES.items():
        r['i_' + t] = iconos_ataques.ATAQUES[a](RAMPAS[rampa])
    for k, f in iconos_objetos.OBJETOS.items():
        r['p_' + k] = f()
    for k, f in iconos_objetos.MEJORAS_PROPIAS.items():
        r['m_' + k] = f()
    for k, o in iconos_objetos.MEJORAS_USAN.items():
        r['m_' + k] = iconos_objetos.OBJETOS[o]()
    for k, f in iconos_objetos.OTROS.items():
        r['x_' + k] = f()
    return r


def atlas(dib):
    claves = list(dib.keys())
    filas = (len(claves) + COLUMNAS - 1) // COLUMNAS
    im = Image.new('RGBA', (COLUMNAS * N, filas * N), (0, 0, 0, 0))
    idx = {}
    for i, k in enumerate(claves):
        c, f = i % COLUMNAS, i // COLUMNAS
        im.paste(dib[k], (c * N, f * N))
        idx[k] = [c, f]
    return im, {'celda': N, 'columnas': COLUMNAS, 'filas': filas, 'ancho': im.width, 'alto': im.height, 'iconos': idx}


def hoja(dib, ruta, cols=8, zoom=5, fondo=(27, 33, 51)):
    claves = list(dib.keys())
    celda = N * zoom + 14
    filas = (len(claves) + cols - 1) // cols
    h = Image.new('RGB', (cols * celda, filas * (celda + 14)), fondo)
    d = ImageDraw.Draw(h)
    try:
        fuente = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 10)
    except OSError:
        fuente = ImageFont.load_default()
    for i, k in enumerate(claves):
        c, f = i % cols, i // cols
        x, y = c * celda + 7, f * (celda + 14) + 4
        g = dib[k].resize((N * zoom, N * zoom), Image.NEAREST)
        h.paste(g, (x, y), g)
        d.text((x, y + N * zoom + 1), k, fill=(190, 200, 230), font=fuente)
    h.save(ruta)


def main():
    args = sys.argv[1:]
    dir_hojas = args[args.index('--hojas') + 1] if '--hojas' in args else None
    solo = set(args[args.index('--solo') + 1].split(',')) if '--solo' in args else None
    dibujos = todos()
    imgs = {k: d.terminar() for k, d in dibujos.items() if solo is None or k in solo}
    if dir_hojas:
        os.makedirs(dir_hojas, exist_ok=True)
        claves = list(imgs.keys())
        for n in range(0, len(claves), 24):
            hoja({k: imgs[k] for k in claves[n:n + 24]}, os.path.join(dir_hojas, 'hoja_%02d.png' % (n // 24)))
    if solo is None:
        im, idx = atlas(imgs)
        os.makedirs(os.path.join(SALIDA, 'textures'), exist_ok=True)
        im.save(os.path.join(SALIDA, 'textures', 'iconos.png'))
        texto = re.sub(r'\[\s+(\d+),\s+(\d+)\s+\]', r'[\1, \2]', json.dumps(idx, ensure_ascii=False, indent=1))
        with open(os.path.join(SALIDA, 'iconos.json'), 'w', encoding='utf-8') as f:
            f.write(texto + '\n')
        print('atlas', im.size, len(imgs), 'iconos')


if __name__ == '__main__':
    main()
