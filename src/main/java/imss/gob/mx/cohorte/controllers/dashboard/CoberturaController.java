package imss.gob.mx.cohorte.controllers.dashboard;

import imss.gob.mx.cohorte.controllers.dashboard.dto.*;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.ExamenRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

@RestController
@RequestMapping("/api/dashboard/cobertura")
@AllArgsConstructor
@Tag(name = "Cobertura", description = "Completitud de la cohorte por examen y tipo de estudio")
@SecurityRequirement(name = "bearerAuth")
@RequireModulo(ModuloSistema.COBERTURA)
public class CoberturaController {

    private final PacienteRepository         pacienteRepository;
    private final ExamenRepository           examenRepository;
    private final TipoEstudioRepository      tipoEstudioRepository;
    private final ResultadoExamenRepository  resultadoExamenRepository;
    private final EstudioMedicoRepository    estudioMedicoRepository;
    private final InstitucionContextService  institucionContextService;

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/cobertura/examenes
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/examenes")
    @Operation(summary = "Cobertura por examen",
               description = "Para cada examen activo: cuántos pacientes activos lo tienen registrado.")
    public ResponseEntity<APIResponse> getCoberturaExamenes() {

        long idInstitucion = institucionContextService.getIdInstitucionActual();

        long total = pacienteRepository.countByActivoAndInstitucion_Id(true, idInstitucion);
        List<Examen> examenes = examenRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion);

        List<CoberturaItemDTO> result = examenes.stream()
                .map(e -> {
                    long con = resultadoExamenRepository.countDistinctPacienteByExamenId(e.getId(), idInstitucion);
                    long sin = total - con;
                    int  pct = total > 0 ? (int) Math.round(con * 100.0 / total) : 0;
                    return new CoberturaItemDTO(e.getId(), e.getParametro(), total, con, 0L, sin, pct);
                })
                .sorted(Comparator.comparingInt(CoberturaItemDTO::pct))   // menos cubiertos primero
                .collect(Collectors.toList());

        return ResponseEntity.ok(new APIResponse("Cobertura de exámenes", result, false, HttpStatus.OK));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/cobertura/estudios
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/estudios")
    @Operation(summary = "Cobertura por tipo de estudio",
               description = "Para cada tipo de estudio activo: cuántos pacientes activos lo tienen registrado.")
    public ResponseEntity<APIResponse> getCoberturaEstudios() {

        long idInstitucion = institucionContextService.getIdInstitucionActual();

        long total = pacienteRepository.countByActivoAndInstitucion_Id(true, idInstitucion);
        List<TipoEstudio> tipos = tipoEstudioRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion);

