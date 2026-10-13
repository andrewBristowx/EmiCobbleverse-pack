# Emi Carreras (`emi_carreras`)

Minijuego de **carreras de Pokémon** estilo Mario Kart para el servidor de Emi. Toda la lógica es de servidor (Fabric 1.21.1, Fabric API + Cobblemon): los jugadores no necesitan hacer nada.
Desde la 0.1.1 el mod se declara para cliente y servidor (`environment: *`) **solo para que funcione también en un mundo de un jugador o abierto a LAN** (el servidor integrado lo carga); en el cliente no hace nada. Hasta **12 corredores** (se puede jugar con menos, incluso solo), una pista con temática Pokémon, puntos de control,
vueltas, cajas de poderes y turbos. Se corre en su propia dimensión, `emi_carreras:pista`.

## Pistas
Se elige con `/carrera abrir [pista]` (sin nombre, la de `pista` en la configuración); `/carrera pistas` las lista.
| Id | Nombre | Cómo es |
|---|---|---|
| `circuito_pokeball` | Circuito Pokéball | Ancha y fácil, 3 vueltas, estatuas de Pokémon, túnel Pokéball. |
| `volcan_cinnabar` | Volcán Cinnabar | **Más difícil**: pista estrecha con desnivel, 2 vueltas, 2 **saltos** sobre lava, un **atajo** con salto largo, **lodo** y **Voltorb fijos**. Volcán con cráter de lava y estatuas de fuego. |

### Mecánicas del Volcán Cinnabar
- **Plataforma de salto** (franja verde con flechas blancas, antes de un hueco con lava; **solo el borde azul de 1 bloque del final dispara el salto**, la parte verde es solo la guía): al pisar el borde azul vuelas en arco hasta la otra orilla (no hace falta saltar; el salto está desactivado). El servidor sienta al jugador en un **asiento invisible** (una entidad) y lo mueve por la parábola; el cliente interpola
  el movimiento de la entidad y sale fluido (versiones anteriores teletransportaban al jugador cada tick o le daban velocidad, y se veía congelado en el aire y de golpe al otro lado, o se quedaba corto con pocos fps). Si pulsas agacharte para bajarte, se te vuelve a montar. Los dos saltos del bucle principal son seguros.
- **Entrada al atajo (CTR)**: el atajo **ya no está unido a la pista**: sale por el lado derecho y solo se llega con un salto con turbo desde la plataforma del **carril interior** (3 de ancho, pegado al lado oeste), precedida por su franja azul y amarilla. Sin turbo (o fuera del carril) no hay entrada; con la plataforma pero sin turbo el arco acaba en la lava.
- **Atajo con turbo («como en Crash Team Racing»)**: se salta ~340 bloques del bucle. Dentro hay un segundo hueco de 17 bloques: **solo se cruza si pisas antes la franja azul y amarilla del suelo (3 de ancho)**, que da turbo; con turbo
  (esa franja, una Baya o un Caramelo) el arco llega a la otra orilla; sin turbo el arco acaba en la lava y vuelves al último punto de control. Al aterrizar bien hay un pequeño turbo extra.
- **Lodo** (marrón): frena un 40-45 %. En el atajo ocupa todo el ancho; en dos curvas del bucle solo media pista (se esquiva).
- **Voltorb fijos**: explotan al tocarlos (como el poder) y reaparecen a los 12 s. Solo existen mientras hay un corredor cerca.
- Caer a la lava (o pisar el suelo fuera de la pista) = volver al último punto de control. El atajo no tiene puntos de control propios: los puntos de control del bucle que se salta no existen (no hay ninguno en ese tramo).
- Los generadores son `pista/camino_volcan.py` y `pista/volcan.py` (Python; `python3 volcan.py ..`). Los datos nuevos del JSON de pista: `saltos` (zona, destino, destino_turbo, normal, altura, ticks), `lodo`, `voltorbs`, `musica`, `ambiente`.

