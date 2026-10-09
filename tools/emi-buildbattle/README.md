# Emi BuildBattle (`emi_buildbattle`)

Minijuego de construcción para el servidor de Emi. **Mod solo de servidor** (Fabric 1.21.1, Fabric API): los jugadores no necesitan instalarlo; en el pack se
distribuye igual porque el cliente lo ignora (`"environment": "server"`).

## Cómo se juega
1. El organizador escribe **`/bb abrir`**: sale un aviso con un botón **[UNIRSE]** para todos (y un título en pantalla). Se repite cada 60 s mientras la cola esté abierta.
2. Quien quiera juega pulsando el botón o escribiendo `/bb unirse` (solo entra quien se apunta). `/bb salir` para echarse atrás.
3. **`/bb empezar [tema]`**: cuenta atrás de 5 s y todos aparecen en su **parcela de 64×64** (muros de ladrillo, altura de construcción 64) en una dimensión vacía aparte
   (`emi_buildbattle:arena`: sin mobs ni Pokémon, siempre de día). Antes se guarda en disco su inventario, posición, modo de juego y punto de reaparición, y se les vacía el inventario.
   El tema se puede cambiar cuando quieras con `/bb tema <texto>`; sale en pantalla y en la barra de acción.
4. En cada parcela hay un **aldeano «Constructor»**: con clic derecho abre un menú de bloques gratis (también `/bb bloques`). Categorías = pestañas del inventario creativo,
   páginas, botones de ±10 páginas y una **búsqueda** (brújula → escribes el nombre en un yunque, entiende bastante español: «escalera de roble», «bloque de oro»…; también `/bb buscar <texto>`).
   Clic izquierdo = un stack, clic derecho = 1 bloque. Se puede pedir todas las veces que haga falta.
5. Opcional: **`/bb temporizador <min>`** pone una barra de tiempo con avisos (5, 2, 1 min, 30 s, cuenta final). Solo avisa: no cambia de fase. `/bb temporizador cancelar`.
6. **`/bb votar`**: se les vacía el inventario, pasan a modo aventura volando y reciben 9 lanas numeradas del 1 al 9 (clic derecho = votar; se puede cambiar hasta que pases a la siguiente).
   Todos son teletransportados a una construcción (orden aleatorio) y no pueden votar la suya. Los organizadores ven en la barra de acción cuántos han votado y un aviso con botón [SIGUIENTE] cuando han votado todos.
7. **`/bb siguiente`** pasa a la siguiente construcción (`/bb anterior` vuelve atrás). En la última, **`/bb siguiente`** revela el top: título + chat de 3.º a 1.º con pausas, fuegos artificiales sobre la construcción ganadora y clasificación completa
   (media, total y nº de votos; los empates comparten puesto, **el desempate y el premio los decide el organizador**). `/bb top` repite la clasificación.
8. **`/bb terminar`**: todos recuperan sus cosas (inventario, posición, modo, spawn) y se limpia la arena (por tandas, sin congelar el servidor). **Antes de limpiar, la construcción de cada jugador se guarda como schematic de WorldEdit**: `config/worldedit/schematics/bb-<jugador>.schem` (se carga con `//schem load bb-<jugador>` y `//paste`). Se guardan los bloques y los datos de cofres, carteles, etc.; no se guardan el suelo de la parcela sin tocar, el muro, las barreras de la jaula ni las entidades (cuadros, soportes de armadura, objetos en marcos). Quien no construyó nada no genera archivo. Si ya existía uno con ese nombre, el viejo pasa a `bb-<jugador>-<fecha>.schem` (no se pisa). También se guarda con `/bb parar`.

Otros: `/bb parar` (cancelar en cualquier momento, devuelve todo), `/bb cerrar` (cerrar la cola sin empezar), `/bb expulsar <jugador>`, `/bb espectar <jugador>` + `/bb volver` (mirar una parcela en espectador sin tocar tu inventario),
`/bb estado`, `/bb ayuda`, `/bb recargar` (relee la config).

