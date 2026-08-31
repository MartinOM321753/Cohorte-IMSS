package imss.gob.mx.cohorte.services.reportes;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Convierte el HTML de un reporte en PDF.
 *
 * <p>Es la única pieza del módulo que sabe de PDF. Todo lo demás —plantillas, datos,
 * maquetado— trabaja con HTML, que es lo que también se muestra en pantalla; así la
 * vista previa y el documento impreso salen del mismo sitio y no pueden divergir.</p>
 *
 * <p>El motor es openhtmltopdf, que renderiza CSS 2.1: posiciones absolutas, tablas y
 * {@code @page} funcionan; flexbox y grid no. El HTML del reporte se arma pensando en
 * eso, y como lo genera el sistema y no el usuario, la limitación no se traslada a
 * nadie.</p>
 *
 * <p><b>Nada de esto toca el módulo de etiquetas.</b> Aquel imprime por navegador y por
 * ZPL, está calibrado contra impresora física y no se modifica.</p>
 */
@Slf4j
@Service
public class ReportePdfService {

    /**
     * Tipografías que se registran en el motor. openhtmltopdf no ve las fuentes del
     * sistema operativo: si no se le entrega el archivo, cae a las catorce fuentes
     * base del formato PDF, y con ellas los acentos dependen de la codificación —que
     * es justo donde un reporte en español se rompe—.
     */
    private static final String FUENTE_REGULAR = "/fonts/DejaVuSans.ttf";
    private static final String FUENTE_NEGRITA = "/fonts/DejaVuSans-Bold.ttf";

    /** Nombre con el que el CSS del reporte pide esta familia. */
    public static final String FAMILIA = "Reporte";

    public byte[] aPdf(String html) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registrarFuentes(builder);
            builder.withHtmlContent(html, null);
            builder.toStream(salida);
            builder.run();
        } catch (Exception e) {
            log.error("No se pudo generar el PDF del reporte: {}", e.getMessage(), e);
            throw new ReporteNoGeneradoException(
                    "No se pudo generar el PDF del reporte. " + e.getMessage(), e);
        }
        return salida.toByteArray();
    }

    /**
     * Registra las tipografías desde el classpath.
     *
     * <p>Se pasan como proveedor perezoso —un {@code FSSupplier}— y no como fichero:
     * dentro del jar no hay ruta de disco que darle al motor.</p>
     */
    private void registrarFuentes(PdfRendererBuilder builder) {
        registrarSiExiste(builder, FUENTE_REGULAR, 400);
        registrarSiExiste(builder, FUENTE_NEGRITA, 700);
    }

    private void registrarSiExiste(PdfRendererBuilder builder, String ruta, int peso) {
        if (getClass().getResource(ruta) == null) {
            // Sin la fuente el reporte se genera igual, pero con las base del PDF y el
            // riesgo de que los acentos salgan mal. Se avisa una vez, al generar.
            log.warn("Tipografía {} no encontrada en el classpath; el PDF usará las fuentes base", ruta);
            return;
        }
        builder.useFont(() -> abrir(ruta), FAMILIA, peso,
                com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle.NORMAL, true);
    }

    private InputStream abrir(String ruta) {
        InputStream in = getClass().getResourceAsStream(ruta);
        if (in == null) {
            throw new ReporteNoGeneradoException("No se pudo abrir la tipografía " + ruta, null);
        }
        return in;
    }

    /** Falla al construir el documento; no es culpa de quien lo pidió. */
    public static class ReporteNoGeneradoException extends RuntimeException {
        public ReporteNoGeneradoException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
