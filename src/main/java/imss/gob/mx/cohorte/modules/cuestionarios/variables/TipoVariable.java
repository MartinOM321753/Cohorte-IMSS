package imss.gob.mx.cohorte.modules.cuestionarios.variables;

/**
 * Qué clase de respuesta admite una variable del cuestionario.
 *
 * <p>No coincide con {@code TipoParametro} de estudios y no debería: un estudio
 * médico mide valores, un cuestionario registra selecciones. Lo que aquí se
 * guarda casi siempre es un <em>código</em> —un dígito que significa una opción—,
 * no una magnitud, y ese código solo tiene sentido contra el catálogo de opciones
 * de su variable.</p>
 *
 * <p>La distinción entre {@link #OPCION_UNICA}, {@link #CASILLA} y
 * {@link #OPCION_MULTIPLE} es la que más cuesta y la que más importa. En el papel
 * las tres se ven igual —círculos para rellenar— y el catálogo que vino de los
 * investigadores las nombra a todas «select one». En el volcado de Remark se
 * distinguen por la forma del dato, y de ahí salen las reglas de validación: una
 * opción única admite un código de su catálogo y nada más; una casilla admite
 * marcada o no marcada; un grupo de opción múltiple admite varias casillas
 * simultáneas y es el único donde una variable puede tener más de una respuesta.</p>
 */
public enum TipoVariable {

    /**
     * Una sola opción de un catálogo cerrado. El valor guardado es el código de la
     * opción elegida.
     */
    OPCION_UNICA,

    /**
     * Una casilla suelta: marcada o sin marcar. En el papel es un círculo con una
     * sola opción, normalmente «Sí», y la ausencia de marca es la otra mitad de la
     * respuesta —no un dato faltante—.
     *
     * <p>Es el caso más numeroso del instrumento principal y el que el catálogo
     * declaraba mal. De aquí viene que un «no» tenga que distinguirse de un «no
     * contestó», distinción que {@link imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia}
     * resuelve.</p>
     */
    CASILLA,

    /**
     * Pertenece a un grupo del que se pueden marcar varias a la vez. La variable
     * admite varias respuestas, numeradas con {@code ordenRespuesta}.
     */
    OPCION_MULTIPLE,

    /** Cantidad escrita a mano y luego burbujeada en una rejilla de dígitos. */
    NUMERO,

    /**
     * Número que además admite un código de escape por fuera del rango, del tipo
     * «99 = no sabe». El escape vive como opción de la variable; cualquier otro
     * valor se interpreta como la cantidad.
     */
    NUMERO_CON_ESCAPE,

    /** Texto manuscrito. Es lo que el reconocimiento de Azure devuelve o no. */
    TEXTO,

    /** Fecha en rejilla dd/mm/aaaa. */
    FECHA,

    /**
     * Encabezado de un conjunto de preguntas, sin casillas propias: no se contesta.
     *
     * <p>Existe porque el catálogo las trae como filas iguales a las demás —«té o
     * infusión», «salsas y aderezos»— y sin marcarlas el formulario pediría
     * respuesta a algo que en el papel no se puede contestar.</p>
     */
    GRUPO
}
