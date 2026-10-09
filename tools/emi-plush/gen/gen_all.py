#!/usr/bin/env python3
"""Genera todas las variantes Emi: Happiny/Chansey/Blissey (a mano) y el resto con el motor generico. Uso: COBBLEMON_JAR=... gen_all.py <resources> [especies,...]"""
import sys, os, json
OUT = sys.argv[1]
only = sys.argv[2].split(',') if len(sys.argv) > 2 else None
sys.argv = [sys.argv[0], OUT]
import gen_emi_poke as P
import emi_species as E
Z = E.cobblemon_jar()
done = []
for sp, fn, folder in [('happiny', P.happiny, '0440_happiny'), ('chansey', P.chansey, '0113_chansey'), ('blissey', P.blissey, '0242_blissey')]:
    if only and sp not in only: continue
    g, a = fn(); P.finish_species(sp, folder, g, a); done.append(sp)
for sp, cfg in E.CONFIG.items():
    if only and sp not in only: continue
    E.write_species(OUT, Z, sp, cfg); done.append(sp)
# aspecto: indicador `emi` para todas las especies
al = sorted(set(done) | set(json.load(open(f'{OUT}/data/cobblemon/species_feature_assignments/emi.json'))['pokemon'] if os.path.exists(f'{OUT}/data/cobblemon/species_feature_assignments/emi.json') else []))
os.makedirs(f'{OUT}/data/cobblemon/species_feature_assignments', exist_ok=True)
json.dump({'pokemon': al, 'features': ['emi']}, open(f'{OUT}/data/cobblemon/species_feature_assignments/emi.json', 'w'))
print('listo', done)
