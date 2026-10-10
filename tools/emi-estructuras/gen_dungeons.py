#!/usr/bin/env python3
"""Genera la seccion "Dungeons" de config/emipokemon/extra-locations.json (y la copia por defecto del mod) a partir de los jars del pack.
Jefes (Bosses of Mass Destruction), When Dungeons Arise y Dungeons and Taverns, en ese orden. Uso: gen_dungeons.py <carpeta con los jars> """
import json, re, sys, zipfile, glob, os

MODS = sys.argv[1]
ROOT = os.path.dirname(os.path.abspath(__file__))
FUENTES = [  # (namespace, patron del jar, prefijo de la etiqueta)
    ('bosses_of_mass_destruction', 'BOMD-*.jar', 'Jefe'),
    ('dungeons_arise', 'DungeonsArise-*.jar', 'DA'),
    ('nova_structures', 'dungeons-and-taverns-*.jar', 'D&T'),
]
EXCLUIR = set()   # ids que no se quieren (YUNG's Better Dungeons queda fuera a proposito)

def titulo(p): return ' '.join(w.capitalize() for w in re.split(r'[_/]+', p))

def estructuras(ns, patron):
    out = []
    for j in glob.glob(os.path.join(MODS, patron)):
        for n in zipfile.ZipFile(j).namelist():
            m = re.match(r'data/%s/worldgen/structure/(.+)\.json$' % re.escape(ns), n)
            if m: out.append(m.group(1))
    return sorted(set(out))

entradas = []
for ns, patron, pref in FUENTES:
    for p in estructuras(ns, patron):
        sid = f'{ns}:{p}'
        if sid in EXCLUIR: continue
        entradas.append({'region': 'Dungeons', 'label': f'{pref}: {titulo(p)}', 'structure': sid, 'aliases': [f'{ns} {p.replace("_", " ").replace("/", " ")}']})

for destino in (os.path.join(ROOT, 'src/main/resources/emi_estructuras/extra-locations-default.json'), os.path.join(ROOT, '../../config/emipokemon/extra-locations.json')):
    base = json.load(open(os.path.join(ROOT, 'src/main/resources/emi_estructuras/extra-locations-default.json'), encoding='utf-8'))
    base['ubicaciones'] = [u for u in base['ubicaciones'] if u['region'].lower() != 'dungeons'] + entradas
    base['_ayuda'] = base['_ayuda'].split(' La region "Dungeons"')[0] + ' La region "Dungeons" va a la dimension emi_estructuras:dungeons (el resto a emi_estructuras:plano).'
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, 'w', encoding='utf-8') as f:
        f.write('{\n  "_ayuda": ' + json.dumps(base['_ayuda'], ensure_ascii=False) + ',\n  "ubicaciones": [\n')
        f.write(',\n'.join('    ' + json.dumps(u, ensure_ascii=False) for u in base['ubicaciones']))
        f.write('\n  ]\n}\n')
print(len(entradas), 'estructuras de Dungeons')
