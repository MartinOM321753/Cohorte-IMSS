package imss.gob.mx.cohorte.services.cuestionarios;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee la etiqueta impresa en la hoja: {@code C1/008601/F4}.
 *
 * <p>Tres cosas en una cadena: qué instrumento es, de quién es y el literal fijo
 * que cierra la etiqueta. De las tres, la que importa en el volcado es el folio: es el
 * único puente entre una fila de respuestas y un participante del padrón, así que
 * interpretarlo mal no produce un error visible —produce dos mil respuestas archivadas
 * en el expediente de otra persona—.
 *
 * <h3>Por qué es tan estricta</h3>
 *
 * <p>Sería fácil «ser tolerante»: tomar lo que haya entre diagonales, o quedarse
 * con los dígitos que aparezcan. Eso es exactamente lo que no debe hacerse. Si la
 * etiqueta viene como {@code C1/8601/F4} —sin los ceros a la izquierda— el folio
 * puede existir y ser de otro participante, y una lectura permisiva lo aceptaría
 * sin una palabra. Lo mismo con un prefijo desconocido: una hoja etiquetada
 * {@code C9} no es un cuestionario del que no sepamos el tipo, es una hoja que hay
 * que revisar.</p>
 *
 * <p>Por eso la clase no corrige nada y no adivina nada: o la etiqueta tiene la
 * forma convenida, o se rechaza con el motivo. El folio se devuelve <em>tal como
 * está escrito</em>, porque así es como está guardado en el padrón.</p>
 */
public final class EtiquetaCuestionario {

    private EtiquetaCuestionario() {}

    /** Literal fijo con el que cierra la etiqueta, igual que en los consentimientos. */
    public static final String LITERAL = "F4";

    /**
     * {@code PREFIJO/FOLIO/F4}. El prefijo es una o dos letras seguidas de dígitos
     * —{@code C1}, {@code CI}— y el folio, dígitos.
     */
    private static final Pattern FORMA = Pattern.compile(
            "^\\s*([A-Za-z]{1,2}\\d{0,2})\\s*/\\s*(\\d{1,20})\\s*/\\s*([A-Za-z0-9]{1,5})\\s*$");

    /**
     * Resultado de leer una etiqueta. Cuando {@code valida} es falso, {@code motivo}
     * dice qué pasa con ella y los demás campos no sirven.
     *
     * @param clave   prefijo del instrumento, en mayúsculas
     * @param folio   folio del participante, con los ceros con los que venía escrito
     * @param literal lo que venía en el tercer segmento
     */
    public record Lectura(boolean valida, String clave, String folio, String literal, String motivo) {

        static Lectura ok(String clave, String folio, String literal) {
            return new Lectura(true, clave, folio, literal, null);
        }

        static Lectura mal(String motivo) {
            return new Lectura(false, null, null, null, motivo);
        }
    }

    public static Lectura leer(String etiqueta) {
        if (etiqueta == null || etiqueta.isBlank()) {
            return Lectura.mal("La etiqueta viene vacia.");
        }

        Matcher m = FORMA.matcher(etiqueta);
        if (!m.matches()) {
            return Lectura.mal("La etiqueta \"" + etiqueta.trim()
                    + "\" no tiene la forma PREFIJO/FOLIO/" + LITERAL + ".");
        }

        String literal = m.group(3).toUpperCase();
        if (!LITERAL.equals(literal)) {
            // No se corrige ni se ignora: el literal es lo que confirma que la
            // cadena es una etiqueta de esta cohorte y no otro identificador con
            // diagonales que alguien escribio en la columna.
            return Lectura.mal("La etiqueta \"" + etiqueta.trim() + "\" termina en \""
                    + literal + "\" y deberia terminar en " + LITERAL + ".");
        }

        return Lectura.ok(m.group(1).toUpperCase(), m.group(2), literal);
    }

    /**
     * El folio solo, para cuando la columna trae el folio pelado en lugar de la
     * etiqueta completa.
     *
     * <p>Existe porque así llegó el primer volcado: la columna se llamaba
     * {@code Id-Participante} y traía {@code 1}, sin prefijo ni literal. Esa forma no
     * dice de qué instrumento es la hoja, y por eso el importador no lo deduce de
     * aquí: el instrumento se elige al cargar el archivo.</p>
     */
    public static boolean esFolioPelado(String valor) {
        return valor != null && valor.trim().matches("\\d{1,20}");
    }

    /**
     * De quién es la fila, venga la etiqueta completa o el folio pelado.
     *
     * <p>Es lo que necesita la carga del volcado, donde el instrumento ya lo eligió
     * quien sube el archivo y lo único que falta es el participante. Las dos formas se
     * admiten porque las dos han llegado, y distinguirlas no aporta nada cuando la
     * clave no decide nada. Lo que no se admite es nada más: un valor con diagonales
     * que no tenga la forma convenida se rechaza con su motivo, igual que antes
     * —{@code C1/8601/F4}, sin los ceros, podría ser el folio de otro participante, y
     * aceptarlo archivaría dos mil respuestas en el expediente equivocado sin una
     * palabra—.</p>
     *
     * @param clave la clave del instrumento cuando venía en la etiqueta, o null si la
     *              columna solo traía el folio. No identifica nada: sirve para avisar
     *              cuando el archivo parece ser de otro instrumento.
     */
    public record Folio(String folio, String clave, String motivo) {
        public boolean valido() {
            return folio != null;
        }
    }

    public static Folio folioDe(String valor) {
        if (valor == null || valor.isBlank()) {
            return new Folio(null, null, "La fila no trae folio ni etiqueta de participante.");
        }
        if (esFolioPelado(valor)) {
            return new Folio(valor.trim(), null, null);
        }
        Lectura l = leer(valor);
        return l.valida()
                ? new Folio(l.folio(), l.clave(), null)
                : new Folio(null, null, l.motivo());
    }
}
