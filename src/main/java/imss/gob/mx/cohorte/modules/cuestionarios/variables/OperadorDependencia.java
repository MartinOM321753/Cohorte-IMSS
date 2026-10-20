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
    EN;

    public String etiqueta() {
        return switch (this) {
            case IGUAL -> "es igual a";
            case DISTINTO -> "es distinto de";
            case EN -> "es alguno de";
        };
    }
}
