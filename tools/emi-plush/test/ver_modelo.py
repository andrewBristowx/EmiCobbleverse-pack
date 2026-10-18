#!/usr/bin/env python3
"""Vista previa rapida (sin Minecraft) de un modelo de Cobblemon/Bedrock: dibuja los cubos con su textura desde varios angulos y los junta en una imagen.
Solo pose de reposo (no aplica animaciones ni la rotacion de los huesos, solo la de los cubos). Sirve para iterar un accesorio en segundos antes de probarlo en el juego.
uso: python3 test/ver_modelo.py <modelo.geo.json> <textura.png> <salida.png> [--vistas frente,3/4,lado,espalda,arriba,3/4atras] [--sin acc_dino_eye,...]"""
import json, math, os, sys
import numpy as np
from PIL import Image
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'gen'))
from emi_lib import box_rects

VISTAS = {'frente': (0, 0), '3/4': (-35, 12), 'lado': (-90, 0), 'espalda': (180, 0), 'arriba': (-20, 60), '3/4atras': (-145, 12)}
LUZ = np.array([-0.4, 0.8, -0.5]); LUZ = LUZ / np.linalg.norm(LUZ)      # en el espacio de la camara (x derecha, y abajo, z hacia dentro): arriba a la izquierda y de frente

def rot(r):
    rx, ry, rz = [math.radians(v) for v in r]
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]); Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]); Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx

def caras(c, piv_hueso):
    """Las 6 caras del cubo como (nombre, 4 vertices TL TR BR BL, normal) en coordenadas del modelo."""
    o, s = c['origin'], c['size']; x0, y0, z0 = o; x1, y1, z1 = o[0] + s[0], o[1] + s[1], o[2] + s[2]
    f = {'north': ([(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)], (0, 0, -1)),
         'south': ([(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)], (0, 0, 1)),
         'east': ([(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)], (1, 0, 0)),
         'west': ([(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)], (-1, 0, 0)),
         'up': ([(x1, y1, z1), (x0, y1, z1), (x0, y1, z0), (x1, y1, z0)], (0, 1, 0)),
         'down': ([(x1, y0, z0), (x0, y0, z0), (x0, y0, z1), (x1, y0, z1)], (0, -1, 0))}
    R = None
    if c.get('rotation'):
        R = rot(c['rotation']); piv = np.array(c.get('pivot') or piv_hueso, float)
    out = []
    for n, (vs, nor) in f.items():
        v = np.array(vs, float); nn = np.array(nor, float)
        if R is not None: v = (v - piv) @ R.T + piv; nn = R @ nn
        out.append((n, v, nn))
    return out