Permisos: los subcomandos del organizador piden ser **operador (nivel 2)** o el permiso **`emi_buildbattle.admin`** de LuckPerms (a un grupo de moderadores, por ejemplo). Los de jugador (`unirse`, `salir`, `bloques`, `buscar`, `estado`, `ayuda`) son para todos.

## Reglas y protecciones
- Se juega en **supervivencia** (no hay inventario creativo: los bloques salen solo del Constructor y se gastan al colocarlos), pero volando, sin daño ni hambre y con un `Haste` + modificador de velocidad de rotura
  (temporal, no se guarda) para que romper sea casi instantáneo aun volando. Cada 0,5 s se borra del inventario todo lo que no esté en el catálogo (lo que suelten los bloques al romperse, TNT, armas…). Se permiten además cuadros, pinturas,
  soportes de armadura, cubo, cubo de agua y polvo de hueso (config `extraItems`).
- **No se puede salir volando**: sobre el muro hay una jaula invisible de bloques barrera hasta el techo (y un techo de barreras a 64 bloques sobre el suelo), que no se pueden romper. Aun así el servidor te devuelve a tu parcela si lo consigues por otro medio.
- Solo se puede colocar/romper dentro de tu parcela (no el muro, las barreras ni la base de bedrock). Si sales de la dimensión por la razón que sea, vuelves a tu parcela.
- En la arena se borra cada segundo todo lo vivo salvo jugadores, el Constructor y la decoración colocada (incluye los Pokémon que saque un jugador).
- **Catálogo**: todos los bloques de todos los mods del servidor (≈14 000 en el pack completo) menos una lista negra (`blockedContains`, `blockedItems`, `blockedNamespaces` en `config/emi_buildbattle.json`: bloques de comandos, estructura, jigsaw, spawners, TNT, ancla de reaparición…).
  **Se excluyen automáticamente los objetos que son tarea de alguna misión de FTB Quests** (se leen de `config/ftbquests/quests`), para que nadie complete misiones pidiéndolos gratis (`blockQuestItems`).
- Si el servidor se cae con una partida en marcha, al volver a entrar cada jugador recupera lo suyo y la arena se limpia al arrancar.
- Si `spawn-npcs=false` en `server.properties` el aldeano no puede existir y el Constructor es un soporte de armadura (funciona igual).

## Configuración (`config/emi_buildbattle.json`, se crea sola)
`saveSchematics` (true), `schematicDir` (`config/worldedit/schematics`), `schematicPrefix` (`bb-`), `plotSize` (64), `plotHeight` (64), `plotSpacing` (96), `plotsPerRow` (6), `floorY`, `wallHeight`, `floorBlock`, `wallBlock`, `minPlayers` (2), `maxPlayers` (24), `queueReminderSeconds` (60), `viewMargin`, `topSize` (3),
`revealSeconds` (6), `stackSize` (64), `blockQuestItems`, listas negras y `extraItems`.

## Compilar
`gradle build` (Fabric Loom 1.10.5, Yarn 1.21.1+build.3; necesita acceso a maven.fabricmc.net). El jar sale en `build/libs/` y se copia a `mods/` del pack.
Para pruebas automáticas existe un comando `/bb _voto <jugador> <n>` que **solo** existe si el servidor arranca con `-Demi.bb.test=1`.

## Probado
En un servidor Fabric de pruebas con un cliente real (capturas incluidas en la conversación) y bots, y arrancando el servidor del pack completo: cola, cuenta atrás, parcelas, Constructor y menú (categorías, páginas, búsqueda),
colocar/romper/muro, limpieza del inventario, temporizador, votación con clic real en las lanas, ceremonia del top, `/bb salir`, `/bb parar`, `/bb terminar` con devolución exacta del inventario/posición/modo, limpieza de la arena entre partidas y recuperación tras reinicio.
**No probado**: una partida con jugadores reales y muchos a la vez, el rendimiento con 24 parcelas, la reconexión de un jugador en mitad de la votación, y un Pokémon sacado por un jugador al entrar en la arena (se descarta por código, no se vio).