## Interfaz de la carrera (0.5.0, parte de cliente del mod)
El servidor manda el estado de la carrera ~4 veces por segundo (`EstadoPayload`) y el poder que llevas (`PoderPayload`) a quien tenga el mod en el cliente (los demás ven solo el texto de la barra). Mientras dura la carrera el cliente:
- **Oculta** la lista de Pokémon de Cobblemon (`HidePartyBinding`), el minimapa de JourneyMap (`ClientAPI.toggleMinimap`), el cuadro de Jade (`IWailaConfig...setDisplayTooltip`) y los avisos emergentes; al acabar (o al salir del servidor / cerrar el juego) lo deja como estaba. Todo por reflexión: si falta alguno de esos mods no pasa nada. El dinero de CobbleDollars (abajo a la derecha) no se oculta.
- **Posiciones** (arriba a la derecha): puesto, cabeza, nombre, vuelta y tiempo de cada corredor, ordenado y actualizándose.
- **Mapa de la pista** (debajo): imagen cenital generada por `pista/mapa.py` (`assets/emi_carreras/textures/gui/mapa_<id>.png`, se sitúa con `mapa` del JSON de la pista) con la **cabeza de cada jugador** donde está (la tuya con borde blanco).
- **Poder**: al cogerlo sale una tarjeta grande (objeto, nombre, qué hace); mientras lo llevas queda un recordatorio sobre la barra de objetos.
Se oculta con F1 como cualquier interfaz.

## Sonido
- **Música de fondo** durante la carrera (chiptune original hecha con `musica/generar_musica.py`, ogg dentro del mod): «circuito» (alegre, 51,2 s) y «volcan» (oscura y rápida, 40 s). El servidor la relanza en bucle (las duraciones están en `DURACION_MUSICA` de `Carrera.java`); suena en la categoría **Música** del cliente.
- **Efectos**: cuenta atrás y fuegos artificiales en el «¡YA!», campanitas en los puntos de control y vueltas (campana en la última), cofre de poder (amatista), turbo (cohete), salto (brisa + slime), lodo, caída a la lava, meta y fin de carrera. En el Volcán hay sonidos de ambiente (burbujas de lava y retumbos lejanos).
- Configuración: `musica`, `volumenMusica`, `sonidos`.

## Cómo se juega
1. El organizador escribe **`/carrera abrir`**: aviso a todos con el botón **[UNIRSE]** (se repite cada 60 s).
2. Cada jugador pulsa el botón o escribe `/carrera unirse`. Sale la lista de su equipo con botones **[ELEGIR]**: **solo valen los Pokémon terrestres**
   (los que caminan y no vuelan ni son de tipo Volador). Si no tiene ninguno se le **presta** uno (`prestados` en la config). Se cambia con `/carrera elegir <1-6>` hasta que empiece.
3. **`/carrera empezar`**: se guarda en disco el inventario, la posición, el modo de juego y el hambre de cada uno y todos aparecen en la **parrilla**. Antes de la cuenta atrás hay una **presentación**: unos segundos de espera (la dimensión tarda en cargar), y luego una cámara da una vuelta alrededor de cada corredor y de su Pokémon, con su nombre y especie en el título (3 s por corredor, menos si son muchos). Después cada uno vuelve a su sitio, ya transformado, y empieza la **cuenta atrás de 5 s**.
4. Durante la carrera el jugador va **transformado en su Pokémon** con el mod **CobbleMorph** (que ya está en el pack): el cliente dibuja el modelo del Pokémon en lugar del jugador, con su misma especie, forma y variocolor, y camina con su animación.
   Si CobbleMorph no está instalado, el jugador va invisible y su Pokémon es una **copia visual** que lo acompaña (puede desfasarse al correr).
   **Todos van exactamente a la misma velocidad** (atributo fijo, sin correr, sin saltar). Se maneja con WASD y el ratón; **pulsa F5** para verte desde fuera.
5. Hay que pasar por todos los **puntos de control en orden** (arcos azules; la meta es el arco rojo con Pokéballs). Tres vueltas por defecto. Si te caes de la pista
   vuelves al último punto de control tras 2 s parado.
6. Las **cajas** (una **moneda del Michialien** que gira, con el michi en pixel art sobre un borde dorado grabado) dan un **poder** al azar, que sale en la ranura 5 de la barra: **clic derecho** para usarlo (también vale el clic izquierdo, o clicar sobre una entidad; agáchate para tirarlo hacia atrás).
   Quien va peor clasificado tiene más probabilidad de los poderes fuertes. Se recargan a los 6 s.
7. Cuando llega el primero, los demás tienen 45 s. Al final: clasificación en el chat, título con el ganador, y a los 10 s todos recuperan lo suyo y vuelven a donde estaban.

### Poderes (temática Pokémon)
| Poder | Efecto |
|---|---|
| Baya Aranja | pequeño turbo (1,5 s) |
| Caramelo Raro | super turbo (3,5 s) |
| Pokéball | proyectil que sale **hacia donde apuntas** (también un poco arriba/abajo): hay que apuntar; el primer rival que toque queda **capturado** 2,5 s |
| Voltorb trampa | suelta un Voltorb detrás de ti; explota al tocarlo (parada + ralentización) |
| Protección | escudo de 8 s: **para todos los ataques** mientras dura (el jugador brilla, sale un título «¡Protegido!» y la cuenta atrás en la barra de abajo) |
| Trueno | rayo sobre **todos** los que van delante de ti |
Las franjas amarillas/naranjas del asfalto son **turbos**.

