#!/usr/bin/env python3
"""Genera el modelo (geo.json de GeckoLib/Bedrock) y la textura del peluche "Michi Dramatico" (Extacy).
Cada cubo tiene UV por cara (K pixeles por unidad de modelo) empaquetados en un atlas; el color de cada pixel sale de
una funcion pintar(x, y, z, normal) en coordenadas del modelo, asi que las caras siempre coinciden entre si.
Uso: python3 gen_michi.py <carpeta_resources>
"""
import json, math, os, sys
from PIL import Image

K = 6                      # pixeles de textura por unidad de modelo
ATLAS = 512
BLANCO = (255, 255, 255)
SOMBRA = (226, 224, 238)
LILA = (186, 142, 239)
LILA_OSC = (140, 96, 200)
TINTA = (52, 36, 72)
NEGRO = (58, 56, 62)
ROSA = (255, 168, 190)
ROSA_OSC = (232, 128, 160)

# (nombre, origen[x,y,z], tamano[x,y,z], pivote|None, rotacion|None, inflate, tipo)
CUBOS = [
    ("cuerpoA", (-5.0, 1.0, -4.0), (10.0, 6.0, 8.0), None, None, 0, "cuerpo"),
    ("cuerpoB", (-4.0, 0.0, -3.4), (8.0, 8.0, 6.8), None, None, 0, "cuerpo"),
    ("cuerpoC", (-4.5, 0.5, -4.4), (9.0, 7.0, 8.6), None, None, 0, "cuerpo"),
    ("orejaIa", (-5.4, 7.2, -1.3), (3.0, 1.7, 1.7), (-3.9, 7.2, -0.4), (0, 0, -9), 0, "oreja"),
    ("orejaIb", (-4.9, 8.8, -1.1), (2.0, 1.5, 1.3), (-3.9, 7.2, -0.4), (0, 0, -9), 0, "oreja"),
    ("orejaDa", (2.4, 7.2, -1.3), (3.0, 1.7, 1.7), (3.9, 7.2, -0.4), (0, 0, 9), 0, "oreja"),
    ("orejaDb", (2.9, 8.8, -1.1), (2.0, 1.5, 1.3), (3.9, 7.2, -0.4), (0, 0, 9), 0, "oreja"),
    ("antI", (-1.95, 7.4, -0.7), (0.6, 4.8, 0.6), (-1.65, 7.4, -0.4), (0, 0, -8), 0, "antena"),
    ("antD", (1.35, 7.4, -0.7), (0.6, 4.8, 0.6), (1.65, 7.4, -0.4), (0, 0, 8), 0, "antena"),
    ("bolaI", (-3.1, 11.9, -1.15), (1.7, 1.7, 1.7), (-1.65, 7.4, -0.4), (0, 0, -8), 0, "bola"),
    ("bolaD", (1.4, 11.9, -1.15), (1.7, 1.7, 1.7), (1.65, 7.4, -0.4), (0, 0, 8), 0, "bola"),
    ("brazoI", (-6.0, 2.6, -2.2), (1.6, 2.2, 2.0), (-5.0, 3.7, -1.2), (0, 0, -18), 0, "cuerpo"),
    ("brazoD", (4.4, 2.6, -2.2), (1.6, 2.2, 2.0), (5.0, 3.7, -1.2), (0, 0, 18), 0, "cuerpo"),
    ("pieI", (-4.2, 0.0, -5.3), (2.4, 1.4, 1.7), None, None, 0, "pie"),
    ("pieD", (1.8, 0.0, -5.3), (2.4, 1.4, 1.7), None, None, 0, "pie"),
    ("barriga", (-2.5, 0.15, -4.62), (5.0, 3.1, 0.24), None, None, 0, "barriga"),
    ("colaA", (1.8, 0.6, 4.0), (2.8, 2.8, 2.6), None, None, 0, "cola"),
    ("colaB", (2.3, 3.0, 4.2), (2.6, 2.8, 2.4), (3.6, 3.0, 5.4), (0, 0, -10), 0, "cola"),
    ("colaC", (3.0, 5.5, 4.4), (2.2, 2.6, 2.0), (4.1, 5.5, 5.4), (0, 0, -26), 0, "cola"),
]
PX = {}   # nombre -> {cara: (u, v, w, h)}

FACES = ["north", "south", "east", "west", "up", "down"]
NORMAL = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}

def face_dims(size, face):
    sx, sy, sz = size
    return {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}[face]

