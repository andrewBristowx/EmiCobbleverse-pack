#!/usr/bin/env python3
"""Genera las plantillas (.nbt), los NPC y las ubicaciones de los gimnasios/ligas de emi_gimnasios y los deja en emi-estructuras.
Uso: python3 construir.py <carpeta tools/emi-estructuras> [<config extra-locations.json del pack>]"""
import json, os, sys
import gymlib

def registrar(nombre, v, npcs, region, etiqueta, aliases, orden_fuera=None):
    """nombre = ruta de la plantilla (p. ej. teselia/elesa); npcs = [(x, y, z, yaw_antes_de_girar, comando)] en coordenadas de la plantilla sin girar"""
    w0 = v.w
    g = gymlib.girar(v, 1)           # la fachada pasa de mirar al norte a mirar al oeste (la entrada segura de Emipokemon esta al oeste)
    lista = []
    for (x, y, z, yaw, cmd) in npcs:
        lista.append({'x': round(z + 0.5, 2), 'y': y, 'z': round(w0 - 1 - x + 0.5, 2), 'yaw': (yaw - 90) % 360, 'cmd': cmd})
    return g, lista

def bloques_validos():
    import zipfile, glob
    cand = glob.glob('/tmp/claude-0/*/*/scratchpad/cl/client.jar') or []
    ruta = os.environ.get('MC_CLIENT_JAR') or (cand[0] if cand else None)
    if not ruta: return None
    z = zipfile.ZipFile(ruta)
    return {n.split('/')[-1][:-5] for n in z.namelist() if n.startswith('assets/minecraft/blockstates/') and n.endswith('.json')}

def validar(v, validos, nombre):
    if validos is None: return
    malos = {}
    for b in set(v.g.values()):
        n = gymlib.parse_estado(b)[0].split(':')[-1]
        if n not in validos and n != 'air': malos[n] = malos.get(n, 0) + 1
    if malos: raise SystemExit(f'{nombre}: bloques que no existen: {sorted(malos)}')

def volcar(raiz, resultados, extra_config=None):
    validos = bloques_validos()
    nbt_dir = f'{raiz}/src/main/resources/data/emi_gimnasios/structure'
    npcs = {}
    ubic = []
    for r in resultados:
        validar(r['v'], validos, r['nombre'])
        ruta = f"{nbt_dir}/{r['nombre']}.nbt"
        p, b = r['v'].guardar(ruta)
        print(f"  {r['nombre']}: {r['v'].w}x{r['v'].h}x{r['v'].l}, {b} bloques, {p} estados")
        if r['npcs']: npcs[f"emi_gimnasios:{r['nombre']}"] = r['npcs']
        ubic.append({'region': r['region'], 'label': r['etiqueta'], 'structure': f"T:emi_gimnasios:{r['nombre']}", 'aliases': r['aliases']})
    json.dump(npcs, open(f'{raiz}/src/main/resources/emi_estructuras/npcs.json', 'w'), indent=1, ensure_ascii=False)
    for ruta in [f'{raiz}/src/main/resources/emi_estructuras/extra-locations-default.json'] + ([extra_config] if extra_config else []):
        d = json.load(open(ruta, encoding='utf-8'))
        d['ubicaciones'] = [u for u in d['ubicaciones'] if not str(u.get('structure', '')).startswith('T:emi_gimnasios:')] + ubic
        json.dump(d, open(ruta, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
