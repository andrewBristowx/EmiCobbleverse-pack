# Emi Ajustes (`emi_ajustes`) — 1.0.0

Mod **solo de servidor** con arreglos de rendimiento para mods de terceros, hechos con *mixins* (no se toca ni se redistribuye ningún archivo ajeno).

## Guardado de aldeanos de Alexandria / Pickable Villagers
- **Problema** (spark de 46 min con 2 jugadores, 2026-10-17): de los 16 ticks que pasaron de 100 ms, el **56 % del tiempo** (8,3 s de 14,8 s) era `CachedVillagers.save` durante el guardado del mundo. Alexandria engancha el evento de guardado de **cada dimensión** (Architectury `SAVE`) y en cada una escribe entero el archivo de aldeanos recogidos (`world/data/villagers.shift`; `CachedEntities.saveAll` no mira de qué mundo viene), en el hilo principal.
- **Cuántas veces:** el servidor de pruebas con el pack completo tiene **23 dimensiones**, así que cada guardado reescribía el archivo 23 veces (11 de ellas son de los minijuegos `emi_*`).
- **Arreglo:** `AlexandriaGuardadoMixin` cancela esa función (`Alexandria.lambda$init$1`) en toda dimensión que no sea el Overworld. El archivo es uno para todo el servidor, así que con un guardado basta. Si otra versión de Alexandria cambia el nombre de la función, el mixin no se aplica y no pasa nada (`require = 0`); está hecho para Alexandria 1.0.3.3.2.
- **Comprobar:** `-Demi.ajustes.debug=true` escribe en la consola «guardado de aldeanos de Alexandria en <dimensión>: omitido / se hace» en cada guardado.
- **Probado** en el servidor completo de pruebas (pack entero): con `save-all flush` salió «se hace» solo en `minecraft:overworld` y «omitido» en las otras 22; el archivo `villagers.shift` se siguió reescribiendo. **No probado:** en el servidor real, y cuánto baja el tirón allí (depende de lo grande que sea su `villagers.shift`; no se ha medido).

## Compilar
`ALEXANDRIA_JAR=<Alexandria-1.0.3.3.2+1.21.1-fabric.jar> ./build.sh` (Gradle sin conexión; el jar de Alexandria solo se usa para compilar). Salida en `build/libs/`.