def face_point(origin, size, face, a, b):
    """a,b en [0,1] recorriendo la region UV (a hacia la derecha, b hacia abajo). Devuelve (x,y,z) local del cubo (sin rotar)."""
    ox, oy, oz = origin
    sx, sy, sz = size
    if face == "north":  return (ox + sx * (1 - a), oy + sy * (1 - b), oz)
    if face == "south":  return (ox + sx * a, oy + sy * (1 - b), oz + sz)
    if face == "east":   return (ox + sx, oy + sy * (1 - b), oz + sz * (1 - a))
    if face == "west":   return (ox, oy + sy * (1 - b), oz + sz * a)
    if face == "up":     return (ox + sx * a, oy + sy, oz + sz * b)
    if face == "down":   return (ox + sx * a, oy, oz + sz * (1 - b))

def rot_point(p, pivot, rot):
    if not rot: return p
    x, y, z = (p[0] - pivot[0], p[1] - pivot[1], p[2] - pivot[2])
    rx, ry, rz = [math.radians(r) for r in rot]
    # Bedrock: se aplica Z, luego Y, luego X (el sentido exacto da igual para pintar, solo necesitamos coherencia aproximada)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    return (x + pivot[0], y + pivot[1], z + pivot[2])

def mezcla(c1, c2, t):
    return tuple(int(c1[i] * (1 - t) + c2[i] * t) for i in range(3))

def elipse(x, y, cx, cy, rx, ry):
    return ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0

def linea(x, y, x1, y1, x2, y2, grosor):
    dx, dy = x2 - x1, y2 - y1
    l2 = dx * dx + dy * dy
    t = max(0, min(1, ((x - x1) * dx + (y - y1) * dy) / l2))
    px, py = x1 + t * dx, y1 + t * dy
    return (x - px) ** 2 + (y - py) ** 2 <= (grosor / 2) ** 2

def luna(x, y, cx, cy, r, off):
    """media luna: circulo menos otro desplazado hacia arriba-derecha"""
    return ((x - cx) ** 2 + (y - cy) ** 2 <= r * r) and not ((x - cx - off) ** 2 + (y - cy - off * 0.8) ** 2 <= (r * 0.88) ** 2)

def pintar(tipo, nombre, face, lx, ly, lz, size, origin, t):
    """(lx,ly,lz): punto en el espacio del modelo (sin rotar el cubo). t: (a,b) en la cara. Devuelve RGBA."""
    x, y, z = lx, ly, lz
    base = BLANCO
    n = NORMAL[face]
    # sombreado suave: abajo y atras un poco mas grises
    sh = 0.0
    if face == "down": sh = 0.55
    elif face in ("south",): sh = 0.25
    elif face in ("east", "west"): sh = 0.12
    base = mezcla(BLANCO, SOMBRA, sh)
    # borde del cubo ligeramente mas oscuro (da volumen)
    a, b = t
    edge = min(a, 1 - a, b, 1 - b)
    w, h = face_dims(size, face)
    edge_u = edge * min(w, h)
    if edge_u < 0.18: base = mezcla(base, SOMBRA, 0.45)

    if tipo == "oreja":
        if face == "north":
            return (mezcla(LILA, (232, 205, 250), 0.0) if edge_u > 0.28 else base) + (255,)
        return base + (255,)
    if tipo == "antena":
        return mezcla(BLANCO, SOMBRA, 0.2) + (255,)
    if tipo == "bola":
        c = LILA
        if face == "up": c = mezcla(LILA, (255, 255, 255), 0.35)
        if face == "down": c = LILA_OSC
        if edge_u < 0.2: c = mezcla(c, LILA_OSC, 0.5)
        return c + (255,)
    if tipo == "pie":
        if face == "north":
            # almohadilla principal + 3 deditos
            cx = origin[0] + size[0] / 2
            if elipse(x, y, cx, 0.55, 0.55, 0.38): return ROSA + (255,)
            for dx in (-0.62, 0.0, 0.62):
                if elipse(x, y, cx + dx, 1.05, 0.2, 0.2): return ROSA_OSC + (255,)
        return base + (255,)
    if tipo == "barriga":
        # placa negra redondeada con luna lila (solo cara frontal visible; el resto transparente-ish negro)
        cx, cy = 0.0, 1.65
        if face == "north":
            if not elipse(x, y, cx, cy, 2.45, 1.5): return (0, 0, 0, 0)
            if luna(x, y, cx, cy - 0.02, 0.95, 0.46): return LILA + (255,)
            return NEGRO + (255,)
        return NEGRO + (255,)
    if tipo == "cola":
        if edge_u < 0.18: base = mezcla(base, SOMBRA, 0.35)
        return base + (255,)
    # cuerpo
    if nombre == "cuerpoC" and face == "north":
        # cara: ojos ovalados cortados por una ceja inclinada (enfadado: la ceja baja hacia el centro)
        for sx in (-1, 1):
            ex, ey = sx * 2.25, 5.25
            if elipse(x, y, ex, ey, 0.85, 1.1):
                u = (-sx) * (x - ex)           # positivo hacia el centro
                lim = ey + 0.75 - 0.55 * (u / 0.85)
                if y <= lim:
                    if elipse(x, y, ex - sx * 0.25, ey - 0.15, 0.2, 0.28): return (235, 226, 252, 255)  # brillo
                    return TINTA + (255,)
            if linea(x, y, sx * 3.25, 6.55, sx * 1.4, 5.85, 0.36): return TINTA + (255,)
        # boquita :3
        for sx in (-1, 1):
            if elipse(x, y, sx * 0.34, 4.12, 0.36, 0.3) and y <= 4.14 and not elipse(x, y, sx * 0.34, 4.12, 0.22, 0.16): return TINTA + (255,)
        if linea(x, y, 0.0, 4.32, 0.0, 4.15, 0.12): return TINTA + (255,)
    if face == "south" and nombre == "cuerpoC":
        # X lila en el trasero
        if linea(x, y, -0.9, 3.0, 0.9, 1.6, 0.34) or linea(x, y, -0.9, 1.6, 0.9, 3.0, 0.34): return LILA + (255,)
    return base + (255,)

