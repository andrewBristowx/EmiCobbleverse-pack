"""Comprueba que cada NPC de cada edificio tiene hueco para estar de pie (2 bloques de aire) y suelo solido debajo, y que no esta dentro de un bloque.
Uso: python3 chequeo_npc.py   (construye todos los edificios en memoria)"""
import build_regiones
import construir

malo = 0
for region, lista in build_regiones.REGIONES.items():
    for ruta, mod, etiqueta, aliases in lista:
        v, npcs = mod.construir()
        for (x, y, z, yaw, cmd) in npcs:
            x2, y2, z2, movido = construir.ubicar_npc(v, x, y, z)
            if movido: print(f'  {ruta}: {cmd.split()[-1]} se mueve de ({int(x)},{y},{int(z)}) a ({int(x2)},{y2},{int(z2)})')
            ok = construir._libre(v.get(int(x2), int(y2), int(z2))) and construir._libre(v.get(int(x2), int(y2) + 1, int(z2))) and construir._firme(v.get(int(x2), int(y2) - 1, int(z2)))
            if not ok:
                malo += 1
                print(f'  {ruta}: {cmd.split()[-1]} SIN SITIO en ({int(x)},{y},{int(z)})')
print('NPC sin sitio valido:', malo)
