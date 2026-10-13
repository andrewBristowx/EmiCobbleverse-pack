"""Estatuas de los PELUCHES del Michialien (GatitoAlien) para las pistas.

Se voxelizan los modelos de bloque de `tools/emi-plush` (geo + textura con UV por cara) con modelo.voxeles_modelo y se guardan en
estatuas/michi_<forma>_<escala*10>.json, de modo que regenerar la pista no necesita nada de emi-plush ni de Minecraft.

formas: 'base' (el peluche Michi Dramático), 'gold' (dorado), '<accesorio>' (sunglasses, beach_hat, crown, bow, headphones, scarf, halo, devil_horns,
witch_hat, santa_hat, flower_crown, pirate) y 'gold_<accesorio>'. `esc` = bloques por unidad del modelo (2.0 -> estatua de ~26 de ancho y 28 de alto).
"""
import json, os
import modelo

AQUI = os.path.dirname(os.path.abspath(__file__))
PLUSH = os.path.join(AQUI, '..', '..', 'emi-plush', 'resources', 'assets', 'emi_plush')


def nombre_modelo(forma):
    return 'michi_dramatico' if forma == 'base' else 'gatitoalien_' + forma


def cargar(forma, esc=2.0, tex_dir=None):
    f = os.path.join(AQUI, 'estatuas', 'michi_%s_%d.json' % (forma, round(esc * 10)))
    if os.path.exists(f):
        return {tuple(k[:3]): k[3] for k in json.load(open(f))}
    if not tex_dir: raise FileNotFoundError('falta %s y no hay MC_TEXTURAS para crearla' % f)
    n = nombre_modelo(forma)
    vox = modelo.voxeles_modelo(os.path.join(PLUSH, 'geo', n + '.geo.json'), os.path.join(PLUSH, 'textures', 'entity', n + '.png'), 0, esc=esc, ocultos=('hidden', 'locator'))
    bl = modelo.a_bloques(vox, tex_dir, sesgo={'amethyst_block': 0.3})      # las antenas y las orejas son lilas
    json.dump([[x, y, z, b] for (x, y, z), b in sorted(bl.items())], open(f, 'w'), separators=(',', ':'))
    return bl
