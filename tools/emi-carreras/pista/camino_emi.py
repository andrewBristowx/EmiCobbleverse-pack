"""Trazado de la pista «Estación Michialien»: un circuito con forma de LUNA CRECIENTE (la luna de la barriga del Michialien).

Arco exterior (grande, con dos chicanes) -> horquilla en la punta norte -> arco interior (curva al reves, con una chicane) -> horquilla en la punta sur.
Se recorre en sentido horario visto desde arriba (x a la derecha, z hacia abajo); la salida esta en el arco exterior, en el lado oeste.
Las horquillas de las puntas se pueden saltar con un salto con turbo (atajo).
"""
import math, camino

C0, R0 = (150.0, 150.0), 130.0          # circulo exterior de la luna
C1, R1 = (200.0, 150.0), 100.0           # circulo que se "muerde" por dentro


def _pol(c, r, a):
    return (c[0] + r * math.cos(math.radians(a)), c[1] + r * math.sin(math.radians(a)))


K = 1.2                       # escala global del circuito (la luna se dibuja con numeros pequenos y se agranda al final)
ANCHO_PUNTA = 38.0          # distancia entre los dos arcos donde empieza la horquilla (la horquilla es una semicircunferencia de este diametro)


def _ray(c, r, e, n):
    """distancia t>0 mas corta a la que el rayo e + t*n corta al circulo (c, r); None si no lo corta"""
    fx, fz = e[0] - c[0], e[1] - c[1]
    b = fx * n[0] + fz * n[1]
    cc = fx * fx + fz * fz - r * r
    disc = b * b - cc
    if disc < 0: return None
    ts = [-b - math.sqrt(disc), -b + math.sqrt(disc)]
    ts = [t for t in ts if t > 1e-6]
    return min(ts) if ts else None


def _extremo():
    """angulo del arco exterior (extremo norte) donde la luna mide ANCHO_PUNTA de grosor"""
    a = 180.0
    while a < 300.0:
        e = _pol(C0, R0 - 6, a)
        ta = (-math.sin(math.radians(a)), math.cos(math.radians(a)))
        n = (-ta[1], ta[0])                    # lado derecho (interior del bucle en sentido horario)
        t = _ray(C1, R1 + 2, e, n)
        if t is not None and t < ANCHO_PUNTA: return a - 1.0, e
        a += 0.5
    raise RuntimeError('no hay punta')


def puntos():
    a_fin, E = _extremo()
    a_ini = 360.0 - a_fin
    ta = (-math.sin(math.radians(a_fin)), math.cos(math.radians(a_fin)))
    E = _pol(C0, R0 - 6, a_fin)
    n = (-ta[1], ta[0])
    w = _ray(C1, R1 + 2, E, n)
    I = (E[0] + n[0] * w, E[1] + n[1] * w)
    pts = []
    # arco exterior: del extremo sur (a_ini) por el oeste (180) hasta el extremo norte (a_fin), angulos crecientes = horario
    k = max(8, int((a_fin - a_ini) / 14))
    for q in range(k + 1):
        a = a_ini + (a_fin - a_ini) * q / k
        t = q / k
        ond = 11.0 * math.sin(t * 2 * math.pi * 1.5) * math.sin(math.pi * t) ** 0.7     # chicanes suaves que se apagan en los extremos
        pts.append(_pol(C0, R0 - 6 + ond, a))
    # horquilla norte: semicircunferencia de diametro E-I que sobresale hacia delante
    rad = w / 2.0
    cx, cz = E[0] + n[0] * rad, E[1] + n[1] * rad
    horq = []
    for q in range(1, 6):
        f = math.radians(180.0 * q / 6)
        horq.append((cx - n[0] * rad * math.cos(f) + ta[0] * rad * math.sin(f), cz - n[1] * rad * math.cos(f) + ta[1] * rad * math.sin(f)))
    pts += horq
    # arco interior: de I (norte) por el oeste del circulo interior hasta el sur, angulos decrecientes
    ai = math.degrees(math.atan2(I[1] - C1[1], I[0] - C1[0])) % 360.0
    k = max(8, int((ai - (360.0 - ai)) / 16))
    for q in range(k + 1):
        a = ai - (ai - (360.0 - ai)) * q / k
        t = q / k
        ond = -9.0 * math.sin(t * 2 * math.pi) * math.sin(math.pi * t) ** 0.7
        pts.append(_pol(C1, R1 + 2 + ond, a))
    # horquilla sur: la norte reflejada (z -> 300 - z) y recorrida al reves
    pts += [(x, 300.0 - z) for (x, z) in reversed(horq)]
    pts = [(x * K, z * K) for (x, z) in pts]
    return pts, (cx * K, cz * K, rad * K), (cx * K, (300.0 - cz) * K, rad * K)


