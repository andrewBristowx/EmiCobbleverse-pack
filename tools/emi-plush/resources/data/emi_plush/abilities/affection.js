({
  name: "Affection",
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
})
