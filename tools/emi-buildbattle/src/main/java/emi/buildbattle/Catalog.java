package emi.buildbattle;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Lista de bloques que el NPC puede dar: todos los bloques (y alguna decoracion) menos los de la lista negra. */
public final class Catalog {
    public static final class Category {
        public final net.minecraft.text.Text name;
        public final Item icon;
        public final List<Item> items = new ArrayList<>();
        Category(net.minecraft.text.Text name, Item icon) { this.name = name; this.icon = icon; }
    }

    private static Set<Item> allowed = Set.of();
    private static List<Item> all = List.of();
    private static List<Category> categories = List.of();

    public static boolean isAllowed(Item item) { return item == Items.AIR || allowed.contains(item); }
    public static List<Item> all() { return all; }
    public static List<Category> categories() { return categories; }

    /** Ids de objeto que aparecen como tarea "item" en las misiones de FTB Quests (config/ftbquests). */
    private static Set<String> questItems() {
        Set<String> out = new java.util.HashSet<>();
        java.nio.file.Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("ftbquests").resolve("quests");
        if (!java.nio.file.Files.isDirectory(dir)) return out;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("item:\\s*(?:\\{[^}]*?\\bid:\\s*)?\"([a-z0-9_.-]+:[a-z0-9_./-]+)\"");
        try (var walk = java.nio.file.Files.walk(dir)) {
            for (java.nio.file.Path f : (Iterable<java.nio.file.Path>) walk.filter(x -> x.toString().endsWith(".snbt"))::iterator) {
                var m = p.matcher(java.nio.file.Files.readString(f));
                while (m.find()) out.add(m.group(1));
            }
        } catch (Exception e) {
            EmiBuildBattle.LOG.warn("No pude leer las misiones de FTB Quests", e);
        }
        return out;
    }

    public static void rebuild(MinecraftServer server, Cfg cfg) {
        Set<String> quest = cfg.blockQuestItems ? questItems() : Set.of();
        if (!quest.isEmpty()) EmiBuildBattle.LOG.info("Excluyo {} objetos que son tareas de misiones", quest.size());
        Set<Item> set = new LinkedHashSet<>();
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            if (item == Items.AIR) continue;
            if (!item.isEnabled(server.getOverworld().getEnabledFeatures())) continue;
            boolean extra = cfg.extraItems.contains(id.toString());
            if (!extra && !(item instanceof BlockItem)) continue;
            if (blocked(id, cfg) || quest.contains(id.toString())) continue;
            set.add(item);
        }
        allowed = set;
        all = new ArrayList<>(set);

