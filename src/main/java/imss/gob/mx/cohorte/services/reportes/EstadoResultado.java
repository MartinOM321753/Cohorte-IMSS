package imss.gob.mx.cohorte.services.reportes;

/**
 * Qué tan lejos queda un resultado de su rango de referencia.
 *
 * <p>Esto decide el <b>color</b> con el que se pinta el resultado, no las palabras.
 * Lo que el documento escribe son solo tres estados —«Dentro del rango», «Por
 * debajo» y «Por arriba»— y salen de {@link RangoReferencia#etiquetaEstado}. El
 * color añade lo que la palabra no dice: una diferencia menor va en ámbar y una que
 * se sale del margen de revisión del analito, en rojo.</p>
 *
 * <p>{@link #SIN_DATO} no es un estado más: «no se sabe» no es «está bien» ni «está
 * mal», y por eso no se rotula ni se pinta.</p>
 */
public enum EstadoResultado {

    /** Dentro del rango que aplica a esa persona. */
    EN_RANGO("#1f7a4d", "#e6f1ea"),

    /** Fuera, pero dentro del margen que ese analito admite como diferencia menor. */
    LIGERAMENTE_FUERA("#b0700f", "#f8eedd"),

    /** Fuera y más allá del margen. */
    REVISAR("#a8261e", "#f8e7e5"),

    /**
     * No hay con qué decidir: falta el valor, falta el rango, o el parámetro no es
     * numérico. No se dibuja semáforo.
     */
    SIN_DATO("#6d7a74", "#eef1ef");

    private final String color;
    private final String fondo;

    EstadoResultado(String color, String fondo) {
        this.color = color;
        this.fondo = fondo;
    }

    /** Color del texto y de la marca. */
    public String color() {
        return color;
    }

    /** Fondo de la franja, cuando el diseño la usa. */
    public String fondo() {
        return fondo;
    }

    /** ¿Hay algo que señalar? SIN_DATO no cuenta: no se sabe. */
    public boolean fuera() {
        return this == LIGERAMENTE_FUERA || this == REVISAR;
    }

    /** ¿Se puede contar en los totales del resumen? */
    public boolean medido() {
        return this != SIN_DATO;
    }
}
