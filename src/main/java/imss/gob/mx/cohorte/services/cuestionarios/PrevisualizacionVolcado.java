package imss.gob.mx.cohorte.services.cuestionarios;

import java.util.List;

/**
 * Lo que se le muestra al usuario antes de confirmar la carga de un volcado.
 *
 * <h3>Por qué hay una previsualización y no un botón de cargar</h3>
 *
 * <p>Un volcado trae miles de celdas por participante y se carga una sola vez; el
 * archivo original después se pierde. Si la carga falla a la mitad, o acierta en lo
 * que no debía, no hay forma de volver atrás sin el archivo. La previsualización
 * hace el trabajo completo —leer, emparejar, interpretar— sin escribir nada, y
 * entrega el recuento para que la decisión de guardar la tome una persona que ya
 * sabe qué va a pasar.</p>
 *
 * <h3>Lo que no trae</h3>
 *
 * <p>No trae las respuestas. Serían dos mil por fila y la pantalla no las puede
 * mostrar ni el navegador recibirlas. Trae por fila qué participante es, si se
 * encontró, y cuántas celdas caen en cada categoría; y aparte, la lista de problemas
 * recortada. Lo que se decide con esto es cargar o no cargar, y para eso el recuento
 * basta.</p>
 *
 * @param claveDetectada     clave(s) de instrumento leída(s) de las etiquetas del archivo,
 *                           o null si la columna solo trae el folio. No decide nada: el
 *                           instrumento es el que se seleccionó al cargar
 * @param avisoEtiqueta      lo que hay que revisar del rotulado —otra clave, o varias—,
 *                           o null si no hay nada que decir. <b>No impide la carga</b>
 * @param nombreInstrumento  instrumento contra el que se va a cargar
 * @param hojaRespuestas     nombre de la pestaña de la que se leyeron las respuestas
 * @param hojaDiscrepancias  nombre de la pestaña de lecturas en desacuerdo, si la hay
 * @param totalColumnas      columnas que trae el archivo
 * @param columnasEmparejadas columnas que corresponden a una variable del catálogo
 * @param columnasSinVariable encabezados que el catálogo no reconoce
 * @param variablesSinColumna variables del catálogo que el archivo no trae
 * @param filas              una entrada por participante del archivo
 * @param problemas          celdas que no encajan con su tipo o su catálogo, recortadas
 * @param problemasOmitidos  cuántos problemas más hay que no se listan
 * @param discrepancias      cuántas lecturas en desacuerdo trae el archivo
 */
public record PrevisualizacionVolcado(
        String claveDetectada,
        String avisoEtiqueta,
        String nombreInstrumento,
        String hojaRespuestas,
        String hojaDiscrepancias,
        int totalColumnas,
        int columnasEmparejadas,
        List<String> columnasSinVariable,
        List<String> variablesSinColumna,
        List<FilaPrevia> filas,
        List<ProblemaCelda> problemas,
        int problemasOmitidos,
        int discrepancias
) {

    /**
     * Un participante del archivo.
     *
     * @param numeroFila      fila del archivo, para poder señalarla en la hoja
     * @param etiqueta        lo que decía la columna: la etiqueta completa o el folio
     *                        pelado, que las dos formas llegan
     * @param folio           folio extraído, que es lo que identifica al participante
     * @param nombreParticipante nombre del participante encontrado, o null
     * @param motivoRechazo   por qué esta fila no se va a cargar, o null si se carga
     * @param yaExiste        si el participante ya tiene este instrumento registrado
     * @param conValor        celdas con respuesta utilizable
     * @param sinRespuesta    celdas que el participante dejó sin marcar
     * @param noAutorizadas   celdas que el reconocimiento de escritura no leyó
     * @param conError        celdas con error de lectura
     * @param noPreguntadas   celdas de preguntas que la llave dejó fuera
     * @param avisoCriterio   por qué el participante no entraba en el público del
     *                        instrumento, o null. <b>No impide la carga</b>: la hoja existe
     *                        y alguien la contestó. Es un hecho de calidad de datos que
     *                        hay que ver antes de guardar, no un motivo para descartar.
     * @param edad            años cumplidos del participante el día de la aplicación
     * @param avisosDeRegla   lo que incumple alguna regla de la plantilla. <b>Tampoco
     *                        impide la carga</b>: la hoja ya se contestó en papel y el
     *                        dato existe aunque incumpla. Tirarlo sería perder la
     *                        respuesta y, con ella, la señal de que la aplicación del
     *                        cuestionario salió mal
     */
    public record FilaPrevia(
            int numeroFila,
            String etiqueta,
            String folio,
            String nombreParticipante,
            String motivoRechazo,
            boolean yaExiste,
            int conValor,
            int sinRespuesta,
            int noAutorizadas,
            int conError,
            int noPreguntadas,
            String avisoCriterio,
            Integer edad,
            List<String> avisosDeRegla
    ) {
        public boolean seCarga() {
            return motivoRechazo == null;
        }
    }

    /** Una celda que no encaja. {@code celda} es la referencia de la hoja ({@code G6}). */
    public record ProblemaCelda(int numeroFila, String celda, String variable, String detalle) {}

    public int filasQueSeCargan() {
        return (int) filas.stream().filter(FilaPrevia::seCarga).count();
    }

    public int filasRechazadas() {
        return filas.size() - filasQueSeCargan();
    }
}
