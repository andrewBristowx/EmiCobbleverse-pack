package emi.cafe;

/**
 * El tiempo del dia (desde 0.29.0): al abrir el cafe sale uno al azar y dura hasta que se cierra. Solo cambia lo que pasa en la TERRAZA
 * (las «Mesas de terraza»): con sol los clientes la prefieren, aguantan mas y pagan mas; con lluvia casi nadie quiere sentarse fuera.
 */
enum Clima {
    SOLEADO("Soleado", "☀", 0.60, 1.20, 1.15, "6 de cada 10 clientes quieren la terraza: en ella aguantan más esperando y pagan un 20 % más."),
    NUBLADO("Nublado", "☁", 0.30, 1.00, 1.00, "Un día normal: 3 de cada 10 clientes piden la terraza y vale igual que el interior."),
    LLUVIA("Lluvia", "☂", 0.08, 0.85, 0.70, "Casi nadie quiere sentarse fuera: en la terraza pagan un 15 % menos y se impacientan antes.");

    final String nombre, icono, descripcion;
    /** Probabilidad de que un cliente quiera sentarse en la terraza, cuanto paga en ella y cuanta paciencia tiene. */
    final double probTerraza, pago, paciencia;

    Clima(String nombre, String icono, double probTerraza, double pago, double paciencia, String descripcion) {
        this.nombre = nombre; this.icono = icono; this.probTerraza = probTerraza; this.pago = pago; this.paciencia = paciencia; this.descripcion = descripcion;
    }

    static Clima de(String id) { for (Clima c : values()) if (c.name().equalsIgnoreCase(id)) return c; return null; }

    /** Un tiempo al azar: 40 % sol, 40 % nublado, 20 % lluvia. */
    static Clima azar(java.util.Random r) { int n = r.nextInt(100); return n < 40 ? SOLEADO : n < 80 ? NUBLADO : LLUVIA; }
}
