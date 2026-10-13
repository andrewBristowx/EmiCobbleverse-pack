"""Trazado de la pista «Volcán Cinnabar»: bucle principal (más cerrado y con más desnivel) + atajo por el interior con un salto largo."""
import math, camino

# bucle principal, sentido horario visto desde arriba (x a la derecha, z hacia abajo)
PUNTOS = [
    (60, 40), (112, 40), (172, 40),                # recta de salida (este)
    (216, 50), (241, 90),                          # curva a la derecha
    (247, 138), (247, 186),                        # lado derecho (el atajo sale por aqui)
    (238, 232), (219, 282), (188, 314), (152, 298),  # brazo largo hacia el sur, con horquilla en la punta
    (150, 256), (146, 214),                        # vuelta del brazo (aqui vuelve a entrar el atajo)
    (112, 190), (78, 200),                         # hacia el oeste
    (44, 234), (18, 200),                          # esquina inferior izquierda
    (24, 152), (22, 108),                          # subida por la izquierda (recta del segundo salto)
    (34, 68),
]
# altura del asfalto (bloques sobre la base) segun la posicion en el bucle (0..1)
ALTURAS = [(0.00, 0), (0.17, 0), (0.24, 4), (0.32, 9), (0.40, 9), (0.47, 4), (0.55, 1), (0.62, 1), (0.70, 6), (0.78, 10), (0.84, 10), (0.90, 3), (0.94, 0), (1.0, 0)]

# atajo por el interior: sale del lado derecho y vuelve a entrar en la vuelta del brazo
ATAJO_ENTRADA = (247, 160)
ATAJO_SALIDA = (112, 190)
ATAJO_MEDIOS = [(214, 166), (182, 174), (150, 182), (128, 187)]

def altura_u(u):
    for (u0, h0), (u1, h1) in zip(ALTURAS, ALTURAS[1:]):
        if u0 <= u <= u1:
            f = (u - u0) / (u1 - u0) if u1 > u0 else 0
            f = f * f * (3 - 2 * f)
            return h0 + (h1 - h0) * f
    return 0

def principal(paso=0.5):
    ms, total = camino.muestras(paso, PUNTOS)
    for m in ms: m['h'] = altura_u(m['s'] / total)
    return ms, total

def abierto(puntos, paso=0.5):
    """curva abierta (Catmull-Rom) por los puntos, sin los tramos de relleno de los extremos; devuelve muestras con tangente"""
    pts = [puntos[0]] + list(puntos) + [puntos[-1]]
    crudo = []
    for i in range(1, len(pts) - 2):
        for k in range(60): crudo.append(camino._cr(pts[i - 1], pts[i], pts[i + 1], pts[i + 2], k / 60))
    crudo.append(puntos[-1])
    acum = [0.0]
    for i in range(1, len(crudo)): acum.append(acum[-1] + math.dist(crudo[i - 1], crudo[i]))
    total = acum[-1]; m = int(total / paso)
    out, j = [], 0
    for q in range(m + 1):
        s = min(q * total / m, total - 1e-6)
        while acum[j + 1] < s: j += 1
        f = (s - acum[j]) / (acum[j + 1] - acum[j])
        a, b = crudo[j], crudo[j + 1]
        out.append({'x': a[0] + (b[0] - a[0]) * f, 'z': a[1] + (b[1] - a[1]) * f, 's': s})
    for i, o in enumerate(out):
        a, b = out[max(0, i - 1)], out[min(len(out) - 1, i + 1)]
        dx, dz = b['x'] - a['x'], b['z'] - a['z']; d = math.hypot(dx, dz)
        o['tx'], o['tz'] = dx / d, dz / d
    return out, total

if __name__ == '__main__':
    ms, tot = principal()
    print('principal: longitud', round(tot), 'radio minimo', round(camino.curvatura_min(ms), 1))
    # distancia minima entre tramos lejanos
    mind, par = 1e9, None
    for i in range(0, len(ms), 4):
        for j in range(i + 1, len(ms), 4):
            gap = min(j - i, len(ms) - (j - i)) * 0.5
            if gap < 60: continue
            d = math.hypot(ms[i]['x'] - ms[j]['x'], ms[i]['z'] - ms[j]['z'])
            if d < mind: mind, par = d, (i, j)
    print('minima distancia entre tramos lejanos', round(mind, 1), par)
    at, lat = abierto([ATAJO_ENTRADA] + ATAJO_MEDIOS + [ATAJO_SALIDA])
    print('atajo: longitud', round(lat))
    # trozo del bucle que se salta el atajo
    ie = min(range(len(ms)), key=lambda q: math.dist((ms[q]['x'], ms[q]['z']), ATAJO_ENTRADA))
    isal = min(range(len(ms)), key=lambda q: math.dist((ms[q]['x'], ms[q]['z']), ATAJO_SALIDA))
    print('el atajo se salta', round((ms[isal]['s'] - ms[ie]['s']) % tot), 'bloques del bucle; entrada s=%d salida s=%d' % (ms[ie]['s'], ms[isal]['s']))
    from PIL import Image, ImageDraw
    im = Image.new('RGB', (560, 680), 'white'); dr = ImageDraw.Draw(im)
    for m in ms: dr.ellipse([m['x'] * 2 - 1, m['z'] * 2 - 1, m['x'] * 2 + 1, m['z'] * 2 + 1], fill=(int(30 + m['h'] * 18), 40, 160))
    for m in at: dr.ellipse([m['x'] * 2 - 1, m['z'] * 2 - 1, m['x'] * 2 + 1, m['z'] * 2 + 1], fill=(220, 60, 20))
    im.save('/tmp/claude-0/-home-user-EmiCobbleverse-pack/095a6f01-c837-5018-9b85-509fd1a3313e/scratchpad/volcan/camino.png')
