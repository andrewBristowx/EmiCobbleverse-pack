"""Genera la pista «Estación Michialien» (tema Emi / GatitoAlien): plantilla de estructura (.nbt) + datos (.json) + mapa de la interfaz.

Un circuito en forma de LUNA CRECIENTE (la luna de la barriga del Michialien) sobre un mar de nubes pastel, con
- estatuas gigantes de los peluches del Michialien (los de `tools/emi-plush`: base, dorado y sus accesorios), voxelizadas por catalogo_peluches.py;
- un cartel enorme con el pixel art del Michialien (el de la moneda), un portico «EMI» en la meta, farolas con forma de antena, arcos de estrellas,
  un planeta con anillo, una luna, un OVNI, estrellas y planetitas flotantes;
- 2 saltos sobre las nubes (plataforma de salto, igual que en el Volcán), un ATAJO que se salta la horquilla norte con un salto con turbo
  (sin turbo caes a las nubes y vuelves al ultimo punto de control), franjas de turbo y zonas de «mermelada» (lodo).

Uso: python3 emi.py <carpeta tools/emi-carreras>      (MC_TEXTURAS=<textures/block de Minecraft> solo si faltan estatuas en estatuas/)
Escribe src/main/resources/data/emi_carreras/structure/estacion_michialien.nbt, .../pistas/estacion_michialien.json y
assets/emi_carreras/textures/gui/mapa_estacion_michialien.png
"""
import sys, os, math, json, random, hashlib
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'emi-gimnasios'))
from gymlib import V
import camino_emi as ce
import escenario
import modelo
import catalogo_peluches
import mapa

ID = 'estacion_michialien'
ORIGEN = (2000, 64, 0)       # lejos de las otras pistas (circuito x 0..300, volcan x 1000..1320)
MARGEN = 75
GY = 5                       # y local de la superficie de las nubes
BASE = 16                    # y local del asfalto en llano
ALTO = 112
MEDIO = 5.5                  # semianchura de la pista (con pianos)
VUELTAS = 2
GAP = 12                     # bloques sin pista de cada salto
GAP_ATAJO_MIN = 0.55         # sin turbo el arco acaba a este tanto por uno del salto (ya sobre las nubes)

# glifos 5x7 del portico
GLIFOS = {
    'E': ['#####', '#....', '#....', '####.', '#....', '#....', '#####'],
    'M': ['#...#', '##.##', '#.#.#', '#...#', '#...#', '#...#', '#...#'],
    'I': ['#####', '..#..', '..#..', '..#..', '..#..', '..#..', '#####'],
}
# estatuas: (forma, escala, u en el recorrido desde la salida, lado -1 = fuera / +1 = dentro del circuito, distancia al centro de la pista)
ESTATUAS = [
    ('crown', 2.0, 0.07, -1, 30), ('sunglasses', 2.0, 0.17, -1, 32), ('bow', 2.0, 0.43, 1, 26), ('halo', 2.0, 0.50, -1, 32),
    ('witch_hat', 2.0, 0.78, -1, 32), ('santa_hat', 2.0, 0.86, -1, 32), ('pirate', 2.0, 0.93, 1, 26),
    ('headphones', 2.0, 0.60, -1, 32), ('scarf', 2.0, 0.34, -1, 30), ('flower_crown', 2.0, 0.70, -1, 32), ('gold', 2.0, 0.47, 1, 26),
    ('beach_hat', 2.0, 0.26, -1, 30)]


