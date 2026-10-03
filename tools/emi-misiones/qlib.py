"""Utilidades para generar el libro de misiones de FTB Quests (1.21.1, formato 2101.x, VERSION 13).

Todo el contenido (config/ftbquests/quests) se escribe como SNBT: data.snbt, chapter_groups.snbt, chapters/*.snbt y lang/*.snbt.
Los textos van en los archivos lang (en 2101.x el titulo y la descripcion ya no se guardan en el capitulo/mision).
"""
import hashlib, os, re

IDENT = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")


class D(float):
    """double (1.5d)"""


class L(int):
    """long (5L)"""


def esc(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n") + '"'


def snbt(o, ind=0):
    t = "\t" * (ind + 1)
    e = "\t" * ind
    if isinstance(o, dict):
        if not o:
            return "{ }"
        return "{\n" + "".join(f"{t}{k if IDENT.match(k) else esc(k)}: {snbt(v, ind + 1)}\n" for k, v in o.items()) + e + "}"
    if isinstance(o, list):
        if not o:
            return "[ ]"
        return "[\n" + "".join(f"{t}{snbt(v, ind + 1)}\n" for v in o) + e + "]"
    if isinstance(o, bool):
        return "true" if o else "false"
    if isinstance(o, D):
        return f"{float(o)!r}d"
    if isinstance(o, L):
        return f"{int(o)}L"
    if isinstance(o, int):
        return str(o)
    if isinstance(o, str):
        return esc(o)
    raise TypeError(type(o))


def hid(*parts):
    """id hexadecimal de 16 digitos, estable (no cambia entre ejecuciones, asi no se pierde el progreso de los jugadores).

    FTB Quests lee los ids con Long.parseLong(hex, 16), que falla si el primer digito es 8-F (numero negativo): el capitulo salia
    "Sin nombre" y las dependencias con esos ids se perdian. Por eso el primer digito se limita a 0-7.
    """
    h = hashlib.sha1("|".join(parts).encode("utf-8")).hexdigest()[:16].upper()
    return format(int(h[0], 16) & 7, "X") + h[1:]


class Quest:
    def __init__(self, key, title, desc, tasks, deps=(), xp=0, subtitle="", optional=False, size=1.0):
        self.key, self.title, self.desc, self.tasks, self.deps = key, title, desc, tasks, list(deps)
        self.xp, self.subtitle, self.optional, self.size = xp, subtitle, optional, size


class Chapter:
    def __init__(self, key, title, icon, quests, subtitle=(), group=None, shape="rsquare", hide_lines=False):
        self.key, self.title, self.icon, self.quests, self.subtitle, self.group, self.shape = key, title, icon, quests, list(subtitle), group, shape
        self.hide_lines = hide_lines


def task_nbt(chapter_key, q, i, t):
    kind = t[0]
    tid = hid("task", chapter_key, q.key, str(i))
    if kind == "item":
        n = t[2] if len(t) > 2 else 1
        d = {"id": tid, "type": "item", "item": {"id": t[1], "count": 1}}
        if n != 1:
            d["count"] = L(n)
        d["consume_items"] = False
        return d
    if kind == "adv":
        return {"id": tid, "type": "advancement", "advancement": t[1], "criterion": t[2]}
    if kind == "check":
        return {"id": tid, "type": "checkmark"}
    raise ValueError(kind)


def layout(quests):
    """Coloca las misiones en columnas segun su profundidad de dependencias."""
    byk = {q.key: q for q in quests}
    depth = {}

    def dp(k, stack=()):
        if k in depth:
            return depth[k]
        if k in stack:
            raise ValueError("ciclo " + k)
        q = byk[k]
        local = [d for d in q.deps if isinstance(d, str)]  # las dependencias de otros capitulos (tuplas) no cuentan para la colocacion
        depth[k] = 0 if not local else 1 + max(dp(d, stack + (k,)) for d in local)
        return depth[k]

    for q in quests:
        dp(q.key)
    cols = {}
    for q in quests:
        cols.setdefault(depth[q.key], []).append(q)
    MAXROWS = 6  # las columnas altas se parten en varias para que quepan en pantalla
    height = min(MAXROWS, max(len(v) for v in cols.values()))
    pos = {}
    x = 0.0
    for c in sorted(cols):
        qs = cols[c]
        for i in range(0, len(qs), MAXROWS):
            chunk = qs[i:i + MAXROWS]
            off = (height - len(chunk)) / 2.0
            for r, q in enumerate(chunk):
                pos[q.key] = (x, (off + r) * 1.7)
            x += 2.0
    return pos


def write_book(out, groups, chapters):
    """groups: lista de (key, titulo, icono). chapters: lista de Chapter (el orden es el de la lista)."""
    os.makedirs(os.path.join(out, "chapters"), exist_ok=True)
    os.makedirs(os.path.join(out, "lang"), exist_ok=True)
    lang = {}
    gid = {k: hid("group", k) for k, _, _ in groups}
    open(os.path.join(out, "data.snbt"), "w", encoding="utf-8", newline="\n").write(snbt({
        "version": 13,
        "default_reward_team": False,
        "default_consume_items": False,
        "default_autoclaim_rewards": "disabled",
        "default_quest_shape": "rsquare",
        "default_quest_disable_jei": False,
        "drop_loot_crates": False,
        "grid_scale": D(0.5),
        "pause_game": False,
        "lock_message": "",
        "disable_gui": False,
        "progression_mode": "flexible",
        "detection_delay": 20,
        "show_lock_icons": True,
        "drop_book_on_death": False,
        "hide_excluded_quests": False,
        "fallback_locale": "en_us",
        "verify_on_load": True,
    }) + "\n")
    open(os.path.join(out, "chapter_groups.snbt"), "w", encoding="utf-8", newline="\n").write(snbt({
        "chapter_groups": [{"id": gid[k]} for k, _, _ in groups]
    }) + "\n")
    for k, title, _ in groups:
        lang[f"chapter_group.{gid[k]}.title"] = title
    order = {}
    for ch in chapters:
        cid = hid("chapter", ch.key)
        pos = layout(ch.quests)
        qs = []
        for q in ch.quests:
            qid = hid("quest", ch.key, q.key)
            x, y = pos[q.key]
            d = {"id": qid, "x": D(x), "y": D(y)}
            if q.size != 1.0:
                d["size"] = D(q.size)
            if q.deps:
                d["dependencies"] = [hid("quest", *((ch.key, k) if isinstance(k, str) else k)) for k in q.deps]
            if q.optional:
                d["optional"] = True
            d["tasks"] = [task_nbt(ch.key, q, i, t) for i, t in enumerate(q.tasks)]
            if q.xp:
                d["rewards"] = [{"id": hid("reward", ch.key, q.key), "type": "xp", "xp": q.xp}]
            qs.append(d)
            lang[f"quest.{qid}.title"] = q.title
            if q.subtitle:
                lang[f"quest.{qid}.quest_subtitle"] = q.subtitle
            lang[f"quest.{qid}.quest_desc"] = q.desc
        gi = order.get(ch.group, 0)
        order[ch.group] = gi + 1
        data = {
            "id": cid,
            "group": gid[ch.group] if ch.group else "",
            "order_index": gi,
            "filename": ch.key,
            "icon": {"id": ch.icon, "count": 1},
            "default_quest_shape": ch.shape,
        }
        if ch.hide_lines:
            data["default_hide_dependency_lines"] = True
        data["quests"] = qs
        open(os.path.join(out, "chapters", ch.key + ".snbt"), "w", encoding="utf-8", newline="\n").write(snbt(data) + "\n")
        lang[f"chapter.{cid}.title"] = ch.title
        if ch.subtitle:
            lang[f"chapter.{cid}.chapter_subtitle"] = ch.subtitle
    body = snbt(lang) + "\n"
    for loc in ("en_us", "es_es"):  # el mismo texto (en español) para cualquier idioma del cliente
        open(os.path.join(out, "lang", loc + ".snbt"), "w", encoding="utf-8", newline="\n").write(body)
    return sum(len(c.quests) for c in chapters)
