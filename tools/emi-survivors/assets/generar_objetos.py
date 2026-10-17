#!/usr/bin/env python3
"""Dibuja los 4 objetos del suelo de Emi Survivors (baya, iman, bomba, caramelo) en una sola imagen de 128x32 (4 dibujos de 32x32).
Se dibuja 8 veces mas grande y se reduce, para que los bordes salgan suaves. Uso: python3 generar_objetos.py [salida.png]
(La salida por defecto es ../src/main/resources/assets/emi_survivors/textures/objetos.png)"""
import math, os, sys
from PIL import Image, ImageDraw

K = 8                      # factor de supertamano
N = 32 * K

def nuevo():
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)

def p(v): return int(round(v * K))

def elipse(d, cx, cy, rx, ry, rell, borde=None, ancho=1.2):
    if borde: d.ellipse([p(cx - rx - ancho), p(cy - ry - ancho), p(cx + rx + ancho), p(cy + ry + ancho)], fill=borde)
    d.ellipse([p(cx - rx), p(cy - ry), p(cx + rx), p(cy + ry)], fill=rell)

def baya():
    im, d = nuevo()
    OSC = (74, 36, 8, 255)
    elipse(d, 16, 18.5, 11, 10.5, (255, 190, 40, 255), OSC)                 # el cuerpo
    elipse(d, 16, 21, 9.5, 8, (255, 160, 20, 255))                           # sombra de abajo
    elipse(d, 12, 14.5, 3.6, 2.6, (255, 240, 170, 255))                      # brillo
    d.line([p(16), p(8), p(17.5), p(3.5)], fill=OSC, width=p(2.4))           # el rabito
    d.line([p(16), p(8), p(17.5), p(3.5)], fill=(120, 80, 30, 255), width=p(1.2))
    d.polygon([(p(17), p(5.5)), (p(26), p(2)), (p(24.5), p(9))], fill=OSC)   # la hoja
    d.polygon([(p(18), p(5.8)), (p(24.8), p(3.4)), (p(23.8), p(8))], fill=(90, 205, 70, 255))
    return im

def iman():
    im, d = nuevo()
    NEG = (20, 20, 30, 255)
    # herradura: dos brazos verticales y un arco abajo (borde negro primero, color encima)
    def forma(col, grosor, borde):
        w = grosor
        # brazo izquierdo (rojo) y derecho (azul)
        for (x0, x1, c) in ((5.5, 5.5 + w, col[0]), (26.5 - w, 26.5, col[1])):
            d.rectangle([p(x0 - borde), p(4 - borde), p(x1 + borde), p(17 + borde)], fill=c if borde == 0 else NEG)
        # arco inferior: mitad izquierda roja y mitad derecha azul
        cx, cy, R = 16, 17, 10.5
        for lado, c in ((0, col[0]), (1, col[1])):
            for k in range(0, 91):
                a = math.radians(180 - k * 1.0 if lado == 0 else k * 1.0)
                a = math.pi - math.radians(k) if lado == 0 else math.radians(k)
                px, py = cx + math.cos(a) * (R - w / 2 + 0.2), cy + math.sin(a) * (R - w / 2 + 0.2) * 0.95
                r = w / 2 + borde
                d.ellipse([p(px - r), p(py - r), p(px + r), p(py + r)], fill=c if borde == 0 else NEG)
    forma(((255, 255, 255, 255), (255, 255, 255, 255)), 7.5, 1.3)
    forma(((224, 48, 48, 255), (48, 88, 230, 255)), 7.5, 0)
    # las puntas blancas
    d.rectangle([p(5.5), p(4), p(13), p(9)], fill=(245, 245, 250, 255))
    d.rectangle([p(19), p(4), p(26.5), p(9)], fill=(245, 245, 250, 255))
    # brillo
    d.rectangle([p(7), p(10), p(8.6), p(17)], fill=(255, 150, 150, 255))
    d.rectangle([p(23.4), p(10), p(25), p(17)], fill=(150, 180, 255, 255))
    return im

