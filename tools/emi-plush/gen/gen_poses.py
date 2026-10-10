#!/usr/bin/env python3
"""Pose de reposo (ground_idle, en el instante 0) de cada especie Emi, sacada de las animaciones de Cobblemon, para los peluches.
Escribe poses/<especie>.json = {hueso: {"rotation": [x,y,z], "position": [x,y,z]}} con las expresiones de Molang ya evaluadas (q.anim_time = 0).
Uso: COBBLEMON_JAR=<jar> python3 gen_poses.py"""
import json, math, os, re, sys, zipfile

JAR = os.environ.get('COBBLEMON_JAR') or sys.exit('Falta COBBLEMON_JAR')
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'poses')
ESPECIES = [('happiny', '0440_happiny'), ('chansey', '0113_chansey'), ('blissey', '0242_blissey'), ('cleffa', '0173_cleffa'), ('clefairy', '0035_clefairy'), ('clefable', '0036_clefable'),
            ('igglybuff', '0174_igglybuff'), ('jigglypuff', '0039_jigglypuff'), ('wigglytuff', '0040_wigglytuff'), ('eevee', '0133_eevee'), ('vaporeon', '0134_vaporeon'),
            ('jolteon', '0135_jolteon'), ('flareon', '0136_flareon'), ('espeon', '0196_espeon'), ('umbreon', '0197_umbreon'), ('leafeon', '0470_leafeon'),
            ('glaceon', '0471_glaceon'), ('sylveon', '0700_sylveon'), ('ralts', '0280_ralts'), ('kirlia', '0281_kirlia'), ('gardevoir', '0282_gardevoir')]

class M:   # funciones de Molang (los angulos van en grados)
    pi = math.pi
    @staticmethod
    def sin(x): return math.sin(math.radians(x))
    @staticmethod
    def cos(x): return math.cos(math.radians(x))
    abs = staticmethod(abs); min = staticmethod(min); max = staticmethod(max); sqrt = staticmethod(math.sqrt)
    floor = staticmethod(math.floor); ceil = staticmethod(math.ceil); round = staticmethod(round); pow = staticmethod(math.pow)
    @staticmethod
    def clamp(x, a, b): return max(a, min(b, x))
    @staticmethod
    def lerp(a, b, t): return a + (b - a) * t
    @staticmethod
    def random(a=0, b=1): return (a + b) / 2

def ev(v):
    if isinstance(v, (int, float)): return float(v)
    if isinstance(v, str):
        e = re.sub(r'\bq\.anim_time\b', '0', v)
        e = re.sub(r'\b(?:q|v|query|variable)\.\w+', '0', e)
        e = e.replace('math.', 'M.')
        try: return float(eval(e, {'M': M, '__builtins__': {}}))
        except Exception: return 0.0
    if isinstance(v, list): return [ev(x) for x in v]
    if isinstance(v, dict):   # fotogramas clave: el primero
        k = min(v, key=lambda t: float(t) if re.match(r'^-?[\d.]+$', t) else 1e9)
        e = v[k]
        if isinstance(e, dict): e = e.get('post', e.get('pre', e.get('vector', [0, 0, 0])))
        return ev(e)
    return 0.0

z = zipfile.ZipFile(JAR)
os.makedirs(OUT, exist_ok=True)
for sp, folder in ESPECIES:
    a = json.loads(z.read(f'assets/cobblemon/bedrock/pokemon/animations/{folder}/{sp}.animation.json'))['animations'][f'animation.{sp}.ground_idle']
    pose = {}
    for bone, t in a.get('bones', {}).items():
        d = {}
        for k in ('rotation', 'position'):
            if k in t:
                val = ev(t[k])
                if isinstance(val, float): val = [val] * 3
                if any(abs(x) > 1e-6 for x in val): d[k] = [round(x, 4) for x in val]
        if d: pose[bone] = d
    json.dump(pose, open(f'{OUT}/{sp}.json', 'w'), indent=1)
    print(sp, len(pose), 'huesos con pose')
