package com.emipokemon.shop;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Añade a la tienda (Poke Mart) las categorias definidas en config/emipokemon/shop/extra/*.json
 * (formato: {"categories":[{"id","title","products":[...]}]}) sin tocar pokemart.json.
 * Las categorias solo se anaden en memoria y si no existe ya una con el mismo id.
 */
public final class MoveShopExtras {
    private static final Logger LOG = LoggerFactory.getLogger("emipokemon");
    private static final Gson GSON = new Gson();
    /** Pestañas del NPC vendedor de movimientos: se muestran solas, y la tienda normal no las incluye. */
    private static final Set<String> MOVE_CATEGORIES = Set.of("tm_moves", "egg_moves", "star_moves", "tutor_moves");

    private static final class Extra {
        List<ShopCatalog.Category> categories;
    }

    private MoveShopExtras() {
    }

    public static void merge(ShopCatalog.Config config, Path shopDir) {
        if (config == null || shopDir == null) {
            return;
        }
        Path dir = shopDir.resolve("extra");
        if (!Files.isDirectory(dir)) {
            return;
        }
        List<Path> files;
        try (var stream = Files.list(dir)) {
            files = stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        } catch (Exception error) {
            LOG.warn("Poke Mart: no se pudo listar {}", dir, error);
            return;
        }
        if (config.categories == null) {
            config.categories = new ArrayList<>();
        }
        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Extra extra = GSON.fromJson(reader, Extra.class);
                if (extra == null || extra.categories == null) {
                    continue;
                }
                int added = 0;
                int products = 0;
                for (ShopCatalog.Category category : extra.categories) {
                    if (category == null || category.id == null || category.id.isBlank() || category.products == null) {
                        continue;
                    }
                    String id = category.id;
                    boolean exists = config.categories.stream().anyMatch(c -> c != null && c.id != null && c.id.equalsIgnoreCase(id));
                    if (exists) {
                        continue;
                    }
                    config.categories.add(category);
                    added++;
                    products += category.products.size();
                }
                LOG.info("Poke Mart: {} categorias extra ({} productos) cargadas de {}", added, products, file.getFileName());
            } catch (Exception error) {
                LOG.warn("Poke Mart: no se pudo leer {}; se ignora", file, error);
            }
        }
    }

    /**
     * Filtra el catalogo que se envia al cliente: si la tienda se abre en una pestaña de movimientos solo se envian las 4 pestañas
     * de movimientos; en cualquier otro caso se omiten (asi caben en el panel y no se envian ~640 KB en cada apertura).
     */
    public static String filter(String json, String category) {
        if (json == null || !json.contains("\"tm_moves\"")) {
            return json;
        }
        try {
            boolean moves = category != null && MOVE_CATEGORIES.contains(category.trim().toLowerCase(Locale.ROOT));
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray kept = new JsonArray();
            for (JsonElement element : root.getAsJsonArray("categories")) {
                String id = element.getAsJsonObject().get("id").getAsString();
                if (MOVE_CATEGORIES.contains(id) == moves) {
                    kept.add(element);
                }
            }
            if (kept.isEmpty()) {
                return json;
            }
            root.add("categories", kept);
            return GSON.toJson(root);
        } catch (Exception error) {
            LOG.warn("Poke Mart: no se pudo filtrar el catalogo por categoria", error);
            return json;
        }
    }
}
