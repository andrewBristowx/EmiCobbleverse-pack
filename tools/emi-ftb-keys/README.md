# Emi FTB Keys (mod propio, CC0)

Deja **sin tecla** todas las teclas de FTB Quests y FTB Teams **una sola vez** por instalacion (crea `config/emi_ftb_keys_reset_v1.txt`;
bórralo para repetirlo). Motivo: las teclas por defecto ya van vacias en el pack (`config/defaultoptions/keybindings.txt`), pero quien ya
abrio el juego las tiene guardadas en su `options.txt` y chocaban con las de otros mods. Despues de esa vez el jugador puede asignar las que
quiera y no se vuelven a tocar.

- Solo cliente (`environment: client`); en el servidor no se carga.
- Se ejecuta en `ClientLifecycleEvents.CLIENT_STARTED`; todo va en try/catch (si falla, no hace nada y el juego arranca igual).
- Compilado contra nombres intermediary (los mismos miembros que ya usan Default Options y FTB Quests): `class_310.field_1690`,
  `class_315.field_1839`, `class_304.method_1431/method_1422/method_1426`, `class_315.method_1640`, `class_3675.field_16237`.

Compilar: `javac -d stubc stub/net/minecraft/*.java && javac --release 21 -cp stubc:fabric-loader.jar:fabric-lifecycle-events-v1.jar:fabric-api-base.jar -d cls src/emi/ftbkeys/EmiFtbKeys.java`
y empaquetar `fabric.mod.json` + `emi/ftbkeys/EmiFtbKeys.class` en `mods/emi-ftb-keys-1.0.0.jar`.

Probado: logica con clases simuladas (4 de 6 teclas dejadas sin asignar, la segunda ejecucion no toca nada y respeta teclas reasignadas) y arranque
de un servidor con el jar sin errores. **No probado en un cliente real.**
