"""Genera la pista «Volcán Cinnabar»: plantilla de estructura (.nbt) + datos (.json) para el mod emi-carreras.

Es una pista más difícil que el Circuito Pokéball: más estrecha, con desnivel, dos SALTOS sobre lava (plataforma de salto: el servidor te lanza en arco),
un ATAJO por el interior con un salto largo que solo se alcanza si pisas antes la franja de turbo («como en Crash Team Racing»), LODO que frena y
Voltorb fijos que explotan si los tocas.

Uso: python3 volcan.py <carpeta tools/emi-carreras>
Escribe src/main/resources/data/emi_carreras/structure/volcan_cinnabar.nbt y .../pistas/volcan_cinnabar.json
"""
import sys, os, math, json, random, hashlib
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'emi-gimnasios'))
from gymlib import V
import camino
import camino_volcan as cv
import escenario
import modelo
import catalogo_estatuas

ID = 'volcan_cinnabar'
ORIGEN = (1000, 64, 0)       # lejos del Circuito Pokéball, que ocupa x 0..300
MARGEN = 40
GY = 5                       # y local de la superficie del suelo (los trozos de lava quedan a este nivel)
BASE = 17                    # y local del asfalto en llano
ALTO = 86
MEDIO = 5.0                  # semianchura de la pista principal (con pianos)
MEDIO_A = 3.6                # semianchura del atajo
VUELTAS = 2
GAP_A = 12                   # bloques sin pista del primer salto
GAP_B = 12
GAP_ATAJO = 17


