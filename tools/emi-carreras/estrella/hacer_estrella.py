# Michiestrellita: estrella de 5 puntas con borde dorado grabado (mismo estilo que la moneda) y la carita del Michialien del dueño (michi_referencia.webp) en el centro.
# Rejilla 80x80 ampliada x8 -> 640x640. Necesita numpy, scipy y Pillow.   Uso: python3 hacer_estrella.py [referencia] [salida.png]
import math, sys, os
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage
AQUI = os.path.dirname(os.path.abspath(__file__))
SRC = sys.argv[1] if len(sys.argv) > 1 else os.path.join(AQUI, "..", "moneda", "michi_referencia.webp")
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(AQUI, "..", "src", "main", "resources", "assets", "emi_carreras", "textures", "item", "estrella_michi.png")
G = 80; UP = 8
ORO = [(88, 58, 6), (140, 94, 10), (190, 134, 18), (232, 176, 32), (255, 214, 78), (255, 240, 160)]
LILA = (233, 219, 253); LILA_S = (221, 207, 249)

# ---- silueta de la estrella (puntas redondeadas un poco): poligono de 10 vertices
cx, cy = (G - 1) / 2, (G - 1) / 2 + 2.5
Re, Ri = 38.5, 17.5
pts = []
for i in range(10):
    ang = -math.pi / 2 + i * math.pi / 5
    r = Re if i % 2 == 0 else Ri
    pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
big = 8
sil = Image.new("L", (G * big, G * big), 0)
ImageDraw.Draw(sil).polygon([(x * big, y * big) for x, y in pts], fill=255)
sil = sil.resize((G, G), Image.LANCZOS).point(lambda v: 255 if v > 127 else 0)
m = np.array(sil) > 0
dist = ndimage.distance_transform_edt(m)           # distancia al borde (en pixeles de la rejilla)
star = Image.new("RGBA", (G, G), (0, 0, 0, 0)); o = star.load()
for y in range(G):
    for x in range(G):
        if not m[y, x]: continue
        d = dist[y, x]
        luz = (-(x - cx) - (y - cy)) / (math.hypot(x - cx, y - cy) + 1e-6) * 0.5
        if d <= 1.0:   c = ORO[0]
        elif d <= 2.0: c = ORO[5] if luz > 0.1 else ORO[1]
        elif d <= 5.0:
            marca = ((x * 3 + y * 5) % 7 == 0)
            c = ORO[1] if marca else (ORO[3] if luz > -0.2 else ORO[2])
            if (x * 7 + y * 13) % 11 == 0: c = ORO[min(5, ORO.index(c) + 1)]
            if d <= 3.0 and luz > 0.35 and not marca: c = ORO[4]
        elif d <= 6.0: c = ORO[0] if luz < 0 else ORO[1]
        elif d <= 7.0: c = ORO[4] if luz < 0 else ORO[1]
        else:
            c = LILA_S if (d < 8.5 and luz > 0.3) else LILA
            if (x * 5 + y * 3) % 17 == 0: c = (228, 214, 250)
        o[x, y] = c + (255,)
N = G * UP
estrella = star.resize((N, N), Image.NEAREST)

# ---- la carita del Michialien (pixel art del dueño): se recorta la cabeza y se pega en el centro de la estrella
im = Image.open(SRC).convert("RGB"); a = np.array(im).astype(int); H = a.shape[0]
bg = np.array(LILA)
yy, xx = np.mgrid[0:H, 0:H]
mask = (np.abs(a - bg).sum(axis=2) > 36) & (np.hypot(xx - (H - 1) / 2, yy - (H - 1) / 2) < 0.405 * H)
lab, n = ndimage.label(mask)
for i, t in enumerate(ndimage.sum(mask, lab, range(1, n + 1)), 1):
    if t < 600: mask[lab == i] = False
mask = ndimage.binary_fill_holes(mask)
mask = ndimage.binary_dilation(mask, iterations=2)
rgba = np.dstack([a.astype(np.uint8), (mask * 255).astype(np.uint8)])
cat = Image.fromarray(rgba, "RGBA").crop((180, 190, 800, 640))        # antenas, orejas y cara (sin cuerpo)
ancho = int(N * 0.30)
cat = cat.resize((ancho, int(ancho * cat.height / cat.width)), Image.LANCZOS)
estrella.alpha_composite  # (silencio de linters)
px = (N - cat.width) // 2; py = int(N * 0.50 - cat.height * 0.40)
estrella.alpha_composite(cat, (px, py)) if False else None
# el gatito nunca sobresale: se recorta con la parte lila de dentro de la estrella (por dentro del borde dorado)
interior = Image.fromarray(((dist >= 8.0) * 255).astype(np.uint8), "L").resize((N, N), Image.NEAREST)
capa = Image.new("RGBA", (N, N), (0, 0, 0, 0))
capa.alpha_composite(cat, (px, py))
al = np.minimum(np.array(capa.split()[3]), np.array(interior))
capa.putalpha(Image.fromarray(al.astype(np.uint8), "L"))
lienzo = estrella.copy()
lienzo.alpha_composite(capa)
os.makedirs(os.path.dirname(OUT), exist_ok=True)
lienzo.save(OUT)
print("guardada", OUT, lienzo.size)
