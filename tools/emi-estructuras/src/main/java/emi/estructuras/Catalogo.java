package emi.estructuras;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Lista ordenada de todo lo que hay que colocar: primero las 69 ubicaciones de Emipokemon, luego el resto de Cobbleverse y Legendary Monuments. */
public final class Catalogo {
    /** region:orden -> id (las 69 ubicaciones que conoce /emipokemon visitar). Las que llevan "T:" son plantillas .nbt sueltas. */
    private static final String[][] LAS_69 = {
        {"kanto:1", "Gimnasio Brock", "cobbleverse:brock"}, {"kanto:2", "Gimnasio Misty", "cobbleverse:misty"},
        {"kanto:3", "Gimnasio Lt. Surge", "cobbleverse:ltsurge"}, {"kanto:4", "Gimnasio Erika", "cobbleverse:erika"},
        {"kanto:5", "Gimnasio Koga", "cobbleverse:koga"}, {"kanto:6", "Gimnasio Sabrina", "cobbleverse:sabrina"},
        {"kanto:7", "Gimnasio Blaine", "cobbleverse:blaine"}, {"kanto:8", "Gimnasio Giovanni", "cobbleverse:giovanni"},
        {"kanto:9", "Liga Kanto", "cobbleverse:kanto_league"}, {"kanto:10", "Altar Articuno", "cobbleverse:legendary/articuno"},
        {"kanto:11", "Altar Zapdos", "cobbleverse:legendary/zapdos"}, {"kanto:12", "Altar Moltres", "cobbleverse:legendary/moltres"},
        {"kanto:13", "Santuario Mew", "cobbleverse:mythical/mew"},
        {"johto:1", "Gimnasio Falkner/Valerio", "cobbleverse:valerio"}, {"johto:2", "Gimnasio Bugsy/Raffaello", "cobbleverse:raffaello"},
        {"johto:3", "Gimnasio Whitney/Chiara", "cobbleverse:chiara"}, {"johto:4", "Gimnasio Morty/Angelo", "cobbleverse:angelo"},
        {"johto:5", "Gimnasio Chuck/Furio", "cobbleverse:furio"}, {"johto:6", "Gimnasio Jasmine", "cobbleverse:jasmine"},
        {"johto:7", "Gimnasio Pryce/Alfredo", "cobbleverse:alfredo"}, {"johto:8", "Gimnasio Clair/Sandra", "cobbleverse:sandra"},
        {"johto:9", "Liga Johto", "cobbleverse:johto_league"}, {"johto:10", "Torre Quemada", "cobbleverse:burned_tower"},
        {"johto:11", "Torre Campana", "cobbleverse:bell_tower"}, {"johto:12", "Islas Remolino", "cobbleverse:whirl_island"},
        {"johto:13", "Santuario Ilex/Celebi", "cobbleverse:celebi_shrine"}, {"johto:14", "Torre Radio Team Rocket", "cobbleverse:rocket_radio_tower"},
        {"hoenn:1", "Gimnasio Roxanne/Petra", "cobbleverse:petra"}, {"hoenn:2", "Gimnasio Brawly/Rudi", "cobbleverse:rudi"},
        {"hoenn:3", "Gimnasio Wattson/Walter", "cobbleverse:walter"}, {"hoenn:4", "Gimnasio Flannery/Fiammetta", "cobbleverse:fiammetta"},
        {"hoenn:5", "Gimnasio Norman", "cobbleverse:norman"}, {"hoenn:6", "Gimnasio Winona/Alice", "cobbleverse:alice"},
        {"hoenn:7", "Gimnasio Tell & Pat", "cobbleverse:tell_pat"}, {"hoenn:8", "Gimnasio Juan/Adriano", "cobbleverse:adriano"},
        {"hoenn:9", "Liga Hoenn", "cobbleverse:hoenn_league"}, {"hoenn:10", "Templo Regirock", "cobbleverse:legendary/regirock"},
        {"hoenn:11", "Templo Regice", "cobbleverse:legendary/regice"}, {"hoenn:12", "Templo Registeel", "cobbleverse:legendary/registeel"},
        {"hoenn:13", "Templo Sumergido Kyogre", "cobbleverse:legendary/kyogre"}, {"hoenn:14", "Isla Volcánica Groudon", "cobbleverse:legendary/groudon"},
        {"hoenn:15", "Pilar Celeste", "cobbleverse:sky_pillar"}, {"hoenn:16", "Jardín Secreto", "cobbleverse:secret_garden"},
        {"hoenn:17", "Wish Cave / Jirachi", "cobbleverse:mythical/jirachi"}, {"hoenn:18", "Santuario Deoxys", "cobbleverse:mythical/deoxys"},
        {"sinnoh:1", "Gimnasio Roark/Pedro", "cobbleverse:pedro"}, {"sinnoh:2", "Gimnasio Gardenia", "cobbleverse:gardenia"},
        {"sinnoh:3", "Gimnasio Maylene/Marzia", "cobbleverse:marzia"}, {"sinnoh:4", "Gimnasio Crasher Wake/Omar", "cobbleverse:omar"},
        {"sinnoh:5", "Gimnasio Fantina/Fannie", "cobbleverse:fannie"}, {"sinnoh:6", "Gimnasio Byron/Ferruccio", "cobbleverse:ferruccio"},
        {"sinnoh:7", "Gimnasio Candice/Bianca", "cobbleverse:bianca"}, {"sinnoh:8", "Gimnasio Volkner/Corrado", "cobbleverse:corrado"},
        {"sinnoh:9", "Liga Sinnoh", "cobbleverse:sinnoh_league"}, {"sinnoh:10", "Wind Plant", "cobbleverse:wind_plant"},
        {"sinnoh:11", "Eterna Building", "cobbleverse:eterna_building"}, {"sinnoh:12", "Team Galactic HQ", "cobbleverse:team_galactic_hq"},
        {"sinnoh:13", "Spear Pillar", "cobbleverse:spear_pillar"}, {"sinnoh:14", "Lake Valor", "legendarymonuments:lake_valor"},
        {"sinnoh:15", "Lake Acuity", "legendarymonuments:lake_acuity"}, {"sinnoh:16", "Lake Verity", "legendarymonuments:lake_verity"},
        {"sinnoh:17", "Stark Mountain", "legendarymonuments:stark_mountain"}, {"sinnoh:18", "Snowpoint Temple", "cobbleverse:snowpoint_temple"},
        {"sinnoh:19", "Split-Decision Temple", "cobbleverse:split_decision_temple"}, {"sinnoh:20", "Fullmoon Island", "cobbleverse:fullmoon_island"},
        {"sinnoh:21", "Newmoon Island", "T:lumymon:newmoon_island"}, {"sinnoh:22", "Flower Paradise", "cobbleverse:flower_paradise"},
        {"sinnoh:23", "Turnback Cave", "legendarymonuments:turnback_cave"}, {"sinnoh:24", "Templo original de Sinnoh", "T:lumymon:temple_of_sinnoh"},
    };

