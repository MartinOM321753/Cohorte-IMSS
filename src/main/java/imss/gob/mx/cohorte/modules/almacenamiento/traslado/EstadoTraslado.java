package imss.gob.mx.cohorte.modules.almacenamiento.traslado;

public enum EstadoTraslado {
    /** Admin registró el traslado; muestra en camino al almacén externo. */
    TRASLADADA,
    /** Encargado confirmó que la muestra llegó a su laboratorio. */
    RECIBIDA,
    /** Encargado inició la devolución; muestra en camino de regreso. */
    EN_DEVOLUCION,
    /** Admin confirmó que la muestra fue recibida de vuelta en el biobanco. */
    DEVUELTA
}
