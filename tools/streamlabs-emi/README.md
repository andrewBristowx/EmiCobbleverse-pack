# Donaciones de Streamlabs -> tiradas de gacha

> **Alternativa recomendada:** el mod `tools/emi-streamlabs-mod` hace lo mismo dentro del servidor (sin RCON, sin puertos y sin PC encendido). Este script queda solo como respaldo.


Cada dólar donado por Streamlabs da **1 tirada** (1 ticket de gacha aleatorio, de los 6 tipos) al jugador que corresponda.
Un programa pequeño (Node.js) escucha las donaciones de tu canal y se las pide al servidor por **RCON** con el comando
`function emipokemon:twitch/donacion {jugador:"Nombre",dolares:5}` (del datapack `EmiTwitch-DP`).

## Qué necesitas
1. **Node.js** 18 o más (https://nodejs.org, versión LTS) en el PC donde haces stream.
2. El `EmiTwitch-DP.zip` **nuevo** en el servidor (+ `/reload`).
3. **RCON** activado en el servidor (si tu hosting no lo permite, mira "Modo manual").
4. Tu **Socket API Token** de Streamlabs: streamlabs.com → Ajustes → *API Settings* → *API Tokens* → **"Su Token API Socket"** → Copiar.
   (No es el "token de acceso a la API" de arriba. Trátalo como una contraseña: no lo compartas.)

## Puesta en marcha
1. En `server.properties` del servidor: `enable-rcon=true`, `rcon.port=25575`, `rcon.password=UNA_CONTRASEÑA_LARGA` y reinicia. (RCON no va cifrado: usa una contraseña única.)
2. Copia `config.example.json` a **`config.json`** y rellena `streamlabsToken` y `rcon` (host = IP/dominio del servidor y el puerto RCON).
3. Doble clic en `instalar.bat` (una vez) y luego en `iniciar.bat`. Verás "Conectado a Streamlabs. Esperando donaciones…".
4. Prueba la parte del servidor: con el jugador conectado, `node streamlabs-emi.js --prueba NombreMinecraft 1` debe darle 1 ticket.
5. Prueba todo el camino: en Streamlabs lanza una alerta de prueba y pon `"aceptarPruebasDeStreamlabs": true` un momento en `config.json`
   (déjalo en `false` después, o cada prueba regalaría tickets).

## A quién le toca la donación
1. Si el mensaje trae **`mc:NombreMinecraft`**, a ese jugador.
2. Si no, el mapa `"jugadores"` de `config.json` (`"nombre_de_twitch_en_minusculas": "NombreMinecraft"`).
3. Si no, el **nombre del donador** tal cual (sirve si es igual a su nombre de Minecraft).
Si no se sabe, el programa lo avisa en la ventana y no entrega nada (puedes darlo a mano con el comando de arriba).

## Reglas
- **1 tirada por dólar entero** (5,99 USD = 5). Otras monedas se convierten con la tabla `"monedas"` de `config.json` (edítala; una moneda que no esté no se entrega y se avisa).
- Mínimo 1 USD; máximo 1279 por donación.
- Si el jugador **no está conectado**, se guarda como pendiente y se reintenta cada minuto hasta 72 horas (`datos/pendientes.json`).
- Una donación **no se procesa dos veces** (se guarda su id en `datos/procesadas.json`).
- Streamlabs **no repite** las donaciones que llegan con el programa apagado: déjalo abierto mientras haces stream (o da esas a mano).
- El servidor avisa en el chat: «✦ Nombre donó 5 USD y recibe sus tiradas de gacha. ¡Gracias!».

## Modo manual (sin RCON)
Si dejas `rcon.password` sin rellenar, el programa solo **imprime el comando** de cada donación para que lo pegues en el juego como OP.

## Comprobado
Con un servidor 1.21.1 con todos los mods, RCON y un Streamlabs simulado: 5 + 3 (por `mc:`) + 10 EUR (por el mapa) = 18 tickets exactos; la donación repetida,
la alerta de prueba, los eventos que no son donaciones y las de menos de 1 USD no entregan nada; el jugador desconectado queda pendiente y se reintenta.
**No comprobado:** la conexión real a `sockets.streamlabs.com` con un token de verdad (no tengo tu token); eso lo ves con la alerta de prueba del paso 5.
