#!/usr/bin/env python3
"""Genera el libro de misiones de FTB Quests "Guia de EmiCobbleverse": cocina (Farmer's Delight + nuestros platos), Oritech y Tom's Storage.

Uso: build_quests.py <config/ftbquests/quests> <mc_es.json> <FarmersDelight.jar> <cobblemon.jar> <oritech.jar> <toms_storage.jar> <client.jar>
  mc_es.json = minecraft/lang/es_es.json (del indice de assets de Mojang). Las recetas y los nombres salen de los jars (datos reales).
"""
import os, shutil, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from qlib import Quest, Chapter, write_book
from data import Jars, Names, recipe_line, crafting_recipe, result_id, tag_name

out, mc_es, fd_jar, cob_jar, ori_jar, ts_jar, client_jar = sys.argv[1:8]
jars = Jars([fd_jar, cob_jar, ori_jar, ts_jar, client_jar])
TS_ES = {  # Tom's Storage no trae es_es
    "block.toms_storage.inventory_connector": "Conector de inventarios", "block.toms_storage.storage_terminal": "Terminal de almacenamiento",
    "block.toms_storage.trim": "Marco de inventario", "block.toms_storage.open_crate": "Caja abierta", "block.toms_storage.inventory_cable": "Cable de inventario",
    "block.toms_storage.inventory_cable_framed": "Cable de inventario enmarcado", "block.toms_storage.inventory_cable_connector": "Conector de cable de inventario",
    "block.toms_storage.inventory_proxy": "Proxy de inventario", "block.toms_storage.crafting_terminal": "Terminal de crafteo",
    "block.toms_storage.basic_inventory_hopper": "Tolva de inventario básica", "block.toms_storage.level_emitter": "Emisor de nivel",
    "block.toms_storage.inventory_cable_connector_framed": "Conector de cable de inventario (enmarcado)", "block.toms_storage.inventory_interface": "Interfaz de inventario",
    "block.toms_storage.filing_cabinet": "Archivador", "item.toms_storage.paint_kit": "Kit de pintura", "item.toms_storage.wireless_terminal": "Terminal inalámbrica",
    "item.toms_storage.adv_wireless_terminal": "Terminal inalámbrica avanzada", "item.toms_storage.item_filter": "Filtro de objetos",
    "item.toms_storage.polymorphic_item_filter": "Filtro de objetos polimórfico", "item.toms_storage.tag_item_filter": "Filtro de etiquetas",
    "item.toms_storage.inventory_configurator": "Configurador de inventarios",
}
N = Names(jars, mc_es, TS_ES)


def exists(iid):
    ns, name = iid.split(":")
    if ns == "minecraft":
        return jars.has(f"assets/minecraft/models/item/{name}.json")
    return jars.has(f"assets/{ns}/models/item/{name}.json") or jars.has(f"assets/{ns}/items/{name}.json")


MISSING = []


def need(iid):
    if not exists(iid):
        MISSING.append(iid)
    return iid


G, R, Y, A, W, GR = "&6", "&a", "&e", "&b", "&f", "&7"


def head(t):
    return f"{G}{t}&r"


def recipe_block(ns, item):
    r = crafting_recipe(jars, ns, item)
    if not r:
        return []
    shape = "con forma" if r["type"].endswith("shaped") else "sin forma"
    return ["", head("Receta de crafteo") + f" {GR}({shape}; en REI pulsa R sobre el objeto para verla){W}", recipe_line(r, N)]


def examples(folder, n=3, sep=" → "):
    out = []
    for p in sorted(jars.names(f"data/oritech/recipe/{folder}/")):
        r = jars.json(p)
        if "compat" in p or not r.get("results") or not r.get("ingredients"):
            continue
        ing = " + ".join(N.ing(i) for i in r["ingredients"])
        res = ", ".join(f"{x.get('count', 1)}× {N.item(x['id'])}" for x in r["results"])
        out.append(f"{ing}{sep}{res}")
    return out[:n]


def ex_block(folder, n=3):
    ex = examples(folder, n)
    return (["", head("Ejemplos de lo que hace")] + [f"• {e}" for e in ex]) if ex else []


