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
- **Habilidad Drama** (`data/emi_plush/abilities/drama.js`, es la habilidad normal del GatitoAlien sin accesorio; la oculta es Gran Encanto; en las formas con rol Drama pasa a ser la oculta): los bostezos de este Pokémon son dobles. **Bostezo alcanza a todos los rivales adyacentes** y **el sueño que provoca dura el doble**. Probada con el simulador de combate de Cobblemon (`test/probar_drama.js`). OJO: Cobblemon une las líneas del archivo con espacios, así que el `.js` **no puede llevar comentarios `//`** (un solo `//` impide que arranque el servidor); el script de prueba lo comprueba.
- Descripción de Pokédex: «la mascota oficial del canal de Emi… muy escandalosa y dramática». Tiene entrada de Pokédex (`dex_entries`, `dexes/emi.json`, añadida a la nacional).
- **No aparece en la naturaleza**: se da con `/givepokemon <jugador> cobblemon:gatitoalien` (o `/spawnpokemon cobblemon:gatitoalien`). Si se quiere en el mundo hay que añadir un `spawn_pool_world`.
- Depende de `cobblemon` (>= 1.7).
- Para probar una animación en bucle: `MICHI_DEBUG=<animacion> ./build.sh`. Retrato y vista de perfil se ajustan con `MICHI_PS/PX/PY` y `MICHI_FS/FX/FY` (ver `poser()`).

## Pokémon de Emi (aspecto `emi`) — 21 especies
Variante cosmética con los colores y el pelo de Emi (no lleva su ropa): melena **plateada** conectada (tapa, paneles laterales y trasero, flequillo con raya al medio, mechones que cuelgan de los paneles y tres tiras atrás; todo se solapa, nada flota) con **puntas rosas**; la línea Ralts tiene el pelo plateado en lugar de verde, ojos de **dos colores** (naranja y magenta), negro y rosa en el cuerpo.
Líneas: **Happiny/Chansey/Blissey**, **Cleffa/Clefairy/Clefable**, **Igglybuff/Jigglypuff/Wigglytuff**, **Eevee y sus 8 evoluciones** (Vaporeon, Jolteon, Flareon, Espeon, Umbreon, Leafeon, Glaceon, Sylveon) y **Ralts/Kirlia/Gardevoir**.
- Happiny, Chansey y Blissey están ajustadas a mano en `gen/gen_emi_poke.py`; el resto las genera el motor de `gen/emi_species.py` (busca la cabeza por los ojos, retiñe con k-means conservando el relieve y añade cubos de pelo; la tabla `CONFIG` fija los colores de cada especie). `gen/gen_all.py` lo lanza todo.
  Necesita `COBBLEMON_JAR=<jar de Cobblemon>` (y numpy); sin esa variable `build.sh` usa lo ya generado en `resources/`.
- Es un **aspecto** (`species_features/emi.json` + `species_feature_assignments/emi.json`, indicador `emi`), no una especie nueva: evoluciona, se cría y combate como la normal. Resolvers `bedrock/pokemon/resolvers/*/1_*_emi.json`; también hay versión **brillante**.
  Al añadir especies nuevas a la lista hay que **reiniciar el servidor** (un `/reload` no recoge las asignaciones de aspectos).
- **Cómo conseguirlos**: `/givepokemon <jugador> <especie> emi=true`, `/spawnpokemon cobblemon:blissey emi=true` para uno salvaje.
- Los huesos nuevos (`hair_*`, `bangs`) cuelgan de la cabeza/torso y se mueven con ellos; no tienen animación propia.

## GatitoAlien: formas Pokémon (accesorios y dorado)
`gen/gen_michi_extras.py` (se ejecuta tras `gen_pokemon.py`), `gen/michi_forms.py` (lista de formas) y `data/cobblemon/species/custom/gatitoalien.json` (`forms`, se reescribe al generar).
- **25 formas** (salen en el Pokédex y en el nombre, p. ej. "GatitoAlien (Crown)"): `Gold`; 12 con accesorio — `Sunglasses`, `BeachHat`, `Crown`, `Bow`, `Headphones`, `Scarf`, `Halo`, `DevilHorns`, `WitchHat`, `SantaHat`, `FlowerCrown`, `Pirate`; y las 12 doradas `Gold<Nombre>` (p. ej. `GoldPirate`).
  Cobblemon se queda con la ÚLTIMA forma cuyos aspectos estén todos presentes, por eso las combinadas van al final de la lista.
