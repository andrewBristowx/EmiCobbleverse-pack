"""Estatuas de la pista: se calculan una vez a partir de los modelos de Cobblemon (modelo.py) y se guardan en estatuas/<especie>_<altura>.json,
asi el generador no necesita los archivos de Cobblemon para volver a construir la pista."""
import json, os
import modelo

AQUI = os.path.dirname(os.path.abspath(__file__))


def cargar(especie, altura, assets=None, tex_dir=None):
    f = os.path.join(AQUI, 'estatuas', '%s_%d.json' % (especie, altura))
    if os.path.exists(f):
        return {tuple(k[:3]): k[3] for k in json.load(open(f))}
    if not assets: raise FileNotFoundError('falta %s y no hay COBBLEMON_ASSETS para crearla' % f)
    bl = modelo.estatua(assets, tex_dir, especie, altura)
    json.dump([[x, y, z, b] for (x, y, z), b in sorted(bl.items())], open(f, 'w'), separators=(',', ':'))
    return bl