# ============================================================ BIENVENIDA
bienvenida = Chapter("bienvenida", "Bienvenida", "minecraft:written_book", [
    Quest("hola", "¡Bienvenido a EmiCobbleverse!", [
        f"{G}Esta guía te enseña, paso a paso, los mods de {W}cocina{G}, {W}tecnología (Oritech){G} y {W}almacenamiento (Tom's Storage){G}.",
        "",
        "• Cada capítulo (a la izquierda) es un mod. Las misiones se desbloquean en orden: las flechas muestran qué necesitas antes.",
        "• Las misiones de objetos se completan solas al tener el objeto en el inventario; no se gasta.",
        "• Para ver cómo se fabrica algo, pasa el ratón sobre el objeto en el inventario y pulsa " + f"{Y}R{W} (recetas) o {Y}U{W} (usos) en REI.",
        "• Cada misión da un poco de experiencia al completarla.",
    ], [("check",)], xp=10, subtitle="Empieza aquí"),
    Quest("cocina", "Cocina Pokémon", [
        "Cocina con la olla de Farmer's Delight y descubre los platos con bayas que dan efectos de Pokémon y de Minecraft.",
        "", "Capítulos: " + f"{Y}Cocina básica{W} y {Y}Platos Pokémon{W}.",
    ], [("check",)], deps=["hola"], xp=10),
    Quest("tecnologia", "Tecnología: Oritech", [
        "Máquinas, energía y automatización. Con la integración de Cobblemon también fabrica objetos de Pokémon.",
        "", "Capítulos: " + f"{Y}Primeros pasos{W}, {Y}Procesamiento{W}, {Y}Energía{W}, {Y}Logística{W}, {Y}Automatización{W} y {Y}Avanzado{W}.",
        "", f"{R}Aviso:{W} los minerales de Oritech solo aparecen en chunks nuevos; explora la zona nueva del mapa.",
    ], [("check",)], deps=["hola"], xp=10),
    Quest("almacen", "Almacenamiento: Tom's Storage", [
        "Conecta tus cofres en una sola red y accede a todo desde una terminal.",
        "", "Capítulo: " + f"{Y}Tom's Storage{W}.",
    ], [("check",)], deps=["hola"], xp=10),
], group=None)

# ============================================================ COCINA BASICA
FD = "farmersdelight:"
cb = []


def fdq(key, item, text, deps=(), xp=15, extra=()):
    need(item)
    cb.append(Quest(key, N.item(item), [text] + recipe_block("farmersdelight", item) + list(extra), [("item", item)], deps, xp))


fdq("cuchillo", FD + "iron_knife", "El cuchillo se usa sobre la tabla de picar para cortar ingredientes y obtener más cosas de los alimentos.")
fdq("tabla", FD + "cutting_board", "Coloca la tabla en el suelo, pon un objeto encima y úsala con la herramienta adecuada (el cuchillo, por ejemplo).", ["cuchillo"])
fdq("parrilla", FD + "stove", "La parrilla es una fuente de calor. La olla necesita una fuente de calor justo debajo para cocinar.")
fdq("olla", FD + "cooking_pot", "La olla es el centro de la cocina: pon hasta 6 ingredientes y, si el plato lo pide, un cuenco como recipiente. Debajo necesita calor (parrilla, hoguera, lava...).",
    ["parrilla"], extra=["", f"{R}Truco:{W} al sacar un plato de cuenco hay que tener un cuenco en la mano."])
fdq("sarten", FD + "skillet", "La sartén cocina alimentos sobre la parrilla o una hoguera, sin necesidad de olla.", ["parrilla"])
fdq("arroz", FD + "rice", "El arroz crece en el agua. Cocínalo en la olla para obtener arroz cocido, base de los curris.", ["olla"],
    extra=["", f"{GR}Plántalo en tierra con agua encima (tierra de cultivo con una capa de agua).{W}"])
fdq("arroz_cocido", FD + "cooked_rice", "Pon arroz en la olla (con calor debajo) y obtendrás arroz cocido. Lo necesitan los 18 curris de baya.", ["arroz"], xp=20)
cb.append(Quest("consejos", "Consejos de cocina", [
    f"{G}Antes de cocinar:{W}",
    "• Usa la parrilla (o una hoguera) debajo de la olla.",
    "• Los platos que se sirven en cuenco necesitan un cuenco en la mano al sacarlos de la olla.",
    "• Mira la receta de cada plato en REI (R sobre el plato).",
    "• Los platos de EmiCobbleverse dan efectos de Minecraft y de Pokémon (CobbleCuisine).",
], [("check",)], ["olla", "sarten", "tabla"], xp=20))
cocina_basica = Chapter("cocina_basica", "Cocina básica", FD + "cooking_pot", cb,
                        subtitle=["Las herramientas de Farmer's Delight"], group="cocina")

