# Emi Gimnasios (`emi_gimnasios`)

Generador (Python, sin dependencias) de los **gimnasios y la liga** de las regiones que tienen líderes NPC en Emipokemon pero ningún edificio
en el mundo. Hecho: **Teselia (Unova)**: 8 gimnasios + la Liga (Alto Mando y Campeón). Pendiente (a petición del dueño): Kalos, Galar, Paldea y Alola.

Cada edificio es una **plantilla de estructura de Minecraft** (`.nbt`, 1.21, `DataVersion` 3955) con diseño propio y la temática del líder; los NPC se ponen dentro.
El mod `emi-estructuras` (v1.2.0) las coloca en la dimensión `emi_estructuras:plano` como al resto de estructuras (ver su README) y crea a los líderes.

## Uso
```
python3 build_teselia.py <carpeta tools/emi-estructuras> [<config/emipokemon/extra-locations.json del pack>]
```
Escribe `src/main/resources/data/emi_gimnasios/structure/teselia/*.nbt`, `src/main/resources/emi_estructuras/npcs.json` (a quién crear y dónde) y añade las
ubicaciones `T:emi_gimnasios:teselia/<nombre>` (región **Teselia**, orden: Trío, Lenora, Burgh, Elesa, Clay, Skyla, Brycen, Drayden, Liga = `teselia:1..9`) a la copia por
defecto de `extra-locations.json` del mod y a la del pack. Después hay que copiar ese JSON también a `tools/emipokemon-patches/extra-locations/com/emipokemon/admin/extra-locations-default.json`
y meterlo en el jar de Emipokemon (ver `tools/emipokemon-patches/README.md`, `PatchExtraLocations`); si no, Emipokemon avisa «no acepto la ubicacion teselia:N».
La salida es determinista (gzip sin fecha): si no cambias nada, git no ve diferencias.

`python3 chequeo_soportes.py <carpeta con los .nbt>` revisa que ningún bloque frágil se rompa al colocarse (linternas colgantes sin techo, azaleas sin tierra,
antorchas, alfombras, rieles, puertas, amatistas sin apoyo…). **Pásalo siempre**: una linterna o azalea sin apoyo se convierte en un objeto suelto al colocar la estructura.

## Ficheros
- `nbtlib2.py`: lector/escritor mínimo de NBT. `gymlib.py`: clase `V` (voxeles) con cajas, cilindros, cúpulas, anillos, líneas, pixel art, escaleras…; `girar(v, k)` la gira
  (hay que girar el edificio porque la entrada segura de Emipokemon está al **oeste** de la caja, así que la fachada mira al oeste) y `guardar` escribe el NBT.
- `piezas.py`: piezas comunes (columnas, lámparas colgantes, arcos, pixel art de emblemas…). `construir.py`: `registrar` (gira, convierte las posiciones de los NPC y les pone el `id`
  interno `challenge_<region>_<lider>`), `volcar` (escribe todo y valida que los bloques existan, si encuentra el `client.jar`; variable `MC_CLIENT_JAR`).
- `g_trio.py` (Cilan/Chili/Cress: tres alas roja, verde y azul con toldo a rayas), `g_lenora.py` (museo con columnas y vitrinas), `g_burgh.py` (colmena de panal con antenas),
  `g_elesa.py` (arena de neón: techo de cobre, emblema dorado y azul, suelo con círculos), `g_clay.py` (mina con vías, puntales de madera y minerales), `g_skyla.py` (aeropuerto con torre y pista),
  `g_brycen.py` (montaña de hielo), `g_drayden.py` (pagoda negra y verde con cabeza y alas de dragón), `g_liga.py` (vestíbulo con alfombra roja, cuatro salas del Alto Mando —Shauntal fantasma, Marshal lucha, Grimsley siniestro, Caitlin psíquico— y salón del Campeón Alder con cúpula).
- `vox.py`: vista isométrica de un NBT con el color medio de las texturas de vanilla (`python3 vox.py entrada.nbt salida.png 9`; variable `MC_TEXTURAS` = carpeta `assets/minecraft/textures/block`);
  sirve para ver la forma sin abrir el juego (los colores son aproximados; las escaleras y losas se dibujan como cubos).

## NPC (`npcs.json`)
Por plantilla: lista de `{x, y, z, yaw, cmd, id}` con la posición relativa a la esquina de la plantilla. `emi-estructuras` ejecuta `cmd` (p. ej. `emipokemon desafios teselia rayo`)
desde ese punto y gira al NPC hacia la entrada (`yaw` 90 = mira al oeste; el comando de Emipokemon lo deja mirando al sur porque la fuente no es una entidad).
Si ese líder **ya existe** y está cargado (p. ej. el del pueblo de Emi), Emipokemon contesta «Ya existe el NPC de desafío X» y el mod lo **mueve** con `/emipokemon npc mover <id>`;
si no está cargado se crea uno nuevo y el viejo se borra a mano con `/emipokemon npc eliminar <id>`. Ids: `challenge_teselia_{trio,basica,insecto,rayo,temblor,reactor,frio,leyenda}` y
`challenge_teselia_{elite_shauntal,elite_marshal,elite_grimsley,elite_caitlin,champion_alder}`.

## Escala y referencias
Los gimnasios de Cobbleverse son 27×17×24 (carcasa gris/blanca, techo del color del líder, interior distinto); los de aquí miden unos 40×26..36×37 y la liga 110×48×81, con
la fachada, el exterior y el interior propios de cada líder.

## Probado
Servidor completo del pack + cliente real: los 9 edificios se generan desde cero (sin forzar nada: 30 s después de arrancar), claves `teselia:1..9` aceptadas por Emipokemon,
`/emipokemon visitar teselia 2` te deja ante el gimnasio de Lenora, los 13 NPC aparecen dentro mirando a la entrada y no queda ningún objeto suelto. Revisados por fuera y por dentro con capturas.
**No probado**: combates contra los líderes de Teselia dentro de los edificios (los equipos y diálogos son los de Emipokemon); cliente de un jugador normal (solo se usó el admin).
