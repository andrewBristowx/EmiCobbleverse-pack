({
  name: "Sovereign",
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
      this.add("-immune", pokemon, "[from] ability: Sovereign");
      return null;
    }
  },
})
