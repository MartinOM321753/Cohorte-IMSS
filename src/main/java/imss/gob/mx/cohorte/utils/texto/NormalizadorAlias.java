package imss.gob.mx.cohorte.utils.texto;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Lleva un encabezado de archivo o un alias configurado a una forma canónica
 * para poder compararlos.
 *
 * <p>Existe porque el mismo dato sale escrito de formas distintas según el
 * instrumento: {@code Sistólica}, {@code SISTOLICA}, {@code "Sistolica "} y
 * {@code Sistolica} son la misma columna, y compararlas en crudo haría fallar
 * lecturas correctas.</p>
 *
 * <p><b>La unicidad de los alias se define sobre esta forma, no sobre la
 * original.</b> Si se definiera sobre el texto tal como se escribió, dos alias
 * como {@code Sistolica} y {@code SISTÓLICA} convivirían en la tabla y chocarían
 * después, al emparejar las columnas — que es el peor momento para enterarse,
 * porque el resultado terminaría guardado en el parámetro equivocado.</p>
 */
public final class NormalizadorAlias {

    private NormalizadorAlias() {}

    /**
     * Mayúsculas, sin acentos, sin espacios en los extremos y con los internos
     * colapsados a uno solo.
     *
     * <p>Se usa {@link Locale#ROOT} a propósito: con el locale del sistema, una
     * JVM configurada en turco convierte la i en ı sin punto y dos alias que
     * deberían coincidir dejan de hacerlo.</p>
     *
     * @return la forma canónica, o null si la entrada es null o queda vacía
     */
    public static String normalizar(String texto) {
        if (texto == null) return null;

        String limpio = texto.trim();
        if (limpio.isEmpty()) return null;

        // NFD separa la letra de su tilde para poder descartar la segunda.
        limpio = Normalizer.normalize(limpio, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // El patron cubre también el espacio duro, que llega pegado en los CSV que
        // pasaron por una hoja de cálculo.
        limpio = limpio.replaceAll("[\\s\\u00A0]+", " ").trim();

        return limpio.toUpperCase(Locale.ROOT);
    }

    /** ¿Estos dos textos son el mismo alias, escritos de cualquier manera? */
    public static boolean coinciden(String a, String b) {
        String na = normalizar(a);
        String nb = normalizar(b);
        return na != null && na.equals(nb);
    }
}
