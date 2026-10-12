# Emi Gimnasios (en preparación)

Generador de estructuras (gimnasios y ligas) para las regiones que tienen líderes NPC en Emipokemon pero ningún edificio en el mundo
(Teselia, Kalos, Galar, Paldea, Alola). **Todavía no está integrado en el pack**: son solo las herramientas y un prototipo.

- `nbtlib2.py`: lector/escritor mínimo de NBT (estructuras de Minecraft 1.21, `DataVersion` 3955).
- `gymlib.py`: clase `V` (voxeles) con cajas, escaleras, anillos y pixel art, que guarda el NBT de plantilla.
- `gym_build.py`: plantilla de gimnasio de 27×23×26 (misma escala que los de Cobbleverse: cuerpo 25×22, techo escalonado, frontón con emblema,
  puerta en arco, arena con suelo de dibujo, gradas, podio del líder y estandartes).
- `tipos.py`: temas por tipo (paleta, emblema 9×6, decoración y remate del tejado). Hechos: eléctrico, fuego, hielo, agua.
- `vox.py`: vista isométrica de un NBT con el color medio de las texturas de vanilla (variable `MC_TEXTURAS` = carpeta `assets/minecraft/textures/block`);
  sirve para previsualizar sin abrir el juego (las escaleras y losas se dibujan como cubos).

Uso: `python3 -c "import tipos; v = tipos.plantilla(tipos.RAYO); v.guardar('out/rayo.nbt')"`.
Los gimnasios de Cobbleverse (Brock, Sabrina…) son también 27×17×24: una carcasa gris/blanca con el tejado del color del líder y el interior distinto.
