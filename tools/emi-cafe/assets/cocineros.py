#!/usr/bin/env python3
"""
Los iconos de los tres cocineros del Cafe (Charmander, Squirtle y Bulbasaur) hechos con los MODELOS REALES de Cobblemon: se dibuja cada modelo de frente (3/4) con su textura,
con los brazos caidos, sin llamas ni lianas, y se le pone un gorro de cocinero hecho de cubos. Sale un PNG de 64x64 por cocinero en src/main/resources/assets/emi_cafe/textures/item.
Los modelos son de Cobblemon (licencia Creative Commons no comercial, ver la carpeta del modelo en el jar); estos iconos derivados siguen esa misma licencia.
Uso:  COBBLEMON_JAR=<jar de Cobblemon> python3 assets/cocineros.py [--vista salida.png]
"""
import copy, json, os, sys, zipfile, io
import numpy as np
from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'emi-plush', 'test'))
sys.path.insert(0, os.path.join(AQUI, '..', '..', 'emi-plush', 'gen'))
import ver_modelo as vm
from emi_lib import box_rects

SALIDA = os.path.join(AQUI, '..', 'src', 'main', 'resources', 'assets', 'emi_cafe', 'textures', 'item')
BLANCO = (250, 247, 250); SOMBRA = (222, 226, 240); SOMBRA2 = (196, 202, 224)

def bola(face, i, j, w, h):
    c = BLANCO
    if (i * 3 + j * 5) % 9 == 0: c = SOMBRA
    if (i * 7 + j * 2) % 23 == 0: c = SOMBRA2
    return c + (255,)

def banda(face, i, j, w, h): return (SOMBRA if i % 4 == 0 else BLANCO) + (255,)

def hacer(geo, tex, quitar, cubos_nuevos, brazos, huesos_sombrero):
    """Devuelve (geo, textura) listos: sin los huesos de `quitar`, con los brazos caidos y el gorro (cubos_nuevos = [(origen, tam, pintor)])."""
    g = copy.deepcopy(geo)
    g['bones'] = [b for b in g['bones'] if not any(b['name'].startswith(p) for p in quitar)]
    for b in g['bones']:
        if b['name'] in brazos: b['cubes'] = [{'origin': brazos[b['name']][0], 'size': brazos[b['name']][1], 'uv': b['cubes'][0]['uv']}] if brazos[b['name']] else []
    t = Image.new('RGBA', (tex.width, tex.height + 128), (0, 0, 0, 0)); t.paste(tex.convert('RGBA'), (0, 0))
    px = t.load(); y = tex.height
    hueso = next(b for b in g['bones'] if b['name'] == huesos_sombrero)
    hueso.setdefault('cubes', [])
    x = 0
    for origen, tam, pint in cubos_nuevos:
        sx, sy, sz = tam
        ancho, alto = 2 * sz + 2 * sx, sz + sy
        if x + ancho > t.width: x = 0; y += 20
        for face, (fx, fy, fw, fh) in box_rects(x, y, sx, sy, sz).items():
            for i in range(fw):
                for j in range(fh): px[fx + i, fy + j] = pint(face, i, j, fw, fh)
        hueso['cubes'].append({'origin': list(origen), 'size': list(tam), 'uv': [x, y]})
        x += ancho
    return g, t

def dibujar(g, t, yaw=-28, pitch=10, salida=64):
    """El modelo ya dibujado a 512 px, recortado a lo que se ve y reducido a un cuadrado de `salida` px con fondo transparente."""
    fondo = (237, 237, 244)
    cajas = [(c['origin'], c['size']) for b in g['bones'] for c in b.get('cubes', [])]
    ys = [o[1] for o, s in cajas] + [o[1] + s[1] for o, s in cajas]
    xs = [o[0] for o, s in cajas] + [o[0] + s[0] for o, s in cajas]
    zs = [o[2] for o, s in cajas] + [o[2] + s[2] for o, s in cajas]
    cen = ((min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2, (min(zs) + max(zs)) / 2)
    alto = max(max(ys) - min(ys), (max(xs) - min(xs)) * 0.9)
    im = vm.render(g, t, yaw, pitch, tam=512, escala=420 / alto, centro=cen)
    a = np.asarray(im.convert('RGB'), dtype=float)
    masc = ~np.all(np.abs(a - np.array(fondo)) < 1.5, axis=2)
    ys_, xs_ = np.where(masc)
    y0, y1, x0, x1 = ys_.min(), ys_.max() + 1, xs_.min(), xs_.max() + 1
    lado = max(y1 - y0, x1 - x0) + 16
    cy, cx = (y0 + y1) // 2, (x0 + x1) // 2
    ny0, nx0 = cy - lado // 2, cx - lado // 2
    L = np.zeros((lado, lado, 4), float)
    for yy in range(lado):
        sy = ny0 + yy
        if 0 <= sy < 512:
            for_x0, for_x1 = max(0, nx0), min(512, nx0 + lado)
            L[yy, for_x0 - nx0:for_x1 - nx0, :3] = a[sy, for_x0:for_x1]
            L[yy, for_x0 - nx0:for_x1 - nx0, 3] = masc[sy, for_x0:for_x1]
    pre = L.copy(); pre[..., :3] *= pre[..., 3:4]
    P = Image.fromarray(pre.astype(np.float32), 'RGBA') if False else None
    # reduccion con el color premultiplicado (para que los bordes no salgan oscuros)
    canales = [Image.fromarray(pre[..., k].astype(np.float32), 'F').resize((salida, salida), Image.BOX) for k in range(4)]
    r = np.stack([np.asarray(c) for c in canales], axis=2)
    al = r[..., 3]
    rgb = np.where(al[..., None] > 0.01, r[..., :3] / np.maximum(al[..., None], 0.01), 0)
    out = np.dstack([rgb, np.where(al > 0.45, 255, 0)])
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), 'RGBA')

