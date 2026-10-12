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
    return nombre, props

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
        s.set(x, y, z, f'{mat}_stairs[facing={facing},half={half},shape={shape}]')
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