        // categorias = pestanas del inventario creativo (si se puede) y el resto por mod
        Map<String, Category> cats = new LinkedHashMap<>();
        Set<Item> placed = new LinkedHashSet<>();
        try {
            ItemGroups.updateDisplayContext(server.getOverworld().getEnabledFeatures(), true, server.getRegistryManager());
            for (ItemGroup g : ItemGroups.getGroups()) {
                if (g.getType() != ItemGroup.Type.CATEGORY) continue;
                Category c = null;
                for (ItemStack s : g.getDisplayStacks()) {
                    Item it = s.getItem();
                    if (!set.contains(it) || placed.contains(it)) continue;
                    if (c == null) c = new Category(g.getDisplayName(), it);
                    c.items.add(it);
                    placed.add(it);
                }
                if (c != null) {
                    String key = g.getDisplayName().getString();
                    if (cats.containsKey(key)) key = key + " (" + c.items.size() + ")";
                    cats.put(key, c);
                }
            }
        } catch (Throwable t) {
            EmiBuildBattle.LOG.warn("No se pudieron leer las pestanas del inventario creativo; agrupo por mod", t);
            cats.clear();
            placed.clear();
        }
        // lo que no sale en ninguna pestana, por mod
        Map<String, Category> rest = new TreeMap<>();
        for (Item it : set) {
            if (placed.contains(it)) continue;
            String ns = Registries.ITEM.getId(it).getNamespace();
            rest.computeIfAbsent(ns, k -> new Category(net.minecraft.text.Text.literal("Otros: " + k), it)).items.add(it);
        }
        cats.putAll(rest.entrySet().stream().collect(LinkedHashMap::new, (m, e) -> m.put("zz" + e.getKey(), e.getValue()), Map::putAll));
        categories = new ArrayList<>(cats.values());
        EmiBuildBattle.LOG.info("Catalogo: {} objetos en {} categorias", all.size(), categories.size());
    }

    private static boolean blocked(Identifier id, Cfg cfg) {
        String full = id.toString();
        if (cfg.blockedItems.contains(full)) return true;
        if (cfg.blockedNamespaces.contains(id.getNamespace())) return true;
        String path = id.getPath();
        for (String s : cfg.blockedContains) if (path.contains(s)) return true;
        return false;
    }

    /** Palabras en espanol -> trozos en ingles (el servidor solo tiene los nombres en ingles). */
    private static final Map<String, String[]> ES = new java.util.HashMap<>();
    static {
        String[] pairs = {
            "bloque=block", "bloques=block", "oro=gold", "dorado=gold", "hierro=iron", "diamante=diamond", "esmeralda=emerald",
            "lapislazuli=lapis", "cobre=copper", "netherita=netherite", "piedra=stone", "adoquin=cobblestone", "adoquines=cobblestone",
            "ladrillo=brick", "ladrillos=brick", "arenisca=sandstone", "arena=sand", "grava=gravel", "tierra=dirt", "cesped=grass",
            "hierba=grass", "madera=wood|planks|log", "tablones=planks", "tablon=planks", "tronco=log", "roble=oak", "abedul=birch",
            "abeto=spruce", "jungla=jungle", "cerezo=cherry", "manglar=mangrove", "bambu=bamboo", "carmesi=crimson", "distorsionado=warped",
            "cristal=glass", "vidrio=glass", "lana=wool", "alfombra=carpet", "escalera=stairs", "escaleras=stairs", "losa=slab", "losas=slab",
            "valla=fence", "muro=wall", "pared=wall", "puerta=door", "trampilla=trapdoor", "ventana=glass pane", "panel=pane", "cama=bed",
            "mesa=table", "silla=chair", "lampara=lamp|lantern", "farol=lantern", "antorcha=torch", "vela=candle", "maceta=pot",
            "flor=flower|tulip|rose|poppy|orchid", "flores=flower", "hoja=leaves", "hojas=leaves", "arbol=sapling|log|leaves", "planta=plant|fern",
            "nieve=snow", "hielo=ice", "roca=rock|stone", "obsidiana=obsidian", "terracota=terracotta", "hormigon=concrete", "cuarzo=quartz",
            "prismarina=prismarine", "basalto=basalt", "pizarra=deepslate", "granito=granite", "diorita=diorite", "andesita=andesite",
            "calcita=calcite", "toba=tuff", "barro=mud|clay", "arcilla=clay", "lingote=ingot", "cabeza=head", "cartel=sign", "estanteria=bookshelf",
            "estante=shelf", "libro=book", "barril=barrel", "cofre=chest", "horno=furnace", "yunque=anvil", "campana=bell", "cadena=chain",
            "rojo=red", "roja=red", "azul=blue", "verde=green", "amarillo=yellow", "amarilla=yellow", "naranja=orange", "rosa=pink",
            "morado=purple", "morada=purple", "purpura=purple", "negro=black", "negra=black", "blanco=white", "blanca=white", "gris=gray|grey",
            "marron=brown", "celeste=light blue", "cian=cyan", "lima=lime", "pulido=polished", "pulida=polished", "liso=smooth", "lisa=smooth",
            "cincelado=chiseled", "agrietado=cracked", "musgoso=mossy", "oxidado=oxidized", "cortado=cut", "tallado=carved", "teñido=stained",
            "tenido=stained", "esponja=sponge", "panal=honey", "miel=honey", "calabaza=pumpkin", "sandia=melon", "melon=melon", "heno=hay",
            "paja=hay", "cesto=basket", "bandera=banner", "estandarte=banner", "pintura=painting", "marco=frame", "luz=light|lamp",
            "linterna=lantern", "fuego=fire|campfire", "hoguera=campfire", "chimenea=campfire", "boton=button", "palanca=lever", "raiel=rail",
            "carril=rail", "piston=piston", "cuerda=rope|string"
        };
        for (String p : pairs) {
            int i = p.indexOf('=');
            ES.put(p.substring(0, i), p.substring(i + 1).split("\\|"));
        }
    }

    private static final Set<String> STOP = Set.of("de", "del", "la", "el", "los", "las", "con", "y", "en", "un", "una", "para");

    /** Busca por nombre visible o por id; entiende bastante espanol (oro, ladrillo, escalera...). */
    public static List<Item> search(String query) {
        String q = norm(query);
        List<Item> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        String[] words = q.split("\\s+");
        for (Item it : all) {
            String hay = norm(it.getName().getString()) + " " + norm(Registries.ITEM.getId(it).toString());
            boolean ok = true;
            for (String w : words) {
                if (STOP.contains(w) || hay.contains(w)) continue;
                String[] alts = ES.get(w);
                boolean any = false;
                if (alts != null) for (String a : alts) if (hay.contains(a)) { any = true; break; }
                if (!any) { ok = false; break; }
            }
            if (ok) out.add(it);
        }
        return out;
    }

    private static String norm(String s) {
        String n = java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replace('_', ' ');
    }
}
