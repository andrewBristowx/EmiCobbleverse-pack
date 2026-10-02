#!/usr/bin/env python3
"""Genera datapacks/EmiCostumes-DP.zip: asocia cada item emipokemon:costume_<slug> con el aspecto real de
Styles Upon Styles (sus_), Fire Fits for Fire Cats (firefits_) y Midnight Meet & Glitz (midnight_).

Uso: build_costume_datapack.py <sus.zip> <firefits.zip> <midnight_datapack.zip> <salida.zip>
El slug <prefijo>_<especie>_<aspecto> debe coincidir con una entrada de data/cobblemon/cosmetic_items de su pack;
si algun slug no se puede emparejar, el script falla en vez de generar un datapack incompleto.
"""
import json, sys, zipfile, os

here = os.path.dirname(os.path.abspath(__file__))
slugs = [l.strip() for l in open(os.path.join(here, 'slugs.txt')) if l.strip()]

def entries(path):
    out = []
    with zipfile.ZipFile(path) as z:
        for n in sorted(z.namelist()):
            if '/cosmetic_items/' in n and n.endswith('.json'):
                d = json.loads(z.read(n))
                base = os.path.splitext(os.path.basename(n))[0]
                for c in d['cosmeticItems']:
                    out.append(dict(file=base, pokemon=d['pokemon'], aspects=c['aspects']))
    return out

def main(sus, ff, mg, dest):
    packs = {'sus': entries(sus), 'firefits': entries(ff), 'midnight': entries(mg)}
    defs = {}
    for slug in slugs:
        prefix, rest = slug.split('_', 1)
        hits = {(tuple(e['pokemon']), tuple(e['aspects'])): e
                for e in packs[prefix] for sp in e['pokemon'] for a in e['aspects']
                if rest == f"{sp.replace(' ', '_')}_{a}"}
        if not hits:
            hits = {(tuple(e['pokemon']), tuple(e['aspects'])): e for e in packs[prefix] if rest == e['file']}
        if len(hits) != 1:
            sys.exit(f'No se pudo emparejar {slug}: {len(hits)} candidatos')
        e = next(iter(hits.values()))
        defs[slug] = {"pokemon": e['pokemon'],
                      "cosmeticItems": [{"consumedItem": "emipokemon:costume_" + slug, "aspects": e['aspects']}]}
    with zipfile.ZipFile(dest, 'w', zipfile.ZIP_DEFLATED) as z:
        def add(name, data):
            zi = zipfile.ZipInfo(name, (2026, 1, 1, 0, 0, 0))
            zi.compress_type = zipfile.ZIP_DEFLATED; zi.external_attr = 0o644 << 16
            z.writestr(zi, data)
        add('pack.mcmeta', json.dumps({"pack": {"pack_format": 48, "description": "EmiCobbleverse: conecta los items emipokemon:costume_* con los disfraces de Cobblemon (Styles Upon Styles, Fire Fits, Midnight Meet & Glitz)"}}, indent=2, ensure_ascii=False))
        for slug in slugs:
            add(f'data/cobblemon/cosmetic_items/emipokemon_costumes/{slug}.json', json.dumps(defs[slug], indent=2) + '\n')
    print(f'{len(slugs)} disfraces -> {dest}')

if __name__ == '__main__':
    if len(sys.argv) != 5: sys.exit(__doc__)
    main(*sys.argv[1:])
