"""Mapa de la pista para la interfaz del cliente: imagen de vista cenital (la pista dibujada con lineas gruesas, huecos de salto sin pista, lava, turbos, saltos y meta).

Se guarda en src/main/resources/assets/emi_carreras/textures/gui/mapa_<id>.png; la posicion de cada jugador se sitúa con (x0, z0, ancho, largo) del JSON de la pista.
"""
import os
from PIL import Image, ImageDraw

ESC = 0.8        # pixeles por bloque de la imagen final
SS = 4           # supersampling


def dibujar(ruta, W, L, trazos, lava=(), cajas=(), marcas=(), meta=None, relleno=()):
    """trazos: [(lista de (x,z) locales, ancho en bloques, color)]; lava: celdas (x,z); cajas: [(x1,z1,x2,z2,color)] (turbos, saltos); meta: (x,z,tx,tz,ancho)"""
    w, h = int(W * ESC), int(L * ESC)
    k = ESC * SS
    im = Image.new('RGBA', (w * SS, h * SS), (14, 14, 24, 255))
    d = ImageDraw.Draw(im)
    for (cx, cz, r, col) in relleno:
        d.ellipse([(cx - r) * k, (cz - r) * k, (cx + r) * k, (cz + r) * k], fill=col)
    for (x, z) in lava:
        d.rectangle([x * k, z * k, (x + 1) * k, (z + 1) * k], fill=(230, 90, 20, 255))
    for pts, ancho, col in trazos:
        borde = tuple(int(c * 0.45) for c in col[:3]) + (255,)
        for (x, z) in pts:                                   # circulos en cada punto: mas limpio que lineas gruesas con juntas
            r = (ancho + 2.2) * k / 2; d.ellipse([x * k - r, z * k - r, x * k + r, z * k + r], fill=borde)
    for pts, ancho, col in trazos:
        for (x, z) in pts:
            r = ancho * k / 2; d.ellipse([x * k - r, z * k - r, x * k + r, z * k + r], fill=col)
    for (x1, z1, x2, z2, col) in cajas:
        d.rectangle([x1 * k, z1 * k, (x2 + 1) * k, (z2 + 1) * k], fill=col)
    for (x, z, r, col) in marcas:
        d.ellipse([(x - r) * k, (z - r) * k, (x + r) * k, (z + r) * k], fill=col)
    if meta:
        x, z, tx, tz, anc = meta
        nx, nz = -tz, tx
        d.line([((x + nx * anc) * k, (z + nz * anc) * k), ((x - nx * anc) * k, (z - nz * anc) * k)], fill=(255, 255, 255, 255), width=int(5.5 * k))
        d.line([((x + nx * anc) * k, (z + nz * anc) * k), ((x - nx * anc) * k, (z - nz * anc) * k)], fill=(20, 20, 20, 255), width=int(2.4 * k))
    im = im.resize((w, h), Image.LANCZOS)
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    im.save(ruta)
    return w, h


def partir(muestras, saltar=(), paso=1):
    """trozos continuos de la polilinea (se corta en los huecos); un punto de cada `paso` muestras"""
    trozos, cur = [], []
    for i, m in enumerate(muestras):
        if i in saltar:
            if len(cur) > 1: trozos.append(cur)
            cur = []
        elif i % paso == 0 or i == len(muestras) - 1:
            cur.append((m['x'], m['z']))
    if len(cur) > 1: trozos.append(cur)
    return trozos
