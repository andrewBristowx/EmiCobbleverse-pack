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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.emipokemon.registry.ModRegistries;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1935;
import net.minecraft.class_3222;
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

    /** Prefijo de los objetos de movimientos (TMCraft): los productos con este objeto se pagan con fichas del casino. */
    private static final String MOVE_ITEM_PREFIX = "tmcraft:";
    private static final long PURCHASE_COOLDOWN_MILLIS = 350L;
    /** id de producto -> producto de movimientos (precio en fichas), rellenado al cargar el catalogo. */
    private static final Map<String, ShopCatalog.Product> CHIP_PRODUCTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_PURCHASE = new ConcurrentHashMap<>();

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
        CHIP_PRODUCTS.clear();
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
                    for (ShopCatalog.Product product : category.products) {
                        if (product != null && product.id != null && product.item != null && product.item.startsWith(MOVE_ITEM_PREFIX)) {
                            CHIP_PRODUCTS.put(product.id, product);
                        }
                    }
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
     * En las pestañas de movimientos el precio es en fichas del casino (sin descuentos) y el saldo es el numero de fichas del jugador.
     */
    public static String filter(String json, String category, class_3222 player) {
        if (json == null || !json.contains("\"tm_moves\"")) {
            return json;
        }
        try {
            boolean moves = category != null && MOVE_CATEGORIES.contains(category.trim().toLowerCase(Locale.ROOT));
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray kept = new JsonArray();
            for (JsonElement element : root.getAsJsonArray("categories")) {
                JsonObject categoryJson = element.getAsJsonObject();
                String id = categoryJson.get("id").getAsString();
                if (MOVE_CATEGORIES.contains(id) != moves) {
                    continue;
                }
                if (moves) {
                    for (JsonElement productElement : categoryJson.getAsJsonArray("products")) {
                        JsonObject productJson = productElement.getAsJsonObject();
                        ShopCatalog.Product product = CHIP_PRODUCTS.get(productJson.get("id").getAsString());
                        if (product != null) {
                            productJson.addProperty("basePrice", product.price);
                            productJson.addProperty("price", product.price);
                        }
                    }
                }
                kept.add(element);
            }
            if (kept.isEmpty()) {
                return json;
            }
            root.add("categories", kept);
            if (moves) {
                root.addProperty("balance", player == null ? 0L : (long) player.method_31548().method_18861(ModRegistries.CASINO_CHIP));
                root.addProperty("discountPercent", 0);
            }
            return GSON.toJson(root);
        } catch (Exception error) {
            LOG.warn("Poke Mart: no se pudo filtrar el catalogo por categoria", error);
            return json;
        }
    }

    /**
     * Compra: los productos de movimientos se cobran en fichas del casino (objeto fisico); el resto va a ShopService.purchase.
     */
    public static ShopService.PurchaseResult purchase(ShopService service, class_3222 player, String productId, int quantity) {
        ShopCatalog.Product product = productId == null ? null : CHIP_PRODUCTS.get(productId);
        if (product == null) {
            return service.purchase(player, productId, quantity);
        }
        synchronized (MoveShopExtras.class) {
            if (quantity < 1 || quantity > product.maxPerPurchase) {
                return ShopService.PurchaseResult.failure("Cantidad no v\u00e1lida para este producto.");
            }
            long now = System.currentTimeMillis();
            Long previous = LAST_PURCHASE.get(player.method_5667());
            if (previous != null && now - previous < PURCHASE_COOLDOWN_MILLIS) {
                return ShopService.PurchaseResult.failure("Espera un instante antes de volver a comprar.");
            }
            net.minecraft.class_2960 itemId = net.minecraft.class_2960.method_12829(product.item);
            if (itemId == null) {
                return ShopService.PurchaseResult.failure("El producto est\u00e1 mal configurado.");
            }
            class_1792 item = (class_1792) net.minecraft.class_7923.field_41178.method_10223(itemId);
            if (item == net.minecraft.class_1802.field_8162) {
                return ShopService.PurchaseResult.failure("El objeto no existe en este modpack.");
            }
            class_1799 template = new class_1799((class_1935) item);
            long capacity = 0L;
            for (int slot = 0; slot < 36 && capacity < quantity; ++slot) {
                class_1799 existing = player.method_31548().method_5438(slot);
                if (existing.method_7960()) {
                    capacity += template.method_7914();
                } else if (class_1799.method_31577(existing, template)) {
                    capacity += Math.max(0, existing.method_7914() - existing.method_7947());
                }
            }
            if (capacity < quantity) {
                return ShopService.PurchaseResult.failure("No tienes espacio suficiente para recibir toda la compra.");
            }
            long total;
            try {
                total = Math.multiplyExact(product.price, (long) quantity);
            } catch (ArithmeticException exception) {
                return ShopService.PurchaseResult.failure("El total de la compra es demasiado grande.");
            }
            if (total > Integer.MAX_VALUE || !removeChips(player, (int) total)) {
                return ShopService.PurchaseResult.failure("No tienes suficientes fichas del casino (necesitas " + total + ").");
            }
            LAST_PURCHASE.put(player.method_5667(), now);
            for (int remaining = quantity; remaining > 0; ) {
                int stackSize = Math.min(remaining, template.method_7914());
                class_1799 delivered = new class_1799((class_1935) item, stackSize);
                player.method_31548().method_7394(delivered);
                if (!delivered.method_7960()) {
                    player.method_7328(delivered, false);
                }
                remaining -= stackSize;
            }
            return ShopService.PurchaseResult.success("Compra completada: " + quantity + "\u00d7 " + template.method_7964().getString() + " por " + total + " fichas.");
        }
    }

    private static boolean removeChips(class_3222 player, int amount) {
        class_1792 chip = ModRegistries.CASINO_CHIP;
        if (player.method_31548().method_18861(chip) < amount) {
            return false;
        }
        int remaining = amount;
        for (int slot = 0; slot < player.method_31548().method_5439() && remaining > 0; ++slot) {
            class_1799 stack = player.method_31548().method_5438(slot);
            if (!stack.method_31574(chip)) {
                continue;
            }
            int removed = Math.min(stack.method_7947(), remaining);
            stack.method_7934(removed);
            remaining -= removed;
        }
        player.method_31548().method_5431();
        return remaining == 0;
    }

    /** Cliente: en los productos de movimientos el precio, el total y el saldo se muestran en fichas en vez de Michicoins. */
    public static String currency(String text, ShopSnapshot.ProductView product) {
        if (text == null || product == null || product.itemId == null || !product.itemId.startsWith(MOVE_ITEM_PREFIX)) {
            return text;
        }
        if (text.endsWith(" Michicoins")) {
            return text.substring(0, text.length() - " Michicoins".length()) + " fichas";
        }
        return text.startsWith("Saldo:") ? text + " fichas" : text;
    }
}
