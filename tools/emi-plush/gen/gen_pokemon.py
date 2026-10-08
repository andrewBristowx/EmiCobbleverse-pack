#!/usr/bin/env python3
"""Genera lo que necesita Cobblemon (el modelo va con UV de caja y KU veces mas grande; la especie usa baseScale = ESC / KU) para el Pokemon Michi Dramatico a partir del modelo del peluche:
 - modelo (geo.json) con huesos animables y un 30% mas grande que el peluche
 - animaciones (caminar, reposo, combate, ataques fisico/especial/estado, dormir, grito)
 - textura normal y shiny (misma textura del peluche; la shiny cambia el lila por turquesa)
Se ejecuta DESPUES de gen_michi.py (lee su geo.json y su textura). Uso: python3 gen_pokemon.py <carpeta_resources>
"""
import json, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from PIL import Image

import gen_michi as gm

KU = 4                  # el modelo se dibuja KU veces mas grande (y la especie usa baseScale = ESC / KU): asi la textura tiene KU px por unidad del peluche
ESC = 1.3               # tamano final respecto al peluche
ATLAS = 512

NOMBRES = ["cuerpoA", "cuerpoB", "cuerpoC", "orejaIa", "orejaIb", "orejaDa", "orejaDb", "antI", "antD", "bolaI", "bolaD",
           "brazoI", "brazoD", "pieI", "pieD", "barriga", "colaA", "colaB", "colaC"]
HUESO = {"cuerpoA": "head", "cuerpoB": "head", "cuerpoC": "head", "barriga": "head",
         "orejaIa": "ear_left", "orejaIb": "ear_left", "orejaDa": "ear_right", "orejaDb": "ear_right",
         "antI": "antenna_left", "bolaI": "antenna_left", "antD": "antenna_right", "bolaD": "antenna_right",
         "brazoI": "arm_left", "brazoD": "arm_right", "pieI": "foot_left", "pieD": "foot_right",
         "colaA": "tail1", "colaB": "tail2", "colaC": "tail3",
         "ojoI": "eye_left", "ojoD": "eye_right", "parpI": "eyelid_left", "parpD": "eyelid_right",
         "feliI": "happy_left", "feliD": "happy_right", "boca": "mouth", "bocaA": "mouth_open", "bocaB": "mouth_yawn"}
# (hueso, padre, pivote en unidades del peluche)
HUESOS = [
    ("michi", None, (0, 0, 0)),
    ("body", "michi", (0, 0, 0)),
    ("head", "body", (0, 3.5, 0)),
    ("ear_left", "head", (-3.9, 7.2, -0.4)), ("ear_right", "head", (3.9, 7.2, -0.4)),
    ("antenna_left", "head", (-1.65, 7.4, -0.4)), ("antenna_right", "head", (1.65, 7.4, -0.4)),
    ("arm_left", "head", (-5.0, 3.7, -1.2)), ("arm_right", "head", (5.0, 3.7, -1.2)),
    ("foot_left", "body", (-3.0, 1.4, -4.4)), ("foot_right", "body", (3.0, 1.4, -4.4)),
    ("eye_left", "head", (-2.25, 5.25, -4.4)), ("eye_right", "head", (2.25, 5.25, -4.4)),
    ("eyelid_left", "head", (-2.25, 6.5, -4.4)), ("eyelid_right", "head", (2.25, 6.5, -4.4)),
    ("happy_left", "head", (-2.25, 5.25, -4.4)), ("happy_right", "head", (2.25, 5.25, -4.4)),
    ("mouth", "head", (0, 4.0, -4.4)), ("mouth_open", "head", (0, 4.0, -4.4)), ("mouth_yawn", "head", (0, 3.7, -4.4)),
    ("tail1", "body", (3.2, 1.5, 4.9)), ("tail2", "tail1", (3.6, 3.0, 5.4)), ("tail3", "tail2", (4.1, 5.5, 5.4)),
]

def sc(v): return [round(x * KU, 4) for x in v]