# ============================================================ PLATOS POKEMON (desde el datapack EmiCocina)
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "emi-cocina"))
import build_cocina_datapack as BC  # noqa: E402
import json  # noqa: E402

pk = [Quest("primer_plato", "Tu primer plato", [
    "Con la olla lista y arroz cocido, ya puedes preparar los platos de EmiCobbleverse.",
    "", "Cada plato se prepara en la olla y se completa al " + f"{Y}comerlo{W}.",
    "", f"{R}Consejo:{W} las bayas se encuentran en los árboles de bayas de Cobblemon.",
], [("item", need("cobblemon:oran_berry"))], xp=15)]
DISH_ICON = {}
for rid, (rec, (name, ing_txt, effects)) in BC.DISHES.items():
    ings = {}
    for i in rec["ingredients"]:
        ings[i["item"]] = ings.get(i["item"], 0) + 1
    title = json.loads(rec["result"]["components"]["minecraft:custom_name"])["text"]
    desc = [f"{G}Ingredientes:{W} {ing_txt}.", f"{G}Efectos:{W} {effects}."]
    desc.append("" if True else "")
    desc.append(f"Cocínalo en la olla de Farmer's Delight" + (" con un cuenco." if rec.get("container") else "."))
    if rid == "festin_directo":
        desc += ["", f"{R}Festín del Directo:{W} activa un bono aleatorio de servidor de 20 minutos para todos. "
                 f"Máximo {BC.MAX_SEGUIDOS} seguidos y {BC.COOLDOWN_MIN} min de enfriamiento; si está en enfriamiento, el plato se devuelve."]
    tasks = [("item", need(i), n) for i, n in ings.items()] + [("adv", f"emicocina:comer/{rid}", "eat")]
    pk.append(Quest(rid, title, desc, tasks, ["primer_plato"] if rid != "festin_directo" else ["primer_plato", "tarta_bayas_dulces"],
                    xp=40 if rid == "festin_directo" else 20, optional=False))
    DISH_ICON[rid] = rec["result"]["id"]
platos = Chapter("platos_pokemon", "Platos Pokémon", "cobblemon:oran_berry", pk,
                 subtitle=["Recetas con bayas y efectos de Pokémon"], group="cocina", hide_lines=True)

# ============================================================ ORITECH
ORI = "oritech:"
byitem = {}


def oq(chapter_list, item, text, xp=15, extra=(), deps=(), title=None, craft=True):
    need(item)
    desc = [text] + (recipe_block("oritech", item) if craft else []) + list(extra)
    q = Quest(item.split(":")[1], title or N.item(item), desc, [("item", item)], list(deps), xp)
    chapter_list.append(q)
    byitem[item] = (chapter_list, q)
    return q


def auto_deps(chapter_list, craft_ns="oritech"):
    """Una mision depende de otra del mismo capitulo si su receta usa el objeto de la otra."""
    keys = {q.tasks[0][1]: q.key for q in chapter_list if q.tasks and q.tasks[0][0] == "item"}
    for q in chapter_list:
        if not q.tasks or q.tasks[0][0] != "item":
            continue
        r = crafting_recipe(jars, craft_ns, q.tasks[0][1])
        if not r:
            continue
        used = set()
        if r["type"].endswith("shaped"):
            for row in r["pattern"]:
                for ch in row:
                    if ch != " " and "item" in r["key"][ch]:
                        used.add(r["key"][ch]["item"])
        else:
            used = {i["item"] for i in r["ingredients"] if isinstance(i, dict) and "item" in i}
        for u in used:
            k = keys.get(u)
            if k and k != q.key and k not in q.deps:
                q.deps.append(k)


