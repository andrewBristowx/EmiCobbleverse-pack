# EmiCobbleverse (packwiz)

Modpack de Emi para Minecraft 1.21.1 / Fabric 0.19.3 (base COBBLEVERSE - Pokemon Adventure + Emipokemon 0.6.0-alpha.1).
La instancia de Prism (`EmiCobbleverse-Prism-Instance.zip`) ejecuta `packwiz-installer-bootstrap` antes de cada arranque y se
actualiza sola desde `pack.toml` de este repositorio.

> **Instancia de Prism:** usa el zip de `prism-instance/EmiCobbleverse-Prism-Instance.zip` (Prism → Añadir instancia → Importar).
> Incluye `packwiz-installer.jar` y arranca con `--bootstrap-no-update`, así el pack se actualiza sin depender de la API de GitHub
> (el bootstrap antiguo la consultaba en cada arranque y, si fallaba, no se actualizaba nada).
> Los argumentos de Java incluyen `-XX:+UnlockExperimentalVMOptions` (necesario para `-XX:G1NewSizePercent`; sin él Java no arranca: "Could not create the Java Virtual Machine").
> Si la instancia ya existe, copia `packwiz-installer.jar` a su carpeta `minecraft/` y añade `--bootstrap-no-update` justo después de
> `packwiz-installer-bootstrap.jar` en Ajustes de la instancia → Configuración personalizada → Comandos personalizados → Pre-lanzamiento.

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
