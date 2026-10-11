({
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
})
