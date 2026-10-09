# EmiLootr-DP (cofres de las estructuras → Lootr)

Lootr solo convierte en cofres Lootr (uno por jugador) los contenedores que tienen **tabla de botín**. Muchas estructuras de Cobbleverse, Legendary Monuments y LumyMon
traen cofres, barriles y cofres dorados de Cobblemon con los **objetos escritos a mano en la plantilla** (sin tabla), y Lootr no los tocaba: el primero que llegaba se lo llevaba todo.

`convert.py` lee las plantillas `.nbt` de esas estructuras y genera el datapack **`datapacks/EmiLootr-DP.zip`**:

| Contenedor en la plantilla | Resultado |
|---|---|
| `minecraft:chest`, `trapped_chest`, `barrel` con objetos fijos | los objetos pasan a una tabla `emi_lootr:<hash>` (mismos objetos y cantidades, todos salen siempre) y Lootr lo convierte |
| cofres dorados de Cobblemon (`*gilded_chest`) | igual (los convierte Lootrmon en `lootrmon:gilded_chest`) |
| barril/cofre de **Sophisticated Storage** y de **Carved Wood** | pasan a `minecraft:barrel`/`chest`/`trapped_chest` con tabla (Lootr no los convierte) |
| estanterías cinceladas, vitrinas, hornos, dispensadores, shulkers, muebles | no se tocan (decoración o funcionales) |

Resultado actual: **83 plantillas, 353 tablas** (150 contenedores vanilla y dorados, 210 de Sophisticated Storage con botín, 16 de Carved Wood). Los objetos que ya no existen en el juego
(p. ej. `mega_showdown:groundmemory`) se omiten (Minecraft rechazaría la tabla entera si tuviera un id desconocido). Los booleanos de los componentes (p. ej. libros) se pasan de byte 0/1 a `true/false`.

El datapack se carga solo y por encima de los de Cobbleverse (su nombre ordena después), así que sustituye las plantillas originales sin tocarlas.

## Uso
```
tools/emi-lootr/run_all.sh <carpeta_mods_del_servidor>   # tarda ~2 min; escribe datapacks/EmiLootr-DP.zip
```
`items-ids.txt` es la lista de ids de objetos del pack (se genera con `/emiestructuras _items`, comando de depuración de emi_estructuras con `-Demi.est.test=1`).
Si cambias de mods o de versión del pack, vuelve a generarla y a ejecutar el script.

## Qué hay que hacer en el servidor
- Subir `EmiLootr-DP.zip` a `world/datapacks/` y reiniciar.
- Afecta a las estructuras que se **generen a partir de ahora**. En la dimensión de `emi_estructuras` hay que regenerar: `/emiestructuras generar forzar` (o borrar `world/dimensions/emi_estructuras` y `world/emi_estructuras/progreso.json` antes de arrancar para que lo haga solo).
- Las estructuras que ya estaban generadas en el mundo normal no cambian.

## Probado
En el servidor de prueba con todo el pack: tras regenerar la dimensión, 283 barriles Lootr, 94 cofres dorados Lootrmon, 14 cofres y 6 cofres trampa Lootr, **todos con tabla**; ya no queda ningún contenedor de loot con objetos fijos.
Se abrió con un cliente real un cofre Lootr con una de las tablas y salieron los objetos esperados (+ el logro «X marca el lugar» de Lootr).
