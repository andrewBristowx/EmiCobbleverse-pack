({
  name: "Pacto Infernal",
  num: -9010,
  rating: 3,
  flags: {},
  onModifyDamage(damage, source, target, move) {
    if (move && move.category !== "Status") {
      return this.chainModify([5324, 4096]);
    }
  },
  onAfterMoveSecondarySelf(source, target, move) {
    if (source && source !== target && move && move.category !== "Status" && !source.forceSwitchFlag) {
      this.damage(source.baseMaxhp / 10, source, source, this.effect);
    }
  },
})
