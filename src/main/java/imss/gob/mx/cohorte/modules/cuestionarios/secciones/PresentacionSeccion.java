package imss.gob.mx.cohorte.modules.cuestionarios.secciones;

/**
 * Cómo se pinta el bloque en pantalla.
 *
 * <h3>Por qué una lista no siempre sirve</h3>
 *
 * <p>Hay bloques que en el papel son una rejilla: la pregunta 112 son quince
 * actividades por nueve columnas —siete días, el tiempo y la intensidad—, y la 146
 * o la 183 son del mismo corte. Aplanados a una lista salen ciento treinta y siete
 * campos en fila india, «Caminar Lunes», «Caminar Martes», «Caminar Miércoles»…
 * Para capturar una hoja ya contestada es incómodo; para que el participante
 * conteste la suya es peor, porque <b>deja de reconocer su propia tabla</b> y tiene
 * que reconstruir mentalmente a qué fila pertenece cada campo.</p>
 *
 * <p>La rejilla no cambia qué se guarda —sigue siendo una respuesta por pregunta—,
 * solo cómo se muestra.</p>
 */
public enum PresentacionSeccion {

    /** Una pregunta debajo de otra. Lo que sirve para la gran mayoría. */
    LISTA,

    /**
     * Rejilla: las preguntas se acomodan por {@code filaMatriz} y
     * {@code columnaMatriz}. Si a una pregunta le falta cualquiera de los dos, se
     * pinta aparte, debajo de la rejilla: es lo que pasa con el «Ninguna» y el
     * «Otro, especifique» de la 112, que no pertenecen a ninguna celda.
     */
    MATRIZ,

    /**
     * Tabla de escala: <b>un renglón por pregunta y una columna por opción</b>.
     *
     * <h3>En qué se diferencia de la rejilla</h3>
     *
     * <p>En una {@link #MATRIZ} cada celda es una pregunta distinta. Aquí no: cada
     * <em>renglón</em> es la pregunta y las columnas son sus posibles respuestas,
     * las mismas para todo el bloque. En el papel las dos se ven igual, por dentro
     * no se parecen.</p>
     *
     * <h3>Por qué hace falta</h3>
     *
     * <p>Es la forma más común del instrumento: la frecuencia alimentaria, el AGES,
     * la 183, la 205, la del índice de Barthel… Son casi mil preguntas. En lista,
     * cada una repetiría sus nueve o diez opciones una debajo de otra, y contestar
     * veintidós alimentos así es ilegible: el participante pierde la referencia de
     * qué escala está usando en cuanto pasa del tercer renglón.</p>
     */
    ESCALA
}
