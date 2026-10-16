# Emi Survivors (`emi_survivors`) — 0.1.0 (fase 0: prueba de viabilidad)

Minijuego de **supervivencia contra hordas de Pokémon** (estilo Vampire Survivors / Survivor.io) para el servidor de Emi: tu Pokémon ataca solo, tú solo te
mueves, las oleadas crecen, subes de nivel y eliges mejoras. Servidor + cámara, teclas e interfaz de cliente (hay que instalarlo también en los clientes; ya va
en el pack).

**Inspirado en [PokéSurvivor](https://pokesurvivor.com) de JPTER78.** El dueño le pidió permiso al autor y contestó que *mientras no se venda el juego, todo bien*.
Aun así **no se copia nada suyo**: ni código (su licencia desde el 14-10-2026 es PolyForm Noncommercial; antes era MIT), ni sprites (CC BY-NC), ni música. Aquí el código es
propio y los Pokémon son los modelos 3D de Cobblemon. Solo se toman las ideas del juego. Poner créditos a PokéSurvivor en lo que se enseñe a los jugadores.

## Decisiones del dueño (2026-10-15/16)
- Se empieza por la **fase 0** (esta).
- El compañero son **tus propios Pokémon** (los del equipo de Cobblemon), transformándote en ellos con CobbleMorph, como en las carreras. El equipo no se toca.
- **Al entrar al mundo del minijuego hay un menú** donde subir tus **mejoras** (permanentes, por Pokémon) y **unirte a tu grupo** (hasta 4).
- Se «gana» con un **top por el tiempo que aguantó el equipo** (como la tabla «Grupo» del original; luego también solo, por día/semana/mes).
- **Toque de Emi y Michialien** (no premios): enemigos y jefes del mundo de Emi (Michialiens, Snorlax Emi…), decoración, música propia.
- **Los premios los da el dueño a mano**: no hay premios automáticos. Solo hará falta poder ver el top (comando/pantalla) para dárselos.

## Plan por fases (cada una se prueba antes de pasar a la siguiente)
0. **Prueba de viabilidad** (esta): arena plana, cámara desde arriba, tu Pokémon, ~64 Pokémon persiguiéndote y 1 ataque automático; medir rendimiento.
1. **Núcleo en solitario + entrada**: el **mundo del minijuego con su zona de entrada y el menú** (mejoras y grupo), 1 bioma, experiencia, subir de nivel con 3 cartas, ~10 ataques,
   vida y muerte, primer jefe, **top por tiempo**.
2. **Cooperativo y contenido**: grupos de hasta 4 (una arena por grupo), 3 biomas, ~30 ataques con evolución, objetos, jefes (min. 3, 6, 9, 12, 15), mareas, personalidades de enemigo, música y sonidos propios.
3. **Progresión**: monedas, mejoras permanentes por Pokémon, misiones y logros, tops por periodo, shinies.
4. **Final y extras**: grietas con legendarios, «Sentencia» final, climas, eventos, más temas de Emi.

## Fase 0: cómo se prueba
Solo para administradores (operador nivel 2 o permiso `emi_survivors.admin`):
- `/survivor prueba [enemigos] [hueco]` — partida de prueba para quien lo escribe: `enemigos` 0-400 (por defecto 64), `hueco` 1-6 del equipo (por defecto 1).
- `/survivor parar` — cancela la partida (cualquiera la suya; el admin, la que haya).
- `/survivor inmortal [on|off]` — el jugador no pierde vida (para medir rendimiento sin que acabe).
- `/survivor estado` — qué pasa ahora (tiempo, enemigos, vida y ms por tick).

**Cómo se juega:** `WASD` se mueve respecto a la **pantalla** (W = arriba/norte, D = derecha/este, en diagonal con dos teclas); tu Pokémon mira hacia donde andas y ataca solo al enemigo más cercano
(proyectil del color de su tipo). Tienes 100 de vida; cada contacto de un enemigo quita 5-8 (con 0,4 s de gracia). Al acabar (o con `/survivor parar`) vuelves a tu sitio con tu inventario, modo de juego
y transformación de antes, y el chat te cuenta cuánto aguantaste, a cuántos derrotaste y cuánto costó al servidor.

## Cómo funciona
- **Mundo** `emi_survivors:arena` (`data/emi_survivors/dimension*/arena.json`): llano infinito de hierba, de día. Unas rocas de 2×2×2 (para probar choques) se ponen al empezar y se quitan al acabar (con piedra debajo, porque la hierba bajo un bloque se vuelve tierra y se quedaría la mancha).
- **Enemigos** (`Enemigo`, `Especies`): `PokemonEntity` reales de Cobblemon **sin IA**, invulnerables, silenciosos y con la etiqueta `emi_survivors`. Salen en un anillo a 21-26 bloques (fuera de la vista), de **todas las especies que
  caminan** (680 en Cobblemon 1.7.3, sin legendarios/míticos/paradoja), en dos tramos por total de estadísticas base (180-330 y 331-405), todas escaladas a 1,0-1,7 bloques de alto. **1 de cada 10 es un Michialien** (GatitoAlien con
  sus 16 aspectos de emi-plush: el toque de Emi). Se mueven con `Entity.move` cada tick (colisiones con bloques incluidas) hacia el jugador y se **separan entre sí** con una rejilla (no se apilan). La vida y el daño son nuestros, no de Minecraft.
- **Ataque automático** (`Proyectil`): cada 14 ticks dispara al enemigo más cercano a menos de 15 bloques; el disparo avanza 1 bloque/tick, atraviesa 2 enemigos, los empuja y los hace parpadear en rojo; las partículas son del tipo del Pokémon (`Arena.particula`).
- **Jugador**: modo aventura, inventario guardado y vacío, velocidad fija (~6 bloques/s), sin salto, **contorno brillante** (se ve a través de la horda), transformado con CobbleMorph. `Saved` guarda y devuelve el inventario, la posición y el modo de juego (también si el servidor se para en plena partida: se le devuelve al volver a entrar).
- **Cliente** (`client/`, `mixin/`): cámara **cenital inclinada y fija** (`CamaraSurvivors`: 14 bloques de alto y 6 al sur del jugador, mirando al norte; se puede cambiar con `-Demi.survivors.altura=` y `-Demi.survivors.atras=`), teclas relativas a la pantalla (`KeyboardInputMixin`), FOV fijo, el ratón no gira al jugador, se ocultan la interfaz de Minecraft y las de otros mods
  (`Ocultar`: lista de Pokémon de Cobblemon, JourneyMap, Jade, CobbleDollars; es lo mismo que hacen las carreras y la huida) y se dibuja una interfaz mínima (vida, tiempo, enemigos, derrotados y las medidas de rendimiento). Compatible con Better Third Person (`BetterThirdPersonMixin`). No suena la música de Minecraft en esta dimensión.
- **Se pausan** (en memoria) el clearlag y los eventos con aviso de Emipokemon mientras dura la partida (`Pausas`, igual que en las carreras y la huida).
- `EnemigoSinEmpujonesMixin`: los enemigos no se empujan entre sí ni calculan apretujones (cuesta mucho con decenas de entidades y no hace falta).

## Lo que aprendí construyéndolo
- **Un Pokémon de Cobblemon con la IA apagada no se mueve con la física normal**: aunque se le ponga velocidad cada tick (`setVelocity`), su posición no cambia. Hay que moverlo con `Entity.move` (que también resuelve colisiones y hace andar su animación).
- **Con muchos enemigos delante, el jugador desaparece de la vista** (en 3D los modelos altos tapan lo que hay detrás): se le pone `setGlowing(true)` (contorno brillante visible a través de otras entidades) y la cámara es bastante empinada (~67°).
- **La hierba bajo un bloque se vuelve tierra** (por los ticks aleatorios): las rocas llevan piedra debajo y se devuelve la hierba al quitarlas.
- El **primer Pokémon del equipo puede ser diminuto** (un Unown): si el compañero es muy pequeño casi no se ve; conviene normalizar su tamaño en la fase 1.
- Crear un `PokemonEntity` cuesta (se reparten las creaciones: máx. 3 por tick, y la partida sube de 0 al número pedido en 10 s).

## Medidas (servidor de pruebas, 1 jugador, 2026-10-16)
**Máquina de pruebas:** 4 CPU **compartidas** entre el servidor y el cliente y **sin tarjeta gráfica** (el cliente dibuja por software a 800×450), con el servidor cargando los mods del pack.
Por eso los ms del servidor incluyen el trabajo de todos los demás mods (ya hay ~20 ms por tick sin ningún enemigo) y los FPS **solo sirven para comparar entre sí**, no son los de un PC de verdad.

- «Servidor» = lo que tarda de media un tick del servidor entero (el límite es **50 ms** = 20 ticks por segundo).
- «Este mod» = lo que gasta `emi_survivors` en su parte del tick (mover y separar enemigos, atacar, mandar la interfaz).
- Cada partida duró ~56 s con el jugador **inmortal** y solo se mide la parte «en régimen» (desde 13 s después de empezar, cuando ya han salido todos los enemigos).

| Enemigos a la vez | Servidor (ms/tick) | Este mod (ms/tick) | Pico máximo del servidor | FPS del cliente (sin GPU) | Derrotados en 56 s |
|---:|---:|---:|---:|---:|---:|
| 0 | 19,9 | 0,01 | 400 ms | ~7,6 | 0 |
| 32 | 20,2 | 2,8 | 188 ms | ~5,7 | 47 |
| 64 | 19,5 | 1,9 | 215 ms | ~6,2 | 48 |
| 128 | 22,3 | 2,9 | 148 ms | ~5,1 | 42 |
| 256 | 34,1 | 5,1 | 1175 ms | ~3,0 | 39 |

Cómo leerlo (con honestidad):
- **El servidor aguanta bien hasta ~128 enemigos** (+2,4 ms sobre la base); con 256 sube a ~34 ms, que aún está por debajo de 50 pero con poco margen en un servidor con 12 jugadores y todos los mods. De esos +14 ms, solo ~5 son de este mod; el resto
  se **deduce** que es del propio Minecraft/Cobblemon (hacer andar a 256 entidades y mandarlas al cliente), no se midió por separado.
- **El «este mod» varía ±1 ms de una medida a otra** (el ordenador está compartido y Java «se calienta»): no hay una subida clara entre 32 y 128; sí a 256.
- **Hay un pico al empezar cada partida** (en los primeros 5 s): de 0,15 a 0,4 s en las partidas normales y ~1,2 s en la de 256 y en la primera tras arrancar el servidor. Probablemente es llevar al jugador a la arena, generar su terreno y crear los primeros enemigos; no se separó la causa. En la fase 1 conviene precargar la arena.
- **Los FPS del cliente son lo más flojo.** Aun sin GPU, bajan de ~7,6 (0 enemigos) a ~5 (128) y ~3 (256): dibujar muchos modelos de Cobblemon cuesta en el cliente. En un PC con tarjeta gráfica será mucho mejor, pero **no se ha medido** y puede ser el límite real de enemigos.
  La interfaz de la partida enseña «Servidor ms/tick · mod ms · FPS» para que cualquiera pueda medirlo en su PC.
- Las filas de 128 y 256 se midieron en una ronda anterior, con una compilación casi igual (la de después solo añadió comprobaciones de seguridad); las de 0, 32 y 64, con el jar de esta versión.
- En todas las partidas **0 fallos al crear** enemigos (182 especies distintas en una de 256; 90 en una de 64).

## Probado / no probado (0.1.0)
**Probado** en el juego (servidor Fabric de pruebas + cliente real de Minecraft 1.21.1, 1 jugador «Bot», teclas simuladas con un robot, capturas):
- La dimensión se carga, el jugador va a ella, se **transforma en su Pokémon** (casi siempre el hueco 4 del equipo; el hueco 1 era un Unown diminuto y casi no se veía) y la cámara pasa a **cenital fija** con la interfaz de la partida.
- Las **teclas relativas a la pantalla** (W = norte, D = este, diagonales) se comprobaron con la posición y la orientación del jugador en el servidor y con capturas.
- **Hasta 256 Pokémon de Cobblemon** persiguen al jugador, se mueven con colisiones, no se apilan del todo (se forma un anillo a su alrededor) y salen de todas las especies que andan; el jugador **se ve entre la horda** (contorno brillante).
- El **ataque automático** derrota enemigos (23-48 derrotados en 30-56 s según la prueba) con el proyectil del color del tipo. El **daño al jugador** (con sus 8 ticks de gracia) se probó con el modo mortal (`/survivor inmortal off`): con 200 enemigos la partida acaba con «Te han derrotado.» a los ~13 s (salen tan rápido que no da tiempo a defenderse; no sirve para valorar la dificultad).
- **Al acabar** (por `/survivor parar` o por morir, las dos comprobadas con el jar final) se devuelve todo: cámara, interfaz, posición, **modo de juego**, **inventario**, forma (se destransforma), **sin brillo ni efectos** (comprobado con comandos tras la partida: misma posición de antes, modo creativo, dimensión `overworld`, objetos de vuelta, sin `Glowing` ni efectos).
- Medidas de rendimiento (tabla de arriba) y reanudación de clearlag/bosses al acabar.

**No probado** (decirlo claro):
- **Sonido**: esta fase no tiene sonidos propios, y la máquina de pruebas no tiene audio.
- **FPS reales** en un PC con tarjeta gráfica (los de aquí son de dibujo por software).
- **Jugarlo «a mano»** con teclado y ratón de verdad (si el movimiento y la cámara se sienten bien, si marea, la dificultad): solo se probó con teclas simuladas.
- **Varios jugadores** (esta fase solo admite una partida de un jugador a la vez) y **el servidor real** (HolyHosting, ~12 jugadores, todos los mods).
- **Volver a entrar al mundo con una partida a medias** (`Saved` lo restaura en teoría, no se ha comprobado en una caída real del servidor).
- **Shinies, formas regionales y animaciones raras**: salen 182 especies distintas sin error, pero no se revisaron una a una.

## Compilar
Como `emi-huida`: `COBBLEMON_JAR=<Cobblemon-fabric-1.7.3+1.21.1.jar> KOTLIN_JAR=<kotlin-stdlib-*.jar> ./build.sh` (Gradle sin conexión; el jar sale en `build/libs/`). Probar con `-Demi.survivors.debug=true` (el cliente escribe sus FPS cada 5 s en el log).
