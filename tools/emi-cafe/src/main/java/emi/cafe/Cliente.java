package emi.cafe;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Un cliente: un Pokemon real de Cobblemon sin IA (invulnerable y callado) que el mod mueve cada tick por una ruta fija: entra por la puerta, va a su mesa, pide un plato
 * (un globo con el dibujo del plato y una barra de paciencia encima), come cuando se lo sirven, paga y se va. Si se le acaba la paciencia se va enfadado y sin pagar.
 */
final class Cliente {
    enum Fase { ENTRANDO, ESPERANDO, COMIENDO, SALIENDO }

    /** Las especies de clientes por nivel de local: el nivel 1 tiene las de siempre y cada ampliacion trae mas variedad. */
    static final String[][] POR_NIVEL = {
            {"rattata", "pidgey", "eevee", "pikachu", "slowpoke", "meowth", "psyduck", "jigglypuff", "snubbull", "growlithe"},
            {"vulpix", "mareep", "marill"},
            {"cubone", "teddiursa", "wooper"},
            {"shinx", "piplup", "buneary"},
            {"togepi", "skitty", "chikorita"},
            {"torchic", "mudkip", "treecko"},
            {"riolu", "munchlax", "pichu"},
            {"ponyta", "dratini", "oddish"},
            {"espeon", "umbreon", "vaporeon"},
            {"flareon", "jolteon", "leafeon"}};

    /** Una especie al azar de las que ya pueden venir a un local de ese nivel. */
    static String elegirEspecie(Random rnd, int nivelLocal, java.util.Map<String, Integer> clientela, Sabores.Region semana) {
        int n = Math.max(1, Math.min(POR_NIVEL.length, nivelLocal));
        List<String> todas = new ArrayList<>();
        for (int i = 0; i < n; i++) todas.addAll(java.util.Arrays.asList(POR_NIVEL[i]));
        return Clientela.elegir(rnd, todas, clientela, e -> Sabores.pesoRegion(semana, e));          // las especies habituales y VIP vienen mas a menudo
    }
    /** Especies cuyo modelo se ve mucho mas grande que su caja de colision (medido en el juego): se encogen para no pasar por encima de la mesa. */
    static final java.util.Map<String, Float> AJUSTE = java.util.Map.of("rattata", 0.58f, "slowpoke", 0.65f, "gatitoalien", 1.2f);
    static String forzar;                           // solo pruebas: /cafe dev especie <id> hace que todos sean de esa especie
    static final double VELOCIDAD = 0.14;           // bloques por tick al andar
    static final double ALTURA = 1.15;              // todos se ven del mismo tamaño aproximado

    private static long LLEGADAS;                // contador de llegadas: el que llego antes se atiende antes
    long llegada;                                // numero de llegada a su mesa (0 = aun camino de ella)
    final PokemonEntity e;
    final Mesa mesa;
    final String especie;
    Fase fase = Fase.ENTRANDO;
    Item pedido;
    int paciencia, pacienciaMax;
    int comiendo;
    int cuenta;                              // monedas que el cajero cobrara en su mesa (0 = nada pendiente); mientras haya, el cliente espera la cuenta (0.34.0)
    boolean calmado;                         // un anfitrion ya le ha calmado (solo una vez)
    boolean enfadado;
    boolean sirvio;                          // se le sirvio el plato (deja plato sucio al irse)
    private final List<double[]> ruta;
    private int paso;
    private double alto = ALTURA;
    private Entity burbuja, icono, barra, titulo, mensaje;
    private PoseType pose = PoseType.STAND;

    /** Si es un visitante especial (un Michialien con accesorio que enseña su plato), cual; null = cliente normal. */
    Especial esp;
    boolean critico;                         // el critico gastronomico (ver Cafe.forzarCritico)
    java.util.Map<Item, Integer> calidades;  // ingredientes de la granja que el chef ya saco de la despensa para su plato (ingrediente -> calidad); null = nadie ha cocinado aun
    int nivelClientela;                      // 0 = cliente nuevo; 1-3 = habitual, fiel o VIP de su especie (ver Clientela)
    /** Los clientes que llegaron juntos (0.33.0). */
    static final class Grupo { final int tam; int servidos, enfadados; Grupo(int tam) { this.tam = tam; } }
    Grupo grupo;
    boolean favorito;                        // pide el plato que mas le gusta a su especie (paga extra)
    /** Un espectador de Twitch que pidio con !cafeteria: su nombre, el plato que escribio (null = cliente normal) y su mensaje (puede estar vacio). */
    String twitchNombre, twitchMsg;
    Item twitchPlato;

