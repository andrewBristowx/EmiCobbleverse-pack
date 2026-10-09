# Emi Estructuras (`emi_estructuras`)

Mod **solo de servidor** (Fabric 1.21.1, Fabric API; en el pack va igual y el cliente lo ignora). Crea la dimensión plana **`emi_estructuras:plano`** y coloca en ella
**todas las estructuras de Cobbleverse y de Legendary Monuments**, bien separadas (una cada 500 bloques, en rejilla de 10 columnas), para que no se pisen como pasó en el mapa ya
generado. Emipokemon (`/emipokemon visitar`) manda a los jugadores a esta dimensión.

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
- **Estructuras flotantes** (Jardín Secreto, Deoxys, Giratina, portal de distorsión): pilar de ladrillo con una plataforma de 9×9 a media altura de la estructura como entrada.

## Requisitos importantes
Las estructuras de **Johto, Hoenn y Sinnoh** vienen de datapacks (`datapacks/extra`: Terralith-DP, Johto, Hoenn, Sinnoh) que hay que **activar ANTES de arrancar el servidor** (los registros de worldgen no se pueden recargar en caliente).
Si faltan, el mod avisa en el log y en el chat de los operadores y **no da por terminada** la generación (se vuelve a intentar al siguiente arranque, solo lo que falte).

## Comandos (operador nivel 2)
| Comando | Qué hace |
|---|---|
| `/emiestructuras generar` | empieza o reanuda lo que falte |
| `/emiestructuras generar forzar` | lo rehace todo desde cero |
| `/emiestructuras parar` | para la generación (se reanuda con `generar`) |
| `/emiestructuras registrar` | vuelve a decirle a Emipokemon donde esta cada estructura ya generada (para mundos generados antes de las ubicaciones extra); no genera nada. Se hace solo 30 s despues de arrancar. |
| `/emiestructuras estado` | cuántas hay hechas y si está en marcha |
| `/emiestructuras ir <estructura>` | te teletransporta a su entrada (autocompleta con las ya generadas) |
| `/emiestructuras lista` | muestra el catálogo (clave de Emipokemon + estructura) |

La dimensión pesa unos 340 MB en disco una vez generada.

## Probado
Servidor completo del pack (Fabric 1.21.1 con todos los mods), arranque desde cero: 86/86 estructuras, 69/69 registradas en Emipokemon con dimensión `emi_estructuras:plano`, 0 fallos, 0 fugas de líquido (medido con mapas de colores
y cortes). Con cliente real: `/emipokemon visitar` (gratis para admin, cobra 5000 Michicoins a los demás), recorrido de rampas y túneles de Giovanni, Ángelo, Turnback Cave y los santuarios, plataformas flotantes y balsas de agua.

## Compilar
`gradle --no-daemon build` (Gradle 8.14, Java 21) → `build/libs/emi-estructuras-1.0.0.jar`. Opción de depuración `-Demi.est.test=1` activa `/emiestructuras _foto <id>` (dibuja vistas aérea, lateral y cortes en `world/emi_estructuras/fotos/`) y `/emiestructuras _planta x0 z0 x1 z1 y` (plano de una capa).
