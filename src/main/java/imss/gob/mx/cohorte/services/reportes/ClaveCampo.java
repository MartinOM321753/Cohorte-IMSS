package imss.gob.mx.cohorte.services.reportes;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Las claves con las que una plantilla nombra un dato.
 *
 * <p>El tipo de estudio va <b>dentro de la clave</b>, no en la plantilla. Es lo que
 * permite que una misma hoja tome cinco parámetros del DEXA, tres de signos vitales
 * y las evidencias solo del primero. Atar la plantilla a un tipo —como estaba— hacía
 * imposible justamente eso.</p>
 *
 * <p>Formas que existen:</p>
 * <pre>
 *   participante.folio                  un dato de la persona
 *   estudio.12.fecha                    la fecha del estudio de tipo 12
 *   estudio.12.param.229                el valor del parámetro 229 dentro de ese estudio
 *   bloque.estudio.12.resultados        la tabla de resultados de ese tipo
 *   bloque.estudio.12.evidencias        los adjuntos de ese tipo
 *   bloque.estudios.listado             todos los estudios del participante
 * </pre>
 *
 * <p>Los identificadores se guardan en la plantilla, así que sobreviven a que se
 * renombre un estudio o un parámetro — que es lo que pasa con el tiempo—. Lo que no
 * sobrevive es que se borren, y para eso el maquetador deja hueco en vez de fallar.</p>
 */
public final class ClaveCampo {

    private static final Pattern ESTUDIO_PARAM =
            Pattern.compile("^estudio\\.(\\d+)\\.param\\.(\\d+)$");
    private static final Pattern ESTUDIO_CAMPO =
            Pattern.compile("^estudio\\.(\\d+)\\.(fecha|realizo|observaciones|nombre)$");
    private static final Pattern BLOQUE_ESTUDIO =
            Pattern.compile("^bloque\\.estudio\\.(\\d+)\\.(resultados|evidencias)$");

    private ClaveCampo() {}

    /** Un parámetro concreto dentro de un tipo de estudio. */
    public record Parametro(long idTipo, long idParametro) {}

    /** Un dato del propio estudio: su fecha, quién lo hizo, sus observaciones. */
    public record CampoEstudio(long idTipo, String campo) {}

    /** Un bloque que depende de un tipo de estudio. */
    public record BloqueEstudio(long idTipo, String bloque) {}

    /** Un analito de laboratorio. Los exámenes no son paneles: cada uno es uno. */
    public record CampoExamen(long idExamen, String campo) {}

    private static final Pattern EXAMEN_CAMPO =
            Pattern.compile("^examen\\.(\\d+)\\.(valor|fecha|unidad|referencia)$");

    public static Parametro comoParametro(String clave) {
        Matcher m = ESTUDIO_PARAM.matcher(clave);
        return m.matches() ? new Parametro(Long.parseLong(m.group(1)), Long.parseLong(m.group(2))) : null;
    }

    public static CampoEstudio comoCampoEstudio(String clave) {
        Matcher m = ESTUDIO_CAMPO.matcher(clave);
        return m.matches() ? new CampoEstudio(Long.parseLong(m.group(1)), m.group(2)) : null;
    }

    public static BloqueEstudio comoBloqueEstudio(String clave) {
        Matcher m = BLOQUE_ESTUDIO.matcher(clave);
        return m.matches() ? new BloqueEstudio(Long.parseLong(m.group(1)), m.group(2)) : null;
    }

    public static CampoExamen comoCampoExamen(String clave) {
        Matcher m = EXAMEN_CAMPO.matcher(clave);
        return m.matches() ? new CampoExamen(Long.parseLong(m.group(1)), m.group(2)) : null;
    }

    private static final Pattern FORMULA = Pattern.compile("^formula\\.(\\d+)$");
    private static final Pattern FORMULA_PARTE =
            Pattern.compile("^formula\\.(\\d+)\\.(minimo|maximo|referencia|estado)$");

    /** Una parte de una fórmula: sus límites, su referencia escrita o si el valor cae dentro. */
    public record ParteFormula(long idFormula, String parte) {}

    public static ParteFormula comoParteDeFormula(String clave) {
        Matcher m = FORMULA_PARTE.matcher(clave);
        return m.matches() ? new ParteFormula(Long.parseLong(m.group(1)), m.group(2)) : null;
    }

    public static String deParteDeFormula(long idFormula, String parte) {
        return "formula." + idFormula + "." + parte;
    }

    /**
     * Una fórmula del catálogo, por su identificador.
     *
     * <p>Es una clave más y no un tipo de elemento nuevo, y eso no es un atajo: al
     * entrar por el mismo sitio que los demás campos, una fórmula se puede meter dentro
     * de un párrafo, en una celda de una tabla hecha a mano y en un campo suelto, sin
     * que ninguna de las tres pantallas tenga que enterarse de que existe.</p>
     */
    public static Long comoFormula(String clave) {
        Matcher m = FORMULA.matcher(clave);
        return m.matches() ? Long.parseLong(m.group(1)) : null;
    }

    public static String deFormula(long idFormula) {
        return "formula." + idFormula;
    }

    // ── Construcción, para que el catálogo y el resolvedor no se separen ─────

    public static String deParametro(long idTipo, long idParametro) {
        return "estudio." + idTipo + ".param." + idParametro;
    }

    public static String deCampoEstudio(long idTipo, String campo) {
        return "estudio." + idTipo + "." + campo;
    }

    public static String deBloqueResultados(long idTipo) {
        return "bloque.estudio." + idTipo + ".resultados";
    }

    public static String deBloqueEvidencias(long idTipo) {
        return "bloque.estudio." + idTipo + ".evidencias";
    }

    public static String deExamen(long idExamen, String campo) {
        return "examen." + idExamen + "." + campo;
    }

    // ── Resumen de laboratorio ───────────────────────────────────────────────

    /**
     * Cuántas mediciones de laboratorio hay en cada situación.
     *
     * <p>Es lo que encabeza el reporte del participante: «23 mediciones · 14 en rango
     * · 7 ligeramente fuera · 2 a revisar». No sale de una fórmula porque una fórmula
     * opera sobre variables sueltas y esto cuenta un conjunto entero.</p>
     *
     * <p>Cuenta los laboratorios y no todo lo capturado a propósito: son los que el
     * reporte de salud lista, y meter en el mismo total mediciones de estudios que la
     * hoja no enseña daría un «23» que no cuadra con lo que se ve debajo.</p>
     */
    private static final Pattern RESUMEN = Pattern.compile(
            "^resumen\\.examenes\\.(total|enRango|ligeramenteFuera|revisar|sinDato)$");

    public static String comoResumen(String clave) {
        Matcher m = RESUMEN.matcher(clave);
        return m.matches() ? m.group(1) : null;
    }

    public static String deResumen(String parte) {
        return "resumen.examenes." + parte;
    }

    /** Las partes que se pueden pedir, en el orden en que se leen en el documento. */
    public static final List<String> PARTES_RESUMEN =
            List.of("total", "enRango", "ligeramenteFuera", "revisar", "sinDato");

    /** El listado de estudios del participante, que no depende de ningún tipo. */
    public static final String BLOQUE_LISTADO_ESTUDIOS = "bloque.estudios.listado";

    /** La tabla con los resultados de laboratorio del participante. */
    public static final String BLOQUE_LISTADO_EXAMENES = "bloque.examenes.listado";
}
