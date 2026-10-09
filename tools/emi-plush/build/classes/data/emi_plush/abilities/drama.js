({
  name: "Drama",
  num: -9001,
  rating: 3,
  flags: {},
  onModifyMove(move, pokemon) {
    if (move.id === "yawn") {
      move.target = "allAdjacentFoes";
      this.add("-ability", pokemon, "Drama");
    }
  },
  onAnyAfterSetStatus(status, target, source, effect) {
    if (!effect || effect.id !== "yawn" || !status || status.id !== "slp") return;
    if (source !== this.effectState.target) return;
    if (target.statusState && target.statusState.time) {
      target.statusState.time *= 2;
      target.statusState.startTime = (target.statusState.startTime || 0) * 2;
    }
  },
})