def main(destino):
    ms, total, ks = ce.principal(0.5)
    n = len(ms)
    print('bucle %d bloques, %d muestras, salida en la muestra %d' % (total, n, ks))
    xs = [m['x'] for m in ms]; zs = [m['z'] for m in ms]
    ox, oz = MARGEN - int(min(xs)), MARGEN - int(min(zs))
    W = int(max(xs)) - int(min(xs)) + MARGEN * 2 + 4
    L = int(max(zs)) - int(min(zs)) + MARGEN * 2 + 4
    v = V(W, ALTO, L)
    rnd = random.Random(2077)
    TEX = os.environ.get('MC_TEXTURAS')

    def idx(x, z): return min(range(n), key=lambda q: math.hypot(ms[q]['x'] - x, ms[q]['z'] - z))
    def au(u): return (ks + int(round(u * n))) % n                 # indice de la muestra en la fraccion u del recorrido desde la salida
    alt = [int(round(m['h'])) for m in ms]
    def sup(i): return BASE + alt[i % n]
    def mundo(x, y, z): return [round(ORIGEN[0] + x, 2), ORIGEN[1] + y, round(ORIGEN[2] + z, 2)]
    def caja_mundo(b): return [b[0] + ORIGEN[0], b[1] + ORIGEN[1], b[2] + ORIGEN[2], b[3] + ORIGEN[0], b[4] + ORIGEN[1], b[5] + ORIGEN[2]]

    # ---------- mar de nubes (suelo): blanco con manchas rosas, celestes y sombras grises
    def ruido(x, z): return math.sin(x * 0.11) + math.sin(z * 0.13 + 1.7) + math.sin((x + z) * 0.07 + 0.5) + 0.6 * math.sin((x - z) * 0.19 + 2.0)
    for x in range(W):
        for z in range(L):
            r = ruido(x, z)
            b = 'white_concrete'
            if r > 2.55: b = 'pink_concrete'
            elif r > 2.0: b = 'light_blue_concrete'
            elif r < -2.3: b = 'light_gray_concrete'
            elif rnd.random() < 0.03: b = 'white_wool'
            v.set(x, GY, z, b); v.set(x, GY - 1, z, 'white_concrete')

    # ---------- rasterizado de la pista
    celdas = {}
    saltar = set()                                    # muestras sin pista (huecos de salto)
    def hueco(i):
        return dict(pad0=i - int(9 / 0.5), pad1=i - 1, g0=i, g1=i + int(GAP / 0.5) - 1, tierra=i + int(GAP / 0.5) + int(5 / 0.5))
    A = hueco(au(0.43))                             # arco interior (recto, bajando)
    B = hueco(au(0.775))                             # arco exterior sur
    for g in (A, B): saltar |= set(range(g['g0'], g['g1'] + 1))
    # atajo: carril interior del arco exterior norte, antes de la horquilla -> aterriza en el arco interior despues de ella
    E = dict(turbo0=au(0.158), turbo1=au(0.158) + int(12 / 0.5), pad0=au(0.190), pad1=au(0.190) + int(9 / 0.5))
    sin_muro = set(range(E['pad0'] - 8, E['pad1'] + 10))
    for i, m in enumerate(ms):
        if i in saltar: continue
        nx, nz = -m['tz'], m['tx']
        o = -MEDIO - 1.2
        while o <= MEDIO + 1.2 + 1e-9:
            c = (int(math.floor(m['x'] + ox + nx * o + 0.5)), int(math.floor(m['z'] + oz + nz * o + 0.5)))
            d = abs(o)
            if c not in celdas or d < celdas[c][0]: celdas[c] = (d, i, o)
            o += 0.35
    for c, (d, i, o) in celdas.items():
        y = sup(i); s = ms[i]['s']
        if d <= MEDIO - 1.0:
            b = 'gray_concrete'
            if d < 0.5 and (s % 8) < 4: b = 'white_concrete'
            elif d > MEDIO - 2.0: b = 'purple_concrete'
        elif d <= MEDIO: b = 'purple_concrete' if int(s / 3) % 2 == 0 else 'white_concrete'
        else: b = 'light_gray_concrete'
        v.set(c[0], y, c[1], b)
        v.set(c[0], y - 1, c[1], 'purple_concrete')
        if (c[0] % 5 == 0 and c[1] % 5 == 0) or d > MEDIO:
            v.fill(c[0], GY + 1, c[1], c[0], y - 2, c[1], 'white_concrete')
        if d > MEDIO and not (i in sin_muro and o > 0):                            # muro de contencion (con hueco junto a la plataforma del atajo)
            bl = int(s / 4) % 2 == 0
            v.set(c[0], y + 1, c[1], 'white_concrete' if bl else 'purple_concrete')
            v.set(c[0], y + 2, c[1], 'purple_concrete' if bl else 'white_concrete')
            if int(s) % 12 == 0: v.set(c[0], y + 3, c[1], 'sea_lantern')

    # ---------- utilidades de franjas
    def faja(i0, i1, ancho, color_fn, o_ini=None):
        """colorea celdas de la pista entre las muestras i0..i1; devuelve la caja (x1,y,z1,x2,y,z2) en local"""
        pts = []
        for i in range(i0, i1 + 1):
            m = ms[i % n]; nx, nz = -m['tz'], m['tx']
            o = -ancho if o_ini is None else o_ini
            while o <= (ancho if o_ini is None else ancho) + 1e-9:
                px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
                v.set(px, sup(i), pz, color_fn(i - i0, o)); pts.append((px, sup(i), pz)); o += 0.5
        return [min(p[0] for p in pts), min(p[1] for p in pts), min(p[2] for p in pts), max(p[0] for p in pts), max(p[1] for p in pts) + 2, max(p[2] for p in pts)]

    saltos, turbos, lodo, cajas = [], [], [], []
    def turbo_c(k, o): return 'light_blue_concrete' if (k // 3) % 2 == 0 else 'yellow_concrete'

    def salto(g, ancho_pad):
        # la plataforma verde solo guia; el salto se dispara en el BORDE AZUL de 1 bloque del final (junto al hueco)
        faja(g['pad0'], g['pad1'] - 2, ancho_pad, lambda k, o: 'lime_concrete' if (k // 3) % 2 == 0 else 'green_concrete')
        zona = faja(g['pad1'] - 1, g['pad1'], ancho_pad, lambda k, o: 'blue_concrete')
        for k in range(g['pad0'] + 2, g['pad1'] - 3, 6):
            m = ms[k]; v.set(int(round(m['x'] + ox)), sup(k), int(round(m['z'] + oz)), 'white_concrete')
        for ii in (g['g0'] - 1, g['g1'] + 1):                                      # borde del hueco
            m = ms[ii]; nx, nz = -m['tz'], m['tx']
            for o in range(-int(MEDIO), int(MEDIO) + 1):
                v.set(int(round(m['x'] + ox + nx * o)), sup(ii), int(round(m['z'] + oz + nz * o)), 'yellow_concrete' if (o % 2 == 0) else 'black_concrete')
        for ii in (g['g0'] - 1, g['g1'] + 1):                                      # farolas de lilas a los lados del hueco
            m = ms[ii]; nx, nz = -m['tz'], m['tx']
            for sg in (-1, 1):
                px, pz = int(round(m['x'] + ox + nx * (MEDIO + 2) * sg)), int(round(m['z'] + oz + nz * (MEDIO + 2) * sg))
                v.fill(px, sup(ii) + 1, pz, px, sup(ii) + 5, pz, 'white_concrete'); v.set(px, sup(ii) + 6, pz, 'sea_lantern')
        mt = ms[g['tierra']]
        d = {'zona': caja_mundo(zona), 'destino': mundo(mt['x'] + ox + 0.5, sup(g['tierra']) + 1, mt['z'] + oz + 0.5), 'normal': [round(-mt['tz'], 3), round(mt['tx'], 3)]}
        m0 = ms[g['pad1']]
        dist = math.hypot(d['destino'][0] - (m0['x'] + ox + ORIGEN[0]), d['destino'][2] - (m0['z'] + oz + ORIGEN[2]))
        d['altura'] = 4.5; d['ticks'] = int(dist / 0.62) + 2
        return d

    saltos.append(salto(A, MEDIO - 1.5)); saltos.append(salto(B, MEDIO - 1.5))
    for g in (A, B):                                   # franja de turbo delante de cada plataforma
        turbos.append(caja_mundo(faja(g['pad0'] - 24, g['pad0'] - 6, 2.5, turbo_c)))
    # turbo suelto justo despues de la salida y otro en el arco interior
    turbos.append(caja_mundo(faja(ks + 40, ks + 56, 2.5, turbo_c)))
    turbos.append(caja_mundo(faja(au(0.545), au(0.545) + 16, 2.5, turbo_c)))

    # ---------- atajo: carril interior (3 de ancho) con turbo, plataforma y salto largo sobre la horquilla norte
    def carril(i0, i1, f_color, o0=1.2, o1=4.3):
        pts = []
        for i in range(i0, i1 + 1):
            m = ms[i]; nx, nz = -m['tz'], m['tx']
            o = o0
            while o <= o1 + 1e-9:
                px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
                v.set(px, sup(i), pz, f_color(i - i0, o)); pts.append((px, sup(i), pz)); o += 0.5
        return [min(p[0] for p in pts), min(p[1] for p in pts), min(p[2] for p in pts), max(p[0] for p in pts), max(p[1] for p in pts) + 2, max(p[2] for p in pts)]
    turbos.append(caja_mundo(carril(E['turbo0'], E['turbo1'], turbo_c)))
    carril(E['pad0'], E['pad1'] - 2, lambda k, o: 'lime_concrete' if (k // 3) % 2 == 0 else 'green_concrete')
    pz_ = carril(E['pad1'] - 1, E['pad1'], lambda k, o: 'blue_concrete')
    for k in range(E['pad0'] + 2, E['pad1'] - 3, 6):
        mm_ = ms[k]; v.set(int(round(mm_['x'] + ox - mm_['tz'] * 2.8)), sup(k), int(round(mm_['z'] + oz + mm_['tx'] * 2.8)), 'white_concrete')
    il = au(0.37)                                     # aterrizaje: arco interior justo despues de la horquilla
    ml = ms[il]
    mA0 = ms[E['pad1']]
    ax, az = mA0['x'] + ox - mA0['tz'] * 2.8 + ORIGEN[0], mA0['z'] + oz + mA0['tx'] * 2.8 + ORIGEN[2]
    bx, bz = ml['x'] + ox + ORIGEN[0] + 0.5, ml['z'] + oz + ORIGEN[2] + 0.5
    dist_e = math.hypot(bx - ax, bz - az)
    vel = 0.55
    saltos.append({'zona': caja_mundo(pz_),
                   'destino': [round(ax + (bx - ax) * GAP_ATAJO_MIN, 2), GY + ORIGEN[1] - 1, round(az + (bz - az) * GAP_ATAJO_MIN, 2)],
                   'destino_turbo': [round(bx, 2), sup(il) + 1 + ORIGEN[1], round(bz, 2)],
                   'altura': 8.0, 'ticks': int(dist_e * GAP_ATAJO_MIN / vel) + 2, 'ticks_turbo': int(dist_e / vel) + 2})
    print('atajo: salto de %.0f bloques, ahorra ~%.0f bloques de pista' % (dist_e, ((ml['s'] - ms[E['pad1']]['s']) % total)))

    # ---------- «mermelada» (lodo): una tras el salto del arco interior y una en el arco sur
    def mancha(i0, i1, ancho, lado=0):
        pts = []
        for i in range(i0, i1 + 1):
            m = ms[i % n]; nx, nz = -m['tz'], m['tx']
            o = -ancho if lado <= 0 else 0.0
            tope = ancho if lado >= 0 else 0.0
            while o <= tope + 1e-9:
                px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
                v.set(px, sup(i), pz, rnd.choice(['magenta_terracotta', 'magenta_terracotta', 'pink_terracotta', 'purple_terracotta']))
                pts.append((px, sup(i), pz)); o += 0.5
        return [min(p[0] for p in pts) + ORIGEN[0], min(p[1] for p in pts) + ORIGEN[1], min(p[2] for p in pts) + ORIGEN[2],
                max(p[0] for p in pts) + ORIGEN[0], max(p[1] for p in pts) + 2 + ORIGEN[1], max(p[2] for p in pts) + ORIGEN[2]]
    i1 = A['tierra'] + 24
    lodo.append(mancha(i1, i1 + 36, MEDIO - 1.0, lado=1) + [0.4])                    # solo la mitad derecha: se esquiva por la izquierda
    i2 = au(0.835)
    lodo.append(mancha(i2, i2 + 40, MEDIO - 1.0, lado=-1) + [0.4])

    # ---------- meta y puntos de control
    s_meta = ms[ks]['s']
    cps = []
    for j, u in enumerate((0.0, 0.085, 0.165, 0.40, 0.50, 0.60, 0.70, 0.84)):      # ninguno dentro del tramo que salta el atajo (0.19 .. 0.37)
        k = au(u)
        m = ms[k]; nx, nz = -m['tz'], m['tx']
        esq = []
        for a_, b_ in ((-MEDIO - 1.5, -1.5), (-MEDIO - 1.5, 1.5), (MEDIO + 1.5, -1.5), (MEDIO + 1.5, 1.5)):
            esq.append((m['x'] + ox + nx * a_ + m['tx'] * b_, m['z'] + oz + nz * a_ + m['tz'] * b_))
        x1, x2 = int(math.floor(min(e[0] for e in esq))), int(math.ceil(max(e[0] for e in esq)))
        z1_, z2_ = int(math.floor(min(e[1] for e in esq))), int(math.ceil(max(e[1] for e in esq)))
        y = sup(k)
        kr = (k + 8) % n; mr = ms[kr]
        yaw = math.degrees(math.atan2(-mr['tx'], mr['tz']))
        cps.append({'k': k, 'zona': [x1, y - 1, z1_, x2, y + 6, z2_], 'resp': [mr['x'] + ox + 0.5, sup(kr) + 1, mr['z'] + oz + 0.5, yaw]})
        if j == 0: continue
        for sgn in (-1, 1):
            px, pz = int(round(m['x'] + ox + nx * (MEDIO + 1.5) * sgn)), int(round(m['z'] + oz + nz * (MEDIO + 1.5) * sgn))
            v.fill(px, y + 1, pz, px, y + 7, pz, 'light_blue_concrete'); v.set(px, y + 8, pz, 'sea_lantern')
        for o in range(-int(MEDIO + 1.5), int(MEDIO + 1.5) + 1):
            v.set(int(round(m['x'] + ox + nx * o)), y + 7, int(round(m['z'] + oz + nz * o)), 'light_blue_concrete')

    # portico de la meta con el cartel «EMI»
    m0 = ms[ks]; y0 = sup(ks); nx0, nz0 = -m0['tz'], m0['tx']
    xm, zm = int(round(m0['x'] + ox)), int(round(m0['z'] + oz))                      # el tramo mira al norte (tx=0, tz=-1): la meta es una fila en x
    for sgn in (-1, 1):
        px = xm + int(round(nx0 * (MEDIO + 1.5))) * sgn
        v.fill(px, y0 + 1, zm, px, y0 + 24, zm + 1, 'white_concrete')
        v.fill(px + sgn, y0 + 1, zm, px + sgn, y0 + 24, zm + 1, 'purple_concrete')
        for yy in range(y0 + 3, y0 + 24, 5): v.set(px, yy, zm - 1, 'sea_lantern')
    ancho_p = int(MEDIO + 2.5)
    for o in range(-ancho_p, ancho_p + 1):
        for dz in (0, 1):
            v.set(xm + o, y0 + 9, zm + dz, 'black_concrete' if (o + dz) % 2 == 0 else 'white_concrete')
            v.set(xm + o, y0 + 10, zm + dz, 'white_concrete' if (o + dz) % 2 == 0 else 'black_concrete')
        for dz in (0, 1):                                                              # marco del cartel
            v.set(xm + o, y0 + 24, zm + dz, 'purple_concrete')
    v.fill(xm - ancho_p - 1, y0 + 11, zm, xm + ancho_p + 1, y0 + 24, zm + 1, 'purple_concrete')
    # letras 5x7 de un bloque por pixel, centradas en el cartel
    tx0 = xm - 9
    for q, ch in enumerate('EMI'):
        for fy, fila in enumerate(GLIFOS[ch]):
            for fx, c in enumerate(fila):
                if c == '#': v.set(tx0 + q * 7 + fx, y0 + 21 - fy, zm + 2, 'white_concrete')      # en la cara sur: la ven los corredores de la parrilla y al pasar la meta
    # raya de meta en el asfalto
    for o in range(-int(MEDIO - 1), int(MEDIO)):
        for dd in (0, 1):
            v.set(xm + o, y0, zm - dd, 'black_concrete' if (o + dd) % 2 == 0 else 'white_concrete')

    # ---------- cajas de poderes (hileras de 4 en la pista, una de 3 en el atajo no hay: es un salto)
    def hilera(k, offs):
        m = ms[k % n]
        for o in offs:
            px, pz = m['x'] + ox + (-m['tz']) * o, m['z'] + oz + m['tx'] * o
            cajas.append([int(round(px)) + ORIGEN[0], sup(k) + 1 + ORIGEN[1], int(round(pz)) + ORIGEN[2]])
    for u in (0.105, 0.30, 0.47, 0.585, 0.68, 0.82, 0.90):
        hilera(au(u), (-3, -1, 1, 3))
    # michiestrellitas: grupos de 3 en zigzag entre las hileras de cajas, fuera de los huecos de salto
    estrellas = []
    for u in (0.045, 0.135, 0.22, 0.41, 0.52, 0.635, 0.715, 0.865, 0.94):
        k0 = au(u)
        for j, o in enumerate((-3.0, 0.0, 3.0)):
            kk = k0 + j * 14
            if any((kk + d) % n in saltar for d in range(-26, 27, 4)): continue
            m = ms[kk % n]
            px, pz = m['x'] + ox + (-m['tz']) * o, m['z'] + oz + m['tx'] * o
            estrellas.append([int(round(px)) + ORIGEN[0], sup(kk) + 1 + ORIGEN[1], int(round(pz)) + ORIGEN[2]])
    print(len(estrellas), 'michiestrellitas')

    # ---------- parrilla de salida (detras de la meta, en el arco sur... la pista viene de abajo)
    parrilla = []
    for fila in range(6):
        for col in (-1, 1):
            k = (ks - int((6 + fila * 3.2 + (1.6 if col > 0 else 0)) / 0.5)) % n
            m = ms[k]; o = col * 2.2
            px, pz = m['x'] + ox + (-m['tz']) * o, m['z'] + oz + m['tx'] * o
            yaw = math.degrees(math.atan2(-m['tx'], m['tz']))
            parrilla.append([px + 0.5, sup(k) + 1, pz + 0.5, yaw])
            v.set(int(round(px)), sup(k), int(round(pz)), 'white_concrete')

    # ---------- farolas con forma de antena de Michialien (poste blanco y bola lila) fuera de los muros
    farolas = 0
    for i in range(0, n, 2):
        s = ms[i]['s']
        if int(s) % 26 != 0 or i in saltar: continue
        m = ms[i]; nx, nz = -m['tz'], m['tx']
        px, pz = int(round(m['x'] + ox - nx * (MEDIO + 3))), int(round(m['z'] + oz - nz * (MEDIO + 3)))
        y = sup(i)
        v.fill(px, GY + 1, pz, px, y + 5, pz, 'white_concrete')
        for dx in range(-1, 2):
            for dy in range(-1, 2):
                for dz in range(-1, 2):
                    if abs(dx) + abs(dy) + abs(dz) <= 2: v.set(px + dx, y + 7 + dy, pz + dz, 'amethyst_block')
        v.set(px, y + 7, pz, 'sea_lantern')
        farolas += 1
    print('farolas', farolas)

    # ---------- arcos de estrellas sobre la pista (tres tramos)
    def arco(k, R):
        m = ms[k % n]; nx, nz = -m['tz'], m['tx']
        y0_ = sup(k)
        for ang in range(0, 181, 4):
            a = math.radians(ang)
            for dr in (0.0, 0.8):
                o = (R + dr) * math.cos(a); h = (R + dr) * math.sin(a)
                px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
                v.set(px, y0_ + 1 + int(round(h)), pz, 'purple_concrete' if (k // 8) % 2 == 0 else 'white_concrete')
        v.set(int(round(m['x'] + ox)), y0_ + 1 + int(R) + 1, int(round(m['z'] + oz)), 'sea_lantern')
        for ang in (45, 135):
            a = math.radians(ang)
            v.set(int(round(m['x'] + ox + nx * (R + 0.4) * math.cos(a))), y0_ + 1 + int(round((R + 0.4) * math.sin(a))), int(round(m['z'] + oz + nz * (R + 0.4) * math.cos(a))), 'sea_lantern')
    for u0, u1 in ((0.46, 0.50), (0.63, 0.67), (0.92, 0.955)):
        for k in range(au(u0), au(u1), 12): arco(k, MEDIO + 2.5)

    # ---------- decoracion del cielo: planeta con anillo, luna, OVNI, estrellas, planetitas
    def esfera(cx, cy, cz, r, f):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for y in range(int(cy - r) - 1, int(cy + r) + 2):
                for z in range(int(cz - r) - 1, int(cz + r) + 2):
                    if math.sqrt((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2) <= r: v.set(x, y, z, f(x, y, z))
    def planeta(cx, cy, cz, r):
        bandas = ['purple_concrete', 'magenta_concrete', 'pink_concrete', 'purple_concrete', 'light_blue_concrete']
        esfera(cx, cy, cz, r, lambda x, y, z: bandas[int((y - cy + r) / 3.2) % len(bandas)])
        for x in range(int(cx - r * 2.3), int(cx + r * 2.3) + 1):
            for z in range(int(cz - r * 2.3), int(cz + r * 2.3) + 1):
                for y0_ in range(int(cy - r), int(cy + r) + 1):
                    pass
        for x in range(int(cx - r * 2.3), int(cx + r * 2.3) + 1):
            for z in range(int(cz - r * 2.3), int(cz + r * 2.3) + 1):
                d = math.hypot(x - cx, z - cz)
                if r * 1.45 < d < r * 2.2:
                    y = int(round(cy + (x - cx) * 0.22))                          # anillo inclinado
                    if math.sqrt((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2) > r + 0.8:
                        v.set(x, y, z, 'white_concrete' if int(d / 2) % 2 == 0 else 'light_blue_concrete')
    def luna(cx, cy, cz, r, esp):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for y in range(int(cy - r) - 1, int(cy + r) + 2):
                for z in range(int(cz - esp), int(cz + esp) + 1):
                    if math.hypot(x - cx, y - cy) <= r and math.hypot(x - (cx + r * 0.42), y - (cy + r * 0.12)) > r * 0.86:
                        v.set(x, y, z, 'amethyst_block' if abs(z - cz) < esp - 1 else 'sea_lantern')
    def ovni(cx, cy, cz, R):
        for x in range(int(cx - R) - 1, int(cx + R) + 2):
            for z in range(int(cz - R) - 1, int(cz + R) + 2):
                d = math.hypot(x - cx, z - cz)
                if d > R: continue
                h = (1 - (d / R) ** 2) ** 0.5
                for y in range(int(cy - h * R * 0.30), int(cy + h * R * 0.26) + 1):
                    v.set(x, y, z, 'light_gray_concrete' if y >= cy else 'gray_concrete')
                if d > R * 0.86 and h * R * 0.3 > 0.5:
                    ang = math.degrees(math.atan2(z - cz, x - cx)) % 360
                    if int(ang / 20) % 2 == 0: v.set(x, int(cy), z, 'sea_lantern')
                if d < R * 0.38:                                                    # cupula de cristal
                    hc = (1 - (d / (R * 0.38)) ** 2) ** 0.5 * R * 0.34
                    for y in range(int(cy + h * R * 0.26) + 1, int(cy + hc + h * R * 0.26) + 1): v.set(x, y, z, 'light_blue_stained_glass')
        v.set(int(cx), int(cy - R * 0.3), int(cz), 'sea_lantern')
    def estrella(cx, cy, cz, r, b):
        for d in range(-r, r + 1):
            v.set(cx + d, cy, cz, b); v.set(cx, cy + d, cz, b); v.set(cx, cy, cz + d, b)
        v.set(cx, cy, cz, 'sea_lantern')
        for dx in (-1, 1):
            for dy in (-1, 1): v.set(cx + dx, cy + dy, cz, b)

    # ---------- estatuas de los peluches (las de emi-plush ya vienen cacheadas en estatuas/)
    def lejos(x, z, d):
        for m in ms[::4]:
            if math.hypot(m['x'] + ox - x, m['z'] + oz - z) < d: return False
        return True
    ocupado = []
    def sitio_libre(x, z, r):
        if not (r + 8 <= W - 1 and x > r + 8 and z > r + 8 and x < W - r - 8 and z < L - r - 8): return False
        if not lejos(x, z, r + MEDIO + 6): return False
        return all(math.hypot(x - ox_, z - oz_) > r + r_ + 2 for ox_, oz_, r_ in ocupado)
    def poner(forma, esc, x, z, hacia=None, veces=None):
        bl = catalogo_peluches.cargar(forma, esc, TEX)
        if veces is None:                                                           # el frente de la estatua mira hacia el punto `hacia` (x, z)
            fx, fz = hacia[0] - x, hacia[1] - z
            veces = (0 if fz < 0 else 2) if abs(fz) >= abs(fx) else (1 if fx > 0 else 3)
        bl = modelo.girar(bl, veces)
        r = max(max(abs(k[0]) for k in bl), max(abs(k[2]) for k in bl)) + 3
        escenario.poner_estatua(v, bl, x, GY + 1, z)
        ocupado.append((x, z, r))
        return r
    # las dos grandes de la meta, mirando a la parrilla (hacia el sur)
    for forma, lado in (('base', -1), ('gold_crown', 1)):
        esc = 2.8
        bl = catalogo_peluches.cargar(forma, esc, TEX)
        r = max(max(abs(k[0]) for k in bl), max(abs(k[2]) for k in bl)) + 3
        x = xm + lado * int(round(MEDIO + 3 + r + 2)); z = zm - 6
        poner(forma, esc, x, z, veces=2)
        print('estatua grande', forma, 'en', mundo(x, GY, z))
    ocupado.append((xm + 30, zm + 60, 12))                                         # sitio del podio
    colocadas = 0
    for forma, esc, u, lado, dist in ESTATUAS:
        k = au(u); m = ms[k]; nx, nz = -m['tz'], m['tx']
        bl = catalogo_peluches.cargar(forma, esc, TEX)
        r = max(max(abs(q[0]) for q in bl), max(abs(q[2]) for q in bl)) + 3
        hecho = False
        for lado_p in (lado, -lado):                                                # si no cabe por un lado, se prueba por el otro
            for extra in (0, 6, 12, 18, 26):
                x = int(round(m['x'] + ox + nx * lado_p * (dist + extra))); z = int(round(m['z'] + oz + nz * lado_p * (dist + extra)))
                if sitio_libre(x, z, r):
                    poner(forma, esc, x, z, hacia=(m['x'] + ox, m['z'] + oz)); colocadas += 1; hecho = True
                    print('estatua %s u=%.2f en %s' % (forma, u, mundo(x, GY, z)))
                    break
            if hecho: break
        if not hecho: print('AVISO: no hay sitio para', forma)
    print(colocadas, 'estatuas de peluches')

    # ---------- podio dentro del circuito, junto a la salida
    pxc, pzc = xm + 30, zm + 60
    v.fill(pxc - 9, GY + 1, pzc - 5, pxc + 9, GY + 1, pzc + 9, 'smooth_quartz')
    for dx, h, col in ((-3, 2, 'light_gray_concrete'), (0, 3, 'yellow_concrete'), (3, 1, 'orange_concrete')):
        v.fill(pxc + dx - 1, GY + 2, pzc - 1, pxc + dx + 1, GY + 1 + h, pzc + 1, col)
    podio = [pxc + 0.5, GY + 2, pzc + 4.5, 180]

    # ---------- cartel gigante del Michialien (pixel art del dueño) al noroeste de la salida, mirando al sur
    ref = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'moneda', 'michi_referencia.webp')
    from PIL import Image
    im = Image.open(ref).convert('RGB').crop((140, 176, 908, 880)).resize((48, 44), Image.NEAREST)
    pal = modelo._paleta(TEX) if TEX else None
    filas, paleta, nombres = [], {}, {}
    for j in range(44):
        fila = ''
        for i in range(48):
            c = im.getpixel((i, j))
            px_, py_ = 140 + (i + 0.5) * 16, 176 + (j + 0.5) * 16                    # centro de la celda en la imagen original
            if math.hypot(px_ - 512, py_ - 512) > 418 or (c[2] - c[1] >= 20 and c[1] > 205): b = 'light_blue_concrete'      # fondo (fuera de la moneda o lavanda)
            elif min(c) > 200 and max(c) - min(c) < 40: b = 'white_concrete'
            elif max(c) < 90: b = 'black_concrete'
            elif c[2] > 190 and c[0] > 130 and c[1] < 200: b = 'amethyst_block'
            else:
                b = modelo.a_bloques({(0, 0, 0): c}, TEX, sesgo={'amethyst_block': 0.3})[(0, 0, 0)] if TEX else 'white_concrete'
            if b not in nombres: nombres[b] = chr(65 + len(nombres))
            paleta[nombres[b]] = b
            fila += nombres[b]
        filas.append(fila)
    cx_c = xm - 34; cz_c = zm - 70; yb = GY + 16
    for px in (cx_c - 20, cx_c + 20):
        v.fill(px, GY + 1, cz_c + 1, px + 1, yb, cz_c + 2, 'quartz_pillar')
    v.fill(cx_c - 26, yb - 3, cz_c, cx_c + 26, yb + 47, cz_c + 1, 'purple_concrete')      # marco
    v.pixelart_z(cx_c - 24, yb, cz_c + 2, filas, paleta)
    v.fill(cx_c - 26, yb - 3, cz_c + 2, cx_c + 26, yb - 3, cz_c + 2, 'sea_lantern'); v.fill(cx_c - 26, yb + 47, cz_c + 2, cx_c + 26, yb + 47, cz_c + 2, 'sea_lantern')
    print('cartel', mundo(cx_c, yb, cz_c))
    luna(cx_c + 4, yb + 70, cz_c - 4, 16, 4)

    # ---------- planeta, OVNI, estrellas y planetitas, en el hueco de la luna (al este) y por el cielo
    bx_, bz_ = (int(max(xs)) + ox) - 20, int(sum(zs) / n + oz)
    planeta(bx_ + 22, 62, bz_ + 40, 15)
    ovni(bx_ + 12, 44, bz_ - 50, 17)
    def cielo_libre(x, y, z, r):
        if abs(x - cx_c) < 32 + r and -8 - r < z - cz_c < 12 + r and y < yb + 52 + r: return False        # delante del cartel
        if y < 66 and any(math.hypot(x - ox_, z - oz_) < r_ + r + 3 for ox_, oz_, r_ in ocupado): return False  # encima de una estatua
        return lejos(x, z, 22 + r) or y > 62
    for _ in range(60):
        x, z = rnd.randint(8, W - 9), rnd.randint(8, L - 9)
        y = rnd.randint(40, ALTO - 8); r = rnd.randint(2, 4)
        if cielo_libre(x, y, z, r + 2):
            estrella(x, y, z, r, rnd.choice(['yellow_concrete', 'yellow_concrete', 'pink_concrete', 'light_blue_concrete', 'white_concrete']))
    for _ in range(20):
        x, z = rnd.randint(10, W - 11), rnd.randint(10, L - 11)
        y = rnd.randint(46, ALTO - 12); r = rnd.randint(3, 7)
        if cielo_libre(x, y, z, r):
            col = rnd.choice(['pink_concrete', 'light_blue_concrete', 'purple_concrete', 'white_concrete', 'magenta_concrete'])
            esfera(x, y, z, r, lambda xx, yy, zz, col=col: col)

    # ---------- mapa para la interfaz
    ruta_mapa = os.path.join(destino, 'src/main/resources/assets/emi_carreras/textures/gui/mapa_' + ID + '.png')
    trazos = [(pts, 2 * MEDIO, (225, 215, 245, 255)) for pts in mapa.partir([{'x': m['x'] + ox, 'z': m['z'] + oz} for m in ms], saltar)]
    cajas_m = [(t[0] - ORIGEN[0], t[2] - ORIGEN[2], t[3] - ORIGEN[0], t[5] - ORIGEN[2], (255, 215, 40, 255)) for t in turbos]
    cajas_m += [(sa['zona'][0] - ORIGEN[0], sa['zona'][2] - ORIGEN[2], sa['zona'][3] - ORIGEN[0], sa['zona'][5] - ORIGEN[2], (60, 120, 255, 255)) for sa in saltos]
    mp = ms[ks]
    mw_, mh_ = mapa.dibujar(ruta_mapa, W, L, trazos, cajas=cajas_m, meta=(mp['x'] + ox, mp['z'] + oz, mp['tx'], mp['tz'], MEDIO),
                            relleno=[(bx_ + 22, bz_ + 40, 15, (80, 45, 130, 255))])

    # ---------- guardar
    caida = ORIGEN[1] + GY + 1.8      # cualquier cosa que pise las nubes fuera de la pista cuenta como caida
    nbt = os.path.join(destino, 'src/main/resources/data/emi_carreras/structure', ID + '.nbt')
    pal_, nb = v.guardar(nbt, aire_relleno=False)
    print('plantilla', W, ALTO, L, '-', nb, 'bloques,', pal_, 'estados ->', nbt)
    ver = hashlib.sha1(open(nbt, 'rb').read()).hexdigest()[:12]
    datos = {
        'version': ver, 'nombre': 'Estación Michialien', 'plantilla': 'emi_carreras:' + ID, 'origen': list(ORIGEN), 'vueltas': VUELTAS,
        'musica': 'emi', 'mapa': {'x0': ORIGEN[0], 'z0': ORIGEN[2], 'ancho': W, 'largo': L, 'px': mw_, 'py': mh_},
        'parrilla': [[round(p[0] + ORIGEN[0], 2), p[1] + ORIGEN[1], round(p[2] + ORIGEN[2], 2), round(p[3], 1)] for p in parrilla],
        'podio': [round(podio[0] + ORIGEN[0], 2), podio[1] + ORIGEN[1], round(podio[2] + ORIGEN[2], 2), podio[3]],
        'checkpoints': [[c['zona'][0] + ORIGEN[0], c['zona'][1] + ORIGEN[1], c['zona'][2] + ORIGEN[2], c['zona'][3] + ORIGEN[0], c['zona'][4] + ORIGEN[1], c['zona'][5] + ORIGEN[2],
                         round(c['resp'][0] + ORIGEN[0], 2), c['resp'][1] + ORIGEN[1], round(c['resp'][2] + ORIGEN[2], 2), round(c['resp'][3], 1)] for c in cps],
        'cajas': cajas, 'estrellas': estrellas, 'turbos': turbos, 'saltos': saltos, 'lodo': lodo, 'voltorbs': [], 'caidaY': caida,
    }
    js = os.path.join(destino, 'src/main/resources/data/emi_carreras/pistas', ID + '.json')
    os.makedirs(os.path.dirname(js), exist_ok=True)
    json.dump(datos, open(js, 'w'), indent=1)
    print('datos ->', js, '|', len(cajas), 'cajas,', len(saltos), 'saltos,', len(lodo), 'zonas de lodo,', len(turbos), 'turbos,', len(cps), 'puntos de control')
    return v, datos


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else '..')
