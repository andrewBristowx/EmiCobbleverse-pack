"""Convierte un modelo de Pokémon de Cobblemon (geometría Bedrock + textura) en bloques para una estatua.

Uso (desde Python): voxeles = modelo_a_voxeles('0025_pikachu', 'pikachu_male', 'pikachu', altura=26)
devuelve {(x, y, z): '#rrggbb'} con y hacia arriba y el frente del Pokémon hacia -z; luego `a_bloques` elige el bloque de color mas parecido.
Los archivos salen de un jar/carpeta de Cobblemon: variable COBBLEMON_ASSETS = carpeta que contiene assets/cobblemon (jar descomprimido).
"""
import json, math, os
from PIL import Image

ROOT = os.environ.get('COBBLEMON_ASSETS', '.')


def _rot(p, piv, rot):
    """rotacion de Bedrock (grados, orden Z·Y·X) de p alrededor de piv"""
    x, y, z = p[0] - piv[0], p[1] - piv[1], p[2] - piv[2]
    rx, ry, rz = (math.radians(a) for a in rot)
    # X
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    # Y
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    # Z
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return (x + piv[0], y + piv[1], z + piv[2])


OCULTOS = ('hidden', 'hide', 'cap', 'hat', 'item', 'locator')


def _molang(v, ctx=None):
    """valor de una expresion de Molang en el instante 0 (sin variables): numeros, 'a+math.sin(q.anim_time*90*4-60)*b'..."""
    if isinstance(v, (int, float)): return float(v)
    if isinstance(v, str):
        import re
        e = v.replace('math.', 'M_').replace('Math.', 'M_')
        e = re.sub(r'\b(q|query|v|variable)\.[a-z_0-9]+', '0', e)
        env = {'M_sin': lambda d: math.sin(math.radians(d)), 'M_cos': lambda d: math.cos(math.radians(d)), 'M_abs': abs, 'M_min': min, 'M_max': max,
               'M_pi': math.pi, 'M_clamp': lambda a, lo, hi: max(lo, min(hi, a)), 'M_floor': math.floor, 'M_sqrt': math.sqrt,
               'M_pow': pow, 'M_lerp': lambda a, b, t: a + (b - a) * t, 'M_random': lambda *a: 0.0}
        try: return float(eval(e, {'__builtins__': {}}, env))
        except Exception: return 0.0
    return 0.0


def _vec(v):
    """[x,y,z] de una animacion: lista, o claves de tiempo (se toma la primera), con expresiones"""
    if v is None: return None
    if isinstance(v, dict):
        if 'vector' in v: v = v['vector']
        else:
            k = sorted(v, key=lambda t: float(t) if str(t).replace('.', '', 1).replace('-', '', 1).isdigit() else 1e9)[0]
            v = v[k]
            if isinstance(v, dict): v = v.get('post', v.get('pre', [0, 0, 0]))
    if isinstance(v, (int, float, str)): v = [v, v, v]
    return [_molang(a) for a in v]


def cargar_pose(anim_path, nombres=('ground_idle', 'idle', 'battle_idle')):
    """{hueso: (rotacion, posicion)} de la animacion de reposo en el instante 0"""
    if not anim_path or not os.path.exists(anim_path): return {}
    d = json.load(open(anim_path))['animations']
    for nom in nombres:
        for k in d:
            if k.endswith('.' + nom) or k.endswith(nom):
                if 'hat' in k or 'sleep' in k: continue
                out = {}
                for b, x in d[k].get('bones', {}).items():
                    r, p = _vec(x.get('rotation')), _vec(x.get('position'))
                    out[b] = (r or [0, 0, 0], p or [0, 0, 0])
                return out
    return {}


def _rot_inv(p, piv, rot):
    """inversa de _rot"""
    x, y, z = p[0] - piv[0], p[1] - piv[1], p[2] - piv[2]
    rx, ry, rz = (math.radians(a) for a in rot)
    x, y = x * math.cos(-rz) - y * math.sin(-rz), x * math.sin(-rz) + y * math.cos(-rz)
    x, z = x * math.cos(-ry) + z * math.sin(-ry), -x * math.sin(-ry) + z * math.cos(-ry)
    y, z = y * math.cos(-rx) - z * math.sin(-rx), y * math.sin(-rx) + z * math.cos(-rx)
    return (x + piv[0], y + piv[1], z + piv[2])


def _cargar_geo(ruta):
    d = json.load(open(ruta))
    g = d['minecraft:geometry'][0]
    bones = {b['name']: b for b in g['bones']}
    return g, bones


def _cadena(bones, nombre):
    out = []
    while nombre:
        b = bones[nombre]
        out.append(b)
        nombre = b.get('parent')
    return out        # del hueso hacia la raiz


