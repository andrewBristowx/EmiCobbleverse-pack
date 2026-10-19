# Emi Fall (`emi_fall`) — 0.2.0 (en desarrollo)

Torneo de **minijuegos por rondas estilo Fall Guys** para el servidor de Emi. **Servidor y partida de un jugador** (`environment: "*"`; con `server` no cargaba en la instancia de Prism): no hay código de cliente, los jugadores no necesitan nada especial (todo sale por chat,
títulos, barra de jefe y entidades «display» de Minecraft). Los jugadores van transformados en **michi** (el GatitoAlien de emi-plush) con CobbleMorph, así que se
ven todos iguales. Pedido del dueño (2026-10-19): cuatro minijuegos —carrera de obstáculos, camino correcto, guerra de bombas por equipos y «busca el símbolo»—.

## Cómo se juega
1. `/fall entrar` → sala `emi_fall:entrada` (se guarda todo lo del jugador: inventario, sitio, modo, hambre… y se devuelve con `/fall salir`).
2. `/fall unirse` → te apuntas al torneo (el primero es el anfitrión, hasta 16). Hay un botón `[EMPEZAR]` en el chat.
3. `/fall empezar [juegos]` (anfitrión o administrador; hacen falta 2 jugadores, o `-Demi.fall.prueba=true`/ser admin): por defecto las 4 rondas en este orden:
   `carrera`, `camino`, `bombas`, `plataformas`; con `juegos` se eligen otros (`/fall empezar bombas,plataformas`).
4. Cada ronda: título con la regla (7 s) → cuenta atrás 3-2-1 (nadie se mueve) → juego → resultados (puntos) → siguiente. Al final sale el campeón y todos vuelven a la sala.
5. **Puntos por ronda**: `n - puesto` (n = jugadores) + bonus 3/2/1 a los tres primeros; el equipo ganador de «bombas» suma +4. Gana el torneo quien más puntos tenga (desempata la última ronda).
   **Los premios de verdad los da el dueño a mano** (como en Survivors): el torneo solo dice quién ganó.

Otros comandos: `/fall estado`, `/fall salir` (te saca del torneo y de la sala), admin: `/fall parar` (cancela), `/fall dev usar` (hace como si pulsaras el botón derecho: para pruebas
de las bombas). Permiso de administrador: operador nivel 2 o `emi_fall.admin` (LuckPerms).

## Los minijuegos (`Mini*.java`, cada uno con su arena en `emi_fall:arena`)
| id | Qué es | Arena (z) | Cómo acaba / clasifica |
|---|---|---|---|
| `carrera` | **Pista larga (384 bloques, desde 0.2.0)** en 11 tramos: piedras sueltas, **barra giratoria**, **puertas** (4 por pared, solo 2 se abren al acercarse), **abismo con trampolín**, **frutas gigantes** (manzanas, melón, bayas y bayas de Cobblemon) que ruedan hacia ti por 4 carriles, dos barras, **camino de islas** sobre el vacío, **cuatro barras seguidas**, otras **dos paredes de puertas**, más frutas (más rápidas) y **tres barras finales**. Si caes, reapareces en el último punto de control (hay 11). Las barras se mueven con una actualización por tick y sin interpolación del cliente. | 0 | llegada; 60 s extra tras el primero; 240 s máximo; los que no llegan, por distancia |
| `camino` | Puente de 14 filas con 3 baldosas de cristal, **estilo Juego del Calamar**: en cada fila solo **una** aguanta (el camino cambia en cada partida, sin saltos de más de una columna para que se pueda cruzar). La falsa se rompe **0,45 s después de aterrizar** en ella (queda rota para todos): si saltas a la siguiente antes de que se rompa, **pasas sin caer** (corriendo y saltando de baldosa en baldosa se cruza). Si caes, vuelves a la salida. | 300 | llegada; 40 s extra tras el primero; 120 s máximo; resto por fila alcanzada |
| `bombas` | **Rojos contra azules** (equipos al azar; cuernos de diablo = rojos, aureola = azules, con contorno de color) en una plataforma redonda de 14,5. 7 puntos del suelo dan **bombas** (TNT que gira). **Se ve todo (0.2.0):** al pasar por una, el TNT **sube volando hasta encima de tu cabeza** (los demás lo ven ahí); en la mano **no llevas nada visible** (el objeto es un palo con un modelo del resource pack del mod que se ve como TNT en la barra y no se dibuja en la mano); **clic derecho** = el brazo lanza, el TNT vuela echando humo y chispas, parpadea y, al caer al suelo, explota a los 7 ticks (si toca a alguien, en el acto): **trozos de suelo saltan por los aires**, humo, fuego y un cráter de ~3,3 de radio; lanza por el aire a quien esté a menos de 6,5. Quien cae queda eliminado (espectador). Pasados 70 s el borde se va comiendo hacia dentro. | 600 | gana el equipo con más vivos o el que se queda solo; 150 s máximo |
| `plataformas` | **«Busca el símbolo»**: una pantalla gigante (item display) enseña una Poké Ball (poke/great/ultra/master/premier/luxury/timer/quick); 8 plataformas de colores, cada una con la suya encima. Al acabar el tiempo (7 s la primera ronda, 3 s al final) **desaparece todo el suelo menos esa plataforma** (7×7, luego 5×5 y 3×3); quien no está encima cae. Mientras tanto **caen yunques** (con aviso de humo en el suelo) que empujan a otras plataformas. | 900 | el último en pie (máx. 14 rondas); clasifica por la ronda en que cae |

