"""Constructor de estructuras (voxeles) que guarda en el NBT de plantilla de Minecraft 1.21 (para /place template o StructureTemplate)."""
import re, os
import nbtlib2
from nbtlib2 import L, Byte

DATA_VERSION = 3955   # 1.21.1

def parse_estado(s):
    m = re.match(r'^([a-z0-9_:]+)(?:\[(.*)\])?$', s)
    nombre = m.group(1)
    if ':' not in nombre: nombre = 'minecraft:' + nombre
    props = {}
    if m.group(2):
        for kv in m.group(2).split(','):
            k, v = kv.split('='); props[k.strip()] = v.strip()
    if nombre.endswith('_leaves') and 'persistent' not in props: props['persistent'] = 'true'   # sin esto las hojas se caen solas (no hay troncos cerca)
    return nombre, props

OPUESTO_ESC = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}

class V:
    def __init__(s, w, h, l):
        s.w, s.h, s.l = w, h, l
        s.g = {}
        s.nbt = {}          # (x,y,z) -> compuesto de entidad de bloque
        s.entidades = []    # [{'pos':[x,y,z], 'nbt':{...}}]
    # ---------------------------------------------------------- basicos
    def dentro(s, x, y, z): return 0 <= x < s.w and 0 <= y < s.h and 0 <= z < s.l
    def set(s, x, y, z, b):
        if s.dentro(x, y, z): s.g[(x, y, z)] = b
    def get(s, x, y, z): return s.g.get((x, y, z), 'air')
    def fill(s, x0, y0, z0, x1, y1, z1, b):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1): s.set(x, y, z, b)
    def box(s, x0, y0, z0, x1, y1, z1, b, interior='air'):
        """caja hueca: paredes/suelo/techo de b; el interior (por defecto aire)"""
        s.fill(x0, y0, z0, x1, y1, z1, b)
        if interior is not None and x1 - x0 > 1 and y1 - y0 > 1 and z1 - z0 > 1:
            s.fill(x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, interior)
    def pared_x(s, x, y0, y1, z0, z1, b): s.fill(x, y0, z0, x, y1, z1, b)
    def pared_z(s, z, y0, y1, x0, x1, b): s.fill(x0, y0, z, x1, y1, z, b)
    def anillo(s, x0, z0, x1, z1, y, b):
        s.fill(x0, y, z0, x1, y, z0, b); s.fill(x0, y, z1, x1, y, z1, b); s.fill(x0, y, z0, x0, y, z1, b); s.fill(x1, y, z0, x1, y, z1, b)
    def stairs(s, x, y, z, mat, facing, half='bottom', shape='straight'):
        """`facing` = hacia donde mira el FRENTE de la escalera (el lado bajo, por donde se sube). El estado de Minecraft guarda el lado contrario: el del respaldo alto."""
        s.set(x, y, z, f'{mat}_stairs[facing={OPUESTO_ESC[facing]},half={half},shape={shape}]')
    def slab(s, x, y, z, mat, tipo='bottom'): s.set(x, y, z, f'{mat}_slab[type={tipo}]')
    def aire(s, x0, y0, z0, x1, y1, z1): s.fill(x0, y0, z0, x1, y1, z1, 'air')
    # ---------------------------------------------------------- pixel art en una pared (plano XY mirando a -z o +z)
    def pixelart_z(s, x0, y0, z, filas, paleta):
        """filas: lista de cadenas (arriba -> abajo); paleta: caracter -> bloque; ' ' = no tocar"""
        n = len(filas)
        for j, fila in enumerate(filas):
            for i, ch in enumerate(fila):
                if ch != ' ' and ch in paleta: s.set(x0 + i, y0 + n - 1 - j, z, paleta[ch])
    # ---------------------------------------------------------- guardado
    def guardar(s, ruta, aire_relleno=True):
        palette, idx = [], {}
        def pid(b):
            if b not in idx:
                nombre, props = parse_estado(b)
                e = {'Name': nombre}
                if props: e['Properties'] = props
                idx[b] = len(palette); palette.append(e)
            return idx[b]
        bloques = []
        for x in range(s.w):
            for y in range(s.h):
                for z in range(s.l):
                    b = s.g.get((x, y, z), 'air')
                    if b == 'air' and not aire_relleno: continue
                    e = {'pos': nbtlib2.L(3, [x, y, z]), 'state': pid(b)}
                    if (x, y, z) in s.nbt: e['nbt'] = s.nbt[(x, y, z)]
                    bloques.append(e)
        for e in bloques: pass
        raiz = {'DataVersion': DATA_VERSION, 'size': L(3, [s.w, s.h, s.l]), 'palette': L(10, palette), 'blocks': L(10, bloques), 'entities': L(10, [
            {'pos': L(6, [nbtlib2.Double(v) for v in en['pos']]), 'blockPos': L(3, [int(v) for v in en['pos']]), 'nbt': en['nbt']} for en in s.entidades])}
        os.makedirs(os.path.dirname(ruta) or '.', exist_ok=True)
        open(ruta, 'wb').write(nbtlib2.dumps(raiz))
        return len(palette), len(bloques)

