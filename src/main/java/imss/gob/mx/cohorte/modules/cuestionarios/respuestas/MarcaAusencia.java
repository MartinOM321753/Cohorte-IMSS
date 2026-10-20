package imss.gob.mx.cohorte.modules.cuestionarios.respuestas;

/**
 * Por qué una respuesta no tiene valor.
 *
 * <p>Un cuestionario en papel escaneado produce al menos cinco formas distintas
 * de «aquí no hay dato», y tratarlas como una sola —nulo— pierde información que
 * después nadie puede recuperar. Las cinco llegan en el volcado de Remark y
 * significan cosas diferentes:</p>
 *
 * <ul>
 *   <li>La celda vacía y el literal {@code BLANK} dicen que el participante no
 *       marcó nada. Es una respuesta ausente, y en una casilla suelta es además
 *       una respuesta válida: significa «no».</li>
 *   <li>El {@code 0} dice que Remark contó cero marcas en la región. Se parece al
 *       anterior pero no es lo mismo: aquí el escáner sí leyó la región.</li>
 *   <li>{@code [UNAUTHORIZED]} no dice nada del participante. Dice que el
 *       reconocimiento de escritura de Azure no estaba licenciado cuando se
 *       procesó la hoja. El dato <em>está</em> en el papel; lo que falta es
 *       haberlo leído. Hoy cae sobre casi nueve de cada diez campos de texto del
 *       instrumento, así que confundirlo con «no contestó» daría por vacío un
 *       cuestionario que está lleno.</li>
 *   <li>{@code ERROR3100} es un fallo de lectura de la propia región.</li>
 * </ul>
 *
 * <p>La diferencia es operativa, no académica: las dos primeras se archivan tal
 * cual, y las tres últimas son trabajo pendiente —hay que recapturar, volver a
 * procesar la hoja o revisarla a mano—. Un informe que las sume todas no sirve
 * para decidir nada.</p>
 */
public enum MarcaAusencia {

    /** La respuesta tiene valor. Es el caso normal. */
    NINGUNA,

    /** Celda vacía en el volcado: el participante no marcó. */
    SIN_RESPUESTA,

    /** Literal {@code BLANK}: Remark declara la región en blanco. */
    EN_BLANCO,

    /** Cero marcas contadas en la región. */
    CERO_MARCAS,

    /**
     * {@code [UNAUTHORIZED]}: el reconocimiento de escritura no estaba licenciado.
     * Pendiente de recaptura; no es una respuesta del participante.
     */
    NO_AUTORIZADO,

    /** {@code ERROR3100} u otro fallo de lectura de la región. */
    ERROR_LECTURA;

    /** Si hace falta intervención humana o volver a procesar la hoja. */
    public boolean requiereAtencion() {
        return this == NO_AUTORIZADO || this == ERROR_LECTURA;
    }
}
