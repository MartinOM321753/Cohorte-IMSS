package imss.gob.mx.cohorte.controllers.dashboard;

import imss.gob.mx.cohorte.controllers.dashboard.dto.*;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenicaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCajaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.RefrigeradorRepository;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.cita.CitaRepository;
import imss.gob.mx.cohorte.modules.documentos.PacienteDocumentoRepository;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.modules.somatometria.SomatometriaRepository;
import imss.gob.mx.cohorte.utils.APIResponse;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@AllArgsConstructor
@Tag(name = "Dashboard", description = "Métricas resumidas y agenda del día para el panel de control")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    /** Zona horaria del sistema (México). */
    private static final ZoneId            ZONA     = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final PacienteRepository          pacienteRepository;
    private final MuestraRepository           muestraRepository;
    private final CitaRepository              citaRepository;
    private final EstudioMedicoRepository     estudioMedicoRepository;
    private final ResultadoExamenRepository   resultadoExamenRepository;
    private final PacienteDocumentoRepository pacienteDocumentoRepository;
    private final SomatometriaRepository      somatometriaRepository;
    private final RefrigeradorRepository      refrigeradorRepository;
    private final CajaCriogenicaRepository    cajaCriogenicaRepository;
    private final PosicionCajaRepository      posicionCajaRepository;
    private final InstitucionContextService   institucionContextService;

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/stats
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    @Operation(
        summary     = "Estadísticas del dashboard (ampliadas)",
        description = "Retorna conteos del mes en curso: pacientes activos, citas, estudios, " +
                      "exámenes, muestras, documentos y delta semanal de pacientes."
    )
    public ResponseEntity<APIResponse> getStats() {

        long idInstitucion = institucionContextService.getIdInstitucionActual();

        // Rango mes actual (UTC → instants)
        YearMonth   mesActual  = YearMonth.now(ZONA);
        LocalDate   primero    = mesActual.atDay(1);
        LocalDate   ultimo     = mesActual.atEndOfMonth();
        Instant     inicioMes  = primero.atStartOfDay(ZONA).toInstant();
        Instant     finMes     = ultimo.atTime(23, 59, 59).atZone(ZONA).toInstant();
        Instant     ahora      = Instant.now();

        // 1. Pacientes activos
        long pacientesActivos = pacienteRepository.countByActivoAndInstitucion_Id(true, idInstitucion);

        // 2. Citas del mes (no canceladas)
        long citasMes = citaRepository.countCitasMes(inicioMes, finMes, idInstitucion);

        // 3. Citas sin actualizar (ya terminaron y siguen en Programada/Confirmada)
        long citasSinActualizar = citaRepository.countCitasSinActualizar(ahora, idInstitucion);

        // 4. Estudios con resultado este mes
        long estudiosConResultadosMes = estudioMedicoRepository
                .countEstudiosConResultadosEnMes(primero, ultimo, idInstitucion);

        // 5. Exámenes de laboratorio este mes
        LocalDateTime inicioMesLdt = primero.atStartOfDay();
        LocalDateTime finMesLdt    = ultimo.atTime(23, 59, 59);
        long examenesLabMes = resultadoExamenRepository
                .countByFechaResultadoBetween(inicioMesLdt, finMesLdt, idInstitucion);

        // 6. Muestras biobanco
        long muestrasBiobanco = muestraRepository.countByInstitucion_Id(idInstitucion);

        // 7. Documentos generales de paciente
        long documentosGenerales = pacienteDocumentoRepository.countByPaciente_Institucion_Id(idInstitucion);

        // 8. Delta pacientes: activos ahora vs. activos hace 7 días
        //    Se aproxima usando la diferencia de registros en los últimos 7 días.
        //    (En producción se podría guardar snapshot; aquí es la variación contable.)
        int deltasPacientes = 0; // placeholder — se implementará con snapshot cuando haya auditoria

        DashboardStatsDTO stats = new DashboardStatsDTO(
                pacientesActivos,
                citasMes,
                citasSinActualizar,
                estudiosConResultadosMes,
                examenesLabMes,
                muestrasBiobanco,
                documentosGenerales,
                deltasPacientes
        );

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
        description = "Retorna las citas no canceladas para el día de hoy, ordenadas por hora de inicio."
    )
    public ResponseEntity<APIResponse> getAgendaHoy() {

        LocalDate hoy      = LocalDate.now(ZONA);
        Instant   inicioDia = hoy.atStartOfDay(ZONA).toInstant();
        Instant   finDia    = hoy.plusDays(1).atStartOfDay(ZONA).toInstant();

        List<AgendaHoyItemDTO> agenda = citaRepository
            .findCitasHoy(inicioDia, finDia, institucionContextService.getIdInstitucionActual())
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
        description = "Retorna todos los registros de somatometría ordenados por fecha ascendente."
    )
    public ResponseEntity<APIResponse> getSomatometriaGlobal() {

        List<SomatometriaGlobalDTO> data = somatometriaRepository
                .findAllByInstitucionOrderByFechaMedicionAsc(institucionContextService.getIdInstitucionActual())
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
        description = "Retorna todos los resultados de exámenes ordenados por fecha ascendente."
    )
    public ResponseEntity<APIResponse> getExamenesGlobal() {

        List<ExamenResultGlobalDTO> data = resultadoExamenRepository
                .findAllWithExamenAndPaciente(institucionContextService.getIdInstitucionActual())
                .stream()
                .sorted(Comparator.comparing(ResultadoExamen::getFechaResultado,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
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
    //  GET /api/dashboard/examenes-calidad
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/examenes-calidad")
    @Operation(
        summary     = "Calidad de resultados de exámenes",
        description = "Clasifica todos los resultados en: en rango, fuera de rango, sin referencia."
    )
    public ResponseEntity<APIResponse> getExamenesCalidad() {

        List<ResultadoExamen> resultados = resultadoExamenRepository
                .findAllWithExamenAndPaciente(institucionContextService.getIdInstitucionActual());

        long enRango       = 0;
        long fueraDeRango  = 0;
        long sinReferencia = 0;

        for (ResultadoExamen r : resultados) {
            Examen examen = r.getExamen();
            if (examen == null || r.getValorObtenido() == null) {
                sinReferencia++;
                continue;
            }

            // Determinar el sexo del paciente
            Persona.Sexo sexo = null;
            if (r.getPaciente() != null && r.getPaciente().getPersona() != null) {
                sexo = r.getPaciente().getPersona().getSexo();
            }

            Double min = null;
            Double max = null;

            if (Persona.Sexo.M.equals(sexo)) {
                min = examen.getValorMinHombres();
                max = examen.getValorMaxHombres();
            } else if (Persona.Sexo.F.equals(sexo)) {
                min = examen.getValorMinMujeres();
                max = examen.getValorMaxMujeres();
            } else {
                // Sin sexo definido: usar cualquiera disponible
                min = examen.getValorMinHombres() != null ? examen.getValorMinHombres() : examen.getValorMinMujeres();
                max = examen.getValorMaxHombres() != null ? examen.getValorMaxHombres() : examen.getValorMaxMujeres();
            }

            if (min == null || max == null) {
                sinReferencia++;
            } else if (r.getValorObtenido() < min || r.getValorObtenido() > max) {
                fueraDeRango++;
            } else {
                enRango++;
            }
        }

        return ResponseEntity.ok(
            new APIResponse("Calidad de exámenes calculada",
                new ExamenesCalidadDTO(enRango, fueraDeRango, sinReferencia),
                false, HttpStatus.OK)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/biobanco-ocupacion
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/biobanco-ocupacion")
    @Operation(
        summary     = "Ocupación de refrigeradores del biobanco",
        description = "Devuelve el porcentaje de ocupación de cada refrigerador activo, ordenado por % DESC."
    )
    public ResponseEntity<APIResponse> getBiobancoOcupacion() {

        List<Refrigerador> refrigeradores = refrigeradorRepository
                .findAllByActivoAndInstitucion_Id(true, institucionContextService.getIdInstitucionActual());

        List<RefrigeradorOcupacionDTO> result = new ArrayList<>();

        for (Refrigerador ref : refrigeradores) {
            long totalRef   = 0;
            long ocupadasRef = 0;
            List<RefrigeradorOcupacionDTO.PisoResumen> pisosDTO = new ArrayList<>();

            if (ref.getPisos() != null) {
                for (PisoRefrigerador piso : ref.getPisos()) {
                    long pisoTotal    = 0;
                    long pisoOcupadas = 0;

                    if (piso.getPosiciones() != null) {
                        for (PosicionPiso pos : piso.getPosiciones()) {
                            pisoTotal++;
                            if (Boolean.TRUE.equals(pos.getOcupada())) pisoOcupadas++;
                        }
                    }

                    int pisoPct = pisoTotal > 0
                            ? (int) Math.round(pisoOcupadas * 100.0 / pisoTotal) : 0;

                    pisosDTO.add(new RefrigeradorOcupacionDTO.PisoResumen(
                            piso.getId(),
                            piso.getNumeroPiso(),
                            pisoTotal,
                            pisoOcupadas,
                            pisoPct
                    ));

                    totalRef    += pisoTotal;
                    ocupadasRef += pisoOcupadas;
                }
            }

            // Ordenar pisos por nombre/número ascendente
            pisosDTO.sort(Comparator.comparing(RefrigeradorOcupacionDTO.PisoResumen::numeroPiso));

            int pct = totalRef > 0 ? (int) Math.round(ocupadasRef * 100.0 / totalRef) : 0;
            result.add(new RefrigeradorOcupacionDTO(
                    ref.getId(),
                    ref.getNombre() != null ? ref.getNombre() : ref.getCodigo(),
                    totalRef,
                    ocupadasRef,
                    pct,
                    pisosDTO
            ));
        }

        result.sort(Comparator.comparingInt(RefrigeradorOcupacionDTO::pct).reversed());

        return ResponseEntity.ok(
            new APIResponse("Ocupación de biobanco obtenida", result, false, HttpStatus.OK)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/biobanco-cajas
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/biobanco-cajas")
    @Operation(
        summary     = "Ocupación de cajas criogénicas del biobanco",
        description = "Devuelve el porcentaje de ocupación de cada caja, ordenado por % DESC."
    )
    public ResponseEntity<APIResponse> getBiobancoOcupacionCajas() {

        List<CajaCriogenica> cajas = cajaCriogenicaRepository
                .findAllByInstitucion_Id(institucionContextService.getIdInstitucionActual());

        List<CajaOcupacionDTO> result = cajas.stream().map(caja -> {
            // Usar el conteo real de posiciones creadas en DB, no filas*columnas
            List<?> todasPosiciones = posicionCajaRepository.findAllByCaja_Id(caja.getId());
            long total    = todasPosiciones.size();
            long ocupadas = posicionCajaRepository
                    .findAllByCaja_IdAndOcupada(caja.getId(), true)
                    .size();
            int pct = total > 0 ? (int) Math.round(ocupadas * 100.0 / total) : 0;
            return new CajaOcupacionDTO(
                    caja.getId(),
                    caja.getCodigoCaja(),
                    caja.getTipoCaja(),
                    total,
                    ocupadas,
                    pct
            );
        })
        .sorted(Comparator.comparingInt(CajaOcupacionDTO::pct).reversed())
        .collect(Collectors.toList());

        return ResponseEntity.ok(
            new APIResponse("Ocupación de cajas obtenida", result, false, HttpStatus.OK)
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
