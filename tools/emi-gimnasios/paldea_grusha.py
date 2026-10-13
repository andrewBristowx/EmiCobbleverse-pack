"""Gimnasio del Monte Glaseado (Grusha, Hielo): refugio de esqui en A con fachada acristalada, rampa de saltos, telesilla y un halfpipe de nieve dentro."""
import math
from gymlib import V
from piezas2 import *

W, H, L = 43, 38, 48
CX = 21
X0, X1 = 7, 35
Z0, Z1 = 14, 46


def abeto(v, x, z, alto):
    for k in range(alto):
        r = max(0, (alto - k) // 3 + (1 if k < alto - 2 else 0))
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if abs(dx) + abs(dz) <= r + 1 and v.get(x + dx, 3 + k, z + dz) == 'air': v.set(x + dx, 3 + k, z + dz, 'spruce_leaves')
    v.fill(x, 1, z, x, 3, z, 'spruce_log[axis=y]'); v.set(x, 3 + alto, z, 'snow_block')


def snowboard(v, x, y, z, color):
    v.fill(x, y, z, x, y + 4, z, color); v.set(x, y + 5, z, color); v.fill(x, y + 2, z + 0, x, y + 2, z, 'white_concrete')


def construir():
    v = V(W, H, L)
    v.aire(0, 0, 0, W - 1, H - 1, L - 1)
    rnd = seed(291)
    for x in range(W):
        for z in range(L):
            v.set(x, 0, z, 'snow_block' if rnd.random() < 0.9 else 'packed_ice')
            if rnd.random() < 0.2: v.set(x, 1, z, 'snow[layers=%d]' % rnd.randint(1, 3))
    for z in range(0, Z0):
        for x in range(CX - 3, CX + 4): v.set(x, 0, z, 'spruce_planks' if (x + z) % 2 else 'dark_oak_planks'); v.set(x, 1, z, 'air')
    abeto(v, 3, 4, 10); abeto(v, 40, 6, 12); abeto(v, 4, 20, 9); abeto(v, 39, 22, 11); abeto(v, 5, 36, 12); abeto(v, 38, 38, 10)
    # ---- rampa de saltos y halfpipe exterior a la derecha
    for z in range(2, 12):
        h = max(1, (12 - z) // 2)
        v.fill(30, 1, z, 38, h, z, 'snow_block')
    for x in range(30, 39): pass
    for z in range(2, 12, 3): v.fill(29, 1, z, 29, 2, z, 'spruce_fence')
    # ---- telesilla: pilones y cabinas en el lateral izquierdo
    for z in (4, 18, 32):
        v.fill(3, 1, z, 3, 10, z, 'iron_bars'); v.fill(2, 10, z, 4, 10, z, 'iron_bars')
    for (z, y) in [(10, 9), (26, 8)]:
        v.fill(3, y - 3, z, 3, y - 2, z, 'chain[axis=y]'); v.fill(2, y - 6, z - 1, 4, y - 4, z + 1, 'red_concrete'); v.fill(2, y - 5, z, 4, y - 5, z, 'glass')
    # ---- chalet en A: base de piedra, muros de troncos, gran frontón de cristal y tejado a dos aguas muy pronunciado
    v.box(X0, 1, Z0, X1, 6, Z1, 'stripped_spruce_log[axis=y]', interior=None)
    v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 6, Z1 - 1)
    v.fill(X0, 1, Z0, X1, 2, Z1, 'stone_bricks'); v.aire(X0 + 1, 2, Z0 + 1, X1 - 1, 6, Z1 - 1)
    for z in range(Z0 + 2, Z1 - 1, 5):
        for xx in (X0, X1): v.set(xx, 4, z, 'glass'); v.set(xx, 5, z, 'glass')
    cum = techo_dos_aguas(v, X0 - 2, X1 + 2, Z0 - 3, Z1 + 1, 7, 'snow_block', 'quartz', 'z')
    v.fill(X0 + 1, 6, Z0 + 1, X1 - 1, 6, Z1 - 1, 'air')
    v.fill(X0 + 1, 7, Z0 + 1, X1 - 1, 7, Z1 - 1, 'spruce_planks')
    for k in range(0, (X1 - X0 + 5) // 2 + 1):                                                              # cubierta de nieve en el tejado
        pass
    for z in range(Z0 - 3, Z1 + 2): v.set(CX, cum + 1, z, 'snow_block')
    # frontón de cristal en la fachada y el fondo
    for k in range(0, (X1 - X0 + 5) // 2 + 1):
        for zz in (Z0 - 3,):
            v.fill(CX - (X1 - X0 + 4) // 2 + k, 7 + k, zz, CX + (X1 - X0 + 4) // 2 - k, 7 + k, zz, 'glass' if 2 <= k < 11 else 'spruce_planks')
    for k in range(0, (X1 - X0 + 5) // 2 + 1):
        v.fill(CX - (X1 - X0 + 4) // 2 + k, 7 + k, Z1 + 1, CX + (X1 - X0 + 4) // 2 - k, 7 + k, Z1 + 1, 'spruce_planks')
    for k in range(2, 11):
        v.set(CX - (X1 - X0 + 4) // 2 + k, 7 + k, Z0 - 3, 'spruce_log[axis=y]'); v.set(CX + (X1 - X0 + 4) // 2 - k, 7 + k, Z0 - 3, 'spruce_log[axis=y]')
    for dx in (-8, -4, 0, 4, 8): v.fill(CX + dx, 7, Z0 - 3, CX + dx, 7 + (13 - abs(dx)), Z0 - 3, 'spruce_log[axis=y]')
    # el cuerpo bajo se prolonga hasta el frontón de cristal: porche cubierto
    v.fill(X0 - 2, 1, Z0 - 3, X1 + 2, 6, Z0 - 3, 'air')
    v.fill(X0 - 2, 1, Z0 - 3, X0 - 2, 6, Z0 - 3, 'spruce_log[axis=y]'); v.fill(X1 + 2, 1, Z0 - 3, X1 + 2, 6, Z0 - 3, 'spruce_log[axis=y]')
    hueco_puerta(v, CX, Z0, 5, 5, 'spruce_log[axis=y]', y0=2)
    for x in range(CX - 3, CX + 4): v.stairs(x, 0, Z0 - 4, 'spruce', 'north') if abs(x - CX) <= 2 else None
    for dx in (-3, 3): v.fill(CX + dx, 1, Z0 - 3, CX + dx, 4, Z0 - 3, 'spruce_fence'); v.set(CX + dx, 5, Z0 - 3, 'lantern')
    # ---- interior: suelo de madera, halfpipe de nieve al centro, chimenea y tablas de snowboard
    for z in range(Z0 + 1, Z1):
        for x in range(X0 + 1, X1): v.set(x, 1, z, 'spruce_planks' if (x + z) % 2 else 'dark_oak_planks')
    for z in range(Z0 + 6, Z1 - 12):
        for x in range(CX - 7, CX + 8):
            t = abs(x - CX) / 7
            h = int(round(t ** 2 * 5))
            v.fill(x, 1, z, x, 1 + h, z, 'snow_block') if h > 0 else v.set(x, 1, z, 'packed_ice')
    for z in range(Z0 + 6, Z1 - 12): v.set(CX - 7, 7, z, 'iron_bars'); v.set(CX + 7, 7, z, 'iron_bars')
    for z in range(Z0 + 2, Z1 - 2, 6):
        for xx in (X0 + 1, X1 - 1): v.fill(xx, 2, z, xx, 6, z, 'spruce_log[axis=y]')
        for x in range(X0 + 4, X1 - 3, 8):
            for k in range(1, 3): pass
            v.set(x, 6, z, 'lantern[hanging=true]') if v.get(x, 7, z) != 'air' else None
    for k, c in enumerate(['red_concrete', 'cyan_concrete', 'yellow_concrete', 'blue_concrete', 'magenta_concrete', 'lime_concrete']):
        snowboard(v, X0 + 2 + k * 3, 2, Z1 - 1, c)
    v.fill(X1 - 6, 2, Z0 + 10, X1 - 2, 6, Z0 + 12, 'stone_bricks'); v.aire(X1 - 5, 2, Z0 + 10, X1 - 3, 3, Z0 + 11)
    v.set(X1 - 4, 2, Z0 + 11, 'campfire[lit=true,facing=north,signal_fire=false,waterlogged=false]')
    for (x, z) in [(X0 + 3, Z0 + 4), (X0 + 3, Z0 + 8), (X1 - 3, Z1 - 12), (X0 + 3, Z1 - 12)]:
        v.set(x, 2, z, 'barrel[facing=up]'); v.set(x + 1, 2, z, 'barrel[facing=up]')
    # estrado del lider: tabla de snowboard gigante de hielo azul
    v.fill(CX - 6, 2, Z1 - 7, CX + 6, 2, Z1 - 1, 'blue_ice'); v.fill(CX - 4, 3, Z1 - 6, CX + 4, 3, Z1 - 1, 'packed_ice')
    for x in range(CX - 6, CX + 7): v.stairs(x, 2, Z1 - 8, 'quartz', 'north') if abs(x - CX) <= 3 else None
    for dx in (-5, 5): v.fill(CX + dx, 4, Z1 - 2, CX + dx, 8, Z1 - 2, 'blue_ice'); v.set(CX + dx, 9, Z1 - 2, 'sea_lantern')
    return v, [(CX, 4, Z1 - 4, 180, 'emipokemon desafios paldea hielo')]
