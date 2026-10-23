# Café Michialien (`emi-cafe`) — 0.5.0 (8 platos y 10 estaciones nuevos y cocineros con la cara de los Pokémon reales; antes 0.4.0: menú propio como el de los otros juegos; 0.3.0: estrellas, tienda de estrellas con el peluche del Michialien chef, misiones y logros; sobre la 0.2.0)

Pedido del dueño (2026-10-23): «un juego como **Pokémon Café** en Minecraft: un restaurante donde cada cierto tiempo entran Pokémon y te piden comida; los platos se
preparan **llevando los ingredientes en las manos (no en el inventario) a la estación que toca** (una papa a la freidora, etc.); al empezar eliges **1 de 3 Pokémon**
y ese te enseña **1 plato**; con el dinero de las ventas **mejoras las instalaciones, compras más platos, desbloqueas ingredientes y estaciones de trabajo**;
y **todos los objetos y estaciones con textura propia**».

**Decisiones del dueño (2026-10-23):** turno de **4 minutos** que **cierra el local y enseña el resumen de ganancias** con «comprar» o «parar por ahora»; **un café por jugador**, que
**puede invitar a su grupo** (los que estén conectados); los **3 Pokémon iniciales** (Charmander, Squirtle, Bulbasaur) y su plato; **la tienda y el Michialien chef** (peluche) más adelante;
nombre «Café Michialien», comando `/cafe`, y **un botón para unirse en Ms. President**.

## 0d. Novedades de la 0.5.0: más comidas, más estaciones y cocineros «de verdad»
Pedido del dueño: «quiero más comidas y estaciones de trabajo, y el icono hecho como los Pokémon reales pero con sombreros de cocineros».
- **8 platos nuevos** (11 en total; cada uno desbloquea sus cajas y estaciones al comprarlo, precio de venta / coste en la tienda): **Café** 12/90 (granos → cafetera), **Tortilla** 14/100 (huevo → plancha), **Sopa de papas** 19/140 (papa → tabla → olla),
  **Batido de bayas** 20/160 (baya + leche en la mesa de emplatar → batidora), **Panqueques** 22/160 (harina + huevo = masa en la mesa de emplatar → plancha), **Capuchino** 20/200 (café + leche en la mesa de emplatar),
  **Hamburguesa** 30/260 (carne → plancha; harina → horno = pan; carne asada + pan en la mesa de emplatar) y **Pastel** 32/300 (masa → horno). La **masa** sale de harina + huevo y sirve para panqueques y pastel.
- **10 estaciones nuevas** con modelo y textura propios: cajas de **huevos, harina, carne, leche y granos de café**, y las estaciones **plancha, olla, horno, batidora y cafetera**. Van en una **segunda fila de la cocina junto a la barra** (z = 5; los huecos x = 7-8 quedan libres para pasar al comedor; la fila solo se monta si algún plato comprado la usa).
  **El horno se quema** como la freidora (avisa a los 6 s y a los 10 s sale «comida quemada», que se tira a la basura); la plancha no. `Estacion` lleva ahora `z`, `quema()` y `quemado()`; las combinaciones de la mesa de emplatar son una tabla (`Recetas.COMBINACIONES`).
- **18 objetos nuevos** (huevo, harina, carne, leche, granos, masa, pan, carne asada, bayas con leche, los 8 platos y comida quemada) y **10 bloques** nuevos, **todo dibujado por código** (`assets/generar.py`).
- **Menú:** el **recetario** va a dos columnas con **páginas** (6 platos por página, flechas ◀ ▶) y la tienda de platos va de 3 en 3 con las mismas flechas. El logro «Chef variado» pasa a 2/4/7/11 platos aprendidos (premios de 10, 20, 40 y 80 estrellas).
- **Iconos de los cocineros:** ya no son bolitas: `assets/cocineros.py` **dibuja los modelos reales de Cobblemon** (Charmander, Squirtle y Bulbasaur, sin llama ni lianas, con los brazos hacia abajo) **con un gorro de cocinero** de cubos y los reduce a 64×64. Hay que pasarle el jar de Cobblemon (`COBBLEMON_JAR=… python3 assets/cocineros.py --vista X.png`);
  `generar.py` ya no los toca (solo escribe su modelo). **Los modelos y texturas de Cobblemon son de licencia no comercial (CC): estos tres PNG son derivados; no usarlos fuera del pack.**