def caja_uv(w, h, d):
    """tamano en px (enteros hacia arriba) del bloque que ocupa un cubo con UV de caja de Bedrock"""
    import math as m
    W, H, D = (m.ceil(w), m.ceil(h), m.ceil(d))
    return 2 * D + 2 * W, H + D

def construir(out):
    """Empaqueta los cubos en un atlas con UV de caja (Cobblemon no soporta UV por cara) y pinta la textura. Devuelve {nombre: [u, v]}"""
    items = []
    for nombre, origin, size, pivot, rot, infl, tipo in gm.CUBOS:
        sx, sy, sz = [v * KU for v in size]
        bw, bh = caja_uv(sx, sy, sz)
        items.append((nombre, bw, bh))
    items.sort(key=lambda r: -r[2])
    pos = {}
    x = y = rowh = 0
    for nombre, bw, bh in items:
        if x + bw + 1 > ATLAS: x = 0; y += rowh + 1; rowh = 0
        pos[nombre] = (x, y); x += bw + 1; rowh = max(rowh, bh)
    if y + rowh > ATLAS: raise SystemExit("atlas demasiado pequeno")
    img = Image.new("RGBA", (ATLAS, ATLAS), (0, 0, 0, 0)); px = img.load()
    for nombre, origin, size, pivot, rot, infl, tipo in gm.CUBOS:
        u0, v0 = pos[nombre]
        sx, sy, sz = [v * KU for v in size]
        import math as m
        W, H, D = m.ceil(sx), m.ceil(sy), m.ceil(sz)
        # rectangulos de cada cara dentro del bloque (convencion de Bedrock)
        rect = {"up": (u0 + D, v0, W, D), "down": (u0 + D + W, v0, W, D),
                "east": (u0, v0 + D, D, H), "north": (u0 + D, v0 + D, W, H),
                "west": (u0 + D + W, v0 + D, D, H), "south": (u0 + 2 * D + W, v0 + D, W, H)}
        for f, (ru, rv, rw, rh) in rect.items():
            for j in range(rh):
                for i in range(rw):
                    a, b = (i + 0.5) / rw, (j + 0.5) / rh
                    p = gm.face_point(origin, size, f, a, b)
                    px[ru + i, rv + j] = gm.pintar(tipo, nombre, f, p[0], p[1], p[2], size, origin, (a, b))
    d = os.path.join(out, "assets/cobblemon/textures/pokemon/michi_dramatico")
    os.makedirs(d, exist_ok=True)
    img.save(os.path.join(d, "michi_dramatico.png"))
    return pos

def geo(out, pos):
    cubos_por_hueso = {}
    for nombre, origin, size, pivot, rot, infl, tipo in gm.CUBOS:
        c = {"origin": sc(origin), "size": sc(size), "uv": list(pos[nombre])}
        if rot: c["pivot"] = sc(pivot); c["rotation"] = list(rot)
        cubos_por_hueso.setdefault(HUESO[nombre], []).append(c)
    huesos = []
    for nombre, padre, piv in HUESOS:
        b = {"name": nombre, "pivot": sc(piv)}
        if padre: b["parent"] = padre
        if nombre in cubos_por_hueso: b["cubes"] = cubos_por_hueso[nombre]
        huesos.append(b)
    res = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.michi_dramatico", "texture_width": ATLAS, "texture_height": ATLAS,
                        "visible_bounds_width": 8, "visible_bounds_height": 8, "visible_bounds_offset": [0, 3, 0]},
        "bones": huesos}]}
    p = os.path.join(out, "assets/cobblemon/bedrock/pokemon/models/michi_dramatico/michi_dramatico.geo.json")
    json.dump(res, open(p, "w"), indent=1)

# ---------- animaciones ----------
def muestrear(largo, f, paso=0.05):
    n = int(round(largo / paso))
    return {("%.4f" % (i * paso)).rstrip("0").rstrip("."): [round(v, 4) for v in f(i * paso)] for i in range(n + 1)}

