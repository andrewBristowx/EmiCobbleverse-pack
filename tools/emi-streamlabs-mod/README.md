# Emi Streamlabs (mod propio de servidor, CC0)

Escucha las donaciones de Streamlabs **desde el propio servidor** y da **1 tirada de gacha por dólar** (ticket aleatorio) ejecutando
`function emipokemon:twitch/donacion {jugador:"Nombre",dolares:N}` (datapack `EmiTwitch-DP`). No hace falta RCON, ni abrir puertos, ni dejar
un programa abierto en un PC: solo conexión de **salida** a `sockets.streamlabs.com`.

- Solo servidor (`environment: server`); en el cliente no se carga.
- Config: `config/emi-streamlabs.json` (se crea sola la primera vez). Pon tu *Socket API Token* de Streamlabs en `streamlabsToken` y reinicia.
  **Es un secreto: no lo subas al repositorio ni lo pegues en chats.** El jar del pack no lleva ningún token.
- Estado: `config/emi-streamlabs-estado.json` (donaciones ya procesadas y pendientes); sobrevive a reinicios.

## A quién le toca
En este orden:
1. `mc:NombreMinecraft` en el mensaje de la donación.
2. El mapa `"jugadores"` del config (`"nombre_de_twitch_en_minusculas": "NombreMinecraft"`), para casos especiales.
3. **Quien vinculó ese nombre de Twitch en Emipokemon**: se lee `config/emipokemon/twitch_profiles.json` (solo lectura, se recarga si cambia;
   cuenta solo si `linked` es true). Así no hay que rellenar el mapa a mano: basta con que el donador ponga su nombre de Twitch como nombre de la donación.
4. El propio nombre del donador, si es un nombre válido de Minecraft.

Si no se sabe a quién, la donación **queda pendiente sin dueño** (log `AVISO ...`): se reintenta resolver el dueño en cada reintento (por si entre tanto vincula su Twitch) y, si pasan 48 h, se reparte (ver «Donaciones sin entregar»).

## Reglas
- 1 tirada por dólar entero (se redondea hacia abajo); moneda convertida con `"monedas"` (si no está, aviso en el log y no se entrega).
- `minimoDolares` (1) y `maximoDolares` (1279, tope del datapack por donación).
- Si el jugador no está conectado, queda pendiente y se reintenta cada `reintentoSegundos`. Pasadas `horasParaRepartir` horas (48 por defecto) se reparte entre todos; si el reparto no es posible, se descarta a las `horasMaximasPendiente` (72).
- Una donación con el mismo id no se procesa dos veces (tampoco tras reiniciar).
- Las alertas de prueba de Streamlabs (`isTest`) se ignoran salvo `aceptarPruebasDeStreamlabs: true`.

El log del servidor muestra `[emi_streamlabs] Conectado a Streamlabs. Esperando donaciones...` cuando está activo.

## Donaciones sin entregar: se reparten entre todos (1.2.0, pack 1.0.139)
Una donación que lleva **más de 2 días** sin poder entregarse (`horasParaRepartir`, 48 por defecto; 0 = nunca repartir) —el dueño no ha entrado o
no se sabe quién es— ya no se pierde ni espera para siempre: sus tickets (1 por dólar) **se reparten entre todos los jugadores que hayan entrado alguna vez**
(los mismos que `/emipokemon correo enviartodos`). Se divide el total a partes iguales y lo que sobra se sortea de uno en uno entre jugadores distintos.
Los conectados lo reciben directo en el inventario y los demás en el correo de Emipokemon (`/correo`); se avisa por el chat y queda en el log (`REPARTIDA entre todos los jugadores…`).
La lógica está en `com.emipokemon.twitch.CommunityRewards.repartirTickets` del **Emipokemon parcheado del pack** (ver `tools/emipokemon-patches/README.md`, `PatchCommunityRewards`):
este mod lo llama por reflexión, así que **necesita ese Emipokemon**; con otro Emipokemon el reparto falla (se anota en el log) y la donación sigue pendiente hasta el descarte de `horasMaximasPendiente`.
En `emi-streamlabs-estado.json` los pendientes guardan `jugador` (puede ser `null` si no se supo el dueño), `creada`, `mensaje` y los dólares; los de la versión 1.1.0 se leen igual (visto en el código: el mensaje que falta se trata como vacío; no probado con un archivo real de la 1.1.0).
Los subs y bits de Twitch **sin entregar** (regalar subs / bits) se reparten igual desde el propio Emipokemon, no desde este mod.

## Compilar
```
javac -d stubc $(find stub -name '*.java')
javac --release 21 -cp stubc:fabric-loader.jar:gson.jar:fabric-lifecycle-events-v1.jar:fabric-api-base.jar -d cls $(find src -name '*.java')
cp fabric.mod.json cls/ && (cd cls && jar --create --file ../emi-streamlabs-1.2.0.jar fabric.mod.json emi)
```
Compilado contra nombres intermediary ya usados por Emipokemon en producción (`MinecraftServer.method_3734/method_3739/method_3760`,
`class_2170.method_44252`, `class_2168.method_9217`, `class_3324.method_14571`, `class_3222.method_7334`).

## Probado
Servidor Fabric 1.21.1 con todos los mods del pack y un Streamlabs simulado (Socket.IO v2): entrega a un jugador conectado (5+3+10 = 18 tiradas
comprobadas en su inventario), `mc:` en el mensaje, mapa de jugadores con conversión EUR→USD, vínculo de Twitch de Emipokemon (con el mapa vacío), id repetido, alerta de prueba, menos de 1 USD,
moneda desconocida, jugador desconectado (pendiente que se entrega al entrar), y reinicio del servidor sin duplicar. Conexión con un token real
comprobada (solo conectar, sin enviar nada). **No probado: una donación real de Streamlabs ni un cliente real.**
Versión 1.2.0 (reparto): en el mismo servidor de pruebas, con donaciones ficticias de fecha adelantada: una de 6 USD con dueño ausente y otra de 3 USD sin dueño pasaron las 48 h y se repartieron
entre los 7 perfiles guardados (cuenta exacta, los desconectados en su correo; con el bot conectado, tickets directos en su inventario y su correo sin cambios); una donación reciente
siguió reintentándose y quedó guardada tras el reparto de las demás. **No probado:** el reparto de una donación real, que el servidor real de HolyHosting tenga el Emipokemon parcheado instalado,
ni jugadores reales.
