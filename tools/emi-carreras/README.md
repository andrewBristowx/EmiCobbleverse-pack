# Emi Carreras (`emi_carreras`)

Minijuego de **carreras de Pokémon** estilo Mario Kart para el servidor de Emi. Toda la lógica es de servidor (Fabric 1.21.1, Fabric API + Cobblemon): los jugadores no necesitan hacer nada.
Desde la 0.1.1 el mod se declara para cliente y servidor (`environment: *`) **solo para que funcione también en un mundo de un jugador o abierto a LAN** (el servidor integrado lo carga); en el cliente no hace nada. Hasta **12 corredores** (se puede jugar con menos, incluso solo), una pista con temática Pokémon, puntos de control,
vueltas, cajas de poderes y turbos. Se corre en su propia dimensión, `emi_carreras:pista`.

## Pistas
Se elige con `/carrera abrir [pista]` (sin nombre, la de `pista` en la configuración); `/carrera pistas` las lista.
| Id | Nombre | Cómo es |
|---|---|---|
| `circuito_pokeball` | Circuito Pokéball | Ancha y fácil, 3 vueltas, estatuas de Pokémon, túnel Pokéball. |
| `estacion_michialien` | Estación Michialien | **Tema Emi / GatitoAlien**: circuito en forma de **luna creciente** (la de la barriga del Michialien) sobre un mar de nubes, 2 vueltas, dificultad media. **Estatuas gigantes de los peluches del Michialien**, cartel enorme con el pixel art de la moneda, portico «EMI» en la meta, farolas con forma de antena, arcos de estrellas, planeta con anillo, luna, OVNI y estrellas. 2 **saltos** sobre las nubes, un **atajo** que se salta la horquilla norte con un salto con turbo, 2 zonas de «mermelada» (lodo) y turbos. Música propia. |
| `volcan_cinnabar` | Volcán Cinnabar | **Más difícil**: pista estrecha con desnivel, 2 vueltas, 2 **saltos** sobre lava, un **atajo** con salto largo, **lodo** y **Voltorb fijos**. Volcán con cráter de lava y estatuas de fuego. |

### Estación Michialien
- Generador: `pista/emi.py` (+ `pista/camino_emi.py`, el trazado de la luna; `python3 emi.py ..`). Necesita `MC_TEXTURAS=<textures/block de Minecraft>` para dibujar bien el cartel (las estatuas ya vienen guardadas en `pista/estatuas/michi_*.json`). Origen (2000, 64, 0); plantilla 304 × 112 × 451.
- **Estatuas de los peluches** (`pista/catalogo_peluches.py`): se voxelizan los modelos de bloque de `tools/emi-plush` (geometría de Blockbench con **UV por cara**, que `modelo.py` entiende desde esta versión) a bloques de color (concreto, lana, terracota, **amatista** para el lila de las antenas) a escala fija (2,0 bloques por unidad: ~26 de ancho y 28 de alto; las dos de la meta, 2,8).
  Formas usadas: base (Michi Dramático), dorado con corona, y con corona, gafas de sol, lazo, aureola, sombrero de bruja, de Papá Noel, pirata, auriculares, bufanda, corona de flores, dorado y sombrero de playa. Para otra forma: añadirla a `ESTATUAS` de `emi.py` (con `MC_TEXTURAS` se crea y se guarda sola).
- **Atajo**: en el arco exterior norte, antes de la horquilla, el **carril interior** (3 de ancho) con su franja de turbo y su plataforma verde con borde azul; con turbo (esa franja, una Baya o un Caramelo) el salto de ~87 bloques aterriza en el arco interior, justo después de la horquilla (se ahorran ~140 bloques de pista); sin turbo el arco acaba en las nubes y vuelves al último punto de control. Ningún punto de control cae en el tramo que se salta.
- **Mermelada** (lodo, rosa y lila): una mancha tras el primer salto (solo media pista) y otra en el arco sur.
- Datos nuevos del JSON: ninguno; el `musica` es `emi` (60 s, re mayor, 128 bpm, `DURACION_MUSICA` 1200 ticks).

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
- **Oculta** la lista de Pokémon de Cobblemon (`HidePartyBinding`), el minimapa de JourneyMap (`ClientAPI.toggleMinimap`), el cuadro de Jade (`IWailaConfig...setDisplayTooltip`) y los avisos emergentes; el contador de CobbleDollars (`ClientConfig.setOverlayPosition(DISABLED)`, solo en memoria; no toca el archivo de configuración); al acabar (o al salir del servidor / cerrar el juego) lo deja como estaba. Todo por reflexión: si falta alguno de esos mods no pasa nada.
- **Oculta también la interfaz de vainilla** (barra de objetos, corazones, hambre, aire, armadura, nivel y barra de experiencia): `mixin/InGameHudMixin` cancela `InGameHud.renderMainHud` y `renderExperienceLevel` mientras `EmiCarrerasClient.activo()`. La mano/inventario no hacen falta: durante la carrera todo el mundo va en **tercera persona**
  (el cliente cambia a `THIRD_PERSON_BACK` al empezar la presentación y devuelve la perspectiva anterior al terminar; con F5 se puede cambiar).
