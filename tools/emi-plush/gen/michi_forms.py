"""Formas del GatitoAlien: (accesorio, nombre de la forma, nombre en español, nombre en inglés). Las formas `Gold<Nombre>` son la misma con el pelaje dorado."""
ACC = [
    ('sunglasses', 'Sunglasses', 'con Gafas de Sol', 'with Sunglasses'),
    ('beach_hat', 'BeachHat', 'con Sombrero de Playa', 'with Beach Hat'),
    ('crown', 'Crown', 'con Corona', 'with Crown'),
    ('bow', 'Bow', 'con Lazo', 'with Bow'),
    ('headphones', 'Headphones', 'con Audífonos', 'with Headphones'),
    ('scarf', 'Scarf', 'con Bufanda', 'with Scarf'),
    ('halo', 'Halo', 'con Halo', 'with Halo'),
    ('devil_horns', 'DevilHorns', 'con Cuernos de Diablillo', 'with Devil Horns'),
    ('witch_hat', 'WitchHat', 'con Sombrero de Bruja', 'with Witch Hat'),
    ('santa_hat', 'SantaHat', 'con Gorro Navideño', 'with Santa Hat'),
    ('flower_crown', 'FlowerCrown', 'con Corona de Flores', 'with Flower Crown'),
    ('pirate', 'Pirate', 'Pirata', 'Pirate'),
]
# (nombre de la forma, aspectos, accesorio o None, dorado). Cobblemon se queda con la ULTIMA forma cuyos aspectos estan todos presentes, asi que las combinadas van al final
FORMAS = [('Gold', ['gold'], None, True)] + [(n, [f'{a}-accessory'], a, False) for a, n, *_ in ACC] + \
         [(f'Gold{n}', ['gold', f'{a}-accessory'], a, True) for a, n, *_ in ACC]
