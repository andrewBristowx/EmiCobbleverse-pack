({
  name: "Toque de Midas",
  num: -9002,
  rating: 3.5,
  flags: {},
  onStart(pokemon) {
    this.boost({def: 1}, pokemon, pokemon);
  },
  onDamagingHit(damage, target, source, move) {
    if (this.checkMoveMakesContact(move, source, target)) {
      this.boost({spe: -1}, source, target, null, true);
    }
  },
})