## Comandos
Jugadores: `/carrera unirse · salir · elegir <1-6> · estado · ayuda`.
Organizador (operador nivel 2 o permiso `emi_carreras.admin` de LuckPerms): `/carrera abrir [pista] · pistas · cerrar · empezar · parar · expulsar <jugador> · instalar · recargar`.
Solo pruebas: `/carrera _poder <baya|pokeball|voltorb|escudo|caramelo|trueno>` (durante la carrera, te da ese poder) y `/carrera _poder golpe_<pokeball|voltorb|trueno>` (te da ese golpe, para probar la Protección).

## Configuración (`config/emi_carreras.json`, se crea sola)
`pista` (la de por defecto), `pistas` (las que se pueden elegir), `musica`, `volumenMusica`, `sonidos`, `vueltas` (0 = las de la pista), `minPlayers` (1), `maxPlayers` (12), `velocidad` (0.23 ≈ 10 bloques/s), `adelanto`, `alturaPokemon`, `presentacion` (true/false: la presentación con cámaras), `presentacionSegundos` (por corredor; 0 = automático), `cuentaAtras`, `segundosTrasPrimero`, `respawnSegundos`,
`queueReminderSeconds`, `poderes`, `cajaRespawnSegundos`, `prestados` (especies prestadas) y **`recompensas`**: lista de comandos por puesto (1.º, 2.º, 3.º…) con `{jugador}`, vacíos por defecto
(p. ej. `"cobbledollars give {jugador} 500"`: pon el comando real de tu economía).
Si existe `config/emi_carreras/pistas/<id>.json` se usa en lugar del de dentro del mod (para ajustar la pista sin recompilar).

## Probarlo sin servidor
En un mundo de un jugador (con trucos activados para tener operador) o abierto a LAN con amigos: `/carrera abrir`, `/carrera unirse`, `/carrera empezar`. Para esto está la instancia de Prism
`prism-instance/EmiCobbleverse-Prism-Instance-PRUEBAS.zip`, que se actualiza sola desde la rama de pruebas del repositorio y **no toca la instancia oficial** (que sigue leyendo `main`). Sin probar aún en un mundo de un jugador.

## Cómo está hecho
- **Clics en mundo de un jugador / LAN (0.3.3)**: los eventos de clic (`UseItemCallback`, etc.) se ejecutan también en el cliente de Fabric, y si devuelven «FAIL» el cliente **no envía el clic al servidor**. En un servidor de verdad el cliente no sabe quién corre (`participa` da falso) y todo iba bien,
  pero en un mundo de un jugador o el anfitrión de una LAN el cliente comparte el mismo estado que el servidor integrado y el clic se perdía (el poder se quedaba en la mano). Ahora, si `world.isClient`, siempre se devuelve «PASS». Reproducido con la 0.3.2 y arreglado con la 0.3.3 en un mundo de un jugador.
- **CobbleMorph (desde la 0.2.0)**: `Morph.java` lo maneja por reflexión y con los comandos `/transform` y `/untransform` (el servidor los ejecuta como consola); la transformación anterior del jugador, si tenía, se guarda con su inventario y se le devuelve al terminar (también tras una caída del servidor). Los efectos de turbo, golpe y escudo salen como partículas.
- **Montura de reserva**: en Cobblemon solo ~115 de 1025 especies se pueden montar, así que no se usa la monta de Cobblemon. El jugador anda con atributos fijos (velocidad, sin salto, escalón de 1 bloque) y
  su Pokémon es una copia sin IA (`PokemonProperties` → `createEntity`) que se coloca encima cada tick, adelantada según lo que se movió (el servidor no conoce la velocidad de un jugador). El equipo del jugador **no se toca**.
- **Seguridad**: lo que tenía el jugador se guarda en `world/emi_carreras/saved/<uuid>.nbt` y se devuelve al terminar, al cancelar o al volver a entrar tras una caída del servidor.
  El tick de la carrera está protegido: si algo falla, se cancela la carrera (y se devuelve todo) en vez de tumbar el servidor.
