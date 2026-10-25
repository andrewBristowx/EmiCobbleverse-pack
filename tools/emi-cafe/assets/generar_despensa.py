#!/usr/bin/env python3
"""Genera los archivos de las despensas (0.27.0): un bloque «Despensa de <cultivo>» por cada cultivo de la granja. Cada una es la caja del ingrediente con otro cuerpo (madera con
flejes de hierro y una etiqueta) para distinguirla; lo que muestra y hace lo decide el codigo (Despensas). Todo dibujado aqui con Pillow. Uso: python3 assets/generar_despensa.py (desde tools/emi-cafe)."""
import json, os, random
from PIL import Image

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'emi_cafe')
T_BLOQ = os.path.join(RAIZ, 'textures', 'block'); M_ITEM = os.path.join(RAIZ, 'models', 'item'); M_BLOQ = os.path.join(RAIZ, 'models', 'block'); BS = os.path.join(RAIZ, 'blockstates')
LANG = os.path.join(RAIZ, 'lang')

# id del cultivo -> (caja del ingrediente, nombre en español, nombre en ingles)
CULTIVOS = [
    ('papas', 'caja_papas', 'Despensa de papas', 'Potato pantry'),
    ('trigo', 'caja_harina', 'Despensa de trigo (harina)', 'Wheat pantry (flour)'),
    ('col', 'caja_lechuga', 'Despensa de col (lechuga)', 'Cabbage pantry (lettuce)'),
    ('tomates', 'caja_tomate', 'Despensa de tomates', 'Tomato pantry'),
    ('cebollas', 'caja_cebolla', 'Despensa de cebollas', 'Onion pantry'),
    ('calabazas', 'caja_calabaza', 'Despensa de calabazas', 'Pumpkin pantry'),
    ('bayas', 'caja_bayas', 'Despensa de bayas', 'Berry pantry'),
    ('arroz', 'caja_arroz', 'Despensa de arroz', 'Rice pantry'),
    ('cacao', 'caja_cacao', 'Despensa de cacao', 'Cocoa pantry'),
    ('granos', 'caja_granos', 'Despensa de granos de café', 'Coffee bean pantry'),
    ('vacas', 'caja_leche', 'Despensa de leche', 'Milk pantry'),
    ('gallinas', 'caja_huevos', 'Despensa de huevos', 'Egg pantry'),
    ('tauros', 'caja_carne', 'Despensa de carne', 'Meat pantry'),
    ('pescado', 'caja_pescado', 'Despensa de pescado', 'Fish pantry'),
    ('queso', 'caja_queso', 'Despensa de queso', 'Cheese pantry'),
    ('abejas', 'caja_miel', 'Despensa de miel', 'Honey pantry'),
]


def w(ruta, obj):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write('\n')


def textura():
    """Tablones verticales de madera con dos flejes de hierro y una etiqueta crema en medio."""
    random.seed(27)
    im = Image.new('RGBA', (16, 16))
    base = [(150, 100, 56), (138, 90, 50), (160, 110, 62), (130, 84, 46)]
    for x in range(16):
        for y in range(16):
            c = base[(x // 4) % 4]
            j = random.randint(-6, 6)
            im.putpixel((x, y), (c[0] + j, c[1] + j, c[2] + j // 2, 255))
        for y in range(16):
            if x % 4 == 3: im.putpixel((x, y), (90, 58, 30, 255))          # la junta entre tablones
    for y in (2, 3, 12, 13):                                                 # flejes
        for x in range(16): im.putpixel((x, y), (96, 100, 112, 255) if y in (2, 12) else (70, 74, 86, 255))
    for x in range(5, 11):                                                   # etiqueta
        for y in range(6, 10): im.putpixel((x, y), (238, 226, 190, 255))
    for x in range(5, 11): im.putpixel((x, 6), (200, 186, 150, 255)); im.putpixel((x, 9), (200, 186, 150, 255))
    for x in (6, 8, 9): im.putpixel((x, 7), (110, 80, 50, 255)); im.putpixel((x + (1 if x == 6 else 0), 8), (110, 80, 50, 255)) if x == 6 else None
    return im


def main():
    textura().save(os.path.join(T_BLOQ, 'despensa.png'))
    es_p = os.path.join(LANG, 'es_es.json'); en_p = os.path.join(LANG, 'en_us.json')
    es = json.load(open(es_p, encoding='utf-8')); en = json.load(open(en_p, encoding='utf-8'))
    for cid, caja, nes, nen in CULTIVOS:
        modelo = json.load(open(os.path.join(M_BLOQ, caja + '.json'), encoding='utf-8'))
        modelo['textures']['mueble'] = 'emi_cafe:block/despensa'
        modelo['textures']['particle'] = 'emi_cafe:block/despensa'
        nombre = 'despensa_' + cid
        w(os.path.join(M_BLOQ, nombre + '.json'), modelo)
        w(os.path.join(M_ITEM, nombre + '.json'), {'parent': 'emi_cafe:block/' + nombre})
        w(os.path.join(BS, nombre + '.json'), {'variants': {'': {'model': 'emi_cafe:block/' + nombre}}})
        es['block.emi_cafe.' + nombre] = nes; en['block.emi_cafe.' + nombre] = nen
    w(es_p, es); w(en_p, en)
    print('listo:', len(CULTIVOS), 'despensas')


if __name__ == '__main__':
    main()