    /** Estructuras que se asientan en el fondo del mar: se les prepara una balsa de agua. */
    private static final Set<String> OCEANO = Set.of(
        "cobbleverse:misty", "cobbleverse:adriano", "cobbleverse:whirl_island", "cobbleverse:legendary/kyogre", "cobbleverse:legendary/groudon",
        "cobbleverse:fullmoon_island", "cobbleverse:flower_paradise", "cobbleverse:mythical/manaphy",
        "cobbleverse:crescent_isle", "cobbleverse:sky_pillar", "lumymon:newmoon_island");

    private static final Set<String> NAMESPACES = Set.of("cobbleverse", "legendarymonuments");

    /** Cuantas de las 69 ubicaciones de Emipokemon no estan registradas en este mundo (datapacks sin activar): mientras haya alguna, la generacion no se da por terminada. */
    public static int saltadas = 0;

    /** Las estructuras de Dungeons que son de mar: balsa de agua alrededor. */
    private static final Set<String> OCEANO_DUNGEONS = Set.of("nova_structures:trident_trial_monument", "nova_structures:conduit_ruin");

    public static List<Entrada> construir(MinecraftServer server, Generador.Mundo mundo) {
        if (mundo.esDungeons()) return construirDungeons(server);
        return construir(server);
    }

