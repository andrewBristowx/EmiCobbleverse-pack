#!/usr/bin/env python3
"""Peluches de los Pokemon Emi (bloques de Pokeblocks): a partir de los modelos `*_emi.geo.json` ya generados crea
 - assets/emi_plush/geo/<n>.geo.json (modelo reducido, sin huesos escondidos, UV por cara) y su textura
 - blockstate, modelos de bloque y objeto, tabla de botin, textura de particula y textos de idioma
 - src/emi/plush/gen/*.java (una clase de entidad de bloque por peluche y el registro)
Uso: python3 gen_dolls.py <carpeta resources> <carpeta src/emi/plush/gen>"""
import json, os, re, sys, copy, shutil
from PIL import Image
from emi_lib import box_rects

RES, SRC = sys.argv[1], sys.argv[2]
B = f'{RES}/assets/cobblemon'
A = f'{RES}/assets/emi_plush'

# especie, carpeta, modelo (nombre del geo sin .geo.json), textura, nombre en español, objetivo de altura (unidades de modelo)
ESPECIES = [
    ('happiny', '0440_happiny', 'happiny', 'happiny', 'Happiny', 14), ('chansey', '0113_chansey', 'chansey', 'chansey', 'Chansey', 14), ('blissey', '0242_blissey', 'blissey', 'blissey', 'Blissey', 15),
    ('cleffa', '0173_cleffa', 'cleffa', 'cleffa', 'Cleffa', 12), ('clefairy', '0035_clefairy', 'clefairy', 'clefairy', 'Clefairy', 14), ('clefable', '0036_clefable', 'clefable', 'clefable', 'Clefable', 15),
    ('igglybuff', '0174_igglybuff', 'igglybuff', 'igglybuff', 'Igglybuff', 12), ('jigglypuff', '0039_jigglypuff', 'jigglypuff', 'jigglypuff', 'Jigglypuff', 13), ('wigglytuff', '0040_wigglytuff', 'wigglytuff', 'wigglytuff', 'Wigglytuff', 15),
    ('eevee', '0133_eevee', 'eevee_male', 'eevee', 'Eevee', 13), ('vaporeon', '0134_vaporeon', 'vaporeon', 'vaporeon', 'Vaporeon', 14), ('jolteon', '0135_jolteon', 'jolteon', 'jolteon', 'Jolteon', 14),
    ('flareon', '0136_flareon', 'flareon', 'flareon', 'Flareon', 14), ('espeon', '0196_espeon', 'espeon', 'espeon', 'Espeon', 15), ('umbreon', '0197_umbreon', 'umbreon', 'umbreon', 'Umbreon', 15),
    ('leafeon', '0470_leafeon', 'leafeon', 'leafeon', 'Leafeon', 15), ('glaceon', '0471_glaceon', 'glaceon', 'glaceon', 'Glaceon', 15), ('sylveon', '0700_sylveon', 'sylveon', 'sylveon', 'Sylveon', 15),
    ('ralts', '0280_ralts', 'ralts', 'ralts', 'Ralts', 12), ('kirlia', '0281_kirlia', 'kirlia', 'kirlia', 'Kirlia', 14), ('gardevoir', '0282_gardevoir', 'gardevoir', 'gardevoir', 'Gardevoir', 16),
]
# los brazos de la pose base salen en T: se bajan (grados) para el peluche
BRAZOS_ABAJO = {'ralts': 65, 'kirlia': 65, 'gardevoir': 55}
# huesos que se dejan caer (grados, hacia abajo; el signo se saca de la posicion en x del pivote)
POSE_CAIDA = {'sylveon': [(('ribbon_neck_left', 'ribbon_neck_right'), 28), (('ribbon_ear_left', 'ribbon_ear_right'), 40)],
              'clefairy': [(('arm_left', 'arm_right'), 32)], 'clefable': [(('arm_left', 'arm_right'), 30)],
              'wigglytuff': [(('arm_left', 'arm_right'), 30)], 'cleffa': [(('arm_left', 'arm_right'), 28)]}
# proporciones de peluche: (escala de la cabeza, escala del cuerpo) con el hueso `head` como raiz de la cabeza
CHIBI = {**{e: (1.4, 0.9) for e in ('eevee', 'vaporeon', 'jolteon', 'flareon', 'espeon', 'umbreon', 'leafeon', 'glaceon', 'sylveon')},
         'ralts': (1.25, 1.0), 'kirlia': (1.35, 0.9), 'gardevoir': (1.7, 0.8), 'chansey': (1.2, 1.0), 'happiny': (1.25, 1.0)}
OCULTOS = re.compile(r'lid|closed|expression|locator|mouth_open|yawn|sleep|blink|^stone|stone_|egg_body|egg_torso|egg_pouch|^eyes$|angry|sad|happy_')

