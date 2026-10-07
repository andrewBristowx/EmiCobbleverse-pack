# Emi Plush (peluches propios, CC0)

Mod pequeño que añade **peluches propios** al pack usando las clases de **Pokeblocks** (`PokedollBlock`, `PokedollBlockEntity`, `PokedollBlockItem`
y el renderizador de GeckoLib), así que se colocan, se ven y se llevan en la mano igual que los pokedolls.

Peluches:
- `emi_plush:michi_dramatico` — **Peluche Michi Dramático** (el gatito de Extacy: blanco, antenas moradas, barriga negra con luna, cara de enfadado).
  Está en el inventario creativo, pestaña **Pokeblocks - Misc**, y se da con `/give @s emi_plush:michi_dramatico`. Al romperlo se suelta a sí mismo.
  No tiene receta ni aparece en cofres: es un objeto de administrador.

Hace falta en **servidor y clientes** (añade un bloque). Depende de `pokeblocks` y `geckolib`, que ya están en el pack.

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
