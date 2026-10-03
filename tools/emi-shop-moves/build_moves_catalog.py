#!/usr/bin/env python3
"""Genera config/emipokemon/shop/extra/mt-movimientos.json: 4 pestanas de la tienda (MT, huevo, estrella, tutor) con TODOS los
movimientos de TMCraft, a precio segun su utilidad (puntuacion 1-100 calculada por score.js a partir de los datos de Showdown).

  node score.js <showdown_dir> <tmcraft models/item dir> > moves_scored.json     (ya incluido)
  python3 build_moves_catalog.py moves_scored.json <salida.json>
"""
import json, sys

TIERS = [(24, "Común", 400), (39, "Poco común", 1000), (54, "Raro", 2500), (69, "Épico", 5000), (100, "Legendario", 10000)]
CATEGORIES = [  # prefijo del item de TMCraft, id de categoria, titulo, multiplicador de precio
    ("tm", "tm_moves", "MT Movimientos", 1.0),
    ("egg", "egg_moves", "Mov. Huevo", 1.5),
    ("star", "star_moves", "Mov. Estrella", 2.0),
    ("tutor", "tutor_moves", "Mov. Tutor", 1.0),
]
TYPES = {"Normal": "Normal", "Fire": "Fuego", "Water": "Agua", "Grass": "Planta", "Electric": "Eléctrico", "Ice": "Hielo",
         "Fighting": "Lucha", "Poison": "Veneno", "Ground": "Tierra", "Flying": "Volador", "Psychic": "Psíquico", "Bug": "Bicho",
         "Rock": "Roca", "Ghost": "Fantasma", "Dragon": "Dragón", "Dark": "Siniestro", "Steel": "Acero", "Fairy": "Hada"}
CATS = {"Physical": "Físico", "Special": "Especial", "Status": "Estado"}
MAX_PER_PURCHASE = 5

def tier(score):
    for limit, name, price in TIERS:
        if score <= limit:
            return name, price
    return TIERS[-1][1], TIERS[-1][2]

def main(src, dest):
    moves = json.load(open(src, encoding="utf-8"))
    out = {"categories": []}
    for prefix, cid, title, mult in CATEGORIES:
        products = []
        for move_id, m in sorted(moves.items(), key=lambda kv: ((kv[1] or {}).get("name") or kv[0]).lower()):
            if m is None:  # sin datos en Showdown: nivel medio
                score, detail = 50, "Sin datos"
            else:
                score = m["score"]
                kind = CATS.get(m["cat"], m["cat"])
                if m["cat"] == "Status":
                    detail = f'{TYPES.get(m["type"], m["type"])} · {kind}'
                else:
                    acc = "-" if m["acc"] is True else f'{m["acc"]}%'
                    detail = f'{TYPES.get(m["type"], m["type"])} · {kind} · Poder {m["bp"] or "var."} · Prec. {acc}'
            tname, base = tier(score)
            products.append({
                "id": f"{prefix}_{move_id}",
                "item": f"tmcraft:{prefix}_{move_id}",
                "price": int(round(base * mult)),
                "maxPerPurchase": MAX_PER_PURCHASE,
                "enabled": True,
                "description": f"{tname} · {detail}",
                "requiredPermission": "",
            })
        out["categories"].append({"id": cid, "title": title, "products": products})
    json.dump(out, open(dest, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, separators=(",", ":"))
    total = sum(len(c["products"]) for c in out["categories"])
    print(f"{total} productos en {len(out['categories'])} pestañas -> {dest}")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(*sys.argv[1:])
