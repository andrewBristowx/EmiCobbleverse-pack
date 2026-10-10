({
  name: "Hex Craft",
  num: -9011,
  rating: 3.5,
  flags: {},
  onBasePowerPriority: 30,
  onBasePower(basePower, attacker, defender, move) {
    if (defender.status || defender.hasAbility("comatose")) {
      this.debug("Hex Craft boost");
      return this.chainModify([5734, 4096]);
    }
  },
})
