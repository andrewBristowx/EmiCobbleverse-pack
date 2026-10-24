# Emi Recursos (`emi_recursos`) — 1.0.0

Mundos de **recursos** para el servidor de Emi: un **overworld con su nether y su end propios** que se **borran y se vuelven a crear con otra semilla cada 3 días**.
Pedido del dueño (2026-10-22): «otro overworld conectado a nether y end propios, para que allí consigan recursos; que se reinicie cada 3 días con una semilla distinta;
que al entrar puedan explorar y ya; si se desconectan dentro, al volver están en el spawn del mundo normal; antes de reiniciar avisa y los devuelve al mundo normal;
y, si es posible, si alguien muere se crea una tumba y aparece en su punto de cama».

**Solo servidor** (`environment: server`): los clientes no necesitan nada (para ellos los mundos son vanilla). El jar lleva dentro la librería
[Fantasy](https://github.com/NucleoidMC/fantasy) (Nucleoid, LGPL; jar-in-jar), que es la que crea y borra mundos mientras el servidor está encendido.

## Cómo se usa (jugadores)
- `/recursos` o `/recursos info`: cuánto falta para el reinicio y cuánta gente hay dentro.
- `/recursos entrar`: te lleva al overworld de recursos (todos aparecen en el mismo sitio, en tierra firme). Se guarda por dónde entraste.
- `/recursos salir`: vuelves al sitio por donde entraste. Lo que lleves encima te lo quedas; lo que dejes en el mundo (cofres, bases…) **se pierde en el reinicio**.
- **Portales:** dentro, un portal del nether encendido con mechero lleva al **nether de recursos** (y de vuelta), y el portal del End lleva al **End de recursos** (con su dragón;
  el portal de salida del End vuelve al overworld de recursos). No hace falta nada más.
- **Avisos:** a quien esté dentro se le avisa por chat a 1 h, 30 min, 10 min, 5 min y 1 min, y con una cuenta atrás en pantalla los últimos 10 s.
  Al llegar a 0 se saca a todos al mundo normal (por donde entraron), se borran los tres mundos y se crean otros con semilla nueva (aviso en el chat a todo el servidor).
- **Si te desconectas dentro**, al volver a conectar apareces en el **spawn del mundo normal** (también si el servidor se reinició o los mundos ya cambiaron).
- **Si mueres dentro:** reapareces donde reaparece siempre (tu cama o el spawn del mundo normal; dormir en una cama o usar un ancla en los mundos de recursos **no** cambia tu punto de
  reaparición) y **tu tumba (Universal Graves) se crea en tu punto de cama del mundo normal** (o en el spawn si no tienes cama), con tus cosas y tu experiencia, para que no se pierdan en el reinicio.

## Administración
- `/recursos reiniciar` (op nivel 2): reinicia ya (saca a todos, borra, crea de nuevo). `/recursos semilla`: muestra el ciclo y la semilla actual.
- `-Demi.recursos.dias=3` (propiedad de Java): duración del ciclo en días (admite decimales: `0.0035` ≈ 5 min, para pruebas).
- Datos: `<mundo>/emi_recursos/estado.json` (ciclo actual, semilla, hora del reinicio, spawn, pelea del dragón y por dónde entró cada jugador) y
  `<mundo>/dimensions/emi_recursos/{overworld,nether,end}_<ciclo>/` (los mundos; se borran solos en cada reinicio y las carpetas de ciclos viejos se limpian al arrancar).
- Si el servidor estaba apagado cuando tocaba reiniciar, al arrancar se crea un ciclo nuevo directamente (no hay nadie dentro: los jugadores que se quedaron dentro aparecen en el spawn al conectarse).
- Para quitar la función: borrar `emi-recursos-*.jar` del servidor. Quien estuviera dentro aparece en el overworld normal (vanilla) y se pueden borrar `world/dimensions/emi_recursos` y `world/emi_recursos`.

## Cómo funciona por dentro (por si hay que tocarlo)
- `Ciclo`: el reloj (cada segundo), avisos, `reiniciar`, y qué pasa al conectarse / cambiar de mundo / respawnear. `Mundos`: abre y borra los tres mundos con Fantasy
  (`getOrOpenPersistentWorld`; claves `emi_recursos:overworld_N|nether_N|end_N` con N = número de ciclo; sobreviven a los reinicios del servidor hasta que toca borrarlos).
  `Estado`: el JSON. `Cmds`: los comandos. `Tumbas`: el cambio de sitio de la tumba.
- **Terreno:** el overworld de recursos usa el generador del overworld de siempre (así respeta los mods de biomas del pack); si el mundo principal no fuera de ruido
  (plano, depuración…) usa el terreno normal del juego. Nether y End usan los de siempre. Todos con la **semilla del ciclo** (Fantasy sustituye `getSeed`; se comprobó que cada ciclo da terreno distinto).
- **Mixins** (`emi_recursos.mixins.json`): `NetherPortalBlockMixin` (a dónde lleva el portal del nether en los mundos de recursos), `EndPortalBlockMixin` (ídem el del End; la salida
  del End de recursos va al overworld de recursos, no a tu cama), `AbstractFireBlockMixin` (el fuego solo encendía portales en el overworld/nether de siempre),
  `ServerWorldMixin` (**importante**: el juego guarda la pelea del dragón de cualquier mundo con pelea en el archivo del mundo principal; sin esto, el End de recursos pisaría la pelea del End de verdad),
  `ServerPlayerEntityMixin` (no se puede fijar el punto de reaparición dentro de los mundos de recursos), `GraveUtilsMixin` (@Pseudo: solo si está Universal Graves; usa nombres intermediary y
  `Tumbas.crearEnLaCama`, que cambia un instante el mundo y la posición del jugador para que Universal Graves cree la tumba en la cama; depende de `GraveUtils.createGrave(...)` de graves 3.4.4)
  y `EntityAccessor`.
- **El spawn del overworld de recursos se guarda en `estado.json`** (Fantasy no guarda el punto de aparición de sus mundos: usarían el del mundo principal).
- **El dragón:** el juego solo crea la pelea del dragón en el End de siempre; `Mundos.ponerDragon` se la pone al End de recursos (nueva en cada ciclo; se guarda aparte cada minuto y al parar
  para no perderla si se reinicia el servidor a mitad de ciclo). Matarlo da el portal de salida y el huevo en cada ciclo.
- Distant Horizons (si está en el servidor) crea sus datos dentro de la carpeta de cada mundo de recursos, así que también se borran con ellos.

## Probado / no probado
**Probado en el juego** (servidor Fabric + cliente real, un solo jugador):
- banco ligero (sin el resto del pack): entrar, spawn en tierra, nether (portal encendido con fuego, ida y vuelta con escala 1:8), End (plataforma, dragón y barra de jefe, portal de salida que vuelve
  al overworld de recursos), desconectarse dentro y volver (aparece en el spawn normal), morir dentro (tumba en la cama del mundo normal con el mensaje de Universal Graves, y sin tocar el punto de
  reaparición), `spawnpoint` dentro (no cambia), avisos de 1 min / cuenta atrás de 10 s / reinicio (ciclo de ~5 min: sacó al jugador por donde entró, borró las carpetas viejas y creó el ciclo 2 con otra semilla),
  reiniciar el servidor a mitad de ciclo (mismos mundos, bloque puesto sigue ahí, dragón muerto sigue muerto, portal de salida del End en su sitio) y arrancar con el ciclo ya caducado (ciclo nuevo y carpetas viejas borradas);
  el cliente con el jar instalado arranca y entra sin problema (no lo carga).
- **servidor y cliente con el pack completo de pruebas (183/213 mods, incluidos Lithium, ScalableLux, Krypton y Distant Horizons)**: arranca (abre los tres mundos en ~11 s), `/recursos entrar`, portal del nether con ida, y morir dentro
  (la tumba quedó guardada en el mundo normal, no en el nether).

**No probado:**
- el **servidor real** (HolyHosting): ni su RAM/CPU al crear los mundos (genera unos cuantos chunks de golpe al reiniciar: puede notarse un tirón de 1-3 s), ni **C2ME** (en el servidor real; no estaba en el servidor de pruebas), ni que Distant Horizons no se
  ponga a generar mucho en los mundos nuevos;
- **varias personas a la vez** (avisos y expulsión con varios dentro, dos en el End a la vez, etc.);
- que el portal del End de verdad (marco con ojos de Ender, fortaleza) funcione: se probó con bloques de portal puestos con comandos; los marcos usan el código normal del juego;
- el portal de salida del End tras matar al dragón de verdad (se probó con un bloque de portal puesto a mano y la pelea guardada tras matar al dragón con `/kill`);
- ancla de reaparición, camas y otros mods que cambien el sitio de reaparición por su cuenta;
- tumba con `keepInventory` activo o sin Universal Graves (en ese caso lo que se suelta se queda en el mundo de recursos y se pierde en el reinicio);
- Cobblemon/Emipokemon dentro de los mundos nuevos más allá de ver Pokémon salvajes aparecer (no se miraron spawns especiales, jefes de Emipokemon ni raids).
