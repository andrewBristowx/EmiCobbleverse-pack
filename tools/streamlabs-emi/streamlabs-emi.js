#!/usr/bin/env node
/**
 * streamlabs-emi: escucha las donaciones de Streamlabs y da 1 tirada de gacha (ticket aleatorio) por dolar en el servidor
 * ejecutando por RCON:  function emipokemon:twitch/donacion {jugador:"Nombre",dolares:N}   (datapack EmiTwitch-DP)
 *
 *   node streamlabs-emi.js                        escucha Streamlabs (uso normal)
 *   node streamlabs-emi.js --prueba Nombre 5      simula una donacion de 5 USD de "Nombre" (prueba RCON sin Streamlabs)
 *
 * A quien le toca: 1) "mc:NombreMinecraft" en el mensaje de la donacion; 2) el mapa "jugadores" de config.json (nombre de Twitch -> Minecraft);
 * 3) el propio nombre del donador (si coincide con su nombre de Minecraft).
 * Si no hay RCON en config.json, solo imprime el comando listo para pegar en el juego (modo manual).
 */
'use strict';
const fs = require('fs');
const path = require('path');

const DIR = __dirname;
const CONFIG_PATH = process.env.STREAMLABS_EMI_CONFIG || path.join(DIR, 'config.json');
const DATA_DIR = process.env.STREAMLABS_EMI_DATA || path.join(DIR, 'datos');

function log(...a) { console.log(new Date().toLocaleString('es-ES'), '-', ...a); }

function loadJson(file, fallback) {
  try { return JSON.parse(fs.readFileSync(file, 'utf8')); } catch (e) { return fallback; }
}
function saveJson(file, data) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file + '.tmp', JSON.stringify(data, null, 2));
  fs.renameSync(file + '.tmp', file);
}

function loadConfig() {
  const cfg = loadJson(CONFIG_PATH, null);
  if (!cfg) {
    console.error('No encuentro config.json. Copia config.example.json a config.json y rellénalo.');
    process.exit(1);
  }
  cfg.monedas = Object.assign({ USD: 1 }, cfg.monedas || {});
  cfg.jugadores = Object.fromEntries(Object.entries(cfg.jugadores || {}).map(([k, v]) => [String(k).toLowerCase(), String(v)]));
  cfg.minimoDolares = cfg.minimoDolares || 1;
  cfg.maximoDolares = cfg.maximoDolares || 1279;
  cfg.reintentoSegundos = cfg.reintentoSegundos || 60;
  cfg.horasMaximasPendiente = cfg.horasMaximasPendiente || 72;
  return cfg;
}

const MC_NAME = /^[A-Za-z0-9_]{3,16}$/;

/** A que jugador de Minecraft va la donacion (o null). */
function resolverJugador(cfg, donador, mensaje) {
  const m = /mc\s*:\s*([A-Za-z0-9_]{3,16})/i.exec(mensaje || '');
  if (m) return m[1];
  const mapeado = cfg.jugadores[String(donador || '').toLowerCase()];
  if (mapeado && MC_NAME.test(mapeado)) return mapeado;
  const limpio = String(donador || '').replace(/\s+/g, '');
  return MC_NAME.test(limpio) ? limpio : null;
}

/** Dolares enteros (1 tirada por dolar). null si la moneda no se conoce. */
function aDolares(cfg, cantidad, moneda) {
  const rate = cfg.monedas[String(moneda || 'USD').toUpperCase()];
  if (rate === undefined) return null;
  const n = Math.floor(parseFloat(cantidad) * rate + 1e-9);
  return Number.isFinite(n) ? n : null;
}

function comandoFuncion(jugador, dolares) {
  return `function emipokemon:twitch/donacion {jugador:"${jugador}",dolares:${dolares}}`;
}

let Rcon = null;
async function enviarRcon(cfg, comando) {
  if (!Rcon) Rcon = require('rcon-client').Rcon;
  const rcon = await Rcon.connect({ host: cfg.rcon.host, port: cfg.rcon.port, password: cfg.rcon.password, timeout: 8000 });
  try { return await rcon.send(comando); } finally { try { rcon.end(); } catch (e) { /* ignore */ } }
}

function hayRcon(cfg) { return cfg.rcon && cfg.rcon.password && !/^TU_/.test(cfg.rcon.password); }

/** @returns 'ok' | 'reintentar' | 'descartar' */
async function entregar(cfg, d) {
  const cmd = comandoFuncion(d.jugador, d.dolares);
  if (!hayRcon(cfg)) {
    log(`MODO MANUAL — pega esto en el juego (como OP):  /${cmd}`);
    return 'ok';
  }
  let resp;
  try { resp = await enviarRcon(cfg, cmd); } catch (e) {
    log(`RCON no disponible (${e.message}); se reintentará.`);
    return 'reintentar';
  }
  const m = /returned (-?\d+)/.exec(resp || '');
  if (m && parseInt(m[1], 10) > 0) {
    log(`ENTREGADO: ${d.dolares} tirada(s) a ${d.jugador} (donación de ${d.donador}).`);
    return 'ok';
  }
  log(`No entregado a ${d.jugador} (¿desconectado?): ${String(resp || '').trim() || '(sin respuesta)'} — se reintentará.`);
  return 'reintentar';
}

