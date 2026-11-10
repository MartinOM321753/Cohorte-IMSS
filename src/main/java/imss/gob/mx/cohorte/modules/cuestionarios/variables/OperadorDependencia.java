package imss.gob.mx.cohorte.modules.cuestionarios.variables;

/**
 * Cómo se compara la respuesta de la pregunta llave con el valor que habilita.
 *
 * <h3>Por qué no basta con la igualdad</h3>
 *
 * <p>El papel tiene saltos de las dos formas. Unos son «si contestó Sí, pase a la
 * 25», que es una igualdad. Otros son una casilla de descarte: la pregunta 112
 * empieza con un círculo «Ninguna» y, si está marcado, las otras ciento treinta y
 * cinco no se le preguntaron. Eso no se puede decir con una igualdad —habría que
 * enumerar todos los valores que no son «Ninguna», y «sin contestar» no es
 * ninguno de ellos—, así que se dice al revés.</p>
 *
 * <p>Son los mismos saltos que el papel imprime; también están en «108 → No, pase
 * a la 112» y en el «No trabajo» de la 114.</p>
 */
public enum OperadorDependencia {

    /** Se pregunta cuando la llave vale exactamente el valor habilitante. */
    IGUAL,

    /**
     * Se pregunta cuando la llave vale cualquier cosa <b>menos</b> el valor
     * habilitante. La llave sin contestar sigue habilitando, igual que con
     * {@link #IGUAL}: ver {@code EvaluadorDependencias}.
     */
    DISTINTO,

    /**
     * Se pregunta cuando la llave vale <b>alguno</b> de varios valores, separados
     * por {@code |}.
     *
     * <p>Hace falta porque una misma pregunta puede tener dos saltos distintos. La
     * 30 del cuestionario general tiene tres respuestas y dos flechas: «nunca he
     * fumado» salta hasta la 50 y «ya no fumo» salta a la 42, así que el tramo de
     * la 42 a la 49 es para quien contestó lo segundo <em>o</em> lo tercero. Con una
     * igualdad habría que partir la pregunta en dos.</p>
     */
    EN,

    /**
     * Se pregunta cuando la llave <b>no tiene respuesta</b>. No compara contra ningún
     * valor: no necesita {@code valorHabilitante}, y la llave puede ser de cualquier
     * tipo, texto libre incluido.
     *
     * <p>Existe por el salto de la 191: «Si usted no trabaja, ¿a qué se dedica?
     * (Especifique y pase a la…)». Es un recuadro de escritura libre, sin catálogo de
     * opciones, así que ninguno de los operadores de arriba lo puede decir; y la
     * condición no es una comparación sino «está vacío». Quien trabaja lo deja en
     * blanco y contesta las 62 preguntas siguientes; quien lo llena se las salta.</p>
     *
     * <p><b>Es la excepción a la regla de «habilita cuando no sabe».</b> Con los demás
     * operadores, una llave vacía habilita; aquí una llave vacía es justo la condición
     * que se evalúa. Ver {@code EvaluadorDependencias}.</p>
     *
     * <p>Ojo con el nombre: {@code MarcaAusencia.SIN_RESPUESTA} es otra cosa, aunque
     * emparentada —la marca de una celda que no trajo respuesta—. Aquí «no tiene
     * respuesta» abarca cualquier ausencia: celda en blanco, casilla sin marcar o marca
     * de no autorizado. Se evita «vacío» porque {@code MarcaAusencia.EN_BLANCO} ya lo
     * usa con otro sentido más estrecho.</p>
     */
    SIN_RESPUESTA,

    /**
     * Lo contrario: se pregunta cuando la llave <b>sí tiene respuesta</b>, sea la que
     * sea. Sirve para la rama que cuelga de «especifique» — «si contestó otro, ¿cuál?»—
     * cuando la llave es un recuadro de texto y no una casilla.
     *
     * <p>Igual que {@link #SIN_RESPUESTA}, no necesita {@code valorHabilitante} y manda
     * sobre la regla de «habilita cuando no sabe»: con la llave vacía <b>no</b>
     * pregunta.</p>
     */
    CON_RESPUESTA;

    /** Los dos que miran si hay respuesta y no cuál es: no llevan valor habilitante. */
    public boolean miraSiHayRespuesta() {
        return this == SIN_RESPUESTA || this == CON_RESPUESTA;
    }

    public String etiqueta() {
        return switch (this) {
            case IGUAL -> "es igual a";
            case DISTINTO -> "es distinto de";
            case EN -> "es alguno de";
            case SIN_RESPUESTA -> "no tiene respuesta";
            case CON_RESPUESTA -> "tiene respuesta";
        };
    }
}
