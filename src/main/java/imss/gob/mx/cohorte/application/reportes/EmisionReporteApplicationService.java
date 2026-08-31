package imss.gob.mx.cohorte.application.reportes;

import imss.gob.mx.cohorte.application.EstudiosApplicationService;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.reportes.PlantillaReporte;
import imss.gob.mx.cohorte.modules.reportes.TipoReporte;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.examenes.ResultadoExamenService;
import imss.gob.mx.cohorte.services.reportes.ContextoEstudio;
import imss.gob.mx.cohorte.services.reportes.MaquetadorReporte;
import imss.gob.mx.cohorte.services.reportes.PlantillaReporteService;
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
    private final PlantillaReporteService plantillaService;
    private final MaquetadorReporte maquetador;
    private final ResultadoExamenService resultadoExamenService;
    private final MuestraService muestraService;

    /** El reporte listo para descargar: los bytes y cómo debe llamarse el archivo. */
    public record ReporteEmitido(byte[] contenido, String nombreArchivo) {}

    @Transactional(readOnly = true)
    public ReporteEmitido deEstudio(Long idEstudio, Long idPlantilla) {
        EstudioMedico estudio = estudiosApplicationService.getEstudio(idEstudio);
        byte[] pdf = pdfService.aPdf(htmlDe(estudio, idPlantilla));
        return new ReporteEmitido(pdf, nombreArchivoDe(estudio));
    }

    /** El HTML sin convertir, para la vista previa en pantalla. */
    @Transactional(readOnly = true)
    public String previsualizarEstudio(Long idEstudio, Long idPlantilla) {
        return htmlDe(estudiosApplicationService.getEstudio(idEstudio), idPlantilla);
    }

    /**
     * Con plantilla se maqueta el diseno; sin ella se usa el formato de siempre.
     *
     * <p>Ese respaldo no es provisional: una institucion que todavia no ha disenado
     * nada tiene que poder emitir igual, y quedarse sin reporte por no haber pasado
     * por el editor seria peor que un formato generico.</p>
     */
    private String htmlDe(EstudioMedico estudio, Long idPlantilla) {
        PlantillaReporte plantilla = resolverPlantilla(idPlantilla);
        if (plantilla == null || plantilla.getDiseno() == null || plantilla.getDiseno().isBlank()) {
            return htmlService.generar(estudio);
        }
        return maquetador.maquetar(plantilla.getDiseno(), contextoDe(estudio));
    }

    /**
     * La pedida, o la predeterminada del tipo. getById comprueba la institucion, asi
     * que no se puede emitir con la plantilla de otra pasando su id.
     */
    private PlantillaReporte resolverPlantilla(Long idPlantilla) {
        if (idPlantilla != null) return plantillaService.getById(idPlantilla);
        return plantillaService.getPredeterminada(TipoReporte.ESTUDIO);
    }

    /**
     * Los conteos se piden a los mismos servicios que alimentan el expediente, no a
     * los repositorios: asi el reporte dice exactamente lo que la pantalla enseña,
     * incluida la regla de hasta donde alcanza el acceso al participante.
     */
    private ContextoEstudio contextoDe(EstudioMedico estudio) {
        String uuid = estudio.getPaciente() != null ? estudio.getPaciente().getUuid() : null;
        if (uuid == null) return new ContextoEstudio(estudio, ContextoEstudio.Totales.sinCalcular());

        return new ContextoEstudio(estudio, new ContextoEstudio.Totales(
                estudiosApplicationService.getEstudiosByPaciente(uuid).size(),
                resultadoExamenService.countByPacienteUuid(uuid),
                muestraService.countByPacienteUuid(uuid)));
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
