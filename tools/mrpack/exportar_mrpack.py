#!/usr/bin/env python3
"""Crea un .mrpack (formato de modpacks de Modrinth) a partir de este pack de packwiz, sin necesitar el programa packwiz.

Uso (desde la raiz del repositorio):   python3 tools/mrpack/exportar_mrpack.py [salida.mrpack]

- Los mods con archivo .pw.toml (descargables) van como "files" con su URL, sha1 y sha512 (el sha1 y el tamano se piden a la API de Modrinth por su sha512;
  si un archivo no esta en Modrinth —p. ej. los 3 de maven.ftb.dev— se descarga para calcularlos).
- Todo lo demas (mods propios, configs, resourcepacks, shaderpacks, datapacks) va dentro de "overrides/".
- `side = "client"/"server"` del .pw.toml pasa al "env" del .mrpack.
Solo usa la libreria estandar de Python 3.11+.
"""
import hashlib, json, os, sys, tomllib, urllib.request, zipfile

RAIZ = os.getcwd()
SALIDA = sys.argv[1] if len(sys.argv) > 1 else "EmiCobbleverse.mrpack"
API = "https://api.modrinth.com/v2/version_files"


def sha(path, alg):
    h = hashlib.new(alg)
    with open(path, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""):
            h.update(b)
    return h.hexdigest()


def pedir_api(sha512s):
    body = json.dumps({"hashes": sha512s, "algorithm": "sha512"}).encode()
    req = urllib.request.Request(API, data=body, headers={"Content-Type": "application/json", "User-Agent": "EmiCobbleverse-mrpack/1.0"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.load(r)


def main():
    pack = tomllib.load(open("pack.toml", "rb"))
    idx = tomllib.load(open(pack["index"]["file"], "rb"))
    metas, otros = [], []
    for f in idx["files"]:
        (metas if f.get("metafile") else otros).append(f["file"])
    info = []
    for m in metas:
        t = tomllib.load(open(m, "rb"))
        d = t["download"]
        assert d["hash-format"] == "sha512", m
        info.append((os.path.join(os.path.dirname(m), t["filename"]).replace("\\", "/"), t.get("side", "both"), d["url"], d["hash"]))
    conocidos = pedir_api([i[3] for i in info])
    files = []
    for ruta, side, url, h512 in info:
        v = conocidos.get(h512)
        fi = None
        if v:
            fi = next((x for x in v["files"] if x["hashes"].get("sha512") == h512), None)
        if fi:
            sha1, size = fi["hashes"]["sha1"], fi["size"]
        else:
            print("no esta en Modrinth, se descarga:", ruta)
            tmp = "_tmp_descarga"
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "packwiz-installer"}), timeout=120) as r, open(tmp, "wb") as out:
                out.write(r.read())
            assert sha(tmp, "sha512") == h512, "hash distinto: " + ruta
            sha1, size = sha(tmp, "sha1"), os.path.getsize(tmp)
            os.remove(tmp)
        env = {"client": "required", "server": "required"}
        if side == "client": env["server"] = "unsupported"
        if side == "server": env["client"] = "unsupported"
        files.append({"path": ruta, "hashes": {"sha1": sha1, "sha512": h512}, "env": env, "downloads": [url], "fileSize": size})
    indice = {"formatVersion": 1, "game": "minecraft", "versionId": pack["version"], "name": pack["name"],
              "summary": "Modpack de Emi (Cobblemon + Emipokemon)", "files": files,
              "dependencies": {"minecraft": pack["versions"]["minecraft"], "fabric-loader": pack["versions"]["fabric"]}}
    with zipfile.ZipFile(SALIDA, "w", zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        z.writestr("modrinth.index.json", json.dumps(indice, indent=1))
        for o in otros:
            z.write(o, "overrides/" + o)
    print(f"{SALIDA}: {len(files)} mods por URL + {len(otros)} archivos en overrides, version {pack['version']}")


if __name__ == "__main__":
    main()
