package imss.gob.mx.cohorte.services.importacion;

/**
 * Topes de la lectura de archivos.
 *
 * <p>Estan juntos y con nombre para poder discutirlos: son decisiones de
 * operacion, no detalles de implementacion, y el dia que un instrumento exporte
 * mas de lo previsto hay que poder subirlos en un solo sitio.</p>
 */
public final class LimitesArchivo {

    private LimitesArchivo() {}

    /** 10 MB. Un export de instrumento son kilobytes; esto ya es holgado. */
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    /**
     * El tope de filas es independiente del de bytes y hace falta: un CSV de 2 MB
     * puede traer medio millon de filas, y cada una se convierte en un estudio con
     * sus resultados dentro de una sola transaccion.
     */
    public static final int MAX_FILAS = 5_000;

    /** Mas columnas que esto no es una tabla de resultados, es otra cosa. */
    public static final int MAX_COLUMNAS = 200;

    /** Ninguna celda legitima de un instrumento se acerca a esto. */
    public static final int MAX_CARACTERES_CELDA = 1_000;

    /**
     * Relacion minima entre lo comprimido y lo descomprimido en un XLSX.
     *
     * <p>Un .xlsx es un ZIP con XML dentro. Con la configuracion por omision, un
     * archivo de un megabyte puede expandirse hasta agotar la memoria del
     * servidor: es la bomba ZIP de toda la vida. POI permite exigir que el
     * contenido no se expanda mas de cierto factor.</p>
     *
     * <p>0.005 equivale a un maximo de 200 a 1. El XML de una hoja de calculo
     * comprime bien, pero no tanto, asi que un archivo legitimo queda muy por
     * encima de ese umbral.</p>
     */
    public static final double RATIO_MINIMO_INFLADO = 0.005d;

    /** Tope de lo que puede pesar una sola entrada del ZIP ya descomprimida. */
    public static final long MAX_BYTES_ENTRADA_ZIP = 64L * 1024 * 1024;
}