def anim(largo, huesos, bucle=True, sonidos=None, paso=0.05):
    """huesos: {hueso: {"rotation": f(t)->[x,y,z], "position": f(t)->[x,y,z], "scale": f(t)->[x,y,z]}}"""
    b = {}
    for h, props in huesos.items():
        b[h] = {}
        for k, f in props.items():
            kf = muestrear(largo, f, paso)
            if bucle:   # que el ultimo fotograma coincida con el primero
                kf[list(kf.keys())[-1]] = kf[list(kf.keys())[0]]
            b[h][k] = kf
    a = {"animation_length": largo, "bones": b}
    if bucle: a["loop"] = True
    if sonidos: a["sound_effects"] = sonidos
    return a

def S(t, periodo, fase=0.0): return math.sin(2 * math.pi * (t / periodo) + fase)
def Z(): return [0, 0, 0]
E = KU

D = -0.40 * E          # los parpados, ojos felices y bocas abiertas estan escondidos dentro del cuerpo; esto los saca hacia delante

def sacar(f):
    return {"position": lambda t: [0, 0, D * f(t)]}

def rampa(t, a, b, c, d):
    """0 antes de a, sube hasta 1 en b, se mantiene hasta c y baja a 0 en d"""
    if t <= a or t >= d: return 0.0
    if t < b: return (t - a) / (b - a)
    if t <= c: return 1.0
    return (d - t) / (d - c)

