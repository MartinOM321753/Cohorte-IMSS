package imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial;

public enum TipoEventoMuestra {
    /** Muestra registrada por primera vez. */
    REGISTRO,
    /** Campo de datos modificado (valor, unidad, observaciones, tipo, tubo…). */
    ACTUALIZACION_CAMPO,
    /** Se asignó o cambió la PosicionCaja en el biobanco. */
    POSICION_ASIGNADA,
    /** Se liberó la PosicionCaja (por movimiento o préstamo). */
    POSICION_LIBERADA,
    /** Muestra salió en préstamo hacia otra institución. */
    PRESTAMO_ENVIADO,
    /** Institución destino confirmó la recepción física de la muestra. */
    PRESTAMO_RECIBIDO,
    /** Muestra devuelta al tenedor anterior — nuevo estado: SIN_POSICION. */
    PRESTAMO_DEVUELTO,
    /** Se registró un EstudioMuestra sobre esta muestra. */
    ESTUDIO_REALIZADO,
    /** Préstamo cancelado por la institución origen antes de recepción confirmada. */
    PRESTAMO_CANCELADO
}
