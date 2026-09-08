package imss.gob.mx.cohorte.application.reportes;

import imss.gob.mx.cohorte.application.EstudiosApplicationService;
import imss.gob.mx.cohorte.application.PacienteApplicationService;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.reportes.PlantillaReporte;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.examenes.ResultadoExamenService;
import imss.gob.mx.cohorte.services.reportes.ContextoReporte;
import imss.gob.mx.cohorte.services.reportes.MaquetadorReporte;
import imss.gob.mx.cohorte.services.reportes.PlantillaReporteService;
import imss.gob.mx.cohorte.services.reportes.ReporteEstudioHtmlService;
import imss.gob.mx.cohorte.services.reportes.ReportePdfService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Emitir un reporte: reunir los datos del participante, maquetarlos y devolver el PDF.
 *
 * <p>Los datos se piden a los servicios de aplicación, no a los repositorios: así el
 * reporte pasa por las mismas comprobaciones de institución y de acceso al
 * participante que la pantalla. <b>Un reporte no puede enseñar lo que la pantalla no
 * enseña</b>, y la forma de garantizarlo es que ambos entren por la misma puerta.</p>
 *
 * <p>El contexto es siempre el participante y todos sus estudios, aunque el reporte
 * hable de uno solo. Es lo que permite que una hoja combine parámetros del DEXA, de
 * signos vitales y de la TANITA sin tener que emitir tres documentos.</p>
 */
@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.REPORTES)
public class EmisionReporteApplicationService {

    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EstudiosApplicationService estudiosApplicationService;
    private final PacienteApplicationService pacienteApplicationService;
    private final ReporteEstudioHtmlService htmlService;
    private final ReportePdfService pdfService;
    private final PlantillaReporteService plantillaService;
    private final MaquetadorReporte maquetador;
    private final ResultadoExamenService resultadoExamenService;
    private final MuestraService muestraService;

    /** El reporte listo para descargar: los bytes y cómo debe llamarse el archivo. */
    public record ReporteEmitido(byte[] contenido, String nombreArchivo) {}

    // ── Reporte de un participante ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReporteEmitido deParticipante(String uuid, Long idPlantilla) {
        String html = htmlDeParticipante(uuid, idPlantilla);
        return new ReporteEmitido(pdfService.aPdf(html), nombreArchivoDeParticipante(uuid));
    }

    @Transactional(readOnly = true)
    public String previsualizarParticipante(String uuid, Long idPlantilla) {
        return htmlDeParticipante(uuid, idPlantilla);
    }

    private String htmlDeParticipante(String uuid, Long idPlantilla) {
        PlantillaReporte plantilla = plantillaService.getById(idPlantilla);
        if (plantilla.getDiseno() == null || plantilla.getDiseno().isBlank()) {
            throw new ValidationException(
                    "La plantilla \"" + plantilla.getNombre() + "\" todavía no tiene diseño.");
        }
        return maquetador.maquetar(plantilla.getDiseno(), contextoDe(uuid, null));
    }

    // ── Reporte de un estudio concreto ───────────────────────────────────────

    @Transactional(readOnly = true)
    public ReporteEmitido deEstudio(Long idEstudio, Long idPlantilla) {
        EstudioMedico estudio = estudiosApplicationService.getEstudio(idEstudio);
        byte[] pdf = pdfService.aPdf(htmlDeEstudio(estudio, idPlantilla));
        return new ReporteEmitido(pdf, nombreArchivoDeEstudio(estudio));
    }

    @Transactional(readOnly = true)
    public String previsualizarEstudio(Long idEstudio, Long idPlantilla) {
        return htmlDeEstudio(estudiosApplicationService.getEstudio(idEstudio), idPlantilla);
    }

    /**
     * Con plantilla se maqueta el diseño; sin ella se usa el formato base.
     *
     * <p>Ese respaldo no es provisional: una institución que todavía no ha diseñado
     * nada tiene que poder emitir igual, y quedarse sin reporte por no haber pasado
     * por el editor sería peor que un formato genérico.</p>
     */
    private String htmlDeEstudio(EstudioMedico estudio, Long idPlantilla) {
        PlantillaReporte plantilla = idPlantilla != null ? plantillaService.getById(idPlantilla) : null;
        if (plantilla == null || plantilla.getDiseno() == null || plantilla.getDiseno().isBlank()) {
            return htmlService.generar(estudio);
        }
        String uuid = estudio.getPaciente() != null ? estudio.getPaciente().getUuid() : null;
        return maquetador.maquetar(plantilla.getDiseno(), contextoDe(uuid, estudio));
    }

    // ── Contexto ─────────────────────────────────────────────────────────────

    /**
     * El participante con todos sus estudios. Los conteos se piden a los servicios que
     * alimentan el expediente, para que el reporte diga exactamente lo que la pantalla
     * enseña, incluida la regla de hasta dónde alcanza el acceso al participante.
     */
    private ContextoReporte contextoDe(String uuid, EstudioMedico focal) {
        if (uuid == null) {
            return new ContextoReporte(focal != null ? focal.getPaciente() : null,
                    focal != null ? List.of(focal) : List.of(), List.of(), focal,
                    ContextoReporte.Totales.sinCalcular());
        }

        Paciente paciente = pacienteApplicationService.findByUUID(uuid);
        List<EstudioMedico> estudios = estudiosApplicationService.getEstudiosByPaciente(uuid);
        // Los resultados de laboratorio se piden por el mismo camino que el
        // expediente, que ya decide hasta dónde alcanza el acceso al participante.
        List<ResultadoExamen> examenes = resultadoExamenService.findAllByUUID(uuid);

        return new ContextoReporte(paciente, estudios, examenes, focal, new ContextoReporte.Totales(
                estudios.size(), examenes.size(), muestraService.countByPacienteUuid(uuid)));
    }

    // ── Nombre del archivo ───────────────────────────────────────────────────

    private String nombreArchivoDeEstudio(EstudioMedico estudio) {
        String tipo = estudio.getTipoEstudio() != null ? estudio.getTipoEstudio().getNombre() : "estudio";
        String folio = estudio.getPaciente() != null && estudio.getPaciente().getFolio() != null
                ? estudio.getPaciente().getFolio() : String.valueOf(estudio.getId());
        String fecha = estudio.getFechaEstudio() != null
                ? estudio.getFechaEstudio().format(FECHA_ARCHIVO) : "";
        return simplificar(tipo) + "_" + simplificar(folio)
                + (fecha.isEmpty() ? "" : "_" + fecha) + ".pdf";
    }

    private String nombreArchivoDeParticipante(String uuid) {
        Paciente p = pacienteApplicationService.findByUUID(uuid);
        String folio = p != null && p.getFolio() != null ? p.getFolio() : "participante";
        return "reporte_" + simplificar(folio) + "_"
                + LocalDate.now().format(FECHA_ARCHIVO) + ".pdf";
    }

    /**
     * Un nombre que se entienda en la carpeta de descargas, sin acentos ni espacios:
     * viaja en una cabecera HTTP y acaba en sistemas de archivos que no siempre los
     * toleran.
     */
    private String simplificar(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String limpio = sinAcentos.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return limpio.isEmpty() ? "reporte" : limpio;
    }
}