def voxeles_modelo(geo_path, tex_path, altura, signo=(1, 1, -1), paso_mult=2.0, pose=None, ocultos=OCULTOS, esc=None):
    """`altura`: alto de la estatua en bloques; si se da `esc` (bloques por unidad del modelo) manda este y el alto sale de el (asi varias estatuas del mismo modelo guardan la misma escala).
    Acepta cubos con UV de caja (Cobblemon) y con UV por cara (modelos de bloque de Blockbench, p. ej. los peluches de emi-plush)."""
    g, bones = _cargar_geo(geo_path)
    tex = Image.open(tex_path).convert('RGBA')
    tw, th = g['description']['texture_width'], g['description']['texture_height']
    sx_t, sy_t = tex.width / tw, tex.height / th
    caras = []   # (puntos 3D del modelo, color) sin voxelizar
    pts = []
    pose = pose or {}
    cubos_info = []      # (origen, tamaño, cadena, cubo) de cada cubo visible
    for b in g['bones']:
        if any(h in b['name'] for h in ocultos): continue
        for c in b.get('cubes', []):
            o, sz = c['origin'], c['size']
            inf = c.get('inflate', b.get('inflate', 0)) or 0
            mirror = c.get('mirror', b.get('mirror', False))
            uvc = c['uv']
            u0, v0 = (0, 0) if isinstance(uvc, dict) else uvc
            X0, Y0, Z0 = o[0] - inf, o[1] - inf, o[2] - inf
            SX, SY, SZ = sz[0] + 2 * inf, sz[1] + 2 * inf, sz[2] + 2 * inf
            cadena = _cadena(bones, b['name'])
            cubos_info.append((c, cadena))
            if isinstance(uvc, dict):                    # UV por cara: {'north': {'uv': [u, v], 'uv_size': [ancho, alto]}, ...} (el tamano puede ser negativo: cara espejada)
                cuadros = {}
                for cara, k in (('n', 'north'), ('e', 'east'), ('s', 'south'), ('w', 'west'), ('u', 'up'), ('d', 'down')):
                    f = uvc.get(k)
                    cuadros[cara] = (f['uv'][0], f['uv'][1], f['uv_size'][0], f['uv_size'][1]) if f else None
            else:
              cuadros = {'n': (u0 + sz[2], v0 + sz[2], sz[0], sz[1]), 'e': (u0, v0 + sz[2], sz[2], sz[1]),
                         's': (u0 + sz[2] + sz[0] + sz[2], v0 + sz[2], sz[0], sz[1]), 'w': (u0 + sz[2] + sz[0], v0 + sz[2], sz[2], sz[1]),
                         'u': (u0 + sz[2], v0, sz[0], sz[2]), 'd': (u0 + sz[2] + sz[0], v0, sz[0], sz[2])}
            if mirror and not isinstance(uvc, dict): cuadros['e'], cuadros['w'] = cuadros['w'], cuadros['e']
            paso = 1.0 / paso_mult
            def muestrear(cara, ai, aj, ak):
                pass
            # recorremos la superficie del cubo en pasos de media unidad
            nx, ny, nz = max(1, int(SX * paso_mult)), max(1, int(SY * paso_mult)), max(1, int(SZ * paso_mult))
            def punto(fx, fy, fz): return (X0 + SX * fx, Y0 + SY * fy, Z0 + SZ * fz)
            def color(cara, a, bb):
                if cuadros[cara] is None: return (0, 0, 0, 0)
                u, v, w, h = cuadros[cara]
                # a, bb en [0,1): a recorre el ancho de la cara, bb el alto
                if mirror and not isinstance(uvc, dict): a = 1 - a
                px = (u + a * w) * sx_t; py = (v + bb * h) * sy_t
                px = min(tex.width - 1, max(0, int(px))); py = min(tex.height - 1, max(0, int(py)))
                return tex.getpixel((px, py))
            for i in range(nx):
                for j in range(ny):
                    fx, fy = (i + 0.5) / nx, (j + 0.5) / ny
                    # cara norte (z minima): u crece hacia -x visto desde fuera => a = 1 - fx
                    pts.append((punto(fx, 1 - fy, 0), color('n', 1 - fx, fy), cadena, c))
                    pts.append((punto(fx, 1 - fy, 1), color('s', fx, fy), cadena, c))
            for k in range(nz):
                for j in range(ny):
                    fz, fy = (k + 0.5) / nz, (j + 0.5) / ny
                    pts.append((punto(1, 1 - fy, fz), color('e', 1 - fz, fy), cadena, c))     # +x (este)
                    pts.append((punto(0, 1 - fy, fz), color('w', fz, fy), cadena, c))         # -x (oeste)
            for i in range(nx):
                for k in range(nz):
                    fx, fz = (i + 0.5) / nx, (k + 0.5) / nz
                    pts.append((punto(fx, 1, fz), color('u', fx, fz), cadena, c))
                    pts.append((punto(fx, 0, 1 - fz), color('d', fx, fz), cadena, c))
    # aplicar rotaciones (cubo y huesos) y voxelizar
    todos = []
    todos_cubo = []
    for p, col, cadena, c in pts:
        if col[3] < 40: continue
        if c.get('rotation'):
            p = _rot(p, c.get('pivot', [0, 0, 0]), [a * s for a, s in zip(c['rotation'], signo)])
        for b in cadena:
            r0 = b.get('rotation') or [0, 0, 0]
            r1, d1 = pose.get(b['name'], ([0, 0, 0], [0, 0, 0]))
            rr = [a + c2 for a, c2 in zip(r0, r1)]
            if any(rr): p = _rot(p, b.get('pivot', [0, 0, 0]), [a * s for a, s in zip(rr, signo)])
            if any(d1): p = (p[0] + d1[0], p[1] + d1[1], p[2] + d1[2])
        todos.append((p, col, (min(c['size']) <= 0.5)))
        todos_cubo.append(c)
    def a_local(c, cadena, p):
        for b in reversed(cadena):
            r0 = b.get('rotation') or [0, 0, 0]
            r1, d1 = pose.get(b['name'], ([0, 0, 0], [0, 0, 0]))
            if any(d1): p = (p[0] - d1[0], p[1] - d1[1], p[2] - d1[2])
            rr = [a + c2 for a, c2 in zip(r0, r1)]
            if any(rr): p = _rot_inv(p, b.get('pivot', [0, 0, 0]), [a * s_ for a, s_ in zip(rr, signo)])
        if c.get('rotation'): p = _rot_inv(p, c.get('pivot', [0, 0, 0]), [a * s_ for a, s_ in zip(c['rotation'], signo)])
        return p
    solidos = [(c, ch) for c, ch in cubos_info if min(c['size']) > 0.5]
    def enterrado(p, propio):
        for c, ch in solidos:
            if c is propio: continue
            q = a_local(c, ch, p)
            o, sz = c['origin'], c['size']
            e = 0.03 - (c.get('inflate') or 0)
            if o[0] + e < q[0] < o[0] + sz[0] - e and o[1] + e < q[1] < o[1] + sz[1] - e and o[2] + e < q[2] < o[2] + sz[2] - e: return True
        return False
    todos = [(p, col, d) for (p, col, d), cu in zip(todos, todos_cubo) if not (d and enterrado(p, cu))]
    ymin = min(p[1] for p, _, _ in todos); ymax = max(p[1] for p, _, _ in todos)
    if esc is None: esc = altura / max(1e-6, ymax - ymin)
    vox = {}
    acum = {}
    for p, col, decal in todos:
        k = (int(math.floor(p[0] * esc)), int(math.floor((p[1] - ymin) * esc)), int(math.floor(p[2] * esc)))
        acum.setdefault(k, []).append((col, decal))
    for k, lista in acum.items():
        deca = [c for c, d in lista if d]
        cols = deca if deca else [c for c, d in lista]          # los "calcomanias" (cubos sin grosor: ojos, boca, manchas) mandan sobre la superficie
        cubos = {}
        for c in cols: cubos.setdefault((c[0] // 40, c[1] // 40, c[2] // 40), []).append(c)
        mejor = max(cubos.values(), key=len)                      # color mas frecuente (no la media: asi no se emborronan ojos y manchas)
        vox[k] = tuple(sum(c[i] for c in mejor) // len(mejor) for i in range(3))
    return vox


def vistas(vox, ruta, escala=10):
    """tres vistas ortogonales: frente (desde -z), lado (desde +x) y arriba"""
    xs = [k[0] for k in vox]; ys = [k[1] for k in vox]; zs = [k[2] for k in vox]
    x0, x1, y0, y1, z0, z1 = min(xs), max(xs), min(ys), max(ys), min(zs), max(zs)
    W = (x1 - x0 + 1) + (z1 - z0 + 1) * 2 + 6
    H = (y1 - y0 + 1) + 4
    im = Image.new('RGB', (W * escala, H * escala), (200, 220, 255))
    from PIL import ImageDraw
    d = ImageDraw.Draw(im)
    def pinta(ox, plano):
        for (a, b), col in plano.items(): d.rectangle([(ox + a) * escala, (y1 - b + 2) * escala, (ox + a + 1) * escala - 1, (y1 - b + 3) * escala - 1], fill=col)
    frente, lado = {}, {}
    for k in sorted(vox, key=lambda k: -k[2]): frente[(k[0] - x0, k[1])] = vox[k]                  # lo mas cerca (z pequeño) gana
    for k in sorted(vox, key=lambda k: k[0]): lado[(k[2] - z0, k[1])] = vox[k]                    # visto desde +x
    atras = {}
    for k in sorted(vox, key=lambda k: k[2]): atras[(x1 - k[0], k[1])] = vox[k]                   # visto desde +z
    pinta(2, frente); pinta(2 + (x1 - x0 + 1) + 2, lado); pinta(2 + (x1 - x0 + 1) + 2 + (z1 - z0 + 1) + 2, atras)
    im.save(ruta)


# ---------------------------------------------------------------- de color a bloque
_PAL = None


def _paleta(tex_dir):
    """colores medios de los bloques de color de Minecraft (concreto, lana, terracota) a partir de sus texturas"""
    global _PAL
    if _PAL is not None: return _PAL
    colores = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black']
    nombres = [c + '_concrete' for c in colores] + [c + '_wool' for c in colores] + [c + '_terracotta' for c in colores] + ['terracotta', 'smooth_sandstone', 'sandstone', 'snow_block', 'gold_block', 'bone_block_side', 'amethyst_block']
    pal = {}
    for n in nombres:
        f = os.path.join(tex_dir, n + '.png')
        if not os.path.exists(f): continue
        im = Image.open(f).convert('RGB').resize((1, 1), Image.BOX)
        pal[n.replace('_side', '')] = im.getpixel((0, 0))
    _PAL = pal
    return pal


def a_bloques(vox, tex_dir, sesgo=None):
    """{(x,y,z): bloque}: el bloque de color mas parecido a cada voxel (distancia de color ponderada).
    `sesgo`: {bloque: factor} multiplica la distancia de ese bloque (factor < 1 lo favorece; p. ej. amethyst_block para los lilas, que no tienen bloque propio)"""
    sesgo = sesgo or {}
    pal = _paleta(tex_dir)
    cache = {}
    out = {}
    for k, c in vox.items():
        q = (c[0] // 6, c[1] // 6, c[2] // 6)
        if q not in cache:
            mejor, md = None, 1e18
            for n, p in pal.items():
                rm = (c[0] + p[0]) / 2
                d = (2 + rm / 256) * (c[0] - p[0]) ** 2 + 4 * (c[1] - p[1]) ** 2 + (2 + (255 - rm) / 256) * (c[2] - p[2]) ** 2
                d *= sesgo.get(n, 1.0)
                if d < md: md, mejor = d, n
            cache[q] = mejor
        out[k] = cache[q]
    return out


def buscar(assets, especie):
    """(geo, textura) de una especie: busca <assets>/bedrock/pokemon/models/NNNN_<especie>/ y su textura"""
    import glob
    ms = glob.glob(os.path.join(assets, 'bedrock/pokemon/models/*_%s' % especie))
    if not ms: raise FileNotFoundError(especie)
    d = ms[0]; nn = os.path.basename(d)
    geos = sorted(glob.glob(os.path.join(d, '*.geo.json')))
    pref = [g for g in geos if os.path.basename(g) in (especie + '.geo.json', especie + '_male.geo.json')]
    geo = (pref or geos)[0]
    td = os.path.join(assets, 'textures/pokemon', nn)
    for cand in (especie + '.png', especie + '_male.png', especie + '_female.png'):
        if os.path.exists(os.path.join(td, cand)): return geo, os.path.join(td, cand)
    pngs = [p for p in sorted(glob.glob(os.path.join(td, '*.png'))) if not any(w in p for w in ('shiny', 'emissive', 'layer'))]
    return geo, pngs[0]


def anim_de(assets, especie):
    import glob
    r = glob.glob(os.path.join(assets, 'bedrock/pokemon/animations/*_%s/%s.animation.json' % (especie, especie)))
    return r[0] if r else None


def estatua(assets, tex_dir, especie, altura, signo=(1, 1, -1)):
    geo, tex = buscar(assets, especie)
    return a_bloques(voxeles_modelo(geo, tex, altura, signo=signo, pose=cargar_pose(anim_de(assets, especie))), tex_dir)


def girar(bl, veces):
    """gira la estatua `veces` cuartos de vuelta alrededor de y (el frente pasa de -z a +x, +z, -x...)"""
    out = {}
    for (x, y, z), b in bl.items():
        for _ in range(veces % 4): x, z = -z, x
        out[(x, y, z)] = b
    return out
