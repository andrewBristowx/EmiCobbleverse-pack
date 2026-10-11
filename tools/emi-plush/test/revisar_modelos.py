#!/usr/bin/env python3
"""Revisa los modelos del paquete antes de empaquetar el jar. Falla (codigo 1) si encuentra:
  1. UV con decimales en un modelo de Pokemon: Cobblemon no carga ese modelo (`Expected an int but was 227.5`) y el Pokemon se ve con el modelo viejo o sin accesorio.
     Pasa cuando un cubo nuevo tiene un tamaño con decimales: el hueco que se reserva en la textura deja de ser entero y todos los UV siguientes se descuadran.
  2. z-fighting: caras de cubos distintos en el mismo plano, mirando al mismo lado y solapadas (el motor las dibuja alternando y se ven rayas parpadeantes)
     entre una pieza añadida por nosotros (huesos emi_*, hair_*, acc_*) y cualquier otra. Se ignoran las caras planas (parpados, bocas) y los huesos
     que Cobblemon alterna (parpado/ojo feliz, boca abierta/bostezo), que nunca se muestran a la vez.
  python3 test/revisar_modelos.py            Pokemon (Emi y GatitoAlien), margen 0.14 unidades de modelo
  python3 test/revisar_modelos.py --peluches peluches (todas las formas), margen 0.02 unidades de bloque"""
import glob, json, os, sys
AQUI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(AQUI, '..', 'gen'))
import zfight

NUEVOS = ('emi_', 'hair_', 'acc_')
ALTERNOS = ('eyelid', 'happy', 'mouth', 'frown', 'blink', 'lid')

def uv_con_decimales(geo):
    return [(b['name'], c['uv']) for b in geo['minecraft:geometry'][0]['bones'] for c in b.get('cubes', [])
            if isinstance(c.get('uv'), list) and any(abs(x - round(x)) > 1e-9 for x in c['uv'])]

def main():
    peluches = '--peluches' in sys.argv
    base = os.path.join(AQUI, '..', 'resources', 'assets')
    if peluches:
        ficheros = sorted(glob.glob(base + '/emi_plush/geo/*.json')); eps = 0.02
    else:
        m = base + '/cobblemon/bedrock/pokemon/models/'
        ficheros = sorted(glob.glob(m + '**/*_emi.geo.json', recursive=True) + glob.glob(m + 'michi_dramatico/*.geo.json')); eps = 0.14
    malos = 0
    for f in ficheros:
        geo = json.load(open(f)); nombre = os.path.basename(f)
        if not peluches:
            dec = uv_con_decimales(geo)
            if dec: malos += len(dec); print(f'{nombre}: {len(dec)} UV con decimales, p. ej. {dec[:2]}')
        pares, _ = zfight.zf(geo, eps=eps, minarea=0.02)
        pares = [p for p in pares if not p[5] and (p[0].startswith(NUEVOS) or p[1].startswith(NUEVOS))
                 and not (any(a in p[0] for a in ALTERNOS) and any(a in p[1] for a in ALTERNOS))]
        if pares: malos += len(pares); print(f'{nombre}: {len(pares)} parejas en el mismo plano, p. ej. {pares[:3]}')
    print(f'{len(ficheros)} modelos revisados, {malos} problemas')
    sys.exit(1 if malos else 0)

main()
