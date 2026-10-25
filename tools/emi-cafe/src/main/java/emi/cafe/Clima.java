package emi.cafe;

/**
 * El tiempo del dia (desde 0.29.0): al abrir el cafe sale uno al azar y dura hasta que se cierra. Solo cambia lo que pasa en la TERRAZA
 * (las mesas al aire libre, sin nada encima): con sol los clientes la prefieren, aguantan mas y pagan mas; con lluvia casi nadie quiere sentarse fuera.
 */
enum Clima {
    SOLEADO("Soleado", "☀", 3.0, 1.20, 1.15, "La terraza se llena: los clientes la prefieren, aguantan más esperando y pagan un 20 % más."),
    NUBLADO("Nublado", "☁", 1.0, 1.00, 1.00, "Un día normal: la terraza vale igual que el interior."),
    LLUVIA("Lluvia", "☂", 0.25, 0.85, 0.70, "Casi nadie quiere sentarse fuera: la terraza se usa poco, pagan un 15 % menos y se impacientan antes.");

    final String nombre, icono, descripcion;
    /** Cuanto prefieren los clientes una mesa de terraza (1 = igual que una de dentro), cuanto pagan en ella y cuanta paciencia tienen. */
    final double preferencia, pago, paciencia;

    Clima(String nombre, String icono, double preferencia, double pago, double paciencia, String descripcion) {
        this.nombre = nombre; this.icono = icono; this.preferencia = preferencia; this.pago = pago; this.paciencia = paciencia; this.descripcion = descripcion;
    }

    static Clima de(String id) { for (Clima c : values()) if (c.name().equalsIgnoreCase(id)) return c; return null; }

    /** Un tiempo al azar: 40 % sol, 40 % nublado, 20 % lluvia. */
    static Clima azar(java.util.Random r) { int n = r.nextInt(100); return n < 40 ? SOLEADO : n < 80 ? NUBLADO : LLUVIA; }
}
