package emi.carreras;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Random;

/** Poderes de las cajas misteriosas, con tematica Pokemon. */
public enum Poder {
    BAYA("Baya Aranja", "Pequeño turbo", "cobblemon:oran_berry", Formatting.AQUA, new int[]{30, 20, 10}),
    POKEBALL("Pokéball", "Lánzala: el primer rival al que alcance queda capturado unos segundos (agáchate para tirarla hacia atrás)", "cobblemon:poke_ball", Formatting.RED, new int[]{20, 25, 15}),
    VOLTORB("Voltorb trampa", "Suelta un Voltorb detrás de ti: explota al tocarlo", "cobblemon:great_ball", Formatting.DARK_RED, new int[]{25, 15, 5}),
    ESCUDO("Protección", "Te protege de los ataques durante unos segundos", "minecraft:shield", Formatting.GREEN, new int[]{20, 15, 10}),
    CARAMELO("Caramelo Raro", "Super turbo", "cobblemon:rare_candy", Formatting.LIGHT_PURPLE, new int[]{5, 15, 30}),
    TRUENO("Trueno", "Un rayo golpea a todos los que van delante de ti", "cobblemon:thunder_stone", Formatting.YELLOW, new int[]{0, 10, 30});

    public final String nombre, descripcion, itemId;
    public final Formatting color;
    /** peso al ir primero / en el medio / en ultimo lugar */
    private final int[] pesos;

    Poder(String nombre, String descripcion, String itemId, Formatting color, int[] pesos) {
        this.nombre = nombre; this.descripcion = descripcion; this.itemId = itemId; this.color = color; this.pesos = pesos;
    }

    public Text titulo() { return Text.literal(nombre).formatted(color, Formatting.BOLD); }

    public ItemStack item() {
        Item it = Registries.ITEM.getOrEmpty(Identifier.of(itemId)).orElse(Items.PAPER);
        ItemStack st = new ItemStack(it);
        st.set(DataComponentTypes.CUSTOM_NAME, Text.empty().append(titulo()).append(Text.literal("  (clic derecho)").formatted(Formatting.GRAY)));
        return st;
    }

    /** Elige un poder al azar; quien va mas atras (r cerca de 1) tiene mas probabilidad de los fuertes. r en [0,1]. */
    public static Poder sortear(double r, Random rnd) {
        double[] w = new double[values().length];
        double tot = 0;
        for (int i = 0; i < w.length; i++) {
            int[] p = values()[i].pesos;
            w[i] = r < 0.5 ? p[0] + (p[1] - p[0]) * (r / 0.5) : p[1] + (p[2] - p[1]) * ((r - 0.5) / 0.5);
            tot += w[i];
        }
        double x = rnd.nextDouble() * tot;
        for (int i = 0; i < w.length; i++) { x -= w[i]; if (x <= 0) return values()[i]; }
        return BAYA;
    }
}
