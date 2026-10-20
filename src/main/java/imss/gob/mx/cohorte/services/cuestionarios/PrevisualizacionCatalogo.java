package imss.gob.mx.cohorte.services.cuestionarios;

import java.util.List;

/**
 * Lo que va a pasar con el catálogo si se confirma la carga.
 *
 * <p>Tiene el mismo motivo que la previsualización del volcado y uno más: aquí no se
 * cargan datos de participantes, se cambia el <em>significado</em> de los que ya hay.
 * Retirar una opción o cambiar un tipo reinterpreta respuestas guardadas, así que lo
 * que hay que poder ver antes de aceptar es justamente la lista de choques.</p>
 *
 * @param nombreCatalogo   el catálogo del archivo que se va a cargar
 * @param variablesNuevas  preguntas que no están en el catálogo actual
 * @param variablesActualizadas preguntas que cambian de texto, tipo u opciones
 * @param variablesSinCambio preguntas idénticas a lo que ya hay
 * @param variablesQueSobran preguntas del catálogo actual que el archivo no trae
 * @param seccionesNuevas  bloques que se van a crear
 * @param opciones         total de opciones que quedarán registradas
 * @param choques          cambios que afectan a respuestas ya guardadas
 */
public record PrevisualizacionCatalogo(
        String nombreCatalogo,
        int variablesNuevas,
        int variablesActualizadas,
        int variablesSinCambio,
        List<String> variablesQueSobran,
        List<String> seccionesNuevas,
        int opciones,
        List<Choque> choques
) {
    /**
     * Un cambio que no es inocuo.
     *
     * @param variable código de la pregunta
     * @param detalle  qué cambia y a qué respuestas afecta
     * @param bloquea  si impide la carga. Un choque que bloquea no se puede aceptar
     *                 desde la pantalla: tiene que resolverse en el archivo o en los
     *                 datos, porque aplicarlo dejaría respuestas ilegibles.
     */
    public record Choque(String variable, String detalle, boolean bloquea) {}

    public boolean tieneBloqueos() {
        return choques.stream().anyMatch(Choque::bloquea);
    }
}