- **Tamaño**: la interfaz se dibuja en un espacio virtual de **300 de alto** (`esc = alto_escalado / 300`, ancho proporcional), así que las proporciones son las mismas con cualquier escala de interfaz y resolución (no depende de «Escala de interfaz»).
- Distribución **al estilo Mario Kart**: **casilla del poder** arriba a la izquierda (objeto grande con un «pop» al cogerlo, nombre, y durante unos segundos lo que hace; vacía = «Sin poder»; si llevas Protección, sale «Protegido N s»);
  **participantes** justo debajo (puesto, cabeza, nombre y vuelta, ordenados y actualizándose); **mapa de la pista** abajo a la derecha (imagen cenital generada por `pista/mapa.py`, `assets/emi_carreras/textures/gui/mapa_<id>.png`, situada con `mapa` del JSON de la pista)
  con la **cabeza de cada jugador** donde está (la tuya con borde blanco) y tu **puesto en grande** debajo del mapa (1º, 2º… en dorado/plata/bronce); **vuelta y tiempo** abajo a la izquierda (bandera de cuadros + «2/3»).
  Con la interfaz activa el servidor ya no repite el texto de la barra (vuelta/puesto/tiempo) a ese jugador.
Se oculta con F1 como cualquier interfaz.

## Sonido
- **Música de fondo** durante la carrera (chiptune original hecha con `musica/generar_musica.py`, ogg dentro del mod): «circuito» (alegre, 51,2 s), «volcan» (oscura y rápida, 40 s) y «emi» (soñadora, con campanitas, 60 s). El servidor la relanza en bucle (las duraciones están en `DURACION_MUSICA` de `Carrera.java`); suena en la categoría **Música** del cliente.
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
| Bostezo del Michi (0.7.0) | los rivales a menos de 14 bloques (por cualquier lado) se quedan **dormidos** 1,7 s y frenados |
| Rayo del OVNI (0.7.0) | un OVNI **abduce al que va primero** (si eres tú, no pasa nada): parada 2,5 s y frenado; el mejorado abduce también al segundo. Solo sale en las cajas a quien no va primero |
| Superestrella (0.7.0) | turbo **y** protección a la vez durante 5 s |
| Grimer pegajoso (0.7.0) | suelta un Grimer detrás de ti: quien lo toque queda **pegado** (muy frenado 5,5 s, sin parar del todo) |
| Nube de tinta (0.7.0) | los que van **delante** de ti quedan **ciegos** 3 s y frenados (solo sale a quien no va primero) |
| Lluvia de michiestrellitas (0.7.0) | te da **+5 michiestrellitas** al instante |
Las franjas amarillas/naranjas del asfalto son **turbos**.

### Michiestrellitas (0.7.0)
- Por cada pista hay grupos de 3 **michiestrellitas** (estrella dorada con la carita del Michialien, hecha por `estrella/hacer_estrella.py`; modelo `emi_carreras:item/estrella_michi` con `custom_model_data` 7020002 sobre `minecraft:nether_star`). Se cogen al pasar, **reaparecen** a los 12 s y el contador sale en la interfaz (debajo de la casilla del poder: «★ 3/10» con barra).
- **10 michiestrellitas mejoran el poder**: si llevas uno pasa a ser **★ mejorado** (dura y pega un 60 % más; la casilla y el objeto llevan una estrella delante y brillan); si no llevas ninguno recibes uno ya mejorado. Si ya llevas uno mejorado, las estrellas se guardan (hasta 10) y mejoran el siguiente poder que cojas.
- **Un golpe de verdad (que no pare el escudo) o una caída te quitan la mitad** (redondeando hacia abajo).
- Las posiciones salen de `estrellas` en el JSON de cada pista (los generadores las ponen); config: `estrellas`, `estrellasMejora` (10), `estrellaRespawnSegundos` (12).
- Pruebas (organizador): `/carrera _estrellas <n>` da estrellas durante una carrera; `/carrera _poder <nombre>` da un poder (también `golpe_<tipo>`: pokeball, voltorb, trueno, bostezo, ovni, mermelada, tinta).