def per_face(c):
    if isinstance(c['uv'], dict): return c['uv']
    sx, sy, sz = c['size']
    r = box_rects(c['uv'][0], c['uv'][1], sx, sy, sz)
    out = {}
    for f, (x, y, w, h) in r.items():
        if w <= 0 or h <= 0: continue
        if sz == 0 and f != 'north': continue
        if sx == 0 and f not in ('east',): continue
        if sy == 0 and f != 'up': continue
        out[f] = {'uv': [x, y], 'uv_size': [w, h]}
    return out

def bounds(bs):
    ys = []
    for b in bs:
        for c in b.get('cubes', []):
            ys += [c['origin'][1], c['origin'][1] + c['size'][1]]
    return min(ys), max(ys)

def build(sp, folder, model, tex, nombre, alto):
    g = json.load(open(f'{B}/bedrock/pokemon/models/{folder}/{model}_emi.geo.json'))
    geo = g['minecraft:geometry'][0]
    bs = [b for b in geo['bones'] if not OCULTOS.search(b['name'])]
    names = {b['name'] for b in bs}
    bs = copy.deepcopy(bs)
    for b in bs:
        for c in b.get('cubes', []): c['uv'] = per_face(c)
    if sp in CHIBI:
        hs, bsc = CHIBI[sp]
        raiz = next(b for b in bs if b['name'] == 'head')
        grupo = {'head'}
        while True:
            nuevos = {b['name'] for b in bs if b.get('parent') in grupo} - grupo
            if not nuevos: break
            grupo |= nuevos
        N = raiz['pivot']; N2 = [v * bsc for v in N]
        for b in bs:
            if b['name'] in grupo: m = lambda p, N=N, N2=N2: [N2[i] + hs * (p[i] - N[i]) for i in range(3)]; k = hs
            else: m = lambda p: [v * bsc for v in p]; k = bsc
            if 'pivot' in b: b['pivot'] = m(b['pivot'])
            for c in b.get('cubes', []):
                c['origin'] = m(c['origin']); c['size'] = [v * k for v in c['size']]
                if 'pivot' in c: c['pivot'] = m(c['pivot'])
                if 'inflate' in c: c['inflate'] *= k
    lo, hi = bounds(bs)
    f = min(1.0, alto / (hi - lo))
    out = []
    for b in bs:
        nb = {'name': b['name']}
        if b.get('parent') in names: nb['parent'] = b['parent']
        nb['pivot'] = [round(v * f, 4) for v in b.get('pivot', [0, 0, 0])]
        if 'rotation' in b: nb['rotation'] = b['rotation']
        if sp in BRAZOS_ABAJO and b['name'] in ('arm_left', 'arm_right'): nb['rotation'] = [0, 0, BRAZOS_ABAJO[sp] * (1 if b['name'] == 'arm_left' else -1)]
        for pref, ang in POSE_CAIDA.get(sp, ()):
            if b['name'] in pref: nb['rotation'] = [0, 0, ang if b['pivot'][0] > 0 else -ang]
        cs = []
        for c in b.get('cubes', []):
            nc = {'origin': [round(v * f, 4) for v in c['origin']], 'size': [round(v * f, 4) for v in c['size']], 'uv': per_face(c)}
            if 'inflate' in c: nc['inflate'] = round(c['inflate'] * f, 4)
            if 'pivot' in c: nc['pivot'] = [round(v * f, 4) for v in c['pivot']]; nc['rotation'] = c['rotation']
            cs.append(nc)
        if cs: nb['cubes'] = cs
        out.append(nb)
    d = geo['description']
    ng = {'format_version': '1.12.0', 'minecraft:geometry': [{'description': {'identifier': f'geometry.{sp}_emi_doll', 'texture_width': d['texture_width'], 'texture_height': d['texture_height'], 'visible_bounds_width': 4, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 0.75, 0]}, 'bones': out}]}
    os.makedirs(f'{A}/geo', exist_ok=True); os.makedirs(f'{A}/textures/entity', exist_ok=True); os.makedirs(f'{A}/textures/block', exist_ok=True)
    json.dump(ng, open(f'{A}/geo/{sp}_emi.geo.json', 'w'), separators=(',', ':'))
    shutil.copy(f'{B}/textures/pokemon/{folder}/{tex}_emi.png', f'{A}/textures/entity/{sp}_emi.png')
    Image.new('RGBA', (16, 16), (240, 200, 215, 255)).save(f'{A}/textures/block/{sp}_emi_particle.png')
    return f

