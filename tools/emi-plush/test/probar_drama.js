// Prueba de la habilidad Drama con el simulador Showdown que lleva Cobblemon.
// Uso: SHOWDOWN=<carpeta showdown del servidor> node probar_drama.js ../resources/data/emi_plush/abilities/drama.js
const path = require('path'); const fs = require('fs');
const root = process.env.SHOWDOWN || path.resolve(__dirname, 'showdown');
const {Dex} = require(root + '/sim/dex'); const {Battle} = require(root + '/sim/battle');
// Cobblemon une las lineas del archivo con espacios y lo mete en ({"drama": <texto>}): por eso el .js NO puede llevar comentarios con //
const txt = fs.readFileSync(process.argv[2], 'utf8');
if (/\/\//.test(txt.replace(/"[^"\n]*"/g, ''))) { console.error('ERROR: el archivo tiene comentarios // (rompen la carga del servidor)'); process.exit(2); }
const drama = eval('({"drama": ' + txt.split(/\r?\n/).join(' ') + '})').drama;
const {Cobblemon} = require(root + '/sim/cobblemon/cobblemon');
Cobblemon.registries.ability.register(drama, 'drama'); Cobblemon.registries.ability.invalidate();
const mk = (sp, ab, moves) => ({species: sp, ability: ab, moves, movesInfo: moves.map(() => ({pp: 16, maxPp: 16})), level: 50});
function sim(label, ab, doubles) {
  const fmt = doubles ? 'gen9doublescustomgame@@@!team preview' : 'gen9customgame@@@!team preview';
  const p1 = doubles ? [mk('Eevee', ab, ['yawn','splash']), mk('Pikachu','static',['splash'])] : [mk('Eevee', ab, ['yawn','splash'])];
  const p2 = doubles ? [mk('Snorlax','thickfat',['splash']), mk('Machop','guts',['splash'])] : [mk('Snorlax','thickfat',['splash'])];
  const b = new Battle({formatid: fmt, seed:[1,2,3,4], p1:{name:'A', team:p1}, p2:{name:'B', team:p2}});
  const foes = b.p2.active; const log = [];
  if (doubles) b.makeChoices('move yawn 1, move splash', 'move splash, move splash'); else b.makeChoices('move yawn', 'move splash');
  for (let t = 1; t <= 12; t++) {
    log.push('t' + t + ': ' + foes.map(f => (f.status || '-') + (f.statusState && f.statusState.time ? '(' + f.statusState.time + ')' : '') + (f.volatiles.yawn ? '[yawn]' : '')).join(' | '));
    if (b.ended) break;
    if (doubles) b.makeChoices('move splash, move splash', 'move splash, move splash'); else b.makeChoices('move splash', 'move splash');
  }
  console.log(label + '\n  ' + log.join('\n  '));
  const errs = b.log.filter(l => /error|crash/i.test(l)); if (errs.length) console.log('ERRORES', errs);
}
sim('1v1 sin drama', 'cutecharm', false); sim('1v1 CON drama', 'drama', false);
sim('2v2 sin drama', 'cutecharm', true); sim('2v2 CON drama', 'drama', true);
