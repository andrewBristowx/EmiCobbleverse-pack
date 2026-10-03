const {Moves}=require(process.argv[2]+'/data/moves.js');
const tm=require('fs').readdirSync(process.argv[3]).filter(f=>f.startsWith('tm_')).map(f=>f.slice(3,-5));
function score(m){
  let s=0, why=[];
  const acc=(m.accuracy===true?100:m.accuracy)/100;
  if(m.category==='Status'){
    s=38;
    const b=m.boosts||{}; const sb=m.self&&m.self.boosts||{};
    let boost=0; for(const k in {...b}) boost+=Math.abs(b[k]); 
    if(m.target==='self'||m.target==='allySide'||m.target==='adjacentAllyOrSelf'){ for(const k in b) boost+=b[k]>0?b[k]:0; }
    s+=Math.min(30,boost*9);
    if(m.heal) s+=28*(m.heal[0]/m.heal[1]>=0.5?1:0.6);
    if(m.sideCondition||m.slotCondition) s+=20;
    if(m.weather||m.terrain||m.pseudoWeather) s+=14;
    if(m.status) s+=14*acc;
    if(m.volatileStatus) s+=8;
    if(m.id==='protect'||m.flags.protect===undefined&&m.stallingMove) s+=10;
    if(m.priority>0) s+=6;
    if(['swordsdance','nastyplot','calmmind','dragondance','quiverdance','shellsmash','bulkup','recover','roost','toxic','willowisp','spore','stealthrock','spikes','toxicspikes','substitute','trickroom','tailwind','wish','protect','thunderwave','taunt','haze','rest','partingshot','encore','sleeptalk','clearsmog','defog','rapidspin','healbell','aromatherapy','reflect','lightscreen','auroraveil','stickyweb','leechseed','painsplit','trick','knockoff','uturn','voltswitch','flipturn','icywind','bellydrum','coil','curse','irondefense','agility','rockpolish','workup','glare','hypnosis','sing','yawn','destinybond','perishsong','whirlwind','roar','imprison'].includes(m.id)) s+=14;
  } else {
    let bp=m.basePower||0; if(m.multihit){const h=Array.isArray(m.multihit)?(m.multihit[0]+m.multihit[1])/2:m.multihit; bp=bp*h*(m.id==='tripleaxel'?1.4:1);} 
    if(m.damage==='level'||typeof m.damage==='number') bp=60;
    if(m.ohko) bp=80;
    if(!bp) bp=60;
    let ep=bp*acc;
    if(m.priority>0) ep*=1.25; if(m.priority<0) ep*=0.85;
    if(m.flags.recharge) ep*=0.55; if(m.flags.charge) ep*=0.75; if(m.selfdestruct) ep*=0.5;
    if(m.recoil) ep*=0.88; if(m.drain) ep*=1.1; if(m.secondary||m.secondaries) ep*=1.1; if(m.self&&m.self.boosts) ep*=1.05; 
    if(m.selfSwitch) ep*=1.2;
    s=Math.min(100,ep*0.62);
  }
  return Math.max(1,Math.min(100,Math.round(s)));
}
const out={};
for(const id of tm){ const m=Moves[id]; if(!m){out[id]=null;continue;} out[id]={name:m.name,type:m.type,cat:m.category,bp:m.basePower,acc:m.accuracy,pri:m.priority,score:score(m),nonstd:m.isNonstandard||'',isZ:!!m.isZ,isMax:!!m.isMax}; }
console.log(JSON.stringify(out));