        List<CoberturaItemDTO> result = tipos.stream()
                .map(t -> {
                    long con = estudioMedicoRepository.countDistinctPacienteByTipoEstudioId(t.getId(), idInstitucion);
                    long sin = total - con;
                    int  pct = total > 0 ? (int) Math.round(con * 100.0 / total) : 0;
                    return new CoberturaItemDTO(t.getId(), t.getNombre(), total, con, 0L, sin, pct);
                })
                .sorted(Comparator.comparingInt(CoberturaItemDTO::pct))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new APIResponse("Cobertura de estudios", result, false, HttpStatus.OK));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/cobertura/distribucion?tipo=EXAMEN|ESTUDIO
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/distribucion")
    @Operation(summary = "Distribución de completitud",
               description = "Cuántos pacientes tienen exactamente k tipos cubiertos (k = 0…N).")
    public ResponseEntity<APIResponse> getDistribucion(@RequestParam String tipo) {

        long idInstitucion = institucionContextService.getIdInstitucionActual();

        boolean esExamen = "EXAMEN".equalsIgnoreCase(tipo);

        int totalTipos = esExamen
                ? examenRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).size()
                : tipoEstudioRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).size();

        long totalPacientes = pacienteRepository.countByActivoAndInstitucion_Id(true, idInstitucion);

        // Obtener el mapa pacienteId → conteo para quienes tienen ≥ 1
        List<Object[]> rows = esExamen
                ? resultadoExamenRepository.countDistinctExamenByPacienteActivo(idInstitucion)
                : estudioMedicoRepository.countDistinctTipoByPacienteActivo(idInstitucion);

        Map<Long, Long> conteoMap = new HashMap<>();
        for (Object[] row : rows) {
            Long pid    = ((Number) row[0]).longValue();
            Long conteo = ((Number) row[1]).longValue();
            conteoMap.put(pid, conteo);
        }

        // Todos los pacientes activos
        List<Paciente> pacientesActivos = pacienteRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion);

        // Frecuencia de cada k
        Map<Integer, Long> freq = new TreeMap<>();
        for (Paciente p : pacientesActivos) {
            int k = conteoMap.getOrDefault(p.getId(), 0L).intValue();
            freq.merge(k, 1L, Long::sum);
        }

        // Asegurar que todos los k de 0 a totalTipos estén representados
        List<DistribucionBucketDTO> result = new ArrayList<>();
        for (int k = 0; k <= totalTipos; k++) {
            long cant = freq.getOrDefault(k, 0L);
            if (cant > 0 || k == 0 || k == totalTipos) {
                result.add(new DistribucionBucketDTO(k, cant, totalTipos));
            }
        }

        return ResponseEntity.ok(new APIResponse("Distribución de completitud", result, false, HttpStatus.OK));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/cobertura/pendientes?tipoId={id}&catalogoTipo=EXAMEN|ESTUDIO
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/pendientes")
    @Operation(summary = "Pacientes pendientes de un tipo",
               description = "Pacientes activos que aún no tienen el examen o estudio indicado.")
    public ResponseEntity<APIResponse> getPendientes(
            @RequestParam Long tipoId,
            @RequestParam String catalogoTipo) {

        long idInstitucion = institucionContextService.getIdInstitucionActual();

        boolean esExamen = "EXAMEN".equalsIgnoreCase(catalogoTipo);
        int totalTipos = esExamen
                ? examenRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).size()
                : tipoEstudioRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).size();

        List<Long> ids = esExamen
                ? resultadoExamenRepository.findPacientesActivosSinExamen(tipoId, idInstitucion)
                : estudioMedicoRepository.findPacientesActivosSinTipoEstudio(tipoId, idInstitucion);

        List<PacientePendienteDTO> result = buildPacientePendientes(ids, esExamen, totalTipos);
        return ResponseEntity.ok(new APIResponse("Participantes pendientes", result, false, HttpStatus.OK));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/cobertura/grupo?cantidadTipos={k}&catalogoTipo=EXAMEN|ESTUDIO
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/grupo")
    @Operation(summary = "Pacientes de un bucket de completitud",
               description = "Pacientes activos que tienen exactamente k tipos cubiertos.")
    public ResponseEntity<APIResponse> getGrupo(
            @RequestParam int cantidadTipos,
            @RequestParam String catalogoTipo) {

        long idInstitucion = institucionContextService.getIdInstitucionActual();

        boolean esExamen = "EXAMEN".equalsIgnoreCase(catalogoTipo);
        int totalTipos = esExamen
                ? examenRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).size()
                : tipoEstudioRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).size();

        List<Long> ids;
        if (cantidadTipos == 0) {
            // pacientes activos sin ningún resultado
            List<Long> conAlguno = esExamen
                    ? resultadoExamenRepository.countDistinctExamenByPacienteActivo(idInstitucion)
                              .stream().map(r -> ((Number) r[0]).longValue()).collect(Collectors.toList())
                    : estudioMedicoRepository.countDistinctTipoByPacienteActivo(idInstitucion)
                              .stream().map(r -> ((Number) r[0]).longValue()).collect(Collectors.toList());
            Set<Long> conAlgunoSet = new HashSet<>(conAlguno);
            ids = pacienteRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).stream()
                    .map(Paciente::getId)
                    .filter(pid -> !conAlgunoSet.contains(pid))
                    .collect(Collectors.toList());
        } else {
            ids = esExamen
                    ? resultadoExamenRepository.findPacientesConExactamenteKExamenes(cantidadTipos, idInstitucion)
                    : estudioMedicoRepository.findPacientesConExactamenteKEstudios(cantidadTipos, idInstitucion);
        }

        List<PacientePendienteDTO> result = buildPacientePendientes(ids, esExamen, totalTipos);
        return ResponseEntity.ok(new APIResponse("Participantes del grupo", result, false, HttpStatus.OK));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET /api/dashboard/cobertura/matriz?catalogoTipo=EXAMEN|ESTUDIO&limit=100
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/matriz")
    @Operation(summary = "Matriz paciente × tipo",
               description = "Hasta 100 pacientes ordenados por menor cobertura, con estado por celda.")
    public ResponseEntity<APIResponse> getMatriz(
            @RequestParam String catalogoTipo,
            @RequestParam(defaultValue = "100") int limit) {

        long idInstitucion = institucionContextService.getIdInstitucionActual();

        boolean esExamen = "EXAMEN".equalsIgnoreCase(catalogoTipo);

        // IDs de tipos activos ordenados alfabéticamente
        List<Long> tipoIds;
        if (esExamen) {
            tipoIds = examenRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).stream()
                    .sorted(Comparator.comparing(Examen::getParametro))
                    .map(Examen::getId)
                    .collect(Collectors.toList());
        } else {
            tipoIds = tipoEstudioRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).stream()
                    .sorted(Comparator.comparing(TipoEstudio::getNombre))
                    .map(TipoEstudio::getId)
                    .collect(Collectors.toList());
        }
        int totalTipos = tipoIds.size();

        // Mapa pacienteId → conteo de tipos cubiertos
        List<Object[]> rows = esExamen
                ? resultadoExamenRepository.countDistinctExamenByPacienteActivo(idInstitucion)
                : estudioMedicoRepository.countDistinctTipoByPacienteActivo(idInstitucion);
        Map<Long, Long> conteoMap = new HashMap<>();
        for (Object[] r : rows) {
            conteoMap.put(((Number) r[0]).longValue(), ((Number) r[1]).longValue());
        }

        // Tomar los primeros `limit` pacientes activos ordenados por menor cobertura
        List<Paciente> pacientesActivos = pacienteRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion).stream()
                .sorted(Comparator.comparingLong(p -> conteoMap.getOrDefault(p.getId(), 0L)))
                .limit(limit)
                .collect(Collectors.toList());

        List<CoberturaPacienteDTO> result = new ArrayList<>();
        for (Paciente p : pacientesActivos) {
            // IDs de tipos cubiertos por este paciente
            Set<Long> cubiertos;
            if (esExamen) {
                cubiertos = new HashSet<>(resultadoExamenRepository.findExamenesCubiertosIdsForPaciente(p.getId()));
            } else {
                cubiertos = new HashSet<>(estudioMedicoRepository.findTiposEstudioCubiertosIdsForPaciente(p.getId()));
            }

            List<CeldaCoberturaDTO> celdas = tipoIds.stream()
                    .map(tid -> new CeldaCoberturaDTO(tid, cubiertos.contains(tid) ? "HECHO" : "FALTA"))
                    .collect(Collectors.toList());

            Persona persona = p.getPersona();
            String nombre = persona != null
                    ? (persona.getApellidoPaterno() + ", " + persona.getNombre())
                    : p.getFolio();
            String sexo = (persona != null && persona.getSexo() != null)
                    ? persona.getSexo().name()
                    : "";

            result.add(new CoberturaPacienteDTO(
                    p.getFolio(),
                    nombre,
                    sexo,
                    cubiertos.size(),
                    totalTipos,
                    celdas
            ));
        }

        return ResponseEntity.ok(new APIResponse("Matriz de cobertura", result, false, HttpStatus.OK));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helper
    // ─────────────────────────────────────────────────────────────────────────
    private List<PacientePendienteDTO> buildPacientePendientes(
            List<Long> ids, boolean esExamen, int totalTipos) {

        return ids.stream()
                .map(pid -> pacienteRepository.findById(pid).orElse(null))
                .filter(Objects::nonNull)
                .map(p -> {
                    long cob = esExamen
                            ? resultadoExamenRepository.countDistinctExamenByPacienteId(p.getId())
                            : estudioMedicoRepository.findTiposEstudioCubiertosIdsForPaciente(p.getId()).size();

                    Persona persona = p.getPersona();
                    String nombre = persona != null
                            ? (persona.getApellidoPaterno()
                               + (persona.getApellidoMaterno() != null ? " " + persona.getApellidoMaterno() : "")
                               + ", " + persona.getNombre())
                            : p.getFolio();
                    String sexo = (persona != null && persona.getSexo() != null)
                            ? persona.getSexo().name()
                            : "";

                    return new PacientePendienteDTO(p.getFolio(), nombre, sexo, (int) cob, totalTipos);
                })
                .sorted(Comparator.comparingInt(PacientePendienteDTO::coberturaTotal))
                .collect(Collectors.toList());
    }
}
