"""Genera la pista «Circuito Pokéball»: plantilla de estructura (.nbt) + datos (.json) para el mod emi-carreras.

Uso: python3 generar.py <carpeta tools/emi-carreras>
Escribe src/main/resources/data/emi_carreras/structure/circuito_pokeball.nbt y .../pistas/circuito_pokeball.json
"""
import sys, os, math, json, random
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'emi-gimnasios'))
from gymlib import V
from piezas2 import arbol
import camino
import escenario
import modelo
import catalogo_estatuas

ID = 'circuito_pokeball'
MARGEN = 45
BASE = 8            # y local de la superficie del asfalto en llano
ALTO = 44
MEDIO = 6.5         # semianchura de la pista incluidos los pianos
ORIGEN = (0, 64, 0)  # donde se coloca la plantilla en la dimension
VUELTAS = 3


def rotulo(v, x, y, z, texto_filas, paleta):
    v.pixelart_z(x, y, z, texto_filas, paleta)


def pokeball(v, cx, cy, cz, r, hacia=(-1, 0)):
    """esfera con forma de Pokéball: rojo arriba, blanco abajo, banda negra y boton hacia `hacia` (dx,dz)"""
    for x in range(cx - r, cx + r + 1):
        for y in range(cy - r, cy + r + 1):
            for z in range(cz - r, cz + r + 1):
                d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2)
                if d > r + 0.3: continue
                dy = y - cy
                b = 'black_concrete' if abs(dy) <= 0 else ('red_concrete' if dy > 0 else 'white_concrete')
                v.set(x, y, z, b)
    bx, bz = cx + hacia[0] * r, cz + hacia[1] * r
    v.set(bx, cy, bz, 'white_concrete')
    v.set(bx + (1 if hacia[0] == 0 else 0) * 0, cy, bz, 'white_concrete')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if (hacia[0] != 0 and dx == 0) or (hacia[1] != 0 and dz == 0): v.set(bx + dx, cy, bz + dz, 'white_concrete')
    v.set(bx + hacia[0], cy, bz + hacia[1], 'light_gray_concrete') if False else None


