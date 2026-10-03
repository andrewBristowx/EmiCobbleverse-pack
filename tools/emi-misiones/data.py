"""Lectura de recetas y nombres (en español) desde los jars de los mods, para escribir las misiones con datos reales."""
import glob, json, os, zipfile


class Jars:
    def __init__(self, paths):
        self.zips = [zipfile.ZipFile(p) for p in paths]
        self.index = {}
        for z in self.zips:
            for n in z.namelist():
                self.index.setdefault(n, z)

    def has(self, name):
        return name in self.index

    def read(self, name):
        return self.index[name].read(name)

    def json(self, name):
        return json.loads(self.read(name))

    def names(self, prefix, suffix=".json"):
        return [n for n in self.index if n.startswith(prefix) and n.endswith(suffix)]


MATERIAL = {
    "iron": "hierro", "gold": "oro", "copper": "cobre", "steel": "acero", "electrum": "electrum", "nickel": "níquel", "netherite": "netherita",
    "redstone": "redstone", "coal": "carbón", "lapis": "lapislázuli", "amethyst": "amatista", "diamond": "diamante", "quartz": "cuarzo",
    "plastic": "plástico", "adamant": "adamantio", "duratium": "duratio", "energite": "energita", "platinum": "platino", "uranium": "uranio",
    "silicon": "silicio", "glowstone": "piedra luminosa", "wheat": "trigo", "biomass": "biomasa", "biosteel": "bioacero", "fluxite": "fluxita",
    "tin": "estaño", "silver": "plata", "aluminum": "aluminio", "bronze": "bronce", "brass": "latón", "zinc": "zinc", "titanium": "titanio",
    "chromium": "cromo", "invar": "invar", "emerald": "esmeralda", "ruby": "rubí", "sapphire": "zafiro", "peridot": "peridoto",
}
KIND = {"ingots": "lingote de {}", "dusts": "polvo de {}", "gems": "gema de {}", "nuggets": "pepita de {}", "plates": "placa de {}",
        "storage_blocks": "bloque de {}", "raw_materials": "{} en bruto", "ores": "mena de {}", "clumps": "grumo de {}", "rods": "varilla de {}"}
TAGS = {
    "c:chests": "cofre", "c:chests/wooden": "cofre de madera", "c:glass_blocks": "bloque de cristal", "c:glass_blocks/colorless": "cristal",
    "c:glass_panes/colorless": "panel de cristal", "c:ender_pearls": "perla de ender", "c:cobblestones": "roca", "c:obsidians/normal": "obsidiana",
    "c:chains": "cadena", "c:leathers": "cuero", "c:feathers": "pluma", "c:silicon": "silicio", "c:carbon_fibre": "fibra de carbono",
    "c:player_workstations/furnaces": "horno", "c:rods/wooden": "palo", "c:crops/rice": "arroz", "c:buckets/water": "cubo de agua",
    "minecraft:planks": "tablones", "minecraft:logs": "troncos", "minecraft:sand": "arena", "minecraft:coals": "carbón", "minecraft:wool": "lana",
    "minecraft:trapdoors": "trampilla", "minecraft:dirt": "tierra", "minecraft:flowers": "flor",
    "oritech:plating": "revestimiento reforzado", "oritech:biomatter": "materia orgánica", "toms_storage:trims": "marco de inventario",
    "c:fuels/bio": "combustible orgánico",
}


def tag_name(tag):
    if tag in TAGS:
        return TAGS[tag]
    ns, _, path = tag.partition(":")
    if ns == "c" and "/" in path:
        kind, mat = path.split("/", 1)
        if kind in KIND:
            return KIND[kind].format(MATERIAL.get(mat, mat.replace("_", " ")))
    return path.replace("/", " ").replace("_", " ")


class Names:
    def __init__(self, jars, mc_es_path, extra=None):
        self.jars = jars
        self.t = json.load(open(mc_es_path, encoding="utf-8"))
        for ns in ("farmersdelight", "cobblemon", "oritech", "toms_storage", "cobblecuisine"):
            for loc in ("en_us", "es_es"):  # es_es pisa a en_us
                n = f"assets/{ns}/lang/{loc}.json"
                if jars.has(n):
                    self.t.update({k: v for k, v in jars.json(n).items() if isinstance(v, str)})
        self.t.update(extra or {})

    def item(self, iid):
        ns, name = iid.split(":")
        for kind in ("item", "block"):
            v = self.t.get(f"{kind}.{ns}.{name}")
            if v:
                return v
        return name.replace("_", " ").capitalize()

    def ing(self, ing):
        if isinstance(ing, list):
            ing = ing[0]
        if "item" in ing:
            return self.item(ing["item"])
        if "tag" in ing:
            return tag_name(ing["tag"])
        return "?"


def shaped_counts(r, names):
    """Cuenta los ingredientes de una receta con forma o sin forma -> lista [(cantidad, nombre)] ordenada por cantidad."""
    counts = {}
    if r["type"].endswith("crafting_shaped"):
        for row in r["pattern"]:
            for ch in row:
                if ch != " ":
                    nm = names.ing(r["key"][ch])
                    counts[nm] = counts.get(nm, 0) + 1
    elif r["type"].endswith("crafting_shapeless"):
        for i in r["ingredients"]:
            nm = names.ing(i)
            counts[nm] = counts.get(nm, 0) + 1
    return sorted(((n, nm) for nm, n in counts.items()), key=lambda t: (-t[0], t[1]))


def recipe_line(r, names):
    return ", ".join(f"{n}× {nm}" if n > 1 else nm for n, nm in shaped_counts(r, names))


def result_id(r):
    res = r.get("result")
    return res.get("id") if isinstance(res, dict) else res


def crafting_recipe(jars, ns, result):
    """Primera receta de crafteo (shaped/shapeless) cuyo resultado es `result` en data/<ns>/recipe/**."""
    for n in sorted(jars.names(f"data/{ns}/recipe/")):
        try:
            r = jars.json(n)
        except Exception:
            continue
        if r.get("type", "").endswith(("crafting_shaped", "crafting_shapeless")) and result_id(r) == result:
            return r
    return None
