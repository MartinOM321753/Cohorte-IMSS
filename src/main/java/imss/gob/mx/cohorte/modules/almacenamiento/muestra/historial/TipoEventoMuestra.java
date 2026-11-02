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
    PRESTAMO_CANCELADO,
    /** Muestra dada de baja de manera irreversible por su institución propietaria. */
    MUESTRA_DADA_BAJA,
    /** Se creó un lote de alícuotas: su volumen queda reservado en la padre. */
    ALICUOTAS_COMPROMETIDAS,
    /** Una alícuota ocupó posición por primera vez y descontó de su padre. */
    ALICUOTA_MATERIALIZADA,
    /** La muestra se quedó sin volumen por consumo normal (no es una baja). */
    MUESTRA_AGOTADA,
    /** Muestra entregada en préstamo a un colaborador (persona física). */
    ENTREGA_REALIZADA,
    /** Entrega a colaborador devuelta: la muestra regresó al biobanco. */
    ENTREGA_DEVUELTA,
    /** Entrega a colaborador cerrada como consumida: la muestra no regresó. */
    ENTREGA_CONSUMIDA,
    /** Entrega a colaborador cancelada (registro corregido): la muestra regresó. */
    ENTREGA_CANCELADA
}
