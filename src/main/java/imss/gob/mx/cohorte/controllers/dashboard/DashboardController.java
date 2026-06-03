package imss.gob.mx.cohorte.controllers.dashboard;

import imss.gob.mx.cohorte.controllers.dashboard.dto.AgendaHoyItemDTO;
import imss.gob.mx.cohorte.controllers.dashboard.dto.DashboardStatsDTO;
import imss.gob.mx.cohorte.controllers.dashboard.dto.ExamenResultGlobalDTO;
import imss.gob.mx.cohorte.controllers.dashboard.dto.SomatometriaGlobalDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.cita.CitaRepository;
import imss.gob.mx.cohorte.modules.documentos.MuestraDocumentoRepository;
import imss.gob.mx.cohorte.modules.documentos.PacienteDocumentoRepository;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.somatometria.SomatometriaRepository;
import imss.gob.mx.cohorte.utils.APIResponse;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@AllArgsConstructor
@Tag(name = "Dashboard", description = "Métricas resumidas y agenda del día para el panel de control")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    /** Zona horaria del sistema (México). */
    private static final ZoneId              ZONA         = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter   FMT_HORA     = DateTimeFormatter.ofPattern("HH:mm");

    private final PacienteRepository          pacienteRepository;
    private final MuestraRepository           muestraRepository;
    private final CitaRepository              citaRepository;
    private final EstudioMedicoRepository     estudioMedicoRepository;
    private final ResultadoExamenRepository   resultadoExamenRepository;
    private final PacienteDocumentoRepository pacienteDocumentoRepository;
    private final MuestraDocumentoRepository  muestraDocumentoRepository;
    private final SomatometriaRepository      somatometriaRepository;

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/stats
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    @Operation(
        summary     = "Estadísticas del dashboard",
        description = "Retorna conteos: pacientes activos, citas programadas/confirmadas del mes, "
                    + "y total de muestras registradas en biobanco."
    )
    public ResponseEntity<APIResponse> getStats() {

        // 1. Pacientes activos
        long pacientesActivos = pacienteRepository.countByActivo(true);

        // 2. Citas programadas o confirmadas en el mes actual
        YearMonth mesActual  = YearMonth.now(ZONA);
        Instant   inicioMes  = mesActual.atDay(1).atStartOfDay(ZONA).toInstant();
        Instant   finMes     = mesActual.atEndOfMonth().atTime(23, 59, 59).atZone(ZONA).toInstant();
        long citasProgramadas = citaRepository.countCitasProgramadasEnMes(inicioMes, finMes);

        // 3. Total de muestras en biobanco
        long muestrasBiobanco = muestraRepository.count();

        // 4. Estudios médicos con al menos un resultado registrado
        long estudiosConResultados = estudioMedicoRepository.countEstudiosConResultados();

        // 5. Total de resultados de exámenes de laboratorio
        long examenesLab = resultadoExamenRepository.count();

        // 6. Documentos de paciente (consentimientos + generales)
        long documentosGenerales = pacienteDocumentoRepository.count();

        // 7. Documentos vinculados a muestras biológicas
        long documentosMuestra = muestraDocumentoRepository.count();

        DashboardStatsDTO stats = new DashboardStatsDTO(
                pacientesActivos, citasProgramadas, muestrasBiobanco,
                estudiosConResultados, examenesLab, documentosGenerales, documentosMuestra);
        return ResponseEntity.ok(
            new APIResponse("Estadísticas obtenidas", stats, false, HttpStatus.OK)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/agenda-hoy
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/agenda-hoy")
    @Operation(
        summary     = "Agenda del día",
        description = "Retorna las citas no canceladas para el día de hoy (en hora local México), "
                    + "ordenadas por hora de inicio ascendente."
    )
    public ResponseEntity<APIResponse> getAgendaHoy() {

        LocalDate hoy      = LocalDate.now(ZONA);
        Instant   inicioDia = hoy.atStartOfDay(ZONA).toInstant();
        Instant   finDia    = hoy.plusDays(1).atStartOfDay(ZONA).toInstant();

        List<AgendaHoyItemDTO> agenda = citaRepository
            .findCitasHoy(inicioDia, finDia)
            .stream()
            .map(this::toAgendaDTO)
            .collect(Collectors.toList());

        return ResponseEntity.ok(
            new APIResponse("Agenda del día obtenida", agenda, false, HttpStatus.OK)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/somatometria-global
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/somatometria-global")
    @Operation(
        summary     = "Datos globales de somatometría para gráficas",
        description = "Retorna todos los registros de somatometría (campos numéricos únicamente) "
                    + "ordenados por fecha ascendente, para construir gráficas de tendencia global."
    )
    public ResponseEntity<APIResponse> getSomatometriaGlobal() {
        List<SomatometriaGlobalDTO> data = somatometriaRepository
                .findAll(Sort.by("fechaMedicion").ascending())
                .stream()
                .map(s -> new SomatometriaGlobalDTO(
                        s.getFechaMedicion() != null ? s.getFechaMedicion().toString() : null,
                        s.getPesoKg()   != null ? s.getPesoKg().doubleValue()   : null,
                        s.getImc()      != null ? s.getImc().doubleValue()       : null,
                        s.getPresionSistolica(),
                        s.getPresionDiastolica(),
                        s.getCircunferenciaAbdominalCm() != null
                                ? s.getCircunferenciaAbdominalCm().doubleValue() : null
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(
            new APIResponse("Datos de somatometría global obtenidos", data, false, HttpStatus.OK)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/examenes-global
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/examenes-global")
    @Operation(
        summary     = "Datos globales de resultados de exámenes para gráficas",
        description = "Retorna todos los resultados de exámenes (campos mínimos) ordenados por "
                    + "fecha ascendente, para construir gráficas de distribución y tendencia global."
    )
    public ResponseEntity<APIResponse> getExamenesGlobal() {
        List<ExamenResultGlobalDTO> data = resultadoExamenRepository
                .findAll(Sort.by("fechaResultado").ascending())
                .stream()
                .filter(r -> r.getExamen() != null)
                .map(r -> new ExamenResultGlobalDTO(
                        r.getFechaResultado() != null ? r.getFechaResultado().toString() : null,
                        r.getExamen().getParametro(),
                        r.getExamen().getUnidad(),
                        r.getValorObtenido(),
                        r.getExamen().getValorMinHombres(),
                        r.getExamen().getValorMaxHombres(),
                        r.getExamen().getValorMinMujeres(),
                        r.getExamen().getValorMaxMujeres()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(
            new APIResponse("Datos de exámenes global obtenidos", data, false, HttpStatus.OK)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private AgendaHoyItemDTO toAgendaDTO(Cita cita) {
        String horaInicio = formatHora(cita.getStartAtUtc());
        String horaFin    = formatHora(cita.getEndAtUtc());

        String folio          = "";
        String nombreCompleto = "";
        if (cita.getPaciente() != null && cita.getPaciente().getPersona() != null) {
            var persona = cita.getPaciente().getPersona();
            folio = cita.getPaciente().getFolio();
            nombreCompleto = (persona.getNombre()
                + " " + persona.getApellidoPaterno()
                + (persona.getApellidoMaterno() != null ? " " + persona.getApellidoMaterno() : "")
            ).trim();
        }

        return new AgendaHoyItemDTO(
            cita.getUuid(),
            horaInicio,
            horaFin,
            cita.getDurationMinutes(),
            cita.getEstadoCita() != null ? cita.getEstadoCita().name() : "",
            cita.getColorHex(),
            cita.getObservaciones(),
            new AgendaHoyItemDTO.PacienteResumen(folio, nombreCompleto)
        );
    }

    private String formatHora(Instant instant) {
        return instant != null ? instant.atZone(ZONA).format(FMT_HORA) : "--:--";
    }
}