### Eventos de Emipokemon durante la carrera (0.7.0)
Mientras hay una carrera (desde que se empieza hasta que se limpia) `Pausas.java` apaga **en memoria** la limpieza de objetos («clearlag»: aviso + borrado) y los eventos con aviso general de Emipokemon (bosses Mega/Gigantamax/Tera, evento shiny, entrenador legendario), y al terminar los vuelve a encender y adelanta sus temporizadores el tiempo que estuvieron parados.
No se toca el archivo de configuración de Emipokemon; **pero** si un organizador cambia algo de la configuración de Emipokemon con un comando durante la carrera, ese guardado se llevaría los valores apagados. En el cliente, `mixin/BossBarHudMixin` oculta las barras de jefe mientras dura la carrera. Se apaga con `pausarEventos: false`.

## Sala, entrenamiento, logros y tienda (0.8.0)
Pedido del dueño (2026-10-17): «integrar el juego de carreras a algo parecido a lo que hicimos con el survival»: sala de espera, menú, entrenamiento en solitario, logros, tienda, y que Ms. President (el mismo NPC de emi-survivors) deje elegir a qué juego entrar.
- **Sala** (`Sala.java`, dimensión `emi_carreras:entrada`, plataforma de asfalto con arco de meta, terminal = mesa de encantamientos y dos estatuas de michis). **`/carrera entrar`** (o el botón «Carreras» de Ms. President) guarda tu inventario, posición, modo, hambre y transformación en disco (`world/emi_carreras/saved/<uuid>.nbt`, marca `lobby`), te deja como persona en la sala y te da la **estrella del menú** (ranura 5). **`/carrera salir`** (o el botón SALIR) te lo devuelve todo; si estás apuntado a una carrera, `salir` te saca primero de la carrera. Al acabar o cancelarse una carrera todos **vuelven a la sala** (no a donde estaban). Si algo te saca de la sala (otro mod, un comando), en 1 s te devuelven lo tuyo donde estés. **No se puede entrar a un minijuego estando en otro** (carreras ↔ survivors ↔ huida): sale un aviso.
- **Menú** (`client/MenuPantalla`, lienzo fijo 640×360 como el de survivors; se abre solo, con la estrella, tocando el terminal o con `/carrera menu`): **CARRERA** (eliges tu Pokémon —solo los terrestres; el resto sale gris— y una pista; «abrir carrera» crea una **cola** de la que los demás jugadores de la sala pueden entrar, con corredores y «listo»), **ENTRENAR** (tú solo en la pista que elijas: sin esperar a nadie, da la mitad de las michiestrellitas y 3 estrellas por terminar), **LOGROS**, **TIENDA** y **TOP** (mejores tiempos por pista). El servidor manda el estado completo en JSON (`Menu.java`, `net/TextoPayload`) y el cliente pide cosas con `net/AccionPayload` (`Acciones.java`; el servidor lo comprueba todo). La sala empuja el estado a los jugadores cuando cambia la cola.
- **Cola desde la sala**: el que abre es el **anfitrión**; empieza cuando todos están listos (a los 8 s con ≥2 corredores y todos listos, o a mano con «empezar»); si se va, el cargo pasa a otro y, si no queda nadie, la cola se cierra. Los anuncios por chat siguen siendo los de siempre cuando se abre con `/carrera abrir`.
- **Estrellas** (la moneda de las carreras, **no** las michiestrellitas de la pista; esas se cuentan aparte durante la carrera): terminar da 5 (3 en entrenamiento), +25/15/10/5 por el puesto 1.º/2.º/3.º/resto en carreras con ≥2 corredores, +las michiestrellitas que recogiste (la mitad en entrenamiento) y +5 por batir tu mejor tiempo en una pista. Si no terminas no ganas nada. Se guardan por jugador en `world/emi_carreras/jugadores/<uuid>.json` (`Datos.java`, escritura atómica), junto con las estadísticas, los logros y lo comprado; los mejores tiempos de cada pista (top 10) en `tiempos.json`.
- **Logros** (`Logros.java`, 12 familias con escalones y premio en estrellas): Piloto, Campeón, Sube al podio, Cazaestrellas, Viajero (pistas distintas), Entrenador, Francotirador (golpes a rivales), Intocable (ganar sin recibir golpes), Coleccionista (Pokémon distintos), Rompe-récords, Fan de Emi (terminar con un Michialien) y Fiel a las carreras (tiempo corriendo). Los números son una primera estimación del asistente y se retocan en las tablas de `Logros.java`.
- **Tienda** (`Tienda.java`, la misma que la de emi-survivors pero pagada con **estrellas**): tickets de Emipokemon (gacha 60, cosméticos 60, cartas 60, tipos 80, tesoros 80, garra 80, especial 160) y objetos (Ultra Ball 40, Caramelo Raro 80, Revivir Máximo 80, Cápsula Habilidad 300, Master Ball 800); **máximo 10 de cada artículo al día**; lo comprado llega al correo (`/correo`, por reflexión a `MailboxService.sendGift` de Emipokemon; solo se cobra si el correo lo acepta; `-Demi.carreras.correo_prueba=true` lo simula para pruebas). **Artículo exclusivo: el peluche «Michialien en carrito de juguete»** (`emi_plush:gatitoalien_carrito`, emi-plush 1.17.0), 1000 estrellas, una vez por jugador.
- **Ms. President** (emi-survivors 0.24.0): al final de su charla da a elegir «Emi Survivors», «Carreras» o «Ahora no»; «Carreras» hace `/carrera entrar` por el jugador (si el mod de carreras no está en el servidor, avisa).
- **Pausas de Emipokemon** (`Pausas.actualizar`, cada segundo): con **alguien en la sala o en la pista** se paran en memoria la limpieza de objetos y flechas, el tope de entidades y los bosses/eventos; ya no solo durante la carrera. Desde esta versión carreras, survivors y huida **comparten un contador** (propiedad del sistema `emi.pausas.estado`): lo de Emipokemon solo se vuelve a encender cuando ya no queda ningún minijuego que lo necesite (antes el primero que acababa lo encendía aunque otro siguiera). Arnés de prueba con un Emipokemon falso: `test/pausas/`.
- **Comandos nuevos**: jugadores `/carrera entrar · menu · crear [pista] · entrenar [pista] · listo · nolisto · lanzar · cancelar` y `salir` (inteligente); admin `_meta` (solo pruebas: cruza la meta).
- **Probado** (cliente real en el banco ligero, un solo jugador): sala (se ve bien), entrar/salir/volver al sitio, entrenamiento lanzado desde el menú y terminado con `_meta` (estrellas, mejor tiempo, JSON en disco, caja de resultado), crear/cancelar/listo/lanzar, las 5 pestañas del menú, compra en la tienda (Ultra Ball y el peluche exclusivo, «COMPRADO ✔», «Hoy 1/10»), Ms. President eligiendo Carreras, el aviso de no entrar a un minijuego desde otro, y el contador de pausas con el arnés. **No probado**: varios jugadores reales (cola con ≥2, auto-inicio por «todos listos», traspaso de anfitrión), la entrega real por `/correo` con Emipokemon, los iconos de los objetos de Emipokemon en la tienda (en el banco no está Emipokemon), la pausa real con Emipokemon (solo con el arnés), el servidor real y la dificultad/equilibrio de las estrellas y los logros.

