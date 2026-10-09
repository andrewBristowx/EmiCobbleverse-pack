# Emi Plush (peluche y Pokémon propios, CC0)

Mod pequeño que añade **peluches propios** al pack usando las clases de **Pokeblocks** (`PokedollBlock`, `PokedollBlockEntity`, `PokedollBlockItem`
y el renderizador de GeckoLib), así que se colocan, se ven y se llevan en la mano igual que los pokedolls.

Peluches:
- `emi_plush:michi_dramatico` — **Peluche Michi Dramático** (el gatito de Extacy: blanco, antenas moradas, barriga negra con luna, cara de enfadado).
  Está en el inventario creativo, pestaña **Pokeblocks - Misc**, y se da con `/give @s emi_plush:michi_dramatico`. Al romperlo se suelta a sí mismo.
  No tiene receta ni aparece en cofres: es un objeto de administrador.

Hace falta en **servidor y clientes** (añade un bloque). Depende de `pokeblocks` y `geckolib`, que ya están en el pack.

## Michi Dramático como Pokémon (Cobblemon)
La misma criatura existe también como **Pokémon** de Cobblemon: `cobblemon:gatitoalien` (nombre en pantalla **GatitoAlien**; especie `data/cobblemon/species/custom/gatitoalien.json`, en el espacio `cobblemon` a propósito: Cobbreeding busca las especies de los huevos solo ahí, y en otro espacio el huevo no eclosiona; el modelo y las animaciones se siguen llamando `michi_dramatico`).
- Tipo **Hada**, nº de Pokédex 10013, estadísticas 70/90/60/80/65/105, habilidades Gran Encanto / Compiescudo / (oculta) Piel Feérica, huevo Campo + Hada.
- Modelo = el del peluche pero **1,3 veces más grande**. Va con UV de caja y dibujado 4 veces más grande, y la especie usa `baseScale` 0,325 (Cobblemon no soporta UV por cara).
- Animaciones (`gen/gen_pokemon.py`): reposo, caminar a saltitos, reposo de combate, dormir, grito, ataque físico (se agazapa y salta con arañazo), especial (salta y las antenas lanzan la energía) y de estado (meneo).
- Grito: maullido de gatito, hecho con los maullidos de gato de Minecraft (`mob/cat/meow1-4`, `purreow1`) a tono alto; `assets/emi_plush/sounds.json`.
- Shiny: lila → turquesa.
- **Gestos**: la cara base (ojos enfadados con brillo y boquita `:3`) está pintada en el cuerpo, igual que en el peluche; encima van piezas sueltas (párpados, ojos felices `^ ^`, boca abierta y bocaza de bostezo). Parpadea, mueve las orejas y la cola solo, y de vez en cuando suelta un **bostezo dramático** (cabeza atrás, ojos cerrados y bocaza). Pone ojos felices con el ataque especial y los movimientos de estado, abre la boca al gruñir y al gritar, y cierra los ojos al dormir. Los párpados y las bocas abiertas están escondidos dentro del cuerpo y la animación los saca hacia delante (`position z`).
- Ataques: aprende **Bostezo** (nivel 8) e **Hipnosis** (nivel 20), además de los de Hada.
- Las patas y la cola cuelgan del hueso `head` (que es el cuerpo entero), así que se mueven con él y no se despegan; `colaA` está metida 0,8 dentro del cuerpo.
- **Habilidad Drama** (`data/emi_plush/abilities/drama.js`, es la habilidad normal; la oculta es Gran Encanto): los bostezos de este Pokémon son dobles. **Bostezo alcanza a todos los rivales adyacentes** y **el sueño que provoca dura el doble**. Probada con el simulador de combate de Cobblemon (`test/probar_drama.js`). OJO: Cobblemon une las líneas del archivo con espacios, así que el `.js` **no puede llevar comentarios `//`** (un solo `//` impide que arranque el servidor); el script de prueba lo comprueba.
- Descripción de Pokédex: «la mascota oficial del canal de Emi… muy escandalosa y dramática». Tiene entrada de Pokédex (`dex_entries`, `dexes/emi.json`, añadida a la nacional).
- **No aparece en la naturaleza**: se da con `/givepokemon <jugador> cobblemon:gatitoalien` (o `/spawnpokemon cobblemon:gatitoalien`). Si se quiere en el mundo hay que añadir un `spawn_pool_world`.
- Depende de `cobblemon` (>= 1.7).
- Para probar una animación en bucle: `MICHI_DEBUG=<animacion> ./build.sh`. Retrato y vista de perfil se ajustan con `MICHI_PS/PX/PY` y `MICHI_FS/FX/FY` (ver `poser()`).

