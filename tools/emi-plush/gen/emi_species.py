"""Motor generico: convierte cualquier Pokemon de Cobblemon en su variante Emi (aspecto `emi`).
 1) retinta la textura por grupos de color (k-means) hacia la paleta de Emi (plata, rosa, negro, piel clara)
 2) ojos de dos colores (naranja a la derecha del Pokemon, magenta a la izquierda)
 3) pelo plateado con puntas rosas colocado automaticamente alrededor de la cabeza (se detecta por la posicion de los ojos)"""
import json, os, re, math
import numpy as np
from PIL import Image
from emi_lib import *
from gen_emi_poke import (SILVER, SILVER_D, SILVER_L, BLACK, BLACK_L, PINK_H, PINK_L, PINK_P, GOLD, SKIN, ORANGE_L, ORANGE, MAGENTA_L, MAGENTA, mix, hair_painter)

WHITE = (250, 247, 250)
PINKS = (248, 190, 215)

class Overflow(Exception): pass

def eye_cubes(geo):
    out = []
    for b in bones(geo):
        n = b['name']
        if re.search(r'lid|expression|locator|closed|lines|line_|^eyes$', n): continue
        if re.search(r'(^|_)(eye|iris)(s)?(_|$)', n) and ('left' in n or 'right' in n):
            for c in b.get('cubes', []): out.append((b, c))
    return out

def paint_eyes(geo, img):
    px = img.load()
    for b, c in eye_cubes(geo):
        cx = c['origin'][0] + c['size'][0] / 2
        l, d = (MAGENTA_L, MAGENTA) if cx > 0 else (ORANGE_L, ORANGE)   # +x = lado izquierdo del Pokemon (derecha vista de frente)
        rects = cube_rects(c)
        for face in ('north', 'south'):
            if face not in rects: continue
            x, y, w, h = rects[face]
            for i in range(w):
                for j in range(h):
                    if not (0 <= x + i < img.width and 0 <= y + j < img.height): continue
                    r, g, bl, a = px[x + i, y + j]
                    if a == 0: continue
                    lu = lum((r, g, bl))
                    if lu > 190: continue                       # reflejos blancos se quedan
                    t = max(0.0, min(1.0, lu / 230.0))
                    px[x + i, y + j] = mix(d, l, t) + (255,)

def find_head(geo):
    """Cubo de la cabeza: el mas pequeño que contiene los ojos (por la cara delantera). Devuelve (hueso, cubo)."""
    eyes = eye_cubes(geo)
    if not eyes: return None
    ex = sum(c['origin'][0] + c['size'][0] / 2 for _, c in eyes) / len(eyes)
    ey = sum(c['origin'][1] + c['size'][1] / 2 for _, c in eyes) / len(eyes)
    ez = min(c['origin'][2] for _, c in eyes)
    best = None
    for b in bones(geo):
        for c in b.get('cubes', []):
            ox, oy, oz = c['origin']; sx, sy, sz = c['size']
            if min(sx, sy, sz) < 1.5: continue
            if ox - 0.6 <= ex <= ox + sx + 0.6 and oy - 0.6 <= ey <= oy + sy + 0.6 and abs(oz - ez) <= 1.6:
                v = sx * sy * sz * (1000 if b['name'].startswith('head') else 1)
                if best is None or v > best[0]: best = (v, b, c)
    if not best: return None
    b, c = best[1], dict(best[2])
    # si el cubo es mucho mas alto que ancho (cuerpo y cabeza en una pieza) la "cabeza" es la parte alta
    W, H = c['size'][0], c['size'][1]
    if H > 1.15 * W:
        top = c['origin'][1] + H
        c['origin'] = [c['origin'][0], top - W, c['origin'][2]]
        c['size'] = [c['size'][0], W, c['size'][2]]
    return b, c