def cargar(zf, carpeta, nombre):
    geo = json.load(zf.open(f'assets/cobblemon/bedrock/pokemon/models/{carpeta}/{nombre}.geo.json'))['minecraft:geometry'][0]
    tex = Image.open(io.BytesIO(zf.read(f'assets/cobblemon/textures/pokemon/{carpeta}/{nombre}.png')))
    return geo, tex

def main():
    jar = os.environ.get('COBBLEMON_JAR')
    if not jar: raise SystemExit('define COBBLEMON_JAR')
    zf = zipfile.ZipFile(jar)
    hojas = []
    # --- Charmander: brazos colgando, sin llama; el gorro encima de la cabeza (cabeza = x -4..4, y 13..21, z -2.5..3.5)
    geo, tex = cargar(zf, '0004_charmander', 'charmander')
    g, t = hacer(geo, tex, quitar=('fire', 'elbow_', 'eyelid_'), brazos={'arm_right': ([-7, 8, -1.5], [3, 5, 3]), 'arm_left': ([4, 8, -1.5], [3, 5, 3])},
                 cubos_nuevos=[((-4.6, 21, -3.1), (9.2, 2, 7.2), banda), ((-5.8, 23, -4.3), (11.6, 4, 9.6), bola), ((-4.8, 27, -3.3), (9.6, 3, 7.6), bola)], huesos_sombrero='head_angle')
    hojas.append(('cocinero_charmander', dibujar(g, t)))
    # --- Squirtle: brazos colgando; cabeza = x -4..4, y 12..19, z -5.5..1.5
    geo, tex = cargar(zf, '0007_squirtle', 'squirtle')
    g, t = hacer(geo, tex, quitar=('elbow_', 'fingers_', 'glasses', 'eyelid_', 'locator'), brazos={'arm_right': ([-7.5, 6, -2], [3, 5, 3]), 'arm_left': ([4.5, 6, -2], [3, 5, 3])},
                 cubos_nuevos=[((-4.6, 19, -6.1), (9.2, 2, 8.2), banda), ((-5.8, 21, -7.3), (11.6, 4, 10.6), bola), ((-4.8, 25, -6.3), (9.6, 3, 8.6), bola)], huesos_sombrero='head_rotation')
    hojas.append(('cocinero_squirtle', dibujar(g, t)))
    # --- Bulbasaur: sin lianas ni orejas planas; cabeza = x -4.5..4.5, y 4.5..10.5, z -8.75..-1.75
    geo, tex = cargar(zf, '0001_bulbasaur', 'bulbasaur')
    g, t = hacer(geo, tex, quitar=('vine', 'ear_', 'eyelid_', 'pattern'), brazos={},
                 cubos_nuevos=[((-4.8, 11, -9.05), (9.6, 2, 7.6), banda), ((-5.8, 13, -10.05), (11.6, 4, 9.6), bola), ((-4.9, 17, -9.15), (9.8, 3, 7.8), bola)], huesos_sombrero='head')
    for b in g['bones']:                                          # los ojos pegados a la cara parpadeaban (z-fighting): se adelantan 0,15
        if b['name'].startswith(('eye_', 'pupil')):
            for cu in b.get('cubes', []): cu['origin'][2] -= 0.15
    hojas.append(('cocinero_bulbasaur', dibujar(g, t, yaw=-12, pitch=4)))
    os.makedirs(SALIDA, exist_ok=True)
    for n, im in hojas:
        im.save(os.path.join(SALIDA, n + '.png')); print('escrito', n)
    if '--vista' in sys.argv:
        out = Image.new('RGBA', (3 * 256 + 40, 276), (70, 60, 90, 255))
        for i, (n, im) in enumerate(hojas): out.paste(im.resize((256, 256), Image.NEAREST), (10 + i * 266, 10), im.resize((256, 256), Image.NEAREST))
        out.save(sys.argv[sys.argv.index('--vista') + 1])

if __name__ == '__main__': main()