# ---- Primeros pasos
o1 = []
oq(o1, ORI + "machine_frame_block", f"Pieza estructural de Oritech. {GR}El mod indica que el tamaño máximo del marco se puede cambiar en su configuración.{W}")
oq(o1, ORI + "machine_core_1", "Los núcleos de máquina tienen 7 niveles. Las máquinas multibloque indican qué núcleos necesitan; el nivel 1 es el primitivo.")
oq(o1, ORI + "machine_core_2", "Núcleo básico: el siguiente nivel de núcleo.")
oq(o1, ORI + "machine_core_3", "Núcleo mejorado.")
oq(o1, ORI + "machine_core_4", "Núcleo avanzado.")
oq(o1, ORI + "machine_core_5", "Núcleo de élite.")
oq(o1, ORI + "machine_core_6", "Núcleo ultra.")
oq(o1, ORI + "machine_core_7", "Núcleo definitivo: el máximo nivel de núcleo.")
oq(o1, ORI + "steel_ingot", "El acero es un material básico de Oritech: se hace con lingotes de hierro y carbón.")
oq(o1, ORI + "electrum_ingot", "El electrum se hace con lingotes de oro y polvo de redstone. Lo usan, por ejemplo, las baterías.")
oq(o1, ORI + "adamant_ingot", "El adamantio es un metal avanzado. También se puede obtener en la fundición con una gema de diamante y un lingote de níquel.")
oq(o1, ORI + "wrench", "Llave para tuberías (Pipe Wrench). Se hace con lingotes de acero y de níquel.")
auto_deps(o1)
oritech1 = Chapter("oritech_inicio", "Primeros pasos", ORI + "machine_frame_block", o1,
                   subtitle=["Núcleos, aleaciones y herramientas"], group="oritech")

# ---- Procesamiento
o2 = []
oq(o2, ORI + "pulverizer_block", "Muele lingotes y otros materiales y los convierte en polvo.", extra=ex_block("pulverizer"))
oq(o2, ORI + "fragment_forge_block", "Hace un trabajo parecido al del pulverizador pero más rápido (sus recetas tardan menos). Con el complemento de rendimiento duplica los subproductos.", extra=ex_block("grinder"))
oq(o2, ORI + "powered_furnace_block", "Horno eléctrico: funde y cocina objetos usando energía.")
oq(o2, ORI + "assembler_block", "Combina varios ingredientes en un objeto nuevo: baterías, componentes electrónicos, cristales...", extra=ex_block("assembler"))
oq(o2, ORI + "foundry_block", "Funde dos materiales en una aleación. Por ejemplo, gema de diamante + lingote de níquel → adamantio; materia biopolimérica + hierro → bioacero.", extra=ex_block("foundry", 3))
oq(o2, ORI + "centrifuge_block", "Procesa materiales con una centrifugadora. Con el complemento de fluidos admite también líquidos.", extra=ex_block("centrifuge"))
oq(o2, ORI + "atomic_forge_block", "Fabricación avanzada de componentes: motores de computación, chips de IA y más.", extra=ex_block("atomicforge"))
oq(o2, ORI + "refinery_block", "Refina fluidos: del biocombustible obtiene diésel y nafta; de otros fluidos, ácido sulfúrico y más. Usa módulos de cámara.")
oq(o2, ORI + "refinery_module_block", "Módulo de cámara de refinería: amplía la refinería.")
oq(o2, ORI + "cooler_block", "Enfría fluidos y los convierte en bloques: agua → hielo, lava → obsidiana, vapor → nieve.", extra=ex_block("cooler"))
oq(o2, ORI + "pump_block", "Bomba: extrae fluidos para llevarlos por tuberías a otras máquinas.")
auto_deps(o2)
oritech2 = Chapter("oritech_procesar", "Procesamiento", ORI + "pulverizer_block", o2,
                   subtitle=["Máquinas que transforman materiales"], group="oritech")