Arquitectura: `Torneo` (cola, rondas, puntos, bossbar, transformación), `Ctx` (ayudas: colocar, eliminar, empujar), `Mini` (interfaz de un minijuego), `Minijuegos` (lista),
`Sala`, `Saved` (guarda/devuelve lo del jugador, copiado de emi-huida), `Retornos` (si el servidor se corta con gente dentro, al reconectar se les devuelve todo),
`Disp` (bloques, objetos y textos «display» con NBT), `Morph` (CobbleMorph por `/transform`). Para añadir un minijuego: una clase `Mini` y una línea en `Minijuegos`.

## Pruebas
`-Demi.fall.tiempo=0.3` acorta a un 30 % la cuenta atrás, los límites y los resultados (para probar el torneo entero en un par de minutos).

## Probado / no probado (0.1.0)
Se ha probado en el banco de pruebas ligero (servidor Fabric con Cobblemon, CobbleMorph y emi-plush + **dos clientes reales** Bot y Bot2, sin audio):
sala, cola, empezar, transformación en michi, cuenta atrás, barra de jefe y HUD de cada ronda; **camino correcto** (caer, reaparecer, contar caídas); **carrera** (arena
vista entera, trampolín medido con un jugador: salta ~8 bloques sin pulsar adelante, la barra giratoria lanza ~8 bloques); **bombas** (recoger, lanzar, explosión que quita 28
bloques y lanza al rival unos 6 bloques); **plataformas** (arena con las 8 Poké Ball, pantalla, eliminación del que no se mueve); torneo de 4 rondas con puntos.
**No probado**: jugarlo a mano con más de 2 personas (dificultad, ritmo, lag con 12-16 jugadores y el pack entero), esquivar de verdad las barras y las bolas, las puertas,
el choque entre varios jugadores, que se vean bien las Poké Ball de la pantalla desde todas partes, sonidos (la máquina de pruebas no tiene audio), el servidor real, y qué pasa si alguien
se desconecta en pleno minijuego. El mensaje «Display entity: Not a string» del registro sale con **cualquier** `summon text_display` en el banco de pruebas: es ajeno al mod.
**0.1.1:** probado en el banco de pruebas con un **cliente en partida de un solo jugador** (como la instancia de Prism): `/fall entrar`, `/fall unirse` y `/fall empezar camino` salen bien (sala, barra «Ronda 1/1 · Camino correcto», interfaz de filas y caídas). Con 0.1.0 (`environment: "server"`) en ese mismo caso el mod no cargaba («Missing data pack emi_fall»). En un solo jugador hay que ser operador (partida con trucos) o arrancar con `-Demi.fall.prueba=true`.

**0.2.0 (pedido del dueño tras probar la 0.1.1):** carrera el doble de larga, frutas en vez de cubos naranjas, barras que de verdad giran, camino de cristal que se cruza saltando y bombas visibles. El mod ahora lleva un **resource pack** (`assets/minecraft/models/item/stick.json` + `assets/emi_fall/models/item/bomba.json`): sin él en el cliente, la bomba se vería como un palo en la mano (todo lo demás funciona igual). **Probado** en el banco ligero con dos clientes reales: arena nueva de la carrera construida sobre la vieja (islas, 4 barras, puertas, frutas con bayas de Cobblemon, meta lejana, punto de control al caer), 3 capturas seguidas de una barra con ángulos distintos, camino de cristal cruzado corriendo y saltando con un piloto automático (llegó el primero en 9 s: ¡cruzar a ciegas saltando funciona, quizá demasiado fácil! `MiniCamino.RETRASO` = 9 ticks lo regula), bomba cogida (vuela hasta la cabeza y se ve desde otro cliente), mano vacía en primera persona con el TNT en la barra, lanzada con clic derecho real y explosión con escombros y cráter. **No probado**: jugarlo con gente de verdad (largo y dificultad de la carrera nueva, ritmo de las frutas), que las barras giren fluidas en el cliente del dueño (en el banco sí), puertas y frutas golpeando de verdad, el modelo invisible con otros resource packs, el servidor real.