- Los corredores entran en el equipo `emi_carrera` (sin colisión de nombre) **solo si no tienen ya equipo**, para no pisar los prefijos/colores de otros mods.
- **Estatuas de Pokémon**: `pista/modelo.py` convierte el modelo y la textura reales de Cobblemon (geometría Bedrock) en bloques de color (concreto, lana, terracota; se elige el más parecido) y `pista/catalogo_estatuas.py`
  guarda cada estatua en `pista/estatuas/<especie>_<altura>.json`, de modo que regenerar la pista no necesita Cobblemon. Para crear una nueva: `COBBLEMON_ASSETS=<jar de Cobblemon descomprimido>/assets/cobblemon MC_TEXTURAS=<textures/block del client.jar> python3 generar.py ..`
  tras añadirla a la lista `figuras` de `generar.py`. Se aplica la animación de reposo de Cobblemon (`ground_idle` en el instante 0, con sus expresiones de Molang) para que no salgan en T, y se descartan las variantes de la cara que quedan tapadas por otras
  (párpados, ojos de emoción, boca abierta...) comprobando si cada superficie fina está dentro de otro cubo; los ojos y la boca ("calcomanías") mandan sobre la superficie de la cabeza. Probados y bien: Pikachu, Charmander, Squirtle, Bulbasaur, Charizard, Blastoise,
  Dragonite, Lapras, Eevee, Psyduck, Jigglypuff, Espeon, Lucario. El Snorlax se dejó como estaba a petición del dueño (su archivo en `estatuas/` no se regenera). Gengar no cupo en la pista.
- **Pista**: `pista/generar.py` (Python, usa `tools/emi-gimnasios/gymlib.py`) genera la plantilla `data/emi_carreras/structure/circuito_pokeball.nbt` y `data/emi_carreras/pistas/circuito_pokeball.json`
  (parrilla, puntos de control, cajas, turbos, podio). `camino.py` define el trazado (curva Catmull-Rom). La plantilla se coloca sola la primera vez que se empieza una carrera (o con `/carrera instalar`).

- **Moneda del Michialien (0.3.0/0.3.1)**: las cajas son entidades `item_display` con un objeto con modelo propio (`assets/emi_carreras/models/item/moneda_michi.json`, dos caras con la textura `moneda_michi.png`, 640x640).
  La textura la genera `moneda/hacer_moneda.py` (necesita numpy, scipy y Pillow): recorta y centra el michi de `moneda/michi_referencia.webp` (pixel art del dueño, tal cual) y le pone un borde dorado grabado dibujado en rejilla de 80x80.
  En 1.21.1 no existe el componente `item_model`, así que se usa `minecraft:nautilus_shell` con `custom_model_data` 7020001 y un *override* en `assets/minecraft/models/item/nautilus_shell.json` (va dentro del mod, no hace falta resource pack; el jugador sí necesita el mod en el cliente).
  La moneda gira porque el servidor le cambia la rotación cada 10 ticks con interpolación (también durante la presentación y la cuenta atrás).
  **Monedas (0.3.2)**: cada moneda se crea con un UUID conocido y se busca por UUID, no por zona: una entidad en un trozo de pista cargado «a medias» (lejos del jugador) no sale en las búsquedas por zona y el servidor creaba una moneda nueva cada tick (miles de monedas quietas duplicadas y el servidor ahogado). Las monedas de trozos descargados se crean al cargarse el trozo, y `barrerMonedasSueltas` quita cada 20 ticks las que no son de la carrera en curso (restos de carreras anteriores).
- **Presentación (0.3.0)**: no se usa `setCameraEntity` (en vanilla mueve el cuerpo del jugador junto a la cámara y se pierde su sitio): los jugadores pasan a modo **espectador** y el servidor los mueve a ellos mismos por la órbita (`requestTeleport` cada tick),
  mientras en cada hueco de la parrilla hay una copia sin IA del Pokémon de su corredor. Al terminar se les devuelve a su hueco, pasan a modo aventura y se transforman con CobbleMorph.

## Compilar
`COBBLEMON_JAR=<Cobblemon-fabric-1.7.3+1.21.1.jar> KOTLIN_JAR=<kotlin-stdlib-*.jar> ./build.sh` (Fabric Loom 1.10.5, Yarn 1.21.1+build.3; Cobblemon y Kotlin solo se usan para compilar).
El jar sale en `build/libs/`. Para regenerar la pista: `cd pista && python3 generar.py ..`.