- **Comando de pruebas nuevo:** `/cafe dev platos` (aprende todos los platos; reconstruye el local).
- **Probado en el juego** (banco ligero, un cliente): los 8 platos cocinados de principio a fin con `dev usar` (el clic derecho simulado), la masa y el pan intermedios, la comida quemada del horno con su aviso, las dos filas de la cocina, el recetario y la tienda con sus páginas (clics reales en las flechas),
  los iconos nuevos en la pantalla de elegir cocinero y en la barra de objetos, un turno abierto con clientes que piden capuchino y hamburguesa y servir un capuchino (monedas). **No probado:** jugar los 8 platos a mano con clic derecho real y dos jugadores, el equilibrio de precios, tiempos y costes (**estimaciones**), la plancha con la sartén y el humo a ojo, sonidos, el servidor real.

## 0c. Novedades de la 0.4.0: el menú es una pantalla propia (como Emi Fall)
Pedido del dueño: «que el menú de Emi Café se vea como el de mis otros juegos». Los cofres de Minecraft se sustituyen por la **pantalla propia** de Emi Fall (`client/MenuPantalla` sobre `client/Lienzo`, lienzo de 640×360 escalado; los datos van por JSON en `net/TextoPayload` y los clics vuelven por `net/AccionPayload`, el servidor lo comprueba todo en `Menus.recibir`). Se abre con la **caja registradora** o con `/cafe menu`. Pestañas: **CAFÉ** (elegir cocinero con tres tarjetas, abrir el turno, parar por ahora, quién está en el café e invitar), **RECETARIO**, **TIENDA** (platos y mejoras con monedas, con barras de nivel), **ESTRELLAS** (la misma disposición que la tienda de Emi Fall, con el peluche chef en la fila dorada), **MISIONES** y **LOGROS** (barras de avance). Al cerrar el turno sale encima el **resumen de ganancias** con COMPRAR / OTRO TURNO / PARAR POR AHORA / SEGUIR EN EL MENÚ (los invitados solo ven ACEPTAR). Se refresca solo cada 1,5 s. **Hace falta el mod también en el cliente** (ya hacía falta por las texturas).
**Probado en el juego** (banco ligero, un cliente real, con clics de ratón): elegir cocinero, las 6 pestañas, comprar un plato (se cobran las monedas y suma el logro «Chef variado»), el resumen tras un turno servido, COMPRAR (lleva a la tienda) y SALIR (devuelve al mundo normal). **No probado** con el menú nuevo: la vista del invitado con dos clientes (el menú del invitado es el mismo con menos botones), el botón INVITAR de la pantalla (sí `/cafe invitar`, probado en 0.2.0), comprar mejoras y artículos de la tienda de estrellas desde la pantalla (la lógica es la de 0.3.0), y otras resoluciones de pantalla.

## 0b. Novedades de la 0.3.0 (y qué se probó)
- **Estrellas** (la moneda de la tienda del servidor, como en carreras/huida/fall): **2 por cliente servido**, **+5 por terminar un turno sirviendo**, **+5 si no se enfadó nadie** (y serviste ≥ 3) y **+5 por batir tu récord** (el primer turno no cuenta como récord), más las de misiones y logros. Cada jugador (dueño o invitado) gana las suyas.
- **Tienda de estrellas** (botón de la estrella en la caja registradora y en el resumen): los **mismos artículos** que las otras tiendas —7 tickets de Emipokemon y 5 objetos de Cobblemon— **máx. 10 de cada uno al día, entrega por `/correo`** (por reflexión a `MailboxService.sendGift`; solo se cobra si el correo lo acepta), y el artículo exclusivo **«Peluche Michialien Chef»** (`emi_plush:gatitoalien_chef`, **1000 estrellas, una vez por jugador**). Los precios son una propuesta del asistente (`Tienda.java`).
- **Misiones:** 3 del día y 2 de la semana (las mismas para todos, salen de la fecha) con premio en estrellas, que se pagan solas al cumplirlas: servir clientes, ganar monedas en un turno, terminar turnos, turno sin enfados, propinas, lavar platos…
- **Logros** con escalones (camarero, jornada completa, pequeño empresario, turno de récord, encantador, todos contentos, chef variado, local de lujo), también pagados en estrellas al alcanzarlos.
- **emi-plush 1.20.0:** aspecto `chef` y el peluche del Michialien chef (ver el README de `emi-plush`).
- Textos nuevos en los datos del jugador (`estrellas`, `misDia`, `logros`, `tiendaCompras`, `unicos`…); admin: `/cafe dev estrellas <n>`; `-Demi.cafe.correo_prueba=true` da por entregado todo sin Emipokemon (solo pruebas).
- **Probado en el juego** (banco ligero con emi-plush 1.20.0): el menú de la caja con la fila nueva (misiones, logros, estrellas), la tienda de estrellas (los objetos de Cobblemon con su dibujo, **comprar el peluche** → «COMPRADO ✔» y el cobro de 1000, entrega con el correo de prueba), los menús de misiones y logros, y un turno servido de verdad: el resumen enseña «Estrellas ganadas: 17» y el saldo sube; el peluche y el Pokémon con el uniforme de chef se ven bien de frente.
- **No probado:** la entrega real por `/correo` con Emipokemon (los tickets salen con un icono de estrella de repuesto cuando Emipokemon no está), el cambio real de día o de semana en las misiones, que todas las misiones y escalones de logros se cumplan jugando de verdad (solo se vio el primer logro), el servidor real y el equilibrio de las cifras (estrellas por cliente, premios y precios son estimaciones).

