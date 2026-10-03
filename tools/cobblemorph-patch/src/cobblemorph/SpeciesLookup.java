package cobblemorph;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import java.util.Locale;

/**
 * Busqueda de especies para CobbleMorph que admite Pokemon personalizados.
 * El mod original usaba species.getName() ("Eevee Gatito", con espacio, que Brigadier parte en dos palabras) y
 * PokemonSpecies.getByName(), que solo busca en el espacio de nombres "cobblemon". Aqui se usa el id del recurso
 * ("eevee_gatito") y se compara sin mayusculas ni signos (eevee_gatito = eeveegatito = "eevee gatito").
 */
public final class SpeciesLookup {
    private SpeciesLookup() {
    }

    private static String norm(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Parte del id sin el espacio de nombres: "emipokemon:eevee_gatito" -> "eevee_gatito". */
    private static String path(Species species) {
        String id = String.valueOf(species.getResourceIdentifier());
        return id.substring(id.indexOf(':') + 1);
    }

    /** Texto que se sugiere en el comando (una sola palabra valida para Brigadier). */
    public static String suggestName(Species species) {
        return path(species).toLowerCase(Locale.ROOT);
    }

    /** Misma firma que PokemonSpecies.getByName, pero encuentra tambien los Pokemon de otros espacios de nombres. */
    public static Species find(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Species direct = PokemonSpecies.getByName(name);
        if (direct != null) {
            return direct;
        }
        String key = norm(name);
        if (key.isEmpty()) {
            return null;
        }
        Species byName = null;
        for (Species species : PokemonSpecies.INSTANCE.getSpecies()) {
            if (norm(path(species)).equals(key)) {
                return species;
            }
            if (byName == null && norm(species.getName()).equals(key)) {
                byName = species;
            }
        }
        return byName;
    }
}
