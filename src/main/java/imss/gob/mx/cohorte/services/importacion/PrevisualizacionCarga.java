package imss.gob.mx.cohorte.services.importacion;

import java.util.List;

/**
 * Lo que se le enseña al usuario antes de guardar nada.
 *
 * <p>La carga masiva no escribe hasta que alguien mira esto y confirma. Por eso
 * el objetivo aqui no es decir "hay 12 errores", sino que cada problema quede
 * pegado a la celda que lo causa y en el numero de fila del archivo, para que se
 * corrija sin adivinar.</p>
 *
 * <p>Se distingue entre lo que detiene la carga y lo que solo se avisa: una
 * columna sobrante es un aviso, un parametro sin columna la detiene. Mezclarlos
 * haria que el usuario tratara de arreglar lo que no hacia falta.</p>
 */
public record PrevisualizacionCarga(
        /** Datos del tipo de estudio, para encabezar la pantalla. */
        Long idTipoEstudio,
        String nombreTipoEstudio,

        /** Problemas de estructura. Si trae algo, no se puede continuar. */
        List<String> problemasDeEstructura,

        /** Columnas del archivo que no corresponden a nada; se ignoraran. */
        List<String> columnasIgnoradas,

        /** Parametros que ningun encabezado reclamo; detienen la carga. */
        List<String> parametrosSinColumna,

        /** Como se decidio leer las fechas, y si hubo que suponerlo. */
        String ordenDeFecha,
        boolean fechaAmbigua,

        /** Las columnas que si se reconocieron, en el orden del archivo. */
        List<ColumnaReconocida> columnas,

        /** Una entrada por fila del archivo. */
        List<FilaPrevisualizada> filas,

        Resumen resumen
) {

    /** @param aliasUsado el alias que hizo la coincidencia, para poder explicarla */
    public record ColumnaReconocida(String encabezado, Long idParametro,
                                    String nombreParametro, String tipo, String aliasUsado) {}

    /**
     * @param numeroDeFila   el del archivo, para buscarlo en la hoja de calculo
     * @param folio          tal como venia
     * @param nombreParticipante  null si no se resolvio
     * @param errorParticipante   por que no se resolvio, si es el caso
     * @param fecha          ya normalizada, o null si no se entendio
     * @param errorFecha     por que no se entendio, si es el caso
     * @param valores        un valor por columna reconocida
     */
    public record FilaPrevisualizada(
            int numeroDeFila,
            String folio,
            String uuidParticipante,
            String nombreParticipante,
            String errorParticipante,
            String fecha,
            String errorFecha,
            List<ValorPrevisualizado> valores
    ) {
        public boolean tieneProblemas() {
            return errorParticipante != null || errorFecha != null
                    || valores.stream().anyMatch(v -> v.error() != null);
        }
    }

    /**
     * @param crudo el texto tal como venia, para poder enseñarlo al corregir
     * @param error null si se entendio
     */
    public record ValorPrevisualizado(Long idParametro, String crudo, String error) {}

    /**
     * @param filasConProblemas cuantas necesitan correccion antes de guardar
     */
    public record Resumen(int totalFilas, int filasListas, int filasConProblemas,
                          int columnasReconocidas, int columnasIgnoradas) {}

    /** Si no hay nada que corregir, la carga puede confirmarse tal cual. */
    public boolean puedeConfirmarse() {
        return problemasDeEstructura.isEmpty()
                && parametrosSinColumna.isEmpty()
                && resumen.filasConProblemas() == 0
                && resumen.totalFilas() > 0;
    }
}
