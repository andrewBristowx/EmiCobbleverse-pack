"""Piezas para los gimnasios de Kalos, Galar, Paldea y Alola (ademas de piezas.py y gymlib.py)."""
import math
from gymlib import V
from piezas import *

HOJAS = {'roble': 'oak_leaves', 'abedul': 'birch_leaves', 'selva': 'jungle_leaves', 'cerezo': 'cherry_leaves', 'azalea': 'azalea_leaves', 'florida': 'flowering_azalea_leaves'}


def elipse_xy(v, cx, cy, z, rx, ry, f):
    """rellena una elipse en el plano XY (pared) en z; f(d, ang) -> bloque o None, con d = distancia normalizada 0..1"""
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            d = math.hypot((x - cx) / rx, (y - cy) / ry)
            if d <= 1.0:
                b = f(d, math.atan2(y - cy, x - cx))
                if b: v.set(x, y, z, b)


def elipse_xz(v, cx, y, cz, rx, rz, f):
    """igual que elipse_xy pero en el plano del suelo (z horizontal)"""
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            d = math.hypot((x - cx) / rx, (z - cz) / rz)
            if d <= 1.0:
                b = f(d, math.atan2(z - cz, x - cx))
                if b: v.set(x, y, z, b)


def disco_f(v, cx, y, cz, r, f):
    """disco en el suelo; f(d, ang) con d = distancia en bloques"""
    ri = int(math.ceil(r))
    for x in range(cx - ri - 1, cx + ri + 2):
        for z in range(cz - ri - 1, cz + ri + 2):
            d = math.hypot(x - cx, z - cz)
            if d <= r + 0.35:
                b = f(d, math.atan2(z - cz, x - cx))
                if b: v.set(x, y, z, b)


def anillos(radios_bloques):
    """f para disco_f: lista [(dmax, bloque), ...] ordenada; el primero cuyo dmax >= d"""
    def f(d, a):
        for dm, b in radios_bloques:
            if d <= dm: return b
        return None
    return f


def tronco(v, x, y0, y1, z, bloque='oak_log[axis=y]', grosor=1):
    if grosor == 1: v.fill(x, y0, z, x, y1, z, bloque)
    else: v.fill(x, y0, z, x + 1, y1, z + 1, bloque)


