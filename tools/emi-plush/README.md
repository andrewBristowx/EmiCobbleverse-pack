# Emi Plush (peluche y Pokémon propios, CC0)

Mod pequeño que añade **peluches propios** al pack usando las clases de **Pokeblocks** (`PokedollBlock`, `PokedollBlockEntity`, `PokedollBlockItem`
y el renderizador de GeckoLib), así que se colocan, se ven y se llevan en la mano igual que los pokedolls.

Peluches:
- `emi_plush:michi_dramatico` — **Peluche Michi Dramático** (el gatito de Extacy: blanco, antenas moradas, barriga negra con luna, cara de enfadado).
  Está en el inventario creativo, pestaña **Pokeblocks - Misc**, y se da con `/give @s emi_plush:michi_dramatico`. Al romperlo se suelta a sí mismo.
  No tiene receta ni aparece en cofres: es un objeto de administrador.

Hace falta en **servidor y clientes** (añade un bloque). Depende de `pokeblocks` y `geckolib`, que ya están en el pack.

## Michi Dramático como Pokémon (Cobblemon)
La misma criatura existe también como **Pokémon** de Cobblemon: `emi_plush:michi_dramatico` (especie `data/emi_plush/species/custom/michi_dramatico.json`).
- Tipo **Hada**, nº de Pokédex 10013, estadísticas 70/90/60/80/65/105, habilidades Gran Encanto / Compiescudo / (oculta) Piel Feérica, huevo Campo + Hada.
- Modelo = el del peluche pero **1,3 veces más grande**. Va con UV de caja y dibujado 4 veces más grande, y la especie usa `baseScale` 0,325 (Cobblemon no soporta UV por cara).
- Animaciones (`gen/gen_pokemon.py`): reposo, caminar a saltitos, reposo de combate, dormir, grito, ataque físico (se agazapa y salta con arañazo), especial (salta y las antenas lanzan la energía) y de estado (meneo).
- Grito: maullido de gatito, hecho con los maullidos de gato de Minecraft (`mob/cat/meow1-4`, `purreow1`) a tono alto; `assets/emi_plush/sounds.json`.
- Shiny: lila → turquesa.
- **Gestos**: la cara va en piezas sueltas (ojos, párpados, ojos felices `^ ^`, boca, boca abierta y bocaza de bostezo). Parpadea, mueve las orejas y la cola solo, y de vez en cuando suelta un **bostezo dramático** (cabeza atrás, ojos cerrados y bocaza). Pone ojos felices con el ataque especial y los movimientos de estado, abre la boca al gruñir y al gritar, y cierra los ojos al dormir. Los párpados y las bocas abiertas están escondidos dentro del cuerpo y la animación los saca hacia delante (`position z`).
- **Habilidad Drama** (`data/emi_plush/abilities/drama.js`, es la habilidad normal; la oculta es Gran Encanto): los bostezos de este Pokémon son dobles. **Bostezo alcanza a todos los rivales adyacentes** y **el sueño que provoca dura el doble**. Probada con el simulador de combate de Cobblemon (`test/probar_drama.js`). OJO: Cobblemon une las líneas del archivo con espacios, así que el `.js` **no puede llevar comentarios `//`** (un solo `//` impide que arranque el servidor); el script de prueba lo comprueba.
- Descripción de Pokédex: «la mascota oficial del canal de Emi… muy escandalosa y dramática». Tiene entrada de Pokédex (`dex_entries`, `dexes/emi.json`, añadida a la nacional).
- **No aparece en la naturaleza**: se da con `/givepokemon <jugador> emi_plush:michi_dramatico` (o `/spawnpokemon`). Si se quiere en el mundo hay que añadir un `spawn_pool_world`.
- Depende de `cobblemon` (>= 1.7).
- Para probar una animación en bucle: `MICHI_DEBUG=<animacion> ./build.sh`. Retrato y vista de perfil se ajustan con `MICHI_PS/PX/PY` y `MICHI_FS/FX/FY` (ver `poser()`).

## Cómo se hizo el modelo
`gen/gen_michi.py` genera el modelo (`assets/emi_plush/geo/michi_dramatico.geo.json`) y la textura (`textures/entity/michi_dramatico.png`) a partir de una lista de cubos:
cada cara tiene su propio rectángulo en el atlas (6 px por unidad) y los colores salen de una función por posición, así que las caras siempre coinciden.
Para cambiar el aspecto se editan `CUBOS` y `pintar()` en ese script y se vuelve a ejecutar `build.sh`.

## Compilar
`build.sh` compila contra los jars reales (nombres *intermediary*, como el resto de mods del pack); necesita variables de entorno:
`MC` (client-intermediary.jar de Fabric Loader), `GECKO`, `POKEBLOCKS`, `FAPI` (carpeta con los jars internos de Fabric API), `LIBS` (librerias de Minecraft y fabric-loader), `OUT`.

## Para añadir otro peluche
Copia `MichiBlockEntity`/`MichiBlockModel`/`MichiItemModel`, registra el bloque, el objeto y el tipo de entidad en `EmiPlush`, el renderizador en `EmiPlushClient`,
y añade blockstate, modelo de bloque y de objeto, loot table, textos de idioma y su geo+textura.
