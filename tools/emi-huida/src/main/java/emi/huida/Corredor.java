package emi.huida;

/** Lo implementan todas las entidades vivas (por {@link emi.huida.mixin.LivingEntityMixin}); solo el Tyrantrum de la huida tiene un paso mayor que 0. */
public interface Corredor {
    /** Metros «recorridos» que se suman por tick a la distancia caminada de la entidad (0 = no hace nada). */
    void emi_huida$setPaso(float paso);
}
