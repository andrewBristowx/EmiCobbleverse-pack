// Prueba de las habilidades de las formas del GatitoAlien con el simulador Showdown que lleva Cobblemon.
// Uso: SHOWDOWN=<carpeta showdown del servidor> node probar_habilidades.js ../resources/data/emi_plush/abilities
const path = require('path'); const fs = require('fs');
const root = process.env.SHOWDOWN || path.resolve(__dirname, 'showdown');
const {Dex} = require(root + '/sim/dex'); const {Battle} = require(root + '/sim/battle');
const {Cobblemon} = require(root + '/sim/cobblemon/cobblemon');
const dir = process.argv[2] || path.resolve(__dirname, '../resources/data/emi_plush/abilities');
const IDS = ['midas', 'coolshades', 'beachday', 'sovereign', 'affection', 'electrobeat', 'snug', 'blessing', 'infernalpact', 'hexcraft', 'giftrain', 'bloom', 'plunder', 'drama'];
for (const id of IDS) {
  const txt = fs.readFileSync(path.join(dir, id + '.js'), 'utf8');
  if (/\/\//.test(txt.replace(/"[^"\n]*"/g, ''))) { console.error('ERROR: ' + id + '.js tiene comentarios // (rompen la carga del servidor)'); process.exit(2); }
  const ab = eval('({"' + id + '": ' + txt.split(/\r?\n/).join(' ') + '})')[id];
  Cobblemon.registries.ability.register(ab, id);
}
Cobblemon.registries.ability.invalidate();

let fallos = 0;
function check(nombre, ok, extra) { if (!ok) fallos++; console.log((ok ? 'OK    ' : 'FALLO ') + nombre + (extra ? '  ' + extra : '')); }
const mk = (sp, ab, moves, o) => Object.assign({species: sp, ability: ab, moves, movesInfo: moves.map(() => ({pp: 32, maxPp: 32})), level: 50}, o || {});
function battle(t1, t2, doubles, seed) {
  const fmt = doubles ? 'gen9doublescustomgame@@@!team preview' : 'gen9customgame@@@!team preview';
  return new Battle({formatid: fmt, seed: seed || [1, 2, 3, 4], p1: {name: 'A', team: t1}, p2: {name: 'B', team: t2}});
}
const turno = (b, a, c) => b.makeChoices(a, c);
const hpPct = m => m.hp / m.maxhp;

// 1. Midas: +1 Def al entrar; los movimientos de contacto bajan la Velocidad del atacante
{ const b = battle([mk('Eevee', 'midas', ['splash'])], [mk('Snorlax', 'thickfat', ['tackle', 'splash'])]);
  const yo = b.p1.active[0], rival = b.p2.active[0];
  check('Midas: +1 Defensa al entrar', yo.boosts.def === 1, 'def=' + yo.boosts.def);
  turno(b, 'move splash', 'move tackle');
  check('Midas: el contacto baja la Velocidad del atacante', rival.boosts.spe === -1, 'spe rival=' + rival.boosts.spe); }

// 2. Gafas Chulas: critico +1, no baja la precision, ignora la evasion
{ const b = battle([mk('Eevee', 'coolshades', ['tackle', 'splash'])], [mk('Snorlax', 'thickfat', ['sandattack', 'splash'])]);
  const yo = b.p1.active[0];
  const mv = b.dex.getActiveMove('tackle');
  const crit = b.runEvent('ModifyCritRatio', yo, b.p2.active[0], mv, 1);
  check('Gafas Chulas: critico +1 nivel', crit === 2, 'ratio=' + crit);
  turno(b, 'move splash', 'move sandattack');
  check('Gafas Chulas: no se baja la precision', yo.boosts.accuracy === 0, 'precision=' + yo.boosts.accuracy);
  const mv2 = b.dex.getActiveMove('tackle'); b.runEvent('ModifyMove', yo, b.p2.active[0], mv2, mv2);
  check('Gafas Chulas: ignora la evasion', mv2.ignoreEvasion === true); }

// 3. Dia de Playa: Agua +30%, cura 1/16 con sol o lluvia
{ const b = battle([mk('Eevee', 'beachday', ['surf', 'splash'])], [mk('Snorlax', 'thickfat', ['splash'])]);
  const yo = b.p1.active[0];
  const mv = b.dex.getActiveMove('surf');
  const bp = b.runEvent('BasePower', yo, b.p2.active[0], mv, 90, true);
  check('Dia de Playa: Agua +30% (90 -> 117)', bp === 117, 'bp=' + bp);
  const mvN = b.dex.getActiveMove('tackle'); const bpN = b.runEvent('BasePower', yo, b.p2.active[0], mvN, 40, true);
  check('Dia de Playa: otros tipos sin cambio', bpN === 40, 'bp=' + bpN);
  yo.sethp(Math.floor(yo.maxhp / 2)); const h0 = yo.hp;
  turno(b, 'move splash', 'move splash'); const sinClima = yo.hp;
  b.field.setWeather('raindance', yo); turno(b, 'move splash', 'move splash');
  check('Dia de Playa: sin clima no cura', sinClima === h0, h0 + '->' + sinClima);
  check('Dia de Playa: con lluvia cura 1/16', yo.hp - sinClima === Math.floor(yo.baseMaxhp / 16), '+' + (yo.hp - sinClima) + ' (1/16=' + Math.floor(yo.baseMaxhp / 16) + ')'); }

// 4. Soberano: -25% con PS completos, inmune a Mofa/Tormento/Otra Vez
{ const dano = (ab, previo) => {
    const b = battle([mk('Eevee', ab, ['splash'])], [mk('Snorlax', 'thickfat', ['tackle', 'taunt'])], false, [5, 6, 7, 8]);
    const yo = b.p1.active[0]; if (previo) yo.sethp(yo.maxhp - 1); const h = yo.hp;
    turno(b, 'move splash', 'move tackle'); return h - yo.hp; };
  const lleno = dano('sovereign', false), base = dano('runaway', false), herido = dano('sovereign', true), baseHerido = dano('runaway', true);
  check('Soberano: PS completos -25%', Math.abs(lleno / base - 0.75) < 0.06, lleno + ' vs ' + base + ' (' + (lleno / base).toFixed(2) + ')');
  check('Soberano: sin PS completos no reduce', Math.abs(herido / baseHerido - 1) < 0.02, herido + ' vs ' + baseHerido);
  const b = battle([mk('Eevee', 'sovereign', ['splash'])], [mk('Snorlax', 'thickfat', ['taunt', 'torment'])]);
  turno(b, 'move splash', 'move taunt'); turno(b, 'move splash', 'move torment');
  const v = b.p1.active[0].volatiles;
  check('Soberano: inmune a Mofa y Tormento', !v.taunt && !v.torment); }

// 5. Carino: cura 1/16 a si mismo y a los aliados
{ const b = battle([mk('Eevee', 'affection', ['splash']), mk('Pikachu', 'static', ['splash'])], [mk('Snorlax', 'thickfat', ['splash']), mk('Machop', 'guts', ['splash'])], true);
  const yo = b.p1.active[0], aliado = b.p1.active[1];
  yo.sethp(Math.floor(yo.maxhp / 2)); aliado.sethp(Math.floor(aliado.maxhp / 2)); const a0 = yo.hp, b0 = aliado.hp;
  b.makeChoices('move splash, move splash', 'move splash, move splash');
  check('Carino: cura 1/16 a si mismo', yo.hp - a0 === Math.floor(yo.baseMaxhp / 16), '+' + (yo.hp - a0));
  check('Carino: cura 1/16 al aliado', aliado.hp - b0 === Math.floor(aliado.baseMaxhp / 16), '+' + (aliado.hp - b0)); }

// 6. Ritmo Electrico: los movimientos de sonido pasan a Electrico y +20%
{ const b = battle([mk('Eevee', 'electrobeat', ['hypervoice', 'tackle'])], [mk('Gyarados', 'intimidate', ['splash'])]);
  const yo = b.p1.active[0];
  const mv = b.dex.getActiveMove('hypervoice'); b.runEvent('ModifyType', yo, b.p2.active[0], mv, mv);
  check('Ritmo Electrico: Voz Cautivadora/Vozarron pasa a Electrico', mv.type === 'Electric', 'tipo=' + mv.type);
  const bp = b.runEvent('BasePower', yo, b.p2.active[0], mv, 90, true);
  check('Ritmo Electrico: +20% (90 -> 108)', bp === 108, 'bp=' + bp);
  const mv2 = b.dex.getActiveMove('tackle'); b.runEvent('ModifyType', yo, b.p2.active[0], mv2, mv2);
  check('Ritmo Electrico: un movimiento normal no cambia', mv2.type === 'Normal'); }

// 7. Abrigo: Fuego/Hielo a la mitad, inmune a quemadura y congelacion
{ const dano = (ab, mov) => {
    const b = battle([mk('Eevee', ab, ['splash'])], [mk('Snorlax', 'thickfat', [mov])], false, [9, 9, 9, 9]);
    const yo = b.p1.active[0]; const h = yo.hp; turno(b, 'move splash', 'move ' + mov); return h - yo.hp; };
  for (const mov of ['flamethrower', 'icebeam', 'surf']) {
    const a = dano('snug', mov), c = dano('runaway', mov), esperado = mov === 'surf' ? 1 : 0.5;
    check('Abrigo: ' + mov + ' x' + esperado, Math.abs(a / c - esperado) < 0.06, a + ' vs ' + c);
  }
  const b = battle([mk('Eevee', 'snug', ['splash'])], [mk('Snorlax', 'thickfat', ['willowisp', 'icebeam'])]);
  turno(b, 'move splash', 'move willowisp'); check('Abrigo: inmune a quemadura', b.p1.active[0].status === '');
  for (let i = 0; i < 12; i++) { turno(b, 'move splash', 'move icebeam'); if (b.p1.active[0].status) break; }
  check('Abrigo: inmune a congelacion', b.p1.active[0].status !== 'frz'); }

// 8. Bendicion: se cura el estado cada turno, Siniestro y Fantasma a la mitad
{ const b = battle([mk('Eevee', 'blessing', ['splash'])], [mk('Snorlax', 'thickfat', ['toxic', 'splash'])]);
  turno(b, 'move splash', 'move toxic'); check('Bendicion: se cura el estado al final del turno', b.p1.active[0].status === '', 'estado=' + b.p1.active[0].status);
  const dano = (ab, mov) => { const bb = battle([mk('Pikachu', ab, ['splash'])], [mk('Snorlax', 'thickfat', [mov])], false, [3, 3, 3, 3]); const y = bb.p1.active[0]; const h = y.hp; turno(bb, 'move splash', 'move ' + mov); return h - y.hp; };
  for (const mov of ['shadowball', 'darkpulse', 'surf']) {
    const a = dano('blessing', mov), c = dano('runaway', mov), esperado = mov === 'surf' ? 1 : 0.5;
    check('Bendicion: ' + mov + ' x' + esperado, Math.abs(a / c - esperado) < 0.06, a + ' vs ' + c);
  } }

// 9. Pacto Infernal: +30% de dano y 1/10 de los PS maximos por ataque
{ const dano = (ab) => { const b = battle([mk('Eevee', ab, ['tackle'])], [mk('Snorlax', 'thickfat', ['splash'])], false, [4, 4, 4, 4]); const r = b.p2.active[0], yo = b.p1.active[0]; const h = r.hp, hy = yo.hp; turno(b, 'move tackle', 'move splash'); return [h - r.hp, hy - yo.hp, yo.baseMaxhp]; };
  const [d, perdido, max] = dano('infernalpact'), [c] = dano('runaway');
  check('Pacto Infernal: +30% de dano', Math.abs(d / c - 1.3) < 0.07, d + ' vs ' + c);
  check('Pacto Infernal: pierde 1/10 de PS', perdido === Math.floor(max / 10), perdido + ' (1/10=' + Math.floor(max / 10) + ')'); }

// 10. Maleficio: +40% contra objetivos con problema de estado
{ const bp = (estado) => { const b = battle([mk('Eevee', 'hexcraft', ['tackle'])], [mk('Snorlax', 'thickfat', ['splash'])]); const r = b.p2.active[0]; if (estado) r.setStatus('brn'); return b.runEvent('BasePower', b.p1.active[0], r, b.dex.getActiveMove('tackle'), 100, true); };
  check('Maleficio: sin estado sin cambio', bp(false) === 100, 'bp=' + bp(false));
  check('Maleficio: con estado +40% (100 -> 140)', bp(true) === 140, 'bp=' + bp(true)); }

// 11. Lluvia de Regalos: 1/3 de probabilidad por turno de subir una caracteristica
{ let subidas = 0, turnos = 0, mal = 0;
  for (let s = 1; s <= 12; s++) { const b = battle([mk('Snorlax', 'giftrain', ['splash'])], [mk('Snorlax', 'thickfat', ['splash'])], false, [s, s + 1, s + 2, s + 3]); const yo = b.p1.active[0];
    for (let t = 0; t < 14; t++) { const antes = ['atk', 'def', 'spa', 'spd', 'spe'].reduce((a, k) => a + yo.boosts[k], 0); turno(b, 'move splash', 'move splash'); const ahora = ['atk', 'def', 'spa', 'spd', 'spe'].reduce((a, k) => a + yo.boosts[k], 0); turnos++; if (ahora - antes === 1) subidas++; else if (ahora !== antes) mal++; } }
  const f = subidas / turnos; check('Lluvia de Regalos: ~1/3 de los turnos suben +1', f > 0.2 && f < 0.47 && !mal, subidas + '/' + turnos + ' (' + f.toFixed(2) + ')'); }

// 12. Floracion: cura 1/16 (1/8 con sol) e inmune a polvos
{ const b = battle([mk('Eevee', 'bloom', ['splash'])], [mk('Snorlax', 'thickfat', ['spore', 'splash'])]);
  const yo = b.p1.active[0]; yo.sethp(Math.floor(yo.maxhp / 2)); const h0 = yo.hp;
  turno(b, 'move splash', 'move splash'); const h1 = yo.hp;
  check('Floracion: cura 1/16', h1 - h0 === Math.floor(yo.baseMaxhp / 16), '+' + (h1 - h0));
  b.field.setWeather('sunnyday', yo); turno(b, 'move splash', 'move splash');
  check('Floracion: con sol cura 1/8', yo.hp - h1 === Math.floor(yo.baseMaxhp / 8), '+' + (yo.hp - h1));
  turno(b, 'move splash', 'move spore'); check('Floracion: inmune a Espora', yo.status === '', 'estado=' + yo.status); }

// 13. Saqueo: al debilitar a un rival sube el Ataque y cura 1/4
{ const b = battle([mk('Eevee', 'plunder', ['tackle'])], [mk('Magikarp', 'swiftswim', ['splash']), mk('Snorlax', 'thickfat', ['splash'])]);
  const yo = b.p1.active[0], rival = b.p2.active[0]; rival.sethp(1); yo.sethp(Math.floor(yo.maxhp / 2)); const h0 = yo.hp;
  turno(b, 'move tackle', 'move splash');
  check('Saqueo: +1 Ataque al derrotar', yo.boosts.atk === 1, 'atk=' + yo.boosts.atk);
  check('Saqueo: cura 1/4 de PS', yo.hp - h0 === Math.floor(yo.baseMaxhp / 4), '+' + (yo.hp - h0)); }

// 14. Drama (la oculta de todas): sigue funcionando
{ const b = battle([mk('Eevee', 'drama', ['yawn'])], [mk('Snorlax', 'thickfat', ['splash'])]); turno(b, 'move yawn', 'move splash');
  check('Drama: Bostezo hace efecto', !!b.p2.active[0].volatiles.yawn || b.p2.active[0].status === 'slp'); }

const errs = [];
console.log(fallos ? '\n' + fallos + ' FALLOS' : '\nTODO OK');
process.exit(fallos ? 1 : 0);