## Comandos
Jugadores: `/carrera unirse · salir · elegir <1-6> · estado · ayuda` y, desde la 0.8.0, `entrar · menu · crear · entrenar · listo · nolisto · lanzar · cancelar` (ver «Sala, entrenamiento, logros y tienda»).
Organizador (operador nivel 2 o permiso `emi_carreras.admin` de LuckPerms): `/carrera abrir [pista] · pistas · cerrar · empezar · parar · expulsar <jugador> · instalar · recargar`.
Solo pruebas: `/carrera _poder <baya|pokeball|voltorb|escudo|caramelo|trueno>` (durante la carrera, te da ese poder) y `/carrera _poder golpe_<pokeball|voltorb|trueno>` (te da ese golpe, para probar la Protección).

## Configuración (`config/emi_carreras.json`, se crea sola)
`pista` (la de por defecto), `pistas` (las que se pueden elegir), `musica`, `volumenMusica`, `sonidos`, `vueltas` (0 = las de la pista), `minPlayers` (1), `maxPlayers` (12), `velocidad` (0.23 ≈ 10 bloques/s), `adelanto`, `alturaPokemon`, `presentacion` (true/false: la presentación con cámaras), `presentacionSegundos` (por corredor; 0 = automático), `cuentaAtras`, `segundosTrasPrimero`, `respawnSegundos`,
`queueReminderSeconds`, `poderes`, `cajaRespawnSegundos`, `estrellas`, `estrellasMejora`, `estrellaRespawnSegundos`, `pausarEventos`, `prestados` (especies prestadas) y **`recompensas`**: lista de comandos por puesto (1.º, 2.º, 3.º…) con `{jugador}`, vacíos por defecto
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

