#!/usr/bin/env python3
"""Genera los archivos de las puertas de cristal de colores (0.18.1): blockstate, modelo cerrado (cubo de cristal), modelo abierto (placa fina pegada al borde), modelo de objeto y textos.
Las texturas son las del cristal de colores de Minecraft (no se copia ninguna). Uso: python3 assets/generar_puertas.py (desde tools/emi-cafe)."""
import json, os
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'emi_cafe')
COLORES = [('white', 'blanca', 'White'), ('pink', 'rosa', 'Pink'), ('magenta', 'magenta', 'Magenta'), ('purple', 'morada', 'Purple'), ('blue', 'azul', 'Blue'), ('light_blue', 'celeste', 'Light Blue'),
           ('cyan', 'cian', 'Cyan'), ('lime', 'lima', 'Lime'), ('yellow', 'amarilla', 'Yellow'), ('orange', 'naranja', 'Orange'), ('red', 'roja', 'Red'), ('black', 'negra', 'Black')]

def w(ruta, obj):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write('\n')

def cara(tex, uv):
    return {'uv': uv, 'texture': '#t', 'cullface': None}

for c, es, en in COLORES:
    nombre = 'puerta_cristal_' + c
    tex = 'minecraft:block/%s_stained_glass' % c
    w(os.path.join(RAIZ, 'blockstates', nombre + '.json'), {'variants': {'abierta=false': {'model': 'emi_cafe:block/' + nombre}, 'abierta=true': {'model': 'emi_cafe:block/' + nombre + '_abierta'}}})
    w(os.path.join(RAIZ, 'models', 'block', nombre + '.json'), {'parent': 'minecraft:block/cube_all', 'textures': {'all': tex}})
    caras = {k: {'uv': [0, 0, 16, 16], 'texture': '#t'} for k in ('north', 'south', 'east', 'west', 'up', 'down')}
    w(os.path.join(RAIZ, 'models', 'block', nombre + '_abierta.json'), {'textures': {'t': tex, 'particle': tex}, 'elements': [{'from': [0, 0, 0], 'to': [16, 16, 1], 'faces': {
        'north': {'uv': [0, 0, 16, 16], 'texture': '#t'}, 'south': {'uv': [0, 0, 16, 16], 'texture': '#t'},
        'east': {'uv': [0, 0, 1, 16], 'texture': '#t'}, 'west': {'uv': [0, 0, 1, 16], 'texture': '#t'},
        'up': {'uv': [0, 0, 16, 1], 'texture': '#t'}, 'down': {'uv': [0, 0, 16, 1], 'texture': '#t'}}}]})
    w(os.path.join(RAIZ, 'models', 'item', nombre + '.json'), {'parent': 'emi_cafe:block/' + nombre})
for fich, idx in (('es_es', 1), ('en_us', 2)):
    ruta = os.path.join(RAIZ, 'lang', fich + '.json')
    d = json.load(open(ruta, encoding='utf-8'))
    for c, es, en in COLORES:
        d['block.emi_cafe.puerta_cristal_' + c] = ('Puerta de cristal ' + es) if idx == 1 else (en + ' glass door')
    with open(ruta, 'w', encoding='utf-8') as f:
        json.dump(d, f, indent=2, ensure_ascii=False); f.write('\n')
print('listo', len(COLORES), 'colores')