def render(geo, tex, yaw, pitch, tam=520, escala=7.0, centro=(0, 22, 0), sin=()):
    img = np.zeros((tam, tam, 4), float); img[..., :3] = (0.93, 0.93, 0.96); img[..., 3] = 1
    zb = np.full((tam, tam), 1e9)
    T = np.asarray(tex.convert('RGBA'), float) / 255.0; th, tw = T.shape[:2]
    ya, pi = math.radians(yaw), math.radians(pitch)
    Ry = np.array([[math.cos(ya), 0, math.sin(ya)], [0, 1, 0], [-math.sin(ya), 0, math.cos(ya)]])
    Rp = np.array([[1, 0, 0], [0, math.cos(pi), -math.sin(pi)], [0, math.sin(pi), math.cos(pi)]])
    V = Rp @ Ry
    cen = np.array(centro, float)
    for b in geo['bones']:
        if any(b['name'].startswith(p) for p in sin): continue
        for c in b.get('cubes', []):
            rects = box_rects(c['uv'][0], c['uv'][1], *c['size']) if isinstance(c['uv'], list) else None
            for nombre, vs, nor in caras(c, b.get('pivot', [0, 0, 0])):
                if rects is None or nombre not in rects: continue
                x, y, w, h = rects[nombre]
                if w <= 0 or h <= 0: continue
                p = (vs - cen) @ V.T                     # espacio de la camara: x (hacia -x del modelo = derecha de la pantalla), y arriba, z hacia dentro
                nc = V @ nor
                if nc[2] > 1e-6: continue                # cara de espaldas a la camara
                sh = 0.5 + 0.5 * max(0.0, float(np.dot(np.array([-nc[0], -nc[1], nc[2]]), LUZ)) * -1) if False else 0.55 + 0.45 * max(0.0, float(np.dot(np.array([nc[0], -nc[1], -nc[2]]), np.array([-0.35, 0.75, 0.55]))))
                px = tam / 2 - p[:, 0] * escala; py = tam / 2 - p[:, 1] * escala
                uvs = np.array([(x, y), (x + w, y), (x + w, y + h), (x, y + h)], float)
                for tri in ((0, 1, 2), (0, 2, 3)):
                    a, bb, cc = tri
                    X = px[[a, bb, cc]]; Y = py[[a, bb, cc]]
                    x0, x1 = int(max(0, math.floor(X.min()))), int(min(tam - 1, math.ceil(X.max())))
                    y0, y1 = int(max(0, math.floor(Y.min()))), int(min(tam - 1, math.ceil(Y.max())))
                    if x1 < x0 or y1 < y0: continue
                    den = (Y[1] - Y[2]) * (X[0] - X[2]) + (X[2] - X[1]) * (Y[0] - Y[2])
                    if abs(den) < 1e-9: continue
                    gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
                    l0 = ((Y[1] - Y[2]) * (gx - X[2]) + (X[2] - X[1]) * (gy - Y[2])) / den
                    l1 = ((Y[2] - Y[0]) * (gx - X[2]) + (X[0] - X[2]) * (gy - Y[2])) / den
                    l2 = 1 - l0 - l1
                    m = (l0 >= -1e-6) & (l1 >= -1e-6) & (l2 >= -1e-6)
                    if not m.any(): continue
                    z = l0 * p[a, 2] + l1 * p[bb, 2] + l2 * p[cc, 2]
                    u = l0 * uvs[a, 0] + l1 * uvs[bb, 0] + l2 * uvs[cc, 0]; v = l0 * uvs[a, 1] + l1 * uvs[bb, 1] + l2 * uvs[cc, 1]
                    ti = np.clip(np.floor(u).astype(int), 0, tw - 1); tj = np.clip(np.floor(v).astype(int), 0, th - 1)
                    col = T[tj, ti]
                    ok = m & (col[..., 3] > 0.5) & (z < zb[y0:y1 + 1, x0:x1 + 1])
                    if not ok.any(): continue
                    zb[y0:y1 + 1, x0:x1 + 1][ok] = z[ok]
                    img[y0:y1 + 1, x0:x1 + 1, :3][ok] = col[..., :3][ok] * sh
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), 'RGBA').convert('RGB')

def main():
    a = sys.argv[1:]
    geo = json.load(open(a[0]))['minecraft:geometry'][0]; tex = Image.open(a[1]); salida = a[2]
    vistas = ['frente', '3/4', 'lado', 'espalda']; sin = ()
    if '--vistas' in a: vistas = a[a.index('--vistas') + 1].split(',')
    if '--sin' in a: sin = tuple(a[a.index('--sin') + 1].split(','))
    ims = [render(geo, tex, *VISTAS[v], sin=sin) for v in vistas]
    cols = 2 if len(ims) <= 4 else 3
    filas = (len(ims) + cols - 1) // cols
    out = Image.new('RGB', (cols * ims[0].width, filas * ims[0].height), (255, 255, 255))
    for i, im in enumerate(ims): out.paste(im, ((i % cols) * im.width, (i // cols) * im.height))
    out.save(salida); print(salida, out.size)

if __name__ == '__main__': main()
