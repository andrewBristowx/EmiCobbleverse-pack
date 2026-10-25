#!/usr/bin/env python3
"""Genera los archivos de la decoracion de la 0.19.0: 8 diseños de mesa, 12 vitrinas de cristal de colores (para mostrar ingredientes y platos) y los iconos de las mejoras nuevas.
Todo se dibuja aqui con Pillow a partir de los dibujos del propio mod (nada copiado). Uso: python3 assets/generar_deco.py (desde tools/emi-cafe)."""
import json, os, glob
from PIL import Image, ImageDraw
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'emi_cafe')
TB = os.path.join(RAIZ, 'textures', 'block')
TI = os.path.join(RAIZ, 'textures', 'item')

COLORES = [('white', 'blanca', 'White', (255, 255, 255)), ('pink', 'rosa', 'Pink', (243, 139, 170)), ('magenta', 'magenta', 'Magenta', (199, 78, 189)), ('purple', 'morada', 'Purple', (137, 50, 184)),
           ('blue', 'azul', 'Blue', (60, 68, 170)), ('light_blue', 'celeste', 'Light Blue', (58, 175, 217)), ('cyan', 'cian', 'Cyan', (22, 156, 156)), ('lime', 'lima', 'Lime', (128, 199, 31)),
           ('yellow', 'amarilla', 'Yellow', (254, 216, 61)), ('orange', 'naranja', 'Orange', (249, 128, 29)), ('red', 'roja', 'Red', (176, 46, 38)), ('black', 'negra', 'Black', (40, 36, 36))]

# diseños de mesa: (id, español, ingles, colores del mantel oscuro/medio/claro, color de la madera, mantel = None -> la tapa es de madera)
MESAS = [('azul', 'Mesa azul', 'Blue table', ((40, 70, 160), (70, 110, 220), (240, 246, 255)), (143, 94, 54)),
         ('verde', 'Mesa verde', 'Green table', ((40, 120, 70), (70, 170, 100), (245, 255, 246)), (143, 94, 54)),
         ('rosa', 'Mesa rosa', 'Pink table', ((214, 90, 140), (244, 140, 180), (255, 244, 248)), (160, 112, 84)),
         ('negra', 'Mesa elegante negra', 'Elegant black table', ((30, 30, 36), (62, 62, 74), (210, 210, 224)), (70, 52, 40)),
         ('blanca', 'Mesa blanca', 'White table', ((196, 196, 210), (232, 232, 244), (255, 255, 255)), (214, 190, 150)),
         ('madera', 'Mesa de madera', 'Wooden table', None, (176, 126, 76)),
         ('piedra', 'Mesa de piedra', 'Stone table', ((104, 104, 116), (148, 148, 160), (200, 200, 212)), (120, 120, 130)),
         ('dorada', 'Mesa dorada', 'Golden table', ((170, 120, 30), (232, 182, 52), (255, 246, 208)), (84, 54, 30)),
         ('terraza', 'Mesa de terraza', 'Terrace table', ((20, 130, 150), (60, 190, 205), (255, 255, 255)), (214, 196, 150))]    # 0.30.0: la mesa donde se sientan los clientes que quieren terraza


def w(ruta, obj):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write('\n')


def lum(c): return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def teñir(im, base):
    """Pinta una textura de madera con otro color manteniendo sus luces y sombras."""
    out = Image.new('RGBA', im.size)
    px = list(im.convert('RGBA').getdata())
    media = sum(lum(p) for p in px) / len(px)
    pix = []
    for p in px:
        f = lum(p) / media
        pix.append((min(255, int(base[0] * f)), min(255, int(base[1] * f)), min(255, int(base[2] * f)), 255))
    out.putdata(pix)
    return out