## 0. Qué había en la 0.2.0 (y qué se probó)
Mod **servidor + cliente** (el cliente lo necesita por las texturas de objetos y bloques; los menús son cofres de Minecraft con objetos propios, sin pantallas de cliente). Dimensión `emi_cafe:cafe`.
- **Entrar:** `/cafe entrar` (o el botón «Café Michialien» de Ms. President, emi-survivors 0.29.0). Se guarda lo tuyo (inventario, sitio, modo…) y se devuelve al salir (`/cafe salir`, el botón «Parar por ahora» o al desconectarte; si se cae el servidor, al volver a entrar).
  Cada dueño tiene **su local** (se construye por código, 64 bloques de separación entre locales).
- **Primera vez:** menú «¿Quién será tu cocinero?» con **Charmander** (papas fritas), **Squirtle** (limonada de bayas) o **Bulbasaur** (ensalada de bayas).
- **Caja registradora** (bloque al final de la barra): menú con **ABRIR EL CAFÉ**, **Recetario**, **Tienda: platos y mejoras**, **Invitar a un amigo** y **Parar por ahora**.
- **Turno de 4 minutos:** llegan clientes (Pokémon reales de Cobblemon sin IA que caminan por una ruta fija), se quedan en su mesa con un **globo con el dibujo del plato** y una **barra de paciencia**.
  Cocinas **llevando el objeto en la mano** (el objeto de verdad en el hueco 1 de la barra, que no se puede cambiar; el resto del inventario está vacío): **clic derecho** en una caja = coger; en una estación = dejarlo / recoger lo terminado;
  en el cliente o su mesa = servir; en la basura = tirar. Al acabar el reloj **se cierra el local** (los que esperan se van sin pagar y sin enfadarse) y sale el **resumen de ganancias** con **Comprar**, **Otro turno** y **Parar por ahora**.
- **Estaciones (todas con modelo y textura propios):** caja de papas, caja de bayas, caja de lechugas, **tabla de cortar** (papa→tiras, lechuga→picada), **freidora** (tiras→papas fritas; **se queman** si no las recoges en 10 s: avisa con humo a los 6 s),
  **exprimidor** (baya→limonada), **mesa de emplatar** (lechuga picada + baya→ensalada; se puede recoger lo que pusiste por error), **fregadero**, **basura**, mesas y la caja registradora.
