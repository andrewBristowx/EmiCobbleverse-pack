({
  name: "Plunder",
  num: -9014,
  rating: 3.5,
  flags: {},
  onSourceAfterFaint(length, target, source, effect) {
    if (effect && effect.effectType === "Move") {
      this.add("-activate", source, "ability: Plunder");
      this.boost({atk: length}, source, source);
      this.heal(source.baseMaxhp / 4, source, source);
    }
  },
})
