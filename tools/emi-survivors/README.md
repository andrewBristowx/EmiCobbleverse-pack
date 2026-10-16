# Emi Survivors (`emi_survivors`) — 0.2.0 (fase 1: núcleo en solitario + entrada)

Minijuego de **supervivencia contra hordas de Pokémon** (estilo Vampire Survivors / Survivor.io) para el servidor de Emi: tu Pokémon ataca solo, tú solo te
mueves, las oleadas crecen, subes de nivel y eliges mejoras. Servidor + cámara, teclas e interfaz de cliente (hay que instalarlo también en los clientes; ya va
en el pack).

**Inspirado en [PokéSurvivor](https://pokesurvivor.com) de JPTER78.** El dueño le pidió permiso al autor y contestó que *mientras no se venda el juego, todo bien*.
Aun así **no se copia nada suyo**: ni código (su licencia desde el 14-10-2026 es PolyForm Noncommercial; antes era MIT), ni sprites (CC BY-NC), ni música. Aquí el código es
propio y los Pokémon son los modelos 3D de Cobblemon. Solo se toman las ideas del juego. **Los créditos a PokéSurvivor salen en el menú del juego** (pie de página:
«Inspirado en PokéSurvivor de JPTER78 · pokesurvivor.com»); hay que mantenerlos en todo lo que se enseñe a los jugadores.

## Decisiones del dueño (2026-10-15/16)
- El compañero son **tus propios Pokémon** (los del equipo de Cobblemon), transformándote en ellos con CobbleMorph, como en las carreras. El equipo no se toca.
- **Al entrar al mundo del minijuego hay un menú** donde subir tus **mejoras** (permanentes, por Pokémon) y **unirte a tu grupo** (hasta 4; el grupo llega en la fase 2).
- Se «gana» con un **top por el tiempo que aguantó el equipo** (como la tabla «Grupo» del original). En la fase 1 el «equipo» es un solo jugador; el top por tiempo en solitario por día/semana/mes llega después.
- **Toque de Emi y Michialien** (no premios): enemigos y jefes del mundo de Emi (Michialiens, Snorlax Emi…), decoración, música propia.
- **Los premios los da el dueño a mano**: no hay premios automáticos. Solo hace falta poder ver el top (`/survivor top` o la pestaña TOP del menú) para dárselos.
- **No se pasa de fase sin el visto bueno del dueño.**

## Plan por fases (cada una se prueba antes de pasar a la siguiente)
0. **Prueba de viabilidad** — HECHA (0.1.0, pack 1.0.142): arena plana, cámara desde arriba, tu Pokémon, enemigos Pokémon y 1 ataque automático; medir rendimiento.
1. **Núcleo en solitario + entrada** — HECHA (0.2.0, esta): el **mundo del minijuego con su sala de entrada y el menú** (jugar, mejoras, grupo, top), 1 bioma, experiencia, subir de nivel con 3 cartas,
   10 ataques y 9 mejoras pasivas, vida y muerte, primer jefe, **top por tiempo**. Hasta 4 partidas a la vez (una por jugador, cada una en su arena).
2. **Cooperativo y contenido**: grupos de hasta 4 (una arena por grupo), 3 biomas, ~30 ataques con evolución, objetos, jefes (min. 3, 6, 9, 12, 15), mareas, personalidades de enemigo, música y sonidos propios.
3. **Progresión**: monedas, mejoras permanentes por Pokémon (más variadas), misiones y logros, tops por periodo, shinies.
4. **Final y extras**: grietas con legendarios, «Sentencia» final, climas, eventos, más temas de Emi.

## Cómo se juega (jugador)
1. `/survivor entrar` te lleva al **mundo del minijuego**: una sala con damero rosa y lila, con las estatuas gigantes de dos peluches Michialien (el dorado con corona y el normal) en islotes de cristal y el **terminal** (una mesa de encantamientos con cuatro farolas) en el centro. Se guarda tu inventario, posición, modo de juego, hambre y punto de reaparición (en disco: sobrevive a un reinicio del servidor) y vas como persona, en modo aventura, sin poder romper ni tocar nada. Se abre el **menú** solo; para volver a abrirlo: clic derecho con la **estrella del menú** (ranura 5), tocar el **terminal** o `/survivor menu`.
2. **Menú** (pantalla propia, 4 pestañas):
   - **JUGAR**: tu equipo de Cobblemon (hasta 6) a la izquierda, con su modelo 3D; a la derecha el Pokémon elegido (nivel, tipo, ataque con el que empieza, mejoras permanentes y mejor tiempo con él) y el botón **¡A JUGAR!** («MUNDO LLENO» si ya hay 4 partidas a la vez). Tras una partida, arriba se enseña el resultado (tiempo, derrotados, nivel, michiestrellitas ganadas y si has entrado en el top).
   - **MEJORAS**: se compran con **michiestrellitas** y valen para **ese Pokémon** (se identifica por su UUID de Cobblemon: si lo sueltas o lo cambias, pierdes sus mejoras). 7 mejoras de 5 niveles (cuesta 25, 50, 75, 100 y 125 cada nivel): Vigor (+12 de vida), Fuerza (+4% daño), Agilidad (+2,5% velocidad), Sabiduría (+6% experiencia), Imán (+10% radio de gemas), Recuperación (+0,15 vida/s) y Suerte (+5% michiestrellitas al acabar).
   - **GRUPO**: de momento solo un aviso («llega en la próxima fase»). Los botones de crear/unirse a un grupo ya están dibujados pero apagados.
   - **TOP**: la mejor marca de cada jugador (tiempo aguantado, Pokémon, derrotados, nivel, ★ si mató al jefe), con tu fila resaltada.
   - **SALIR** te devuelve a tu sitio con tus cosas (mismo efecto que `/survivor salir`).
3. **La partida**: tu Pokémon se hace grande (~1,7 bloques de alto, sea cual sea) y la cámara pasa a **cenital fija** (como las carreras y la huida, el resto de la interfaz se esconde). `WASD` se mueve respecto a la **pantalla** (W = arriba), sin salto; el ataque es automático. Los enemigos son **Pokémon de Cobblemon de todas las especies que andan** (1 de cada 10 es un **Michialien**) y llegan cada vez más rápido y más fuertes.
   - **Experiencia**: cada enemigo suelta una **michiestrellita** (gema); se recoge pasando cerca (el imán la atrae). Al llenar la barra **subes de nivel: la partida se pausa y eliges 1 de 3 cartas** (30 s, si no se elige una al azar): un ataque nuevo, subir un ataque que ya tienes (hasta nivel 5) o una mejora pasiva. Se pueden llevar hasta 6 ataques y 6 pasivas; sale más a menudo subir lo que ya tienes que lo nuevo.
   - **Ataques** (10): Disparo Rápido, Orbes Lunares, Onda Expansiva, Rayo en Cadena, Lluvia de Meteoros, Tajo Frontal, Disco Boomerang, Charco Tóxico, Púas Ocultas y Rayo Giratorio. Cada Pokémon **empieza con el suyo según su tipo** (fuego/agua/hielo/fantasma/normal → Disparo con su nombre propio: Ascuas, Pistola Agua, Rayo Hielo, Bola Sombra…; eléctrico → Rayo en Cadena; planta → Orbes; lucha/bicho/dragón/siniestro → Tajo; veneno → Charco; tierra/psíquico/hada → Onda; volador/acero → Boomerang; roca → Meteoros) y las partículas del primer ataque son del color de su tipo.
   - **Pasivas** (9): Corazón Michi (+20 vida), Hierba Curativa (+0,6 vida/s), Zapatos Veloces (+8% velocidad), Garras Afiladas (+12% daño), Reloj de Arena (ataques un 8% más rápidos), Lupa Estelar (+10% tamaño de los ataques), Imán de Estrellas (+30% radio de gemas), Sombrero de Sabio (+10% experiencia) y Escudo Lunar (−8% de daño recibido).
   - **Campeones** (enemigos grandes con mucha vida) cada minuto, y a los **3:00 el jefe: «Snorlax Emi»** (el de Emipokemon; si no está el mod, un Snorlax normal), que persigue despacio y alterna dos ataques **avisados con partículas rojas en el suelo** para poder esquivarlos: un salto aplastante (círculo rojo) y una carga en línea recta (línea roja). Al derrotarlo: +2 niveles y la mitad de tu vida.
   - Tienes la vida del minijuego (100 + mejoras); cada contacto con un enemigo quita al principio 5-12 (sube con el tiempo; el jefe, 25 y 35-40 con sus ataques) y hay 0,4 s de gracia entre golpes. **Al llegar a 0 la partida acaba** (o con `/survivor parar`), y vuelves a la sala con el resultado.
   - **Michiestrellitas** al acabar: `segundos/12 + derrotados/15 + nivel×2 (+20 si mataste al jefe)`, por (1 + Suerte). **El tiempo que cuenta para el top es el que aguantaste vivo** (sin contar las pausas de las cartas); solo cuenta tu mejor marca.
4. Si te desconectas en plena partida, la partida se acaba (se te apunta lo jugado) y al volver a entrar apareces en la sala; si te vas de la sala por otro camino (un comando de otro mod: `/spawn`, `/home`…), en un segundo se te devuelven tus cosas donde estés.

### Comandos
Para todos: `/survivor entrar`, `/survivor salir`, `/survivor menu` (en la sala), `/survivor top [cuántos]` (en el chat), `/survivor parar` (tu partida).
Solo administradores (operador nivel 2 o permiso `emi_survivors.admin`):
- `/survivor prueba [hueco]` — partida de prueba directa (sin pasar por la sala) con el Pokémon del hueco 1-6; no cuenta para el top ni da monedas, y al acabar te devuelve a tu sitio.
- `/survivor medida <enemigos 0-400> [hueco]` — el modo de medida de la fase 0 (número fijo de enemigos desde el principio, sin oleadas ni jefe).
- `/survivor inmortal [on|off]` — las siguientes partidas son de prueba: no pierdes vida y no cuentan para el top ni dan monedas.
- `/survivor parar` (sin estar en partida) — cancela todas las partidas. `/survivor estado` — qué pasa ahora (tiempo, enemigos, vida y ms por tick de cada partida).
- `/survivor top reiniciar` — vacía el top (guarda una copia del anterior en `emi_survivors/`).
- `/survivor dev …` (solo en tu propia partida): `xp|nivel|tiempo <n>`, `jefe`, `elite`, `curar`, `sinxp` (alterna: no ganas experiencia), `arma|solo|pasiva <id> [nivel]`, `monedas <n>`.

## Cómo funciona
- **Dos mundos propios** (`data/emi_survivors/dimension*/`, vacíos tipo «void» con generador plano; el bioma `emi_survivors:pradera` solo da el cielo y los colores):
  - `emi_survivors:entrada` (la **sala**, de noche): se construye sola la primera vez que arranca el servidor (`Mundo.construirSala`: ~1 s; la marca de «ya construida» es un bloque de obsidiana llorona bajo el centro). Ahí no se rompe ni se toca nada (`UseItem/UseBlock/UseEntity/Attack…Callback` en `EmiSurvivors`); los objetos que alguien tire al suelo se borran cada segundo y se devuelve la estrella del menú a quien la pierde.
  - `emi_survivors:arena` (de día): **una arena por partida**, en `x = hueco×1024`, `z = (VERSIÓN−1)×1024` (`Arena.origen`). Isla circular de hierba de **radio 46** con un **muro** (amatista y bloques rosas y lilas, con farolas de luz perlada) que no se puede cruzar, flores, manchas de musgo y rocas, una **luna creciente amarilla** en el centro y, fuera, cuatro islotes con estatuas gigantes de peluches Michialien (corona, aureola, sombrero de bruja y gafas de sol; voxelizadas de los modelos de `emi-plush`, JSON en `data/emi_survivors/estatuas/`). Se construye **la primera vez que se usa cada hueco** (~1 s con el pack entero, ~2,2 s en el banco ligero: esa vez el servidor se queda parado un momento) y queda guardada en el mundo. **Si se cambia el diseño de la arena hay que subir `Arena.VERSION`**: así las arenas nuevas salen en otro sitio (`z` distinta) y no se mezclan con las viejas.
- **Varias partidas a la vez** (`Partida`, `Run`): cada partida es de **un** jugador en su arena; hasta `MAX_PARTIDAS` = 4 a la vez (`-Demi.survivors.max_partidas=N`). Cada tick se llama a cada partida; la cantidad de enemigos se reduce sola si el servidor va lento (`Partida.factorRendimiento`, según los ms por tick). Mientras haya alguna partida, se **pausan en memoria** el clearlag y los eventos con aviso de Emipokemon (`Pausas`, igual que en las carreras y la huida) y se reanudan al acabar la **última**.
- **El jugador** (`Partida.prepararJugador`, `Saved`, `Morph`): aventura, «inmortal» para Minecraft (la vida es la del minijuego), inventario vacío, sin salto, velocidad fija (~6 bloques/s por las mejoras), contorno brillante, transformado con CobbleMorph (`/transform` por reflexión) y a tamaño normalizado (atributo `GENERIC_SCALE`; el modelo transformado lo respeta). `Saved` guarda y devuelve inventario, posición, modo, hambre, reaparición y transformación (`world/emi_survivors/saved/<uuid>.nbt`; con la marca `lobby` si entró por `/survivor entrar` y no por una prueba de administrador).
- **Enemigos** (`Enemigo`, `Especies`): `PokemonEntity` reales de Cobblemon **sin IA**, invulnerables, silenciosos y con la etiqueta `emi_survivors`; **se mueven con `Entity.move`** (con la IA apagada la física normal no los mueve). Salen en un anillo a 21-26 bloques, de las ~620-680 especies que caminan (sin legendarios/míticos/paradoja), en dos tramos por total de estadísticas base (180-330 y 331-405), escaladas a 1,0-1,7 bloques de alto; los normales tienen 24 de vida, los fuertes 42 y los Michialiens 36 (todo ×`vidaMul`; 16 aspectos de `emi-plush`: normal, dorado y accesorios). **Elite**: 240 de vida, 2,4-2,9 bloques de alto. **Jefe**: `1200 + 4×(t−180)` de vida, 4,6 de alto. La vida y el daño son nuestros, no de Minecraft. Se **separan entre sí** con una rejilla de 1,6 bloques.
- **Números del juego** (`Curvas`, `Meta`, `Catalogo`, `Cartas`, `Stats`: lógica pura, se prueba sin Minecraft con `test/probar.sh`): experiencia para pasar del nivel *n* al siguiente `round(5 + 4,5n + 0,22n²)` (10, 14, 19…, 72 en el 10); enemigos a la vez `min(120, 22 + 0,42·t)`, creados a `1,6 + t/45` por segundo; vida ×`(1 + t/140)`, daño ×`(1 + t/420)`, velocidad ×`(1 + min(0,35, t/900))`; probabilidad de salir del tramo fuerte `min(0,75, 0,05 + t/400)`. **Todos son primeras estimaciones: nadie los ha jugado a mano** (ver «No probado»).
- **Persistencia** (`Datos`, JSON en `world/emi_survivors/`): `jugadores/<uuid>.json` (michiestrellitas, partidas y, por Pokémon, mejoras, mejor tiempo y partidas) y `top.json` (la mejor marca de cada jugador, máx. 100). Se escribe a un archivo temporal y se renombra. Las partidas de prueba no tocan nada de esto.
- **Red** (`net/`): el servidor manda `EstadoPayload` (vida, tiempo, enemigos, nivel, experiencia, jefe, ataques y pasivas; cada 4 ticks), `GemasPayload` (las gemas, que **no son entidades**: la lista va al cliente, que las dibuja con la textura de la michiestrellita) y `TextoPayload` (JSON de las pantallas «cartas» y «menu»); el cliente manda `AccionPayload` (jugar, comprar, carta, menú, salir…). **El servidor lo comprueba todo** (el cliente no es de fiar) y no manda nada a quien no tenga el mod (`canSend`; `/survivor entrar` se niega con un aviso).
- **Cliente** (`client/`, `mixin/`): `CamaraSurvivors` (cenital inclinada, 14 de alto y 6 al sur), `KeyboardInputMixin` (teclas relativas a la pantalla), `Ocultar` (esconde lista de Pokémon, JourneyMap, Jade, CobbleDollars…), `SurvivorsHud`, `CartasPantalla`, `MenuPantalla` (con `Lienzo`: un lienzo de 640×360 que se escala a la ventana), `GemasCliente`. Compatible con Better Third Person. No suena la música de Minecraft en estas dimensiones. Los modelos del menú se dibujan con `PokemonGuiUtilsKt.drawProfilePokemon` de Cobblemon.
- `EnemigoSinEmpujonesMixin`: los enemigos no se empujan entre sí ni calculan apretujones (cuesta mucho con decenas de entidades).

## Lo que aprendí construyéndolo
- **Un Pokémon de Cobblemon con la IA apagada no se mueve con la física normal**: hay que moverlo con `Entity.move`.
- **Con muchos enemigos delante, el jugador desaparece de la vista**: contorno brillante (`setGlowing`) y cámara bastante empinada (~67°).
- **La hierba bajo un bloque se vuelve tierra** (ticks aleatorios): lo que se pone encima lleva piedra debajo.
- **El tamaño del Pokémon del jugador se normaliza** (un Unown diminuto y un Snorlax se ven parecidos): `GENERIC_SCALE` con un modificador temporal; el modelo de CobbleMorph lo respeta.
- **Mundos «void» con generador plano**: las islas se construyen con colocación masiva de bloques (`Obra`, con las banderas `NOTIFY_LISTENERS|FORCE_STATE`) y la marca de «ya construida» es un bloque de obsidiana llorona.
- **Modelo 3D de Cobblemon en una pantalla**: `drawProfilePokemon(…)` recibe el **centro visual** del modelo (no los pies); hay que probar posiciones hasta que quede dentro del recuadro.
- **Menú y doble clic**: la primera vez que se usa una arena el servidor tarda 1-3 s; un segundo clic en JUGAR llegaba después y «reabría» el menú sobre la partida. Ahora el botón se apaga al pulsarlo, el servidor ignora el segundo, y el cliente cierra el menú en cuanto llega el estado de la partida.
- **Cambiar la ranura elegida desde el servidor no avisa al cliente**: hay que mandar `UpdateSelectedSlotS2CPacket` (si no, la estrella del menú está en la ranura 5 pero el cliente sigue con la 1).
- **Lo que se cambia por mod en el jugador y se guarda con él** (`Invulnerable`, `Glowing`) **sobrevive a una caída del servidor**; los atributos temporales no. Por eso al devolver las cosas siempre se llama a `Partida.quitarEfectos`.
- **Banco de pruebas** (para quien lo repita): con la máquina de 4 CPU, un servidor ligero (solo Fabric API, Kotlin, Cobblemon, GeckoLib, PokeBlocks, CobbleMorph, `emi-plush` y este mod) arranca en ~1 minuto y un segundo cliente (otro usuario y otra pantalla virtual) permite probar varias partidas a la vez. Los clics de un robot sobre el menú fallan si el ratón no ha pasado antes por encima: se mueve, se espera ~0,5 s y se pulsa. Los comandos de consola con `execute as <jugador> run …` mandan la respuesta a la consola, no al jugador.

## Medidas (2026-10-16)
**Ojo con lo que significan:** máquina de pruebas de 4 CPU **compartidas** entre el servidor y el cliente y **sin tarjeta gráfica** (el cliente dibuja por software); por eso el tick del servidor incluye al cliente peleándose por la CPU y los FPS solo sirven para comparar. El «servidor ya gasta ~20 ms por tick sin ningún enemigo» cuando va con **todo el pack** (~180 mods).

**Coste para el servidor de UNA partida** (ms por tick; el límite de un servidor sano son **50 ms** = 20 ticks por segundo; «este mod» = lo que gasta `emi_survivors` en su parte del tick):

| Dónde | Enemigos a la vez | Servidor entero (ms/tick) | Este mod (ms/tick) |
|---|---:|---:|---:|
| Banco ligero (sin el resto del pack), partidas de 4-5 min | 20-120 | 3-5 | 1-3 |
| **Pack entero** (181 mods), jefe incluido | 64-106 | 20-39 | 4-7 |

Cómo leerlo (con honestidad):
- Con el pack entero el servidor **ya gasta ~20 ms por tick sin ninguna partida** (los demás mods) y en esta máquina el cliente (dibuja por software) le quita CPU; con una partida llegó a 20-39 ms (media de 100 ticks). **Este mod** gastó 4-7 ms con 64-106 enemigos y el jefe.
- **Con 4 partidas a la vez** el coste de este mod se multiplicaría por ~4 (≈16-28 ms, **es una extrapolación, no se midió**): sobre los ~20 ms de base rozaría los 50. El reductor automático (`Partida.factorRendimiento`: con el tick medio de más de 38 / 42 / 46 ms se permiten el 75 / 50 / 30 % de los enemigos) debería proteger al servidor, pero **no se ha probado con 3-4 partidas** y el servidor real (HolyHosting, 8 CPU) puede ir mejor o peor que esta máquina. Si en el servidor real hay lag, `-Demi.survivors.max_partidas=2` baja el máximo.
- **Hay un tirón al empezar**: la primera vez que se usa cada arena el servidor se queda parado ~1 s con el pack entero (2-3 s en el banco ligero: «Can't keep up! … 55 ticks behind»); ocurre como mucho una vez por hueco (4) y otra vez si se sube `Arena.VERSION`. Con el pack entero se vieron además picos de ~1,1-1,3 s en el primer minuto de la partida (arena + creación de los primeros enemigos); no se separó la causa.
- La sala se construye al arrancar el servidor la primera vez (~1 s).
- Los FPS del cliente (por software, sin GPU) fueron ~5-10 con 30-100 enemigos: **solo valen para comparar**; la interfaz de la partida enseña «Servidor ms/tick · mod ms · FPS» para que cualquiera pueda medirlo en su PC. En la fase 0 se midió que el cliente es el cuello de botella probable (de ~7,6 FPS con 0 enemigos a ~3 con 256).

**Ritmo y dificultad** (banco ligero; **jugadores simulados, no personas**, así que solo sirven de referencia):
- Un jugador **quieto** (modo mortal) murió a los **27 s**: obliga a moverse.
- Un jugador que **solo da vueltas en círculo** (8 direcciones, con el ataque inicial y sin ir a por las gemas) aguantó **4:28** (nivel 4, 89 derrotados) y no llegó a matar al jefe, que salió a los 3:00.
- Un jugador **quieto e inmortal con cartas automáticas** (se eligen solas al cabo de 1 s) llegó a **nivel 9 a los 2:00, 13 a los 3:00 y 21 a los 4:28** (1167 derrotados) y mató al jefe unos 27 s después de que saliera. Es solo para ver el ritmo de subida de nivel: un jugador real se mueve y recoge gemas de otra manera.
- Los números de las curvas son primeras estimaciones: **hay que jugarlo con gente y retocarlos** (`Curvas.java`, `Meta.java`, `Catalogo.java`; los textos del menú y las cartas salen de esos archivos).

## Probado / no probado (0.2.0)
**Probado** en el juego (servidor Fabric de pruebas + cliente real de Minecraft 1.21.1; teclas y clics simulados con un robot; capturas):
- **Servidor ligero y servidor con el pack entero** (181 mods, Emipokemon incluido): arranca sin errores de este mod, construye la sala (0,9-1 s) y las arenas (0,9-2,2 s la primera vez), registra las dimensiones y el bioma.
- **La sala y el menú** (cliente ligero y cliente con el pack entero): aspecto de la sala, las 4 pestañas, modelos 3D del equipo, botón JUGAR, resultado de la partida anterior, créditos a PokéSurvivor; entrar/salir (`/survivor entrar|salir|menu`, estrella, terminal, botón SALIR); la sala no deja romper ni tocar nada y borra lo que se tire al suelo.
- **Una partida entera**: transformación y escala, cámara, interfaz, gemas, **subir de nivel con las 3 cartas** (clic en una carta, o al azar a los 30 s), los **10 ataques** (cada uno probado por separado con `/survivor dev solo`), las pasivas, campeones, **el jefe Snorlax Emi** (el de Emipokemon, con el pack entero) y sus dos ataques avisados, la **muerte** (resultado en la sala), `/survivor parar`, y volver con todo en su sitio.
- **Monedas, mejoras y top**: ganar michiestrellitas al acabar, comprar mejoras (y ver su efecto en la siguiente partida: 136 de vida con Vigor 3 y la pasiva), que el top solo apunte partidas de verdad y se recuerde tras reiniciar el servidor.
- **Dos partidas a la vez con dos clientes reales**: cada una en su arena (huecos 0 y 1), acabar una no corta la otra, y `Pausas` solo se reanuda al acabar la última (leído en el código y visto en el registro con una partida).
- **Casos raros**: parar el servidor en plena partida (se devuelve todo), reconectar estando en una arena (vuelves a la sala), reconectar con tus cosas guardadas pero fuera de la sala (te las devuelven), irte de la sala con un comando (te las devuelven en 1 s y te quedas donde estés), doble clic en JUGAR, `/survivor entrar` estando ya dentro (abre el menú), tirar objetos en la sala.
- Las partidas con el pack entero y con 2 jugadores a la vez en el banco ligero **no** dieron errores en el registro del servidor ni del cliente atribuibles a este mod.

**No probado** (decirlo claro):
- **Sonido**: el mod solo usa sonidos de Minecraft (subir de nivel, recoger gemas, jefe derrotado) y la máquina de pruebas no tiene audio.
- **FPS reales** en un PC con tarjeta gráfica (los de aquí son de dibujo por software).
- **Jugarlo «a mano»** con teclado y ratón de verdad: si se siente bien, la **dificultad** y el ritmo de niveles (los números son primeras estimaciones; los medidos con un jugador quieto no valen para juzgar), el jefe, si las cartas se entienden, si el menú se ve bien en pantallas pequeñas o con otro tamaño de interfaz.
- **El servidor real** (HolyHosting, ~12 jugadores, todos los mods) y **más de 2 partidas a la vez**: con 4 el coste del mod se multiplica por 4 (aquí ~4-7 ms/tick por partida con el pack entero) y el servidor ya gasta ~20 ms/tick él solo; el reductor automático de enemigos debería proteger, pero no se ha medido con 3-4 partidas.
- **Varios jugadores en el top** (empates, nombres largos, cambios de nombre) y la pestaña TOP con muchas filas.
- **Que el actualizador de packwiz** entregue bien el jar nuevo a los clientes, y que quien **no** tiene el mod reciba el aviso al hacer `/survivor entrar` (está en el código, pero no se probó en el juego).
- **Una caída real del servidor** (kill -9) en plena partida: se probó parar el servidor de forma normal; lo guardado está en disco, pero no se mató el proceso.
- **El grupo** (solo hay un aviso) y las **mascotas decorativas** de la sala (no hay).

## Compilar y probar
`COBBLEMON_JAR=<Cobblemon-fabric-1.7.3+1.21.1.jar> KOTLIN_JAR=<kotlin-stdlib-*.jar> ./build.sh` (Gradle sin conexión; el jar sale en `build/libs/`). Prueba rápida de la lógica pura (sin Minecraft): `test/probar.sh`.
Propiedades útiles para probar (`-D…` en el servidor): `emi.survivors.debug=true` (escribe medidas cada 5 s), `emi.survivors.max_partidas=N`, `emi.survivors.carta_ticks=N` (los ticks que se dan para elegir carta; 600 por defecto).
