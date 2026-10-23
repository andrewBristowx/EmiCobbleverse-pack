# Café Michialien (`emi-cafe`) — PLAN (todavía no hay código)

Pedido del dueño (2026-10-23): «un juego como **Pokémon Café** en Minecraft: un restaurante donde cada cierto tiempo entran Pokémon y te piden comida; los platos se
preparan **llevando los ingredientes en las manos (no en el inventario) a la estación que toca** (una papa a la freidora, etc.); al empezar eliges **1 de 3 Pokémon**
y ese te enseña **1 plato**; con el dinero de las ventas **mejoras las instalaciones, compras más platos, desbloqueas ingredientes y estaciones de trabajo**;
y **todos los objetos y estaciones con textura propia**».

Este documento es el plan. Nada de esto está construido ni probado todavía.

## 1. Qué es el juego (resumen)
Un **turno** dura ~4 minutos. Llegan Pokémon clientes, se sientan, piden un plato (aparece su icono en un globo y en tu lista de pedidos) y tienen una **barra de paciencia**.
Tú cocinas, llevas el plato a la mesa, cobras, recoges el plato sucio, lo lavas… Al acabar el turno ves el resumen (ventas, propinas, reputación) y gastas el dinero en mejoras.
Es un juego tipo *Overcooked* en cuanto a mecánica (llevar cosas de una estación a otra) y tipo *Pokémon Café* en cuanto a tema (clientes Pokémon, ayudantes Pokémon, ingredientes con bayas).

### Bucle de un turno
1. Empieza el turno (cuenta atrás, música). La puerta se abre.
2. **Entra un cliente** (Pokémon real de Cobblemon, sin IA, que camina por una ruta fija hasta una mesa libre y se sienta/queda de pie).
3. **Pide**: sobre su cabeza aparece un globo con el icono del plato; el pedido también sale en la lista de la pantalla con su barra de paciencia.
4. **Cocinas**: coges ingredientes de las cajas, los llevas a las estaciones (cortar, freír, hervir, hornear…), los juntas en la mesa de emplatar.
5. **Sirves**: con el plato en las manos haces clic derecho en el cliente (o su mesa). Come, **paga** (más si fue rápido o si era su plato favorito) y se va; deja el plato sucio.
6. **Lavas** el plato sucio en el fregadero (si no hay platos limpios no puedes servir: esto es lo que hace que haya que organizarse).
7. Fin del turno → resumen → menú de mejoras.

### Llevar cosas en las manos (sin inventario)
- Llevas **1 objeto a la vez** (mejora «bandeja»: 2). Se ve **flotando delante de tus manos** (un `item_display` que el mod mueve cada tick, igual que la bomba de *emi-fall*, que ya está probada).
  La barra de objetos de Minecraft **no se toca** ni se llena: dentro del café el inventario queda guardado y se devuelve al salir (como en survivors/carreras).
- **Clic derecho con las manos vacías** en una caja o estación = coger. **Clic derecho cargando algo** en una estación = dejarlo. **Agacharse + clic** en la basura = tirar.
- Cada estación enseña una **barra de progreso** y suena; si te olvidas la comida en la freidora se **quema** (avisa antes con humo y un sonido).

## 2. Estaciones (todas con modelo y textura propios)
| Estación | Qué hace | Ejemplo |
|---|---|---|
| **Cajas de ingredientes** (infinitas) | dan un ingrediente | papa, baya, lechuga, harina… |
| **Tabla de cortar** | corta (2-3 s) | papa → papa en tiras; lechuga → picada |
| **Freidora** | fríe (6 s, se quema si te olvidas) | papa en tiras → papas fritas |
| **Plancha** | cocina a la plancha | carne → carne hecha; pan → tostada |
| **Olla** | hierve varios ingredientes juntos | sopa, pasta, arroz |
| **Horno** | hornea (lento) | pizza, tartas |
| **Batidora** | mezcla líquidos y bayas | batidos, limonada |
| **Cafetera / tetera** | bebidas calientes | café con leche, té de bayas |
| **Mesa de emplatar** | junta los componentes en un plato limpio | hamburguesa completa |
| **Fregadero** | lava platos sucios → limpios | — |
| **Basura** | tira lo que sobra | — |
| **Caja registradora / buzón de pedidos** | cobra y muestra pedidos | — |
Empiezas con pocas (cortar, freír o la que toque según tu inicial, emplatar, fregadero) y el resto se **compran**.

