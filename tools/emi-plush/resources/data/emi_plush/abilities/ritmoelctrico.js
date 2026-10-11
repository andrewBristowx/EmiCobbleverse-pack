({
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
})
