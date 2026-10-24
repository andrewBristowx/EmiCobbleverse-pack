package emi.cafe;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * La guia de inicio: te lleva de la mano por tu primer plato (el que te enseña tu cocinero) con una barra arriba que dice que hacer ahora y una columna de chispas sobre la estacion
 * a la que hay que ir. Cada paso se da por hecho solo, mirando lo que llevas en la mano y lo que hay en las estaciones; no hace falta pulsar nada. Al servir el primer plato termina,
 * da unas estrellas y explica el resto del juego. {@code /cafe tutorial} la repite y {@code /cafe tutorial saltar} la quita.
 */
final class Tutorial {
    /** Un paso: lo que hay que hacer, adonde ir (null = ninguna estacion en concreto) y cuando se considera hecho. */
    private record Paso(String texto, Estacion objetivo, BiPredicate<Cafe, ServerPlayerEntity> hecho) {}

    private final Cafe c;
    private final List<Paso> pasos = new ArrayList<>();
    private final ServerBossBar barra = new ServerBossBar(Text.empty(), BossBar.Color.PINK, BossBar.Style.PROGRESS);
    private int i;
    private boolean fin;

    Tutorial(Cafe c, Recetas.Inicial ini) {
        this.c = c;
        pasos.add(new Paso("Abre el café en la CAJA REGISTRADORA (clic derecho)", Estacion.REGISTRADORA, (cf, p) -> cf.estado == Cafe.Estado.ABIERTO));
        pasos.add(new Paso("Espera un cliente: su globo dice lo que pide", null, (cf, p) -> hayClienteEsperando(cf)));
        switch (ini) {
            case CHARMANDER -> {
                pasos.add(new Paso("Coge una papa en la CAJA DE PAPAS (clic derecho)", Estacion.CAJA_PAPAS, mano(CafeItems.PAPA)));
                pasos.add(new Paso("Llévala a la TABLA DE CORTAR y espera la barra", Estacion.TABLA, ocupada(Estacion.TABLA)));
                pasos.add(new Paso("Cuando ponga ¡Listo!, clic otra vez para cogerlas", Estacion.TABLA, mano(CafeItems.PAPA_TIRAS)));
                pasos.add(new Paso("Llévalas a la FREIDORA y espera", Estacion.FREIDORA, ocupada(Estacion.FREIDORA)));
                pasos.add(new Paso("Recógelas ya: ¡se queman si te despistas!", Estacion.FREIDORA, mano(CafeItems.PAPAS_FRITAS)));
            }
            case SQUIRTLE -> {
                pasos.add(new Paso("Coge una baya en la CAJA DE BAYAS (clic derecho)", Estacion.CAJA_BAYAS, mano(CafeItems.BAYA)));
                pasos.add(new Paso("Llévala al EXPRIMIDOR y espera la barra", Estacion.EXPRIMIDOR, ocupada(Estacion.EXPRIMIDOR)));
                pasos.add(new Paso("Cuando ponga ¡Listo!, clic otra vez para cogerla", Estacion.EXPRIMIDOR, mano(CafeItems.LIMONADA)));
            }
            case BULBASAUR -> {
                pasos.add(new Paso("Coge una lechuga en la CAJA DE LECHUGAS", Estacion.CAJA_LECHUGA, mano(CafeItems.LECHUGA)));
                pasos.add(new Paso("Llévala a la TABLA DE CORTAR y espera la barra", Estacion.TABLA, ocupada(Estacion.TABLA)));
                pasos.add(new Paso("Cuando ponga ¡Listo!, clic otra vez para cogerla", Estacion.TABLA, mano(CafeItems.LECHUGA_PICADA)));
                pasos.add(new Paso("Déjala en la MESA DE EMPLATAR (clic derecho)", Estacion.EMPLATAR, emplatarCon(CafeItems.LECHUGA_PICADA)));
                pasos.add(new Paso("Ahora coge una baya en la CAJA DE BAYAS", Estacion.CAJA_BAYAS, mano(CafeItems.BAYA)));
                pasos.add(new Paso("Déjala también en la mesa de emplatar", Estacion.EMPLATAR, (cf, p) -> mano(null).test(cf, p) && estadoOcupado(cf, Estacion.EMPLATAR)));
                pasos.add(new Paso("Cuando ponga ¡Listo!, coge la ensalada", Estacion.EMPLATAR, mano(CafeItems.ENSALADA)));
            }
        }
        pasos.add(new Paso("Sirve el plato: clic derecho en el cliente o su mesa", null, (cf, p) -> cf.servidos > 0));
    }

    // ------------------------------------------------------------ condiciones

