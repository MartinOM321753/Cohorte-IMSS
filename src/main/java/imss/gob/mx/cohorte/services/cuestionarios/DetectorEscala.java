package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.secciones.PresentacionSeccion;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Descubre qué bloques se contestan en una tabla de escala: un renglón por
 * pregunta y una columna por opción.
 *
 * <h3>La señal es que comparten el catálogo</h3>
 *
 * <p>No hace falta adivinar nada del texto, al contrario que en
 * {@link DetectorRejilla}: cuando un bloque se contesta en tabla, todas sus
 * preguntas ofrecen exactamente las mismas respuestas, porque las respuestas son
 * los encabezados de las columnas. Eso se comprueba, no se infiere.</p>
 *
 * <h3>Los umbrales, y por qué son suaves</h3>
 *
 * <p>Aquí equivocarse cuesta poco: una tabla donde cabía una lista se lee igual de
 * bien, mientras que una {@link PresentacionSeccion#MATRIZ} inventada reacomoda las
 * preguntas y el capturista deja de encontrarlas. Por eso basta con tres preguntas
 * que compartan dos opciones, y se admite que alguna del bloque se salga —el «Otro,
 * especifique» que acompaña a casi todas estas tablas—.</p>
 *
 * <p>Solo cuentan las de opción única. Una casilla no tiene columnas que encabezar:
 * se marca o no se marca.</p>
 */
public final class DetectorEscala {

    private DetectorEscala() {}

    private static final int MIN_PREGUNTAS = 3;
    private static final int MIN_OPCIONES = 2;

    /** Qué parte del bloque tiene que compartir el mismo juego de opciones. */
    private static final double MIN_PARTE = 0.8;

    /**
     * @param opcionesPorPregunta los códigos de opción de cada pregunta de opción
     *                            única del bloque, en orden
     * @return las opciones que hacen de columnas, o una lista vacía si el bloque no
     *         es una tabla de escala
     */
    public static List<String> columnas(Collection<List<String>> opcionesPorPregunta) {
        if (opcionesPorPregunta.size() < MIN_PREGUNTAS) return List.of();

        Map<List<String>, Integer> cuantas = new HashMap<>();
        for (List<String> juego : opcionesPorPregunta) {
            if (juego == null || juego.isEmpty()) continue;
            cuantas.merge(juego, 1, Integer::sum);
        }

        List<String> mayoritario = null;
        int veces = 0;
        for (Map.Entry<List<String>, Integer> e : cuantas.entrySet()) {
            if (e.getValue() > veces) {
                mayoritario = e.getKey();
                veces = e.getValue();
            }
        }

        if (mayoritario == null || mayoritario.size() < MIN_OPCIONES) return List.of();
        if (veces < MIN_PREGUNTAS) return List.of();
        if (veces < opcionesPorPregunta.size() * MIN_PARTE) return List.of();
        return mayoritario;
    }
}
