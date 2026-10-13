# Moneda del Michialien: el gato es el pixel art de michi_referencia.webp (dado por el dueño), recortado y centrado, sobre un borde dorado grabado hecho aquí (rejilla 80x80 ampliada x8 -> 640x640). Necesita numpy, scipy y Pillow.
import math, sys
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage
import os
AQUI = os.path.dirname(os.path.abspath(__file__))
SRC = sys.argv[1] if len(sys.argv) > 1 else os.path.join(AQUI, "michi_referencia.webp")
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(AQUI, "..", "src", "main", "resources", "assets", "emi_carreras", "textures", "item", "moneda_michi.png")
G = 80; UP = 8
# ------------------------------------------------ borde (80x80)
ORO = [(88, 58, 6), (140, 94, 10), (190, 134, 18), (232, 176, 32), (255, 214, 78), (255, 240, 160)]
LILA = (233, 219, 253); LILA_S = (221, 207, 249)
rim = Image.new("RGBA", (G, G), (0, 0, 0, 0)); o = rim.load()
C = (G - 1) / 2
for y in range(G):
    for x in range(G):
        dx, dy = x - C, y - C
        r = math.hypot(dx, dy)
        if r > 40.0: continue
        th = math.atan2(dy, dx)
        luz = (-dx - dy) / (r + 1e-6) * 0.5
        if r > 39.0:   c = ORO[0]
        elif r > 38.0: c = ORO[5] if luz > 0.1 else ORO[1]
        elif r > 34.0:
            v1 = math.sin(th * 18 + r * 1.1); v2 = math.sin(th * 18 - r * 1.1 + 1.7)
            marca = abs(v1) < 0.2 or abs(v2) < 0.2
            c = ORO[1] if marca else (ORO[3] if luz > -0.2 else ORO[2])
            if (x * 7 + y * 13) % 11 == 0: c = ORO[min(5, ORO.index(c) + 1)]
            if r > 37.0 and not marca and luz > 0.35: c = ORO[4]
        elif r > 33.0: c = ORO[0] if luz < 0 else ORO[1]
        elif r > 32.0: c = ORO[4] if luz < 0 else ORO[1]
        else:
            c = LILA_S if (r > 30.4 and luz > 0.3) else LILA
            if (x * 5 + y * 3) % 17 == 0 and r < 29: c = (228, 214, 250)
        o[x, y] = c + (255,)
N = G * UP
coin = rim.resize((N, N), Image.NEAREST)
# ------------------------------------------------ gato del dueno
im = Image.open(SRC).convert("RGB"); a = np.array(im).astype(int)
H = a.shape[0]
bg = np.array(LILA)
yy, xx = np.mgrid[0:H, 0:H]
mask = (np.abs(a - bg).sum(axis=2) > 36) & (np.hypot(xx - (H - 1) / 2, yy - (H - 1) / 2) < 0.405 * H)
lab, n = ndimage.label(mask)
tam = ndimage.sum(mask, lab, range(1, n + 1))
for i, t in enumerate(tam, 1):
    if t < 600: mask[lab == i] = False
mask = ndimage.binary_fill_holes(mask)
mask = ndimage.binary_dilation(mask, iterations=2)          # el halo es del color del fondo: no se nota
ys, xs = np.where(mask)
cx, cy = (xs.min() + xs.max()) / 2, (ys.min() + ys.max()) / 2
dx, dy = (H - 1) / 2 - cx, (H - 1) / 2 - cy
print("centro del gato", cx, cy, "-> se mueve", round(dx), round(dy))
rgba = np.dstack([a.astype(np.uint8), (mask * 255).astype(np.uint8)])
cat = Image.fromarray(rgba, "RGBA")
cat = cat.transform(cat.size, Image.AFFINE, (1, 0, -dx, 0, 1, -dy), resample=Image.NEAREST)
cat = cat.resize((N, N), Image.LANCZOS)
al = cat.split()[3].filter(ImageFilter.GaussianBlur(0.6))
cat.putalpha(al)
coin.alpha_composite(cat)
coin.save(OUT)
bgi = Image.new("RGBA", coin.size, (16, 16, 16, 255)); bgi.alpha_composite(coin)
pass
