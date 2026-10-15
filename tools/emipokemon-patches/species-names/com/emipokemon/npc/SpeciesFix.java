package com.emipokemon.npc;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Red de seguridad para los equipos de NPC (pack 1.0.141): {@code NpcBattleService.validateTeam} rechazaba con «Pokemon no valido en el equipo: mr_mime level=N»
 * cualquier especie escrita con guion bajo, guion, punto o apostrofe, porque en Cobblemon los ids no los llevan ({@code mrmime}, {@code greattusk}, {@code mimejr}...).
 * {@code PatchSpeciesNames} llama a {@link #fix(String)} justo despues de limpiar cada linea del equipo. Solo cambia la especie si la escrita NO existe y la
 * misma sin esos signos SI existe (asi no toca especies propias como {@code snorlax_emi}); si algo falla, devuelve la linea tal cual.
 * Se usa reflexion para no depender de Cobblemon al compilar.
 */
public final class SpeciesFix {
    private static Method getByName;
    private static boolean intentado;

    private SpeciesFix() {}

    private static boolean existe(String nombre) throws Exception {
        if (!intentado) {
            intentado = true;
            try {
                getByName = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies").getMethod("getByName", String.class);
            } catch (Throwable e) {
                getByName = null;
            }
        }
        return getByName != null && getByName.invoke(null, nombre) != null;
    }

    public static String fix(String spec) {
        try {
            if (spec == null || spec.isBlank()) return spec;
            int corte = 0;
            while (corte < spec.length() && !Character.isWhitespace(spec.charAt(corte))) corte++;
            String cabeza = spec.substring(0, corte);
            if (cabeza.indexOf('=') >= 0 || existe(cabeza)) return spec;
            String limpia = cabeza.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(".", "").replace("'", "");
            if (!limpia.equals(cabeza) && existe(limpia)) return limpia + spec.substring(corte);
            return spec;
        } catch (Throwable e) {
            return spec;
        }
    }
}
