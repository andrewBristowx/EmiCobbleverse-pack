"""Utilidades para retocar los modelos base de Cobblemon (geo de Bedrock + textura) y crear variantes "Emi"."""
import json, os, io, zipfile, copy
from PIL import Image

PINK = (240, 98, 156)

def cobblemon_jar():
    j = os.environ.get('COBBLEMON_JAR')
    if not j or not os.path.exists(j):
        raise SystemExit('Falta COBBLEMON_JAR (ruta al jar de Cobblemon)')
    return zipfile.ZipFile(j)

def load_base(z, folder, name):
    geo = json.loads(z.read(f'assets/cobblemon/bedrock/pokemon/models/{folder}/{name}.geo.json'))
    tex = Image.open(io.BytesIO(z.read(f'assets/cobblemon/textures/pokemon/{folder}/{name}.png'))).convert('RGBA')
    return geo, tex

def bones(geo):
    return geo['minecraft:geometry'][0]['bones']

def bone(geo, name):
    for b in bones(geo):
        if b['name'] == name: return b
    raise KeyError(name)

def box_rects(u, v, sx, sy, sz):
    """Rectangulos (x, y, w, h) de cada cara de un cubo con UV de caja de Bedrock."""
    u, v, sx, sy, sz = [int(round(t)) if not isinstance(t, int) else t for t in (u, v, sx, sy, sz)]
    return {'east': (u, v + sz, sz, sy), 'north': (u + sz, v + sz, sx, sy), 'west': (u + sz + sx, v + sz, sz, sy),
            'south': (u + 2 * sz + sx, v + sz, sx, sy), 'up': (u + sz, v, sx, sz), 'down': (u + sz + sx, v, sx, sz)}

def cube_rects(c):
    if isinstance(c['uv'], dict):   # UV por cara
        out = {}
        for k, f in c['uv'].items():
            out[k] = (int(f['uv'][0]), int(f['uv'][1]), int(abs(f['uv_size'][0])), int(abs(f['uv_size'][1])))
        return out
    return box_rects(c['uv'][0], c['uv'][1], *c['size'])

def bone_rects(geo, names):
    rs = []
    for n in names:
        for c in bone(geo, n).get('cubes', []):
            rs += [r for r in cube_rects(c).values() if r[2] > 0 and r[3] > 0]
    return rs

def lum(p): return 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]

def retint(img, rects, target, keep=1.0, floor=0.0):
    """Cambia el color de las zonas conservando el relieve (sombras y ruido) de la textura original."""
    px = img.load()
    pts = set()
    for (x, y, w, h) in rects:
        for i in range(x, x + w):
            for j in range(y, y + h):
                if 0 <= i < img.width and 0 <= j < img.height and px[i, j][3] > 0: pts.add((i, j))
    if not pts: return
    mean = sum(lum(px[p]) for p in pts) / len(pts)
    for (i, j) in pts:
        a = px[i, j]
        f = lum(a) / mean if mean > 0 else 1.0
        f = 1.0 + (f - 1.0) * keep
        f = max(floor, f)
        px[i, j] = (min(255, int(target[0] * f)), min(255, int(target[1] * f)), min(255, int(target[2] * f)), a[3])

class Atlas:
    """Reserva huecos nuevos en la textura (ampliada hacia abajo) para las piezas añadidas."""
    def __init__(self, img, extra_rows):
        self.base_h = img.height
        self.img = Image.new('RGBA', (img.width, img.height + extra_rows), (0, 0, 0, 0))
        self.img.paste(img, (0, 0))
        self.x, self.y, self.rowh = 0, self.base_h, 0
    def alloc(self, w, h):
        if self.x + w > self.img.width:
            self.x = 0; self.y += self.rowh; self.rowh = 0
        if self.y + h > self.img.height: raise SystemExit('Atlas lleno')
        r = (self.x, self.y); self.x += w; self.rowh = max(self.rowh, h)
        return r

