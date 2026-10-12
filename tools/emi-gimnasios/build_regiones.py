"""Genera todas las plantillas de gimnasios y ligas (Teselia, Kalos, Galar, Paldea, Alola) y los registros de emi-estructuras.
Uso: python3 build_regiones.py <carpeta tools/emi-estructuras> [<config/emipokemon/extra-locations.json del pack>]  (siempre genera todo)"""
import sys, construir
import g_lenora, g_burgh, g_elesa, g_clay, g_skyla, g_brycen, g_drayden, g_trio, g_liga
import kalos_viola, kalos_grant, kalos_korrina, kalos_ramos, kalos_clemont, kalos_valerie, kalos_olympia, kalos_wulfric, kalos_liga
import galar_milo, galar_nessa, galar_kabu, galar_bea, galar_opal, galar_gordie, galar_piers, galar_raihan, galar_leon
import paldea_katy, paldea_brassius, paldea_iono, paldea_kofu, paldea_larry, paldea_ryme, paldea_tulip, paldea_grusha, paldea_liga
import alola_ilima, alola_lana, alola_kiawe, alola_mallow, alola_sophocles, alola_acerola, alola_mina, alola_hala, alola_olivia, alola_nanu, alola_hapu, alola_liga

# region -> [(ruta de la plantilla, modulo, etiqueta del menu, aliases)] en el orden del menu (1..N de la region)
def al(r, *extra): return [f'emi gimnasios {r}'] + list(extra)

