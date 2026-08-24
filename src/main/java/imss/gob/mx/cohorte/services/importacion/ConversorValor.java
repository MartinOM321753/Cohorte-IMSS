package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;

import java.util.List;
import java.util.Locale;

/**
 * Convierte el texto de una celda al valor que espera un parametro.
 *
 * <p>El lector entrega todo como texto a proposito, porque no sabe a que
 * parametro corresponde cada columna. Aqui ya se sabe, y por eso este es el
 * unico sitio donde se decide si "72,5" es un numero, si "SI" es verdadero o si
 * "Izquierda" esta entre las opciones configuradas.</p>
 *
 * <h3>Por que nunca devuelve un valor a medias</h3>
 *
 * <p>Cuando algo no se entiende se lanza {@link ValorNoValidoException} con un
 * mensaje dirigido a quien exporto el archivo, y la celda se marca en la
 * previsualizacion para que la corrija. Convertir "N/A" en 0 o en null seria
 * peor que fallar: el estudio quedaria guardado con un dato inventado y nadie
 * volveria a mirarlo.</p>
 */
public final class ConversorValor {

    private ConversorValor() {}

    /** Longitud de la columna valor_texto en la base. */
    private static final int MAX_TEXTO = 255;

    /**
     * El valor ya convertido. Solo uno de los tres campos viene relleno, el que
     * corresponde al tipo del parametro.
     */
    public record Convertido(Double numerico, String texto, Boolean booleano) {

        public static Convertido deNumero(double v) { return new Convertido(v, null, null); }
        public static Convertido deTexto(String v)  { return new Convertido(null, v, null); }
        public static Convertido deBooleano(boolean v) { return new Convertido(null, null, v); }
    }

    /** Lo que un aparato o una hoja de calculo escriben cuando no hay dato. */
    private static final List<String> AUSENCIAS = List.of(
            "N/A", "NA", "N.A.", "ND", "N.D.", "-", "--", "---", "SIN DATO", "NULL", "NONE", "#N/A");

    private static final List<String> AFIRMATIVOS = List.of(
            "SI", "S", "TRUE", "T", "VERDADERO", "V", "1", "X", "YES", "Y", "POSITIVO");

    private static final List<String> NEGATIVOS = List.of(
            "NO", "N", "FALSE", "F", "FALSO", "0", "NEGATIVO");

    /**
     * @param crudo    el texto tal como venia en la celda
     * @param tipo     el tipo del parametro al que corresponde la columna
     * @param opciones las opciones configuradas; solo se usan en TEXTO_OPCIONES
     * @throws ValorNoValidoException si la celda esta vacia o no se entiende
     */
    public static Convertido convertir(String crudo, TipoParametro tipo, List<OpcionParametro> opciones) {
        String limpio = crudo == null ? "" : crudo.trim();

        // Todos los parametros son obligatorios, asi que una celda vacia es un
        // problema que hay que corregir, no un valor que se pueda omitir.
        if (limpio.isEmpty()) {
            throw new ValorNoValidoException("La celda esta vacia y el parametro es obligatorio.");
        }
        if (AUSENCIAS.contains(limpio.toUpperCase(Locale.ROOT))) {
            throw new ValorNoValidoException(
                    "\"" + limpio + "\" indica que no hay dato, y el parametro es obligatorio. "
                            + "Captura el valor o elimina la fila.");
        }

        return switch (tipo) {
            case NUMERICO -> Convertido.deNumero(aNumero(limpio));
            case BOOLEANO -> Convertido.deBooleano(aBooleano(limpio));
            case TEXTO -> Convertido.deTexto(aTexto(limpio));
            case TEXTO_OPCIONES -> Convertido.deTexto(aOpcion(limpio, opciones));
        };
    }

    // ── Numerico ─────────────────────────────────────────────────────────────

    /**
     * Acepta lo que producen los aparatos y las hojas de calculo en espanol:
     * coma decimal, separador de miles, unidades pegadas al numero y el signo
     * de menos "largo" que insertan algunos exportadores.
     */
    private static double aNumero(String texto) {
        String t = texto
                .replace('−', '-')   // minus matematico
                .replace(' ', ' ')   // espacio duro
                .replace("%", "")
                .trim();

        // Un numero con unidad pegada ("72.5 kg") se acepta quitando la unidad:
        // el parametro ya define cual es, y rechazarlo obligaria a reeditar el
        // archivo por algo que no cambia el valor.
        t = t.replaceAll("(?i)\\s*[a-z/²³%µ]+\\.?$", "").trim();

        t = separadorDecimal(t);

        if (t.isEmpty()) {
            throw new ValorNoValidoException("\"" + texto + "\" no contiene ningun numero.");
        }
        try {
            double v = Double.parseDouble(t);
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new ValorNoValidoException("\"" + texto + "\" no es un numero valido.");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new ValorNoValidoException(
                    "\"" + texto + "\" no es un numero. Se esperaba algo como 72.5 o 72,5.");
        }
    }

