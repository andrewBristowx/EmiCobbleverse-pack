#!/usr/bin/env python3
"""Genera los archivos de las puertas de cristal de colores (0.18.2): puertas de verdad de 2 bloques (como las de vainilla, giran al abrirse) con marco y cristal de color.
Dos estilos (marco de madera y marco blanco) x 12 colores de cristal = 24 puertas. Texturas dibujadas aqui con Pillow (nada copiado). Uso: python3 assets/generar_puertas.py (desde tools/emi-cafe)."""
import json, os, glob
from PIL import Image
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'emi_cafe')
COLORES = [('white', 'blanco', 'White', (255, 255, 255)), ('pink', 'rosa', 'Pink', (243, 139, 170)), ('magenta', 'magenta', 'Magenta', (199, 78, 189)), ('purple', 'morado', 'Purple', (137, 50, 184)),
           ('blue', 'azul', 'Blue', (60, 68, 170)), ('light_blue', 'celeste', 'Light Blue', (58, 175, 217)), ('cyan', 'cian', 'Cyan', (22, 156, 156)), ('lime', 'lima', 'Lime', (128, 199, 31)),
           ('yellow', 'amarillo', 'Yellow', (254, 216, 61)), ('orange', 'naranja', 'Orange', (249, 128, 29)), ('red', 'rojo', 'Red', (176, 46, 38)), ('black', 'negro', 'Black', (40, 36, 36))]
ESTILOS = {'madera': dict(es='de madera y cristal', en='wood and glass', base=(138, 106, 61), oscuro=(84, 62, 33), claro=(172, 134, 82), pomo=(212, 178, 74)),
           'blanca': dict(es='blanca de cristal', en='white glass', base=(232, 232, 234), oscuro=(160, 160, 168), claro=(255, 255, 255), pomo=(120, 120, 128))}

def w(ruta, obj):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write('\n')

def rect(im, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1): im.putpixel((x, y), c + (255,) if len(c) == 3 else c)

def textura(est, rgb, mitad):
    e = ESTILOS[est]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    vidrio = rgb + (55 if rgb != (40, 36, 36) else 110,)
    rect(im, 0, 0, 15, 15, e['oscuro'])                       # contorno
    rect(im, 1, 1, 14, 14, e['base']) if mitad == 'top' else rect(im, 1, 0, 14, 14, e['base'])
    if mitad == 'top':
        rect(im, 1, 1, 14, 1, e['claro']); rect(im, 1, 1, 1, 14, e['claro'])
        rect(im, 3, 3, 12, 15, vidrio)                         # cristal
        for k in range(4): im.putpixel((4 + k, 9 - k), tuple(min(255, v + 90) for v in rgb) + (150,))
        rect(im, 3, 3, 12, 3, e['oscuro'] + (255,)) if False else None
        rect(im, 0, 15, 15, 15, e['oscuro'])
        rect(im, 3, 15, 12, 15, vidrio)
    else:
        rect(im, 1, 0, 1, 14, e['claro'])
        rect(im, 3, 0, 12, 10, vidrio)
        for k in range(4): im.putpixel((4 + k, 7 - k), tuple(min(255, v + 90) for v in rgb) + (150,))
        rect(im, 3, 11, 12, 13, e['oscuro']); rect(im, 4, 12, 11, 12, e['claro'])     # panel de abajo
        rect(im, 11, 4, 12, 5, e['pomo'])                                              # pomo
    return im

def icono(est, rgb):
    e = ESTILOS[est]
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    rect(im, 4, 0, 11, 15, e['oscuro']); rect(im, 5, 1, 10, 14, e['base']); rect(im, 5, 1, 5, 14, e['claro'])
    vidrio = rgb + (80,)
    rect(im, 6, 2, 9, 6, vidrio); rect(im, 6, 8, 9, 11, vidrio)
    im.putpixel((6, 5), tuple(min(255, v + 90) for v in rgb) + (230,)); im.putpixel((7, 4), tuple(min(255, v + 90) for v in rgb) + (230,))
    rect(im, 10, 7, 10, 8, e['pomo'])
    return im

def estado(facing, mitad, bisagra, abierta):
    # los giros de las puertas de vainilla
    base = {'east': 0, 'south': 90, 'west': 180, 'north': 270}[facing]
    if not abierta: y = base
    elif bisagra == 'left': y = (base + 90) % 360
    else: y = (base + 270) % 360
    return y

# limpiar lo de la 0.18.1 (paneles planos) y lo generado antes
for ruta in glob.glob(os.path.join(RAIZ, '*', 'puerta_cristal_*')) + glob.glob(os.path.join(RAIZ, 'models', '*', 'puerta_cristal_*')) + glob.glob(os.path.join(RAIZ, 'models', '*', 'puerta_madera_*')) + glob.glob(os.path.join(RAIZ, 'models', '*', 'puerta_blanca_*')):
    os.remove(ruta)

for est in ESTILOS:
    for c, es, en, rgb in COLORES:
        n = 'puerta_%s_%s' % (est, c)
        textura(est, rgb, 'top').save(os.path.join(RAIZ, 'textures', 'block', n + '_top.png')) if os.makedirs(os.path.join(RAIZ, 'textures', 'block'), exist_ok=True) is None else None
        textura(est, rgb, 'bottom').save(os.path.join(RAIZ, 'textures', 'block', n + '_bottom.png'))
        os.makedirs(os.path.join(RAIZ, 'textures', 'item'), exist_ok=True)
        icono(est, rgb).save(os.path.join(RAIZ, 'textures', 'item', n + '.png'))
        tex = {'top': 'emi_cafe:block/%s_top' % n, 'bottom': 'emi_cafe:block/%s_bottom' % n}
        for mitad in ('bottom', 'top'):
            for bis in ('left', 'right'):
                for ab in ('', '_open'):
                    nombre = 'door_%s_%s%s' % (mitad, bis, ab)
                    w(os.path.join(RAIZ, 'models', 'block', '%s_%s_%s%s.json' % (n, mitad, bis, ab)), {'parent': 'minecraft:block/' + nombre, 'textures': tex})
        variants = {}
        for facing in ('east', 'south', 'west', 'north'):
            for mitad, mh in (('lower', 'bottom'), ('upper', 'top')):
                for bis in ('left', 'right'):
                    for ab in (False, True):
                        clave = 'facing=%s,half=%s,hinge=%s,open=%s' % (facing, mitad, bis, 'true' if ab else 'false')
                        m = {'model': 'emi_cafe:block/%s_%s_%s%s' % (n, mh, bis, '_open' if ab else '')}
                        y = estado(facing, mitad, bis, ab)
                        if y: m['y'] = y
                        variants[clave] = m
        w(os.path.join(RAIZ, 'blockstates', n + '.json'), {'variants': variants})
        w(os.path.join(RAIZ, 'models', 'item', n + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'emi_cafe:item/' + n}})
for fich, idx in (('es_es', 1), ('en_us', 2)):
    ruta = os.path.join(RAIZ, 'lang', fich + '.json')
    d = json.load(open(ruta, encoding='utf-8'))
    for k in [k for k in d if k.startswith('block.emi_cafe.puerta_')]: del d[k]
    for est, e in ESTILOS.items():
        for c, es, en, rgb in COLORES:
            d['block.emi_cafe.puerta_%s_%s' % (est, c)] = ('Puerta %s %s' % (e['es'], es)) if idx == 1 else ('%s %s door' % (en, e['en']))
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=2, ensure_ascii=False); f.write('\n')
print('listo', len(ESTILOS) * len(COLORES), 'puertas')
