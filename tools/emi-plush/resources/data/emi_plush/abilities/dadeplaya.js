({
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
})
