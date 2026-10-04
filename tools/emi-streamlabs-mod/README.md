# Emi Streamlabs (mod propio de servidor, CC0)

Escucha las donaciones de Streamlabs **desde el propio servidor** y da **1 tirada de gacha por dólar** (ticket aleatorio) ejecutando
`function emipokemon:twitch/donacion {jugador:"Nombre",dolares:N}` (datapack `EmiTwitch-DP`). No hace falta RCON, ni abrir puertos, ni dejar
un programa abierto en un PC: solo conexión de **salida** a `sockets.streamlabs.com`.

- Solo servidor (`environment: server`); en el cliente no se carga.
- Config: `config/emi-streamlabs.json` (se crea sola la primera vez). Pon tu *Socket API Token* de Streamlabs en `streamlabsToken` y reinicia.
  **Es un secreto: no lo subas al repositorio ni lo pegues en chats.** El jar del pack no lleva ningún token.
- Estado: `config/emi-streamlabs-estado.json` (donaciones ya procesadas y pendientes); sobrevive a reinicios.

## A quién le toca
1. `mc:NombreMinecraft` en el mensaje de la donación.
2. El mapa `"jugadores"` (`"nombre_de_twitch_en_minusculas": "NombreMinecraft"`).
3. El propio nombre del donador, si es un nombre válido de Minecraft.

Si no se sabe a quién, se anota en el log (`AVISO ...`) y no se entrega nada.

## Reglas
- 1 tirada por dólar entero (se redondea hacia abajo); moneda convertida con `"monedas"` (si no está, aviso en el log y no se entrega).
- `minimoDolares` (1) y `maximoDolares` (1279, tope del datapack por donación).
- Si el jugador no está conectado, queda pendiente y se reintenta cada `reintentoSegundos` hasta `horasMaximasPendiente` horas.
- Una donación con el mismo id no se procesa dos veces (tampoco tras reiniciar).
- Las alertas de prueba de Streamlabs (`isTest`) se ignoran salvo `aceptarPruebasDeStreamlabs: true`.

El log del servidor muestra `[emi_streamlabs] Conectado a Streamlabs. Esperando donaciones...` cuando está activo.

## Compilar
```
javac -d stubc $(find stub -name '*.java')
javac --release 21 -cp stubc:fabric-loader.jar:gson.jar:fabric-lifecycle-events-v1.jar:fabric-api-base.jar -d cls $(find src -name '*.java')
cp fabric.mod.json cls/ && (cd cls && jar --create --file ../emi-streamlabs-1.0.0.jar fabric.mod.json emi)
```
Compilado contra nombres intermediary ya usados por Emipokemon en producción (`MinecraftServer.method_3734/method_3739/method_3760`,
`class_2170.method_44252`, `class_2168.method_9217`, `class_3324.method_14571`, `class_3222.method_7334`).

## Probado
Servidor Fabric 1.21.1 con todos los mods del pack y un Streamlabs simulado (Socket.IO v2): entrega a un jugador conectado (5+3+10 = 18 tiradas
comprobadas en su inventario), `mc:` en el mensaje, mapa de jugadores con conversión EUR→USD, id repetido, alerta de prueba, menos de 1 USD,
moneda desconocida, jugador desconectado (pendiente que se entrega al entrar), y reinicio del servidor sin duplicar. Conexión con un token real
comprobada (solo conectar, sin enviar nada). **No probado: una donación real de Streamlabs ni un cliente real.**