- Cómo se activan (aspectos): `gold=true` y/o `accessory=<sunglasses|beach_hat|crown|bow|headphones|scarf|halo|devil_horns|witch_hat|santa_hat|flower_crown|pirate>`.
  `/givepokemon <jugador> gatitoalien accessory=halo gold=true`. Se pueden combinar con `shiny` (la dorada con shiny vuelve a los colores normales; la normal con shiny es dorada).
  OJO: `form=Gold` en `/spawnpokemon` no funciona (Cobblemon solo elige la forma por aspectos); usar los aspectos de arriba.
- Cada accesorio es un modelo aparte (copia del base + cubos nuevos en el hueso `head`) que usa la misma textura (los accesorios están pintados en la zona libre de abajo). Datos: `species_features/{accessory,gold}.json`, sus asignaciones y el resolver `1_michi_dramatico_accessories.json`. Requiere reiniciar el servidor.
### Roles, tipos y habilidades de las formas (v1.9.0)
Cada forma es **Hada + otro tipo**, con estadísticas orientadas a un papel (todas suman 470, como el GatitoAlien normal), una **habilidad propia** con su temática y movimientos de su tipo; la habilidad oculta de todas es **Drama**. Las doradas con accesorio (`GoldX`) usan el rol de X. Se definen en `gen/michi_roles.py` (tipos, estadísticas, movimientos, textos y los scripts de Showdown de las habilidades), que `gen_michi_extras.py` vuelca en la especie (`forms`), en `data/emi_plush/abilities/<id>.js` y en los textos.

| Forma | Tipo 2 | Rol | PS/Atq/Def/AtqE/DefE/Vel | Habilidad |
|---|---|---|---|---|
| Gold (dorado) | Acero | Muro físico | 85/65/115/55/100/50 | **Toque de Midas**: el contacto baja 1 la Velocidad del atacante; +1 Def al entrar |
| Sunglasses | Siniestro | Físico veloz | 60/105/55/70/60/120 | **Gafas Chulas**: crítico +1, no se le baja la precisión, ignora la evasión |
| BeachHat | Agua | Especial de agua | 70/50/65/110/85/90 | **Día de Playa**: Agua +30%; con sol o lluvia cura 1/16 por turno |
| Crown | Dragón | Mixto robusto | 90/80/80/80/80/60 | **Soberano**: con PS completos recibe -25% de daño; inmune a Mofa, Tormento y Otra Vez |
| Bow | Normal | Curandero | 100/50/80/70/110/60 | **Cariño**: cura 1/16 de los PS de sí mismo y de sus aliados cada turno |
| Headphones | Eléctrico | Especial de sonido | 65/50/60/120/80/95 | **Ritmo Eléctrico**: sus movimientos de sonido pasan a Eléctrico y +20% |
| Scarf | Hielo | Físico robusto | 95/105/90/45/65/70 | **Abrigo**: Fuego e Hielo le hacen la mitad; inmune a quemadura y congelación |
| Halo | Volador | Muro especial | 80/45/75/105/105/60 | **Bendición**: se cura cualquier estado cada turno; Siniestro y Fantasma le hacen la mitad |
| DevilHorns | Fuego | Cañón de cristal mixto | 55/115/50/115/50/85 | **Pacto Infernal**: +30% de daño, pero cada ataque le cuesta 1/10 de sus PS |
| WitchHat | Fantasma | Especial de maleficios | 70/50/60/115/90/85 | **Maleficio**: +40% de daño a objetivos con algún estado alterado |
| SantaHat | Psíquico | Especial defensivo | 95/50/85/95/95/50 | **Lluvia de Regalos**: cada turno 1/3 de probabilidad de subir 1 una característica al azar |
| FlowerCrown | Planta | Tanque regenerador | 100/50/100/70/100/50 | **Floración**: cura 1/16 por turno (1/8 con sol); inmune a polvos y esporas |
| Pirate | Lucha | Físico saqueador | 80/120/80/45/55/90 | **Saqueo**: al derrotar a un Pokémon +1 Ataque y cura 1/4 de sus PS |

