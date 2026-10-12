"""Revisa las plantillas generadas buscando bloques que Minecraft rompería al colocarse (sin apoyo)."""
import sys, glob, os, nbtlib2
from collections import Counter

SOLIDO_NO = ('air', 'cave_air', 'void_air', 'water', 'lava', 'glass_pane', 'iron_bars', 'chain', 'lantern', 'candle',
             'torch', 'wall_torch', 'carpet', 'ladder', 'vine', 'azalea', 'flowering_azalea', 'lightning_rod',
             'amethyst_cluster', 'large_amethyst_bud', 'medium_amethyst_bud', 'small_amethyst_bud', 'skull', 'head',
             'banner', 'sign', 'button', 'pressure_plate', 'rail', 'redstone', 'fire', 'flower_pot', 'end_rod', 'cobweb',
             'pointed_dripstone', 'sea_pickle', 'scaffolding', 'tripwire', 'lever', 'repeater', 'comparator')
TIERRA = ('dirt', 'grass_block', 'podzol', 'mycelium', 'coarse_dirt', 'rooted_dirt', 'moss_block', 'mud', 'farmland', 'clay',
          'muddy_mangrove_roots', 'sand', 'red_sand', 'terracotta', 'snow_block')

def nombre(b): return b.split('[')[0].replace('minecraft:', '')
def es_solido(n):
    return n not in ('air', 'cave_air', 'void_air') and not any(n == s or n.endswith('_' + s) or n.endswith(s) and s in ('glass_pane', 'bars') for s in SOLIDO_NO)

NO_FIRME = ('leaves', 'slab', 'stairs', 'fence', 'fence_gate', '_wall', 'trapdoor', '_door', '_pane', 'bars', '_bed', 'chest', 'sapling',
            'flower', 'roots', 'azalea', '_cluster', '_bud', 'vine', 'vines', 'ladder', 'chain', 'lantern', 'candle', 'carpet', 'head', 'skull')
def es_firme(n):
    """cara completa donde puede agarrarse una estalactita o una amatista (las hojas, losas, escaleras... no valen)"""
    return es_solido(n) and not n.endswith(NO_FIRME) and n not in ('red_mushroom', 'brown_mushroom', 'short_grass', 'fern', 'tall_grass', 'cobweb')

