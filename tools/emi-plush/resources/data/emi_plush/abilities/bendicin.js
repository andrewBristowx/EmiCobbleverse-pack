({
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
})