    /**
     * Resuelve cual de los dos signos es el decimal.
     *
     * <p>"1,234" es ambiguo —mil doscientos treinta y cuatro, o uno coma dos tres
     * cuatro— y aqui no hay forma de saberlo. Se resuelve por la posicion: si el
     * separador deja exactamente tres cifras a la derecha y hay otro separador
     * antes, es de miles; en cualquier otro caso es decimal. Es la lectura que
     * acierta con lo que exportan los aparatos, que rara vez pasan de miles.</p>
     */
    private static String separadorDecimal(String t) {
        boolean tieneComa = t.indexOf(',') >= 0;
        boolean tienePunto = t.indexOf('.') >= 0;

        if (tieneComa && tienePunto) {
            // El ultimo en aparecer es el decimal; el otro es de miles.
            return t.lastIndexOf(',') > t.lastIndexOf('.')
                    ? t.replace(".", "").replace(',', '.')
                    : t.replace(",", "");
        }
        if (tieneComa) {
            // Una sola coma: decimal, salvo que sea el patron de miles (1,234).
            return esSeparadorDeMiles(t, ',') ? t.replace(",", "") : t.replace(',', '.');
        }
        if (tienePunto && esSeparadorDeMiles(t, '.')) {
            return t.replace(".", "");
        }
        return t;
    }

    /** 1,234 o 12,345,678: grupos de tres, ninguno decimal. */
    private static boolean esSeparadorDeMiles(String t, char sep) {
        String cuerpo = t.startsWith("-") || t.startsWith("+") ? t.substring(1) : t;
        String patron = "\\d{1,3}(\\" + sep + "\\d{3})+";
        return cuerpo.matches(patron);
    }

    // ── Booleano ─────────────────────────────────────────────────────────────

    private static boolean aBooleano(String texto) {
        String t = NormalizadorAlias.normalizar(texto);
        if (AFIRMATIVOS.contains(t)) return true;
        if (NEGATIVOS.contains(t)) return false;
        throw new ValorNoValidoException(
                "\"" + texto + "\" no se entiende como si o no. Usa Si/No, 1/0 o Verdadero/Falso.");
    }

    // ── Texto ────────────────────────────────────────────────────────────────

    private static String aTexto(String texto) {
        if (texto.length() > MAX_TEXTO) {
            throw new ValorNoValidoException(
                    "El texto tiene " + texto.length() + " caracteres y el maximo es " + MAX_TEXTO + ".");
        }
        return texto;
    }

    /**
     * Empareja contra las opciones configuradas ignorando acentos y mayusculas,
     * pero guarda la opcion tal como esta en el catalogo: si el archivo trae
     * "izquierda" y el catalogo dice "Izquierda", se guarda la del catalogo, o
     * el mismo parametro acabaria con dos escrituras del mismo valor.
     */
    private static String aOpcion(String texto, List<OpcionParametro> opciones) {
        if (opciones == null || opciones.isEmpty()) {
            throw new ValorNoValidoException(
                    "El parametro es de seleccion pero no tiene opciones configuradas en el catalogo.");
        }
        for (OpcionParametro o : opciones) {
            if (NormalizadorAlias.coinciden(o.getValor(), texto)) {
                return o.getValor();
            }
        }
        String admitidas = opciones.stream().map(OpcionParametro::getValor).reduce((a, b) -> a + ", " + b).orElse("");
        throw new ValorNoValidoException(
                "\"" + texto + "\" no es una de las opciones configuradas. Admitidas: " + admitidas + ".");
    }

    /**
     * Problema de UNA celda, no del archivo.
     *
     * <p>Va aparte de {@link ArchivoInvalidoException} porque no detiene la
     * carga: la celda se marca en la previsualizacion para que el usuario la
     * corrija sin volver a exportar nada.</p>
     */
    public static class ValorNoValidoException extends RuntimeException {
        public ValorNoValidoException(String mensaje) {
            super(mensaje);
        }
    }
}
