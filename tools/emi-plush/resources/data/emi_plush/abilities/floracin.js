({
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
})