class Estado {
  constructor() {
    this.procesadasFile = path.join(DATA_DIR, 'procesadas.json');
    this.pendientesFile = path.join(DATA_DIR, 'pendientes.json');
    this.procesadas = new Set(loadJson(this.procesadasFile, []));
    this.pendientes = loadJson(this.pendientesFile, []);
  }
  guardar() {
    saveJson(this.procesadasFile, [...this.procesadas].slice(-5000));
    saveJson(this.pendientesFile, this.pendientes);
  }
}

async function procesarDonacion(cfg, estado, don) {
  const id = String(don.id);
  if (estado.procesadas.has(id)) { log(`Donación ${id} repetida: se ignora.`); return; }
  estado.procesadas.add(id);
  const dolares = aDolares(cfg, don.cantidad, don.moneda);
  if (dolares === null) {
    log(`AVISO: moneda "${don.moneda}" desconocida (donación ${id} de ${don.donador}: ${don.cantidad}). Añádela a "monedas" en config.json. NO se ha entregado nada.`);
    estado.guardar(); return;
  }
  if (dolares < cfg.minimoDolares) { log(`Donación de ${don.donador} (${don.cantidad} ${don.moneda}) < mínimo; sin tiradas.`); estado.guardar(); return; }
  const jugador = resolverJugador(cfg, don.donador, don.mensaje);
  if (!jugador) {
    log(`AVISO: no sé a qué jugador dar la donación ${id} de "${don.donador}" (${dolares} USD). Pide que ponga mc:NombreMinecraft en el mensaje o añádelo a "jugadores".`);
    estado.guardar(); return;
  }
  const item = { id, donador: don.donador, jugador, dolares: Math.min(dolares, cfg.maximoDolares), creada: Date.now() };
  const r = await entregar(cfg, item);
  if (r === 'reintentar') estado.pendientes.push(item);
  estado.guardar();
}

async function reintentar(cfg, estado) {
  if (!estado.pendientes.length) return;
  const limite = Date.now() - cfg.horasMaximasPendiente * 3600 * 1000;
  const restantes = [];
  for (const item of estado.pendientes) {
    if (item.creada < limite) { log(`Pendiente ${item.id} (${item.jugador}, ${item.dolares} USD) descartada por antigua.`); continue; }
    const r = await entregar(cfg, item);
    if (r === 'reintentar') restantes.push(item);
  }
  estado.pendientes = restantes;
  estado.guardar();
}

function normalizarEventoStreamlabs(ev, cfg) {
  const out = [];
  if (!ev || ev.type !== 'donation' || !Array.isArray(ev.message)) return out;
  for (const m of ev.message) {
    if (!m) continue;
    if (m.isTest && !cfg.aceptarPruebasDeStreamlabs) { log('Alerta de PRUEBA de Streamlabs ignorada (aceptarPruebasDeStreamlabs=false).'); continue; }
    const id = m.id || m._id || m.donation_id || ev.event_id;
    if (!id) { log('Donación sin id; se ignora por seguridad.'); continue; }
    out.push({ id, donador: m.name || m.from || '', cantidad: m.amount, moneda: m.currency || 'USD', mensaje: m.message || '' });
  }
  return out;
}

async function main() {
  const cfg = loadConfig();
  const estado = new Estado();
  const args = process.argv.slice(2);

  const i = args.indexOf('--prueba');
  if (i >= 0) {
    const [nombre, cantidad] = [args[i + 1], args[i + 2]];
    if (!nombre || !cantidad) { console.error('Uso: node streamlabs-emi.js --prueba Nombre 5'); process.exit(1); }
    await procesarDonacion(cfg, estado, { id: 'prueba-' + Date.now(), donador: nombre, cantidad, moneda: 'USD', mensaje: '' });
    process.exit(0);
  }

  if (!cfg.streamlabsToken || /^PEGA_/.test(cfg.streamlabsToken)) { console.error('Falta "streamlabsToken" en config.json (Socket API Token de Streamlabs).'); process.exit(1); }
  const io = require('socket.io-client');
  const socket = io(`${cfg.streamlabsUrl}?token=${encodeURIComponent(cfg.streamlabsToken)}`, { transports: ['websocket'], reconnection: true });
  socket.on('connect', () => log('Conectado a Streamlabs. Esperando donaciones…'));
  socket.on('disconnect', (r) => log('Desconectado de Streamlabs:', r, '(reintentando)'));
  socket.on('connect_error', (e) => log('Error de conexión con Streamlabs:', e && e.message));
  let cola = Promise.resolve();
  socket.on('event', (ev) => {
    for (const don of normalizarEventoStreamlabs(ev, cfg)) {
      log(`Donación recibida: ${don.donador} — ${don.cantidad} ${don.moneda} — "${don.mensaje}"`);
      cola = cola.then(() => procesarDonacion(cfg, estado, don)).catch((e) => log('Error procesando donación:', e && e.stack || e));
    }
  });
  setInterval(() => { cola = cola.then(() => reintentar(cfg, estado)).catch((e) => log('Error en reintento:', e && e.message)); }, cfg.reintentoSegundos * 1000);
  log(`Pendientes guardadas: ${estado.pendientes.length}. ${hayRcon(cfg) ? 'RCON activado.' : 'SIN RCON: modo manual (solo imprime el comando).'}`);
}

if (require.main === module) main().catch((e) => { console.error(e); process.exit(1); });
module.exports = { resolverJugador, aDolares, comandoFuncion, normalizarEventoStreamlabs, procesarDonacion };