PUNTOS, HORQ_N, HORQ_S = puntos()
# altura del asfalto (bloques sobre la base) segun la posicion respecto a la LINEA DE SALIDA (0..1): meseta de salida, bajada, llano en la punta norte,
# colina en el arco interior, bajada a la punta sur, llano y subida suave de vuelta a la meseta
ALTURAS = [(0.00, 4), (0.03, 4), (0.12, 0), (0.34, 0), (0.40, 3), (0.48, 9), (0.56, 9), (0.62, 2), (0.70, 0), (0.82, 0), (0.92, 3), (1.0, 4)]


def altura_u(u):
    for (u0, h0), (u1, h1) in zip(ALTURAS, ALTURAS[1:]):
        if u0 <= u <= u1:
            f = (u - u0) / (u1 - u0) if u1 > u0 else 0
            f = f * f * (3 - 2 * f)
            return h0 + (h1 - h0) * f
    return 0


def principal(paso=0.5):
    """(muestras, longitud, indice de la linea de salida): la salida esta en el punto mas al oeste del arco exterior, mirando al norte"""
    ms, total = camino.muestras(paso, PUNTOS)
    n = len(ms)
    ks = min(range(n), key=lambda i: ms[i]['x'])
    for i, m in enumerate(ms): m['h'] = altura_u(((i - ks) % n) / n)
    return ms, total, ks


if __name__ == '__main__':
    import sys
    ms, tot, ks = principal()
    print('longitud', round(tot), 'radio minimo', round(camino.curvatura_min(ms), 1), 'puntos', len(PUNTOS))
    mind, par = 1e9, None
    for i in range(0, len(ms), 4):
        for j in range(i + 1, len(ms), 4):
            gap = min(j - i, len(ms) - (j - i)) * 0.5
            if gap < 50: continue
            d = math.hypot(ms[i]['x'] - ms[j]['x'], ms[i]['z'] - ms[j]['z'])
            if d < mind: mind, par = d, (i, j)
    print('minima distancia entre tramos lejanos', round(mind, 1), par, [(round(ms[q]['x']), round(ms[q]['z'])) for q in par])
    xs = [m['x'] for m in ms]; zs = [m['z'] for m in ms]
    print('x', round(min(xs)), round(max(xs)), 'z', round(min(zs)), round(max(zs)))
    from PIL import Image, ImageDraw
    im = Image.new('RGB', (560, 560), 'white'); dr = ImageDraw.Draw(im)
    for m in ms: dr.ellipse([m['x'] * 2 - 1, m['z'] * 2 - 1, m['x'] * 2 + 1, m['z'] * 2 + 1], fill=(int(30 + m['h'] * 18), 40, 160))
    for (x, z) in PUNTOS: dr.ellipse([x * 2 - 3, z * 2 - 3, x * 2 + 3, z * 2 + 3], outline=(200, 0, 0))
    im.save(sys.argv[1] if len(sys.argv) > 1 else 'camino_emi.png')
