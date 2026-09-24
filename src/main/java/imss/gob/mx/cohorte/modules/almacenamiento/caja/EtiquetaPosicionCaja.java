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

    /** Fila y columna, 1-based, tal como se guardan. */
    public record Coordenada(int fila, int columna) {}

    /**
     * Lee una etiqueta escrita a mano y la convierte en fila y columna.
     *
     * <p>Es la inversa de {@link #etiqueta}, y existe porque la carga masiva
     * recibe el hueco como lo escribe una persona —{@code A1}, {@code b7},
     * {@code AA 12}— mientras que la base guarda dos enteros. Vive aquí, junto a
     * la ida, para que las dos direcciones de la misma convención no puedan
     * separarse.</p>
     *
     * @return null si el texto no tiene la forma letra(s) + número, o si alguno
     *         de los dos es cero. Devolver null en vez de lanzar deja que quien
     *         llama redacte el error con el contexto de su fila.
     */
    public static Coordenada parsear(String texto) {
        if (texto == null) return null;
        String limpio = texto.trim().replace(" ", "").toUpperCase(java.util.Locale.ROOT);
        if (limpio.isEmpty()) return null;

        int corte = 0;
        while (corte < limpio.length() && Character.isLetter(limpio.charAt(corte))) {
            corte++;
        }
        if (corte == 0 || corte == limpio.length()) return null;

        String letras = limpio.substring(0, corte);
        String digitos = limpio.substring(corte);
        for (int i = 0; i < letras.length(); i++) {
            if (letras.charAt(i) < 'A' || letras.charAt(i) > 'Z') return null;
        }
        for (int i = 0; i < digitos.length(); i++) {
            if (!Character.isDigit(digitos.charAt(i))) return null;
        }

        int fila = filaDeLetras(letras);
        int columna;
        try {
            columna = Integer.parseInt(digitos);
        } catch (NumberFormatException e) {
            // Más dígitos de los que caben en un int no es una caja, es un error
            // de tecleo; el llamador lo dirá con su número de fila.
            return null;
        }
        if (fila < 1 || columna < 1) return null;
        return new Coordenada(fila, columna);
    }

    /** Número de fila de una o más letras: A→1, Z→26, AA→27. */
    public static int filaDeLetras(String letras) {
        int n = 0;
        for (int i = 0; i < letras.length(); i++) {
            n = n * 26 + (letras.charAt(i) - 'A' + 1);
        }
        return n;
    }
}
