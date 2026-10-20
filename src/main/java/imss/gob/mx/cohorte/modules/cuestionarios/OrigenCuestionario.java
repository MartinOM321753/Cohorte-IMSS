package imss.gob.mx.cohorte.modules.cuestionarios;

/**
 * De dónde salieron las respuestas.
 *
 * <p>Se guarda porque la confianza que merece el dato no es la misma y el día que
 * convivan las dos vías habrá que poder separarlas en cualquier análisis. Un
 * cuestionario contestado en la aplicación no pasó por escáner: no tiene celdas
 * sin autorizar, no tiene discrepancias de lectura y sus rangos se validaron al
 * teclear. Uno que vino del papel arrastra todo eso.</p>
 */
public enum OrigenCuestionario {

    /** Hoja en papel escaneada con Remark y cargada desde el volcado. */
    PAPEL_REMARK,

    /** Contestado directamente en la aplicación. */
    APLICACION,

    /** Transcrito a mano desde el papel, sin pasar por el escáner. */
    CAPTURA_MANUAL
}
