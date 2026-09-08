package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.reportes.ImagenReporte;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Convierte {@code imagen:{id}} en algo que el motor de PDF sepa dibujar.
 *
 * <p>Se incrusta como data URI en vez de dejar que el motor descargue una URL. Dos
 * razones, y ninguna es de comodidad:</p>
 *
 * <ul>
 *   <li>Quien descargaría sería el <b>servidor</b>, no el navegador. Una URL guardada
 *       en un diseño se convertiría en una petición que sale de dentro de la red, a
 *       donde diga el diseño. Resolviendo solo referencias de la galería, el servidor
 *       nunca pide nada que no sea suyo.</li>
 *   <li>Una URL firmada caduca y una fija ata el diseño al dominio desde el que se
 *       guardó, que aquí no es el mismo en local, pruebas y producción.</li>
 * </ul>
 *
 * <p>Una imagen que ya no existe se omite en silencio: el reporte sale sin el logo,
 * que es mucho mejor que no salir.</p>
 */
@Service
@AllArgsConstructor
public class ImagenesReporte {

    private static final Logger log = LoggerFactory.getLogger(ImagenesReporte.class);

    /** Tope de seguridad. La subida ya limita a 2 MB; esto cubre datos antiguos. */
    private static final int MAX_BYTES = 4 * 1024 * 1024;

    private final ImagenReporteService service;

    /**
     * Una imagen ya resuelta: sus bytes y sus medidas reales.
     *
     * <p>Las medidas hacen falta para colocarla: sin la proporción original no se
     * puede respetar, y la imagen acaba estirada.</p>
     */
    public record Resuelta(String dataUri, Integer anchoPx, Integer altoPx) {}

    /**
     * La imagen de esa clave, o null si no se puede resolver.
     *
     * @param cache memoria por documento: un membrete repetido en veinte páginas se
     *              descarga y se codifica una sola vez
     */
    public Resuelta resuelta(String clave, Map<String, Resuelta> cache) {
        if (cache != null && cache.containsKey(clave)) return cache.get(clave);

        Resuelta resultado = resolver(clave);
        if (cache != null) cache.put(clave, resultado);
        return resultado;
    }

    private Resuelta resolver(String clave) {
        Long id = ClaveImagen.idDe(clave);
        if (id == null) return null;

        try {
            ImagenReporte imagen = service.getById(id);
            if (imagen.getBytes() != null && imagen.getBytes() > MAX_BYTES) {
                log.warn("La imagen {} pesa {} bytes y no se incrusta en el reporte.",
                        id, imagen.getBytes());
                return null;
            }
            try (InputStream in = service.contenidoDe(imagen)) {
                byte[] contenido = in.readAllBytes();
                if (contenido.length > MAX_BYTES) return null;
                String dataUri = "data:" + imagen.getContentType() + ";base64,"
                        + Base64.getEncoder().encodeToString(contenido);
                return new Resuelta(dataUri, imagen.getAnchoPx(), imagen.getAltoPx());
            }
        } catch (Exception e) {
            // Se borró, MinIO no responde, o es de otra institución. Ninguna de esas
            // cosas debe tumbar la emisión del documento entero.
            log.warn("No se pudo incrustar la imagen {}: {}", clave, e.getMessage());
            return null;
        }
    }

    /** Una memoria nueva para un documento. */
    public static Map<String, Resuelta> nuevaCache() {
        return new HashMap<>();
    }
}
