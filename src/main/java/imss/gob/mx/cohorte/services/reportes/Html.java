package imss.gob.mx.cohorte.services.reportes;

/**
 * Escapado de texto que va al documento.
 *
 * <p>Está aparte porque lo usan el maquetador y cada bloque, y tres copias de esto
 * son tres oportunidades de que a una se le olvide un carácter. Los nombres, las
 * observaciones y los valores de texto los escribe una persona: un «&» suelto
 * rompería el documento, y una etiqueta cruda sería algo peor.</p>
 */
public final class Html {

    private Html() {}

    public static String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Escapa y convierte los saltos de línea en saltos visibles. */
    public static String escaparConSaltos(String s) {
        return escapar(s).replace("\n", "<br/>");
    }
}