    private static boolean hayClienteEsperando(Cafe cf) {
        for (Cliente x : cf.clientes) if (x.fase == Cliente.Fase.ESPERANDO) return true;
        return false;
    }

    private static BiPredicate<Cafe, ServerPlayerEntity> mano(Item item) { return (cf, p) -> cf.manos.get(p.getUuid()) == item; }

    private static boolean estadoOcupado(Cafe cf, Estacion e) {
        Cafe.Est est = cf.estDe(e);
        return est != null && (est.resultado != null || est.listo != null);
    }

    /** Con las manos vacias y la estacion trabajando (o ya con algo listo). */
    private static BiPredicate<Cafe, ServerPlayerEntity> ocupada(Estacion e) { return (cf, p) -> cf.manos.get(p.getUuid()) == null && estadoOcupado(cf, e); }

    private static BiPredicate<Cafe, ServerPlayerEntity> emplatarCon(Item item) {
        return (cf, p) -> {
            if (cf.manos.get(p.getUuid()) != null) return false;
            Cafe.Est est = cf.estDe(Estacion.EMPLATAR);
            return est != null && (est.dentro.contains(item) || est.resultado != null || est.listo != null);
        };
    }

    // ------------------------------------------------------------ vida de la guia

    boolean terminada() { return fin; }

    void quitar() { barra.clearPlayers(); }

    /** Cada medio segundo: da por hechos los pasos cumplidos, pone el texto de la barra y las chispas sobre el sitio al que hay que ir. */
    void tick() {
        if (fin) return;
        ServerPlayerEntity p = c.jugador(c.duenio);
        if (p == null || c.construyendo) { barra.clearPlayers(); return; }
        if (!barra.getPlayers().contains(p)) barra.addPlayer(p);
        while (i < pasos.size() && pasos.get(i).hecho().test(c, p)) {
            i++;
            if (i < pasos.size()) Msg.sound(p, net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
        }
        if (i >= pasos.size()) { terminar(p); return; }
        Paso paso = pasos.get(i);
        boolean cerrado = i >= 2 && c.estado != Cafe.Estado.ABIERTO;
        String texto = cerrado ? "Abre el café otra vez en la caja registradora" : paso.texto();
        barra.setName(Text.literal("Guía " + (i + 1) + "/" + pasos.size() + ": " + texto).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        barra.setPercent((float) i / pasos.size());
        BlockPos objetivo = cerrado ? Local.pos(c.ox, Local.X_REGISTRADORA, 1, Local.Z_REGISTRADORA) : destino(paso);
        if (objetivo != null) {
            for (double dy = 1.2; dy <= 2.6; dy += 0.35) c.w.spawnParticles(p, ParticleTypes.END_ROD, true, objetivo.getX() + 0.5, objetivo.getY() + dy, objetivo.getZ() + 0.5, 1, 0.05, 0.0, 0.05, 0.0);
        }
    }

    private BlockPos destino(Paso paso) {
        if (paso.objetivo() == Estacion.REGISTRADORA) return Local.pos(c.ox, Local.X_REGISTRADORA, 1, Local.Z_REGISTRADORA);
        if (paso.objetivo() != null) return c.posDe(paso.objetivo());
        if (i == pasos.size() - 1) for (Cliente x : c.clientes) if (x.fase == Cliente.Fase.ESPERANDO) return x.mesa.bloque;      // el ultimo paso: la mesa del que espera
        return null;
    }

    private void terminar(ServerPlayerEntity p) {
        fin = true;
        barra.clearPlayers();
        Datos.Jugador d = c.dj();
        d.tutorialHecho = true;
        Progreso.dar(c, d, c.duenio, 10);
        EmiCafe.datos.guardar(c.duenio);
        Msg.title(p, Text.literal("¡Primer plato servido!").formatted(Formatting.GOLD, Formatting.BOLD), Text.literal("Ya sabes lo básico: +10 estrellas").formatted(Formatting.YELLOW), 5, 70, 15);
        Sonidos.jugador(p, "subir", 1f, 1f);
        p.sendMessage(Msg.say("¡Muy bien! Con las monedas compra platos nuevos y mejoras en la TIENDA de la caja registradora. Cuando el café esté cerrado puedes ampliarlo (más sitio, más estaciones y más platos), contratar ayudantes y decorarlo en el MODO CONSTRUCCIÓN."), false);
        p.sendMessage(Msg.say("Mira también el RECETARIO (cómo se hace cada plato), las MISIONES y los LOGROS, invita a un amigo a cocinar con /cafe invitar, y visita el café de otros en la pestaña CAFÉS. Para repetir esta guía: /cafe tutorial."), false);
    }
}
