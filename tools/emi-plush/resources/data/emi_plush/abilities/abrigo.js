({
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
})
