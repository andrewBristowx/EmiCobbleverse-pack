# EmiCobbleverse — contexto para Claude

Responde siempre en **español**. El dueño (SomitadePony, nick de Minecraft `somitadepony`) no es programador: explica en simple, di
siempre **qué probaste y qué no**, y no inventes resultados.

## Qué es esto
Modpack **packwiz** de Emi: Minecraft **1.21.1 / Fabric 0.19.3**, base COBBLEVERSE (Cobblemon 1.7.3) + **Emipokemon 0.6.0-alpha.1**.
Repositorio: `andrewBristowx/EmiCobbleverse-pack` (rama principal `main`). Los jugadores se actualizan solos desde `pack.toml`.
- `pack.toml` (versión del pack + hash del índice) e `index.toml` (sha256 de cada archivo; los `.pw.toml` van con `metafile = true`).
  Al cambiar un archivo hay que actualizar su hash en `index.toml`, y luego el hash del índice y la `version` en `pack.toml`.
- `mods/*.pw.toml` = mods de Modrinth; `mods/*.jar` = jars propios (Emipokemon parcheado, `emi-plush`, `emi-estructuras`, etc.).
- `tools/<mod>/` = código fuente y README de cada mod propio. **Lee el README del mod antes de tocarlo.**
  Los mods usan nombres *intermediary* de Minecraft (`class_xxxx` / `method_xxxx`) y se compilan con `build.sh` con variables de
  entorno (`MC`, `GECKO`, `POKEBLOCKS`, `FAPI`, `LIBS`, `OUT`, `COBBLEMON_JAR`); ver `tools/emi-plush/build.sh`.
- `tools/emipokemon-patches/`: Emipokemon no tiene código fuente; se parchea el jar (clases/recursos inyectados). Ver su README.

## Mods propios (estado en pack 1.0.126)
- `emi-plush` 1.13.1 (+26 peluches gigantes, peluches llevables en la cabeza y en la ranura sombrero de Accessories): peluches, GatitoAlien (25 formas), Gasha Especial de Eventos (ticket + **máquina** `emi_plush:maquina_gasha_michi`
  con **pantalla propia** `GashaPantalla`, animación normal y de premio mayor). Ver su README. Se quitaron del gasha 4 Pokémon de Emi
  (espurr/zorua/sprigatito/floragato) porque Cobblemon no puede releerlos de los datos guardados (evoluciones con espacio de nombres).
- `emi-estructuras` 1.3.1 (+ recetas de los 5 medalleros de Emipokemon: Teselia, Kalos, Galar, Paldea y Alola): dimensiones `emi_estructuras:plano` y `emi_estructuras:dungeons`, comando `/emiestructuras`. Desde 1.2.0/1.3.0 coloca también los
  **gimnasios y ligas de Teselia, Kalos, Galar, Paldea y Alola** (48 plantillas `.nbt` propias; Teselia = `teselia:1..9`, Kalos = `kalos:1..9`, Galar/Paldea/Alola
  detrás de sus ubicaciones previas) y crea a sus líderes NPC dentro (`NpcsGimnasio`, `npcs.json`). Las plantillas las genera `tools/emi-gimnasios/`
  (Python; **lee su README**, y pasa `chequeo_soportes.py` y `chequeo_npc.py` antes de empaquetar). Con las 5 regiones ya no falta ningún edificio de líderes.
- `emi-carreras` 0.7.0 (servidor + parte de cliente: interfaz de carrera estilo Mario Kart que oculta la interfaz de vainilla y fuerza tercera persona; hay que instalarlo también en los clientes, ya va en el pack): minijuego de **carreras de Pokémon** tipo Mario Kart en la dimensión `emi_carreras:pista` (hasta 12 jugadores, también con menos; el jugador corre a pie
  transformado en su Pokémon terrestre con CobbleMorph, todos a la misma velocidad; puntos de control, 3 vueltas, cajas de poderes = moneda giratoria del Michialien; presentación con cámaras antes de la cuenta atrás). Tres pistas generadas por `tools/emi-carreras/pista/`: «Circuito Pokéball» (fácil), «Volcán Cinnabar» (difícil: saltos sobre lava, atajo con turbo, lodo, Voltorb fijos; `/carrera abrir volcan_cinnabar`) y «Estación Michialien» (tema Emi: luna creciente con estatuas de los peluches del Michialien, saltos, atajo con turbo; `/carrera abrir estacion_michialien`). Música chiptune y efectos de sonido (`musica/`). Desde 0.7.0: **michiestrellitas** (10 mejoran el poder; un golpe o caída quitan la mitad), 6 poderes nuevos, y durante la carrera se pausan (en memoria) el clearlag y los bosses de Emipokemon (`Pausas.java`).
  **Lee su README.** Probado solo con un jugador (admin); falta probarlo con varios jugadores reales.