- **Todo en español (v1.9.1)**: los nombres de las habilidades se muestran en español también **dentro del combate** (Cobblemon escribe «¡<habilidad> de <Pokémon> activado!» con el nombre tal cual lo manda el simulador, sin traducirlo, así que el nombre del script tiene que ser el español). Por eso el id de cada habilidad es `toID(nombre)` (Showdown lo deduce del nombre y no pueden diferir): `toquedemidas`, `gafaschulas`, `dadeplaya` (Día de Playa), `soberano`, `cario` (Cariño), `ritmoelctrico`, `abrigo`, `bendicin`, `pactoinfernal`, `maleficio`, `lluviaderegalos`, `floracin`, `saqueo`; `gen/michi_roles.py::abid` lo calcula y `test/probar_habilidades.js` comprueba que coincidan. Los nombres de las formas en el Pokédex (`cobblemon.ui.pokedex.info.form.<forma>`) también tienen texto (Dorado, Con Gafas de Sol…), y los textos se copian a es_ar, es_cl, es_ec, es_uy y es_ve además de es_mx/es_es.
- Los textos de cada habilidad y la entrada de Pokédex de cada forma salen de `lang/{en_us,es_es,es_mx}.json` (`cobblemon.ability.<id>`, `emi_plush.species.gatitoalien_<forma>.desc`).
- Pruebas: `SHOWDOWN=<carpeta showdown del servidor> node test/probar_habilidades.js` ejecuta 37 comprobaciones contra el simulador de Cobblemon (cada habilidad hace lo que dice: +30%, x0,5, curaciones…, y que los `.js` no tengan comentarios `//`). Además, un combate real en el juego con la forma Pirate arrancó con la habilidad `plunder` en el equipo.
- El GatitoAlien normal (sin accesorio) sigue siendo Hada puro con Drama y 70/90/60/80/65/105.
- Probado en el juego (v1.8.2): las 25 formas se ven bien, se pueden sacar, combatir (ganó un combate real) y el aspecto `emi` se conserva al evolucionar (Ralts → Kirlia). La corona dorada pasa a plata sobre el pelaje dorado para que se vea.
- Para añadir otro accesorio: una entrada en `ACCESORIOS` (gen_michi_extras.py) y otra en `ACC` (michi_forms.py); forma, resolver, peluches y textos salen solos.

## Comandos rápidos (operador)
```
/givepokemon <jugador> eevee emi=true            # cualquier especie Emi (21), con shiny: añadir  shiny
/givepokemon <jugador> gatitoalien accessory=halo gold=true
/give <jugador> emi_plush:eevee_emi               # peluche Emi
/give <jugador> emi_plush:gatitoalien_gold_halo   # peluche de una forma del GatitoAlien
```
Si `/givepokemon` se escribe desde la consola hace falta `execute as <jugador> run givepokemon <especie> ...` (pide un jugador).

