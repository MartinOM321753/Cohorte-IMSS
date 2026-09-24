package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Base64;

/**
 * Posición exacta dentro del listado de muestras, para paginar por llave.
 *
 * <p>El listado se ordena por {@code fecha_registro DESC, id_muestra DESC}. Un
 * cursor es el par de valores de la última fila entregada: la siguiente página
 * es «todo lo estrictamente menor que este par». No se usa OFFSET a propósito
 * —al registrar una muestra nueva mientras alguien pagina, un OFFSET desplaza
 * la ventana entera y hace que una fila se repita o se salte—; con la llave, la
 * frontera es un dato concreto y eso no puede ocurrir.</p>
 *
 * <p>La fecha sola no basta como llave: dos muestras registradas en el mismo
 * milisegundo —el caso normal al crear un lote de alícuotas— tendrían el mismo
 * valor, y la comparación estricta se saltaría una de ellas. El id desempata.</p>
 *
 * <p>Viaja a la pantalla codificado en Base64 URL-safe. No es un secreto ni una
 * medida de seguridad: es para que nadie construya cursores a mano y el formato
 * pueda cambiar sin romper a quien ya los tenga guardados.</p>
 */
public record CursorMuestra(long milisegundos, long id) {

    private static final String SEPARADOR = "|";

    public static CursorMuestra de(Muestra muestra) {
        Timestamp registro = muestra.getFechaRegistro();
        long millis = registro != null ? registro.getTime() : 0L;
        return new CursorMuestra(millis, muestra.getId());
    }

    public Timestamp fechaRegistro() {
        return new Timestamp(milisegundos);
    }

    public String codificar() {
        String plano = milisegundos + SEPARADOR + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(plano.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Devuelve {@code null} —no lanza— cuando el texto no es un cursor válido.
     *
     * <p>Un cursor corrupto llega de una URL vieja o editada a mano, y lo
     * razonable ahí es entregar la primera página, no un error 500 que deja la
     * pantalla en blanco.</p>
     */
    public static CursorMuestra decodificar(String codificado) {
        if (codificado == null || codificado.isBlank()) {
            return null;
        }
        try {
            String plano = new String(Base64.getUrlDecoder().decode(codificado.trim()),
                    StandardCharsets.UTF_8);
            int corte = plano.indexOf(SEPARADOR);
            if (corte <= 0) {
                return null;
            }
            return new CursorMuestra(
                    Long.parseLong(plano.substring(0, corte)),
                    Long.parseLong(plano.substring(corte + 1)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
