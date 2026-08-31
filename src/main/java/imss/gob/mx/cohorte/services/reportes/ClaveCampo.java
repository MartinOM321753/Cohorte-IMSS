package imss.gob.mx.cohorte.services.reportes;

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

    /** El listado de estudios del participante, que no depende de ningún tipo. */
    public static final String BLOQUE_LISTADO_ESTUDIOS = "bloque.estudios.listado";
}
