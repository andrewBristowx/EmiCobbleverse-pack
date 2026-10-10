"""Roles de las formas del GatitoAlien: cada una (Gold y las 12 con accesorio) tiene su propio segundo tipo (ademas de Hada), unas estadisticas
orientadas a un papel, una HABILIDAD propia con su tematica y movimientos de su tipo. Las formas doradas con accesorio (GoldX) usan el rol de X.
La habilidad oculta de todas es Drama (el rasgo de la familia). Cada habilidad es un script de Showdown: data/emi_plush/abilities/<id>.js
(Cobblemon une las lineas con espacios: NO se pueden usar comentarios //).
Las estadisticas suman 470 como el GatitoAlien normal."""

import re

def abid(rol):
    """Id de la habilidad = toID(nombre en español) (Showdown deduce el id del nombre, asi que no pueden diferir): sin acentos, ni espacios, ni ñ."""
    return re.sub(r'[^a-z0-9]', '', rol['ab_es'].lower())

# id del rol -> datos. Estadisticas: hp, attack, defence, special_attack, special_defence, speed
ROLES = {
    None: dict(  # Gold
        tipo='steel', rol=('Muro físico', 'Physical wall'),
        stats=(85, 65, 115, 55, 100, 50), ev=dict(defence=2, special_defence=1),
        ab='midas', ab_en='Midas', ab_es='Toque de Midas',
        d_en='Contact moves against it lower the attacker\'s Speed by 1 (sticky gold), and it raises its own Defense by 1 when it enters the battle.',
        d_es='Los movimientos de contacto que lo golpean bajan 1 la Velocidad del atacante (oro pegajoso), y al entrar al combate sube 1 su Defensa.',
        moves=['1:metalclaw', '14:irondefense', '24:bulletpunch', '32:ironhead', '38:flashcannon', '44:gyroball', '50:heavyslam', 'tm:ironhead', 'tm:flashcannon', 'tm:irondefense', 'tm:gyroball', 'tm:stealthrock', 'tm:toxic', 'tm:bulletpunch'],
        dex_en='Dipped in solid gold. Hard to move, harder to hurt, and whatever touches it comes away sticky and slow.',
        dex_es='Bañado en oro macizo. Cuesta moverlo y más herirlo, y lo que lo toca queda pegajoso y lento.'),
    'sunglasses': dict(
        tipo='dark', rol=('Atacante físico veloz', 'Fast physical attacker'),
        stats=(60, 105, 55, 70, 60, 120), ev=dict(attack=1, speed=2),
        ab='coolshades', ab_en='Cool Shades', ab_es='Gafas Chulas',
        d_en='Its critical-hit ratio is raised by 1 stage, its accuracy cannot be lowered, and its moves ignore the target\'s evasion.',
        d_es='Su probabilidad de golpe crítico sube 1 nivel, no se le puede bajar la precisión y sus movimientos ignoran la evasión del objetivo.',
        moves=['1:feintattack', '12:suckerpunch', '22:nightslash', '30:knockoff', '36:swordsdance', '42:throatchop', '48:darkpulse', 'tm:suckerpunch', 'tm:knockoff', 'tm:darkpulse', 'tm:swordsdance', 'tm:taunt', 'tm:nightslash'],
        dex_en='It never takes the shades off, not even to nap. It hits first, hits hard and always knows where you are.',
        dex_es='Nunca se quita las gafas, ni para dormir la siesta. Golpea primero, golpea fuerte y siempre sabe dónde estás.'),
    'beach_hat': dict(
        tipo='water', rol=('Atacante especial de agua', 'Special water attacker'),
        stats=(70, 50, 65, 110, 85, 90), ev=dict(special_attack=2),
        ab='beachday', ab_en='Beach Day', ab_es='Día de Playa',
        d_en='Water-type moves are 30% stronger, and under sun or rain it restores 1/16 of its HP every turn.',
        d_es='Los movimientos de tipo Agua son un 30% más fuertes, y con sol o lluvia recupera 1/16 de sus PS cada turno.',
        moves=['1:watergun', '14:waterpulse', '24:scald', '32:surf', '38:raindance', '44:icebeam', '50:hydropump', 'tm:surf', 'tm:scald', 'tm:hydropump', 'tm:raindance', 'tm:icebeam', 'tm:sunnyday'],
        dex_en='Always on vacation. Give it sun or a little rain and it recovers while it splashes the whole opposing team.',
        dex_es='Siempre está de vacaciones. Con sol o un poco de lluvia se recupera mientras salpica a todo el equipo rival.'),
    'crown': dict(
        tipo='dragon', rol=('Mixto robusto', 'Bulky all-rounder'),
        stats=(90, 80, 80, 80, 80, 60), ev=dict(hp=2, attack=1),
        ab='sovereign', ab_en='Sovereign', ab_es='Soberano',
        d_en='While its HP is full it takes 25% less damage, and nobody can order it around: it is immune to Taunt, Torment and Encore.',
        d_es='Con los PS completos recibe un 25% menos de daño, y nadie le da órdenes: es inmune a Mofa, Tormento y Otra Vez.',
        moves=['1:dragonbreath', '20:dragonclaw', '28:dragondance', '34:dragonpulse', '40:outrage', '48:dracometeor', 'tm:dragonpulse', 'tm:dragonclaw', 'tm:dragondance', 'tm:outrage', 'tm:dracometeor', 'tm:earthquake'],
        dex_en='The self-proclaimed king of the channel. It endures anything with its head held high and listens to no one.',
        dex_es='El rey autoproclamado del canal. Aguanta lo que sea con la cabeza bien alta y no hace caso a nadie.'),
    'bow': dict(
        tipo='normal', rol=('Soporte curandero', 'Healing support'),
        stats=(100, 50, 80, 70, 110, 60), ev=dict(hp=1, special_defence=2),
        ab='affection', ab_en='Affection', ab_es='Cariño',
        d_en='At the end of every turn it restores 1/16 of the HP of itself and of its allies.',
        d_es='Al final de cada turno restaura 1/16 de los PS de sí mismo y de sus aliados.',
        moves=['1:wish', '16:softboiled', '24:healbell', '30:aromatherapy', '36:bodyslam', '42:lifedew', 'tm:wish', 'tm:healbell', 'tm:aromatherapy', 'tm:bodyslam', 'tm:softboiled'],
        dex_en='Its bow is a gift to everyone near it. Whoever stands next to it feels better in a few turns.',
        dex_es='Su lazo es un regalo para todos los que están cerca. Quien se pone a su lado se siente mejor en unos turnos.'),
    'headphones': dict(
        tipo='electric', rol=('Especial de sonido', 'Special sound attacker'),
        stats=(65, 50, 60, 120, 80, 95), ev=dict(special_attack=2, speed=1),
        ab='electrobeat', ab_en='Electro Beat', ab_es='Ritmo Eléctrico',
        d_en='Its sound-based moves become Electric type and are 20% stronger.',
        d_es='Sus movimientos de sonido pasan a ser de tipo Eléctrico y son un 20% más fuertes.',
        moves=['1:thundershock', '14:spark', '22:echoedvoice', '28:thunderbolt', '34:voltswitch', '40:discharge', '46:boomburst', 'tm:thunderbolt', 'tm:voltswitch', 'tm:discharge', 'tm:boomburst', 'tm:screech', 'tm:risingvoltage'],
        dex_en='Its music is so loud the air crackles. Every shout it lets out comes with lightning.',
        dex_es='Su música suena tan fuerte que el aire chispea. Cada grito suyo viene con rayos.'),
    'scarf': dict(
        tipo='ice', rol=('Atacante físico robusto', 'Bulky physical attacker'),
        stats=(95, 105, 90, 45, 65, 70), ev=dict(hp=1, attack=1, defence=1),
        ab='snug', ab_en='Snug', ab_es='Abrigo',
        d_en='Fire and Ice moves deal half damage to it, and it cannot be burned or frozen.',
        d_es='Los movimientos de tipo Fuego y Hielo le hacen la mitad de daño, y no se le puede quemar ni congelar.',
        moves=['1:icywind', '14:iceshard', '24:icepunch', '30:avalanche', '36:iciclecrash', '42:blizzard', '48:snowscape', 'tm:icepunch', 'tm:iciclecrash', 'tm:blizzard', 'tm:icebeam', 'tm:snowscape', 'tm:earthquake'],
        dex_en='Wrapped up in its scarf, cold and fire are the same to it. It tackles blizzards and bonfires with the same yawn.',
        dex_es='Abrigado con su bufanda, para él el frío y el fuego son lo mismo. Se enfrenta a ventiscas y hogueras con el mismo bostezo.'),
    'halo': dict(
        tipo='flying', rol=('Defensa especial purificadora', 'Special wall and cleanser'),
        stats=(80, 45, 75, 105, 105, 60), ev=dict(special_attack=1, special_defence=2),
        ab='blessing', ab_en='Blessing', ab_es='Bendición',
        d_en='At the end of every turn it cures its own status condition, and Dark and Ghost moves deal half damage to it.',
        d_es='Al final de cada turno se cura de cualquier problema de estado, y los movimientos de tipo Siniestro y Fantasma le hacen la mitad de daño.',
        moves=['1:gust', '16:roost', '24:airslash', '30:tailwind', '36:calmmind', '42:defog', '48:hurricane', 'tm:airslash', 'tm:roost', 'tm:tailwind', 'tm:calmmind', 'tm:defog', 'tm:hurricane'],
        dex_en='A little glow floats over its antennas. Curses slide off it and wounds close before the next turn.',
        dex_es='Un halo flota sobre sus antenas. Las maldiciones resbalan y las heridas se cierran antes del siguiente turno.'),
    'devil_horns': dict(
        tipo='fire', rol=('Cañón de cristal mixto', 'Mixed glass cannon'),
        stats=(55, 115, 50, 115, 50, 85), ev=dict(attack=1, special_attack=1, speed=1),
        ab='infernalpact', ab_en='Infernal Pact', ab_es='Pacto Infernal',
        d_en='Its damaging moves are 30% stronger, but every attack costs it 1/10 of its max HP.',
        d_es='Sus movimientos de daño son un 30% más fuertes, pero cada ataque le cuesta 1/10 de sus PS máximos.',
        moves=['1:ember', '14:firefang', '24:willowisp', '30:flamethrower', '36:flareblitz', '42:overheat', '48:fireblast', 'tm:flamethrower', 'tm:flareblitz', 'tm:fireblast', 'tm:overheat', 'tm:willowisp', 'tm:darkpulse'],
        dex_en='It signed something it should not have. It burns with power, but the deal is not cheap.',
        dex_es='Firmó algo que no debía. Arde de poder, pero el trato no es barato.'),
    'witch_hat': dict(
        tipo='ghost', rol=('Especial de maleficios', 'Hex special attacker'),
        stats=(70, 50, 60, 115, 90, 85), ev=dict(special_attack=2, special_defence=1),
        ab='hexcraft', ab_en='Hex Craft', ab_es='Maleficio',
        d_en='Its moves deal 40% more damage to targets that have a status condition.',
        d_es='Sus movimientos hacen un 40% más de daño a los objetivos que tengan un problema de estado.',
        moves=['1:shadowsneak', '14:willowisp', '24:hex', '30:nastyplot', '36:shadowball', '42:trick', '46:painsplit', '52:destinybond', 'tm:shadowball', 'tm:hex', 'tm:willowisp', 'tm:nastyplot', 'tm:trick', 'tm:destinybond'],
        dex_en='Its brews are always one ailment away from a disaster. If you are already sick, it hits much harder.',
        dex_es='Sus pociones siempre están a un mal de provocar un desastre. Si ya estás enfermo, golpea mucho más fuerte.'),
    'santa_hat': dict(
        tipo='psychic', rol=('Especial defensivo de regalos', 'Bulky special gift-giver'),
        stats=(95, 50, 85, 95, 95, 50), ev=dict(hp=1, special_attack=1, special_defence=1),
        ab='giftrain', ab_en='Gift Rain', ab_es='Lluvia de Regalos',
        d_en='At the end of each turn it has a 1 in 3 chance of raising a random stat by 1 stage.',
        d_es='Al final de cada turno tiene 1 posibilidad entre 3 de subir 1 nivel una característica al azar.',
        moves=['1:confusion', '15:present', '22:psybeam', '28:calmmind', '34:reflect', '34:lightscreen', '40:psychic', '46:futuresight', 'tm:psychic', 'tm:calmmind', 'tm:reflect', 'tm:lightscreen', 'tm:futuresight', 'tm:present'],
        dex_en='It sees who has been good and who has been dramatic. Every few turns it leaves a surprise gift of power.',
        dex_es='Sabe quién se ha portado bien y quién se ha puesto dramático. Cada pocos turnos deja un regalo sorpresa de poder.'),
    'flower_crown': dict(
        tipo='grass', rol=('Tanque regenerador', 'Regenerating tank'),
        stats=(100, 50, 100, 70, 100, 50), ev=dict(hp=2, defence=1),
        ab='bloom', ab_en='Bloom', ab_es='Floración',
        d_en='It restores 1/16 of its HP every turn (1/8 under harsh sunlight) and is immune to powder and spore moves.',
        d_es='Recupera 1/16 de sus PS cada turno (1/8 con sol intenso) y es inmune a los movimientos de polvo y esporas.',
        moves=['1:absorb', '14:megadrain', '22:leechseed', '28:synthesis', '34:petalblizzard', '40:gigadrain', '46:energyball', '52:solarbeam', 'tm:gigadrain', 'tm:energyball', 'tm:leechseed', 'tm:synthesis', 'tm:sleeppowder', 'tm:solarbeam'],
        dex_en='Flowers grow back on its crown as fast as you pluck them. It shrugs off pollen, spores and most of your attacks.',
        dex_es='Las flores de su corona vuelven a crecer tan rápido como las arrancas. Ignora el polen, las esporas y casi todos tus ataques.'),
    'pirate': dict(
        tipo='fighting', rol=('Atacante físico saqueador', 'Plundering physical attacker'),
        stats=(80, 120, 80, 45, 55, 90), ev=dict(attack=2, speed=1),
        ab='plunder', ab_en='Plunder', ab_es='Saqueo',
        d_en='When it knocks out a Pokémon it raises its Attack by 1 stage and restores 1/4 of its max HP.',
        d_es='Al derrotar a un Pokémon sube 1 nivel su Ataque y recupera 1/4 de sus PS máximos.',
        moves=['1:aquajet', '14:bulkup', '22:brickbreak', '30:drainpunch', '36:swordsdance', '42:closecombat', '48:crabhammer', 'tm:brickbreak', 'tm:drainpunch', 'tm:closecombat', 'tm:bulkup', 'tm:swordsdance', 'tm:aquajet'],
        dex_en='Each rival it defeats is another chest for its treasure. It comes out of every fight stronger and in a better mood.',
        dex_es='Cada rival que derrota es otro cofre para su tesoro. Sale de cada combate más fuerte y de mejor humor.'),
}

