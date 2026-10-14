# Emi Huida (`emi_huida`) — 0.2.0 (prototipo)

Minijuego de **huida de un Pokémon perseguidor** al estilo Crash Bandicoot (plan del documento «Plan evento persecución»: «Nivel 1: La huida del Tyrantrum»).
Mod de servidor + parte de cliente (cámara, teclas e interfaz). **Hay que instalarlo también en los clientes** (ya va en el pack): un cliente sin el mod no puede jugar (no tendría la cámara ni el control).
Necesita Cobblemon (el perseguidor es un Pokémon de Cobblemon sin IA). Dimensión: `emi_huida:selva`.

## Cómo se juega
Igual que las carreras, **sin lobby ni NPC**:
1. El organizador escribe **`/huida abrir`** (aviso a todos con el botón **[UNIRSE]**, se repite cada 60 s).
2. Cada jugador pulsa el botón o escribe **`/huida unirse`** (`/huida salir` para retirarse).
3. El organizador escribe **`/huida empezar`**: se guarda en disco el inventario, la posición, el modo de juego y el hambre de cada uno, y todos aparecen a la vez, **cada uno en su propia copia del nivel**, con una cuenta atrás de 5 s (el Tyrantrum ruge antes de salir).
4. **Controles**: **W** = adelante por el camino (hacia la cámara), **A** = izquierda **de la pantalla**, **D** = derecha de la pantalla, **espacio** = saltar, **shift** = agacharse. El ratón **no gira** al jugador (siempre mira a lo largo del camino) y se corre siempre solo.
5. Si el Tyrantrum te alcanza pierdes; si cruzas la puerta dorada ganas. Al terminar se devuelve a cada jugador su inventario y su sitio (también si se desconecta o se reinicia el servidor: se le devuelve al volver a entrar).
Otros comandos del organizador: `/huida parar` (cancela y devuelve a todos), `/huida cerrar` (cierra la cola sin empezar). Permiso: operador nivel 2 o el permiso `emi_huida.admin` (LuckPerms). `/huida estado`, `/huida ayuda` para todos.

## Cámara (estilo Crash)
La cámara va **por delante del jugador** (6 bloques), a poca altura (3,8) y **mirando hacia atrás**: se ve el camino que viene (los obstáculos están entre la cámara y tú), al jugador corriendo hacia la pantalla y, detrás, al Tyrantrum. Sigue el rumbo del camino (en las curvas gira con él). Como el jugador va de cara a la cámara, `KeyboardInputMixin` invierte izquierda/derecha. `CameraMixin` coloca la cámara; `CamaraHuida.fijarMirada` fija la mirada del jugador al camino cada tick y cada fotograma. Se ocultan la barra de objetos, vida, hambre, experiencia y las barras de jefe (`InGameHudMixin`, `BossBarHudMixin`).
Prueba: con `-Demi.huida.dbgfile=<archivo>` y creando ese archivo, la cámara se pone al lado del Tyrantrum.

## El nivel (`Nivel.java`)
Camino de ruinas de selva de **815 bloques** (unos 2,5 min), con **9 curvas** (arcos de 90° de radio 12-16, dos curvas en S), de **7 a 9 bloques de ancho** (varía), con **muros de ruinas irregulares** (alturas distintas, columnas altas, tramos bajos), antorchas, **selva alrededor** (hierba y árboles) y suelo de piedra musgosa **uniforme** (sin franjas de color antes de los obstáculos). Está definido como una línea central (rectas y arcos); cada celda del terreno se asigna a la muestra más cercana y de ahí salen suelo, muro, selva, huecos, puentes y obstáculos. Los obstáculos solo van en las rectas (así son bandas limpias alineadas con los ejes). El nivel se construye **una vez por sesión del servidor** (~1 s la primera copia) y en cada partida solo se vuelven a poner los obstáculos y las tablas.
**Obstáculos** (47 en total, `MODS`): **T** tronco (saltar), **B** barra baja con raíces colgando (agacharse), **L / R / C** pilares a la izquierda / derecha / centro (esquivar; en serie forman eslálones), **H** hueco de 2 con lava (saltar; hay un tipo **W** de 3 para más dificultad), **P** puente de 14 de largo con tres tablas de ancho que **se derrumba** al pisarlo (hay que correr sin parar). Combinaciones: escalera de troncos, hueco+tronco, barra+pilares…
Caer a la lava o salir del camino = error y vuelta al punto seguro anterior (antes del hueco/puente); el Tyrantrum queda al menos a 9 bloques.