def main(destino):
    ms, total = camino.muestras(0.5)
    print('longitud %d bloques, radio minimo %.1f' % (total, camino.curvatura_min(ms)))
    xs = [m['x'] for m in ms]; zs = [m['z'] for m in ms]
    W = int(max(xs)) + MARGEN * 2 - int(min(xs)) + 20
    L = int(max(zs)) + MARGEN * 2 - int(min(zs)) + 20
    ox, oz = MARGEN - int(min(xs)), MARGEN - int(min(zs))        # desplazamiento a coordenadas locales
    v = V(W, ALTO, L)
    rnd = random.Random(2026)

    # ---- suelo: cesped con algo de variedad
    for x in range(W):
        for z in range(L):
            v.set(x, 0, z, 'grass_block' if rnd.random() > 0.04 else 'dirt_path')

    # ---- rasterizado de la pista: cada celda toma la muestra de eje mas cercana
    celdas = {}
    n = len(ms)
    for i, m in enumerate(ms):
        nx, nz = -m['tz'], m['tx']
        o = -MEDIO - 1.2
        while o <= MEDIO + 1.2 + 1e-9:
            px, pz = m['x'] + nx * o + ox, m['z'] + nz * o + oz
            c = (int(math.floor(px + 0.5)), int(math.floor(pz + 0.5)))
            d = abs(o)
            if c not in celdas or d < celdas[c][0]: celdas[c] = (d, i, o)
            o += 0.35
    # altura entera de cada muestra: nunca cambia mas de 1 bloque a la vez
    alt = [int(round(m['h'])) for m in ms]
    suelo_y = {}
    for c, (d, i, o) in celdas.items():
        y = BASE + alt[i]
        suelo_y[c] = y
        s = ms[i]['s']
        if d <= MEDIO - 1.0:
            b = 'gray_concrete'
            if d < 0.5 and (s % 8) < 4: b = 'white_concrete'
            elif d > MEDIO - 2.0: b = 'light_gray_concrete'
        elif d <= MEDIO:
            b = 'red_concrete' if int(s / 3) % 2 == 0 else 'white_concrete'
        else:
            b = 'stone_bricks'
        v.set(c[0], y, c[1], b)
        v.set(c[0], y - 1, c[1], 'stone_bricks')
        # cimiento hasta el suelo cada pocos bloques (pilares) para que no flote
        if y - 2 > 0 and ((c[0] % 5 == 0 and c[1] % 5 == 0) or d > MEDIO):
            v.fill(c[0], 1, c[1], c[0], y - 2, c[1], 'stone_bricks')
        if d > MEDIO:                                   # muro de contencion rojo y blanco
            roj = int(s / 4) % 2 == 0
            v.set(c[0], y + 1, c[1], 'red_concrete' if roj else 'white_concrete')
            v.set(c[0], y + 2, c[1], 'white_concrete' if roj else 'red_concrete')

    def centro(s_obj):
        """muestra (indice) mas cercana al recorrido s_obj"""
        k = min(range(n), key=lambda q: abs(ms[q]['s'] - s_obj))
        return k

    def mundo(x, y, z): return [ORIGEN[0] + x, ORIGEN[1] + y, ORIGEN[2] + z]

    # ---- meta: la recta de salida esta en el tramo x ~ 75
    k_meta = min(range(n), key=lambda q: abs(ms[q]['x'] - 75) + abs(ms[q]['z'] - 40) * 3 if ms[q]['tx'] > 0.9 else 1e9)
    s_meta = ms[k_meta]['s']

    def sup_y(i): return BASE + alt[i]

    # ---- puntos de control repartidos por el recorrido
    N_CP = 8
    cps, huecos_cp = [], []
    for j in range(N_CP):
        s_obj = (s_meta + j * total / N_CP) % total
        k = centro(s_obj)
        ok = False
        for desp in [0, 6, -6, 12, -12, 20, -20, 30, -30]:
            kk = (k + int(desp / 0.5)) % n
            if j == 0 and desp != 0: break
            m = ms[kk]
            nx, nz = -m['tz'], m['tx']
            esq = []
            for a, b in ((-MEDIO - 1.5, -1.5), (-MEDIO - 1.5, 1.5), (MEDIO + 1.5, -1.5), (MEDIO + 1.5, 1.5)):
                esq.append((m['x'] + ox + nx * a + m['tx'] * b, m['z'] + oz + nz * a + m['tz'] * b))
            x1, x2 = int(math.floor(min(e[0] for e in esq))), int(math.ceil(max(e[0] for e in esq)))
            z1, z2 = int(math.floor(min(e[1] for e in esq))), int(math.ceil(max(e[1] for e in esq)))
            # la caja no debe tocar otro tramo de pista (distancia de recorrido > 40)
            malo = False
            for q in range(0, n, 2):
                dq = abs(ms[q]['s'] - m['s']); dq = min(dq, total - dq)
                if dq > 40 and x1 - 3 <= ms[q]['x'] + ox <= x2 + 3 and z1 - 3 <= ms[q]['z'] + oz <= z2 + 3: malo = True; break
            if not malo: ok = True; break
        if not ok: print('AVISO: el punto de control', j, 'toca otro tramo'); kk = k; m = ms[kk]
        y = sup_y(kk)
        # respawn un poco mas adelante
        kr = (kk + 8) % n; mr = ms[kr]
        yaw = math.degrees(math.atan2(-mr['tx'], mr['tz']))
        cps.append({'k': kk, 'zona': [x1, y - 1, z1, x2, y + 6, z2],
                    'resp': [mr['x'] + ox + 0.5, sup_y(kr) + 1, mr['z'] + oz + 0.5, yaw]})
        # arco visual
        nx, nz = -m['tz'], m['tx']
        col = 'red_concrete' if j == 0 else 'light_blue_concrete'
        for sgn in (-1, 1):
            px, pz = int(round(m['x'] + ox + nx * (MEDIO + 1.5) * sgn)), int(round(m['z'] + oz + nz * (MEDIO + 1.5) * sgn))
            v.fill(px, y + 1, pz, px, y + (9 if j == 0 else 7), pz, col if j else 'white_concrete')
        for o in range(-int(MEDIO + 1.5), int(MEDIO + 1.5) + 1):
            px, pz = int(round(m['x'] + ox + nx * o)), int(round(m['z'] + oz + nz * o))
            if j == 0: v.set(px, y + 9, pz, 'black_concrete' if (o + (px + pz)) % 2 == 0 else 'white_concrete'); v.set(px, y + 10, pz, 'white_concrete' if (o + (px + pz)) % 2 == 0 else 'black_concrete')
            else: v.set(px, y + 7, pz, col); v.set(px, y + 6, pz, 'white_concrete') if abs(o) % 4 == 0 else None
        if j == 0:
            # Pokébolas sobre los pilares de la meta
            for sgn in (-1, 1):
                px, pz = int(round(m['x'] + ox + nx * (MEDIO + 1.5) * sgn)), int(round(m['z'] + oz + nz * (MEDIO + 1.5) * sgn))
                pokeball(v, px, y + 14, pz, 4, hacia=(-1, 0))
            # linea de meta a cuadros en el asfalto
            for o in range(-int(MEDIO - 1), int(MEDIO)):
                for dd in (0, 1):
                    px, pz = int(round(m['x'] + ox + nx * o + m['tx'] * dd)), int(round(m['z'] + oz + nz * o + m['tz'] * dd))
                    v.set(px, y, pz, 'black_concrete' if (o + dd) % 2 == 0 else 'white_concrete')

    # ---- turbos (tres franjas) y cajas de poderes (cuatro hileras de cuatro)
    turbos = []
    for fr in (0.17, 0.46, 0.80):
        k = centro((s_meta + fr * total) % total)
        m = ms[k]
        nx, nz = -m['tz'], m['tx']
        pts = []
        for a in range(0, 14):
            kk = (k + a) % n
            mm = ms[kk]
            for o in (-3, -2, -1, 0, 1, 2, 3):
                px, pz = int(round(mm['x'] + ox + (-mm['tz']) * o)), int(round(mm['z'] + oz + mm['tx'] * o))
                v.set(px, sup_y(kk), pz, 'yellow_concrete' if int(a / 2) % 2 == 0 else 'orange_concrete')
                pts.append((px, pz, sup_y(kk)))
        x1, x2 = min(p[0] for p in pts), max(p[0] for p in pts)
        z1, z2 = min(p[1] for p in pts), max(p[1] for p in pts)
        y = pts[0][2]
        turbos.append([x1, y, z1, x2, y + 2, z2])
    cajas = []
    for fr in (0.08, 0.30, 0.55, 0.78):
        k = centro((s_meta + fr * total) % total)
        m = ms[k]
        for o in (-4.5, -1.5, 1.5, 4.5):
            px, pz = m['x'] + ox + (-m['tz']) * o, m['z'] + oz + m['tx'] * o
            cajas.append([int(round(px)), sup_y(k) + 1, int(round(pz))])

    # ---- parrilla de salida: filas detras de la meta, dos columnas
    parrilla = []
    for fila in range(6):
        for col in (-1, 1):
            k = (k_meta - int((6 + fila * 3.2 + (1.6 if col > 0 else 0)) / 0.5)) % n
            m = ms[k]
            o = col * 2.6
            px, pz = m['x'] + ox + (-m['tz']) * o, m['z'] + oz + m['tx'] * o
            yaw = math.degrees(math.atan2(-m['tx'], m['tz']))
            parrilla.append([px + 0.5, sup_y(k) + 1, pz + 0.5, yaw])
            # marca de salida en el asfalto
            v.set(int(round(px)), sup_y(k), int(round(pz)), 'white_concrete')

    # ---- podio junto a la salida (dentro del circuito)
    mm = ms[k_meta]
    pxc, pzc = int(round(mm['x'] + ox)), int(round(mm['z'] + oz)) + 28
    for dx, h, col in ((-3, 2, 'light_gray_concrete'), (0, 3, 'yellow_concrete'), (3, 1, 'orange_concrete')):
        v.fill(pxc + dx - 1, 1, pzc - 1, pxc + dx + 1, h, pzc + 1, col)
    podio = [pxc + 0.5, 1, pzc + 4.5, 180]
    v.fill(pxc - 8, 0, pzc - 5, pxc + 8, 0, pzc + 8, 'smooth_stone')

    cercano = {}
    for m in ms[::4]:
        cercano[(int(m['x'] + ox) // 12, int(m['z'] + oz) // 12)] = True
    def lejos(x, z, d=16):
        for m in ms[::6]:
            if abs(m['x'] + ox - x) < d and abs(m['z'] + oz - z) < d and math.hypot(m['x'] + ox - x, m['z'] + oz - z) < d: return False
        return True

    # ---- tunel Pokéball sobre un tramo recto sin cajas, turbos ni puntos de control
    ocupados = [(s_meta + f * total) % total for f in [j / N_CP for j in range(N_CP)] + [0.08, 0.30, 0.55, 0.78, 0.17, 0.46, 0.80]]
    def dist_s(a, b): d = abs(a - b); return min(d, total - d)
    mejor = None
    LT = 36.0
    for k in range(0, n, 4):
        s0 = ms[k]['s']
        if any(dist_s(s0 + LT / 2, o) < LT / 2 + 12 for o in ocupados): continue
        k2 = (k + int(LT / 0.5)) % n
        giro = abs(math.atan2(ms[k]['tx'] * ms[k2]['tz'] - ms[k]['tz'] * ms[k2]['tx'], ms[k]['tx'] * ms[k2]['tx'] + ms[k]['tz'] * ms[k2]['tz']))
        if mejor is None or giro < mejor[0]: mejor = (giro, k, k2)
    print('tunel: giro %.2f rad entre muestras %d y %d' % mejor if mejor else 'sin sitio para el tunel')
    if mejor and mejor[0] < 1.0:
        escenario.tunel_pokeball(v, ms, ox, oz, mejor[1], mejor[2], sup_y, MEDIO)

    # ---- Centro Pokémon, Tienda, Pikachu y Snorlax dentro del circuito
    cx0, cz0 = pxc + 14, pzc - 9
    escenario.edificio(v, cx0, cz0, 18, 12, 7, 'white_concrete', 'red_concrete', 'red_concrete')
    escenario.pokeball_cartel(v, cx0 + 9, 6, cz0 - 1, 3)
    escenario.edificio(v, cx0 + 26, cz0, 14, 10, 6, 'white_concrete', 'blue_concrete', 'blue_concrete')
    ASSETS, TEX = os.environ.get('COBBLEMON_ASSETS'), os.environ.get('MC_TEXTURAS')
    ocupado = [(pxc + 20, pzc - 3, 30), (pxc - 6, pzc + 3, 14)]          # centro/tienda y podio: (x, z, radio)
    def sitio_libre(x, z, r):
        if not lejos(x, z, r + 8): return False
        return all(math.hypot(x - ox_, z - oz_) > r + r_ for ox_, oz_, r_ in ocupado)
    def estatua(especie, altura, x, z, veces=0):
        bl = catalogo_estatuas.cargar(especie, altura, ASSETS, TEX)
        if veces: bl = modelo.girar(bl, veces)
        escenario.poner_estatua(v, bl, x, 1, z)
        ocupado.append((x, z, max(max(abs(k[0]) for k in bl), max(abs(k[2]) for k in bl)) + 3))
        print('estatua %s en (%d, %d) giro %d' % (especie, x + ORIGEN[0], z + ORIGEN[2], veces))
    # las dos grandes dentro del circuito, de cara a la recta de salida (norte = -z)
    estatua('pikachu', 30, pxc - 32, pzc + 16)
    estatua('snorlax', 36, pxc + 12, pzc + 54)
    # el resto, junto al asfalto y mirando a la pista
    figuras = [('charmander', 24, 0.05), ('bulbasaur', 20, 0.12), ('squirtle', 22, 0.19), ('charizard', 34, 0.27), ('dragonite', 34, 0.34),
               ('lapras', 26, 0.45), ('eevee', 22, 0.53), ('blastoise', 30, 0.60), ('psyduck', 20, 0.67), ('jigglypuff', 22, 0.74),
               ('espeon', 24, 0.88), ('lucario', 30, 0.95)]
    for especie, alt, fr in figuras:
        k = centro((s_meta + fr * total) % total)
        m = ms[k]
        colocada = False
        for lado in (1, -1):
            for dist in (MEDIO + 17, MEDIO + 22, MEDIO + 28):
                x = int(round(m['x'] + ox + (-m['tz']) * dist * lado)); z = int(round(m['z'] + oz + m['tx'] * dist * lado))
                if sitio_libre(x, z, 9):
                    dx, dz = (m['x'] + ox) - x, (m['z'] + oz) - z       # hacia la pista
                    veces = 0 if (dz < 0 and abs(dz) >= abs(dx)) else (1 if dx > 0 and abs(dx) > abs(dz) else (2 if dz > 0 and abs(dz) >= abs(dx) else 3))
                    estatua(especie, alt, x, z, veces); colocada = True; break
            if colocada: break
        if not colocada: print('AVISO: no hay sitio para', especie)

    # ---- decoracion: arboles lejos de la pista
    for _ in range(260):
        x, z = rnd.randint(4, W - 5), rnd.randint(4, L - 5)
        if lejos(x, z): arbol(v, x, 1, z, rnd.randint(4, 6), rnd.randint(2, 3), rnd=rnd)
    for _ in range(14):
        x, z = rnd.randint(8, W - 9), rnd.randint(8, L - 9)
        if lejos(x, z, 22): pokeball(v, x, 5, z, 4, hacia=(rnd.choice([-1, 1]), 0)); v.fill(x - 1, 1, z - 1, x + 1, 1, z + 1, 'stone_bricks')

    # ---- guardar
    caida = ORIGEN[1] + 3
    nbt = os.path.join(destino, 'src/main/resources/data/emi_carreras/structure', ID + '.nbt')
    pal, nb = v.guardar(nbt, aire_relleno=False)
    print('plantilla', W, ALTO, L, '-', nb, 'bloques,', pal, 'estados ->', nbt)
    import hashlib
    ver = hashlib.sha1(open(nbt, 'rb').read()).hexdigest()[:12]
    datos = {
        'version': ver,
        'nombre': 'Circuito Pokéball', 'plantilla': 'emi_carreras:' + ID, 'origen': list(ORIGEN), 'vueltas': VUELTAS,
        'parrilla': [[round(a + ORIGEN[0] if i == 0 else a + (ORIGEN[1] if i == 1 else ORIGEN[2] if i == 2 else 0), 2) for i, a in enumerate(p)] for p in parrilla],
        'podio': [round(podio[0] + ORIGEN[0], 2), podio[1] + ORIGEN[1], round(podio[2] + ORIGEN[2], 2), podio[3]],
        'checkpoints': [[c['zona'][0] + ORIGEN[0], c['zona'][1] + ORIGEN[1], c['zona'][2] + ORIGEN[2], c['zona'][3] + ORIGEN[0], c['zona'][4] + ORIGEN[1], c['zona'][5] + ORIGEN[2],
                         round(c['resp'][0] + ORIGEN[0], 2), c['resp'][1] + ORIGEN[1], round(c['resp'][2] + ORIGEN[2], 2), round(c['resp'][3], 1)] for c in cps],
        'cajas': [[c[0] + ORIGEN[0], c[1] + ORIGEN[1], c[2] + ORIGEN[2]] for c in cajas],
        'turbos': [[t[0] + ORIGEN[0], t[1] + ORIGEN[1], t[2] + ORIGEN[2], t[3] + ORIGEN[0], t[4] + ORIGEN[1], t[5] + ORIGEN[2]] for t in turbos],
        'caidaY': caida,
    }
    js = os.path.join(destino, 'src/main/resources/data/emi_carreras/pistas', ID + '.json')
    os.makedirs(os.path.dirname(js), exist_ok=True)
    json.dump(datos, open(js, 'w'), indent=1)
    print('datos ->', js)
    return v, datos


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else '..')
