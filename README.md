# EmiCobbleverse (packwiz)

Modpack de Emi para Minecraft 1.21.1 / Fabric 0.19.3 (base COBBLEVERSE - Pokemon Adventure + Emipokemon 0.6.0-alpha.1).
La instancia de Prism (`EmiCobbleverse-Prism-Instance.zip`) ejecuta `packwiz-installer-bootstrap` antes de cada arranque y se
actualiza sola desde `pack.toml` de este repositorio.

- `mods/*.pw.toml`  → mods de Modrinth (versión y hash fijos, `side` = client / server / both)
- `mods/*.jar`      → jars propios que no están en Modrinth (Emipokemon, EmiProtecciones, EmiMobControl, …)
- `config/`, `datapacks/`, `resourcepacks/`, `shaderpacks/` → tal cual el perfil de Emi (sin datos de servidor ni de jugadores)

## Publicar una actualización

1. Cambia lo que haga falta (por ejemplo, reemplaza `mods/Emipokemon-….jar` por el nuevo).
2. Con `packwiz.exe` en esta carpeta: `packwiz update --all` (opcional) y `packwiz refresh`.
3. Sube la versión en `pack.toml` (`version = "1.0.1"`) y ejecuta `git add -A && git commit -m "..." && git push`.
4. Los jugadores lo reciben la próxima vez que abran la instancia.

## Paquetes de resource packs de audio (no incluidos)

`COBBLEVERSE Soundtrack.zip` (180 MB), `JigglyRadio.zip` (125 MB), `Original Pokemon Battle Music.zip` (93 MB) y `PokeDiscs.zip` (25 MB)
son demasiado grandes para este repositorio; cópialos a mano en `resourcepacks/` si los quieres.
