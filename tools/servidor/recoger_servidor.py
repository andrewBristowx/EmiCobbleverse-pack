#!/usr/bin/env python3
"""
Recoge del servidor (por SFTP, SOLO LECTURA) lo necesario para revisar un crash de Minecraft y lo deja en un .zip.

Uso:   python recoger_servidor.py
Pide por teclado: direccion, puerto, usuario y contrasena (la contrasena no se muestra ni se guarda).
No escribe, borra ni cambia nada en el servidor: solo lista y lee archivos.

Descarga:
  - los 4 logs mas recientes de logs/ (latest.log y los .log.gz; los .gz se descomprimen aqui)
  - los 3 crash-reports mas recientes
  - config/: c2me, DistantHorizons, lithium, neruina, ferritecore, cobbleoptimizer, cobblepastureoptimizer, cobblemon
  - server.properties, scripts de arranque (*.sh, *.bat) y user_jvm_args.txt
  - la lista de mods (solo nombres y tamanos)
Antes de guardar, tapa las contrasenas/tokens de los archivos de configuracion y las direcciones IP de los logs.
"""
import getpass, gzip, io, os, re, stat, sys, zipfile, datetime

try:
    import paramiko
except ImportError:
    print("Falta paramiko. Instalalo con:   python -m pip install paramiko")
    sys.exit(1)

MAX_BYTES = 40 * 1024 * 1024      # tope por archivo (si es mas grande se toma el final)
IP = re.compile(r"\b(?!127\.0\.0\.1\b)(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\b")
SECRETO = re.compile(r"(?i)^(\s*[\w.\-\"']*(pass|password|secret|token|apikey|api_key|api-key|key)[\w.\-\"']*\s*[=:]\s*).*$")
CONFIG_PREFIJOS = ("c2me", "distanthorizons", "lithium", "neruina", "ferritecore", "cobbleoptimizer",
                   "cobblepastureoptimizer", "cobblemon", "chunky", "spark")


def tapar_secretos(texto):
    return "\n".join(SECRETO.sub(r"\1***TAPADO***", l) for l in texto.split("\n"))


def tapar_ips(texto):
    return IP.sub(r"\1.x.x.x", texto)


def leer(sftp, ruta, tope=MAX_BYTES):
    tam = sftp.stat(ruta).st_size
    with sftp.open(ruta, "rb") as f:
        f.prefetch()
        if tam > tope:
            f.seek(tam - tope)
        return f.read(), tam > tope


def listar(sftp, ruta):
    try:
        return sftp.listdir_attr(ruta)
    except IOError:
        return []


def buscar_raiz(sftp):
    for r in (".", "/", "/home/container", "container", "server", "minecraft"):
        nombres = {a.filename for a in listar(sftp, r)}
        if "server.properties" in nombres or ("mods" in nombres and "config" in nombres):
            return r
    for a in listar(sftp, "."):
        if stat.S_ISDIR(a.st_mode):
            nombres = {b.filename for b in listar(sftp, a.filename)}
            if "server.properties" in nombres or ("mods" in nombres and "config" in nombres):
                return a.filename
    return None


