package imss.gob.mx.cohorte.application.reportes;

import imss.gob.mx.cohorte.application.EstudiosApplicationService;
import imss.gob.mx.cohorte.application.PacienteApplicationService;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.examenes.ResultadoExamenService;
import imss.gob.mx.cohorte.services.reportes.ContextoReporte;
import imss.gob.mx.cohorte.services.reportes.ExportacionDatosService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Descargar los datos de varios participantes.
 *
 * <p>Los participantes y sus datos se piden a los mismos servicios de aplicación que
 * alimentan el expediente y la emisión de reportes. Es la misma regla que ya sigue el
 * reporte: <b>una descarga no puede sacar lo que la pantalla no enseña</b>, y la forma
 * de garantizarlo es que todo entre por la misma puerta en vez de bajar al
 * repositorio.</p>
 */
@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.REPORTES)
public class ExportacionDatosApplicationService {

    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ExportacionDatosService exportacion;
    private final PacienteApplicationService pacienteApplicationService;
    private final EstudiosApplicationService estudiosApplicationService;
    private final ResultadoExamenService resultadoExamenService;

    /** El archivo listo para descargar: los bytes y cómo debe llamarse. */
    public record Descarga(byte[] contenido, String nombreArchivo) {}

    /**
     * @param claves    las columnas
     * @param uuids     qué participantes; vacío o nulo significa todos los que se alcanzan
     * @param separador coma o punto y coma
     */
    @Transactional(readOnly = true)
    public Descarga aCsv(List<String> claves, List<String> uuids, String separador) {
        List<String> seleccion = (uuids == null || uuids.isEmpty()) ? todosLosAlcanzables() : uuids;

        byte[] contenido = exportacion.aCsv(claves, separador, seleccion, this::contextoDe);
        return new Descarga(contenido,
                "datos_" + LocalDate.now().format(FECHA_ARCHIVO) + ".csv");
    }

    /**
     * Los participantes que este usuario puede ver.
     *
     * <p>Se usa el listado con jerarquía, que es el mismo que llena la pantalla de
     * participantes: incluye los de las sedes por debajo cuando la institución tiene esa
     * visibilidad concedida, y solo esos.</p>
     */
    private List<String> todosLosAlcanzables() {
        return pacienteApplicationService.getAllConJerarquia().stream()
                .map(Paciente::getUuid)
                .filter(uuid -> uuid != null && !uuid.isBlank())
                .toList();
    }

    private ContextoReporte contextoDe(String uuid) {
        Paciente paciente = pacienteApplicationService.findByUUID(uuid);
        var estudios = estudiosApplicationService.getEstudiosByPaciente(uuid);
        var examenes = resultadoExamenService.findAllByUUID(uuid);
        return new ContextoReporte(paciente, estudios, examenes, null,
                ContextoReporte.Totales.sinCalcular());
    }
}