# ====================================================================== formas y utilidades
import math, random

def _disco(v, cx, y, cz, r, b, hueco=0):
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            d = math.hypot(x - cx, z - cz)
            if d <= r + 0.35 and d >= hueco: v.set(x, y, z, b)
V.disco = _disco

def _cilindro(v, cx, y0, y1, cz, r, b, interior=None):
    """cilindro hueco (paredes de b) o macizo; interior = bloque del hueco (por defecto aire)"""
    for y in range(y0, y1 + 1):
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                d = math.hypot(x - cx, z - cz)
                if d <= r + 0.35:
                    pared = d > r - 1.0
                    v.set(x, y, z, b if (pared or interior is None and False) else (interior if interior else 'air'))
V.cilindro = _cilindro

def _anillo_circ(v, cx, y, cz, r, b, grosor=1.0):
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.hypot(x - cx, z - cz)
            if r - grosor < d <= r + 0.35: v.set(x, y, z, b)
V.anillo_circ = _anillo_circ

def _cupula(v, cx, y0, cz, r, b, hueco=True, ry=None, vidrio=None):
    """media esfera (cupula) con base en y0; ry = radio vertical (por defecto r)"""
    ry = ry or r
    for y in range(y0, y0 + ry + 1):
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                d = math.sqrt(((x - cx) / r) ** 2 + ((z - cz) / r) ** 2 + ((y - y0) / ry) ** 2)
                if d <= 1.0 + 0.5 / r:
                    cascara = d > 1.0 - 1.2 / r
                    if cascara: v.set(x, y, z, b)
                    elif hueco: v.set(x, y, z, 'air')
V.cupula = _cupula

def _linea(v, x0, y0, z0, x1, y1, z1, b):
    n = max(abs(x1 - x0), abs(y1 - y0), abs(z1 - z0), 1)
    for i in range(n + 1):
        v.set(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), round(z0 + (z1 - z0) * i / n), b)
V.linea = _linea

def _esfera(v, cx, cy, cz, r, b, hueco=False):
    for x in range(cx - r, cx + r + 1):
        for y in range(cy - r, cy + r + 1):
            for z in range(cz - r, cz + r + 1):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2)
                if d <= r + 0.3:
                    if hueco and d < r - 1: v.set(x, y, z, 'air')
                    else: v.set(x, y, z, b)
V.esfera = _esfera

def _elipse_xz(v, cx, y, cz, rx, rz, b, hueco=0.0):
    for x in range(cx - rx - 1, cx + rx + 2):
        for z in range(cz - rz - 1, cz + rz + 2):
            d = math.sqrt(((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2)
            if hueco <= d <= 1.0 + 0.2 / min(rx, rz): v.set(x, y, z, b)
V.elipse_xz = _elipse_xz

# ---------------------------------------------------------------------- giro de la estructura (para que la fachada mire a donde toca)
_GIRO_DIR = {'north': 'west', 'west': 'south', 'south': 'east', 'east': 'north'}
def _rot_props(props, k):
    for _ in range(k % 4):
        n = {}
        for key, val in props.items():
            if key == 'facing' and val in _GIRO_DIR: n[key] = _GIRO_DIR[val]
            elif key == 'axis': n[key] = {'x': 'z', 'z': 'x'}.get(val, val)
            elif key == 'rotation': n[key] = str((int(val) + 12) % 16)      # antihorario 90 grados (4 pasos de 22,5)
            elif key in ('north', 'south', 'east', 'west'): n[_GIRO_DIR[key]] = val
            else: n[key] = val
        props = n
    return props

def girar(v, k=1):
    """gira la estructura k x 90 grados en sentido antihorario visto desde arriba (norte -> oeste); devuelve una V nueva"""
    k %= 4
    if k == 0: return v
    w, l = v.w, v.l
    out = V(l, v.h, w) if k % 2 else V(w, v.h, l)
    def mp(x, z):
        for _ in range(k):
            x, z = z, w - 1 - x
            # tras un giro las dimensiones se intercambian
        return x, z
    # giro una vez cada vez para llevar las dimensiones bien
    cur_w, cur_l = w, l
    g = dict(v.g); nb = dict(v.nbt)
    for _ in range(k):
        g2 = {}
        for (x, y, z), b in g.items():
            nombre, props = parse_estado(b)
            props = _rot_props(props, 1)
            s = nombre + ('[' + ','.join(f'{a}={c}' for a, c in props.items()) + ']' if props else '')
            g2[(z, y, cur_w - 1 - x)] = s
        nb2 = {(z, y, cur_w - 1 - x): d for (x, y, z), d in nb.items()}
        g, nb = g2, nb2
        cur_w, cur_l = cur_l, cur_w
    out.g, out.nbt = g, nb
    out.w, out.l = cur_w, cur_l
    for e in v.entidades:
        x, y, z = e['pos']; out.entidades.append({'pos': [z, y, w - 1 - x], 'nbt': e['nbt']})
    return out