def main(destino):
    ms, total = cv.principal(0.5)
    at, tot_at = cv.abierto([cv.ATAJO_ENTRADA] + cv.ATAJO_MEDIOS + [cv.ATAJO_SALIDA], 0.5)
    n, na = len(ms), len(at)
    print('bucle %d bloques, atajo %d bloques' % (total, tot_at))
    xs = [m['x'] for m in ms] + [m['x'] for m in at]; zs = [m['z'] for m in ms] + [m['z'] for m in at]
    ox, oz = MARGEN - int(min(xs)), MARGEN - int(min(zs))
    W = int(max(xs)) - int(min(xs)) + MARGEN * 2 + 4
    L = int(max(zs)) - int(min(zs)) + MARGEN * 2 + 4
    v = V(W, ALTO, L)
    rnd = random.Random(1912)

    def idx(path, x, z): return min(range(len(path)), key=lambda q: math.hypot(path[q]['x'] - x, path[q]['z'] - z))
    def lx(m): return m['x'] + ox
    def lz(m): return m['z'] + oz

    # ---------- alturas enteras
    alt = [int(round(m['h'])) for m in ms]
    i_ent, i_sal = idx(ms, *cv.ATAJO_ENTRADA), idx(ms, *cv.ATAJO_SALIDA)
    h0, h1 = alt[i_ent], alt[i_sal]
    ALTA = 7                                         # el atajo sube a una cresta para saltar la lava
    alt_a = []
    for i in range(na):
        t = i / (na - 1)
        if t < 0.30: h = h0 + (ALTA) * (t / 0.30)
        elif t < 0.68: h = h0 + ALTA
        else: h = h0 + ALTA + (h1 - h0 - ALTA) * ((t - 0.68) / 0.32)
        alt_a.append(int(round(h)))
    # ---------- huecos (saltos) en el recorrido; las posiciones se buscan sobre las muestras
    def hueco(path, x_ini, z_ini, largo, pad):
        """indices de muestra: pad0..pad1 (plataforma), g0..g1 (hueco), aterrizaje"""
        i = idx(path, x_ini, z_ini)
        paso = 0.5
        return dict(pad0=i - int(pad / paso), pad1=i - 1, g0=i, g1=i + int(largo / paso) - 1, tierra=i + int(largo / paso) + int(5 / paso))
    A = hueco(ms, 126, 40, GAP_A, 9)
    B = hueco(ms, 24, 150, GAP_B, 9)
    # el segundo salto va hacia el norte (z decrece)
    ia = idx(at, 196, 168)
    S = dict(turbo0=ia - int(16 / 0.5), turbo1=ia - int(9 / 0.5) - 1, pad0=ia - int(8 / 0.5), pad1=ia - 1, g0=ia, g1=ia + int(GAP_ATAJO / 0.5) - 1, tierra=ia + int(GAP_ATAJO / 0.5) + int(5 / 0.5))
    saltar_main = set(range(A['g0'], A['g1'] + 1)) | set(range(B['g0'], B['g1'] + 1))
    saltar_at = set(range(S['g0'], S['g1'] + 1))
    print('huecos: A en s=%d, B en s=%d, atajo en s=%d' % (ms[A['g0']]['s'], ms[B['g0']]['s'], at[S['g0']]['s']))

    # ---------- suelo de basalto y blackstone
    for x in range(W):
        for z in range(L):
            r = rnd.random()
            b = 'basalt' if r > 0.18 else ('blackstone' if r > 0.04 else 'magma_block')
            v.set(x, GY, z, b)
            v.set(x, GY - 1, z, 'blackstone')

    # ---------- rasterizado de las dos pistas
    celdas = {}
    def rasterizar(path, medio, nombre, saltar):
        for i, m in enumerate(path):
            if i in saltar: continue
            nx, nz = -m['tz'], m['tx']
            o = -medio - 1.2
            while o <= medio + 1.2 + 1e-9:
                c = (int(math.floor(m['x'] + ox + nx * o + 0.5)), int(math.floor(m['z'] + oz + nz * o + 0.5)))
                d = abs(o)
                if c not in celdas or d < celdas[c][0]: celdas[c] = (d, i, o, nombre)
                o += 0.35
    rasterizar(ms, MEDIO, 'm', saltar_main)
    rasterizar(at, MEDIO_A, 'a', saltar_at)
    sup = {'m': lambda i: BASE + alt[i], 'a': lambda i: BASE + alt_a[i]}
    paths = {'m': ms, 'a': at}
    medios = {'m': MEDIO, 'a': MEDIO_A}
    for c, (d, i, o, nom) in celdas.items():
        y = sup[nom](i); s = paths[nom][i]['s']; medio = medios[nom]
        if d <= medio - 1.0:
            b = 'gray_concrete'
            if d < 0.5 and (s % 8) < 4: b = 'orange_concrete'
            elif d > medio - 2.0: b = 'black_concrete'
        elif d <= medio: b = 'orange_concrete' if int(s / 3) % 2 == 0 else 'black_concrete'
        else: b = 'blackstone'
        v.set(c[0], y, c[1], b)
        v.set(c[0], y - 1, c[1], 'polished_blackstone_bricks')
        if (c[0] % 5 == 0 and c[1] % 5 == 0) or d > medio:
            v.fill(c[0], GY + 1, c[1], c[0], y - 2, c[1], 'blackstone')
        if d > medio:                                   # muro de contencion
            roj = int(s / 4) % 2 == 0
            v.set(c[0], y + 1, c[1], 'black_concrete' if roj else 'orange_concrete')
            v.set(c[0], y + 2, c[1], 'orange_concrete' if roj else 'black_concrete')
            if int(s) % 12 == 0: v.set(c[0], y + 3, c[1], 'shroomlight')

    def mundo(x, y, z): return [ORIGEN[0] + x, ORIGEN[1] + y, ORIGEN[2] + z]

    # ---------- fosos de lava bajo los huecos (a nivel del suelo; el borde del suelo los contiene)
    pozos = []
    def pozo(path, g0, g1, ancho):
        celdas_p = set()
        for i in range(g0 - 2, g1 + 3):
            m = path[i]; nx, nz = -m['tz'], m['tx']
            o = -ancho
            while o <= ancho:
                celdas_p.add((int(math.floor(m['x'] + ox + nx * o + 0.5)), int(math.floor(m['z'] + oz + nz * o + 0.5)))); o += 0.4
        for c in celdas_p:
            if c in celdas and celdas[c][3]: continue          # no pisar la pista
            v.set(c[0], GY - 3, c[1], 'blackstone')
            for y in range(GY - 2, GY + 1): v.set(c[0], y, c[1], 'lava')
        pozos.append(celdas_p)
    pozo(ms, A["g0"], A["g1"], MEDIO + 9)
    pozo(ms, B["g0"], B["g1"], MEDIO + 9)
    pozo(at, S["g0"], S["g1"], MEDIO_A + 10)

    # ---------- utilidades de franjas
    def faja(path, nom, i0, i1, ancho, color_fn, extra_y=0):
        """colorea celdas de la pista entre las muestras i0..i1 (|o|<=ancho); devuelve la caja (x1,y,z1,x2,y,z2) en local"""
        pts = []
        for i in range(i0, i1 + 1):
            m = path[i]; nx, nz = -m['tz'], m['tx']
            o = -ancho
            while o <= ancho + 1e-9:
                px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
                y = sup[nom](i)
                v.set(px, y, pz, color_fn(i - i0, o))
                pts.append((px, y, pz)); o += 0.5
        return [min(p[0] for p in pts), min(p[1] for p in pts), min(p[2] for p in pts), max(p[0] for p in pts), max(p[1] for p in pts) + 2, max(p[2] for p in pts)]

    saltos, turbos, lodo, voltorbs = [], [], [], []

    def salto(path, nom, g, ancho_pad, normal_dest=None, turbo_dest=None):
        """pone la plataforma de salto y devuelve el dict del salto (coordenadas de mundo)"""
        zona = faja(path, nom, g['pad0'], g['pad1'], ancho_pad, lambda k, o: ('lime_concrete' if (k // 3) % 2 == 0 else 'green_concrete') if abs(o) > 0.6 or (k // 3) % 2 == 0 else 'white_concrete')
        # flechas blancas hacia delante
        for k in range(g['pad0'] + 2, g['pad1'], 6):
            m = path[k]
            px, pz = int(round(m['x'] + ox)), int(round(m['z'] + oz)); y = sup[nom](k)
            v.set(px, y, pz, 'white_concrete')
        # borde del hueco
        for ii in (g['g0'] - 1, g['g1'] + 1):
            m = path[ii]; nx, nz = -m['tz'], m['tx']
            for o in range(-int(medios[nom]), int(medios[nom]) + 1):
                px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
                v.set(px, sup[nom](ii), pz, 'yellow_concrete' if (o % 2 == 0) else 'black_concrete')
        mt = path[g['tierra']]
        yt = sup[nom](g['tierra'])
        d = {'zona': [zona[0] + ORIGEN[0], zona[1] + ORIGEN[1], zona[2] + ORIGEN[2], zona[3] + ORIGEN[0], zona[4] + ORIGEN[1], zona[5] + ORIGEN[2]],
             'destino': [round(mt['x'] + ox + ORIGEN[0] + 0.5, 2), yt + 1 + ORIGEN[1], round(mt['z'] + oz + ORIGEN[2] + 0.5, 2)],
             'normal': [round(-mt['tz'], 3), round(mt['tx'], 3)]}
        return d, mt

    # salto A y B (principal)
    dA, mA = salto(ms, 'm', A, MEDIO - 1.5)
    dB, mB = salto(ms, 'm', B, MEDIO - 1.5)
    for d, g, m0 in ((dA, A, ms[A['pad1']]), (dB, B, ms[B['pad1']])):
        dist = math.hypot(d['destino'][0] - (m0['x'] + ox + ORIGEN[0]), d['destino'][2] - (m0['z'] + oz + ORIGEN[2]))
        d['altura'] = 4.5; d['ticks'] = int(dist / 0.62) + 2
        saltos.append(d)
    # franjas de turbo delante de cada plataforma (en el principal) para entrar rapido
    for g in (A, B):
        turbos.append([ (b + (ORIGEN[k % 3] if True else 0)) for k, b in enumerate(faja(ms, 'm', g['pad0'] - 24, g['pad0'] - 6, 2.5, lambda k, o: 'light_blue_concrete' if (k // 3) % 2 == 0 else 'yellow_concrete')) ])

    # atajo: franja de turbo ESTRECHA (3 de ancho) y plataforma con dos destinos (con turbo se llega a la otra orilla; sin turbo se cae a la lava)
    t_zona = faja(at, 'a', S['turbo0'], S['turbo1'], 1.0, lambda k, o: 'light_blue_concrete' if (k // 2) % 2 == 0 else 'yellow_concrete')
    turbos.append([t_zona[0] + ORIGEN[0], t_zona[1] + ORIGEN[1], t_zona[2] + ORIGEN[2], t_zona[3] + ORIGEN[0], t_zona[4] + ORIGEN[1], t_zona[5] + ORIGEN[2]])
    dS, mS = salto(at, 'a', S, MEDIO_A - 1.2)
    m0 = at[S['pad1']]
    dist_l = math.hypot(dS['destino'][0] - (m0['x'] + ox + ORIGEN[0]), dS['destino'][2] - (m0['z'] + oz + ORIGEN[2]))
    dS['destino_turbo'] = list(dS['destino'])
    # sin turbo: el arco acaba a un 55 % del hueco, ya dentro de la lava
    mm = at[S['g0'] + int((S['g1'] - S['g0']) * 0.55)]
    dS['destino'] = [round(mm['x'] + ox + ORIGEN[0] + 0.5, 2), GY + ORIGEN[1] - 1, round(mm['z'] + oz + ORIGEN[2] + 0.5, 2)]
    dS['altura'] = 5.5
    dS['ticks_turbo'] = int(dist_l / 0.62) + 2
    dS['ticks'] = int(math.hypot(dS['destino'][0] - (m0['x'] + ox + ORIGEN[0]), dS['destino'][2] - (m0['z'] + oz + ORIGEN[2])) / 0.62) + 2
    saltos.append(dS)

    # ---------- lodo: tras el salto del atajo (toda la anchura) y una mancha en una curva del principal
    def mancha_lodo(path, nom, i0, i1, ancho, lado=0):
        pts = []
        for i in range(i0, i1 + 1):
            m = path[i]; nx, nz = -m['tz'], m['tx']
            o = -ancho if lado <= 0 else 0.0
            tope = ancho if lado >= 0 else 0.0
            while o <= tope + 1e-9:
                px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
                v.set(px, sup[nom](i), pz, 'mud' if rnd.random() > 0.2 else 'brown_terracotta')
                pts.append((px, sup[nom](i), pz)); o += 0.5
        return [min(p[0] for p in pts) + ORIGEN[0], min(p[1] for p in pts) + ORIGEN[1], min(p[2] for p in pts) + ORIGEN[2],
                max(p[0] for p in pts) + ORIGEN[0], max(p[1] for p in pts) + 2 + ORIGEN[1], max(p[2] for p in pts) + ORIGEN[2]]
    z1 = mancha_lodo(at, 'a', S['tierra'] + 6, S['tierra'] + 6 + 22, MEDIO_A - 0.5)
    lodo.append(z1 + [0.45])
    i_l = idx(ms, 212, 52)
    z2 = mancha_lodo(ms, 'm', i_l, i_l + 36, MEDIO - 1.0, lado=1)        # solo la mitad derecha: se esquiva por la izquierda
    lodo.append(z2 + [0.4])
    i_l2 = idx(ms, 188, 314)
    z3 = mancha_lodo(ms, 'm', i_l2 - 10, i_l2 + 26, MEDIO - 1.0, lado=-1)
    lodo.append(z3 + [0.4])

    # ---------- Voltorb fijos: dos en el atajo (uno a cada lado, tras el barro) y uno en el principal
    def voltorb(path, nom, i, o):
        m = path[i]; nx, nz = -m['tz'], m['tx']
        px, pz = m['x'] + ox + nx * o + 0.5, m['z'] + oz + nz * o + 0.5
        voltorbs.append([round(px + ORIGEN[0], 2), sup[nom](i) + 1 + ORIGEN[1], round(pz + ORIGEN[2], 2)])
        v.set(int(px), sup[nom](i), int(pz), 'red_concrete')
    voltorb(at, 'a', S['tierra'] + 6 + 34, -1.6)
    voltorb(at, 'a', S['tierra'] + 6 + 44, 1.6)
    voltorb(ms, 'm', idx(ms, 24, 100), 0.0)

    # ---------- meta y puntos de control
    k_meta = idx(ms, 100, 40)
    s_meta = ms[k_meta]['s']
    def sup_y(i): return BASE + alt[i]
    objetivos = [(100, 40), (176, 41), (243, 100), (247, 138), (78, 200), (20, 206), (24, 172), (30, 90)]
    cps = []
    for j, (cx_, cz_) in enumerate(objetivos):
        k = idx(ms, cx_, cz_)
        m = ms[k]
        nx, nz = -m['tz'], m['tx']
        esq = []
        for a_, b_ in ((-MEDIO - 1.5, -1.5), (-MEDIO - 1.5, 1.5), (MEDIO + 1.5, -1.5), (MEDIO + 1.5, 1.5)):
            esq.append((m['x'] + ox + nx * a_ + m['tx'] * b_, m['z'] + oz + nz * a_ + m['tz'] * b_))
        x1, x2 = int(math.floor(min(e[0] for e in esq))), int(math.ceil(max(e[0] for e in esq)))
        z1_, z2_ = int(math.floor(min(e[1] for e in esq))), int(math.ceil(max(e[1] for e in esq)))
        y = sup_y(k)
        kr = (k + 8) % n; mr = ms[kr]
        yaw = math.degrees(math.atan2(-mr['tx'], mr['tz']))
        cps.append({'k': k, 'zona': [x1, y - 1, z1_, x2, y + 6, z2_], 'resp': [mr['x'] + ox + 0.5, sup_y(kr) + 1, mr['z'] + oz + 0.5, yaw]})
        col = 'red_concrete' if j == 0 else 'light_blue_concrete'
        for sgn in (-1, 1):
            px, pz = int(round(m['x'] + ox + nx * (MEDIO + 1.5) * sgn)), int(round(m['z'] + oz + nz * (MEDIO + 1.5) * sgn))
            v.fill(px, y + 1, pz, px, y + (9 if j == 0 else 7), pz, col if j else 'white_concrete')
        for o in range(-int(MEDIO + 1.5), int(MEDIO + 1.5) + 1):
            px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
            if j == 0:
                v.set(px, y + 9, pz, 'black_concrete' if (o + (px + pz)) % 2 == 0 else 'white_concrete'); v.set(px, y + 10, pz, 'white_concrete' if (o + (px + pz)) % 2 == 0 else 'black_concrete')
            else:
                v.set(px, y + 7, pz, col)
        if j == 0:
            for o in range(-int(MEDIO - 1), int(MEDIO)):
                for dd in (0, 1):
                    px, pz = int(round(m['x'] + ox + nx * o + m['tx'] * dd)), int(round(m['z'] + oz + nz * o + m['tz'] * dd))
                    v.set(px, y, pz, 'black_concrete' if (o + dd) % 2 == 0 else 'white_concrete')

    # ---------- cajas de poderes (4 hileras en el principal y una en el atajo)
    cajas = []
    def hilera(path, nom, k, offs):
        m = path[k]
        for o in offs:
            px, pz = m['x'] + ox + (-m['tz']) * o, m['z'] + oz + m['tx'] * o
            cajas.append([int(round(px)) + ORIGEN[0], sup[nom](k) + 1 + ORIGEN[1], int(round(pz)) + ORIGEN[2]])
    for (x_, z_) in ((190, 41), (244, 122), (150, 270), (70, 205), (21, 190)):
        hilera(ms, 'm', idx(ms, x_, z_), (-3, -1, 1, 3))
    hilera(at, 'a', S['turbo0'] - 24, (-2, 0, 2))
    hilera(at, 'a', S['tierra'] + 6 + 28, (-2, 0, 2))

    # ---------- parrilla de salida (detras de la meta)
    parrilla = []
    for fila in range(6):
        for col in (-1, 1):
            k = (k_meta - int((6 + fila * 3.2 + (1.6 if col > 0 else 0)) / 0.5)) % n
            m = ms[k]
            o = col * 2.2
            px, pz = m['x'] + ox + (-m['tz']) * o, m['z'] + oz + m['tx'] * o
            yaw = math.degrees(math.atan2(-m['tx'], m['tz']))
            parrilla.append([px + 0.5, sup_y(k) + 1, pz + 0.5, yaw])
            v.set(int(round(px)), sup_y(k), int(round(pz)), 'white_concrete')

    # ---------- podio dentro del circuito, junto a la salida
    mm0 = ms[k_meta]
    pxc, pzc = int(round(mm0['x'] + ox)), int(round(mm0['z'] + oz)) + 26
    v.fill(pxc - 9, GY + 1, pzc - 5, pxc + 9, GY + 1, pzc + 9, 'smooth_basalt')
    for dx, h, col in ((-3, 2, 'light_gray_concrete'), (0, 3, 'yellow_concrete'), (3, 1, 'orange_concrete')):
        v.fill(pxc + dx - 1, GY + 2, pzc - 1, pxc + dx + 1, GY + 1 + h, pzc + 1, col)
    podio = [pxc + 0.5, GY + 2, pzc + 4.5, 180]

    # ---------- volcan en el interior
    def lejos(x, z, d):
        for path in (ms, at):
            for m in path[::4]:
                if math.hypot(m['x'] + ox - x, m['z'] + oz - z) < d: return False
        return True
    VX, VZ, VR, VH = 125 + ox, 112 + oz, 44, 46
    for x in range(VX - VR, VX + VR + 1):
        for z in range(VZ - VR, VZ + VR + 1):
            r = math.hypot(x - VX, z - VZ)
            if r > VR: continue
            h = int(VH * (1 - r / VR) ** 1.35 + rnd.random() * 1.2)
            if h < 1: continue
            for y in range(GY + 1, GY + h + 1):
                f = rnd.random()
                v.set(x, y, z, 'blackstone' if f > 0.25 else ('basalt' if f > 0.06 else 'magma_block'))
            if h > 4 and rnd.random() < 0.04: v.set(x, GY + h, z, 'magma_block')
    for x in range(VX - 9, VX + 10):                        # crater con lava (el borde del cono la contiene)
        for z in range(VZ - 9, VZ + 10):
            if math.hypot(x - VX, z - VZ) <= 8.5:
                for y in range(GY + VH - 11, GY + VH + 3): v.set(x, y, z, 'air')
                v.set(x, GY + VH - 12, z, 'blackstone')
                for y in range(GY + VH - 11, GY + VH - 7): v.set(x, y, z, 'lava')
    # anillo del crater
    for x in range(VX - 12, VX + 13):
        for z in range(VZ - 12, VZ + 13):
            r = math.hypot(x - VX, z - VZ)
            if 8.5 < r <= 11.5:
                for y in range(GY + VH - 12, GY + VH - 6): v.set(x, y, z, 'blackstone')

    # ---------- estatuas Pokémon de fuego (las de Cobblemon ya vienen cacheadas en estatuas/)
    ASSETS, TEX = os.environ.get('COBBLEMON_ASSETS'), os.environ.get('MC_TEXTURAS')
    ocupado = [(pxc, pzc, 16), (VX, VZ, VR + 6)]
    def sitio_libre(x, z, r):
        if not lejos(x, z, r + 8): return False
        return all(math.hypot(x - ox_, z - oz_) > r + r_ for ox_, oz_, r_ in ocupado)
    def estatua(especie, altura, x, z, veces=0):
        bl = catalogo_estatuas.cargar(especie, altura, ASSETS, TEX)
        if veces: bl = modelo.girar(bl, veces)
        escenario.poner_estatua(v, bl, x, GY + 1, z)
        ocupado.append((x, z, max(max(abs(k[0]) for k in bl), max(abs(k[2]) for k in bl)) + 3))
        print('estatua %s en (%d, %d)' % (especie, x + ORIGEN[0], z + ORIGEN[2]))
    for especie, alt_e, (px_, pz_) in (('charizard', 34, (62, 150)), ('dragonite', 34, (60, 100)), ('charmander', 24, (200, 90)), ('lucario', 30, (196, 128))):
        x, z = px_ + ox, pz_ + oz
        if sitio_libre(x, z, 9): estatua(especie, alt_e, x, z)
        else: print('AVISO: no hay sitio para', especie)
    # ---------- decoracion: columnas de basalto y charcos de lava lejos de la pista
    for _ in range(170):
        x, z = rnd.randint(4, W - 5), rnd.randint(4, L - 5)
        if lejos(x, z, 14) and math.hypot(x - VX, z - VZ) > VR + 4:
            h = rnd.randint(3, 11)
            for y in range(GY + 1, GY + 1 + h): v.set(x, y, z, 'basalt' if rnd.random() > 0.15 else 'blackstone')
            if rnd.random() < 0.3: v.set(x, GY + h, z, 'magma_block')
    for _ in range(26):
        x, z = rnd.randint(8, W - 9), rnd.randint(8, L - 9)
        if lejos(x, z, 18) and math.hypot(x - VX, z - VZ) > VR + 6:
            r_ = rnd.randint(2, 4)
            for xx in range(x - r_, x + r_ + 1):
                for zz in range(z - r_, z + r_ + 1):
                    if math.hypot(xx - x, zz - z) <= r_:
                        v.set(xx, GY - 3, zz, 'blackstone')
                        for y in range(GY - 2, GY + 1): v.set(xx, y, zz, 'lava')

    # ---------- guardar
    caida = ORIGEN[1] + GY + 1.8      # cualquier cosa que pise el suelo (o la lava) fuera de la pista cuenta como caida
    nbt = os.path.join(destino, 'src/main/resources/data/emi_carreras/structure', ID + '.nbt')
    pal, nb = v.guardar(nbt, aire_relleno=False)
    print('plantilla', W, ALTO, L, '-', nb, 'bloques,', pal, 'estados ->', nbt)
    ver = hashlib.sha1(open(nbt, 'rb').read()).hexdigest()[:12]
    datos = {
        'version': ver, 'nombre': 'Volcán Cinnabar', 'plantilla': 'emi_carreras:' + ID, 'origen': list(ORIGEN), 'vueltas': VUELTAS,
        'musica': 'volcan', 'ambiente': 'volcan',
        'parrilla': [[round(p[0] + ORIGEN[0], 2), p[1] + ORIGEN[1], round(p[2] + ORIGEN[2], 2), round(p[3], 1)] for p in parrilla],
        'podio': [round(podio[0] + ORIGEN[0], 2), podio[1] + ORIGEN[1], round(podio[2] + ORIGEN[2], 2), podio[3]],
        'checkpoints': [[c['zona'][0] + ORIGEN[0], c['zona'][1] + ORIGEN[1], c['zona'][2] + ORIGEN[2], c['zona'][3] + ORIGEN[0], c['zona'][4] + ORIGEN[1], c['zona'][5] + ORIGEN[2],
                         round(c['resp'][0] + ORIGEN[0], 2), c['resp'][1] + ORIGEN[1], round(c['resp'][2] + ORIGEN[2], 2), round(c['resp'][3], 1)] for c in cps],
        'cajas': cajas, 'turbos': turbos, 'saltos': saltos, 'lodo': lodo, 'voltorbs': voltorbs, 'caidaY': caida,
    }
    js = os.path.join(destino, 'src/main/resources/data/emi_carreras/pistas', ID + '.json')
    os.makedirs(os.path.dirname(js), exist_ok=True)
    json.dump(datos, open(js, 'w'), indent=1)
    print('datos ->', js, '|', len(cajas), 'cajas,', len(saltos), 'saltos,', len(lodo), 'zonas de lodo,', len(voltorbs), 'Voltorb,', len(turbos), 'turbos')
    return v, datos


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else '..')
