package imss.gob.mx.cohorte.services.formulas;

/**
 * Qué mide una unidad.
 *
 * <p>Existe para una sola cosa: saber qué unidades se pueden intercambiar entre sí.
 * Centímetros y metros miden longitud, así que ofrecer el cambio de una a otra es
 * seguro; kilogramos y milímetros no tienen nada que ver y no se ofrece nada.</p>
 *
 * <p>El catálogo de unidades del sistema es texto libre y no dice nada de esto: para
 * él «cm», «metros» y «milímetros» son tres cadenas sin relación. Esta es la capa
 * que faltaba.</p>
 *
 * <p><b>Lo que no hace:</b> impedir operaciones. Quien arma una fórmula puede
 * combinar lo que quiera —esa fue una decisión explícita— y aquí solo se responde
 * qué es convertible, no qué está permitido.</p>
 */
public enum Dimension {

    LONGITUD,
    MASA,
    TIEMPO,
    PRESION,
    VOLUMEN,

    /** Volumen por unidad de tiempo: los flujos de la espirometría. */
    FLUJO,

    /** Masa disuelta por volumen: la química sanguínea. */
    CONCENTRACION,

    /** Actividad de una enzima por volumen. */
    ACTIVIDAD_ENZIMATICA,

    /** Cuentas celulares por volumen. */
    CONTEO_POR_VOLUMEN,

    /** Latidos, respiraciones y demás repeticiones por unidad de tiempo. */
    FRECUENCIA,

    /** Longitud recorrida por unidad de tiempo. */
    VELOCIDAD,

    MASA_POR_SUPERFICIE,
    ENERGIA_POR_TIEMPO,
    ATENUACION,
    RESISTENCIA_ELECTRICA,

    /** Porcentajes, puntajes de escala y todo lo que no lleva unidad física. */
    ADIMENSIONAL,

    /**
     * La unidad no se reconoció.
     *
     * <p>El catálogo tiene entradas que no son unidades —«Según equipo», «Sexo de
     * nacimiento», «A la entrega»— y variantes de una misma unidad escritas de
     * formas distintas. Nada de eso se adivina: se marca como desconocida, el valor
     * sigue sirviendo para calcular, y lo único que se pierde es poder cambiarlo a
     * otra unidad.</p>
     *
     * <p>Dos unidades desconocidas nunca son convertibles entre sí, aunque el texto
     * coincida. Suponer que lo son sería inventar un factor.</p>
     */
    DESCONOCIDA
}
