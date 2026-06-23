package imss.gob.mx.cohorte.modules.almacenamiento.traslado;

public enum EstadoTraslado {
    /** Institución origen envió la muestra — en tránsito hacia destino. */
    ENVIADA,
    /** Institución destino confirmó la recepción física de la muestra. */
    RECIBIDA,
    /** Institución destino inició la devolución — muestra en tránsito de regreso. */
    EN_DEVOLUCION,
    /** Institución anterior confirmó la recepción de vuelta. Registro histórico. */
    DEVUELTA,
    /** Préstamo cancelado por la institución origen antes de que el destino confirmara recepción. */
    CANCELADO
}
