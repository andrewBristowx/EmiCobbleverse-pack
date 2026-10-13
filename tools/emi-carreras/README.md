# Emi Carreras (`emi_carreras`)

Minijuego de **carreras de Pokémon** estilo Mario Kart para el servidor de Emi. Toda la lógica es de servidor (Fabric 1.21.1, Fabric API + Cobblemon): los jugadores no necesitan hacer nada.
Desde la 0.1.1 el mod se declara para cliente y servidor (`environment: *`) **solo para que funcione también en un mundo de un jugador o abierto a LAN** (el servidor integrado lo carga); en el cliente no hace nada. Hasta **12 corredores** (se puede jugar con menos, incluso solo), una pista con temática Pokémon, puntos de control,
vueltas, cajas de poderes y turbos. Se corre en su propia dimensión, `emi_carreras:pista`.

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
| Protección | escudo 8 s: absorbe el siguiente ataque (el Pokémon brilla) |
| Trueno | rayo sobre **todos** los que van delante de ti |
Las franjas amarillas/naranjas del asfalto son **turbos**.

## Comandos
Jugadores: `/carrera unirse · salir · elegir <1-6> · estado · ayuda`.
Organizador (operador nivel 2 o permiso `emi_carreras.admin` de LuckPerms): `/carrera abrir · cerrar · empezar · parar · expulsar <jugador> · instalar · recargar`.
Solo pruebas: `/carrera _poder <baya|pokeball|voltorb|escudo|caramelo|trueno>` (durante la carrera, te da ese poder).

## Configuración (`config/emi_carreras.json`, se crea sola)
`pista`, `vueltas` (0 = las de la pista), `minPlayers` (1), `maxPlayers` (12), `velocidad` (0.23 ≈ 10 bloques/s), `adelanto`, `alturaPokemon`, `presentacion` (true/false: la presentación con cámaras), `presentacionSegundos` (por corredor; 0 = automático), `cuentaAtras`, `segundosTrasPrimero`, `respawnSegundos`,
`queueReminderSeconds`, `poderes`, `cajaRespawnSegundos`, `prestados` (especies prestadas) y **`recompensas`**: lista de comandos por puesto (1.º, 2.º, 3.º…) con `{jugador}`, vacíos por defecto
(p. ej. `"cobbledollars give {jugador} 500"`: pon el comando real de tu economía).
Si existe `config/emi_carreras/pistas/<id>.json` se usa en lugar del de dentro del mod (para ajustar la pista sin recompilar).

## Probarlo sin servidor
En un mundo de un jugador (con trucos activados para tener operador) o abierto a LAN con amigos: `/carrera abrir`, `/carrera unirse`, `/carrera empezar`. Para esto está la instancia de Prism
`prism-instance/EmiCobbleverse-Prism-Instance-PRUEBAS.zip`, que se actualiza sola desde la rama de pruebas del repositorio y **no toca la instancia oficial** (que sigue leyendo `main`). Sin probar aún en un mundo de un jugador.

## Cómo está hecho
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
  La moneda gira porque el servidor le cambia la rotación cada 10 ticks con interpolación.
- **Presentación (0.3.0)**: no se usa `setCameraEntity` (en vanilla mueve el cuerpo del jugador junto a la cámara y se pierde su sitio): los jugadores pasan a modo **espectador** y el servidor los mueve a ellos mismos por la órbita (`requestTeleport` cada tick),
  mientras en cada hueco de la parrilla hay una copia sin IA del Pokémon de su corredor. Al terminar se les devuelve a su hueco, pasan a modo aventura y se transforman con CobbleMorph.

## Compilar
`COBBLEMON_JAR=<Cobblemon-fabric-1.7.3+1.21.1.jar> KOTLIN_JAR=<kotlin-stdlib-*.jar> ./build.sh` (Fabric Loom 1.10.5, Yarn 1.21.1+build.3; Cobblemon y Kotlin solo se usan para compilar).
El jar sale en `build/libs/`. Para regenerar la pista: `cd pista && python3 generar.py ..`.

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
