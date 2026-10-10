# Emi Estructuras (`emi_estructuras`)

Mod **solo de servidor** (Fabric 1.21.1, Fabric API; en el pack va igual y el cliente lo ignora). Crea **dos dimensiones planas** y coloca en ellas las estructuras, bien separadas
(una cada 500 bloques, en rejilla de 10 columnas), para que no se pisen como pasó en el mapa ya generado:
- **`emi_estructuras:plano`** ("mundo `pokemon`"): todas las estructuras de Cobbleverse y de Legendary Monuments.
- **`emi_estructuras:dungeons`** ("mundo `dungeons`", v1.1.0): los **jefes de Bosses of Mass Destruction** (Lich, Obsidilith, Void Blossom, Gauntlet), las **40 de When Dungeons Arise** y las **96 de Dungeons and Taverns**
  (140 en total; YUNG's Better Dungeons queda fuera a propósito). Salen en `/emipokemon visitar` / Diario en una sección nueva **Dungeons** (región `Dungeons` de `config/emipokemon/extra-locations.json`, en orden: jefes, DA, D&T).
Emipokemon (`/emipokemon visitar`) manda a los jugadores a la dimensión que toca.

## Qué hace
- **Dimensión `emi_estructuras:plano`**: mundo plano (bedrock, piedra, tierra y césped en y=63), siempre de día, sin biomas ni mobs naturales. Viene como datapack dentro del propio jar (no hay que instalar nada más).
- **Generación automática solo la primera vez**: 30 s después de arrancar el servidor, si todavía no está terminado, empieza a colocar las estructuras (unas 86 y ~8 min de ticks, sin congelar el servidor: 25 ms por tick;
  se puede reanudar). Cuando termina guarda `world/emi_estructuras/progreso.json` con `terminado: true` y nunca vuelve a hacerlo solo.
- Orden: primero las **69 ubicaciones de Emipokemon** (Kanto 13, Johto 14, Hoenn 18, Sinnoh 24; incluye los lagos, Spear Pillar, Turnback Cave, Newmoon Island, el Templo de Sinnoh…) y después todas las demás estructuras
  registradas en los espacios `cobbleverse` y `legendarymonuments` (ordenadas por nombre).
- **Ubicaciones extra del menu**: las estructuras que Emipokemon no traia (Torre Team Rocket, Casa de Ash, Isla Giratina, regiones Galar, Paldea y Alola…) tienen hueco gracias al parche `PatchExtraLocations` y a `config/emipokemon/extra-locations.json`, que este mod lee para saber que estructura va en cada hueco (si no existe usa la lista por defecto del jar).
- Cada estructura se registra en Emipokemon (`ImportantLocationService.recordGeneratedLocation`) con una **entrada segura** junto a ella, así `/emipokemon visitar` / `marcar` / el mapa de misiones la encuentran ahí.
- **Contención de líquidos**: las estructuras de mar (islas, Kyogre, Groudon, Misty…) se colocan sobre una balsa de agua de 28 bloques de margen y la entrada queda fuera, en tierra firme; tras colocar cada una se espera 4 s y
  se borran los líquidos que se hayan escapado por encima del suelo fuera de la estructura (lava de Moltres/Groudon incluida). Medido: 0 fugas en todas.
- **Estructuras enterradas** (gimnasios de Giovanni y Ángelo, Turnback Cave, santuarios de Legendary Monuments): se abre un pozo con una rampa de escalones de ladrillo y, si la sala queda cerrada bajo piedra, un túnel hasta la capa con más espacio libre de la estructura (en Turnback Cave, donde están las salas grandes) y escalones hasta el suelo si la sala es alta (santuarios), para no caer.
- **Estructuras selladas** (v1.1.0; p. ej. la caverna del Void Blossom, la mina de Dungeons Arise, los santuarios de combate): aunque su caja salga por arriba, si tienen una cavidad grande bajo el suelo (>1000 bloques de aire a 6+ bloques de profundidad) a la que no se llega andando desde la entrada, se recorre el aire para comprobarlo y, si no llega, se abre un túnel en pendiente desde la entrada hasta la sala (el mismo de las enterradas).
- **Estructuras flotantes** (Jardín Secreto, Deoxys, Giratina, portal de distorsión, Obsidilith, dirigible, castillo del End…; todo lo que empieza más de 12 bloques sobre el suelo): pilar de ladrillo con una plataforma de 9×9 a media altura de la estructura como entrada.

## Requisitos importantes
Las estructuras de **Johto, Hoenn y Sinnoh** vienen de datapacks (`datapacks/extra`: Terralith-DP, Johto, Hoenn, Sinnoh) que hay que **activar ANTES de arrancar el servidor** (los registros de worldgen no se pueden recargar en caliente).
Si faltan, el mod avisa en el log y en el chat de los operadores y **no da por terminada** la generación (se vuelve a intentar al siguiente arranque, solo lo que falte).

## Comandos (operador nivel 2)
`<mundo>` = `pokemon` o `dungeons`.
| Comando | Qué hace |
|---|---|
| `/emiestructuras generar [<mundo>]` | empieza o reanuda lo que falte (sin mundo: los dos) |
| `/emiestructuras generar [<mundo>] forzar` | lo rehace todo: cada zona se limpia y se vuelve a colocar |
| `/emiestructuras regenerar <mundo>` | **regenera el mundo**: vuelve al suelo plano la zona de cada estructura ya hecha (se borra lo roto y lo puesto, cofres y entidades sueltas) y la coloca otra vez |
| `/emiestructuras regenerar <mundo> <estructura>` | lo mismo, solo con una estructura (autocompleta) |
| `/emiestructuras parar` | para la generación (se reanuda con `generar`) |
| `/emiestructuras registrar` | vuelve a decirle a Emipokemon donde esta cada estructura ya generada; no genera nada. Se hace solo 30 s despues de arrancar. |
| `/emiestructuras estado [<mundo>]` | cuántas hay hechas, si está en marcha y los fallos |
| `/emiestructuras ir <estructura>` | te teletransporta a su entrada (autocompleta con las ya generadas, de los dos mundos) |
| `/emiestructuras lista [<mundo>]` | muestra el catálogo (clave de Emipokemon + estructura) |

**Cómo regenera**: la zona de una estructura es su caja más 40 bloques de margen, la rampa/pilar de la entrada y todo el alto del mundo. Cada sección de chunk se compara con el suelo plano
(bedrock, 124 de piedra, 2 de tierra, césped en y=63) contando bloques, así que solo se reescribe donde hay algo distinto; luego se descartan las entidades que no sean jugadores
(quien esté dentro sale al spawn del mundo normal) y la estructura se coloca igual que la primera vez (misma celda). Lo que se construya fuera de esa zona no se toca. Con muchas estructuras tarda
(del orden de 10-20 min por mundo) pero sin congelar el servidor. En Dungeons una estructura que falle no impide darlo por terminado: queda en `/emiestructuras estado` y se reintenta con `generar dungeons`.

**Radio de visita**: `PatchVisitRadius` (tools/emipokemon-patches) sube de 150 a 300 bloques el radio libre alrededor de la entrada, porque algunas mazmorras miden ~200 bloques.

Cada dimensión pesa cientos de MB en disco una vez generada (Pokémon ~340 MB).

## Probado
Servidor completo del pack (Fabric 1.21.1 con todos los mods), arranque desde cero: 86/86 estructuras, 69/69 registradas en Emipokemon con dimensión `emi_estructuras:plano`, 0 fallos, 0 fugas de líquido (medido con mapas de colores
y cortes). Con cliente real: `/emipokemon visitar` (gratis para admin, cobra 5000 Michicoins a los demás), recorrido de rampas y túneles de Giovanni, Ángelo, Turnback Cave y los santuarios, plataformas flotantes y balsas de agua.

## Compilar
`gradle --no-daemon build` (Gradle 8.14, Java 21) → `build/libs/emi-estructuras-1.1.0.jar`. `gen_dungeons.py <carpeta con los jars>` regenera la sección Dungeons de `config/emipokemon/extra-locations.json` y de la copia por defecto del mod. Opción de depuración `-Demi.est.test=1` activa `/emiestructuras _foto <id>` (dibuja vistas aérea, lateral y cortes en `world/emi_estructuras/fotos/`) y `/emiestructuras _planta x0 z0 x1 z1 y` (plano de una capa).
