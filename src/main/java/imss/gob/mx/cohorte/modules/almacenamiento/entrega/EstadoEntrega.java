package imss.gob.mx.cohorte.modules.almacenamiento.entrega;

public enum EstadoEntrega {
    /** Muestra entregada al colaborador — en sus manos, pendiente de desenlace. */
    ENTREGADA,
    /** El colaborador devolvió la muestra; regresó al biobanco. Terminal. */
    DEVUELTA,
    /** La muestra no regresó porque se consumió. Terminal. */
    CONSUMIDA,
    /** Entrega cancelada (registro corregido antes de cualquier cierre). Terminal. */
    CANCELADA
}
