# Emi Huida (`emi_huida`) — prototipo 0.1.0

Minijuego de **huida de un Pokémon perseguidor** al estilo Crash Bandicoot (plan del documento «Plan evento persecución»: «Nivel 1: La huida del Tyrantrum»).
Mod de servidor + una pizca de cliente (cámara). **Hay que instalarlo también en los clientes** (ya va en el pack): el servidor le manda un mensaje al cliente para poner la cámara en tercera persona; un cliente sin el mod no puede jugar.
Necesita Cobblemon (el perseguidor es un Pokémon de Cobblemon sin IA).

## Cómo se juega
Igual que las carreras, **sin lobby ni NPC**:
1. El organizador escribe **`/huida abrir`** (aviso a todos con el botón **[UNIRSE]**, se repite cada 60 s).
2. Cada jugador pulsa el botón o escribe **`/huida unirse`** (`/huida salir` para retirarse).
3. El organizador escribe **`/huida empezar`**: se guarda en disco el inventario, la posición, el modo de juego y el hambre de cada uno, y todos aparecen a la vez, **cada uno en su propia copia del nivel** (separadas 24 bloques), con una cuenta atrás de 5 s.
4. Se corre hacia delante (WASD), **saltar** con espacio y **agacharse** con shift. El Tyrantrum sale 18 bloques por detrás y acelera (4,4 → 5,5 bloques/s; el jugador camina a 5,6). Si te alcanza pierdes; si cruzas la puerta dorada ganas.
5. Al terminar se devuelve a cada jugador su inventario y su sitio (también si se desconecta o se reinicia el servidor: se le devuelve al volver a entrar).
Otros comandos del organizador: `/huida parar` (cancela y devuelve a todos), `/huida cerrar` (cierra la cola sin empezar). Permiso: operador nivel 2 o el permiso `emi_huida.admin` (LuckPerms). `/huida estado`, `/huida ayuda` para todos.

## Decisiones tomadas (con el dueño)
- Cámara: **tercera persona por detrás** (la que ya usan las carreras; con **F5** se puede ver de frente y se ve al Tyrantrum). Más adelante se puede estudiar una cámara en rieles.
- Movimiento: **libre con las teclas normales** (no avance automático).
- Varios jugadores: **un circuito copiado por jugador**, todos a la vez (hasta 12).
- **Mod nuevo aparte** (no dentro de emi-carreras).

## El nivel (prototipo, `Nivel.java`)
Pasillo de ruinas de selva de 7 de ancho y ~190 de largo (unos 30-40 s). Las franjas de color del suelo avisan de qué viene: verde = salta, naranja = agáchate, amarillo = esquiva, rojo = hueco.
1. Tronco (1 bloque): saltar. 2. Raíces bajas (techo a 1,5): agacharse. 3. Rocas en carriles (izquierda+centro, centro+derecha, los lados, el centro): esquivar. 4. Hueco de 2 bloques: saltar (caer = error y vuelves antes del hueco). 5. Dos troncos seguidos. 6. Puerta dorada (meta).
Los obstáculos son **bloques de verdad**; el Tyrantrum los **destruye** al pasar.
- **Errores**: tropezar con un obstáculo (te empuja hacia atrás) o caer al hueco. Se cuentan.
- **Medallas** al escapar: **oro** 0 errores, **plata** ≤ 2, **bronce** el resto (la de tiempo vendrá con las fases siguientes).
- **Ajustes del jugador** durante la huida: modo aventura, inmortal, inventario vacío (se guarda), velocidad fija (0,13), sin correr (hambre en 6), **salto más potente** (+0,08: con el normal el margen para saltar un tronco de 1 bloque caminando era de ~0,1 s) y **agacharse a la misma velocidad que caminar** (atributo `player.sneaking_speed`).
- Si estás transformado con CobbleMorph te pide destransformarte (`/untransform`).

## Lo que aprendí construyéndolo (por si se toca)
- El servidor **no marca `horizontalCollision` en los jugadores** (el movimiento lo calcula el cliente): los choques se detectan mirando si la caja del jugador toca un bloque de obstáculo (`tocaObstaculo`).
- Con A/D mirando al sur, **+x es la izquierda**.
- Depuración: `-Demi.huida.debug=true` (o `-Demi.carreras.debug=true`) escribe la posición cada 2 ticks (`dbg t=… x=… y=… z=…`); `-Demi.huida.vel_ini` / `vel_max` cambian la velocidad del perseguidor sin recompilar.
- Compilar: igual que emi-carreras (`COBBLEMON_JAR`, `KOTLIN_JAR`, `./build.sh`).

## Probado / no probado (0.1.0)
Probado en el juego (servidor Fabric + cliente real, **un solo jugador**, cliente lento en software): abrir/unirse/empezar, entrada a su copia, cuenta atrás, cámara en tercera persona, correr, Tyrantrum visible detrás (cresta roja) y que te alcanza, saltar el tronco, agacharse bajo las raíces (pose CROUCHING, misma velocidad), tropezar con rocas (cuenta error), caer al hueco (error y vuelta), meta («¡ESCAPASTE! Medalla de ORO»), devolución del inventario y del sitio.
**NO probado**: recorrer el nivel entero sin fallos (el piloto automático no lo consiguió por el cliente lento: los márgenes de salto hay que sentirlos con un cliente normal), varios jugadores a la vez, desconexión a mitad, reinicio del servidor a mitad, la sensación de la velocidad del Tyrantrum.
**Pendiente** (fases del plan): ocultar las interfaces que estorban (lista de Pokémon, minimapa… como las carreras) y una interfaz propia, música y sonidos de pisadas/rugidos mejores, el Tyrantrum animado (embestida), puente que se derrumba, bifurcación con atajo, trampas por proximidad, puntos de control, puntuación y tabla de mejores tiempos, recompensas, pausar clearlag/bosses (como `Pausas` de carreras) y más mundos (Gyarados, Onix, Salamence, Gengar).
