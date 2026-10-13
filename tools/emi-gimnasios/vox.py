"""Vista isometrica de una estructura (NBT de Minecraft) con el color medio de las texturas de vanilla."""
import os, math, hashlib, re
from functools import lru_cache
from PIL import Image, ImageDraw
import nbtlib2

TEX = os.environ.get('MC_TEXTURAS', os.path.join(os.path.dirname(os.path.abspath(__file__)), 'tex/assets/minecraft/textures/block'))

@lru_cache(None)
def media(nombre):
    p = f'{TEX}/{nombre}.png'
    if not os.path.exists(p): return None
    im = Image.open(p).convert('RGBA')
    if im.height > im.width: im = im.crop((0, 0, im.width, im.width))
    px = [q for q in im.getdata() if q[3] > 40]
    if not px: return None
    return tuple(sum(q[i] for q in px) // len(px) for i in range(3))

SUF = ['_stairs', '_slab', '_wall', '_fence_gate', '_fence', '_pane', '_trapdoor', '_door', '_button', '_pressure_plate', '_sign', '_wall_sign']
def base(n):
    n = n.split(':')[-1]
    if n.startswith('waxed_'): n = n[6:]
    if n.startswith('copper_bulb'): return 'copper_bulb_lit'
    for s in SUF:
        if n.endswith(s): n = n[:-len(s)]; break
    if n.endswith('_stained_glass'): return n
    return {'stone_brick': 'stone_bricks', 'brick': 'bricks', 'nether_brick': 'nether_bricks', 'cut_copper': 'cut_copper', 'smooth_quartz': 'quartz_block_bottom',
            'quartz': 'quartz_block_side', 'purpur': 'purpur_block', 'sandstone': 'sandstone', 'smooth_sandstone': 'sandstone_top', 'red_sandstone': 'red_sandstone',
            'prismarine_brick': 'prismarine_bricks', 'end_stone_brick': 'end_stone_bricks', 'mud_brick': 'mud_bricks', 'tuff_brick': 'tuff_bricks', 'polished_blackstone_brick': 'polished_blackstone_bricks',
            'deepslate_brick': 'deepslate_bricks', 'deepslate_tile': 'deepslate_tiles', 'bamboo': 'bamboo_planks', 'bamboo_mosaic': 'bamboo_mosaic', 'cobblestone': 'cobblestone', 'mossy_cobblestone': 'mossy_cobblestone'}.get(n, n)

@lru_cache(None)
def color(nombre, cara):
    b = base(nombre)
    if b in ('air', 'cave_air', 'void_air', 'structure_void', 'light', 'barrier'): return None
    cand = []
    if cara == 'top': cand = [f'{b}_top', b, f'{b}_side', f'{b}_planks', f'{b}_block', f'{b}_block_top']
    else: cand = [f'{b}_side', b, f'{b}_planks', f'{b}_block', f'{b}_top', f'{b}_block_side']
    cand += [b.replace('stripped_', '') + '_top', b + '_still', b + '_0']
    for c in cand:
        m = media(c)
        if m: return m
    if 'water' in b: return (50, 90, 220)
    if 'lava' in b: return (230, 100, 20)
    h = hashlib.md5(b.encode()).digest(); return (110 + h[0] % 90, 110 + h[1] % 90, 110 + h[2] % 90)

def cargar(ruta):
    r = nbtlib2.parse(open(ruta, 'rb').read()) if isinstance(ruta, str) else ruta
    w, h, l = r['size']
    pal = [p['Name'] + ('[' + ','.join(f'{k}={v}' for k, v in sorted(p.get('Properties', {}).items())) + ']' if p.get('Properties') else '') for p in r['palette']]
    g = {}
    for b in r['blocks']:
        x, y, z = b['pos']; g[(x, y, z)] = pal[b['state']]
    return (w, h, l), g

SOLIDO_CORTO = ('_slab',)
def rotar(w, l, g, ang):
    for _ in range(ang % 4):
        g = {(l - 1 - z, y, x): n for (x, y, z), n in g.items()}   # giro de 90 grados
        w, l = l, w
    return w, l, g

def render(ruta, esc=8, salida=None, ang=0, recorte_y=None, fondo=(24, 18, 40)):
    (w, h, l), g = cargar(ruta) if not isinstance(ruta, tuple) else ruta
    w, l, g = rotar(w, l, g, ang)
    def gi(x, y, z): return g.get((x, y, z))
    def vacio(n): return n is None or color(n.split('[')[0], 'top') is None
    def opaco(n): return not vacio(n)
    # rotacion de la vista: ang 0..3 (90 grados cada una)
    def rot(x, z):
        for _ in range(ang): x, z = z, w - 1 - x if False else (l - 1 - z if False else z), x
        return x, z
    cw = esc * 2
    Wp = int((w + l) * esc) + 40; Hp = int((w + l) * esc / 2 + h * esc) + 40
    im = Image.new('RGB', (Wp, Hp), fondo); d = ImageDraw.Draw(im)
    ox = l * esc + 20; oy = h * esc + 20
    def proj(x, y, z):
        return (ox + (x - z) * esc, oy + (x + z) * esc / 2 - y * esc)
    ordenados = sorted(g.items(), key=lambda kv: (kv[0][1], kv[0][0] + kv[0][2]))
    # painter: de atras a delante: y creciente no sirve; se ordena por x+z (lejos->cerca) y luego y
    ordenados = sorted(g.items(), key=lambda kv: (kv[0][0] + kv[0][2], kv[0][1]))
    for (x, y, z), n in ordenados:
        nb = n.split('[')[0]
        c0 = color(nb, 'top')
        if c0 is None: continue
        alto = 0.5 if (nb.endswith('_slab') and 'type=top' not in n) else 1.0
        up = gi(x, y + 1, z); xp = gi(x + 1, y, z); zp = gi(x, y, z + 1)
        cs = color(nb, 'side') or c0
        x0, y0, z0 = x, y, z
        yt = y + alto
        glass = 'glass' in nb
        if vacio(up) or alto < 1:
            p = [proj(x, yt, z), proj(x + 1, yt, z), proj(x + 1, yt, z + 1), proj(x, yt, z + 1)]
            d.polygon(p, fill=tuple(min(255, int(c * 1.0)) for c in c0))
        if vacio(xp):
            p = [proj(x + 1, yt, z), proj(x + 1, yt, z + 1), proj(x + 1, y, z + 1), proj(x + 1, y, z)]
            d.polygon(p, fill=tuple(int(c * 0.78) for c in cs))
        if vacio(zp):
            p = [proj(x, yt, z + 1), proj(x + 1, yt, z + 1), proj(x + 1, y, z + 1), proj(x, y, z + 1)]
            d.polygon(p, fill=tuple(int(c * 0.58) for c in cs))
    if salida: im.save(salida)
    return im

if __name__ == '__main__':
    import sys
    render(sys.argv[1], int(sys.argv[3]) if len(sys.argv) > 3 else 8, sys.argv[2])
