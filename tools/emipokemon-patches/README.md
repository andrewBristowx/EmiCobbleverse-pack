# Parches de bytecode para Emipokemon-0.6.0-alpha.1.jar

El codigo fuente de Emipokemon 0.6.0 no esta en GitHub, asi que los arreglos se aplicaron directamente sobre el jar
(pack 1.0.29, 1.0.30, 1.0.32 y 1.0.35). Estos scripts permiten **reaplicarlos** sobre una copia sin parchear del mismo jar
(hash original `f6298c511ce42848ff6a1eecaeb5536fbccc9d8156cd28f96c42c5cb92c7c2e2`). Si las clases no coinciden, fallan con un error claro
en vez de dejar el jar a medias.

- `PatchCasino`: un jugador solo ve una mesa a la vez (poker/blackjack/ruleta/dados) y `broadcast()` solo envia a quien mira esa mesa.
  Toca `CasinoTableService` y `CasinoNetworking`.
- `PatchTreasure`: Caceria del Tesoro; al ganar a un guardian se llama a `TreasureHuntService.onTrainerVictory` (rompe el sello, abre el pasillo, sube el progreso).
  Toca `TreasureHuntService` y `NpcBattleService`.
- `PatchTwitch`: `/twitch eventos ...` (los avisos del mod lo anuncian asi, pero solo existia `/emi twitch eventos ...`). Toca `TwitchCommands`.
- `PatchShop` (+ `shop/`): tienda de movimientos. `ShopCatalog` carga `config/emipokemon/shop/extra/*.json` en memoria (clase nueva `MoveShopExtras`); `ShopNetworking` envia solo las 4 pestañas de movimientos cuando el NPC abre en `tm_moves|egg_moves|star_moves|tutor_moves` (y las omite en la tienda normal); `NpcKind.safeCategory` acepta esas 4 categorias. El catalogo sale de `tools/emi-shop-moves/`.

## Uso (Java 21, ASM 9.7: asm, asm-tree)

```
mkdir jar && cd jar && unzip ../Emipokemon-0.6.0-alpha.1.jar 'com/emipokemon/*' && cd ..
CP=asm-9.7.jar:asm-tree-9.7.jar
# 1) stub compilado con javac para los metodos nuevos del casino
mkdir stubout && javac --release 21 -d stubout -cp jar stub/com/emipokemon/casino/*.java
# 2) parches
mkdir out out2
javac --release 21 -cp $CP -d . PatchCasino.java PatchTreasure.java
java -cp .:$CP PatchCasino jar stubout out
java -cp .:$CP PatchTreasure jar out2
```
Despues sustituye en el jar las clases generadas en `out/` y `out2/` (mismas rutas `com/emipokemon/...`),
actualiza el sha256 de `index.toml` y el hash del indice en `pack.toml`.

Si algun dia recuperas el codigo fuente real, aplica estos mismos cambios alli y descarta estos scripts.

Nota: `PatchTwitch jar out3` (lee del jar original, escribe en `out3`); sustituye tambien `out3/com/emipokemon/twitch/TwitchCommands.class`.

`PatchShop <jar_extraido> <stub_classes> <MoveShopExtras_classes> <salida>`: compila `shop/com/emipokemon/shop/MoveShopExtras.java` con `javac --release 21 -cp <jar_extraido>:gson:slf4j-api` y `shop/stub/.../ServiceNpcEntity.java` (stub de `safeCategory`).