    private Cliente(PokemonEntity e, Mesa mesa, String especie, List<double[]> ruta) { this.e = e; this.mesa = mesa; this.especie = especie; this.ruta = ruta; }

    /** Crea el cliente en la calle, delante de la puerta, y lo manda a su mesa; null si Cobblemon no pudo crearlo. */
    /** El critico gastronomico (un Persian): pide el plato mas caro, paga el triple y da o quita reputacion. */
    static final String ESPECIE_CRITICO = "persian";

    static Cliente crear(ServerWorld w, int ox, Mesa mesa, Random rnd, Especial esp, boolean critico, int nivelLocal, Rutas rutas, long ahora, boolean exigirRuta, Cafe cafe) {
        java.util.Map<String, Integer> clientela = cafe.dj().clientela;
        String especie = esp != null ? "gatitoalien" : critico ? ESPECIE_CRITICO : forzar != null ? forzar : elegirEspecie(rnd, nivelLocal, clientela, cafe.region);
        try {
            PokemonEntity e = PokemonProperties.Companion.parse(especie + (esp != null ? " " + esp.aspecto : "") + " level=" + (5 + rnd.nextInt(20))).createEntity(w);
            e.setAiDisabled(true);
            e.setEnablePoseTypeRecalculation(false);
            e.setInvulnerable(true);
            e.setSilent(true);
            e.setNoGravity(true);
            e.noClip = true;                      // la ruta es fija: ni las mesas ni los otros clientes lo frenan (un Slowpoke no cabria entre las mesas)
            e.setPersistent();
            e.setCustomNameVisible(false);
            try { e.hideNameRendering(); } catch (Throwable t) { /* version de Cobblemon sin esta funcion */ }
            e.addCommandTag(Disp.TAG);
            double x0 = ox + Local.PUERTA_X, z0 = Local.CALLE_Z;
            e.refreshPositionAndAngles(x0, Local.Y + 1, z0, 180f, 0f);
            w.spawnEntity(e);
            List<double[]> ruta = null;
            if (rutas != null) {                                   // por donde se pueda andar: esquiva paredes, mesas y decoracion y se sienta en una casilla libre junto a su mesa
                List<double[]> cam = rutas.camino(x0, z0, Local.Y + 1, rutas.adyacentes(mesa.bloque, ahora), ahora);
                if (cam != null && !cam.isEmpty()) {
                    ruta = new ArrayList<>();
                    ruta.add(new double[]{x0, z0, Local.Y + 1});
                    ruta.addAll(cam);
                    double[] fin = cam.get(cam.size() - 1);
                    mesa.sx = fin[0]; mesa.sz = fin[1];
                }
            }
            if (ruta == null && exigirRuta) { e.discard(); return null; }       // parcela libre: si no se puede llegar a la mesa, no entra nadie
            if (ruta == null) {                                    // sin camino (la puerta tapada...): la ruta de siempre
                mesa.sx = mesa.bloque.getX() + 0.5; mesa.sz = mesa.bloque.getZ() + 1.5;
                ruta = new ArrayList<>();
                ruta.add(new double[]{x0, z0, Local.Y + 1});
                ruta.add(new double[]{x0, Local.PASILLO_Z, Local.Y + 1});
                ruta.add(new double[]{mesa.sx, Local.PASILLO_Z, Local.Y + 1});
                ruta.add(new double[]{mesa.sx, mesa.sz, Local.Y + 1});
            }
            Cliente c = new Cliente(e, mesa, especie, ruta);
            float escala = (float) Math.max(0.35, Math.min(3.0, ALTURA / Math.max(0.3, e.getHeight()))) * AJUSTE.getOrDefault(especie, 1f);
            e.getPokemon().setScaleModifier(escala);
            c.alto = ALTURA;
            c.esp = esp;
            c.critico = critico;
            c.nivelClientela = esp == null && !critico ? Clientela.nivel(clientela, especie) : 0;
            return c;
        } catch (Throwable t) {
            EmiCafe.LOG.error("No se pudo crear el cliente {}", especie, t);
            return null;
        }
    }

    /** Un compañero de grupo se fue enfadado: me queda un tercio de paciencia como mucho. */
    void impacientar() { if (fase == Fase.ESPERANDO) paciencia = Math.min(paciencia, Math.max(40, pacienciaMax / 3)); }

