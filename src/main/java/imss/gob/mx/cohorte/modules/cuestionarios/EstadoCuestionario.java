package imss.gob.mx.cohorte.modules.cuestionarios;

/** En qué punto del circuito está un cuestionario contestado. */
public enum EstadoCuestionario {

    /**
     * Capturándose. Admite faltantes y no se exige nada: un cuestionario de dos mil
     * preguntas no se llena de una sentada, y obligar a terminarlo para poder
     * guardarlo garantizaría que se capture en papel y se transcriba después.
     */
    BORRADOR,

    /** Terminado: todas las obligatorias habilitadas tienen respuesta. */
    COMPLETO,

    /**
     * Revisado por una persona contra la hoja original. Es el estado que distingue
     * «lo que dijo el escáner» de «lo que alguien confirmó», y hace falta porque el
     * escáner se equivoca: de eso habla la hoja de discrepancias de cada volcado.
     */
    VALIDADO,

    /**
     * Retirado del análisis sin borrarlo. Las respuestas se conservan —son el
     * registro de lo que se capturó— pero el cuestionario no cuenta.
     */
    ANULADO
}