def add_cube(geo, atlas, bone_name, parent, pivot, origin, size, painter, rotation=None, inflate=0.0):
    """Añade un cubo (en un hueso nuevo si no existe) y lo pinta con painter(face, x, y, w, h) -> (r,g,b,a)."""
    sx, sy, sz = size
    W = int(2 * sz + 2 * sx); H = int(sz + sy)
    ux, uy = atlas.alloc(W, H)
    rects = box_rects(ux, uy, sx, sy, sz)
    px = atlas.img.load()
    for face, (x, y, w, h) in rects.items():
        for i in range(w):
            for j in range(h):
                px[x + i, y + j] = painter(face, i, j, w, h)
    try:
        b = bone(geo, bone_name)
    except KeyError:
        b = {'name': bone_name, 'parent': parent, 'pivot': list(pivot), 'cubes': []}
        bones(geo).append(b)
    c = {'origin': list(origin), 'size': list(size), 'uv': [ux, uy]}
    if inflate: c['inflate'] = inflate
    if rotation:
        c['pivot'] = list(pivot); c['rotation'] = list(rotation)
    b.setdefault('cubes', []).append(c)
    return c

def finish(geo, atlas, new_id):
    g = geo['minecraft:geometry'][0]
    g['description']['identifier'] = new_id
    g['description']['texture_height'] = atlas.img.height
    return geo, atlas.img

# ---------------------------------------------------------------------------------------------- generico (todas las especies)
import numpy as np, re

def base_for(z, species):
    """Lee el resolver de la especie y devuelve (carpeta, [(aspectos, modelo, textura, poser)]) de las variantes con modelo."""
    names = z.namelist()
    res = [n for n in names if n.endswith('.json') and 'bedrock/pokemon/resolvers/' in n and re.search(r'/\d+_' + species + r'/', n)]
    folder = re.search(r'/(\d+_' + species + r')/', res[0]).group(1)
    out = []
    for r in sorted(res):
        d = json.loads(z.read(r))
        for v in d['variations']:
            if v.get('model'):
                out.append((v.get('aspects', []), v['model'].split(':')[1][:-4], v['texture'].split(':')[1] if v.get('texture') else None, v.get('poser')))
    return folder, out

def load_geo_tex(z, folder, model, tex_path):
    geo = json.loads(z.read(f'assets/cobblemon/bedrock/pokemon/models/{folder}/{model}.geo.json'))
    tex = Image.open(io.BytesIO(z.read('assets/cobblemon/' + tex_path))).convert('RGBA')
    return geo, tex

LUM = np.array([0.299, 0.587, 0.114])

def kmeans(img, k, iters=25):
    a = np.array(img)
    m = a[:, :, 3] > 0
    px = a[m][:, :3].astype(float)
    lum_ = px @ LUM
    order = np.argsort(lum_)
    c = px[order[(np.linspace(0.05, 0.95, k) * (len(px) - 1)).astype(int)]]
    for _ in range(iters):
        d = ((px[:, None, :] - c[None, :, :]) ** 2).sum(-1)
        lab = d.argmin(1)
        for i in range(k):
            if (lab == i).any(): c[i] = px[lab == i].mean(0)
    d = ((px[:, None, :] - c[None, :, :]) ** 2).sum(-1); lab = d.argmin(1)
    cnt = np.bincount(lab, minlength=k)
    idx = np.argsort(c @ LUM)   # de oscuro a claro
    return c[idx], cnt[idx] / len(px)

def remap_clusters(img, centers, targets, keep=0.85):
    """Cada pixel opaco se asigna a su color mas cercano y se pinta con el color objetivo de ese grupo conservando el relieve."""
    a = np.array(img).astype(float)
    m = a[:, :, 3] > 0
    px = a[m][:, :3]
    d = ((px[:, None, :] - centers[None, :, :]) ** 2).sum(-1)
    lab = d.argmin(1)
    cl = centers @ LUM
    pl = px @ LUM
    ratio = np.where(cl[lab] > 1, pl / np.maximum(cl[lab], 1), 1.0)
    ratio = 1.0 + (ratio - 1.0) * keep
    tg = np.array([targets[i] if targets[i] is not None else centers[i] for i in lab], dtype=float)
    out = np.clip(tg * ratio[:, None], 0, 255)
    res = a.copy()
    res[m, :3] = out
    return Image.fromarray(res.astype(np.uint8), 'RGBA')