## Pokémon de Emi (aspecto `emi`) — 21 especies
Variante cosmética con los colores y el pelo de Emi (no lleva su ropa): melena **plateada** (tapa fina, flequillo con raya al medio, mechones sueltos de distinto largo a los lados y tres tiras atrás) con **puntas rosas**; la línea Ralts tiene el pelo plateado en lugar de verde, ojos de **dos colores** (naranja y magenta), negro y rosa en el cuerpo.
Líneas: **Happiny/Chansey/Blissey**, **Cleffa/Clefairy/Clefable**, **Igglybuff/Jigglypuff/Wigglytuff**, **Eevee y sus 8 evoluciones** (Vaporeon, Jolteon, Flareon, Espeon, Umbreon, Leafeon, Glaceon, Sylveon) y **Ralts/Kirlia/Gardevoir**.
- Happiny, Chansey y Blissey están ajustadas a mano en `gen/gen_emi_poke.py`; el resto las genera el motor de `gen/emi_species.py` (busca la cabeza por los ojos, retiñe con k-means conservando el relieve y añade cubos de pelo; la tabla `CONFIG` fija los colores de cada especie). `gen/gen_all.py` lo lanza todo.
  Necesita `COBBLEMON_JAR=<jar de Cobblemon>` (y numpy); sin esa variable `build.sh` usa lo ya generado en `resources/`.
- Es un **aspecto** (`species_features/emi.json` + `species_feature_assignments/emi.json`, indicador `emi`), no una especie nueva: evoluciona, se cría y combate como la normal. Resolvers `bedrock/pokemon/resolvers/*/1_*_emi.json`; también hay versión **brillante**.
  Al añadir especies nuevas a la lista hay que **reiniciar el servidor** (un `/reload` no recoge las asignaciones de aspectos).
- **Cómo conseguirlos**: `/givepokemon <jugador> <especie> emi=true`, `/spawnpokemon cobblemon:blissey emi=true` para uno salvaje.
- Los huesos nuevos (`hair_*`, `bangs`) cuelgan de la cabeza/torso y se mueven con ellos; no tienen animación propia.

## GatitoAlien: shiny dorado y accesorios
`gen/gen_michi_extras.py` (se ejecuta tras `gen_pokemon.py`):
- **Shiny dorado**: el pelaje blanco y los lilas pasan a oro (`michi_dramatico_shiny.png`).
- **Accesorios** (aspecto `accessory`): `sunglasses`, `beach_hat`, `crown`, `bow`, `headphones`, `scarf`. Cada uno es un modelo aparte (copia del base + cubos nuevos en el hueso `head`) que usa la misma textura (los accesorios están pintados en la zona libre de abajo). Se combinan con el shiny.
  `/givepokemon <jugador> gatitoalien accessory=sunglasses` (añadir `shiny` para la versión dorada). Datos: `species_features/accessory.json`, asignación y resolver `1_michi_dramatico_accessories.json`. Requiere reiniciar el servidor.

## Peluches de todos los Pokémon Emi
Un bloque de Pokeblocks por cada especie Emi (21) además del GatitoAlien: `/give @s emi_plush:<especie>_emi` (p. ej. `emi_plush:blissey_emi`). Aparecen en la pestaña **Pokeblocks - Misc**; se colocan mirando hacia el jugador y se recogen al romperlos.
- `gen/gen_dolls.py` los genera a partir de los `*_emi.geo.json`: modelo reducido (sin huesos ocultos como párpados o boca abierta, UV por cara), textura, blockstate, modelos de bloque y objeto, loot table, textos (es/en) y las clases Java (`src/emi/plush/gen/Doll_*.java`, `DollRegistry.java`). Proporciones de peluche (`CHIBI`: cabeza más grande y cuerpo más pequeño) y poses (`BRAZOS_ABAJO`, `POSE_CAIDA`: brazos de la línea Ralts, Clefairy/Clefable/Wigglytuff y las cintas de Sylveon se dejan caer porque el modelo base está en T).
- `EmiDollModel`/`EmiDollItemModel` cargan `geo/<nombre>.geo.json` y `textures/entity/<nombre>.png`. `build.sh` ejecuta todos los generadores antes de compilar.

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