def copa(v, cx, cy, cz, rx, ry, rz, hoja, rnd=None, hueco=0.0):
    """copa de arbol (elipsoide de hojas); solo rellena aire"""
    for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
        for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
            for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
                d = math.sqrt(((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2)
                if hueco < d <= 1.0 and not (rnd and d > 0.8 and rnd.random() < 0.3) and v.get(x, y, z) == 'air':
                    v.set(x, y, z, hoja)


def arbol(v, x, y, z, alto, radio, hoja='oak_leaves', log='oak_log[axis=y]', rnd=None):
    v.fill(x, y, z, x, y + alto - 1, z, log)
    copa(v, x, y + alto - 1, z, radio, max(2, radio * 0.75), radio, hoja, rnd)


def seta(v, x, y, z, alto, r, tallo='mushroom_stem', sombrero='red_mushroom_block', motas='white_mushroom_block', rnd=None):
    """seta gigante: tallo cuadrado de 3x3 (o 1x1 si r<3) y sombrero en media esfera con motas"""
    t = 1 if r >= 3 else 0
    v.fill(x - t, y, z - t, x + t, y + alto - 1, z + t, tallo)
    cy = y + alto - 1
    for xx in range(x - r, x + r + 1):
        for yy in range(cy, cy + r + 1):
            for zz in range(z - r, z + r + 1):
                d = math.sqrt((xx - x) ** 2 + (zz - z) ** 2 + (yy - cy) ** 2 * (r / max(1, r * 0.75)) ** 2 / 1.0)
                if d <= r + 0.3 and d > r - 1.3:
                    b = sombrero
                    if motas and rnd and rnd.random() < 0.16: b = motas
                    v.set(xx, yy, zz, b)


def cono(v, cx, y0, cz, r, h, bloque, esc=None):
    """cono macizo escalonado (techo de torre) de radio r y altura h; si esc se pone una pieza de escalera/losa en el borde"""
    for k in range(h):
        rr = r * (1 - k / h)
        if rr < 0.4: rr = 0.4
        for x in range(int(cx - rr) - 1, int(cx + rr) + 2):
            for z in range(int(cz - rr) - 1, int(cz + rr) + 2):
                if math.hypot(x - cx, z - cz) <= rr + 0.2: v.set(x, y0 + k, z, bloque)


def torre_redonda(v, cx, cz, r, y0, y1, muro, techo=None, alto_techo=None, ventanas=None, banda=None):
    """torre cilindrica hueca con techo conico; ventanas = bloque de ventana cada 90 grados"""
    v.cilindro(cx, y0, y1, cz, r, muro)
    v.disco(cx, y0, cz, r, muro)
    if banda: v.anillo_circ(cx, y1 - 1, cz, r, banda, 1.0)
    if ventanas:
        for yy in range(y0 + 3, y1 - 1, 5):
            for (dx, dz) in ((r, 0), (-r, 0), (0, r), (0, -r)):
                v.set(cx + dx, yy, cz + dz, ventanas); v.set(cx + dx, yy + 1, cz + dz, ventanas)
    if techo:
        h = alto_techo or r * 2
        cono(v, cx, y1 + 1, cz, r + 1, h, techo)


def fila_columnas(v, x0, x1, z, y0, y1, paso, fuste, base=None, capitel=None):
    for x in range(x0, x1 + 1, paso):
        v.fill(x, y0, z, x, y1, z, fuste)
        if base: v.set(x, y0, z, base)
        if capitel: v.set(x, y1, z, capitel)


def podio(v, cx, z, y, ancho, prof, mat, esc, pisos=2):
    """podio escalonado de `pisos` alturas centrado en cx, con frente hacia -z (hacia la entrada); el NPC se pone en y+pisos"""
    for k in range(pisos):
        a = ancho - 2 * k
        v.fill(cx - a, y + k, z + k, cx + a, y + k, z + prof - k, mat)
        for x in range(cx - a, cx + a + 1): v.stairs(x, y + k, z + k - 1, esc, 'north')


def estandarte(v, x, y, z, alto, color, facing='north'):
    """estandarte (banner de pared) colgado: tira de lana/terracota del color de la paleta"""
    v.fill(x, y, z, x, y + alto - 1, z, color)


def cartel_pixel(v, x0, y0, z, filas, paleta, espejo=False):
    """pixel art vertical; con espejo=True se dibuja la mitad izquierda y se espeja a la derecha (la ultima columna es el centro)"""
    n = len(filas)
    for j, fila in enumerate(filas):
        for i, ch in enumerate(fila):
            if ch == ' ' or ch not in paleta: continue
            y = y0 + n - 1 - j
            v.set(x0 + i, y, z, paleta[ch])
            if espejo: v.set(x0 + 2 * (len(fila) - 1) - i, y, z, paleta[ch])


def hueco_puerta(v, cx, z, ancho, alto, marco=None, y0=1, prof=1):
    """abre un vano (aire) de ancho impar en la pared z (grosor prof) y pone marco"""
    h = ancho // 2
    v.aire(cx - h, y0, z, cx + h, y0 + alto - 1, z + prof - 1)
    if marco:
        for y in range(y0, y0 + alto):
            for zz in range(z, z + prof): v.set(cx - h - 1, y, zz, marco); v.set(cx + h + 1, y, zz, marco)
        for zz in range(z, z + prof): v.fill(cx - h - 1, y0 + alto, zz, cx + h + 1, y0 + alto, zz, marco)


def luz_pared(v, x, y, z, bloque='lantern'):
    v.set(x, y, z, bloque)


def foco_pie(v, x, y, z, luz='sea_lantern', pata='iron_bars', marco='white_concrete'):
    """foco de estudio: pie de barras y pantalla luminosa"""
    v.fill(x, y, z, x, y + 2, z, pata); v.set(x, y + 3, z, marco); v.set(x, y + 4, z, luz)


def gradas_anillo(v, cx, cz, r0, filas, base, esc, y0=1, hueco=None):
    """gradas concentricas que suben hacia fuera (el frente de cada escalon mira al centro): fila k en el radio r0+k y la altura y0+k"""
    for k in range(filas):
        r = r0 + k; y = y0 + k
        for x in range(int(cx - r) - 2, int(cx + r) + 3):
            for z in range(int(cz - r) - 2, int(cz + r) + 3):
                d = math.hypot(x - cx, z - cz)
                if r - 1.0 < d <= r + 0.3 and not (hueco and hueco(x, z)):
                    if y > y0: v.fill(x, y0, z, x, y - 1, z, base)
                    dx, dz = x - cx, z - cz
                    f = ('west' if dx > 0 else 'east') if abs(dx) >= abs(dz) else ('north' if dz > 0 else 'south')   # el frente mira al centro
                    v.stairs(x, y, z, esc, f)


def gradas_rect(v, x0, x1, z0, filas, base, esc, y0=1, lado='norte'):
    """gradas rectas a lo largo de x (de x0 a x1) que suben alejandose de z0: lado 'norte' = suben hacia -z, 'sur' = hacia +z"""
    for k in range(filas):
        z = z0 + (k if lado == 'sur' else -k); y = y0 + k
        for x in range(x0, x1 + 1):
            if y > y0: v.fill(x, y0, z, x, y - 1, z, base)
            v.stairs(x, y, z, esc, 'north' if lado == 'sur' else 'south')


def foco_torre(v, x, z, alto, mat='iron_bars', luz='sea_lantern', base=None):
    """torre de focos de estadio: poste de barras con panel de luces arriba"""
    v.fill(x, 1, z, x, alto, z, mat)
    for dx in (-1, 0, 1): v.set(x + dx, alto + 1, z, luz); v.set(x + dx, alto + 2, z, luz)
    if base: v.fill(x - 1, 1, z - 1, x + 1, 2, z + 1, base)


def poligono_xy(v, pts, z, bloque, y_min=-999, y_max=999):
    """rellena un poligono (lista de (x, y)) en el plano XY a la profundidad z; bloque puede ser una funcion f(x, y) -> bloque o None"""
    xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
    for x in range(int(min(xs)), int(max(xs)) + 1):
        for y in range(int(min(ys)), int(max(ys)) + 1):
            if y < y_min or y > y_max: continue
            c = False; px, py = x + 0.5, y + 0.5
            for (x1, y1), (x2, y2) in zip(pts, pts[1:] + pts[:1]):
                if (y1 > py) != (y2 > py) and px < (x2 - x1) * (py - y1) / (y2 - y1) + x1: c = not c
            if c:
                b = bloque(x, y) if callable(bloque) else bloque
                if b: v.set(x, y, z, b)


def poligono_xz(v, pts, y, bloque):
    """igual que poligono_xy pero en el plano del suelo (x, z) a la altura y"""
    xs = [p[0] for p in pts]; zs = [p[1] for p in pts]
    for x in range(int(min(xs)), int(max(xs)) + 1):
        for z in range(int(min(zs)), int(max(zs)) + 1):
            c = False; px, pz = x + 0.5, z + 0.5
            for (x1, z1), (x2, z2) in zip(pts, pts[1:] + pts[:1]):
                if (z1 > pz) != (z2 > pz) and px < (x2 - x1) * (pz - z1) / (z2 - z1) + x1: c = not c
            if c:
                b = bloque(x, z) if callable(bloque) else bloque
                if b: v.set(x, y, z, b)


def estrella(cx, cz, R, r=None, giro=-math.pi / 2):
    """vertices de una estrella de cinco puntas (radio exterior R, interior r) para poligono_xz / poligono_xy"""
    r = r or R * 0.42
    return [(cx + math.cos(giro + k * math.pi / 5) * (R if k % 2 == 0 else r), cz + math.sin(giro + k * math.pi / 5) * (R if k % 2 == 0 else r)) for k in range(10)]