def recursos(sp, nombre):
    n = f'{sp}_emi'
    os.makedirs(f'{A}/blockstates', exist_ok=True); os.makedirs(f'{A}/models/block', exist_ok=True); os.makedirs(f'{A}/models/item', exist_ok=True)
    os.makedirs(f'{RES}/data/emi_plush/loot_table/blocks', exist_ok=True)
    json.dump({'variants': {'': {'model': f'emi_plush:block/{n}'}}}, open(f'{A}/blockstates/{n}.json', 'w'), indent=2)
    json.dump({'parent': 'builtin/entity', 'texture_size': [16, 16], 'textures': {'particle': f'emi_plush:block/{n}_particle'}}, open(f'{A}/models/block/{n}.json', 'w'), indent=2)
    item = json.load(open(f'{A}/models/item/michi_dramatico.json')); item['parent'] = f'emi_plush:block/{n}'
    json.dump(item, open(f'{A}/models/item/{n}.json', 'w'), indent=2)
    json.dump({'type': 'minecraft:block', 'pools': [{'bonus_rolls': 0.0, 'conditions': [{'condition': 'minecraft:survives_explosion'}], 'entries': [{'type': 'minecraft:item', 'name': f'emi_plush:{n}'}], 'rolls': 1.0}]}, open(f'{RES}/data/emi_plush/loot_table/blocks/{n}.json', 'w'), indent=2)

def lang():
    textos = {'en_us': 'Emi {n} Plush', 'es_es': 'Peluche {n} de Emi', 'es_mx': 'Peluche {n} de Emi'}
    for code, plantilla in textos.items():
        p = f'{A}/lang/{code}.json'
        d = json.load(open(p, encoding='utf-8'))
        for sp, folder, model, tex, nombre, alto in ESPECIES:
            t = plantilla.format(n=nombre)
            d[f'block.emi_plush.{sp}_emi'] = t; d[f'item.emi_plush.{sp}_emi'] = t
        json.dump(d, open(p, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)

def java():
    os.makedirs(SRC, exist_ok=True)
    for f in os.listdir(SRC):
        if f.endswith('.java'): os.remove(f'{SRC}/{f}')
    for sp, *_ in ESPECIES:
        cls = 'Doll_' + sp
        open(f'{SRC}/{cls}.java', 'w').write(f'''package emi.plush.gen;

import dev.mrshawn.pokeblocks.block.entity.BlockEntityTypeRegistry;
import dev.mrshawn.pokeblocks.block.entity.PokedollBlockEntity;
import net.minecraft.class_2338;
import net.minecraft.class_2680;

/** Entidad de bloque del peluche {sp}_emi (generada por gen_dolls.py). */
public class {cls} extends PokedollBlockEntity {{
    public {cls}(class_2338 pos, class_2680 state) {{
        super(BlockEntityTypeRegistry.get({cls}.class), pos, state);
    }}
}}
''')
    regs = '\n'.join(f'        reg("{sp}_emi", Doll_{sp}.class, Doll_{sp}::new);' for sp, *_ in ESPECIES)
    open(f'{SRC}/DollRegistry.java', 'w').write(f'''package emi.plush.gen;

import dev.mrshawn.pokeblocks.block.custom.PokedollBlock;
import dev.mrshawn.pokeblocks.block.entity.BlockEntityTypeRegistry;
import dev.mrshawn.pokeblocks.block.entity.PokedollBlockEntity;
import dev.mrshawn.pokeblocks.item.DollRarity;
import dev.mrshawn.pokeblocks.item.custom.PokedollBlockItem;
import emi.plush.EmiDollItemModel;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.class_1792;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_2378;
import net.minecraft.class_2591;
import net.minecraft.class_2680;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** Registro de todos los peluches Emi (generado por gen_dolls.py). */
public final class DollRegistry {{
    private DollRegistry() {{}}

    public static final Map<String, class_2591<? extends PokedollBlockEntity>> TIPOS = new LinkedHashMap<>();
    public static final Map<String, class_1792> OBJETOS = new LinkedHashMap<>();

    private static <T extends PokedollBlockEntity> void reg(String nombre, Class<T> clase, BiFunction<class_2338, class_2680, T> ctor) {{
        class_2960 id = class_2960.method_60655("emi_plush", nombre);
        class_2248 bloque = class_2378.method_10230(class_7923.field_41175, id, new PokedollBlock<T>(() -> clase));
        class_1792 objeto = class_2378.method_10230(class_7923.field_41178, id, new PokedollBlockItem(bloque, DollRarity.NONE, 0, () -> new EmiDollItemModel(nombre)));
        class_2591<T> tipo = class_2378.method_10230(class_7923.field_41181, id, FabricBlockEntityTypeBuilder.create(ctor::apply, bloque).build());
        BlockEntityTypeRegistry.register(clase, tipo);
        TIPOS.put(nombre, tipo);
        OBJETOS.put(nombre, objeto);
    }}

    public static void registrar() {{
{regs}
    }}
}}
''')

for sp, folder, model, tex, nombre, alto in ESPECIES:
    f = build(sp, folder, model, tex, nombre, alto)
    recursos(sp, nombre)
    print(f'{sp}: escala {f:.2f}')
lang()
java()