def bomba():
    im, d = nuevo()
    NEG = (15, 15, 25, 255)
    elipse(d, 15, 19.5, 11, 10.5, (48, 48, 64, 255), NEG)                    # el cuerpo
    elipse(d, 15, 21.5, 9.5, 8, (30, 30, 44, 255))
    elipse(d, 10.5, 15, 3.2, 2.4, (190, 190, 220, 255))                      # brillo
    d.rectangle([p(11.5), p(6.5), p(18.5), p(10.5)], fill=(90, 90, 110, 255), outline=NEG, width=p(0.9))   # el casquillo
    d.line([p(15), p(6.5), p(18), p(3.5), p(22), p(3)], fill=(150, 100, 40, 255), width=p(1.6))            # la mecha
    # la chispa
    cx, cy = 24, 2.8
    d.polygon([(p(cx), p(cy - 3.4)), (p(cx + 1), p(cy - 1)), (p(cx + 3.4), p(cy)), (p(cx + 1), p(cy + 1)), (p(cx), p(cy + 3.4)), (p(cx - 1), p(cy + 1)), (p(cx - 3.4), p(cy)), (p(cx - 1), p(cy - 1))], fill=(255, 220, 60, 255))
    d.ellipse([p(cx - 1.2), p(cy - 1.2), p(cx + 1.2), p(cy + 1.2)], fill=(255, 255, 220, 255))
    # una media luna amarilla (el toque Emi)
    elipse(d, 15, 20, 5.2, 5.2, (255, 220, 70, 255))
    elipse(d, 17.3, 19, 4.6, 4.6, (30, 30, 44, 255))
    return im

def caramelo():
    im, d = nuevo()
    OSC = (14, 52, 110, 255)
    # los lazos de los extremos
    d.polygon([(p(1), p(9)), (p(8.5), p(16)), (p(1), p(23))], fill=OSC)
    d.polygon([(p(31), p(9)), (p(23.5), p(16)), (p(31), p(23))], fill=OSC)
    d.polygon([(p(2.4), p(11.2)), (p(8), p(16)), (p(2.4), p(20.8))], fill=(150, 215, 255, 255))
    d.polygon([(p(29.6), p(11.2)), (p(24), p(16)), (p(29.6), p(20.8))], fill=(150, 215, 255, 255))
    # el cuerpo
    d.rounded_rectangle([p(6.5), p(7.5), p(25.5), p(24.5)], radius=p(6), fill=OSC)
    d.rounded_rectangle([p(8), p(9), p(24), p(23)], radius=p(5), fill=(40, 150, 255, 255))
    d.rounded_rectangle([p(9.5), p(10.2), p(22.5), p(14.5)], radius=p(2.5), fill=(120, 200, 255, 255))   # brillo de arriba
    # una estrellita blanca
    cx, cy, R, r = 16, 17.2, 4.6, 1.9
    pts = []
    for k in range(10):
        a = -math.pi / 2 + k * math.pi / 5
        rr = R if k % 2 == 0 else r
        pts.append((p(cx + math.cos(a) * rr), p(cy + math.sin(a) * rr)))
    d.polygon(pts, fill=(255, 255, 255, 255))
    return im

def main():
    salida = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "emi_survivors", "textures", "objetos.png")
    atlas = Image.new("RGBA", (128, 32), (0, 0, 0, 0))
    for i, f in enumerate((baya, iman, bomba, caramelo)):
        atlas.paste(f().resize((32, 32), Image.LANCZOS), (i * 32, 0))
    atlas.save(salida)
    # una vista ampliada para revisarlos a ojo (no va en el mod)
    atlas.resize((512, 128), Image.NEAREST).save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "objetos_x4.png"))
    print("guardado", salida)

if __name__ == "__main__":
    main()
