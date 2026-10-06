# Parches de bytecode para Emipokemon-0.6.0-alpha.1.jar

El codigo fuente de Emipokemon 0.6.0 no esta en GitHub, asi que los arreglos se aplicaron directamente sobre el jar
(pack 1.0.29, 1.0.30, 1.0.32, 1.0.35 y 1.0.39). Estos scripts permiten **reaplicarlos** sobre una copia sin parchear del mismo jar
(hash original `f6298c511ce42848ff6a1eecaeb5536fbccc9d8156cd28f96c42c5cb92c7c2e2`). Si las clases no coinciden, fallan con un error claro
en vez de dejar el jar a medias.

- `PatchCasino`: un jugador solo ve una mesa a la vez (poker/blackjack/ruleta/dados) y `broadcast()` solo envia a quien mira esa mesa.
  Toca `CasinoTableService` y `CasinoNetworking`.
- `PatchTreasure`: Caceria del Tesoro; al ganar a un guardian se llama a `TreasureHuntService.onTrainerVictory` (rompe el sello, abre el pasillo, sube el progreso).
  Toca `TreasureHuntService` y `NpcBattleService`.
- `PatchTwitch`: `/twitch eventos ...` (los avisos del mod lo anuncian asi, pero solo existia `/emi twitch eventos ...`). Toca `TwitchCommands`.
- `PatchShop` (+ `shop/`): tienda de movimientos. `ShopCatalog` carga `config/emipokemon/shop/extra/*.json` en memoria (clase nueva `MoveShopExtras`); `ShopNetworking` envia solo las 4 pestañas de movimientos cuando el NPC abre en `tm_moves|egg_moves|star_moves|tutor_moves` (y las omite en la tienda normal); `NpcKind.safeCategory` acepta esas 4 categorias. **Se pagan con fichas del casino** (objeto fisico, 1 ficha = 100 Michicoins al canjearla; los precios del catalogo estan en fichas): `ShopNetworking` redirige `purchase` a `MoveShopExtras.purchase` (cobra `ModRegistries.CASINO_CHIP`), el saldo que ve el cliente es su numero de fichas y `ShopScreen`/`ShopProductButton` muestran "fichas" (`MoveShopExtras.currency`) en los productos `tmcraft:`. El catalogo sale de `tools/emi-shop-moves/`.
- `PatchBonus` (+ `bonus/stub/`): nuevo comando `/emi twitch bonus activar` (OP nivel 2) que activa un bono aleatorio de servidor de 20 min, como la Caja Misteriosa del Directo, pero **con su propio anuncio** ("FESTÍN DEL DIRECTO") y devolviendo 1/0 según haya funcionado. Añade `TwitchService.emiActivateBonusQuiet` (el bono sin el anuncio de Caja Misteriosa, que sigue intacto para la caja real). Lo usa el Festín del Directo del datapack `EmiCocina-DP` (`tools/emi-cocina/`), que además controla el límite de 3 y el enfriamiento.
- `PatchSnorlaxEmi` (+ `snorlax/`): el jefe Snorlax pasa a ser **Snorlax Emi** (`emipokemon:snorlax_emi`, la especie custom del mod) con voz propia. `SnorlaxRaidService.spawnSnorlax` crea `emipokemon:snorlax_emi level=... uncatchable=true`; los sonidos de aparicion, cambio de fase, aviso del aplastamiento e impacto del aplastamiento pasan por `SnorlaxVoice` (clase nueva), que reproduce `emipokemon:snorlax_emi_voz` (`assets/emipokemon/sounds/snorlax_emi_voz.ogg`, entrada nueva en `assets/emipokemon/sounds.json`; el audio fuente esta en `snorlax/snorlax_emi_voz.ogg`) con volumen minimo 2 y tono 1.0. Los textos visibles pasan de "Snorlax" a "Snorlax Emi" (jefe, cola, avisos) y el jefe dice «¡Los voy a aplastar a todos!» al aparecer y «¡Te voy a aplastar, <jugador>!» al cargar el aplastamiento. `PatchSnorlaxEmi <jar_extraido> <clases_SnorlaxVoice> <salida>`: compila `snorlax/stub/**` y `snorlax/com/emipokemon/raid/snorlax/SnorlaxVoice.java` (`javac --release 21 -cp stubs`), parchea `SnorlaxRaidService`, `SnorlaxQueueService` y `SnorlaxBossCommands` y copia `SnorlaxVoice.class`. Luego hay que meter las clases generadas, el ogg y el `sounds.json` actualizado en el jar. Falla con error claro si las clases no coinciden.
- `PatchSnorlaxBoss` (+ `snorlax-boss/`, se aplica **despues** de `PatchSnorlaxEmi`): (1) el jefe ya no teletransporta a los participantes (los 2 `ServerPlayerEntity.teleport` de `SnorlaxRaidService` pasan a `SnorlaxExtras.noTeleport`) ni les pone la camara al aparecer y morir (las 2 `startCutscene` pasan a `SnorlaxExtras.skipCutscene`, que ejecuta directamente lo que iba al terminar); (2) `resolveCombat` pasa por `SnorlaxExtras.resolve`, que añade tres ataques para quien lo ataca de lejos: **Salto Aplastante** (salta sobre quien lleva mas de ~4 s a mas de 12 bloques, o sobre el mas lejano; aviso de 1,5 s con marca roja, daño de golpe de area x1,3 y empuje), **Lluvia de Rocas** (fase 2+; marcas naranjas bajo 2-4 jugadores, 2 s de aviso, daño de golpe cuerpo a cuerpo x0,9) y **Bostezo** (fase 2+; ralentiza y debilita a los que estan a menos de 12 bloques; en fase 3 tambien nauseas). Los ataques extra usan comandos de Minecraft (`damage`, `effect`, `particle`, `playsound`, `tp`), no cambian la config y vuelven al combate original si algo falla. Para ver diagnosticos: `-Demi.snorlax.debug=true`. (3) `NpcCommands.createCustom`: al crear el NPC `emi_snorlax_queue` se le pone la skin `assets/emipokemon/bundled_npc_skins/emi_snorlax_queue.png` (`SnorlaxNpcSkin`; el png fuente esta en `snorlax-boss/emi_snorlax_queue.png`). `PatchSnorlaxBoss <jar_extraido> <clases_compiladas> <salida>`: compilar `snorlax-boss/stub/**` y luego `snorlax-boss/src/**` (`javac --release 21 -encoding UTF-8 -cp stubs`); mete en el jar las clases generadas y el png.

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

`PatchShop <jar_extraido> <stub_classes> <MoveShopExtras_classes> <salida>`: compila `shop/com/emipokemon/shop/MoveShopExtras.java` con `javac --release 21 -cp <jar_extraido>:gson:slf4j-api` y los stubs de `shop/stub/` (`ServiceNpcEntity` para `safeCategory`; `net/minecraft/*` y `ModRegistries` solo para que compile `MoveShopExtras`, no se incluyen en el jar).

`PatchBonus <jar_extraido> <stub_classes> <salida>`: compila `bonus/stub/**` con `javac -encoding UTF-8 --release 21 -cp brigadier.jar` y parchea `TwitchCommands` y `TwitchService` (ejecutar despues de `PatchTwitch`, sobre el jar que ya lo incluye).