def main():
    host = input("Direccion del SFTP (host): ").strip()
    puerto = input("Puerto [22]: ").strip() or "22"
    usuario = input("Usuario: ").strip()
    clave = getpass.getpass("Contrasena (no se ve al escribir): ")
    try:
        t = paramiko.Transport((host, int(puerto)))
        t.connect(username=usuario, password=clave)
    except Exception as e:
        print("No se pudo conectar:", e)
        sys.exit(1)
    sftp = paramiko.SFTPClient.from_transport(t)
    raiz = buscar_raiz(sftp)
    if raiz is None:
        print("No encuentro la carpeta del servidor (server.properties). Contenido de la carpeta inicial:")
        for a in listar(sftp, "."):
            print("  ", a.filename)
        sys.exit(1)
    print("Carpeta del servidor:", raiz)
    j = lambda *p: "/".join([raiz.rstrip("/")] + list(p)) if raiz not in ("/",) else "/" + "/".join(p)

    salida = {}      # nombre dentro del zip -> bytes
    avisos = []

    def anadir(nombre, datos, truncado=False):
        salida[nombre] = datos
        if truncado:
            avisos.append(f"{nombre}: era muy grande, solo se guardo el final")

    # logs
    logs = [a for a in listar(sftp, j("logs")) if not stat.S_ISDIR(a.st_mode) and a.filename.endswith((".log", ".log.gz"))]
    logs.sort(key=lambda a: a.st_mtime, reverse=True)
    for a in logs[:4]:
        try:
            datos, trunc = leer(sftp, j("logs", a.filename))
            if a.filename.endswith(".gz"):
                try:
                    datos = gzip.decompress(datos)
                except Exception:
                    avisos.append(f"{a.filename}: no se pudo descomprimir")
                    continue
                nombre = "logs/" + a.filename[:-3]
            else:
                nombre = "logs/" + a.filename
            texto = tapar_ips(datos.decode("utf-8", "replace"))
            anadir(nombre, texto.encode("utf-8"), trunc)
            print("  log:", a.filename)
        except Exception as e:
            avisos.append(f"{a.filename}: {e}")

    # crash reports
    cr = [a for a in listar(sftp, j("crash-reports")) if not stat.S_ISDIR(a.st_mode)]
    cr.sort(key=lambda a: a.st_mtime, reverse=True)
    for a in cr[:3]:
        try:
            datos, trunc = leer(sftp, j("crash-reports", a.filename))
            anadir("crash-reports/" + a.filename, tapar_ips(datos.decode("utf-8", "replace")).encode("utf-8"), trunc)
            print("  crash:", a.filename)
        except Exception as e:
            avisos.append(f"{a.filename}: {e}")

    # configs
    for a in listar(sftp, j("config")):
        if stat.S_ISDIR(a.st_mode) or not a.filename.lower().startswith(CONFIG_PREFIJOS):
            continue
        if a.st_size > 2 * 1024 * 1024:
            continue
        try:
            datos, _ = leer(sftp, j("config", a.filename))
            anadir("config/" + a.filename, tapar_secretos(datos.decode("utf-8", "replace")).encode("utf-8"))
            print("  config:", a.filename)
        except Exception as e:
            avisos.append(f"config/{a.filename}: {e}")

    # raiz: server.properties y scripts de arranque
    for a in listar(sftp, raiz):
        n = a.filename
        if stat.S_ISDIR(a.st_mode):
            continue
        if n == "server.properties" or n == "user_jvm_args.txt" or n.endswith((".sh", ".bat")):
            if a.st_size > 1024 * 1024:
                continue
            try:
                datos, _ = leer(sftp, j(n))
                anadir(n, tapar_secretos(datos.decode("utf-8", "replace")).encode("utf-8"))
                print("  raiz:", n)
            except Exception as e:
                avisos.append(f"{n}: {e}")

    # lista de mods
    mods = [a for a in listar(sftp, j("mods")) if not stat.S_ISDIR(a.st_mode)]
    mods.sort(key=lambda a: a.filename.lower())
    lineas = [f"{a.filename}\t{a.st_size} bytes\t{datetime.datetime.fromtimestamp(a.st_mtime):%Y-%m-%d %H:%M}" for a in mods]
    anadir("mods_lista.txt", ("\n".join(lineas) + "\n").encode("utf-8"))
    print(f"  mods: {len(mods)} archivos")

    if avisos:
        anadir("avisos.txt", ("\n".join(avisos) + "\n").encode("utf-8"))
    sftp.close()
    t.close()

    zipname = "revision_servidor.zip"
    with zipfile.ZipFile(zipname, "w", zipfile.ZIP_DEFLATED) as z:
        for n, d in salida.items():
            z.writestr(n, d)
    print(f"\nListo: {os.path.abspath(zipname)} ({os.path.getsize(zipname)//1024} KB, {len(salida)} archivos)")
    if avisos:
        print("Avisos:", *avisos, sep="\n  ")
    print("Subeme ese .zip al chat. Antes puedes abrirlo y mirar que contiene.")


if __name__ == "__main__":
    main()