## 3. Empezar: elegir 1 de 3 Pokémon (propuesta)
Al entrar por primera vez, **Ms. President** (la michi ejecutiva de los otros minijuegos) te presenta a tres Pokémon cocineros; el que elijas **te enseña tu primer plato** y se queda en el café
como **mascota/ayudante** (mejora posterior: trabaja solo en «su» estación).
- **Charmander** → **Papas fritas** (papa → tabla de cortar → freidora).
- **Squirtle** → **Limonada de bayas** (baya + agua → batidora).
- **Bulbasaur** → **Ensalada de bayas** (lechuga + baya → tabla de cortar → plato).
(Son propuestas: se pueden cambiar por los que quieras.) Quien elige otro Pokémon aprende otro plato, así que dos jugadores no tienen el mismo café al principio.

## 4. Platos, ingredientes y progresión (propuesta, ~20 platos)
El café tiene **niveles de reputación (1-5)**; subir de nivel abre platos, estaciones e ingredientes nuevos, que **se compran con dinero**.
- **Nivel 1:** tu plato inicial + 2 platos básicos (los de arriba: papas fritas, limonada, ensalada).
- **Nivel 2:** tostadas con mermelada, sopa de verduras, hamburguesa sencilla, té de bayas.
- **Nivel 3:** pizza Pokéball (horno), batido de bayas, pasta con queso, sándwich Michialien, huevos revueltos.
- **Nivel 4:** curry de Snorlax (ración gigante), tarta de bayas, helado, café con leche michi, croquetas.
- **Nivel 5:** platos especiales/legendarios (banquete de Emi, «el plato de la Presidenta»…).
Ingredientes (~25): papa, bayas (oran, pecha, aranja…), lechuga, tomate, zanahoria, carne, pan, queso, huevo, leche, harina/masa, arroz, setas, azúcar, hielo, café en grano, hierbas…
Cada plato tiene una **receta fija** (qué ingredientes, qué pasos y en qué orden) que se ve en el **recetario** del menú.

## 5. Clientes Pokémon
- Especies reales de Cobblemon (sin IA, movidas por el mod; ya hecho en *emi-survivors*): al principio los comunes (Rattata, Pidgey, Eevee, Pikachu, Slowpoke), y con la reputación llegan otros.
- Cada especie tiene **gustos** (por tipo: los de fuego aman lo picante, los de agua lo fresco…): si les sirves su favorito pagan más y dan propina.
- Rarezas: **Snorlax** (pide raciones enormes y paga mucho), **Meowth** (propina grande), **Gengar** (solo viene de noche/turnos especiales), **inspector** (visita de Ms. President: premio gordo si sale todo bien).
- **Paciencia** de 40-90 s según la especie; si se acaba se van sin pagar y bajan la reputación.
- Dificultad creciente: más clientes a la vez y pedidos más complicados según el nivel.

## 6. Dinero, mejoras y tienda
- **Monedas del café** (se ganan por ventas y propinas) → se gastan en **mejoras del café**: velocidad de cada estación (3 niveles), más **mesas** (de 2 a 8), **paciencia extra**, **bandeja** (llevar 2 cosas),
  **ayudantes Pokémon** (uno por estación), estaciones nuevas, platos nuevos, ingredientes nuevos, **decoración Michialien** (cambia el aspecto del local).
- **Reputación (⭐)** = nivel del café (no se gasta): sube con clientes contentos y baja con los que se van enfadados.
- **Conexión con el resto del servidor** (mismo patrón que carreras/huida/fall, a confirmar con el dueño): al acabar cada turno se ganan **estrellas del café** que se cambian en una **tienda** por tickets de Emipokemon
  y objetos Pokémon (entrega por `/correo`), más un **peluche exclusivo** (propuesta: *Michialien chef*, con gorro de cocinero, accesorio nuevo de `emi-plush`) por 1000 estrellas, una vez por jugador.
- **Misiones** del día/semana («sirve 10 papas fritas», «haz un turno sin quemar nada») y **logros**.

## 7. Cómo se hace por dentro (técnica)
**Mod nuevo `emi-cafe`** (servidor + cliente, igual que `emi-fall`/`emi-huida`; Gradle/Loom como ellos). Reutiliza lo que ya está probado en los otros minijuegos:
| Pieza | De dónde sale |
|---|---|
| Sala de entrada, menú propio (Lienzo/MenuPantalla), pestañas, guardado por jugador en JSON | `emi-fall`/`emi-huida` |
| Estrellas, tienda, misiones, logros, entrega por `/correo` | `emi-fall`/`emi-huida` |
| Un café por jugador/equipo en su propia zona de la dimensión, construido por código | arenas de `emi-survivors` |
| Pokémon de Cobblemon sin IA que caminan por una ruta | enemigos de `emi-survivors` |
| Objeto que «flota» en tus manos (display que el mod mueve cada tick) | bomba de `emi-fall` |
| Globos de texto/iconos sobre la cabeza | michi ángel de `emi-huida` |
| Botón en Ms. President («Café») | los botones de las otras salas |
| Música y sonidos propios generados con Python | `emi-fall`/`emi-survivors` |