    /** El mundo de Dungeons y jefes: las entradas de la region "Dungeons" de extra-locations.json, en su orden (esa es la numeracion del menu). */
    private static List<Entrada> construirDungeons(MinecraftServer server) {
        Registry<Structure> reg = server.getRegistryManager().get(RegistryKeys.STRUCTURE);
        List<Entrada> out = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        try {
            com.google.gson.JsonArray lista = leerExtras();
            int orden = 0;
            for (var el : lista) {
                var o = el.getAsJsonObject();
                String region = regionClave(o.get("region").getAsString());
                if (!region.equals("dungeons")) continue;
                orden++;
                String id = o.has("structure") ? o.get("structure").getAsString().trim() : "";
                Identifier ident = Identifier.tryParse(id);
                if (ident == null || id.isEmpty() || !vistos.add(id)) continue;
                if (!reg.containsId(ident)) { EmiEstructuras.LOG.warn("Dungeons: la estructura {} no esta registrada; la salto", id); continue; }
                out.add(new Entrada(ident, false, "dungeons:" + orden, o.get("label").getAsString(), OCEANO_DUNGEONS.contains(id)));
            }
        } catch (Exception e) {
            EmiEstructuras.LOG.warn("No pude leer las ubicaciones de Dungeons", e);
        }
        return out;
    }

    static String regionClave(String region) { return region.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", ""); }

    private static com.google.gson.JsonArray leerExtras() throws java.io.IOException {
        String texto;
        java.nio.file.Path f = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("emipokemon").resolve("extra-locations.json");
        if (java.nio.file.Files.exists(f)) texto = java.nio.file.Files.readString(f);
        else try (var in = Catalogo.class.getResourceAsStream("/emi_estructuras/extra-locations-default.json")) { texto = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); }
        return com.google.gson.JsonParser.parseString(texto).getAsJsonObject().getAsJsonArray("ubicaciones");
    }

    public static List<Entrada> construir(MinecraftServer server) {
        saltadas = 0;
        Registry<Structure> reg = server.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Set<String> vistos = new LinkedHashSet<>();
        List<Entrada> out = new ArrayList<>();
        for (String[] f : LAS_69) {
            boolean plantilla = f[2].startsWith("T:");
            String id = plantilla ? f[2].substring(2) : f[2];
            Identifier ident = Identifier.of(id);
            if (!plantilla && !reg.containsId(ident)) {
                EmiEstructuras.LOG.warn("La estructura {} ({}) no esta registrada en este mundo; la salto", id, f[1]);
                saltadas++;
                continue;
            }
            vistos.add(id);
            out.add(new Entrada(ident, plantilla, f[0], f[1], OCEANO.contains(id)));
        }
        añadirExtras(reg, vistos, out);
        TreeSet<String> resto = new TreeSet<>();
        for (Identifier id : reg.getIds()) if (NAMESPACES.contains(id.getNamespace()) && !vistos.contains(id.toString())) resto.add(id.toString());
        for (String id : resto) out.add(new Entrada(Identifier.of(id), false, null, id, OCEANO.contains(id)));
        return out;
    }

    /**
     * Las ubicaciones extra del menu (config/emipokemon/extra-locations.json, las crea el parche de Emipokemon; si todavia no existe
     * se usan las del jar). Cada una va al final de su region: orden = lo que ya tiene la region + su posicion en el archivo.
     */
    private static void añadirExtras(Registry<Structure> reg, Set<String> vistos, List<Entrada> out) {
        try {
            com.google.gson.JsonArray lista = leerExtras();
            Map<String, Integer> contador = new LinkedHashMap<>();
            for (String[] fila : LAS_69) contador.merge(fila[0].substring(0, fila[0].indexOf(':')), 1, Integer::sum);
            for (var el : lista) {
                var o = el.getAsJsonObject();
                String region = regionClave(o.get("region").getAsString());
                if (region.equals("dungeons")) continue;      // van al otro mundo
                int orden = contador.merge(region, 1, Integer::sum);
                String id = o.has("structure") ? o.get("structure").getAsString().trim() : "";
                if (id.isEmpty() || vistos.contains(id)) continue;
                Identifier ident = Identifier.tryParse(id);
                if (ident == null || !reg.containsId(ident)) { EmiEstructuras.LOG.warn("Ubicacion extra {}: la estructura {} no esta registrada; la salto", o.get("label"), id); continue; }
                vistos.add(id);
                out.add(new Entrada(ident, false, region + ":" + orden, o.get("label").getAsString(), OCEANO.contains(id)));
            }
        } catch (Exception e) {
            EmiEstructuras.LOG.warn("No pude leer las ubicaciones extra", e);
        }
    }

    public static Map<String, Entrada> porId(List<Entrada> l) {
        Map<String, Entrada> m = new LinkedHashMap<>();
        for (Entrada e : l) m.put(e.id.toString(), e);
        return m;
    }
}
