#!/usr/bin/env python3
"""
Genera el datapack EmiLootr-DP: convierte los cofres/barriles de las estructuras con contenido FIJO (items escritos a mano en la plantilla,
sin tabla de botin) en contenedores con tabla de botin, para que Lootr los convierta en cofres Lootr (cada jugador abre el suyo).
 - minecraft:chest / trapped_chest / barrel y los cofres dorados de Cobblemon (*gilded_chest): Items -> LootTable.
 - Sophisticated Storage y Carved Wood (barril/cofre): pasan a minecraft:barrel / chest / trapped_chest (Lootr no los convierte) con LootTable.
 - Se dejan como estan estanterias, vitrinas, hornos, dispensadores, shulkers... (decoracion o funcionales).
Cada contenido unico genera una tabla emi_lootr:<hash> con los mismos objetos y cantidades (cada objeto siempre sale).
Uso: convert.py [--items ids.txt] <salida.zip> <origen.zip|jar>...
--items: un id de objeto por linea (los que existen en el juego; se obtienen con el comando de depuracion /emiestructuras _items de emi_estructuras); los demas se omiten.
"""
import sys, zipfile, gzip, hashlib, json, io, os, collections
from nbt import load, dump, P, L

LOOT_KEYS = (b'minecraft:chest', b'minecraft:barrel', b'minecraft:trapped_chest', b'gilded_chest', b'sophisticatedstorage:', b'carved_wood:')

def js(x):
    if isinstance(x, dict): return {k: js(v) for k, v in x.items()}
    if isinstance(x, L): return [js(v) for v in x]
    if x.t == 1 and x.v in (0, 1): return bool(x.v)   # los componentes booleanos (p. ej. resolved) vienen como byte 0/1 y el codec JSON no los acepta
    if x.t in (7,): return list(x.v)
    if x.t in (11, 12): return list(x.v)
    return x.v

def stack_entry(it):
    """Items[i] (1.21: {Slot, id, count, components?}) -> pool de tabla de botin."""
    d = {'type': 'minecraft:item', 'name': it['id'].v}
    f = [{'function': 'minecraft:set_count', 'count': it['count'].v if 'count' in it else (it['Count'].v if 'Count' in it else 1)}]
    comp = it.get('components')
    if comp:
        f.append({'function': 'minecraft:set_components', 'components': js(comp)})
    d['functions'] = f
    return {'rolls': 1, 'entries': [d]}

VALID = None   # ids de objetos que existen en el juego (--items); un id desconocido hace que Minecraft rechace TODA la tabla

def table_for(items):
    pools = []
    for it in sorted(items, key=lambda i: i['Slot'].v if 'Slot' in i else 0):
        if VALID is not None and it['id'].v not in VALID:
            STATS['objeto_desconocido_omitido'] += 1
            continue
        pools.append(stack_entry(it))
    return {'type': 'minecraft:chest', 'pools': pools}

def container_kind(name):
    if name.startswith('sophisticatedstorage:'):
        if 'shulker' in name: return None
        if 'barrel' in name: return 'soph_barrel'
        if 'chest' in name: return 'soph_chest'
        return None
    if name in ('minecraft:chest', 'minecraft:trapped_chest', 'minecraft:barrel'): return 'vanilla'
    if name.startswith('carved_wood:'):
        if 'trapped' in name and name.endswith('chest'): return 'carved_trapped'
        if name.endswith('_chest') or name.endswith(':chest'): return 'carved_chest'
        if name.endswith('_barrel') or name.endswith(':barrel'): return 'carved_barrel'
    if name.startswith('cobblemon:') and name.endswith('gilded_chest'): return 'vanilla'
    return None

def soph_items(nb):
    try: return nb['storageWrapper']['contents']['inventory']['Items']
    except Exception: return None

STATS = collections.Counter()