**Cosas nuevas que habrá que hacer** (y que no están probadas): rutas de los clientes dentro del café (puntos fijos; sentarse), estaciones con estado (qué hay dentro, progreso, quemado), recetas como datos,
la lista de pedidos en pantalla, las mejoras que cambian el local, y todo el equilibrio (tiempos, precios, paciencia: serán **estimaciones** hasta jugarlo con gente).

## 8. Texturas y modelos propios (todo lo que se ve)
Por pedido del dueño **todo tiene textura**: ingredientes, platos, platos sucios, estaciones, iconos de la interfaz.
- **Objetos** (~25 ingredientes + ~25 estados de ingrediente/platos + platos/cubiertos + dinero ≈ **60-80 iconos de 16×16**) como **objetos registrados del mod** con su modelo y textura.
- **Estaciones** (~12) como **bloques con modelo 3D propio** (JSON de cubos generado por código, como los peluches de `emi-plush`) con su textura.
- **Interfaz**: iconos de pedidos, monedas, estrellas, barra de paciencia.
- **Cómo se hacen:** con **scripts de Python** que dibujan el pixel-art (como los 111 iconos de `emi-survivors`), estilo **kawaii/Michialien** con una paleta fija. Se revisan en una **hoja de contacto** (todas las texturas juntas) y en el juego.
  **Limitación honesta:** el pixel-art dibujado por código es sencillo; si el dueño quiere un estilo concreto puede mandar referencias o reemplazar cualquier PNG (los nombres de archivo serán fijos y se documentarán).
- No se copian texturas de otros mods (Farmer's Delight, Cobblecuisine…): aunque están en el pack, el café usa objetos propios para no depender de ellos ni de sus licencias.

## 9. Fases (cada una se prueba en el juego con capturas y se entrega por separado; **no se pasa de fase sin el visto bueno del dueño**)
- **F0 – Prototipo de viabilidad:** una sala con 3 estaciones (caja de papas, tabla, freidora), llevar la papa en las manos, un cliente Pokémon que entra, se sienta, pide con globo y paga. Texturas provisionales. *Sirve para comprobar que el «feeling» es bueno antes de invertir en lo demás.*
- **F1 – Núcleo jugable:** turno completo, cocinar de principio a fin, platos sucios y fregadero, paciencia, quemado, dinero, resumen del turno, 3 platos y las estaciones de F0 + plancha/olla/emplatar. Texturas definitivas de lo que haya.
- **F2 – Empezar y progresar:** entrada con **elegir 1 de 3 Pokémon**, menú (CAFÉ / RECETARIO / MEJORAS / MISIONES / TOP), guardado, mejoras, niveles de reputación, comprar platos/ingredientes/estaciones.
- **F3 – Contenido:** el resto de estaciones, ~20 platos, ~25 ingredientes, más especies y gustos, clientes especiales, ayudantes Pokémon, decoración Michialien. Todas las texturas.
- **F4 – Pulido y conexión con el servidor:** música y sonidos propios, estrellas + tienda + peluche exclusivo, misiones y logros, botón en Ms. President, README, empaquetado en el pack.
- **F5 – Cooperativo y equilibrio:** 2-4 jugadores en el mismo café (repartirse estaciones), equilibrio con gente real, servidor real.

## 10. Decisiones que propongo (el dueño las confirma o cambia)
1. **Un café por jugador** (cooperativo de 2-4 en F5). 2. **Turnos de ~4 min** con resumen (no café abierto sin fin). 3. Iniciales **Charmander / Squirtle / Bulbasaur** con papas fritas / limonada / ensalada.
4. Moneda propia **+ estrellas** que se cambian por tickets y un **peluche Michialien chef**. 5. Estilo **kawaii pixel-art 16×16** con la paleta de Emi. 6. Nombre **«Café Michialien»**, comando `/cafe`, Ms. President como patrocinadora.

## 11. Riesgos conocidos
- Clientes caminando y sentándose sin IA: hay que ver cómo quedan las poses de Cobblemon (sentado/dormido) y la ruta; en survivors solo se probó andar/quieto.
- Muchos objetos/estaciones nuevos = mucho trabajo de texturas (el grueso de F3).
- El equilibrio (tiempos, precios, paciencia) no se puede medir sin jugarlo; serán cifras provisionales.
- Rendimiento con varios cafés a la vez en el servidor real (cada café tendría ~10-20 entidades); se mide en F5.
