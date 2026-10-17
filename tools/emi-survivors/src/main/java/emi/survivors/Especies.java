package emi.survivors;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Las especies que pueden salir como enemigos: todas las de Cobblemon que caminan (no vuelan ni son de tipo Volador, como en las carreras), con su estatura y
 * el total de estadisticas base (BST), que decide lo fuerte que es el enemigo. Se calcula una vez, al primer uso.
 * Se dejan fuera los legendarios, miticos, ultraentes y paradoja (en el juego original solo salen como jefes en las grietas).
 */
final class Especies {
    private Especies() {}

    /** @param id nombre de la especie en Cobblemon (p. ej. {@code mrmime}); alto = altura natural en bloques con su escala base; bst = total de estadisticas base */
    record E(String id, float alto, int bst, String tipo1, String tipo2) {}

    private static final Set<String> RAROS = Set.of("legendary", "mythical", "ultra_beast", "paradox", "restricted", "sub_legendary");
    private static List<E> todas;

    static synchronized List<E> todas() {
        if (todas != null) return todas;
        List<E> l = new ArrayList<>();
        for (Species sp : PokemonSpecies.getSpecies()) {
            try {
                if (!sp.getImplemented()) continue;
                if (!"cobblemon".equals(sp.getResourceIdentifier().getNamespace())) continue;
                var mv = sp.getBehaviour().getMoving();
                if (!mv.getWalk().getCanWalk() || mv.getFly().getCanFly()) continue;
                boolean volador = false;
                for (var t : sp.getTypes()) if ("flying".equals(t.getName())) volador = true;
                if (volador) continue;
                boolean raro = false;
                for (String lb : sp.getLabels()) if (RAROS.contains(lb)) raro = true;
                if (raro) continue;
                int bst = 0;
                for (int v : sp.getBaseStats().values()) bst += v;
                float h = Math.max(sp.getHitbox().height(), sp.getHitbox().width() * 0.8f) * sp.getBaseScale();
                if (h < 0.05f || bst <= 0) continue;
                l.add(new E(sp.getResourceIdentifier().getPath(), h, bst, tipoDe(sp, true), tipoDe(sp, false)));
            } catch (Throwable t) { /* una especie rara no debe romper la lista */ }
        }
        todas = l;
        EmiSurvivors.LOG.info("Especies que pueden salir de enemigos: {}", l.size());
        return l;
    }

    /** Los aspectos del Michialien (GatitoAlien) que salen como enemigos: normal, dorado y los accesorios de emi-plush. */
    static final String[] FORMAS_MICHI = {"", "gold=true", "accessory=crown", "accessory=bow", "accessory=halo", "accessory=devil_horns", "accessory=witch_hat", "accessory=santa_hat",
            "accessory=headphones", "accessory=sunglasses", "accessory=pirate", "accessory=flower_crown", "accessory=scarf", "accessory=beach_hat", "accessory=crown gold=true", "accessory=halo gold=true"};
    private static E michi;
    private static boolean michiBuscado;

    /** El Michialien (el toque de Emi), o null si emi-plush no esta instalado. */
    static synchronized E michi() {
        if (!michiBuscado) {
            michiBuscado = true;
            try {
                Species sp = PokemonSpecies.getByName("gatitoalien");
                if (sp != null) {
                    int bst = 0; for (int v : sp.getBaseStats().values()) bst += v;
                    michi = new E("gatitoalien", Math.max(sp.getHitbox().height(), sp.getHitbox().width() * 0.8f) * sp.getBaseScale(), bst, tipoDe(sp, true), tipoDe(sp, false));
                }
            } catch (Throwable t) { michi = null; }
        }
        return michi;
    }

    /** Una especie por su identificador (p. ej. {@code emipokemon:snorlax_emi}, o solo {@code snorlax}), o null si no existe en este servidor. */
    static E porId(String id) {
        try {
            Species sp = id.contains(":") ? PokemonSpecies.getByIdentifier(net.minecraft.util.Identifier.of(id)) : PokemonSpecies.getByName(id);
            if (sp == null) return null;
            int bst = 0; for (int v : sp.getBaseStats().values()) bst += v;
            return new E(id, Math.max(sp.getHitbox().height(), sp.getHitbox().width() * 0.8f) * sp.getBaseScale(), bst, tipoDe(sp, true), tipoDe(sp, false));
        } catch (Throwable t) { return null; }
    }

    /** El tipo principal (o el secundario, si {@code principal} es false) de una especie, en minusculas y en ingles como lo escribe Cobblemon («fire»); vacio si no tiene. */
    private static String tipoDe(Species sp, boolean principal) {
        try {
            var t = principal ? sp.getPrimaryType() : sp.getSecondaryType();
            return t == null ? "" : t.getName().toLowerCase(java.util.Locale.ROOT);
        } catch (Throwable e) { return ""; }
    }

    /** Las especies de ese tipo (principal o secundario) con BST entre {@code min} y {@code max}. */
    static List<E> deTipo(String tipo, int min, int max) {
        List<E> out = new ArrayList<>();
        for (E e : todas()) if (e.bst() >= min && e.bst() <= max && (tipo.equals(e.tipo1()) || tipo.equals(e.tipo2()))) out.add(e);
        return out;
    }

    /** Las especies con BST entre {@code min} y {@code max} (ambos incluidos). */
    static List<E> entre(int min, int max) {
        List<E> out = new ArrayList<>();
        for (E e : todas()) if (e.bst() >= min && e.bst() <= max) out.add(e);
        return out;
    }
}