- **Platos sucios:** al irse un cliente satisfecho deja un **plato sucio** en la mesa (la mesa no se puede usar hasta recogerlo); se lava en el fregadero y vuelve a la **pila de platos limpios** (empiezas con 4); **sin platos limpios no puedes servir**.
- **Dinero:** cada plato paga su precio (papas 12, limonada 10, ensalada 16) **+ propina de hasta el 60 %** según la paciencia que le quedara. Va a **quien sirve**.
- **Tienda** (entre turnos): **platos nuevos** (papas 60, limonada 80, ensalada 120; cada plato desbloquea sus cajas y estaciones en la cocina) y **5 mejoras de 2 niveles**: más mesas (de 2 a 4), estaciones más rápidas (−20 % por nivel), clientes más pacientes (+25 %), más vajilla (+2 platos), publicidad (clientes más seguido).
- **Invitar:** el dueño elige a un jugador conectado (menú con cabezas o `/cafe invitar <jugador>`); le llega un mensaje con **[ACEPTAR]** (o `/cafe aceptar <dueño>`, vale 1 minuto). Entra como invitado (hasta 4 personas en total), cocina y sirve, **lo que gana es suyo**;
  si el dueño sale o se desconecta, el café se cierra y los invitados vuelven a donde estaban. Con más gente llegan clientes más seguido.
- **Datos** en `world/emi_cafe/jugadores/<uuid>.json` (monedas, cocinero, platos, mejoras, turnos, mejor turno) y `world/emi_cafe/saved/<uuid>.nbt` (lo que tenías antes de entrar).
- **Comandos:** `/cafe entrar|salir|info|invitar <jugador>|aceptar [dueño]`; admin (`op` 2 o permiso `emi_cafe.admin`): `/cafe dev cliente|monedas <n>|estrellas <n>|turno|inicial <charmander|squirtle|bulbasaur>|platos|reiniciar|usar <x y z>|local`.
- **Texturas:** 44 objetos (ingredientes, platos, platos quemados y sucios, cocineros con gorro, iconos del menú) y 22 bloques con 30 texturas de superficie, **dibujadas por código** (`assets/generar.py`, Python con Pillow; **ejecútalo para regenerar** y `--hoja X.png` para ver todas juntas).
  Para cambiar una a mano basta reemplazar el PNG (los nombres son fijos) y no volver a ejecutar el script.
- **Compilar:** `COBBLEMON_JAR=… KOTLIN_JAR=… tools/emi-cafe/build.sh` (como `emi-fall`). **En el servidor real hay que copiar a mano el jar a `mods/`**; y `emi-survivors` 0.29.0 para el botón de Ms. President.

**Probado en el juego** (banco ligero: servidor Fabric + cliente real con Xvfb, sin Emipokemon ni el pack entero): entrar, elegir cocinero con clic, abrir el menú de la caja con **clic derecho real**, abrir turno con clic, los **3 platos completos** (papas, limonada, ensalada),
la **quema** de las papas y tirarlas, plato sucio → fregadero → pila de platos, cliente que paga y se va (monedas y propina), cierre del turno y resumen, comprar un plato y una mejora (el local se reconstruye con las estaciones/mesas nuevas),
**dos clientes reales**: invitar, aceptar con el mensaje, el invitado sirve y cobra él, el menú del invitado, salir el dueño y que el invitado vuelva, desconectarse dentro y recuperar lo guardado al volver; el botón de **Ms. President** (clic real) que lleva al café.
**No probado:** un turno de 4 minutos jugado a mano de principio a fin (se acortó con `dev turno`), **3-4 jugadores a la vez**, varios cafés a la vez en el servidor, el servidor real y el pack entero (otros mods metidos), sonidos (no hay audio en la máquina de pruebas),
el **equilibrio** (precios, tiempos, paciencia y llegada de clientes son **estimaciones**), clientes con especies de tamaños raros (todos se escalan a ~1,15 bloques de alto) y el comportamiento si **otro mod** teletransporta al jugador dentro del café.
**Limitación conocida:** si un jugador dentro del café escribe a mano el comando de otro minijuego (`/fall entrar`…) sin salir antes, el café lo da de baja sin devolverle lo suyo hasta que vuelva a la dimensión del café (entonces `Retornos` se lo devuelve); `Ms. President` y survivors ya lo avisan.

## Lo que falta (ver las fases de abajo)
**F3 contenido:** ~20 platos (hoy 11), más ingredientes y estaciones, más especies de clientes y sus gustos, ayudantes Pokémon, decoración.
**F4 (lo que queda):** música y sonidos propios y reputación del café (estrellas, tienda, peluche chef, misiones y logros ya están en la 0.3.0).
**F5:** equilibrio con gente real y servidor real.

---

# Plan original (2026-10-23, lo que se propuso antes de construir; la 0.2.0 sigue este plan)

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