## Peluches de todos los Pokémon Emi y de las formas del GatitoAlien
Un bloque de Pokeblocks por cada especie Emi (21), por cada forma del GatitoAlien (25: `gatitoalien_gold`, `gatitoalien_<accesorio>`, `gatitoalien_gold_<accesorio>`) y el peluche original del GatitoAlien: `/give @s emi_plush:<especie>_emi` (p. ej. `emi_plush:blissey_emi`). Aparecen en la pestaña **Pokeblocks - Misc**; se colocan mirando hacia el jugador y se recogen al romperlos.
- `gen/gen_dolls.py` los genera a partir de los `*_emi.geo.json` (v1.8.1: los huesos ocultos conservan su sitio en el árbol sin cubos, así los ojos no se pierden al agrandar la cabeza; `mirror` de Bedrock se traduce a UV volteado; la pose de reposo de Cobblemon se aplica con `gen/poses/<especie>.json`, que saca `gen/gen_poses.py`): modelo reducido (sin huesos ocultos como párpados o boca abierta, UV por cara), textura, blockstate, modelos de bloque y objeto, loot table, textos (es/en) y las clases Java (`src/emi/plush/gen/Doll_*.java`, `DollRegistry.java`). Proporciones de peluche (`CHIBI`: cabeza más grande y cuerpo más pequeño) y poses (`BRAZOS_ABAJO`, `POSE_CAIDA`: brazos de la línea Ralts, Clefairy/Clefable/Wigglytuff y las cintas de Sylveon se dejan caer porque el modelo base está en T).
- Los de las formas del GatitoAlien salen de los modelos de Pokémon `michi_dramatico_<accesorio>.geo.json` reducidos a 1/4 (igual que el peluche original) con la textura normal o la dorada (`build_michi` en gen_dolls.py).
- `EmiDollModel`/`EmiDollItemModel` cargan `geo/<nombre>.geo.json` y `textures/entity/<nombre>.png`. `build.sh` ejecuta todos los generadores antes de compilar.

## Gasha Especial de Eventos (v1.10.0; ticket nuevo en la v1.10.1)
Objeto `emi_plush:ticket_gasha_michi` («Ticket Gasha Especial de Eventos», textura 32×32 con el michialien). **Clic derecho = 1 tirada; agachado = hasta 10 de golpe.** Cada tirada gasta un ticket y entrega:

| Casillas (de 1000) | Premio |
|---|---|
| 40 % | Objetos útiles comunes (Poké Balls, pociones, comida, hierro, oro, caramelos S) |
| 25 % | Objetos útiles buenos (Ultra Balls, hiperpociones, caramelos M/L, diamantes, esmeraldas, fichas de casino, ticket gacha) |
| 9 % | Objetos épicos (caramelos raros, caramelos XL, Restaurar todo, PP Máx, cápsula/parche de habilidad, piedras evolutivas, tickets de tesoros y cosméticos, netherita, Amuleto Shiny, Master Ball…) |
| 10 % | Pokémon del mod de Emi (`emipokemon:*`: Unown 6/7/67, Meowstic, Zoroark, Meowscarada, Eevee Gatito, Snorlax Emi; Espurr, Zorua, Sprigatito y Floragato se quitaron, ver abajo) |
| 7 % | Peluche suelto (los 21 Emi, las 25 formas del GatitoAlien y el Michi original) |
| 7 % | Pokémon suelto: los 21 Pokémon Emi (`emi=true`) y las 25 formas del GatitoAlien |
| 2 % | **Pareja**: Pokémon + su peluche |

**Garantía:** si llevas 10 tiradas sin pareja, la décima es una pareja (se muestra en la barra de acción «n/10»). Cualquier pareja reinicia la cuenta. Las especies más raras pesan menos: GatitoAlien dorado con accesorio < GatitoAlien con accesorio < Pokémon Emi evolucionados < intermedios < bebés. Los premios gordos se anuncian a todo el servidor.

Todo está en funciones de datos (`data/emi_plush/function/gasha/*.mcfunction`), generadas por `gen/gen_gasha.py`: ahí se editan listas, pesos, niveles y casillas (`tirada.mcfunction`). Comandos de operador:
```
/give <jugador> emi_plush:ticket_gasha_michi 10
/execute as <jugador> run function emi_plush:gasha/tirar       (una tirada sin ticket)
/scoreboard players get <jugador> emi_gasha                     (cuenta hacia la garantía)
/scoreboard players set <jugador> emi_gasha 9                   (la siguiente es la pareja)
```
Los Pokémon se entregan con `givepokemon` (van al PC si el equipo está lleno).