def animaciones():
    A = {}
    # reposo en el suelo
    P = 3.0
    A["ground_idle"] = anim(P, {
        "head": {"position": lambda t: [0, 0.18 * E * S(t, P), 0], "scale": lambda t: [1 + 0.012 * S(t, P), 1 - 0.018 * S(t, P), 1 + 0.012 * S(t, P)]},
        "antenna_left": {"rotation": lambda t: [3 * S(t, P / 2), 0, 5 * S(t, P, 0.5)]},
        "antenna_right": {"rotation": lambda t: [3 * S(t, P / 2, 1), 0, -5 * S(t, P, 0.5)]},
        "ear_left": {"rotation": lambda t: [0, 0, 3 * max(0, S(t, P * 2 / 3))]},
        "ear_right": {"rotation": lambda t: [0, 0, -3 * max(0, S(t, P * 2 / 3, 2))]},
        "tail1": {"rotation": lambda t: [0, 8 * S(t, P), 0]},
        "tail2": {"rotation": lambda t: [0, 10 * S(t, P, -0.7), 0]},
        "tail3": {"rotation": lambda t: [0, 12 * S(t, P, -1.4), 0]},
        "arm_left": {"rotation": lambda t: [0, 0, 3 * S(t, P, 1)]},
        "arm_right": {"rotation": lambda t: [0, 0, -3 * S(t, P, 1)]},
    })
    # caminar: saltitos de gatito (2 pasos por ciclo)
    P = 0.8
    hop = lambda t: abs(math.sin(math.pi * t / (P / 2)))
    A["ground_walk"] = anim(P, {
        "body": {"position": lambda t: [0, 0.9 * E * hop(t), 0], "rotation": lambda t: [-2 * hop(t), 0, 3 * S(t, P)]},
        "head": {"rotation": lambda t: [3 * S(t, P / 2, 1.2), 0, -3 * S(t, P)]},
        "foot_left": {"position": lambda t: [0, 0.7 * E * max(0, S(t, P)), -0.9 * E * S(t, P, math.pi / 2)], "rotation": lambda t: [-20 * S(t, P, math.pi / 2), 0, 0]},
        "foot_right": {"position": lambda t: [0, 0.7 * E * max(0, -S(t, P)), 0.9 * E * S(t, P, math.pi / 2)], "rotation": lambda t: [20 * S(t, P, math.pi / 2), 0, 0]},
        "arm_left": {"rotation": lambda t: [-25 * S(t, P), 0, 0]},
        "arm_right": {"rotation": lambda t: [25 * S(t, P), 0, 0]},
        "antenna_left": {"rotation": lambda t: [10 * S(t, P / 2, -0.9), 0, 6 * S(t, P)]},
        "antenna_right": {"rotation": lambda t: [10 * S(t, P / 2, -0.9), 0, -6 * S(t, P)]},
        "ear_left": {"rotation": lambda t: [0, 0, 6 * S(t, P / 2, -0.5)]},
        "ear_right": {"rotation": lambda t: [0, 0, -6 * S(t, P / 2, -0.5)]},
        "tail1": {"rotation": lambda t: [0, 16 * S(t, P), 0]},
        "tail2": {"rotation": lambda t: [0, 18 * S(t, P, -0.8), 0]},
        "tail3": {"rotation": lambda t: [0, 22 * S(t, P, -1.6), 0]},
    })
    # en combate: agachado y con las antenas tensas
    P = 2.0
    A["battle_idle"] = anim(P, {
        "body": {"rotation": lambda t: [0, 0, 2.5 * S(t, P)]},
        "head": {"rotation": lambda t: [7, 0, 0], "position": lambda t: [0, -0.5 * E + 0.15 * E * S(t, P / 2), 0.4 * E]},
        "antenna_left": {"rotation": lambda t: [0, 0, 8 + 3 * S(t, 0.5)]},
        "antenna_right": {"rotation": lambda t: [0, 0, -8 - 3 * S(t, 0.5, 1)]},
        "ear_left": {"rotation": lambda t: [0, 0, 6]}, "ear_right": {"rotation": lambda t: [0, 0, -6]},
        "tail1": {"rotation": lambda t: [0, 14 * S(t, P / 2), 0]},
        "tail2": {"rotation": lambda t: [0, 16 * S(t, P / 2, -0.8), 0]},
        "tail3": {"rotation": lambda t: [0, 20 * S(t, P / 2, -1.6), 0]},
        "arm_left": {"rotation": lambda t: [-12, 0, 0]}, "arm_right": {"rotation": lambda t: [-12, 0, 0]},
    })
    # dormir: hecho una bolita
    P = 4.0
    A["sleep"] = anim(P, {
        "head": {"position": lambda t: [0, -1.1 * E, 0.5 * E], "rotation": lambda t: [28, 0, 7], "scale": lambda t: [1.02 + 0.015 * S(t, P), 0.9 + 0.025 * S(t, P), 1.02]},
        "antenna_left": {"rotation": lambda t: [0, 0, 28]}, "antenna_right": {"rotation": lambda t: [0, 0, -28]},
        "ear_left": {"rotation": lambda t: [0, 0, 14]}, "ear_right": {"rotation": lambda t: [0, 0, -14]},
        "foot_left": {"position": lambda t: [0, 0, 0.5 * E]}, "foot_right": {"position": lambda t: [0, 0, 0.5 * E]},
        "eyelid_left": sacar(lambda t: 1), "eyelid_right": sacar(lambda t: 1),
        "tail1": {"rotation": lambda t: [0, -30, 0]}, "tail2": {"rotation": lambda t: [0, -30, 0]}, "tail3": {"rotation": lambda t: [0, -25, 0]},
    })
    # grito de gatito (el sonido se lanza a los 0.1 s)
    A["cry"] = anim(1.2, {
        "head": {"rotation": lambda t: [-14 * math.sin(math.pi * min(1, t / 1.0)) ** 0.7, 0, 0],
                 "position": lambda t: [0, 0.5 * E * math.sin(math.pi * min(1, t / 1.0)), 0],
                 "scale": lambda t: [1 + 0.04 * math.sin(math.pi * min(1, t / 0.5)), 1 - 0.05 * math.sin(math.pi * min(1, t / 0.5)), 1]},
        "antenna_left": {"rotation": lambda t: [0, 0, 12 * S(t, 0.3)]}, "antenna_right": {"rotation": lambda t: [0, 0, -12 * S(t, 0.3)]},
        "arm_left": {"rotation": lambda t: [-35 * math.sin(math.pi * min(1, t / 1.0)), 0, 0]},
        "arm_right": {"rotation": lambda t: [-35 * math.sin(math.pi * min(1, t / 1.0)), 0, 0]},
        "tail1": {"rotation": lambda t: [0, 10 * S(t, 0.6), 0]},
        "mouth_open": sacar(lambda t: rampa(t, 0.05, 0.12, 0.8, 0.95)),
    }, bucle=False, sonidos={"0.1": {"effect": "emi_plush:pokemon.michi_dramatico.cry"}})
    # ataque fisico: se agazapa, salta hacia delante y arana
    L = 0.9
    def pulso(t, a, b): return max(0.0, min(1.0, (t - a) / (b - a)))
    def lunge(t):   # 0 -> -1 (atras) -> 1 (adelante) -> 0
        if t < 0.25: return -pulso(t, 0, 0.25)
        if t < 0.4: return -1 + 2 * pulso(t, 0.25, 0.4)
        return 1 - pulso(t, 0.45, 0.9)
    A["physical"] = anim(L, {
        "body": {"position": lambda t: [0, 0.6 * E * math.sin(math.pi * pulso(t, 0.25, 0.55)), 2.2 * E * lunge(t)]},
        "head": {"rotation": lambda t: [-12 * max(0, -lunge(t)) + 20 * max(0, lunge(t)), 0, 0]},
        "arm_left": {"rotation": lambda t: [-40 * max(0, -lunge(t)) + 85 * max(0, lunge(t)), 0, 0]},
        "arm_right": {"rotation": lambda t: [-40 * max(0, -lunge(t)) + 85 * max(0, lunge(t)), 0, 0]},
        "antenna_left": {"rotation": lambda t: [-15 * max(0, lunge(t)), 0, 0]}, "antenna_right": {"rotation": lambda t: [-15 * max(0, lunge(t)), 0, 0]},
        "tail1": {"rotation": lambda t: [0, -25 * max(0, lunge(t)), 0]},
        "mouth_open": sacar(lambda t: rampa(t, 0.2, 0.28, 0.6, 0.8)),
    }, bucle=False)
    # ataque especial: salta y las antenas lanzan la energia
    L = 1.2
    up = lambda t: math.sin(math.pi * pulso(t, 0.2, 0.9))
    A["special"] = anim(L, {
        "body": {"position": lambda t: [0, 1.6 * E * up(t), 0]},
        "head": {"rotation": lambda t: [-16 * up(t), 0, 0]},
        "antenna_left": {"rotation": lambda t: [-20 * up(t), 0, 30 * up(t) + 10 * S(t, 0.2) * up(t)]},
        "antenna_right": {"rotation": lambda t: [-20 * up(t), 0, -30 * up(t) - 10 * S(t, 0.2) * up(t)]},
        "arm_left": {"rotation": lambda t: [-110 * up(t), 0, 20 * up(t)]}, "arm_right": {"rotation": lambda t: [-110 * up(t), 0, -20 * up(t)]},
        "tail1": {"rotation": lambda t: [-20 * up(t), 0, 0]},
        "ear_left": {"rotation": lambda t: [0, 0, -12 * up(t)]}, "ear_right": {"rotation": lambda t: [0, 0, 12 * up(t)]},
        "happy_left": sacar(lambda t: rampa(t, 0.15, 0.25, 0.95, 1.1)), "happy_right": sacar(lambda t: rampa(t, 0.15, 0.25, 0.95, 1.1)),
        "mouth_open": sacar(lambda t: rampa(t, 0.2, 0.3, 0.9, 1.05)),
    }, bucle=False)
    # movimiento de estado: meneo y saltito
    L = 1.0
    A["status"] = anim(L, {
        "body": {"position": lambda t: [0, 0.9 * E * abs(math.sin(math.pi * 2 * t / L)) * math.sin(math.pi * t / L), 0]},
        "head": {"rotation": lambda t: [0, 0, 14 * S(t, 0.5) * math.sin(math.pi * t / L)]},
        "antenna_left": {"rotation": lambda t: [0, 0, 14 * S(t, 0.25) * math.sin(math.pi * t / L)]},
        "antenna_right": {"rotation": lambda t: [0, 0, -14 * S(t, 0.25) * math.sin(math.pi * t / L)]},
        "tail1": {"rotation": lambda t: [0, 25 * S(t, 0.5) * math.sin(math.pi * t / L), 0]},
        "happy_left": sacar(lambda t: rampa(t, 0.05, 0.15, 0.85, 1.0)), "happy_right": sacar(lambda t: rampa(t, 0.05, 0.15, 0.85, 1.0)),
    }, bucle=False)
    # ---- gestos sueltos (los usa el poser como "quirks": se disparan solos de vez en cuando) ----
    A["blink"] = anim(0.2, {"eyelid_left": sacar(lambda t: rampa(t, 0.0, 0.04, 0.12, 0.2)), "eyelid_right": sacar(lambda t: rampa(t, 0.0, 0.04, 0.12, 0.2))}, bucle=False, paso=0.02)
    A["quirk_ear_left"] = anim(0.5, {"ear_left": {"rotation": lambda t: [0, 0, 14 * math.sin(math.pi * 4 * t) * (1 - t / 0.5)]}}, bucle=False, paso=0.025)
    A["quirk_ear_right"] = anim(0.5, {"ear_right": {"rotation": lambda t: [0, 0, -14 * math.sin(math.pi * 4 * t) * (1 - t / 0.5)]}}, bucle=False, paso=0.025)
    A["quirk_tail"] = anim(1.4, {
        "tail1": {"rotation": lambda t: [0, 35 * math.sin(math.pi * 3 * t) * math.sin(math.pi * t / 1.4), 0]},
        "tail2": {"rotation": lambda t: [0, 40 * math.sin(math.pi * 3 * t - 0.8) * math.sin(math.pi * t / 1.4), 0]},
        "tail3": {"rotation": lambda t: [0, 45 * math.sin(math.pi * 3 * t - 1.6) * math.sin(math.pi * t / 1.4), 0]}}, bucle=False)
    # el bostezo dramatico (su habilidad es Drama): cabeza hacia atras, ojos cerrados y bocaza
    y_ = lambda t: rampa(t, 0.25, 0.7, 2.0, 2.5)
    A["quirk_yawn"] = anim(2.8, {
        "head": {"rotation": lambda t: [-16 * y_(t), 0, 0], "position": lambda t: [0, 0.6 * E * y_(t), 0], "scale": lambda t: [1 + 0.05 * y_(t), 1 + 0.04 * y_(t), 1 + 0.05 * y_(t)]},
        "eyelid_left": sacar(lambda t: rampa(t, 0.3, 0.6, 2.0, 2.4)), "eyelid_right": sacar(lambda t: rampa(t, 0.3, 0.6, 2.0, 2.4)),
        "mouth_yawn": sacar(lambda t: rampa(t, 0.35, 0.7, 1.9, 2.3)),
        "antenna_left": {"rotation": lambda t: [0, 0, 20 * y_(t)]}, "antenna_right": {"rotation": lambda t: [0, 0, -20 * y_(t)]},
        "arm_left": {"rotation": lambda t: [-70 * y_(t), 0, 0]}, "arm_right": {"rotation": lambda t: [-70 * y_(t), 0, 0]},
        "tail1": {"rotation": lambda t: [-12 * y_(t), 0, 0]},
    }, bucle=False)
    for a in A.values():       # escrito con "positivo = hacia fuera" en el lado izquierdo; el motor lo da al reves
        for h in ("antenna_left", "antenna_right", "ear_left", "ear_right", "arm_left", "arm_right"):
            r = a["bones"].get(h, {}).get("rotation")
            if r:
                for k in r: r[k][2] = -r[k][2] if r[k][2] else 0
    dbg = os.environ.get("MICHI_DEBUG")      # solo para pruebas: repite esa animacion en bucle
    if dbg and dbg in A: A[dbg]["loop"] = True
    return {"format_version": "1.8.0", "animations": {"animation.michi_dramatico." + k: v for k, v in A.items()}}

