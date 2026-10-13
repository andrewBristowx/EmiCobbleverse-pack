#!/usr/bin/env python3
"""Genera las plantillas (.nbt), los NPC y las ubicaciones de los gimnasios/ligas de emi_gimnasios y los deja en emi-estructuras.
Uso: python3 construir.py <carpeta tools/emi-estructuras> [<config extra-locations.json del pack>]"""
import json, os, sys
import gymlib

def _libre(b): return b.split('[')[0] in ('air', 'cave_air', 'void_air')


def _firme(b):
    n = b.split('[')[0].replace('minecraft:', '')
    if _libre(b) or n in ('water', 'lava') or 'slab' in n or 'stairs' in n or 'fence' in n or 'wall' in n or 'carpet' in n or n.endswith(('_bars', 'pane', 'cobweb', 'campfire', 'lantern', 'candle', 'torch')): return False
    return True


def ubicar_npc(v, x, y, z):
    """busca el sitio mas cercano donde el NPC pueda estar de pie: 2 bloques de aire y un bloque firme debajo (primero cambiando la altura, luego los lados)"""
    cands = [(0, 0)] + [(dx, dz) for r in (1, 2) for dx in range(-r, r + 1) for dz in range(-r, r + 1) if max(abs(dx), abs(dz)) == r]
    for (dx, dz) in cands:
        for dy in (0, -1, 1, -2, 2, -3, 3):
            xx, yy, zz = int(x) + dx, int(y) + dy, int(z) + dz
            if _libre(v.get(xx, yy, zz)) and _libre(v.get(xx, yy + 1, zz)) and _firme(v.get(xx, yy - 1, zz)):
                return xx + (x - int(x)), yy, zz + (z - int(z)), (dx, dy, dz) != (0, 0, 0)
    return x, y, z, False


def registrar(nombre, v, npcs, region, etiqueta, aliases, orden_fuera=None):
    """nombre = ruta de la plantilla (p. ej. teselia/elesa); npcs = [(x, y, z, yaw_antes_de_girar, comando)] en coordenadas de la plantilla sin girar"""
    w0 = v.w
    g = gymlib.girar(v, 1)           # la fachada pasa de mirar al norte a mirar al oeste (la entrada segura de Emipokemon esta al oeste)
    lista = []
    for (x, y, z, yaw, cmd) in npcs:
        x2, y2, z2, movido = ubicar_npc(v, x, y, z)
        if movido: print(f'    (NPC {cmd.split()[-1]} de {nombre} movido de ({int(x)},{y},{int(z)}) a ({int(x2)},{y2},{int(z2)}))')
        x, y, z = x2, y2, z2
        e = {'x': round(z + 0.5, 2), 'y': y, 'z': round(w0 - 1 - x + 0.5, 2), 'yaw': (yaw - 90) % 360, 'cmd': cmd}
        p = cmd.split()
        if len(p) == 4 and p[:2] == ['emipokemon', 'desafios']: e['id'] = f'challenge_{p[2]}_{p[3]}'   # id interno del NPC en Emipokemon (para moverlo si ya existe)
        lista.append(e)
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