def main():
    global VALID
    args = sys.argv[1:]
    if args and args[0] == '--items':
        VALID = set(l.strip() for l in open(args[1], encoding='utf-8') if l.strip()); args = args[2:]
    out, srcs = args[0], args[1:]
    tables = {}
    files = {}
    stats = STATS
    for src in srcs:
        zf = zipfile.ZipFile(src)
        for n in zf.namelist():
            if not n.endswith('.nbt') or not n.startswith('data/') or '/structure/' not in n: continue
            raw = gzip.decompress(zf.read(n))
            if not any(k in raw for k in LOOT_KEYS): continue
            try: name, root = load(zf.read(n))
            except Exception: continue
            if not isinstance(root, dict) or 'blocks' not in root: continue
            pal = root.get('palette')
            if pal is None: continue    # plantillas con varias paletas: no hay ninguna en estas fuentes
            kinds = {i: container_kind(p['Name'].v) for i, p in enumerate(pal)}
            changed = False
            for b in root['blocks']:
                k = kinds.get(b['state'].v)
                if not k: continue
                nb = b.get('nbt')
                if nb is None: continue
                if k == 'vanilla' or k.startswith('carved_'):
                    items = nb.get('Items')
                    if 'LootTable' in nb or items is None or len(items) == 0: continue
                    tbl = table_for(items)
                    h = hashlib.sha1(json.dumps(tbl, sort_keys=True).encode()).hexdigest()[:12]
                    tables[h] = tbl
                    del nb['Items']; nb['LootTable'] = P(8, 'emi_lootr:' + h)
                    if k.startswith('carved_'): nb['id'] = P(8, {'carved_barrel': 'minecraft:barrel', 'carved_chest': 'minecraft:chest', 'carved_trapped': 'minecraft:trapped_chest'}[k])
                    stats['vanilla' if k == 'vanilla' else k] += 1; changed = True
                else:
                    items = soph_items(nb)
                    h = None
                    if items is not None and len(items) > 0:
                        tbl = table_for(items)
                        h = hashlib.sha1(json.dumps(tbl, sort_keys=True).encode()).hexdigest()[:12]
                        tables[h] = tbl
                    bid = 'minecraft:barrel' if k == 'soph_barrel' else 'minecraft:chest'
                    new = {'id': P(8, bid)}
                    if h: new['LootTable'] = P(8, 'emi_lootr:' + h)
                    b['nbt'] = new
                    stats[k + ('_con_botin' if h else '_vacio')] += 1; changed = True
            # paleta: los bloques de Sophisticated Storage pasan a vanilla (solo si se convirtio algun bloque)
            for i, p in enumerate(pal):
                nm = p['Name'].v
                kk = container_kind(nm)
                if kk in ('soph_barrel', 'soph_chest', 'carved_barrel', 'carved_chest', 'carved_trapped'):
                    props = p.get('Properties')
                    facing = props['facing'].v if props and 'facing' in props else 'north'
                    typ = props['type'].v if props and 'type' in props and props['type'].v in ('single', 'left', 'right') else 'single'
                    wl = props['waterlogged'].v if props and 'waterlogged' in props else 'false'
                    isb = kk.endswith('barrel')
                    p['Name'] = P(8, 'minecraft:barrel' if isb else ('minecraft:trapped_chest' if kk == 'carved_trapped' else 'minecraft:chest'))
                    np = {'facing': P(8, facing)}
                    if isb: np['open'] = P(8, 'false')
                    else: np['type'] = P(8, typ); np['waterlogged'] = P(8, wl)
                    p['Properties'] = np
            if changed:
                files[n] = dump(name, root)
                stats['plantillas'] += 1
        print(src, dict(stats), flush=True)
    with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('pack.mcmeta', json.dumps({'pack': {'pack_format': 48, 'description': 'EmiLootr: cofres y barriles con contenido fijo de las estructuras pasan a tabla de botin (Lootr)'}}, indent=2))
        for n, b in files.items(): z.writestr(n, b)
        for h, t in tables.items(): z.writestr('data/emi_lootr/loot_table/%s.json' % h, json.dumps(t, separators=(',', ':')))
    print('listo', dict(stats), 'tablas', len(tables), '->', out)

main()