# ---- Energia
o3 = []
oq(o3, ORI + "basic_generator_block", "Genera energía quemando combustible.")
oq(o3, ORI + "bio_generator_block", "Genera energía quemando combustibles orgánicos: biomasa, trigo empaquetado...", extra=ex_block("biogen", 3))
oq(o3, ORI + "fuel_generator_block", "Genera energía a partir de combustibles líquidos: petróleo, diésel y turbocombustible (el que más rinde).")
oq(o3, ORI + "lava_generator_block", "Genera energía con lava (o con fuego de Sheol, que rinde mucho más).")
oq(o3, ORI + "steam_engine_block", "Convierte el vapor en energía y agua. Puedes encadenar varios en línea: solo el primero recibe el vapor. " + f"{GR}(Información del propio mod.){W}")
oq(o3, ORI + "big_solar_panel_block", "Panel solar grande. Su producción depende de la calidad del núcleo que lleva.")
oq(o3, ORI + "small_storage_block", "Almacén de energía portátil.")
oq(o3, ORI + "large_storage_block", "Gran almacén de energía.")
oq(o3, ORI + "charger_block", "Se usa para cargar y llenar equipamiento.")
oq(o3, ORI + "basic_battery", "La batería almacena energía en un objeto que puedes llevar.", craft=False,
   extra=["", head("Cómo se hace") + " En el ensamblador: placa de plástico + 2 lingotes de electrum + 1 lingote de acero."])
auto_deps(o3)
oritech3 = Chapter("oritech_energia", "Energía", ORI + "basic_generator_block", o3,
                   subtitle=["Generadores, almacenes y cargadores"], group="oritech")

# ---- Logistica
o4 = []
oq(o4, ORI + "energy_pipe", "Tubería de energía: conecta generadores, máquinas y almacenes.")
oq(o4, ORI + "item_pipe", "Tubería de objetos: mueve objetos entre inventarios y máquinas.")
oq(o4, ORI + "fluid_pipe", "Tubería de fluidos: transporta líquidos entre depósitos, bombas y máquinas.")
oq(o4, ORI + "superconductor", "Superconductor: transporta energía sin pérdidas. No se conecta a tuberías de energía normales. " + f"{GR}(Información del propio mod.){W}", craft=False,
   extra=["", head("Cómo se hace") + " Se crea sin forma a partir de un conducto de superconductor."])
oq(o4, ORI + "item_filter_block", "Filtro de objetos: decide qué objetos pueden entrar en un inventario.")
oq(o4, ORI + "pipe_booster_block", "Impulsor de tuberías (Pipe Booster): se coloca en una tubería para impulsar lo que transporta.")
oq(o4, ORI + "power_pole_block", "Poste de transmisión de energía.")
oq(o4, ORI + "small_tank_block", "Tanque portátil para fluidos.")
auto_deps(o4)
oritech4 = Chapter("oritech_logistica", "Logística", ORI + "energy_pipe", o4,
                   subtitle=["Tuberías y transporte"], group="oritech")

# ---- Automatizacion
o5 = []
oq(o5, ORI + "destroyer_block", "Rompe bloques automáticamente; con el complemento de toque de seda se llevan intactos.")
oq(o5, ORI + "placer_block", "Coloca bloques automáticamente.")
oq(o5, ORI + "fertilizer_block", "Fertiliza los cultivos de su zona automáticamente.")
oq(o5, ORI + "treefeller_block", "Corta árboles automáticamente.")
oq(o5, ORI + "laser_arm_block", "Láser endérico: rompe bloques a distancia (con el complemento de rendimiento obtienes más y con el de toque de seda, bloques intactos). Algunos bloques, como ciertos paneles, deben alimentarse con él.", extra=ex_block("laser", 2))
oq(o5, ORI + "deep_drill_block", "Extractor de roca base: extrae recursos de los nodos de recurso que hay bajo el suelo.", extra=ex_block("deepdrill", 4))
oq(o5, ORI + "drone_port_block", "Puerto de drones: gestiona drones que transportan objetos (y fluidos con el complemento de fluidos).")
oq(o5, ORI + "spawner_controller_block", "Controlador de generador: atrapa al primer mob que pasa sobre él en una jaula de debajo y recoge las almas de entidades que mueren cerca. " + f"{GR}(Información del propio mod.){W}")
oq(o5, ORI + "spawner_cage_block", "Jaula de generador: se usa para construir una jaula del tamaño que necesites bajo el controlador de generador.")
oq(o5, ORI + "enchanter_block", "Encantador estabilizado: aplica encantamientos usando energía y las almas de los catalizadores cercanos.")
oq(o5, ORI + "enchantment_catalyst_block", "Catalizador arcano: almacena almas de las entidades que mueren cerca y puede aplicar encantamientos de libros. " + f"{R}Advertencia del mod:{W} experimentar con lo arcano puede tener consecuencias peligrosas.")
auto_deps(o5)
oritech5 = Chapter("oritech_auto", "Automatización", ORI + "destroyer_block", o5,
                   subtitle=["Máquinas que trabajan por ti"], group="oritech")