**Espurr, Zorua, Sprigatito y Floragato de Emi no se dan (v1.12.0).** Sus evoluciones están escritas con espacio de nombres en los datos de Emipokemon (`"result": "emipokemon:meowstic_emi"`) y Cobblemon 1.7.3 (`Pokemon.setGmaxFactor` → `PokemonSpecies.getByName`) lanza `InvalidIdentifierException` al volver a leerlos de los datos guardados: en el log sale «Failed to read a pokémon» y esos Pokémon no se cargan al reconectar. Pasa con cualquier origen, no solo con el Gasha. Si Emipokemon corrige sus datos, se pueden volver a añadir a `MOD` en `gen/gen_gasha.py`.

## Máquina Gasha Especial de Eventos (v1.11.0) y su pantalla (v1.12.0)
Bloque `emi_plush:maquina_gasha_michi` («Máquina Gasha Especial de Eventos», pestaña Pokeblocks-Misc), de dos bloques de alto (la mitad de arriba es `maquina_gasha_michi_arriba`, invisible). Usa la geometría y las animaciones GeckoLib de la máquina Emi de Emipokemon (`emipokemon:geo/emi_gacha_machine.geo.json`, `animations/standard_gacha_machine.animation.json`) con una textura propia (`textures/block/maquina_gasha_michi.png`, la de Emi con los carmesí pasados a morado y las cremas a lila; la genera `gen_gasha.py` leyendo el jar de Emipokemon de `mods/`). Si se quita Emipokemon el modelo no carga. Al romperla suelta ella misma; la mitad de arriba no suelta nada. `/give <jugador> emi_plush:maquina_gasha_michi`; no tiene receta. El ícono (`textures/item/maquina_gasha_michi.png`) es una captura recortada de la máquina real; `gen_gasha.py` no la pisa si ya existe.

**Pantalla (v1.12.0).** Clic derecho en la máquina abre la pantalla `GashaPantalla` (cliente). Los tickets se cuentan y gastan de **todo el inventario** (ya no hace falta llevarlo en la mano). Botones «TIRAR x1» y «TIRAR x10» (10 tickets, 10 tiradas); se desactivan si no hay tickets. Muestra la garantía (n/10), los porcentajes, el último premio y la Pokébola de michialien en el domo.
- **Animación normal** (~1,6 s; 1,15 s por tirada en x10): la bola se sacude, destello y la carta con el premio. **Premio mayor** (~4 s): la pantalla se oscurece, la bola se vuelve dorada, vibra con anillos, columna de luz con rayos, destello blanco y carta dorada «¡¡PREMIO MAYOR!!» con lluvia de estrellas y huellas. Es premio mayor: pareja (Pokémon + peluche), cualquier premio con «Dorado» en el nombre, Master Ball y Amuleto Shiny. Un clic o espacio/intro salta la animación; ESC cierra.
- **La carta del premio** enseña el objeto (icono y cantidad), el modelo 3D del Pokémon (el de Cobblemon, con sus aspectos: forma Emi, dorado, accesorio) y el peluche; en una pareja salen los dos con un «+».
- **Cómo funciona:** `GashaPantalla` (dibujo y animación), `GashaServidor` (valida: cerca de la máquina, tickets suficientes; gasta los tickets y ejecuta `emi_plush:gasha/tirada` por cada tirada) y los paquetes `GashaPeticion` (cliente→servidor) y `GashaRespuesta` (servidor→cliente), con JSON dentro de una cadena. Qué premio salió lo guardan las funciones en el almacenamiento `emi_plush:gasha` → `res` (`pool` + `sel` con el nombre y los comandos); el servidor lo convierte en objetos / Pokémon y el cliente lo dibuja. Los premios se entregan en el momento de tirar, aunque se cierre la pantalla durante la animación.
- **Imágenes:** `gen/gen_gasha_gui.py` genera el fondo y los sprites (`textures/gui/gasha/`); el dibujo del michialien con traje (`emi.png`) está en el repositorio y el generador no lo toca. Los textos los pone el juego, no vienen en la imagen. El lienzo es de 1536×1024 y se escala a la ventana.
- **Sin probar:** servidor real con varios jugadores a la vez, ventanas de tamaños muy distintos al de prueba (1280×720), idioma que no sea español (los textos de la pantalla están escritos en español en el código).

