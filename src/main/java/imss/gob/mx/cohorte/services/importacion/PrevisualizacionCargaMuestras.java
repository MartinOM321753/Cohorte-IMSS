package imss.gob.mx.cohorte.services.importacion;

import java.util.List;
import java.util.Map;

/**
 * Lo que se le enseña al usuario antes de escribir una sola muestra.
 *
 * <p>Mismo criterio que la carga de estudios: el objetivo no es decir «hay 12
 * errores», sino que cada problema quede pegado a la celda que lo causa y al
 * número de fila del archivo, para que se corrija sin adivinar.</p>
 *
 * <p>Se distingue entre lo que <b>detiene</b> la carga y lo que solo se
 * <b>avisa</b>. Un hueco ya ocupado la detiene; un volumen por encima del
 * nominal del tubo solo se avisa, porque el vial ya existe con lo que tenga y el
 * catálogo no puede desmentir un hecho consumado. Mezclarlos haría que el
 * usuario tratara de arreglar lo que no hace falta.</p>
 *
 * <p>Aparte de las filas viaja la lista de <b>lotes</b>, que es lo que de verdad
 * se va a crear: una muestra padre por cada grupo de folio, tipo, tubo y día.
 * Sin ella la pantalla solo podría decir «619 viales» y nadie vería que además
 * nacen 101 registros de tubo padre.</p>
 */
public record PrevisualizacionCargaMuestras(

        /** Problemas de estructura. Si trae algo, no se puede continuar. */
        List<String> problemasDeEstructura,

        /** Encabezados que no corresponden a nada; se ignorarán al guardar. */
        List<String> columnasIgnoradas,

        /** Cómo se decidió leer las fechas, y si hubo que suponerlo. */
        String ordenDeFecha,
        boolean fechaAmbigua,

        /** Si el archivo trae columna de fecha. Si no, se pide una para todo. */
        boolean traeColumnaFecha,

        /**
         * La tabla tal como se leyó, para que la pantalla pueda editarla y
         * devolverla a revalidar sin obligar a subir el archivo otra vez.
         */
        TablaLeida tabla,

        /**
         * Dónde está cada columna dentro de la tabla, por el nombre de
         * {@link ColumnaMuestra}. Las que el archivo no trae no aparecen.
         */
        Map<String, Integer> indices,

        /** Una entrada por fila del archivo. */
        List<FilaPrevisualizada> filas,

        /** Los lotes que se crearían: una muestra padre cada uno. */
        List<LotePrevisualizado> lotes,

        Resumen resumen
) {

    /**
     * Un problema atado a una celda.
     *
     * @param campo nombre de {@link ColumnaMuestra}, o null si es de la fila
     *              entera y no de una columna concreta
     */
    public record Problema(String campo, String mensaje) {}

    /**
     * Una fila ya interpretada: a qué participante, tipo, tubo y hueco apunta.
     *
     * @param volumenHeredado true cuando la celda venía vacía y se tomó el
     *                        nominal del tubo. Se marca para que la pantalla
     *                        pueda decir cuántos viales se van a guardar con un
     *                        volumen que nadie escribió.
     * @param etiquetaPrevista la que llevará el vial. Se calcula aquí porque es
     *                        lo único que se puede comparar con lo que hay
     *                        pegado en la caja.
     */
    public record FilaPrevisualizada(
            int numeroDeFila,
            String folio,
            String uuidParticipante,
            String nombreParticipante,
            String tipoMuestra,
            Long idTipoMuestra,
            String tubo,
            Long idTuboMuestra,
            String fecha,
            Integer numeroAlicuota,
            Double volumen,
            String unidad,
            boolean volumenHeredado,
            String codigoCaja,
            String posicion,
            Long idPosicionCaja,
            String etiquetaPrevista,
            /** A qué lote pertenece; la misma clave que en {@link LotePrevisualizado}. */
            String claveLote,
            List<Problema> errores,
            List<Problema> avisos
    ) {
        public boolean tieneProblemas() {
            return errores != null && !errores.isEmpty();
        }

        public boolean tieneAvisos() {
            return avisos != null && !avisos.isEmpty();
        }
    }

    /**
     * Un lote: la muestra padre que se creará y los viales que cuelgan de ella.
     *
     * @param configuradas   alícuotas que el tubo define, que es el denominador
     *                       de la etiqueta
     * @param quedaraAgotada si la padre nace sin volumen porque todos sus viales
     *                       llegan con posición. Si alguno viene sin ubicar, su
     *                       volumen se queda reservado y la padre no se agota.
     */
    public record LotePrevisualizado(
            String clave,
            String folio,
            String nombreParticipante,
            String tipoMuestra,
            String tubo,
            String fecha,
            int viales,
            int configuradas,
            Double volumenTotal,
            String unidad,
            String etiquetaPadre,
            boolean quedaraAgotada,
            List<String> avisos
    ) {}

    /**
     * @param filasConProblemas cuántas necesitan corrección antes de guardar
     * @param filasConAvisos    cuántas se guardarían tal cual pero conviene mirar
     * @param filasSinHora      cuántas traían día pero no hora. Va en el resumen
     *                          y no como aviso por fila a propósito: una hoja de
     *                          cálculo no trae hora en ninguna, así que marcarlas
     *                          una a una pintaría de ámbar el archivo entero y
     *                          enterraría los avisos que sí señalan algo raro
     */
    public record Resumen(int totalFilas, int filasListas, int filasConProblemas,
                          int filasConAvisos, int lotes,
                          int vialesConPosicion, int vialesSinPosicion,
                          int volumenesHeredados, int filasSinHora,
                          int columnasIgnoradas) {}

    /** Si no hay nada que corregir, la carga puede confirmarse tal cual. */
    public boolean puedeConfirmarse() {
        return problemasDeEstructura.isEmpty()
                && resumen.filasConProblemas() == 0
                && resumen.totalFilas() > 0;
    }
}
