package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

public enum EstadoMuestra {
    /** Llegó a la institución pero no tiene ubicación física asignada aún. */
    SIN_POSICION,
    /** Tiene PosicionCaja asignada en el biobanco de su institucionActual. */
    EN_BIOBANCO,
    /** En préstamo — institucionActual indica a quién se le prestó. */
    PRESTADA,
    /** Dada de baja / descartada. */
    BAJA
}
