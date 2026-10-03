# Parche de CobbleMorph 1.0.5 (licencia CC0)

Problema: `/transform <jugador> <pokemon>` sugeria `species.getName()` en minusculas ("eevee gatito", con espacio, que Brigadier parte en
dos palabras) y buscaba con `PokemonSpecies.getByName()`, que solo mira el espacio de nombres `cobblemon`, asi que los Pokemon
personalizados (`emipokemon:eevee_gatito`, ...) nunca se encontraban. Tambien fallaban "Tapu Koko", "Mr. Mime", etc.

Arreglo (`mods/cobblemorph-1.0.5.jar` ya parcheado, reemplaza al `cobblemorph.pw.toml` de Modrinth):
- Sugerencias: el id del recurso sin espacio de nombres (`eevee_gatito`, `tapukoko`).
- Busqueda (`SpeciesLookup.find`): primero `getByName`; si no, compara sin mayusculas ni signos con el id y con el nombre
  (`eevee_gatito` = `eeveegatito` = `Eevee Gatito`). Se usa en el comando (servidor) y en la capa de render (cliente).

Reaplicar sobre el jar original (Java 21, ASM 9.7: asm + asm-tree):
```
unzip cobblemorph-1.0.5.jar -d x
javac -d stubc stub/net/minecraft/class_2960.java
javac --release 21 -cp stubc:cobblemon.jar -d cls src/cobblemorph/SpeciesLookup.java
javac -cp asm-9.7.jar:asm-tree-9.7.jar -d pc Patch.java && java -cp asm-9.7.jar:asm-tree-9.7.jar:pc Patch x out
```
Despues copia `out/**` y `cls/cobblemorph/SpeciesLookup.class` dentro del jar.

Probado en un servidor con todos los mods: `/transform <jugador> eevee_gatito|eeveegatito|tapukoko|zorua_emi|unown_67` funcionan y un nombre
inexistente da "Unknown Pokemon". Antes del parche `eevee_gatito` daba "Unknown Pokemon". No se ha probado el render en un cliente.