def main(out):
    # empaquetado: filas simples
    rects = []
    for c in CUBOS:
        for f in FACES:
            w, h = face_dims(c[2], f)
            rects.append((c[0], f, max(2, round(w * K)), max(2, round(h * K))))
    rects.sort(key=lambda r: -r[3])
    x = y = rowh = 0
    for nombre, f, w, h in rects:
        if x + w + 1 > ATLAS: x = 0; y += rowh + 1; rowh = 0
        PX.setdefault(nombre, {})[f] = (x, y, w, h)
        x += w + 1; rowh = max(rowh, h)
    if y + rowh > ATLAS: raise SystemExit("atlas demasiado pequeno")
    img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0))
    px = img.load()
    for nombre, origin, size, pivot, rot, infl, tipo in CUBOS:
        for f in FACES:
            u0, v0, w, h = PX[nombre][f]
            for j in range(h):
                for i in range(w):
                    a, b = (i + 0.5) / w, (j + 0.5) / h
                    p = face_point(origin, size, f, a, b)
                    r = pintar(tipo, nombre, f, p[0], p[1], p[2], size, origin, (a, b))
                    px[u0 + i, v0 + j] = r
    # rellenar un pixel de margen duplicando bordes (evita sangrado con filtrado)
    os.makedirs(os.path.join(out, "assets/emi_plush/textures/entity"), exist_ok=True)
    img.save(os.path.join(out, "assets/emi_plush/textures/entity/michi_dramatico.png"))
    cubes = []
    for nombre, origin, size, pivot, rot, infl, tipo in CUBOS:
        uv = {}
        for f in FACES:
            u0, v0, w, h = PX[nombre][f]
            uv[f] = {"uv": [u0, v0], "uv_size": [w, h]}
        c = {"origin": list(origin), "size": list(size), "uv": uv}
        if infl: c["inflate"] = infl
        if rot: c["pivot"] = list(pivot); c["rotation"] = list(rot)
        cubes.append(c)
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.michi_dramatico", "texture_width": ATLAS, "texture_height": ATLAS,
                        "visible_bounds_width": 4, "visible_bounds_height": 3.5, "visible_bounds_offset": [0, 0.75, 0]},
        "bones": [{"name": "bb_main", "pivot": [0, 0, 0], "cubes": cubes}]}]}
    os.makedirs(os.path.join(out, "assets/emi_plush/geo"), exist_ok=True)
    json.dump(geo, open(os.path.join(out, "assets/emi_plush/geo/michi_dramatico.geo.json"), "w"), indent=1)
    # textura de particula (16x16 blanco/gris)
    os.makedirs(os.path.join(out, "assets/emi_plush/textures/block"), exist_ok=True)
    Image.new("RGBA", (16, 16), (240, 238, 248, 255)).save(os.path.join(out, "assets/emi_plush/textures/block/michi_dramatico_particle.png"))
    print("ok", len(cubes), "cubos")

if __name__ == "__main__":
    main(sys.argv[1])
