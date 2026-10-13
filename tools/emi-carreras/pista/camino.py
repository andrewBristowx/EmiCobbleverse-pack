"""Trazado de la pista: curva cerrada (Catmull-Rom) con altura por arco. Devuelve muestras con posicion, tangente y altura."""
import math

# puntos de paso (x, z) en bloques, sentido horario visto desde arriba (x a la derecha, z hacia abajo)
PUNTOS = [
    (30, 40), (80, 40), (130, 40),            # recta de salida (hacia el este)
    (172, 46), (203, 78),                     # curva ancha a la derecha
    (208, 122), (190, 156),                   # lado derecho, bajando
    (160, 176), (140, 202), (108, 198),       # chicane inferior
    (80, 218), (44, 198),                     # curva inferior izquierda
    (30, 160), (52, 126), (32, 94), (24, 62), # lado izquierdo en zig-zag hacia la salida
]
# altura relativa del asfalto segun la posicion en el recorrido (0..1): colina suave y bajada
ALTURAS = [(0.00, 0), (0.10, 0), (0.22, 0), (0.30, 4), (0.40, 4), (0.48, 0), (0.60, 0), (0.70, 6), (0.80, 6), (0.88, 0), (1.0, 0)]

def _cr(p0, p1, p2, p3, t, alpha=0.5):
    def tj(ti, a, b): return ti + max(1e-6, math.dist(a, b)) ** alpha
    t0 = 0; t1 = tj(t0, p0, p1); t2 = tj(t1, p1, p2); t3 = tj(t2, p2, p3)
    tt = t1 + (t2 - t1) * t
    def lerp(a, b, ta, tb): return tuple(((tb - tt) * a[i] + (tt - ta) * b[i]) / (tb - ta) for i in range(2))
    a1 = lerp(p0, p1, t0, t1); a2 = lerp(p1, p2, t1, t2); a3 = lerp(p2, p3, t2, t3)
    b1 = lerp(a1, a2, t0, t2); b2 = lerp(a2, a3, t1, t3)
    return lerp(b1, b2, t1, t2)

def muestras(paso=0.5, puntos=PUNTOS):
    """lista de dicts {x,z,tx,tz,s,h} equiespaciados cada `paso` bloques de recorrido"""
    n = len(puntos)
    crudo = []
    for i in range(n):
        p0, p1, p2, p3 = puntos[(i - 1) % n], puntos[i], puntos[(i + 1) % n], puntos[(i + 2) % n]
        for k in range(60): crudo.append(_cr(p0, p1, p2, p3, k / 60))
    # re-muestreo uniforme
    acum = [0.0]
    for i in range(1, len(crudo) + 1): acum.append(acum[-1] + math.dist(crudo[i - 1], crudo[i % len(crudo)]))
    total = acum[-1]
    m = int(total / paso)
    out, j = [], 0
    for q in range(m):
        s = q * total / m
        while acum[j + 1] < s: j += 1
        f = (s - acum[j]) / (acum[j + 1] - acum[j])
        a, b = crudo[j], crudo[(j + 1) % len(crudo)]
        out.append({'x': a[0] + (b[0] - a[0]) * f, 'z': a[1] + (b[1] - a[1]) * f, 's': s})
    for i, o in enumerate(out):
        a, b = out[i - 1], out[(i + 1) % len(out)]
        dx, dz = b['x'] - a['x'], b['z'] - a['z']; d = math.hypot(dx, dz)
        o['tx'], o['tz'] = dx / d, dz / d
        u = o['s'] / total
        o['h'] = _altura(u)
    return out, total

def _altura(u):
    for (u0, h0), (u1, h1) in zip(ALTURAS, ALTURAS[1:]):
        if u0 <= u <= u1:
            f = (u - u0) / (u1 - u0) if u1 > u0 else 0
            f = f * f * (3 - 2 * f)       # suavizado
            return h0 + (h1 - h0) * f
    return 0

def curvatura_min(ms):
    """radio de curvatura minimo (bloques)"""
    mn = 1e9
    for i in range(len(ms)):
        a, b = ms[i - 2], ms[(i + 2) % len(ms)]
        c = ms[i]
        ang = abs(math.atan2(a['tx'] * b['tz'] - a['tz'] * b['tx'], a['tx'] * b['tx'] + a['tz'] * b['tz']))
        ds = 2 * 0.5 * 2
        if ang > 1e-4: mn = min(mn, ds / ang)
    return mn

if __name__ == '__main__':
    ms, tot = muestras()
    print('longitud', round(tot), 'muestras', len(ms), 'radio minimo', round(curvatura_min(ms), 1))
    xs = [m['x'] for m in ms]; zs = [m['z'] for m in ms]
    print('x', min(xs), max(xs), 'z', min(zs), max(zs))
    # distancia minima entre tramos no contiguos
    mind = 1e9; par = None
    for i in range(0, len(ms), 4):
        for j in range(i + 1, len(ms), 4):
            gap = min(j - i, len(ms) - (j - i)) * 0.5
            if gap < 60: continue
            d = math.hypot(ms[i]['x'] - ms[j]['x'], ms[i]['z'] - ms[j]['z'])
            if d < mind: mind, par = d, (i, j)
    print('distancia minima entre tramos lejanos', round(mind, 1), par)
    try:
        from PIL import Image, ImageDraw
        im = Image.new('RGB', (600, 500), 'white'); dr = ImageDraw.Draw(im)
        for m in ms: dr.ellipse([m['x'] * 2 - 1, m['z'] * 2 - 1, m['x'] * 2 + 1, m['z'] * 2 + 1], fill=(int(40 + m['h'] * 25), 40, 160))
        for p in PUNTOS: dr.ellipse([p[0] * 2 - 3, p[1] * 2 - 3, p[0] * 2 + 3, p[1] * 2 + 3], outline='red')
        im.save('camino.png')
    except Exception as e: print('sin imagen', e)
