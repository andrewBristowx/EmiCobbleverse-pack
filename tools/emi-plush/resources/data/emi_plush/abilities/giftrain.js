({
  name: "Gift Rain",
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
    this.add("-activate", pokemon, "ability: Gift Rain");
    const boost = {};
    boost[stat] = 1;
    this.boost(boost, pokemon, pokemon);
  },
})
