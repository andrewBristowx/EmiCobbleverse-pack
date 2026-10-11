#!/usr/bin/env python3
"""Detector de z-fighting: caras de dos cubos sin rotar que estan en el mismo plano, miran al mismo lado y se solapan.
uso: zfight.py <geo.json> [--min 0.05] ; devuelve lista de (hueso a, hueso b, eje, plano, area)"""
import json, sys, itertools

def cubes(geo):
    g = geo['minecraft:geometry'][0]['bones']
    by = {b['name']: b for b in g}
    def rotated(n):
        while n:
            b = by[n]
            if b.get('rotation') and any(abs(x) > 1e-6 for x in b['rotation']): return True
            n = b.get('parent')
        return False
    out = []; rot = 0
    for b in g:
        for c in b.get('cubes', []):
            if c.get('rotation') or rotated(b['name']): rot += 1; continue
            o, s = c['origin'], c['size']; inf = c.get('inflate', 0) or 0
            lo = [o[i] - inf for i in range(3)]; hi = [o[i] + s[i] + inf for i in range(3)]
            out.append((b['name'], lo, hi, s))
    return out, rot

def zf(geo, eps=0.02, minarea=0.05):
    cs, rot = cubes(geo)
    res = []
    for (na, la, ha, sa), (nb, lb, hb, sb) in itertools.combinations(cs, 2):
        if na == nb and la == lb: continue
        for ax in range(3):
            for side in (0, 1):            # 0 = cara minima (mira al -), 1 = cara maxima (mira al +)
                pa = (ha if side else la)[ax]; pb = (hb if side else lb)[ax]
                if abs(pa - pb) > eps: continue
                # planas (tamaño 0 en ese eje) cuentan igual; area solapada en los otros dos ejes
                a1, a2 = [i for i in range(3) if i != ax]
                w = min(ha[a1], hb[a1]) - max(la[a1], lb[a1]); h = min(ha[a2], hb[a2]) - max(la[a2], lb[a2])
                if w > 0.01 and h > 0.01 and w * h >= minarea:
                    # una cara plana (cubo de grosor 0) en la cara de otro es normal de Cobblemon (parpados, bocas): se cuenta igual pero se marca
                    res.append((na, nb, 'xyz'[ax] + ('+' if side else '-'), round(pa, 3), round(w * h, 2), sa[ax] == 0 or sb[ax] == 0))
    return res, rot

def conflictos(geo, cubo, sep=0.15, minarea=0.02, lista=False):
    """Cuantos otros cubos (sin rotar) comparten con `cubo` una cara coplanar (a menos de `sep`) que mire al mismo lado y se solape.
    Con lista=True devuelve los cubos en conflicto (uno por cada cubo, aunque choquen en varias caras)."""
    g = geo['minecraft:geometry'][0]['bones']
    by = {b['name']: b for b in g}
    def rotado(n):
        while n:
            b = by[n]
            if b.get('rotation') and any(abs(x) > 1e-6 for x in b['rotation']): return True
            n = b.get('parent')
        return False
    def caja(c):
        o, s = c['origin'], c['size']; inf = c.get('inflate', 0) or 0
        return [o[i] - inf for i in range(3)], [o[i] + s[i] + inf for i in range(3)]
    la, ha = caja(cubo)
    n = 0; otros = []
    for b in g:
        if rotado(b['name']): continue
        for c in b.get('cubes', []):
            if c is cubo or c.get('rotation'): continue
            lb, hb = caja(c)
            for ax in range(3):
                for lado in (0, 1):
                    if abs((ha if lado else la)[ax] - (hb if lado else lb)[ax]) >= sep: continue
                    a1, a2 = [i for i in range(3) if i != ax]
                    w = min(ha[a1], hb[a1]) - max(la[a1], lb[a1]); h = min(ha[a2], hb[a2]) - max(la[a2], lb[a2])
                    if w > 0.01 and h > 0.01 and w * h >= minarea:
                        n += 1
                        if not any(o is c for o in otros): otros.append(c)
    return otros if lista else n

if __name__ == '__main__':
    r, rot = zf(json.load(open(sys.argv[1])))
    for x in r: print(x)
    print(len(r), 'pares; cubos con rotacion (no analizados):', rot)