def mantel(cols):
    im = Image.open(os.path.join(TB, 'mantel.png')).convert('RGBA')
    viejo = [(184, 40, 60), (226, 73, 92), (255, 246, 242)]
    mapa = {viejo[i]: cols[i] for i in range(3)}
    pix = [mapa.get(p[:3], p[:3]) + (255,) for p in im.getdata()]
    out = Image.new('RGBA', im.size); out.putdata(pix)
    return out


# ------------------------------------------------------------------ las mesas
modelo_mesa = json.load(open(os.path.join(RAIZ, 'models', 'block', 'mesa.json'), encoding='utf-8'))
for ruta in glob.glob(os.path.join(RAIZ, '*', 'mesa_*')) + glob.glob(os.path.join(RAIZ, 'models', '*', 'mesa_*')) + glob.glob(os.path.join(TB, 'mesa_*')) + glob.glob(os.path.join(TB, 'madera_*')) + glob.glob(os.path.join(TB, 'mantel_*')):
    if os.path.isfile(ruta) and os.path.basename(ruta) not in ('madera_oscura.png',): os.remove(ruta)
madera = Image.open(os.path.join(TB, 'madera.png')).convert('RGBA')
madera_osc = Image.open(os.path.join(TB, 'madera_oscura.png')).convert('RGBA')
for id_, es, en, cols, mad in MESAS:
    n = 'mesa_' + id_
    teñir(madera, mad).save(os.path.join(TB, 'madera_%s.png' % id_))
    teñir(madera_osc, tuple(int(v * 0.65) for v in mad)).save(os.path.join(TB, 'madera_oscura_%s.png' % id_))
    if cols is None:
        teñir(madera, tuple(min(255, int(v * 1.12)) for v in mad)).save(os.path.join(TB, 'mantel_%s.png' % id_))
    else:
        mantel(cols).save(os.path.join(TB, 'mantel_%s.png' % id_))
    m = json.loads(json.dumps(modelo_mesa))
    m['textures'] = {'particle': 'emi_cafe:block/madera_' + id_, 'madera': 'emi_cafe:block/madera_' + id_, 'madera_oscura': 'emi_cafe:block/madera_oscura_' + id_, 'mantel': 'emi_cafe:block/mantel_' + id_}
    w(os.path.join(RAIZ, 'models', 'block', n + '.json'), m)
    w(os.path.join(RAIZ, 'blockstates', n + '.json'), {'variants': {'': {'model': 'emi_cafe:block/' + n}}})
    w(os.path.join(RAIZ, 'models', 'item', n + '.json'), {'parent': 'emi_cafe:block/' + n})

# ------------------------------------------------------------------ las vitrinas
for ruta in glob.glob(os.path.join(RAIZ, '*', 'vitrina_*')) + glob.glob(os.path.join(RAIZ, 'models', '*', 'vitrina_*')) + glob.glob(os.path.join(TB, 'vitrina_*')):
    if os.path.isfile(ruta): os.remove(ruta)


def cristal(rgb):
    im = Image.new('RGBA', (16, 16), rgb + (105,))
    d = ImageDraw.Draw(im)
    marco = tuple(min(255, v + 70) for v in rgb) + (230,)
    d.rectangle([0, 0, 15, 15], outline=marco)
    brillo = tuple(min(255, v + 110) for v in rgb) + (170,)
    for k in range(4): im.putpixel((3 + k, 9 - k), brillo)
    return im