def add_hair(geo, atlas, head, opts):
    """Pelo de Emi, todo CONECTADO (cada pieza se solapa con la siguiente, nada flota): tapa que cubre lo alto, paneles finos a los lados y por detras
    (la cabeza queda abierta por delante), flequillo con raya al medio pegado a la tapa, y mechones que cuelgan de los bordes de los paneles
    (delanteros y laterales, mmas largos hacia atras) y tiras traseras de distinto largo. Las puntas son rosas.
    Las medidas salen del tamaño de la cabeza (no del torso, para las especies donde la cabeza es el cuerpo entero)."""
    b, c = head
    x0, y0, z0 = c['origin']; W, H, D = c['size']
    x1, y1, z1 = x0 + W, y0 + H, z0 + D
    Hs = min(H, W, 9)                                # las cabezas enormes (el cuerpo entero) no se cubren de pelo: se acota
    parent = b['name']
    piv = [0, y1, 0]
    eyes = eye_cubes(geo)
    eye_top = max(cc['origin'][1] + cc['size'][1] for _, cc in eyes)
    room = max(1, int(y1 - eye_top))
    ri = lambda v: max(1, int(round(v)))
    solid = hair_painter(tip=None)
    cx = (x0 + x1) / 2
    Wc = ri(W); Dc = ri(D)
    xa = cx - Wc / 2.0
    top = y1 + 0.9                                   # techo de la tapa (la tapa va de y1-0.1 a y1+0.9)
    # tapa
    add_cube(geo, atlas, 'emi_hair_top', parent, piv, [xa, y1 - 0.1, z0 - 0.1], [Wc, 1, Dc + 1], solid, inflate=0.02)
    # paneles laterales: de arriba hasta media cabeza, de delante a atras
    hs = ri(Hs * 0.55)
    ybot_side = top - hs - 1
    for sx in (-1, 1):
        xs = x1 - 0.05 if sx > 0 else x0 - 0.95
        add_cube(geo, atlas, 'emi_side_l' if sx > 0 else 'emi_side_r', parent, piv, [xs, ybot_side, z0 - 0.1], [1, hs + 1, Dc + 1], hair_painter(tip=None), inflate=0.02)
    # panel trasero: casi toda la espalda de la cabeza
    hb = ri(min(H, Hs * 1.3) * 0.8)
    ybot_back = top - hb - 1
    add_cube(geo, atlas, 'emi_hair_nape', parent, piv, [xa, ybot_back, z1 - 0.05], [Wc, hb + 1, 1], hair_painter(tip=None), inflate=0.02)
    # flequillo con raya al medio, pegado a la tapa
    n = 6
    wseg = max(1, round(W / n))
    heights = [2, 2, 1, 1, 2, 2]
    for i in range(n):
        h = min(ri(heights[i] * max(0.8, Hs / 7.0)), room)
        add_cube(geo, atlas, 'emi_bangs', parent, piv, [round(x0 + i * W / n, 2), y1 - h + 0.3, z0 - 0.9], [wseg, h, 1], solid)
    # mechones que cuelgan de los paneles laterales (uno por delante y otro mas largo hacia atras)
    for sx in (-1, 1):
        nm = 'emi_lock_l' if sx > 0 else 'emi_lock_r'
        xo = x1 - 0.05 if sx > 0 else x0 - 0.95
        for zf, ln in ((0.0, 0.5), (0.5, 0.75)):
            Lb = y0 - min(Hs * ln * 0.8, 4.0 * ln / 0.75)
            top_l = ybot_side + 0.5
            add_cube(geo, atlas, nm, parent, piv, [xo, Lb, z0 + D * zf - 0.1], [1, ri(top_l - Lb), max(1, ri(D * 0.3))], hair_painter(tip_from=0.45))
    # tiras traseras (centro mas larga) que cuelgan del panel trasero
    cw = max(1, round(Wc * 0.9 / 3))
    xs0 = cx - cw * 1.5
    for i, ln in enumerate((0.6, 0.95, 0.6)):
        Lb = y0 - min(Hs * ln * 0.9, 4.5 * ln / 0.95)
        add_cube(geo, atlas, 'emi_back', parent, piv, [xs0 + i * cw, Lb, z1 - 0.05], [cw, ri(ybot_back + 0.5 - Lb), 1], hair_painter(tip_from=0.4))

def build_variant(Z, sp, folder, model, texp, cfg):
    geo, tex = load_geo_tex(Z, folder, model, texp)
    desc = geo['minecraft:geometry'][0]['description']
    if (desc.get('texture_width'), desc.get('texture_height')) != tex.size:
        print('  AVISO', sp, model, 'descripcion', desc.get('texture_width'), desc.get('texture_height'), 'png', tex.size)
    k = cfg.get('k', 6)
    centers, share = kmeans(tex, k)
    img = remap_clusters(tex, centers, cfg['map'], keep=cfg.get('keep', 0.85))
    # zonas que se vuelven transparentes (p. ej. el flequillo de Ralts, que tapa los ojos): (hueso, filas de la cara)
    for bn, rows in cfg.get('clear', []):
        for b in bones(geo):
            if b['name'] != bn: continue
            for cb in b.get('cubes', []):
                for face in ('north', 'south'):
                    r = cube_rects(cb).get(face)
                    if not r: continue
                    x, y, w, h = r
                    for j in rows:
                        for i in range(w):
                            if 0 <= x + i < img.width and 0 <= y + j < img.height: img.putpixel((x + i, y + j), (0, 0, 0, 0))
    rows = 64
    while True:
        try:
            atlas = Atlas(img, rows)
            paint_eyes(geo, atlas.img)
            head = find_head(geo)
            if head is None:
                raise SystemExit(f'{sp}: no se encontro la cabeza (ojos)')
            add_hair(copy.deepcopy(geo) if False else geo, atlas, head, cfg)
            break
        except SystemExit as e:
            if 'Atlas lleno' in str(e):
                rows *= 2
                geo, _ = load_geo_tex(Z, folder, model, texp)
                if rows > 1024: raise
                continue
            raise
    print(f'  {sp}/{model}: cabeza={head[0]["name"]} {head[1]["size"]} atlas+{rows}')
    return geo, atlas

