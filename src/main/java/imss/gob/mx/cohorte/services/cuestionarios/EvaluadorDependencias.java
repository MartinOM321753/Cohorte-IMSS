package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.variables.OperadorDependencia;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Decide si una pregunta se le hace al participante, dadas las respuestas que ya
 * tiene.
 *
 * <p>Es el «si contestó no, pase a la pregunta 25» del papel. En pantalla sirve para
 * no preguntar lo que no toca; al importar sirve para algo más importante: distinguir
 * una pregunta sin contestar de una que no se preguntó. Sin esta distinción, un
 * participante que no fuma aparecería con decenas de respuestas faltantes en el bloque
 * de tabaco, y cualquier medida de «qué tan completo está el cuestionario» quedaría
 * inservible.</p>
 *
 * <h3>Lo que hace cuando no sabe</h3>
 *
 * <p>Si la llave no tiene respuesta todavía, la dependiente se considera
 * <b>habilitada</b>. Es deliberado y es el lado seguro: tratarla como deshabilitada
 * escondería preguntas —y, al importar, descartaría respuestas que el archivo sí
 * trae— por el único motivo de que la llave venía vacía. Una pregunta de más que se
 * muestra se puede dejar en blanco; una respuesta que se descartó no vuelve.</p>
 *
 * <p>Una llave que apunta a una pregunta que no existe se trata igual. Es un defecto
 * del catálogo, y la forma de que se note es que la pregunta aparezca, no que
 * desaparezca en silencio.</p>
 *
 * <h3>Las tres formas de la condición</h3>
 *
 * <p>{@link OperadorDependencia#IGUAL} pregunta cuando la llave vale lo esperado;
 * {@link OperadorDependencia#DISTINTO}, cuando vale cualquier otra cosa —es la casilla
 * de descarte, el «Ninguna» de la 112—; y {@link OperadorDependencia#EN}, cuando vale
 * alguno de varios, que es lo que hace falta cuando una pregunta tiene dos saltos
 * distintos.</p>
 *
 * <p><b>La regla de «habilita cuando no sabe» manda sobre las tres.</b> Con la llave
 * sin contestar se pregunta igual, sea cual sea el operador: la negación es sobre el
 * valor contestado, no sobre la ausencia de respuesta.</p>
 *
 * <h3>La excepción: los operadores que miran si hay respuesta</h3>
 *
 * <p>{@link OperadorDependencia#SIN_RESPUESTA} y {@link OperadorDependencia#CON_RESPUESTA}
 * son lo contrario de lo anterior: la ausencia de respuesta <em>es</em> la condición.
 * Por eso se resuelven antes de esa regla y están exentos de ella. Son los del salto de
 * la 191 —«si no trabaja, escriba a qué se dedica»—, donde la llave es un recuadro de
 * texto y la condición es «está vacío».</p>
 *
 * <p>La exención tiene un costo que hay que tener presente al importar: una llave que
 * viene vacía en el volcado habilita sus dependientes (la persona trabaja, o nadie
 * llenó el recuadro: en los dos casos se conservan las respuestas), pero una llave
 * llena las deshabilita aunque el archivo traiga datos en ellas. Ver
 * {@code CargaVolcadoService}, que cuenta esos casos.</p>
 *
 * <p>El mapa de respuestas trae lo que cuenta como respuesta —código, texto, número o
 * fecha—; ver {@link ValorContestado}.</p>
 *
 * <h3>Y las cadenas</h3>
 *
 * <p>Los saltos se encadenan: en el cuestionario general, la 34 depende de la 33, que
 * depende de la 30. Si quien nunca fumó no contestó la 33, mirar solo la llave
 * inmediata daría la 34 por habilitada —la llave está vacía, así que «no sabemos»— y
 * se le preguntaría a alguien que ya dijo que no fuma.</p>
 *
 * <p>Por eso, antes de comparar, se comprueba que la llave <b>sí se haya preguntado</b>.
 * Una llave que quedó fuera por su propio salto deshabilita a las suyas. Nótese la
 * diferencia con el caso de arriba: una llave <em>sin contestar</em> habilita; una
 * llave <em>que no se preguntó</em>, no.</p>
 */
public final class EvaluadorDependencias {

    private EvaluadorDependencias() {}

    /**
     * Sin el catálogo a mano no se pueden seguir las cadenas, así que solo se mira la
     * llave inmediata. Sirve donde el que llama ya filtró el bloque.
     */
    public static boolean habilitada(VariableCuestionario variable,
                                     Map<String, String> respuestasPorCodigo) {
        return habilitada(variable, respuestasPorCodigo, Map.of());
    }

    /**
     * @param respuestasPorCodigo el código contestado de cada variable, por su código
     * @param variablesPorCodigo  el catálogo, para poder seguir la cadena de llaves.
     *                            Puede venir vacío.
     */
    public static boolean habilitada(VariableCuestionario variable,
                                     Map<String, String> respuestasPorCodigo,
                                     Map<String, VariableCuestionario> variablesPorCodigo) {
        return habilitada(variable, respuestasPorCodigo, variablesPorCodigo, new HashSet<>());
    }

    private static boolean habilitada(VariableCuestionario variable,
                                      Map<String, String> respuestas,
                                      Map<String, VariableCuestionario> variables,
                                      Set<String> enCurso) {
        String llave = variable.getDependeDe();
        if (llave == null || llave.isBlank()) return true;

        OperadorDependencia operador = variable.getOperadorDependencia() == null
                ? OperadorDependencia.IGUAL
                : variable.getOperadorDependencia();

        String esperado = variable.getValorHabilitante();
        // Los operadores que miran si hay respuesta no comparan contra nada, así que no
        // llevan valor: exigírselo los daría por incompletos y los habilitaría siempre.
        if (!operador.miraSiHayRespuesta() && (esperado == null || esperado.isBlank())) {
            // Depende de algo pero no se dijo de qué valor. El catálogo está
            // incompleto; se habilita, por el mismo motivo que arriba.
            return true;
        }

        // Un catálogo con una llave que apunta a sí misma, o un anillo de llaves, no
        // puede esconder preguntas: se corta el recorrido y se habilita.
        if (!enCurso.add(variable.getCodigo())) return true;

        VariableCuestionario laLlave = variables.get(llave);
        if (laLlave != null && !habilitada(laLlave, respuestas, variables, enCurso)) {
            return false;   // a la llave no se le pregunto; a esta tampoco le toca
        }

        if (operador.miraSiHayRespuesta()) {
            // Se resuelve ANTES de la regla de «habilita cuando no sabe», no dentro del
            // switch de abajo: esa regla devuelve true con la llave vacía, y la llave
            // vacía es justo el caso que a estos dos les interesa. Metidos en el switch
            // no se ejecutarían nunca.
            //
            // Una llave que el catálogo no conoce es un defecto del catálogo y se trata
            // como siempre: la pregunta aparece, para que el defecto se note. Solo se
            // puede saber con el catálogo a mano.
            if (laLlave == null && !variables.isEmpty()) return true;

            String c = respuestas.get(llave);
            boolean hayRespuesta = c != null && !c.isBlank();
            return operador == OperadorDependencia.SIN_RESPUESTA ? !hayRespuesta : hayRespuesta;
        }

        if (!respuestas.containsKey(llave)) return true;   // llave inexistente

        String contestado = respuestas.get(llave);
        if (contestado == null || contestado.isBlank()) return true;   // sin contestar

        return switch (operador) {
            case IGUAL -> esperado.equals(contestado);
            case DISTINTO -> !esperado.equals(contestado);
            case EN -> estaEntre(contestado, esperado);
            // Ya resueltos arriba; el switch tiene que ser exhaustivo.
            case SIN_RESPUESTA, CON_RESPUESTA -> throw new IllegalStateException(
                    "Operador resuelto antes del switch: " + operador);
        };
    }

    /** {@code "2|3"} contiene a {@code "3"}. Se compara entero, no por substring. */
    private static boolean estaEntre(String contestado, String valores) {
        for (String valor : valores.split("\\|")) {
            if (valor.trim().equals(contestado)) return true;
        }
        return false;
    }
}
