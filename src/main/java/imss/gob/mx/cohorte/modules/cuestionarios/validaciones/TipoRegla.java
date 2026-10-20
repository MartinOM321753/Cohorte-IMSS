package imss.gob.mx.cohorte.modules.cuestionarios.validaciones;

/**
 * Las formas de comprobación que el cuestionario necesita de verdad.
 *
 * <p>Son cinco porque son las que aparecen en el instrumento, no las que se podrían
 * imaginar. Cada una salió de leer el papel: hay opciones de descarte que excluyen al
 * resto, hay «otro, especifique» que se quedan sin especificar, hay secciones que solo
 * contestan las mujeres, hay fechas que no pueden estar adelante y hay números que no
 * pueden pasar de la edad de quien contesta.</p>
 *
 * <p>El salto —«pase a la pregunta N»— no está aquí: vive en la propia pregunta, en
 * {@code dependeDe}, porque no es una comprobación sino parte de qué se pregunta.</p>
 */
public enum TipoRegla {

    /**
     * Marcó la opción de descarte y además otra cosa del mismo bloque. El «Ninguna»
     * de la 112, el «No trabajo» de la 114.
     *
     * <p>Es la regla que más datos salva: sin ella, el salto da esas respuestas por
     * «no preguntadas» y al importar se tiran sin que nadie se entere.</p>
     */
    EXCLUYE,

    /** Marcó «otro» y no escribió cuál. */
    REQUIERE_TEXTO,

    /** La pregunta o el bloque es solo de hombres o solo de mujeres. */
    SOLO_SEXO,

    /** Una fecha que no puede estar adelante de hoy. */
    FECHA_NO_FUTURA,

    /** Un número que no puede pasar de otro, ni de la edad del participante. */
    NO_MAYOR_QUE
}