# Scripts de Showdown (JS) de cada habilidad. Sin comentarios // (Cobblemon une las lineas con espacios). num: unico y negativo.
JS = {
'midas': '''({
  name: "Toque de Midas",
  num: -9002,
  rating: 3.5,
  flags: {},
  onStart(pokemon) {
    this.boost({def: 1}, pokemon, pokemon);
  },
  onDamagingHit(damage, target, source, move) {
    if (this.checkMoveMakesContact(move, source, target)) {
      this.boost({spe: -1}, source, target, null, true);
    }
  },
})''',
'coolshades': '''({
  name: "Gafas Chulas",
  num: -9003,
  rating: 3,
  flags: {},
  onModifyCritRatio(critRatio) {
    return critRatio + 1;
  },
  onTryBoost(boost, target, source, effect) {
    if (source && target === source) return;
    if (boost.accuracy && boost.accuracy < 0) {
      delete boost.accuracy;
      if (!effect.secondaries && effect.id !== "octolock") {
        this.add("-fail", target, "unboost", "accuracy", "[from] ability: Gafas Chulas", "[of] " + target);
      }
    }
  },
  onModifyMove(move) {
    move.ignoreEvasion = true;
  },
})''',
'beachday': '''({
  name: "Día de Playa",
  num: -9004,
  rating: 3.5,
  flags: {},
  onBasePowerPriority: 30,
  onBasePower(basePower, attacker, defender, move) {
    if (move.type === "Water") {
      this.debug("Beach Day boost");
      return this.chainModify(1.3);
    }
  },
  onResidualOrder: 5,
  onResidualSubOrder: 4,
  onResidual(pokemon) {
    if (pokemon.hp && this.field.isWeather(["sunnyday", "desolateland", "raindance", "primordialsea"])) {
      this.heal(pokemon.baseMaxhp / 16, pokemon, pokemon);
    }
  },
})''',
'sovereign': '''({
  name: "Soberano",
  num: -9005,
  rating: 4,
  flags: {},
  onSourceModifyDamage(damage, source, target, move) {
    if (target.hp >= target.maxhp) {
      this.debug("Sovereign weaken");
      return this.chainModify(0.75);
    }
  },
  onTryAddVolatile(status, pokemon) {
    if (["taunt", "torment", "encore"].includes(status.id)) {
      this.add("-immune", pokemon, "[from] ability: Soberano");
      return null;
    }
  },
})''',
'affection': '''({
  name: "Cariño",
  num: -9006,
  rating: 3.5,
  flags: {},
  onResidualOrder: 5,
  onResidualSubOrder: 4,
  onResidual(pokemon) {
    for (const ally of pokemon.alliesAndSelf()) {
      if (ally.hp && ally.hp < ally.maxhp) {
        this.heal(ally.baseMaxhp / 16, ally, pokemon);
      }
    }
  },
})''',
'electrobeat': '''({
  name: "Ritmo Eléctrico",
  num: -9007,
  rating: 4,
  flags: {},
  onModifyTypePriority: -1,
  onModifyType(move, pokemon) {
    if (move.flags["sound"] && !pokemon.volatiles["dynamax"]) {
      move.type = "Electric";
      move.typeChangerBoosted = this.effect;
    }
  },
  onBasePowerPriority: 23,
  onBasePower(basePower, pokemon, target, move) {
    if (move.typeChangerBoosted === this.effect) return this.chainModify([4915, 4096]);
  },
})''',
'snug': '''({
  name: "Abrigo",
  num: -9008,
  rating: 3.5,
  flags: {breakable: 1},
  onSourceModifyAtkPriority: 6,
  onSourceModifyAtk(atk, attacker, defender, move) {
    if (move.type === "Ice" || move.type === "Fire") {
      this.debug("Snug weaken");
      return this.chainModify(0.5);
    }
  },
  onSourceModifySpAPriority: 5,
  onSourceModifySpA(atk, attacker, defender, move) {
    if (move.type === "Ice" || move.type === "Fire") {
      this.debug("Snug weaken");
      return this.chainModify(0.5);
    }
  },
  onUpdate(pokemon) {
    if (pokemon.status === "brn" || pokemon.status === "frz") {
      this.add("-activate", pokemon, "ability: Abrigo");
      pokemon.cureStatus();
    }
  },
  onSetStatus(status, target, source, effect) {
    if (status.id !== "brn" && status.id !== "frz") return;
    if (effect && effect.effectType === "Move" && !effect.secondaries) {
      this.add("-immune", target, "[from] ability: Abrigo");
    }
    return false;
  },
})''',
'blessing': '''({
  name: "Bendición",
  num: -9009,
  rating: 3.5,
  flags: {breakable: 1},
  onResidualOrder: 5,
  onResidualSubOrder: 3,
  onResidual(pokemon) {
    if (pokemon.hp && pokemon.status) {
      this.add("-activate", pokemon, "ability: Bendición");
      pokemon.cureStatus();
    }
  },
  onSourceModifyAtkPriority: 6,
  onSourceModifyAtk(atk, attacker, defender, move) {
    if (move.type === "Dark" || move.type === "Ghost") {
      this.debug("Blessing weaken");
      return this.chainModify(0.5);
    }
  },
  onSourceModifySpAPriority: 5,
  onSourceModifySpA(atk, attacker, defender, move) {
    if (move.type === "Dark" || move.type === "Ghost") {
      this.debug("Blessing weaken");
      return this.chainModify(0.5);
    }
  },
})''',
'infernalpact': '''({
  name: "Pacto Infernal",
  num: -9010,
  rating: 3,
  flags: {},
  onModifyDamage(damage, source, target, move) {
    if (move && move.category !== "Status") {
      return this.chainModify([5324, 4096]);
    }
  },
  onAfterMoveSecondarySelf(source, target, move) {
    if (source && source !== target && move && move.category !== "Status" && !source.forceSwitchFlag) {
      this.damage(source.baseMaxhp / 10, source, source, this.effect);
    }
  },
})''',
'hexcraft': '''({
  name: "Maleficio",
  num: -9011,
  rating: 3.5,
  flags: {},
  onBasePowerPriority: 30,
  onBasePower(basePower, attacker, defender, move) {
    if (defender.status || defender.hasAbility("comatose")) {
      this.debug("Hex Craft boost");
      return this.chainModify([5734, 4096]);
    }
  },
})''',
'giftrain': '''({
  name: "Lluvia de Regalos",
  num: -9012,
  rating: 3.5,
  flags: {},
  onResidualOrder: 28,
  onResidualSubOrder: 2,
  onResidual(pokemon) {
    if (!pokemon.hp || !this.randomChance(1, 3)) return;
    const stats = [];
    for (const stat in pokemon.boosts) {
      if (stat !== "accuracy" && stat !== "evasion" && pokemon.boosts[stat] < 6) stats.push(stat);
    }
    if (!stats.length) return;
    const stat = this.sample(stats);
    this.add("-activate", pokemon, "ability: Lluvia de Regalos");
    const boost = {};
    boost[stat] = 1;
    this.boost(boost, pokemon, pokemon);
  },
})''',
'bloom': '''({
  name: "Floración",
  num: -9013,
  rating: 3.5,
  flags: {},
  onResidualOrder: 5,
  onResidualSubOrder: 4,
  onResidual(pokemon) {
    if (!pokemon.hp) return;
    if (this.field.isWeather(["sunnyday", "desolateland"])) {
      this.heal(pokemon.baseMaxhp / 8, pokemon, pokemon);
    } else {
      this.heal(pokemon.baseMaxhp / 16, pokemon, pokemon);
    }
  },
  onTryHitPriority: 1,
  onTryHit(target, source, move) {
    if (move.flags["powder"] && target !== source && this.dex.getImmunity("powder", target)) {
      this.add("-immune", target, "[from] ability: Floración");
      return null;
    }
  },
})''',
'plunder': '''({
  name: "Saqueo",
  num: -9014,
  rating: 3.5,
  flags: {},
  onSourceAfterFaint(length, target, source, effect) {
    if (effect && effect.effectType === "Move") {
      this.add("-activate", source, "ability: Saqueo");
      this.boost({atk: length}, source, source);
      this.heal(source.baseMaxhp / 4, source, source);
    }
  },
})''',
}
