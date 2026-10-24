package emi.cafe;

/** El suceso del dia: al abrir el cafe sale uno al azar y dura hasta que se cierra. */
enum Evento {
    NINGUNO("", ""),
    HORA_PUNTA("Hora punta", "Llegan clientes mucho más seguido, pero tienen un poco menos de paciencia."),
    PLATO_DEL_DIA("Plato del día", "Un plato se paga el doble y lo piden más."),
    DIA_MICHIALIEN("Día Michialien", "Vienen muchos más visitantes Michialien especiales."),
    CRITICO("Crítico gastronómico", "Un crítico viene a probar tu plato más caro: paga el triple y da reputación (o la quita si se va enfadado).");

    final String nombre, descripcion;

    Evento(String nombre, String descripcion) { this.nombre = nombre; this.descripcion = descripcion; }

    static Evento de(String id) { for (Evento e : values()) if (e.name().equalsIgnoreCase(id)) return e; return null; }
}
