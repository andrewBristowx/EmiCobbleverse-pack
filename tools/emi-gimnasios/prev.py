"""Previsualiza un gimnasio sin abrir el juego: python3 prev.py <modulo g_xxx> [escala] [corte_y]
Genera 4 vistas isometricas (una por esquina) y, con corte_y, otras 4 sin lo que hay por encima de esa altura (para ver el interior).
Las imagenes quedan en la carpeta que diga la variable PREV_OUT (por defecto ./prev_out). Variable MC_TEXTURAS = textures/block del client.jar."""
import sys, os, importlib
import vox
from PIL import Image

mod = importlib.import_module(sys.argv[1])
esc = int(sys.argv[2]) if len(sys.argv) > 2 else 7
corte = int(sys.argv[3]) if len(sys.argv) > 3 else None
out = os.environ.get('PREV_OUT', 'prev_out'); os.makedirs(out, exist_ok=True)
v, npcs = mod.construir()
g = {k: b for k, b in v.g.items()}
print(sys.argv[1], f'{v.w}x{v.h}x{v.l}', len(g), 'bloques;', 'NPC:', [(n[0], n[1], n[2], n[4]) for n in npcs])
def vistas(g, nombre):
    angs = (2,) if os.environ.get('PREV_UNA') else (2, 3, 0, 1)
    ims = [vox.render(((v.w, v.h, v.l), g), esc, None, ang=a) for a in angs]   # ang 2: se ve la fachada (z=0) desde el lado +x
    w = max(i.width for i in ims); h = max(i.height for i in ims)
    cols = 1 if len(ims) == 1 else 2
    hoja = Image.new('RGB', (w * cols, h * ((len(ims) + cols - 1) // cols)), (24, 18, 40))
    for k, im in enumerate(ims): hoja.paste(im, ((k % cols) * w, (k // cols) * h))
    hoja.save(f'{out}/{nombre}.png'); print(f'{out}/{nombre}.png', hoja.size)
vistas(g, sys.argv[1])
if corte is not None: vistas({k: b for k, b in g.items() if k[1] <= corte}, sys.argv[1] + '_corte')
