package imss.gob.mx.cohorte.modules.almacenamiento.caja;

/**
 * Cómo se nombra una posición dentro de una caja criogénica.
 *
 * <p>La fila y la columna se guardan como dos enteros 1..N; las letras son solo
 * la forma de leerlas. La convención es la de una placa de laboratorio —fila en
 * letra, columna en número: A1, B7, C12— que es como vienen rotuladas las cajas
 * físicas.
 *
 * <p>El cliente tiene su gemelo en {@code features/biobanco/lib/posicionCaja.ts};
 * los dos deben cambiar juntos si la convención cambia.
 */
public final class EtiquetaPosicionCaja {

    private EtiquetaPosicionCaja() {}

    /**
     * Letra de una fila 1-based: 1→A, 26→Z, 27→AA.
     *
     * <p>Se recorre en base 26 y no con un {@code (char)('A' + n - 1)} porque ese
     * atajo devuelve símbolos sueltos en cuanto una caja pasa de 26 filas.
     */
    public static String letraFila(Integer fila) {
        if (fila == null || fila < 1) return "";
        int n = fila;
        StringBuilder sb = new StringBuilder();
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + (n % 26)));
            n /= 26;
        }
        return sb.toString();
    }

    /** Etiqueta corta de una posición. Ej. {@code B7}. */
    public static String etiqueta(Integer fila, Integer columna) {
        if (fila == null || columna == null) return "";
        return letraFila(fila) + columna;
    }
}
