# Emi Gimnasios (`emi_gimnasios`)

Generador (Python, sin dependencias salvo Pillow para la vista previa) de los **gimnasios, estadios, pruebas y ligas** de las regiones que tienen líderes NPC en
Emipokemon pero ningún edificio en el mundo: **Teselia, Kalos, Galar, Paldea y Alola** (48 edificios, cada líder con su diseño propio y la temática de su tipo).

Cada edificio es una **plantilla de estructura de Minecraft** (`.nbt`, 1.21, `DataVersion` 3955). El mod `emi-estructuras` (v1.3.0) las coloca en la dimensión
`emi_estructuras:plano` como al resto de estructuras (ver su README) y crea a los líderes dentro.

## Uso
```
python3 build_regiones.py <carpeta tools/emi-estructuras> [<config/emipokemon/extra-locations.json del pack>]    # genera TODO (~2 min)
python3 chequeo_soportes.py <carpeta con .nbt>      # bloques que se romperian al colocarse, fluidos que se derramarian
python3 chequeo_npc.py                              # que cada NPC tenga sitio (2 de aire y suelo firme)
PREV_OUT=salida MC_TEXTURAS=<textures/block del client.jar> python3 prev.py <modulo> [escala] [corte_y]    # vista previa isometrica (4 esquinas; PREV_UNA=1 una sola)
```
`build_regiones.py` escribe `src/main/resources/data/emi_gimnasios/structure/<region>/*.nbt`, `src/main/resources/emi_estructuras/npcs.json` (a quién crear y dónde) y reemplaza las
ubicaciones `T:emi_gimnasios:<region>/<nombre>` de la copia por defecto de `extra-locations.json` del mod y de la del pack. Después hay que copiar ese JSON también a
`tools/emipokemon-patches/extra-locations/com/emipokemon/admin/extra-locations-default.json` y meterlo en el jar de Emipokemon (`PatchExtraLocations`); si no, Emipokemon avisa
«no acepto la ubicacion region:N». La salida es determinista (gzip sin fecha). Los números del menú de cada región son el orden de la lista `REGIONES` de `build_regiones.py`
(en Galar, Paldea y Alola van detrás de las ubicaciones que ya traían de Legendary Monuments y Cobbleverse).

**Pasa siempre `chequeo_soportes.py` y `chequeo_npc.py`**: una linterna, azalea, vela o cactus sin apoyo se convierte en un objeto suelto al colocar la estructura, y el agua o la lava sin muro se derraman.

## Reglas que aprendimos (y están en el código)
- `V.stairs(x, y, z, material, facing)`: `facing` es hacia donde mira el **frente** de la escalera (el lado bajo, por donde se sube). Minecraft guarda el lado contrario (el respaldo alto) y `V.stairs` lo convierte. En las primeras plantillas salían al revés.
- Las hojas se guardan con `persistent=true` (`parse_estado`); sin eso se caen solas al no haber troncos cerca.
- Las plantillas se giran 90° al guardar (`gymlib.girar`) porque la entrada segura de Emipokemon está al **oeste** de la caja: se construyen con la fachada en z=0 (norte) y el NPC mirando a 180°.
- El agua y la lava van siempre encerradas (muros o cristal por los lados, suelo debajo); la lava de los fosos va a nivel del suelo.
- Velas, linternas apoyadas, alfombras y flores necesitan un bloque firme debajo (o `hanging=true` con techo); setas pequeñas solo sobre podzol; cactus sobre arena y sin nada pegado.
- `construir.ubicar_npc` mueve al NPC a la casilla válida más cercana si el punto pedido está dentro de un bloque o en el aire.

## Ficheros
- `nbtlib2.py`, `gymlib.py` (clase `V`: cajas, cilindros, cúpulas, anillos, líneas, escaleras…, `girar`, `guardar`), `piezas.py` (techos a dos aguas, frontones, columnas), `piezas2.py` (elipses y polígonos en pared o suelo, gradas en anillo,
  setas, árboles, conos, estrellas…), `ligalib.py` (salas con techo, vanos, torres, tarimas, rotondas), `alolalib.py` (palmeras, tikis, antorchas, techos de bambú), `construir.py` (registro y volcado), `vox.py` + `prev.py` (vista previa).
- **Teselia** (`g_*.py`): Trío (alas roja, verde y azul), Lenora (museo), Burgh (colmena), Elesa (arena de neón), Clay (mina), Skyla (aeropuerto), Brycen (montaña de hielo), Drayden (pagoda con alas de dragón), Liga (Alto Mando + Alder).
- **Kalos** (`kalos_*.py`): Viola (estudio parisino con mariposa gigante), Grant (acantilado de escalada y fósiles), Korrina (coliseo con ring), Ramos (invernadero con árbol enorme), Clemont (taller con engranaje y bobinas),
  Valerie (castillo rosa entre setas), Olympia (cúpula estrellada con reloj de sol), Wulfric (catedral de hielo con Avalugg), Liga (palacio azul y oro: Malva, Siebold, Wikstrom, Drasna y el teatro de Diantha).
- **Galar** (`galar_*.py`): Milo (granero con molino), Nessa (puerto con hangar a rayas, faro y barco), Kabu (acería con foso de lava), Bea (dojo con torii), Opal (mansión tudor con bola de cristal), Gordie (crómlech con altar de carbón),
  Piers (sala de conciertos punk con calavera), Raihan (fortaleza con dragones), Leon (estadio de Wyndon con Charizard).
- **Paldea** (`paldea_*.py`): Katy (tarta de tres pisos), Brassius (museo con árbol de colores), Iono (torre de neón), Kofu (patio morisco con cangrejo), Larry (oficinas), Ryme (mansión embrujada con escenario de rap),
  Tulip (pabellón-flor de cristal), Grusha (refugio de esquí), Liga (cinco bases Team Star con estrellas + cúpula de Geeta).
- **Alola** (`alola_*.py`, son pruebas al aire libre): Ilima (Caverna Verdeante), Lana (laguna), Kiawe (caldera de volcán con lava), Mallow (casa en el árbol), Sophocles (observatorio), Acerola (supermercado abandonado),
  Mina (barco-restaurante), Hala (ring de piedra con tikis), Olivia (pirámide escalonada), Nanu (calle de noche), Hapu (cañón con castillo de arena), Liga (templo del Monte Lanakila: Hala, Olivia, Acerola, Kahili y Kukui).

## NPC (`npcs.json`)
Por plantilla: lista de `{x, y, z, yaw, cmd, id}` con la posición relativa a la esquina de la plantilla. `emi-estructuras` ejecuta `cmd` (p. ej. `emipokemon desafios kalos voltaje`) desde ese punto y gira al NPC hacia la entrada
(`yaw` 90 = mira al oeste; el comando de Emipokemon lo deja mirando al sur porque la fuente no es una entidad). Si ese líder **ya existe** y está cargado (p. ej. el del pueblo de Emi), Emipokemon contesta «Ya existe el NPC de desafío X»
y el mod lo **mueve** con `/emipokemon npc mover <id>`; si no está cargado se crea uno nuevo y el viejo se borra a mano con `/emipokemon npc eliminar <id>`. Ids: `challenge_<region>_<lider>` (p. ej. `challenge_alola_captain_ilima`).

## Probado
Ver el README de `emi-estructuras`. **No probado**: combates contra los líderes dentro de los edificios (equipos y diálogos son los de Emipokemon), jugadores normales (solo se usó un admin) y las pestañas nuevas del Diario en pantalla.