    boolean fuera() { return e.isRemoved(); }

    /** Un tick. Devuelve true cuando el cliente ya se ha ido y se puede borrar. */
    boolean tick(Cafe cafe, Random rnd) {
        ServerWorld w = (ServerWorld) e.getWorld();
        switch (fase) {
            case ENTRANDO -> {
                if (andar(ruta.get(paso)) && ++paso >= ruta.size()) {
                    fase = Fase.ESPERANDO;
                    mirarMesa();
                    llegada = ++LLEGADAS;
                    pedido = twitchPlato != null ? twitchPlato : esp != null ? esp.plato : critico ? cafe.pedidoCritico() : cafe.elegirPedido(rnd, especie);
                    favorito = esp == null && !critico && twitchPlato == null && pedido == cafe.favoritoDe(especie);
                    pacienciaMax = paciencia = (int) Math.round((esp != null || twitchPlato != null ? cafe.pacienciaTicks() * 8 / 5 : critico ? cafe.pacienciaTicks() * 7 / 10 : cafe.pacienciaTicks()) * cafe.climaPaciencia(mesa) * (1.0 + Clientela.PACIENCIA_PCT[nivelClientela] / 100.0));
                    if (twitchPlato != null) cafe.alLlegarTwitch(this);
                    if (esp != null) cafe.alLlegarEspecial(this);
                    if (critico) cafe.alLlegarCritico(this);
                    ponerPose(PoseType.STAND);
                    crearGlobo(w);
                    Sonidos.mundo(w, e.getX(), e.getY(), e.getZ(), "campana", 1.0f, 1.0f);
                }
            }
            case ESPERANDO -> {
                if (--paciencia <= 0) { enfadado = true; irse(w); cafe.alEnfadarse(this); }
                else if (paciencia % 10 == 0) actualizarGlobo(w);
            }
            case COMIENDO -> {
                if (--comiendo <= 0) irse(w);
                else if (comiendo % 12 == 0) w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, e.getX(), e.getY() + alto + 0.1, e.getZ(), 2, 0.25, 0.1, 0.25, 0.0);
            }
            case SALIENDO -> {
                if (andar(ruta.get(paso))) { if (--paso < 0) { quitar(); return true; } }
            }
        }
        return false;
    }

    /** Se le ha servido el plato correcto: empieza a comer. */
    void comer() {
        fase = Fase.COMIENDO;
        sirvio = true;
        comiendo = 70;
        quitarGlobo();
        ponerPose(PoseType.STAND);
    }

    /** ¿Ha terminado de comer y espera a que el cajero le cobre la cuenta? */
    boolean esperaCuenta() { return fase == Fase.COMIENDO && cuenta > 0; }

    /** El cajero ha cobrado la cuenta: se va enseguida. */
    void cuentaCobrada() { cuenta = 0; comiendo = Math.min(comiendo, 25); }

    /** Un anfitrion le atiende: recupera paciencia (una vez). */
    void calmar(double fraccion) { if (fase == Fase.ESPERANDO && !calmado) { calmado = true; paciencia = Math.min(pacienciaMax, paciencia + (int) Math.round(pacienciaMax * fraccion)); } }

    /** Se acaba el turno: se va sin pagar y sin enfadarse. */
    void despedir() { if (fase == Fase.ESPERANDO) irse((ServerWorld) e.getWorld()); }

    private void irse(ServerWorld w) {
        quitarGlobo();
        fase = Fase.SALIENDO;
        paso = ruta.size() - 2;                  // vuelve por donde vino (sin repetir el punto de la mesa)
        if (enfadado) w.spawnParticles(ParticleTypes.ANGRY_VILLAGER, e.getX(), e.getY() + alto, e.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
    }

    /** Se sienta mirando a su mesa. */
    private void mirarMesa() {
        double dx = mesa.bloque.getX() + 0.5 - e.getX(), dz = mesa.bloque.getZ() + 0.5 - e.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        e.setYaw(yaw); e.setHeadYaw(yaw); e.setBodyYaw(yaw);
    }

    /** Un paso hacia (x, z) de la ruta; true si ya llego. */
    private boolean andar(double[] p) {
        double dx = p[0] - e.getX(), dz = p[1] - e.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 0.12) { ponerPose(PoseType.STAND); return true; }
        double v = Math.min(VELOCIDAD, d);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        e.setYaw(yaw); e.setHeadYaw(yaw); e.setBodyYaw(yaw);
        e.setVelocity(Vec3d.ZERO);
        double ty = p.length > 2 ? p[2] : Local.Y + 1;
        e.setPosition(e.getX() + dx / d * v, e.getY() + (ty - e.getY()) * Math.min(1.0, v / d), e.getZ() + dz / d * v);      // sube y baja escaleras poco a poco
        ponerPose(PoseType.WALK);
        return false;
    }

    /** La pose del modelo (con la IA apagada Cobblemon las dejaria siempre «de pie»): andando, el modelo hace su animacion de caminar. */
    private void ponerPose(PoseType nueva) {
        if (nueva == pose) return;
        pose = nueva;
        try { e.getDataTracker().set(PokemonEntity.getPOSE_TYPE(), nueva); } catch (Throwable t) { /* se queda como estaba */ }
    }

    // ------------------------------------------------------------ el globo del pedido

    private double ybase() { return e.getY() + alto + 0.55; }

    private void crearGlobo(ServerWorld w) {
        burbuja = Disp.objeto(w, CafeItems.id(CafeItems.BURBUJA), e.getX(), ybase(), e.getZ(), 0.95f, true);
        icono = Disp.objeto(w, CafeItems.id(pedido), e.getX(), ybase() + 0.08, e.getZ(), 0.5f, true, 0.04f);
        barra = Disp.texto(w, barraJson(), e.getX(), ybase() + 0.62, e.getZ(), 1.0f, false);
        if (twitchNombre != null) {
            titulo = Disp.texto(w, "{\"text\":\"✦ " + twitchNombre + " ✦\",\"color\":\"light_purple\",\"bold\":true}", e.getX(), ybase() + 0.95, e.getZ(), 0.9f, false);
            if (twitchMsg != null && !twitchMsg.isEmpty()) {
                String txt = TwitchTexto.enLineas(twitchMsg);
                int lineas = txt.split("\n", -1).length;
                mensaje = Disp.texto(w, "{\"text\":\"" + txt + "\",\"color\":\"white\"}", e.getX(), ybase() + 1.2 + 0.09 * lineas, e.getZ(), 0.8f, true);
            }
        }
        else if (esp != null) titulo = Disp.texto(w, "{\"text\":\"★ " + esp.michi + " ★\",\"color\":\"gold\",\"bold\":true}", e.getX(), ybase() + 0.95, e.getZ(), 0.9f, false);
        else if (critico) titulo = Disp.texto(w, "{\"text\":\"★ Crítico gastronómico ★\",\"color\":\"aqua\",\"bold\":true}", e.getX(), ybase() + 0.95, e.getZ(), 0.9f, false);
        else if (nivelClientela > 0 || favorito || grupo != null) {
            String t = (grupo != null ? "👥 Grupo de " + grupo.tam + (nivelClientela > 0 || favorito ? " · " : "") : "")
                    + (nivelClientela > 0 ? Clientela.ICONOS[nivelClientela] + " " + Clientela.NOMBRES[nivelClientela] + (favorito ? " · ♥ ¡Su favorito!" : "") : favorito ? "♥ ¡Su favorito!" : "");
            titulo = Disp.texto(w, "{\"text\":\"" + t + "\",\"color\":\"" + (nivelClientela > 0 ? Clientela.COLORES[nivelClientela] : "light_purple") + "\",\"bold\":true}", e.getX(), ybase() + 0.95, e.getZ(), 0.8f, false);
        }
    }

    private void actualizarGlobo(ServerWorld w) { Disp.cambiarTexto(barra, barraJson(), e.getX(), ybase() + 0.62, e.getZ(), 1.0f, false); }

    private String barraJson() {
        double f = pacienciaMax == 0 ? 0 : (double) paciencia / pacienciaMax;
        int n = (int) Math.ceil(f * 6);
        String color = f > 0.5 ? "green" : f > 0.25 ? "yellow" : "red";
        return "{\"text\":\"" + "▮".repeat(Math.max(0, n)) + "\",\"color\":\"" + color + "\"}";
    }

    private void quitarGlobo() { Disp.quitar(burbuja); Disp.quitar(icono); Disp.quitar(barra); Disp.quitar(titulo); Disp.quitar(mensaje); burbuja = icono = barra = titulo = mensaje = null; }

    void quitar() { quitarGlobo(); if (!e.isRemoved()) e.discard(); }

    /** La paciencia que le queda, de 0 a 1 (para la propina). */
    double pacienciaRestante() { return pacienciaMax == 0 ? 0 : (double) paciencia / pacienciaMax; }
}