def revisar(path):
    r = nbtlib2.parse(open(path, 'rb').read())
    pal = []
    for p in r['palette']:
        props = p.get('Properties', {})
        pal.append((nombre(p['Name']), props))
    sx, sy, sz = r['size']
    g = {}
    for b in r['blocks']:
        x, y, z = b['pos']; g[(x, y, z)] = pal[b['state']]
    prob = Counter(); ej = {}
    for (x, y, z), (n, pr) in g.items():
        def at(dx, dy, dz): return g.get((x + dx, y + dy, z + dz), ('air', {}))
        err = None
        if n == 'lantern':
            if pr.get('hanging') == 'true':
                a = at(0, 1, 0)[0]
                if not es_solido(a) and a != 'chain': err = 'linterna colgante sin soporte'
            else:
                a = at(0, -1, 0)[0]
                if not es_solido(a): err = 'linterna apoyada en aire/no solido'
        elif n in ('azalea', 'flowering_azalea'):
            if at(0, -1, 0)[0] not in TIERRA: err = 'azalea no sobre tierra: ' + at(0, -1, 0)[0]
        elif n in ('torch',) or n.endswith('_candle') or n == 'candle':
            if not es_solido(at(0, -1, 0)[0]): err = n + ' sin base'
        elif n.endswith('carpet'):
            if at(0, -1, 0)[0] in ('air', 'cave_air'): err = 'alfombra sin base'
        elif n.endswith('pressure_plate') or n.endswith('_rail') or n == 'rail':
            if not es_solido(at(0, -1, 0)[0]): err = n + ' sin base'
        elif n in ('lightning_rod',) or n == 'end_rod':
            pass
        elif n.endswith('_door'):
            if pr.get('half') == 'lower' and not es_solido(at(0, -1, 0)[0]): err = 'puerta sin base'
        elif n in ('short_grass', 'grass', 'fern', 'tall_grass', 'dandelion', 'poppy', 'azure_bluet', 'cornflower', 'lily_of_the_valley',
                   'allium', 'oxeye_daisy', 'red_tulip', 'pink_tulip', 'orange_tulip', 'white_tulip', 'blue_orchid', 'lilac', 'peony',
                   'rose_bush', 'sunflower', 'sweet_berry_bush', 'bamboo', 'sugar_cane', 'cactus'):
            if at(0, -1, 0)[0] not in TIERRA and n not in ('cactus', 'sugar_cane', 'bamboo'): err = n + ' no sobre tierra: ' + at(0, -1, 0)[0]
        elif n == 'amethyst_cluster' or n.endswith('amethyst_bud'):
            f = pr.get('facing', 'up'); d = {'up': (0, -1, 0), 'down': (0, 1, 0), 'north': (0, 0, 1), 'south': (0, 0, -1), 'east': (-1, 0, 0), 'west': (1, 0, 0)}[f]
            if not es_firme(at(*d)[0]): err = 'amatista sin soporte (' + f + ')'
        elif n == 'pointed_dripstone':
            d = pr.get('vertical_direction', 'down')
            ref = at(0, 1, 0) if d == 'down' else at(0, -1, 0)
            if not (es_firme(ref[0]) or ref[0] == 'pointed_dripstone'): err = 'estalactita/estalagmita sin soporte'
        elif n == 'hanging_roots':
            a = at(0, 1, 0)[0]
            if not (es_firme(a) or a == 'hanging_roots'): err = 'raices colgantes sin techo'
        elif n in ('red_mushroom', 'brown_mushroom'):
            if at(0, -1, 0)[0] not in ('podzol', 'mycelium'): err = 'seta pequeña sin podzol/micelio'
        elif n == 'cactus':
            if at(0, -1, 0)[0] not in ('sand', 'red_sand', 'cactus'): err = 'cactus sin arena'
            elif any(es_solido(at(dx, 0, dz)[0]) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))): err = 'cactus pegado a un bloque'
        elif n in ('dead_bush',):
            if at(0, -1, 0)[0] not in TIERRA + ('sand', 'red_sand', 'terracotta', 'red_terracotta', 'orange_terracotta', 'yellow_terracotta', 'brown_terracotta', 'white_terracotta'): err = 'arbusto seco sin tierra'
        elif n == 'snow':
            if not es_solido(at(0, -1, 0)[0]): err = 'capa de nieve sin base'
        elif n == 'lily_pad':
            if at(0, -1, 0)[0] != 'water': err = 'nenufar sin agua'
        elif n == 'bamboo':
            if at(0, -1, 0)[0] not in TIERRA + ('bamboo',): err = 'bambu sin tierra'
        elif n in ('ladder',):
            f = pr.get('facing', 'north'); d = {'north': (0, 0, 1), 'south': (0, 0, -1), 'east': (-1, 0, 0), 'west': (1, 0, 0)}[f]
            if not es_solido(at(*d)[0]): err = 'escalera de mano sin pared'
        elif n.endswith('wall_torch') or n.endswith('wall_banner') or n.endswith('wall_sign') or n.endswith('wall_skull') or n.endswith('wall_head'):
            f = pr.get('facing', 'north'); d = {'north': (0, 0, 1), 'south': (0, 0, -1), 'east': (-1, 0, 0), 'west': (1, 0, 0)}[f]
            if not es_solido(at(*d)[0]): err = n + ' sin pared'
        elif n in ('vine', 'glow_lichen'):
            pass
        elif n == 'chain':
            pass
        if n in ('water', 'lava') and 'level' not in pr:
            # el fluido no debe tener aire (ni el borde de la plantilla) a los lados o debajo: se derramaria
            for (dx, dy, dz) in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
                nx, ny, nz = x + dx, y + dy, z + dz
                if ny < 0: continue      # bajo la plantilla esta el suelo del mundo plano
                if not (0 <= nx < sx and 0 <= ny < sy and 0 <= nz < sz): err = 'fluido en el borde de la plantilla'; break
                nb = g.get((nx, ny, nz), ('air', {}))[0]
                if nb in ('air', 'cave_air'): err = n + ' se derramaria (aire al lado/debajo)'; break
        if err:
            prob[err] += 1; ej.setdefault(err, (x, y, z))
    return prob, ej

if __name__ == '__main__':
    d = sys.argv[1]
    for f in sorted(glob.glob(os.path.join(d, '*.nbt'))):
        prob, ej = revisar(f)
        print('==', os.path.basename(f), 'OK' if not prob else '')
        for e, n in prob.most_common(): print('   %4d  %s  (ej. %s)' % (n, e, ej[e]))