for c, es, en, rgb in COLORES:
    n = 'vitrina_' + c
    cristal(rgb).save(os.path.join(TB, n + '.png'))
    cara = lambda t: {'texture': '#' + t}
    todas = lambda t: {k: cara(t) for k in ('north', 'south', 'east', 'west', 'up', 'down')}
    modelo = {'parent': 'minecraft:block/block',
              'textures': {'particle': 'emi_cafe:block/madera_oscura', 'base': 'emi_cafe:block/madera_oscura', 'vidrio': 'emi_cafe:block/' + n},
              'elements': [{'from': [0, 0, 0], 'to': [16, 2, 16], 'faces': todas('base')},
                           {'from': [1, 2, 1], 'to': [15, 14, 15], 'faces': todas('vidrio')},
                           {'from': [0, 14, 0], 'to': [16, 15, 1], 'faces': todas('base')},          # el borde de arriba (la tapa es de cristal)
                           {'from': [0, 14, 15], 'to': [16, 15, 16], 'faces': todas('base')},
                           {'from': [0, 14, 1], 'to': [1, 15, 15], 'faces': todas('base')},
                           {'from': [15, 14, 1], 'to': [16, 15, 15], 'faces': todas('base')}]}
    w(os.path.join(RAIZ, 'models', 'block', n + '.json'), modelo)
    w(os.path.join(RAIZ, 'blockstates', n + '.json'), {'variants': {'': {'model': 'emi_cafe:block/' + n}}})
    w(os.path.join(RAIZ, 'models', 'item', n + '.json'), {'parent': 'emi_cafe:block/' + n})

# ------------------------------------------------------------------ iconos de las mejoras nuevas
def C(h): return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def icono_estaciones():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rectangle([1, 6, 14, 14], fill=C('8d93a8'), outline=C('3c4256'))          # la cocina
    d.rectangle([2, 7, 13, 8], fill=C('b9bfd4'))
    d.rectangle([3, 11, 12, 13], fill=C('5a6078'))
    for cx in (4, 10):                                                         # dos ollas
        d.rectangle([cx, 3, cx + 3, 5], fill=C('e6e8f2'), outline=C('3c4256'))
        d.line([cx + 1, 2, cx + 2, 2], fill=C('3c4256'))
    d.rectangle([7, 9, 8, 10], fill=C('ffb23f'))
    im.putpixel((7, 9), C('ff7a2f')); im.putpixel((8, 10), C('ffd75a'))
    return im


def icono_lujo():
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rectangle([1, 5, 14, 8], fill=C('e8b634'), outline=C('a06c10'))          # mantel dorado
    for x in range(2, 14, 3): d.rectangle([x, 6, x + 1, 7], fill=C('fff3c4'))
    d.rectangle([7, 9, 8, 14], fill=C('8a5a34')); d.rectangle([4, 14, 11, 15], fill=C('5e3a20'))
    d.rectangle([7, 2, 8, 4], fill=C('3fa75a')); d.rectangle([6, 1, 9, 2], fill=C('ff6f9c'))   # florero
    return im


os.makedirs(TI, exist_ok=True)
for n, f in (('estaciones_icono', icono_estaciones), ('mesa_lujo_icono', icono_lujo)):
    f().save(os.path.join(TI, n + '.png'))
    w(os.path.join(RAIZ, 'models', 'item', n + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'emi_cafe:item/' + n}})

# ------------------------------------------------------------------ nombres
for fich, idx in (('es_es', 1), ('en_us', 2)):
    ruta = os.path.join(RAIZ, 'lang', fich + '.json')
    d = json.load(open(ruta, encoding='utf-8'))
    for k in [k for k in d if k.startswith('block.emi_cafe.mesa_') or k.startswith('block.emi_cafe.vitrina_')]: del d[k]
    for id_, es, en, cols, mad in MESAS: d['block.emi_cafe.mesa_' + id_] = es if idx == 1 else en
    for c, es, en, rgb in COLORES: d['block.emi_cafe.vitrina_' + c] = ('Vitrina de cristal %s' % es) if idx == 1 else ('%s glass display case' % en)
    d['item.emi_cafe.estaciones_icono'] = 'Más estaciones' if idx == 1 else 'More stations'
    d['item.emi_cafe.mesa_lujo_icono'] = 'Mesas elegantes' if idx == 1 else 'Elegant tables'
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=2, ensure_ascii=False); f.write('\n')
print('listo', len(MESAS), 'mesas,', len(COLORES), 'vitrinas, 2 iconos')