## Probado en la 0.7.0 (michiestrellitas, poderes nuevos, pausa de eventos, estatuas)
Servidor de pruebas y cliente real (un solo jugador): las estrellas salen en las 3 pistas (24, 26 y 25), se cogen y reaparecen y el contador de la interfaz sube; con 11 estrellas el poder pasa a «★ Baya Aranja — ¡Mejorado!» y quedan 1; una caída con 7 estrellas deja 4;
los 6 poderes nuevos y el Voltorb/Trueno se usaron con clic derecho sin errores en el registro; `golpe_ovni` se aplica. Con la carrera en marcha `/emipokemon admin items cleanup status` dice DESACTIVADA y al parar vuelve a ACTIVADA; una barra de jefe de prueba (`/bossbar`) no se ve durante la carrera y vuelve después. Las estatuas grandes salen macizas (antes con huecos: el muestreo de la superficie era más ancho que un bloque con escalas grandes).
**No probado**: con varios jugadores (el efecto real del Bostezo, OVNI, Tinta y Grimer sobre un rival: solo el golpe de prueba sobre uno mismo), la pausa con un boss real de Emipokemon (solo se comprobó la limpieza de objetos y la barra de jefe), y el sonido.

## Probado en la 0.6.0 (pista Estación Michialien)
Servidor de pruebas y cliente real (un solo jugador): la plantilla se coloca (5,4 s), la presentación, el aviso y la cuenta atrás salen bien, el mapa de la interfaz dibuja la luna con los turbos, los saltos y la meta; los **dos saltos** (borde azul) llevan al jugador a su aterrizaje; la **mermelada** baja la velocidad de 0,23 a 0,138 dentro de la zona y la devuelve al salir; el **atajo** con turbo (usando un Caramelo) aterriza en el sitio previsto y **sin turbo se cae** y vuelve a la parrilla;
pasando por los 8 puntos de control en orden (teletransportado) se completan las 2 vueltas y la carrera termina con resultados. Vistas desde el suelo y desde el aire de las estatuas, el cartel, los arcos, el planeta y el OVNI (capturas); el portico dice «EMI» legible desde la parrilla.
**No probado**: correr la vuelta entera a pie (el cliente de pruebas va con pocos fps), con varios jugadores, el servidor real, la música (no hay tarjeta de audio: solo se comprobó que el ogg dura 60,0 s), ni coger las cajas de poderes en esta pista.
Nota sobre el generador: `emi.py` no es byte a byte repetible (el orden de la paleta del .nbt cambia entre ejecuciones; el contenido es el mismo), así que la plantilla del repositorio es la que va dentro del jar probado.
Nota: la primera vez que se coloca una pista (o tras cambiar su plantilla) el servidor tarda unos segundos y puede ir con tirones; mejor ponerla antes con `/carrera instalar` cuando no haya nadie esperando.

## Probado en la 0.5.2 (escalas de interfaz y ocultar la interfaz de vainilla)
Servidor de pruebas y cliente real (un solo jugador, carrera «Circuito Pokéball» con Protección en la casilla). Con la carrera en marcha no se ven barra de objetos, corazones, hambre, nivel/experiencia, lista de Pokémon, minimapa ni contador de CobbleDollars, y la cámara está en tercera persona; al parar la carrera **todo vuelve** (capturas antes/durante/después).
Tamaños y escalas probados (la disposición queda igual de proporcionada en todos): 1280×720 con escalas 1, 2 y 4; 1024×768 (4:3) con escalas 1, 2 y 3 (la 4 no existe en esa ventana, Minecraft la baja a 3); 1280×540 (muy ancha) con escalas 1, 2 y 3; 640×360 (ventana pequeña).
Para probar escalas sin reiniciar: arrancar el cliente con `-Demi.carreras.debug=true` y escribir un número en `<carpeta del juego>/emi_escala.txt` (solo pruebas).
**No probado**: con varios jugadores a la vez, el servidor real, el sonido (no hay tarjeta de audio en el entorno de pruebas), relaciones de aspecto distintas de las de arriba ni los mods de interfaz que no sean los listados.

## Probado en la 0.5.1 (interfaz estilo Mario Kart)
Servidor de pruebas y cliente real (un solo jugador): casilla del poder, participantes, mapa con cabeza, puesto grande y vuelta/tiempo en su sitio; al usar el poder la casilla pasa a «Sin poder» y sale «Protegido N s» (capturas). **No probado**: con varios jugadores a la vez ni otras escalas de interfaz.

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