def write_species(OUT, Z, sp, cfg):
    folder, variants = base_for(Z, sp)
    # variantes con modelo propio: la base (sin aspectos) y las que cambian de modelo (p. ej. Eevee hembra)
    base_tex = [v for v in variants if v[2]][0]
    mdir = f'{OUT}/assets/cobblemon/bedrock/pokemon/models/{folder}'
    tdir = f'{OUT}/assets/cobblemon/textures/pokemon/{folder}'
    rdir = f'{OUT}/assets/cobblemon/bedrock/pokemon/resolvers/{folder}'
    for d in (mdir, tdir, rdir): os.makedirs(d, exist_ok=True)
    res_vars = []
    first = True
    for (aspects, model, texp, poser) in variants:
        tex_path = texp or base_tex[2]
        geo, atlas = build_variant(Z, sp, folder, model, tex_path, cfg)
        geo, img = finish(geo, atlas, f'geometry.{model}_emi')
        g = geo['minecraft:geometry'][0]['description']; g['texture_width'] = img.width; g['texture_height'] = img.height
        json.dump(geo, open(f'{mdir}/{model}_emi.geo.json', 'w'), separators=(',', ':'))
        tname = (os.path.basename(tex_path)[:-4] if texp else model) + '_emi'   # una variante sin textura propia (Eevee hembra) se pinta aparte: sus ojos estan en otro sitio
        img.save(f'{tdir}/{tname}.png')
        v = {'aspects': ['emi'] + [a for a in aspects], 'model': f'cobblemon:{model}_emi.geo', 'texture': f'cobblemon:textures/pokemon/{folder}/{tname}.png', 'layers': []}
        if poser: v['poser'] = poser
        res_vars.append(v)
        sh = img.copy(); sp_ = sh.load()
        for x in range(sh.width):
            for y in range(sh.height):
                r, gg, b, a = sp_[x, y]
                if a: sp_[x, y] = (r, int(gg * 0.85), min(255, int(b * 1.1)), a)
        sh.save(f'{tdir}/{tname}_shiny.png')
        res_vars.append({'aspects': ['emi', 'shiny'] + [a for a in aspects], 'texture': f'cobblemon:textures/pokemon/{folder}/{tname}_shiny.png'})
    json.dump({'species': f'cobblemon:{sp}', 'order': 1, 'variations': res_vars}, open(f'{rdir}/1_{sp}_emi.json', 'w'), indent=2)

# ---------------------------------------------------------------------------------------------- configuracion por especie
# map: color objetivo para cada uno de los 6 grupos de color de la textura original, de oscuro a claro (None = no tocar)
PINKLAD = [BLACK, PINK_H, PINK_L, PINKS, SKIN, WHITE]
CONFIG = {
    'cleffa':     dict(map=[BLACK, PINK_H, PINK_L, PINKS, SKIN, WHITE]),
    'clefairy':   dict(map=[BLACK, PINK_H, PINK_L, PINKS, SKIN, WHITE]),
    'clefable':   dict(map=[BLACK, PINK_H, PINK_L, PINKS, SKIN, WHITE]),
    'igglybuff':  dict(map=[BLACK, PINK_H, PINK_L, PINKS, SKIN, WHITE]),
    'jigglypuff': dict(map=[BLACK, PINK_H, PINK_L, PINKS, SKIN, WHITE]),
    'wigglytuff': dict(map=[BLACK, PINK_H, PINK_L, PINKS, SKIN, WHITE]),
    'eevee':      dict(map=[BLACK, SILVER_D, SILVER_D, SILVER, SILVER_L, PINK_L]),
    'vaporeon':   dict(map=[BLACK, PINK_H, PINK_L, PINKS, SKIN, WHITE]),
    'jolteon':    dict(map=[BLACK, SILVER_D, SILVER, SILVER, SILVER_L, WHITE]),
    'flareon':    dict(map=[PINK_H, PINK_L, SILVER_D, SILVER, SILVER_L, WHITE]),
    'espeon':     dict(map=[BLACK, PINK_H, PINK_L, PINKS, SKIN, WHITE]),
    'umbreon':    dict(map=[(28, 8, 32), (58, 20, 56), (86, 30, 80), (112, 40, 98), (138, 54, 118), PINK_H]),
    'leafeon':    dict(map=[BLACK, PINK_H, PINK_L, SILVER_D, SILVER, SILVER_L]),
    'glaceon':    dict(map=[BLACK, PINK_H, PINK_L, SILVER_D, SILVER, SILVER_L]),
    'sylveon':    dict(map=[PINK_H, PINK_H, PINK_L, PINKS, SKIN, WHITE]),
    'ralts':      dict(map=[PINK_H, SILVER_D, SILVER_D, SILVER, SILVER_L, SKIN], clear=[('hair_front', (2, 3))]),
    'kirlia':     dict(map=[PINK_H, SILVER_D, SILVER_D, SILVER, SILVER_L, SKIN]),
    'gardevoir':  dict(map=[PINK_H, SILVER_D, SILVER_D, SILVER, SILVER_L, SKIN]),
}
