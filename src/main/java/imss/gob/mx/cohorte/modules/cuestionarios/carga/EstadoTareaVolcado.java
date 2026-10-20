package imss.gob.mx.cohorte.modules.cuestionarios.carga;

/**
 * En qué va una carga de volcado.
 *
 * <p>Son tres y no cuatro a propósito: no hay «ENCOLADA» aparte de «EN_PROCESO».
 * Distinguir lo que espera turno de lo que ya corre obligaría a mantener ese estado
 * sincronizado con el ejecutor —que es quien de verdad lo sabe— y al que subió el
 * archivo le da igual: lo que pregunta es si ya acabó.</p>
 */
public enum EstadoTareaVolcado {

    /** Se está leyendo y guardando. Las cifras que lleva van creciendo. */
    EN_PROCESO,

    /**
     * Acabó de recorrer el archivo.
     *
     * <p>Terminada no quiere decir que todo entrara: puede haber filas omitidas y se
     * informan una por una. Quiere decir que ya no va a cambiar.</p>
     */
    TERMINADA,

    /**
     * Se cortó antes de recorrer el archivo entero.
     *
     * <p>Es para lo que impide seguir —el archivo no se puede leer, no trae la columna
     * del folio, se cayó la conexión a la base—, no para una fila que no se pudo
     * guardar: eso se anota como fila omitida y la carga continúa.</p>
     */
    FALLIDA
}