## Probado en la 0.5.0
Servidor de pruebas y cliente real (un solo jugador): el salto **solo** se dispara en el borde azul (de pie sobre el verde no pasa nada; al llegar al azul salta); la entrada al atajo con turbo sigue funcionando; la interfaz sale (posiciones, mapa con la cabeza del jugador, tarjeta del poder con objeto/nombre/descripción), y la lista de Pokémon, el minimapa y el cuadro de Jade desaparecen durante la carrera y **vuelven al cancelarla** (capturas antes/durante/después).
**No probado**: la interfaz con **varios jugadores** (cabezas moviéndose por el mapa, orden de la tabla), con otras resoluciones o escalas de interfaz, ni el servidor real.

## Probado en la 0.4.1 (arreglos tras las pruebas del dueño)
Con servidor de pruebas y cliente real: el salto del bucle (asiento invisible) recorre la parábola exacta (log de posiciones jugador/asiento) y aterriza en la pista; la entrada al atajo con turbo aterriza en el atajo y sin turbo cae a la lava. **No probado**: lo fluido que se ve en un cliente normal con red real (el cliente de pruebas va a pocos fps y sin audio), ni varios jugadores.

## Probado en la 0.4.0
Servidor de pruebas y cliente real (un solo jugador):
- **Volcán Cinnabar**: se coloca solo (7 s), los puntos de control van en orden (por el atajo y por el bucle), la **plataforma de salto** del bucle lanza en arco y aterriza en la pista, la **del atajo con turbo** cruza el hueco y **sin turbo cae a la lava** y reaparece, el **Voltorb fijo** explota y golpea, el **lodo**, las estatuas (Charizard, Dragonite, Charmander, Lucario) y el volcán se ven bien (capturas).
- **Protección**: con ella activa el golpe de prueba no te afecta; al acabar los 8 s sí (log del servidor).
- Selector de pista (`abrir volcan_cinnabar` y `abrir` sin nombre) y música/efectos: el servidor los envía sin errores, los .ogg son válidos y están registrados en `sounds.json`; **no se pudo oír** nada porque el cliente de pruebas no tiene tarjeta de sonido.
- **No probado**: una vuelta completa corriendo a pie por el Volcán (se probó por tramos con teletransportes y a pie en los saltos), varios jugadores a la vez, el balance real del atajo (cuánto gana), el ruido real de los sonidos y la música, y el servidor real.

## Probado en la 0.3.0
Con servidor de pruebas y un cliente real (un solo jugador): la **moneda** (con el michi del dueño centrado y el borde nuevo, 0.3.1) se ve por las dos caras y gira; la **presentación** (espera, cámara orbitando al GatitoAlien con su nombre en el título, vuelta al hueco ya transformado y cuenta atrás 5-4-3…) funciona.
**No probado**: la presentación con **varios** jugadores a la vez (todos ven la misma cámara; con 12 son ~18 s), un jugador que mueva el ratón durante la cámara, y la espera de 5 s frente a una carga de dimensión lenta del servidor real.

## Probado (v0.1.0; la 0.1.1 solo cambia las estatuas y el entorno)
Servidor Fabric de pruebas con todos los mods del pack y un **cliente real** (un solo jugador, administrador):
- cola, parrilla, cuenta atrás, carrera, vueltas y puntos de control en orden (teletransportando por los arcos), meta, tabla de resultados, podio y devolución exacta del inventario/posición/modo/hambre (también con `/carrera parar`);
- la montura: el Pokémon visual acompaña al jugador, anima el paso y todos van a ~10 bloques/s (medido); el filtro de terrestres (Unown fuera; GatitoAlien, Meowstic y Espeon valen);
- cajas (clic real), poderes con clic derecho real: **Caramelo** (turbo medido: ~15 bloques/s), **Protección**, **Voltorb** (se suelta, explota y golpea; se probó contra el propio dueño) y **Pokéball** (se usa; no se llegó a ver el proyectil volando);
- caída de la pista → reaparece en el punto de control; la plantilla de la pista se coloca sola (y se vuelve a colocar, borrando la anterior, si cambia).
**No probado**: carreras con **varios jugadores** (solo hubo uno: colisiones, clasificación entre varios, que la Pokéball/el Voltorb/el Trueno golpeen a un rival de verdad, el balance de poderes), un jugador que no sea administrador, la reconexión en mitad de la carrera,
el servidor real (HolyHosting) y el rendimiento con 12 corredores. Con Distant Horizons en el servidor, una dimensión nueva con una pista grande puede cargar mucho al generar LODs: vigilar el primer día.
Visto una sola vez y sin repetirse: tras una carrera con muchas pruebas seguidas, una orden de teletransporte entre dimensiones no movió al jugador (los teletransportes dentro de la dimensión sí funcionaban); el mod ahora reintenta el teletransporte a la parrilla durante 3 s y deja en el log el motivo si alguien se retira.