def poser():
    n = "michi_dramatico"
    dbg = os.environ.get("MICHI_DEBUG")
    q = lambda a: "q.bedrock('%s', '%s')" % (n, a)
    blink = "q.bedrock_quirk('%s', 'blink')" % n
    orejas = "q.bedrock_quirk('%s', q.array('quirk_ear_left', 'quirk_ear_right'), 6, 20, 1)" % n
    cola = "q.bedrock_quirk('%s', 'quirk_tail', 10, 25, 1)" % n
    bostezo = "q.bedrock_quirk('%s', 'quirk_yawn', 25, 60, 1)" % n
    return {
        "portraitScale": float(os.environ.get("MICHI_PS", 0.46)), "portraitTranslation": [float(os.environ.get("MICHI_PX", 0)), float(os.environ.get("MICHI_PY", 0.62)), 0],
        "profileScale": float(os.environ.get("MICHI_FS", 0.34)), "profileTranslation": [float(os.environ.get("MICHI_FX", 0.25)), float(os.environ.get("MICHI_FY", 1.0)), 0],
        "rootBone": "michi",
        "animations": {
            "cry": "q.bedrock_stateful('%s', 'cry')" % n,
            "physical": "q.bedrock_primary('%s', 'physical', q.curve('symmetrical_wide'))" % n,
            "special": "q.bedrock_primary('%s', 'special', q.curve('symmetrical_wide'))" % n,
            "status": "q.bedrock_primary('%s', 'status', q.curve('symmetrical_wide'))" % n,
        },
        "poses": {
            "battle-standing": {"poseTypes": ["STAND"], "isBattle": True, "animations": ["q.look('head')", q("battle_idle")], "quirks": [blink, orejas]},
            "standing": {"poseTypes": ["STAND", "FLOAT", "NONE", "PORTRAIT", "PROFILE"], "isBattle": False, "animations": ["q.look('head')", q(dbg or "ground_idle")], "quirks": [blink, orejas, cola, bostezo]},
            "walking": {"poseTypes": ["WALK", "SWIM"], "animations": ["q.look('head')", q("ground_walk")], "quirks": [blink]},
            "sleep": {"poseTypes": ["SLEEP"], "animations": [q("sleep")]},
        }}

