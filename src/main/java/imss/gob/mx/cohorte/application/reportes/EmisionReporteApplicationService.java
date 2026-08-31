package imss.gob.mx.cohorte.application.reportes;

import imss.gob.mx.cohorte.application.EstudiosApplicationService;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.reportes.ReporteEstudioHtmlService;
import imss.gob.mx.cohorte.services.reportes.ReportePdfService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;

/**
 * Emitir un reporte: reunir los datos, maquetarlos y devolver el PDF.
 *
 * <p>Los datos se piden a {@link EstudiosApplicationService}, no al repositorio: así el
 * reporte pasa por las mismas comprobaciones de institución y de acceso al participante
 * que la pantalla. <b>Un reporte no puede enseñar lo que la pantalla no enseña</b>, y la
 * forma de garantizarlo es que ambos entren por la misma puerta.</p>
 *
 * <p>De momento el diseño es el fijo de la primera fase. Cuando exista el editor, aquí
 * se elegirá la plantilla; lo demás no cambia.</p>
 */
@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.REPORTES)
public class EmisionReporteApplicationService {

    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EstudiosApplicationService estudiosApplicationService;
    private final ReporteEstudioHtmlService htmlService;
    private final ReportePdfService pdfService;

    /** El reporte listo para descargar: los bytes y cómo debe llamarse el archivo. */
    public record ReporteEmitido(byte[] contenido, String nombreArchivo) {}

    @Transactional(readOnly = true)
    public ReporteEmitido deEstudio(Long idEstudio) {
        EstudioMedico estudio = estudiosApplicationService.getEstudio(idEstudio);
        byte[] pdf = pdfService.aPdf(htmlService.generar(estudio));
        return new ReporteEmitido(pdf, nombreArchivoDe(estudio));
    }

    /** El HTML sin convertir, para la vista previa en pantalla. */
    @Transactional(readOnly = true)
    public String previsualizarEstudio(Long idEstudio) {
        return htmlService.generar(estudiosApplicationService.getEstudio(idEstudio));
    }

    /**
     * Un nombre que se entienda en la carpeta de descargas: tipo de estudio, folio y
     * fecha. Sin acentos ni espacios, porque el nombre viaja en una cabecera HTTP y
     * acaba en sistemas de archivos que no siempre los toleran.
     */
    private String nombreArchivoDe(EstudioMedico estudio) {
        String tipo = estudio.getTipoEstudio() != null ? estudio.getTipoEstudio().getNombre() : "estudio";
        String folio = estudio.getPaciente() != null && estudio.getPaciente().getFolio() != null
                ? estudio.getPaciente().getFolio() : String.valueOf(estudio.getId());
        String fecha = estudio.getFechaEstudio() != null
                ? estudio.getFechaEstudio().format(FECHA_ARCHIVO) : "";
        return (simplificar(tipo) + "_" + simplificar(folio) + (fecha.isEmpty() ? "" : "_" + fecha) + ".pdf");
    }

    private String simplificar(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String limpio = sinAcentos.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return limpio.isEmpty() ? "reporte" : limpio;
    }
}