## El Tyrantrum
Tyrantrum real de Cobblemon, **~9 bloques de alto** (escala fijada), sin IA, que el mod mueve por el camino. Ruge (`cry()`) antes de salir y corre con la pose de correr de Cobblemon (`setSprinting`). Destruye los obstáculos y las tablas que alcanza su boca. Va **a una distancia que se acorta con el tiempo** (12 → 8,5 bloques; si te adelantas de más acelera, si fallas no te espera); te atrapa a 6 bloques. Velocidad base 5,3 bloques/s (el jugador corre a 5,6).

## Decisiones tomadas (con el dueño)
- Cámara: **delante del jugador mirando hacia atrás** (como las persecuciones de Crash), poco elevada, con el Tyrantrum cerca.
- Movimiento: **libre con las teclas normales** (no avance automático); mirada fija al camino.
- Varios jugadores: **un circuito copiado por jugador**, todos a la vez (hasta 12; copias separadas `SEP` bloques).
- **Mod nuevo aparte** (no dentro de emi-carreras).

## Ajustes del jugador durante la huida
Modo aventura, inmortal, inventario vacío (se guarda), hambre llena (para poder correr), **sprint forzado** (así el salto es largo), **salto más potente** (+0,18: 0,42 → 0,60; ~2,4 de alto y ~4 de largo corriendo; con el salto normal el margen para un tronco de 1 bloque era de ~0,06 s, injugable) y **agacharse a 4,3 bloques/s**. Si estás transformado con CobbleMorph te pide destransformarte (`/untransform`).

## Lo que aprendí construyéndolo (por si se toca)
- El servidor **no marca `horizontalCollision` en los jugadores** (el movimiento lo calcula el cliente): los choques se detectan mirando si la caja del jugador toca la forma de colisión de un bloque de obstáculo (`tocaObstaculo`; los bloques sin colisión, como las raíces colgando, no cuentan).
- Un salto caminando en Minecraft solo cubre ~2 bloques; corriendo, ~4. Por eso se fuerza el sprint.
- Moviéndose siempre por la tangente del camino, el lateral `v` se mantiene constante: en las curvas basta con W. El jugador gira con el camino porque su mirada se fija al rumbo.
- Depuración: `-Demi.huida.debug=true` (o `-Demi.carreras.debug=true`) escribe la posición en el camino cada 2 ticks (`dbg t=… idx=… v=… y=… s=… chaser=…`); `-Demi.huida.vel_base=3` hace al Tyrantrum más lento (solo para pruebas).
- Compilar: igual que emi-carreras (`COBBLEMON_JAR`, `KOTLIN_JAR`, `./build.sh`). `Nivel` valida al cargar que los obstáculos caben en sus rectas y no se pisan.

## Probado / no probado (0.2.0)
Probado en el juego (servidor Fabric + cliente real, **un solo jugador**, cliente lento en software, con un piloto automático que lee mi posición y salta/agacha/esquiva; con el Tyrantrum más lento para que el piloto pudiera llegar): el nivel entero se completa (815 bloques en 152 s) pasando por todos los tipos de obstáculo, los huecos, los puentes y la meta («¡ESCAPASTE!»); la cámara de frente con el Tyrantrum detrás y el camino por delante, en las curvas también; el control con izquierda/derecha invertidos; las caídas (error y vuelta al punto seguro); la devolución del inventario y del sitio.
**NO probado**: jugarlo a mano con un cliente normal (los márgenes de salto y la velocidad del Tyrantrum a su ritmo real —5,3 bloques/s— hay que sentirlos), varios jugadores a la vez, desconexión o reinicio del servidor a mitad, el rugido (se lanza sin errores pero no lo oí), que la animación de correr sea fluida (se ve al Tyrantrum inclinado en pose de carrera y cambiando de postura entre fotogramas), los 12 jugadores a la vez (construir las 12 copias la primera vez tardará unos segundos).
**Pendiente** (fases del plan): bifurcación con atajo, trampas por proximidad, ocultar también las interfaces de otros mods (lista de Pokémon, minimapa… como las carreras) y una interfaz propia (distancia al Tyrantrum, errores, tiempo), música y sonidos mejores, puntuación y tabla de mejores tiempos, recompensas, pausar clearlag/bosses (como `Pausas` de carreras) y más mundos (Gyarados, Onix, Salamence, Gengar).
