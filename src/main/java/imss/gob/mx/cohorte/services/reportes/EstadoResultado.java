package imss.gob.mx.cohorte.services.reportes;

/**
 * En qué situación queda un resultado frente a su rango de referencia.
 *
 * <p>Hasta ahora esto era un booleano —dentro o fuera— y el documento lo mostraba
 * pintando el valor de rojo y añadiéndole un asterisco. Para un reporte que se
 * entrega en mano al participante eso es poco y es duro a la vez: poco porque no
 * distingue una diferencia de dos décimas de una que hay que atender, y duro porque
 * pinta las dos igual de rojo.</p>
 *
 * <p>Los cuatro valores son los que usa el reporte de salud. El cuarto no es un
 * adorno: <b>«no se sabe» no es «está bien» ni «está mal»</b>, y confundirlo con
 * cualquiera de los dos es el error que este enum existe para impedir.</p>
 */
public enum EstadoResultado {

    /** Dentro del rango que aplica a esa persona. */
    EN_RANGO("En rango habitual", "En rango", "#1f7a4d", "#e6f1ea"),

    /** Fuera, pero dentro del margen que ese analito admite como diferencia menor. */
    LIGERAMENTE_FUERA("Ligeramente fuera de rango", "Ligeramente fuera", "#b0700f", "#f8eedd"),

    /** Fuera y más allá del margen. */
    REVISAR("Revisar con su médico", "A revisar", "#a8261e", "#f8e7e5"),

    /**
     * No hay con qué decidir: falta el valor, falta el rango, o el parámetro no es
     * numérico. No se dibuja semáforo.
     */
    SIN_DATO("Sin dato registrado", "Sin dato", "#6d7a74", "#eef1ef");

    private final String etiqueta;
    private final String etiquetaCorta;
    private final String color;
    private final String fondo;

    EstadoResultado(String etiqueta, String etiquetaCorta, String color, String fondo) {
        this.etiqueta = etiqueta;
        this.etiquetaCorta = etiquetaCorta;
        this.color = color;
        this.fondo = fondo;
    }

    /** Cómo se nombra donde hay sitio: una leyenda, un pie de tabla. */
    public String etiqueta() {
        return etiqueta;
    }

    /**
     * Cómo se nombra dentro de una fila.
     *
     * <p>Hay dos formas y no una porque en la columna de una tabla «Revisar con su
     * médico» se parte en dos renglones, y una etiqueta de estado partida deja de
     * leerse de un vistazo, que es lo único que tiene que hacer.</p>
     */
    public String etiquetaCorta() {
        return etiquetaCorta;
    }

    /** Color del texto y de la marca. */
    public String color() {
        return color;
    }

    /** Fondo de la pastilla, cuando el diseño la usa. */
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
