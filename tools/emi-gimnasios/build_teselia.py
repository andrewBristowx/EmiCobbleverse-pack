import sys, construir
import g_lenora, g_burgh, g_elesa, g_clay, g_skyla, g_brycen, g_drayden, g_trio, g_liga

GIMNASIOS = [   # (ruta, modulo, region, etiqueta, aliases)  en el orden del menu (orden de Unova: Trio, Lenora, Burgh, Elesa, Clay, Skyla, Brycen, Drayden)
    ('teselia/trio', g_trio, 'Gimnasio Cilan/Chili/Cress (Trío)', ['emi gimnasios teselia trio', 'gimnasio trio striaton']),
    ('teselia/lenora', g_lenora, 'Gimnasio Lenora (Básica)', ['emi gimnasios teselia lenora', 'gimnasio lenora nacrene']),
    ('teselia/burgh', g_burgh, 'Gimnasio Burgh (Insecto)', ['emi gimnasios teselia burgh', 'gimnasio burgh castelia']),
    ('teselia/elesa', g_elesa, 'Gimnasio Elesa (Rayo)', ['emi gimnasios teselia elesa', 'gimnasio elesa nimbasa']),
    ('teselia/clay', g_clay, 'Gimnasio Clay (Temblor)', ['emi gimnasios teselia clay', 'gimnasio clay driftveil']),
    ('teselia/skyla', g_skyla, 'Gimnasio Skyla (Reactor)', ['emi gimnasios teselia skyla', 'gimnasio skyla mistralton']),
    ('teselia/brycen', g_brycen, 'Gimnasio Brycen (Frío)', ['emi gimnasios teselia brycen', 'gimnasio brycen icirrus']),
    ('teselia/drayden', g_drayden, 'Gimnasio Drayden (Leyenda)', ['emi gimnasios teselia drayden', 'gimnasio drayden opelucid']),
    ('teselia/liga', g_liga, 'Liga de Teselia (Alto Mando y Campeón Alder)', ['emi gimnasios teselia liga', 'liga teselia alto mando campeon alder']),
]

def todos():
    res = []
    for ruta, mod, etiqueta, aliases in GIMNASIOS:
        v, npcs = mod.construir()
        g, lista = construir.registrar(ruta, v, npcs, 'Teselia', etiqueta, aliases)
        res.append({'nombre': ruta, 'v': g, 'npcs': lista, 'region': 'Teselia', 'etiqueta': etiqueta, 'aliases': aliases})
    return res

if __name__ == '__main__':
    raiz = sys.argv[1]; cfg = sys.argv[2] if len(sys.argv) > 2 else None
    construir.volcar(raiz, todos(), cfg)