REGIONES = {
    'Teselia': [
        ('teselia/trio', g_trio, 'Gimnasio Cilan/Chili/Cress (Trío)', ['emi gimnasios teselia trio', 'gimnasio trio striaton']),
        ('teselia/lenora', g_lenora, 'Gimnasio Lenora (Básica)', ['emi gimnasios teselia lenora', 'gimnasio lenora nacrene']),
        ('teselia/burgh', g_burgh, 'Gimnasio Burgh (Insecto)', ['emi gimnasios teselia burgh', 'gimnasio burgh castelia']),
        ('teselia/elesa', g_elesa, 'Gimnasio Elesa (Rayo)', ['emi gimnasios teselia elesa', 'gimnasio elesa nimbasa']),
        ('teselia/clay', g_clay, 'Gimnasio Clay (Temblor)', ['emi gimnasios teselia clay', 'gimnasio clay driftveil']),
        ('teselia/skyla', g_skyla, 'Gimnasio Skyla (Reactor)', ['emi gimnasios teselia skyla', 'gimnasio skyla mistralton']),
        ('teselia/brycen', g_brycen, 'Gimnasio Brycen (Frío)', ['emi gimnasios teselia brycen', 'gimnasio brycen icirrus']),
        ('teselia/drayden', g_drayden, 'Gimnasio Drayden (Leyenda)', ['emi gimnasios teselia drayden', 'gimnasio drayden opelucid']),
        ('teselia/liga', g_liga, 'Liga de Teselia (Alto Mando y Campeón Alder)', ['emi gimnasios teselia liga', 'liga teselia alto mando campeon alder']),
    ],
    'Kalos': [
        ('kalos/viola', kalos_viola, 'Gimnasio Viola (Bicho)', ['emi gimnasios kalos viola', 'gimnasio viola santalune']),
        ('kalos/grant', kalos_grant, 'Gimnasio Grant (Roca)', ['emi gimnasios kalos grant', 'gimnasio grant cyllage']),
        ('kalos/korrina', kalos_korrina, 'Gimnasio Korrina (Lucha)', ['emi gimnasios kalos korrina', 'gimnasio korrina shalour']),
        ('kalos/ramos', kalos_ramos, 'Gimnasio Ramos (Planta)', ['emi gimnasios kalos ramos', 'gimnasio ramos coumarine']),
        ('kalos/clemont', kalos_clemont, 'Gimnasio Clemont (Eléctrico)', ['emi gimnasios kalos clemont', 'gimnasio clemont lumiose']),
        ('kalos/valerie', kalos_valerie, 'Gimnasio Valerie (Hada)', ['emi gimnasios kalos valerie', 'gimnasio valerie laverre']),
        ('kalos/olympia', kalos_olympia, 'Gimnasio Olympia (Psíquico)', ['emi gimnasios kalos olympia', 'gimnasio olympia anistar']),
        ('kalos/wulfric', kalos_wulfric, 'Gimnasio Wulfric (Hielo)', ['emi gimnasios kalos wulfric', 'gimnasio wulfric snowbelle']),
        ('kalos/liga', kalos_liga, 'Liga de Kalos (Alto Mando y Campeona Diantha)', ['emi gimnasios kalos liga', 'liga kalos alto mando campeona diantha']),
    ],
    'Galar': [
        ('galar/milo', galar_milo, 'Estadio Milo (Planta)', ['emi gimnasios galar milo', 'estadio milo turffield']),
        ('galar/nessa', galar_nessa, 'Estadio Nessa (Agua)', ['emi gimnasios galar nessa', 'estadio nessa hulbury']),
        ('galar/kabu', galar_kabu, 'Estadio Kabu (Fuego)', ['emi gimnasios galar kabu', 'estadio kabu motostoke']),
        ('galar/bea', galar_bea, 'Estadio Bea (Lucha)', ['emi gimnasios galar bea', 'estadio bea stow-on-side']),
        ('galar/opal', galar_opal, 'Estadio Opal (Hada)', ['emi gimnasios galar opal', 'estadio opal ballonlea']),
        ('galar/gordie', galar_gordie, 'Estadio Gordie (Roca)', ['emi gimnasios galar gordie', 'estadio gordie circhester']),
        ('galar/piers', galar_piers, 'Estadio Piers (Siniestro)', ['emi gimnasios galar piers', 'estadio piers spikemuth']),
        ('galar/raihan', galar_raihan, 'Estadio Raihan (Dragón)', ['emi gimnasios galar raihan', 'estadio raihan hammerlocke']),
        ('galar/leon', galar_leon, 'Estadio de Wyndon (Campeón Leon)', ['emi gimnasios galar leon', 'estadio wyndon campeon leon']),
    ],
    'Paldea': [
        ('paldea/katy', paldea_katy, 'Gimnasio Katy (Bicho)', ['emi gimnasios paldea katy', 'gimnasio katy cortondo']),
        ('paldea/brassius', paldea_brassius, 'Gimnasio Brassius (Planta)', ['emi gimnasios paldea brassius', 'gimnasio brassius artazon']),
        ('paldea/iono', paldea_iono, 'Gimnasio Iono (Eléctrico)', ['emi gimnasios paldea iono', 'gimnasio iono levincia']),
        ('paldea/kofu', paldea_kofu, 'Gimnasio Kofu (Agua)', ['emi gimnasios paldea kofu', 'gimnasio kofu cascarrafa']),
        ('paldea/larry', paldea_larry, 'Gimnasio Larry (Normal)', ['emi gimnasios paldea larry', 'gimnasio larry medali']),
        ('paldea/ryme', paldea_ryme, 'Gimnasio Ryme (Fantasma)', ['emi gimnasios paldea ryme', 'gimnasio ryme montenevera']),
        ('paldea/tulip', paldea_tulip, 'Gimnasio Tulip (Psíquico)', ['emi gimnasios paldea tulip', 'gimnasio tulip alfornada']),
        ('paldea/grusha', paldea_grusha, 'Gimnasio Grusha (Hielo)', ['emi gimnasios paldea grusha', 'gimnasio grusha glaseado']),
        ('paldea/liga', paldea_liga, 'Liga de Paldea (Team Star y Campeona Geeta)', ['emi gimnasios paldea liga', 'liga paldea team star campeona geeta']),
    ],
    'Alola': [
        ('alola/ilima', alola_ilima, 'Prueba de Ilima (Normal)', ['emi gimnasios alola ilima', 'prueba ilima caverna verdeante']),
        ('alola/lana', alola_lana, 'Prueba de Lana (Agua)', ['emi gimnasios alola lana', 'prueba lana colina arroyuelo']),
        ('alola/kiawe', alola_kiawe, 'Prueba de Kiawe (Fuego)', ['emi gimnasios alola kiawe', 'prueba kiawe volcan wela']),
        ('alola/mallow', alola_mallow, 'Prueba de Mallow (Planta)', ['emi gimnasios alola mallow', 'prueba mallow selva frondosa']),
        ('alola/sophocles', alola_sophocles, 'Prueba de Sophocles (Eléctrico)', ['emi gimnasios alola sophocles', 'prueba sophocles observatorio hokulani']),
        ('alola/acerola', alola_acerola, 'Prueba de Acerola (Fantasma)', ['emi gimnasios alola acerola', 'prueba acerola thrifty megamart']),
        ('alola/mina', alola_mina, 'Prueba de Mina (Hada)', ['emi gimnasios alola mina', 'prueba mina aldea marinera']),
        ('alola/hala', alola_hala, 'Gran prueba de Hala (Lucha)', ['emi gimnasios alola hala', 'gran prueba hala ruinas del conflicto']),
        ('alola/olivia', alola_olivia, 'Gran prueba de Olivia (Roca)', ['emi gimnasios alola olivia', 'gran prueba olivia ruinas de la vida']),
        ('alola/nanu', alola_nanu, 'Gran prueba de Nanu (Siniestro)', ['emi gimnasios alola nanu', 'gran prueba nanu ciudad malie']),
        ('alola/hapu', alola_hapu, 'Gran prueba de Hapu (Tierra)', ['emi gimnasios alola hapu', 'gran prueba hapu canon de poni']),
        ('alola/liga', alola_liga, 'Liga de Alola (Alto Mando y Campeón Kukui)', ['emi gimnasios alola liga', 'liga alola monte lanakila alto mando campeon kukui']),
    ],
}


def todos():
    res = []
    for region, lista in REGIONES.items():
        for ruta, mod, etiqueta, aliases in lista:
            v, npcs = mod.construir()
            g, nl = construir.registrar(ruta, v, npcs, region, etiqueta, aliases)
            res.append({'nombre': ruta, 'v': g, 'npcs': nl, 'region': region, 'etiqueta': etiqueta, 'aliases': aliases})
    return res


if __name__ == '__main__':
    raiz = sys.argv[1]; cfg = sys.argv[2] if len(sys.argv) > 2 else None
    construir.volcar(raiz, todos(), cfg)
