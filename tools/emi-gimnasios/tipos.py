from gym_build import *

EMB_RAYO = [
    "    ###  ",
    "   ###   ",
    "  ###### ",
    "    ###  ",
    "   ###   ",
    "  ##     ",
]

def props_rayo(v, T):
    """Bobinas Tesla en las esquinas de la arena, franjas amarillas y negras, cables de cobre."""
    for (x, z) in [(X0 + 6, Z0 + 9), (X1 - 6, Z0 + 9), (X0 + 6, Z1 - 7), (X1 - 6, Z1 - 7)]:
        v.fill(x, 2, z, x, 3, z, 'waxed_copper_block')
        v.set(x, 4, z, 'waxed_cut_copper')
        v.set(x, 5, z, 'copper_bulb[lit=true,powered=false]')
        v.set(x, 6, z, 'lightning_rod[facing=up,powered=false,waterlogged=false]')
        v.set(x, 7, z, 'lightning_rod[facing=up,powered=false,waterlogged=false]')
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            v.stairs(x + dx, 2, z + dz, 'waxed_cut_copper', {(1, 0): 'west', (-1, 0): 'east', (0, 1): 'north', (0, -1): 'south'}[(dx, dz)], 'bottom')
    # franja de peligro en el zocalo interior
    for x in range(X0 + 1, X1):
        v.set(x, 2, Z1 - 1, 'yellow_concrete' if x % 2 == 0 else 'black_concrete')
    # cuadros de cobre en el techo
    for x in range(X0 + 2, X1 - 1, 4):
        v.set(x, 11, Z0 + 2, 'waxed_copper_block')

def crown_rayo(v, T):
    """Remate del tejado: aguja de pararrayos sobre bloques de cobre."""
    k = v.techo_k; y = 12 + k
    cx, cz = CX, (Z0 + Z1) // 2
    v.fill(cx - 1, y - 1, cz - 1, cx + 1, y, cz + 1, 'waxed_cut_copper')
    v.fill(cx, y + 1, cz, cx, y + 3, cz, 'waxed_copper_block')
    for dy in range(4, 8): v.set(cx, y + dy, cz, 'lightning_rod[facing=up,powered=false,waterlogged=false]')
    for (dx, dz) in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        v.set(cx + dx, y - 1, cz + dz, 'copper_bulb[lit=true,powered=false]')

RAYO = dict(
    wall='white_concrete', trim='waxed_cut_copper', trim_mat='waxed_cut_copper', base='polished_andesite', floor_edge='smooth_stone',
    acc='yellow_concrete', acc2='black_concrete', pillar='waxed_copper_block', glass='yellow_stained_glass', roof='yellow_concrete', roof_stairs='waxed_cut_copper',
    ceiling='gray_concrete', lamp='sea_lantern', carpet='yellow_carpet', floor_a='black_concrete', floor_b='gray_concrete', floor_line='yellow_concrete',
    emblem=EMB_RAYO, emblem_pal={'#': 'yellow_concrete'}, props=props_rayo, crown=crown_rayo)

def crown_generico(bloque, luz):
    def f(v, T):
        k = v.techo_k; y = 12 + k; cx, cz = CX, (Z0 + Z1) // 2
        v.fill(cx - 1, y - 1, cz - 1, cx + 1, y, cz + 1, bloque)
        v.fill(cx, y + 1, cz, cx, y + 4, cz, bloque)
        v.set(cx, y + 5, cz, luz)
        for (dx, dz) in ((2, 0), (-2, 0), (0, 2), (0, -2)): v.set(cx + dx, y - 1, cz + dz, luz)
    return f

def props_esquinas(pilar, luz, base):
    def f(v, T):
        for (x, z) in [(X0 + 6, Z0 + 9), (X1 - 6, Z0 + 9), (X0 + 6, Z1 - 7), (X1 - 6, Z1 - 7)]:
            v.fill(x, 2, z, x, 4, z, pilar)
            v.set(x, 5, z, luz)
            v.set(x, 1, z, base)
    return f

EMB_FUEGO = [
    "    #    ",
    "   ###   ",
    "  ## ##  ",
    " ##   ## ",
    " ## # ## ",
    "  #####  ",
]
EMB_HIELO = [
    "    #    ",
    " # ### # ",
    "  #####  ",
    " ####### ",
    "  #####  ",
    " # ### # ",
]
EMB_AGUA = [
    "    #    ",
    "   ###   ",
    "  #####  ",
    " ####### ",
    " ####### ",
    "  #####  ",
]

FUEGO = dict(
    wall='red_nether_bricks', trim='polished_blackstone_bricks', trim_mat='polished_blackstone_brick', base='blackstone', floor_edge='polished_blackstone',
    acc='orange_concrete', acc2='black_concrete', pillar='polished_blackstone', glass='orange_stained_glass', roof='red_concrete', roof_stairs='nether_brick',
    ceiling='nether_bricks', lamp='shroomlight', carpet='red_carpet', floor_a='red_terracotta', floor_b='orange_terracotta', floor_line='black_concrete',
    emblem=EMB_FUEGO, emblem_pal={'#': 'orange_concrete'}, props=props_esquinas('magma_block', 'shroomlight', 'blackstone'), crown=crown_generico('magma_block', 'shroomlight'))
HIELO = dict(
    wall='snow_block', trim='packed_ice', trim_mat='stone_brick', base='stone_bricks', floor_edge='smooth_quartz',
    acc='light_blue_concrete', acc2='cyan_concrete', pillar='blue_ice', glass='light_blue_stained_glass', roof='white_concrete', roof_stairs='smooth_quartz',
    ceiling='packed_ice', lamp='sea_lantern', carpet='light_blue_carpet', floor_a='packed_ice', floor_b='blue_ice', floor_line='light_blue_concrete',
    emblem=EMB_HIELO, emblem_pal={'#': 'light_blue_concrete'}, props=props_esquinas('blue_ice', 'sea_lantern', 'packed_ice'), crown=crown_generico('blue_ice', 'sea_lantern'))
AGUA = dict(
    wall='prismarine_bricks', trim='dark_prismarine', trim_mat='dark_prismarine', base='stone_bricks', floor_edge='prismarine_bricks',
    acc='blue_concrete', acc2='cyan_concrete', pillar='dark_prismarine', glass='light_blue_stained_glass', roof='blue_terracotta', roof_stairs='prismarine_brick',
    ceiling='prismarine', lamp='sea_lantern', carpet='blue_carpet', floor_a='blue_concrete', floor_b='light_blue_concrete', floor_line='white_concrete',
    emblem=EMB_AGUA, emblem_pal={'#': 'blue_concrete'}, props=props_esquinas('dark_prismarine', 'sea_lantern', 'prismarine'), crown=crown_generico('dark_prismarine', 'sea_lantern'))