- `emi-huida` 0.4.0 (**prototipo**; servidor + cámara, teclas e interfaz de cliente, hay que instalarlo también en los clientes, ya va en el pack): minijuego de **huida de un Pokémon perseguidor** estilo Crash Bandicoot («Nivel 1: La huida del Tyrantrum», plan en `tools/emi-huida/`). Sin lobby ni NPC, como las carreras: `/huida abrir` → los jugadores `/huida unirse` → el organizador `/huida empezar` → cada jugador confirma **`/huida listo`** (tecla R) y, cuando están todos, cuenta atrás (el organizador puede saltar la confirmación con `/huida saltar` o empezar con `/huida empezar ya`). Cada jugador corre en su copia del nivel (~850 bloques con curvas y 47 obstáculos; el Tyrantrum rodea los dos puentes por un sendero de la selva en vez de volar) con **274 michiestrellas** (306 puntos; las dibuja el cliente con la textura de emi-carreras; gana quien consiga más puntos, clasificación al final) y música y sonidos propios (`tools/emi-huida/audio/generar_sonidos.py`) en la dimensión `emi_huida:selva`; cámara estilo Crash por delante del jugador mirando hacia atrás con el Tyrantrum (~9 de alto) persiguiendo; saltar, agacharse (shift) y esquivar; errores y medallas. **Lee su README** (decisiones, qué está probado y qué no, fases pendientes). Probado solo con un jugador y con un piloto automático, incluido con Better Third Person (que rompía el movimiento lateral; ya compatible); falta jugarlo a mano y con varios jugadores.
- Emipokemon parcheado (pokéballs de tipo, tickets rediseñados, pestaña Dungeons), `no-chat-reports` en el pack.

## Cómo se trabaja (reglas del dueño)
- **No abras un PR** hasta que el dueño diga "mergea". Desarrolla en la rama que te indiquen y haz push; no a `main`.
- Commits terminan con las líneas de atribución que pida el entorno. Los jars pequeños se entregan como archivo; los grandes
  (Emipokemon, 35 MB) con enlace crudo de GitHub + SHA-256.
- Antes de decir que algo funciona, **pruébalo en el juego** (servidor Fabric + cliente real) cuando sea posible; muestra capturas.
- Los UV de los modelos Cobblemon deben ser enteros; evita z-fighting (caras coplanares). `tools/emi-plush/test/revisar_modelos.py`.

## Servidor real (HolyHosting)
- Hosting: **HolyHosting**, con acceso **SFTP** (lo da el panel). Linux, Java 21, `-Xms12G -Xmx12G` con G1, 8 CPUs, ~12 jugadores.
- **Credenciales:** viven solo en variables de entorno del PC del dueño (`HOLY_SFTP_HOST`, `HOLY_SFTP_PORT`, `HOLY_SFTP_USER`,
  `HOLY_SFTP_PASS`). **Nunca las escribas en archivos, commits, logs ni respuestas**, y no las pidas por el chat.
- **Por defecto solo lectura.** Para cambiar o subir cualquier archivo del servidor: 1) baja primero una copia de respaldo,
  2) di al dueño exactamente qué cambiarás y espera su OK, 3) nunca pares, reinicies ni borres sin permiso explícito.
- `tools/servidor/recoger_servidor.py` baja por SFTP (solo lectura) logs, crash-reports, configs clave y lista de mods a un `.zip`,
  tapando contraseñas/tokens e IPs. Requiere `python -m pip install paramiko`.
- Ya diagnosticado y avisado al dueño: `enable-command-block=true` en `server.properties` (activadores de legendarios/torres);
  la dimensión Dungeons se genera con `/emiestructuras generar dungeons`; No Chat Reports en servidor y clientes.

## Pendiente: crash del servidor del 2026-10-12 02:37 (Watchdog)
Hilo principal atascado en `ServerChunkManager.getChunk` (desde Lithium, colisiones de un jugador); todos los hilos de generación
inactivos → un futuro de chunk que nunca completó. Mods implicados en el stack: **C2ME 0.4.0-alpha.0.29** (módulo experimental
`c2me-rewrites-chunk-system` y `instrumentGetChunk`), Lithium 0.15.4. También en el servidor: **Distant Horizons 3.2.0-b** (16 hilos de
generación), Neruina 3.3.3, Chunky 1.4.23, spark 1.10.109, ScalableLux. Swap en uso ~7 GB (poca RAM libre en la máquina).
Sospechosos por orden: C2ME alpha > Distant Horizons generando en servidor > chunk corrupto. **Sin confirmar.** Siguiente paso:
leer el `latest.log` / `.log.gz` de las 02:35–02:38, revisar `config/c2me.toml` y `config/DistantHorizons.toml`, y proponer
(1) quitar o bajar de versión C2ME, (2) apagar la generación de LODs de DH en el servidor, (3) pregenerar con Chunky.
Los mods propios (`emi_*`) **no** aparecen en el stack del crash.
