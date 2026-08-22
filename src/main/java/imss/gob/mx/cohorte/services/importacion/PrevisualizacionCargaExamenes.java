package imss.gob.mx.cohorte.services.importacion;

import java.util.List;

/**
 * Lo que se le enseña al usuario antes de guardar resultados de laboratorio.
 *
 * <h3>Por que no reutiliza la de estudios</h3>
 *
 * <p>La unidad de cuenta es distinta. En un estudio, una fila del archivo es un
 * estudio con todos sus parametros dentro: o entra entero o no entra. En
 * laboratorio, una fila con cinco columnas de examen son cinco resultados
 * independientes, y que la glucosa venga mal no tiene por que impedir guardar el
 * colesterol de esa misma fila.</p>
 *
 * <p>Por eso aqui se cuentan resultados y no filas: decir "2 filas listas" cuando
 * cada fila trae cinco exámenes no le dice a nadie cuanto se va a escribir.</p>
 */
public record PrevisualizacionCargaExamenes(

        /** Problemas de estructura. Si trae algo, no se puede continuar. */
        List<String> problemasDeEstructura,

        /** Columnas del archivo que no corresponden a ningun examen; se ignoraran. */
        List<String> columnasIgnoradas,

        /**
         * Examenes del catalogo que este archivo no trae.
         *
         * <p>Es informativo, no un error: un perfil de lipidos no incluye la
         * glucosa y eso es normal. Se enseña para que se note si falta uno que
         * si deberia venir.</p>
         */
        List<String> examenesNoIncluidos,

        String ordenDeFecha,
        boolean fechaAmbigua,

        List<ColumnaExamen> columnas,

        /** La tabla tal como se leyo, para poder editarla y devolverla. */
        TablaLeida tabla,
        int indiceFolio,
        int indiceFecha,

        List<FilaExamenes> filas,

        Resumen resumen
) {

    public record ColumnaExamen(int indice, String encabezado, Long idExamen,
                                String nombreExamen, String unidad, String aliasUsado) {}

    /**
     * @param numeroDeFila el del archivo, para buscarlo en la hoja de calculo
     * @param valores      uno por columna de examen reconocida
     */
    public record FilaExamenes(
            int numeroDeFila,
            String folio,
            String uuidParticipante,
            String nombreParticipante,
            String errorParticipante,
            String fecha,
            String errorFecha,
            List<ValorExamen> valores
    ) {
        /** La fila entera es inservible: sin participante o sin fecha no se guarda nada de ella. */
        public boolean inservible() {
            return errorParticipante != null || errorFecha != null;
        }
    }

    /**
     * @param vacio true si la celda venia en blanco. No es un error: un archivo de
     *              laboratorio deja huecos cuando ese examen no se hizo. Se omite
     *              en silencio, y por eso se distingue de una celda con un valor
     *              que no se entiende.
     * @param idResultadoExistente resultado ya registrado para ese participante,
     *              examen y dia; null si no hay
     */
    public record ValorExamen(Long idExamen, String crudo, boolean vacio,
                              String error, Long idResultadoExistente) {}

    /**
     * Se cuentan resultados, no filas, porque es lo que se va a escribir.
     *
     * @param resultadosListos    los que se guardarian ahora mismo
     * @param resultadosConError  celdas con un valor que no se entiende
     * @param celdasVacias        huecos que se omitiran sin avisar
     * @param resultadosDuplicados los que chocan con uno ya registrado
     */
    public record Resumen(int totalFilas, int filasInservibles,
                          int resultadosListos, int resultadosConError,
                          int celdasVacias, int resultadosDuplicados,
                          int columnasReconocidas, int columnasIgnoradas) {}

    public boolean puedeConfirmarse() {
        return problemasDeEstructura.isEmpty()
                && resumen.filasInservibles() == 0
                && resumen.resultadosConError() == 0
                && resumen.resultadosListos() + resumen.resultadosDuplicados() > 0;
    }
}