# ---- Avanzado: complementos, reactor, acelerador, equipo
o6 = []
ADD = [
    ("machine_speed_addon", "Complemento de velocidad: aumenta la velocidad de operación de la máquina."),
    ("machine_efficiency_addon", "Complemento de eficiencia: mejora la eficiencia energética."),
    ("machine_yield_addon", "Complemento de rendimiento: duplica los subproductos de la forja de fragmentos y aumenta el rendimiento al romper bloques (destructor, láser endérico)."),
    ("machine_capacitor_addon", "Complemento de capacitor: añade almacenamiento de energía a la máquina."),
    ("machine_fluid_addon", "Complemento de fluidos: permite procesar fluidos. Se aplica a la centrifugadora y al puerto de drones."),
    ("machine_burst_addon", "Complemento de ráfaga: permite que la máquina funcione en ráfagas muy rápidas. Se aplica a máquinas de procesamiento."),
    ("machine_processing_addon", "Cámara de procesamiento auxiliar: permite procesar una receta adicional por ciclo (cada cámara extra procesa más objetos por ciclo)."),
    ("machine_silk_touch_addon", "Complemento de toque de seda: aplicable al destructor y al láser endérico."),
    ("machine_redstone_addon", "Unidad de control (Control Unit Addon): complemento de control de la máquina."),
    ("machine_inventory_proxy_addon", "Proxy de inventario: complemento que da acceso al inventario de la máquina."),
    ("machine_acceptor_addon", "Complemento aceptador (Acceptor Addon)."),
    ("machine_extender", "Extensor de complementos: añade más ranuras de complementos a la máquina."),
]
for n, t in ADD:
    oq(o6, ORI + n, t)
oq(o6, ORI + "shrinker_block", "Empalmador de complementos (Addon Splicer).")
oq(o6, ORI + "reactor_controller", "Controlador del reactor nuclear: el centro de un reactor multibloque. Es contenido avanzado: necesita paredes, barras de combustible, refrigeración y puertos.", xp=30)
oq(o6, ORI + "reactor_rod", "Barra de reactor simple: el combustible del reactor.", xp=30)
oq(o6, ORI + "accelerator_controller", "Controlador del acelerador de partículas: multibloque avanzado que se construye con anillos guía, motores y sensores.", xp=30)
oq(o6, ORI + "particle_collector_block", "Colector de taquiones: recoge la energía de los taquiones que emiten las colisiones de partículas. " + f"{GR}(Información del propio mod.){W}", xp=30)
oq(o6, ORI + "jetpack", "El jetpack necesita electricidad o turbocombustible para volar; con turbocombustible es más rápido. " + f"{GR}(Información del propio mod.){W}")
oq(o6, ORI + "hand_drill", "Taladro manual: herramienta eléctrica. Se hace con compuesto enderico, adamantio, un motor y acero.")
oq(o6, ORI + "chainsaw", "Sierra eléctrica: herramienta que funciona con energía.")
oq(o6, ORI + "exo_helmet", "Casco exo: pieza del conjunto de armadura exo.", xp=25)
oq(o6, ORI + "exo_chestplate", "Peto exo.", xp=25)
oq(o6, ORI + "exo_leggings", "Leotardos exo.", xp=25)
oq(o6, ORI + "exo_boots", "Botas exo.", xp=25)
for q in o6:
    if q.key.startswith("exo_"):
        pass
auto_deps(o6)
oritech6 = Chapter("oritech_avanzado", "Avanzado", ORI + "reactor_controller", o6,
                   subtitle=["Complementos, reactor, acelerador y equipo"], group="oritech")

# ============================================================ TOM'S STORAGE
TS = "toms_storage:"
t1 = []


def tq(item, text, deps=(), xp=15, craft=True):
    need(item)
    t1.append(Quest(item.split(":")[1], N.item(item), [text] + (recipe_block("toms_storage", item) if craft else []), [("item", item)], list(deps), xp))