## Revisión de modelos: UV enteros y z-fighting (v1.9.3)
`python3 test/revisar_modelos.py` (Pokémon) y `python3 test/revisar_modelos.py --peluches` (peluches) se ejecutan a mano antes de empaquetar y fallan si encuentran:
- **UV con decimales** en un modelo de Pokémon. Cobblemon no carga ese modelo (`Expected an int but was 227.5` en el log del cliente) y la forma se ve con el modelo de antes o sin accesorio. Ocurre cuando un cubo nuevo tiene un tamaño con decimales, porque el hueco que se reserva en la textura deja de ser entero y todos los UV siguientes se descuadran. `gen_michi_extras.cube()` rechaza tamaños con decimales; las posiciones (`origin`) sí pueden tenerlos.
- **z-fighting**: caras de dos cubos en el mismo plano, mirando al mismo lado y solapadas (rayas blancas parpadeantes, como las del sombrero pirata). Los accesorios del Gatito se generan con al menos 0,3 unidades de separación y `gen_michi_extras.py` lo comprueba al generar. Las piezas nuevas de las especies Emi (pelo) se engordan con `inflate` en `emi_lib.sin_zfight`; cuando chocan dos piezas nuevas solo engorda la que se añadió después (si engordaran las dos seguirían en el mismo plano). Las caras iguales que ya traen los modelos originales de Cobblemon (torso/torso2 del Eevee, etc.) no se tocan, y los párpados y bocas alternativos nunca se muestran a la vez.
- Cambios de la v1.9.3 en el Gatito: los auriculares ahora van por fuera de la cabeza (que mide ±20 unidades), solo las almohadillas rosas con su tapa negra y sin el arco; antes quedaban dentro de la cabeza y no se veían. La corona (v1.9.4) es un aro de oro con puntas por delante, detrás y a los lados y gemas rosas; en la forma dorada pasa a plata con las gemas rosas. En combate real se comprobó que «Toque de Midas» se anuncia con su nombre en español al entrar el Pokémon; el resto de habilidades se comprueban con el simulador (`test/probar_habilidades.js`, 37 comprobaciones).

## Cómo se hizo el modelo
`gen/gen_michi.py` genera el modelo (`assets/emi_plush/geo/michi_dramatico.geo.json`) y la textura (`textures/entity/michi_dramatico.png`) a partir de una lista de cubos:
cada cara tiene su propio rectángulo en el atlas (6 px por unidad) y los colores salen de una función por posición, así que las caras siempre coinciden.
Para cambiar el aspecto se editan `CUBOS` y `pintar()` en ese script y se vuelve a ejecutar `build.sh`.

## Compilar
`build.sh` genera todo, **ejecuta `test/revisar_modelos.py` (y se detiene si hay UV con decimales o z-fighting)** y compila contra los jars reales (nombres *intermediary*, como el resto de mods del pack); necesita variables de entorno:
`MC` (client-intermediary.jar de Fabric Loader), `GECKO`, `POKEBLOCKS`, `FAPI` (carpeta con los jars internos de Fabric API), `LIBS` (librerias de Minecraft y fabric-loader), `OUT`.

## Para añadir otro peluche
Copia `MichiBlockEntity`/`MichiBlockModel`/`MichiItemModel`, registra el bloque, el objeto y el tipo de entidad en `EmiPlush`, el renderizador en `EmiPlushClient`,
y añade blockstate, modelo de bloque y de objeto, loot table, textos de idioma y su geo+textura.

**Eevee Emi (v1.12.1).** Su pelaje original es marrón y el grupo de color más oscuro (21 % de la textura) se pintaba de negro: salían manchas negras y la cara se perdía. Ahora todo el pelaje es plata y solo la crema (pecho, punta de la cola) va en rosa pálido (`CONFIG['eevee']` en `gen/emi_species.py`). Ojo con esto al añadir especies: si el grupo más oscuro ocupa mucho, no lo pintes de `BLACK`.
