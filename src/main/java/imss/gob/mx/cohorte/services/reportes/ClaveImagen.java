package imss.gob.mx.cohorte.services.reportes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cómo apunta un diseño a una imagen de la galería: {@code imagen:{id}}.
 *
 * <p>Se guarda la referencia y no una URL. Una firmada caduca —y una plantilla que
 * dejó de mostrar el membrete a los sesenta minutos no da ninguna pista de por qué—, y
 * una fija ataría el diseño al dominio desde el que se guardó, que en este sistema no
 * es el mismo en producción, en pruebas y en local.</p>
 *
 * <p>El editor resuelve la referencia contra el endpoint que sirve los bytes y el
 * maquetador la convierte en data URI para el PDF. Los dos parten de lo mismo, que es
 * lo que hace que la vista previa no mienta.</p>
 */
public final class ClaveImagen {

    private ClaveImagen() {}

    private static final String PREFIJO = "imagen:";
    private static final Pattern PATRON = Pattern.compile("^imagen:(\\d+)$");

    public static String de(Long id) {
        return id == null ? "" : PREFIJO + id;
    }

    /** El id que hay dentro de la clave, o null si eso no es una clave de imagen. */
    public static Long idDe(String clave) {
        if (clave == null) return null;
        Matcher m = PATRON.matcher(clave.trim());
        return m.matches() ? Long.valueOf(m.group(1)) : null;
    }

    public static boolean esClave(String valor) {
        return idDe(valor) != null;
    }
}