tq(TS + "inventory_connector", "Conecta entre sí todos los inventarios que lo tocan (cofres, barriles...) en una sola red. Los marcos de inventario rellenan los huecos.")
tq(TS + "trim", "Los marcos conectan inventarios con el conector de inventarios, aunque haya huecos entre ellos. Se pueden pintar con el kit de pintura.")
tq(TS + "storage_terminal", "Da acceso a todos tus objetos desde un solo lugar. Colócala encima de un inventario conectado.")
tq(TS + "crafting_terminal", "Como la terminal de almacenamiento, pero con una mesa de crafteo integrada.")
tq(TS + "inventory_cable", "Conecta inventarios a larga distancia. Hay que unirlos con conectores de cable de inventario; el cable necesita un conector de inventarios.")
tq(TS + "inventory_cable_connector", "Conecta un inventario a la red del cable. Usa el configurador de inventarios para filtrar.")
tq(TS + "inventory_proxy", "Extiende la cara del bloque al que apunta.")
tq(TS + "inventory_interface", "Da acceso al inventario del conector de inventarios al que está conectado (por ejemplo, para usar tolvas o tuberías con la red).")
tq(TS + "basic_inventory_hopper", "Importa o exporta objetos de la red de inventarios. Clic derecho con un objeto para ponerle filtro; agáchate con la mano vacía para quitarlo.")
tq(TS + "level_emitter", "Emite una señal de redstone según el contenido de la red de inventarios (más o menos de cierta cantidad).")
tq(TS + "filing_cabinet", "Guarda muchas variantes del mismo objeto.")
tq(TS + "open_crate", "Convierte el bloque que tiene delante en un inventario. " + f"{R}Cuidado:{W} crea entidades de objeto.")
tq(TS + "item_filter", "Permite filtrar varios objetos, con o sin sus componentes.")
tq(TS + "polymorphic_item_filter", "Solo deja insertar objetos que ya existen en ese inventario.", craft=False)
tq(TS + "tag_item_filter", "Filtra usando etiquetas.", craft=False)
tq(TS + "inventory_configurator", "Configura cómo interactúa cada inventario con el conector. Resalta los bloques pintados.")
tq(TS + "paint_kit", "Pinta los bloques para que combinen con lo que tienen alrededor.")
tq(TS + "wireless_terminal", "Amplía tu alcance a la terminal. Alcance limitado en bloques.")
tq(TS + "adv_wireless_terminal", "Agáchate y clic derecho sobre una terminal para vincularla. Un faro cerca de la terminal (radio de 8 bloques) amplía el alcance: con nivel suficiente funciona en toda la dimensión o entre dimensiones.")
auto_deps(t1, "toms_storage")
# dependencias a mano (las recetas usan etiquetas)
for q in t1:
    if q.key in ("storage_terminal", "trim", "inventory_cable", "inventory_interface", "basic_inventory_hopper", "level_emitter", "filing_cabinet",
                 "inventory_configurator", "wireless_terminal") and "inventory_connector" not in q.deps:
        q.deps.append("inventory_connector")
    if q.key in ("polymorphic_item_filter", "tag_item_filter") and "item_filter" not in q.deps:
        q.deps.append("item_filter")
    if q.key == "inventory_cable_connector" and "inventory_cable" not in q.deps:
        q.deps.append("inventory_cable")
tom = Chapter("toms_storage", "Tom's Storage", TS + "storage_terminal", t1, subtitle=["Una red de inventarios con terminal"], group="almacen")

# ============================================================ ESCRIBIR
if os.path.isdir(out):
    shutil.rmtree(out)
groups = [("cocina", "Cocina", FD + "cooking_pot"), ("oritech", "Oritech", ORI + "machine_frame_block"), ("almacen", "Almacenamiento", TS + "storage_terminal")]
chapters = [bienvenida, cocina_basica, platos, oritech1, oritech2, oritech3, oritech4, oritech5, oritech6, tom]
n = write_book(out, [(k, t, i) for k, t, i in groups], chapters)
print(f"{len(chapters)} capitulos, {n} misiones -> {out}")
if MISSING:
    print("OBJETOS QUE NO EXISTEN:", sorted(set(MISSING)))
    sys.exit(1)