def resolver():
    return {"species": "emi_plush:michi_dramatico", "order": 0, "variations": [
        {"aspects": [], "poser": "cobblemon:michi_dramatico", "model": "cobblemon:michi_dramatico.geo",
         "texture": "cobblemon:textures/pokemon/michi_dramatico/michi_dramatico.png", "layers": []},
        {"aspects": ["shiny"], "texture": "cobblemon:textures/pokemon/michi_dramatico/michi_dramatico_shiny.png"}]}

def texturas(out):
    d = os.path.join(out, "assets/cobblemon/textures/pokemon/michi_dramatico")
    src = Image.open(os.path.join(d, "michi_dramatico.png")).convert("RGBA")
    # shiny: lila -> turquesa
    px = src.load(); w, h = src.size
    sh = src.copy(); sp = sh.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a and b > 200 and r < 235 and g < 170 and b - g > 40:
                sp[x, y] = (int(g * 0.55), min(255, int(b * 0.95)), min(255, int(b * 0.85)), a)
            elif a and b > 150 and 100 <= r < 200 and g < 130 and b - g > 50:
                sp[x, y] = (int(r * 0.35), int(b * 0.7), int(b * 0.65), a)
    sh.save(os.path.join(d, "michi_dramatico_shiny.png"))

def main(out):
    base = os.path.join(out, "assets/cobblemon/bedrock/pokemon")
    pos = construir(out)
    geo(out, pos)
    os.makedirs(base + "/animations/michi_dramatico", exist_ok=True)
    json.dump(animaciones(), open(base + "/animations/michi_dramatico/michi_dramatico.animation.json", "w"))
    os.makedirs(base + "/posers/michi_dramatico", exist_ok=True)
    json.dump(poser(), open(base + "/posers/michi_dramatico/michi_dramatico.json", "w"))
    os.makedirs(base + "/resolvers/michi_dramatico", exist_ok=True)
    json.dump(resolver(), open(base + "/resolvers/michi_dramatico/0_michi_dramatico_base.json", "w"), indent=2)
    texturas(out)
    print("pokemon ok")

if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    main(sys.argv[1])
