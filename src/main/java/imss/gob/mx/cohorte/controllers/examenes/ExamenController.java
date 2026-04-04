package imss.gob.mx.cohorte.controllers.examenes;

import imss.gob.mx.cohorte.application.ExamenApplicationService;
import imss.gob.mx.cohorte.controllers.examenes.dto.ExamenMapper;
import imss.gob.mx.cohorte.controllers.examenes.dto.ExamenRequestDTO;
import imss.gob.mx.cohorte.controllers.examenes.dto.ExamenResponseDTO;
import imss.gob.mx.cohorte.controllers.examenes.dto.ResultadoExamenMapper;
import imss.gob.mx.cohorte.controllers.examenes.dto.ResultadoExamenRequestDTO;
import imss.gob.mx.cohorte.controllers.examenes.dto.ResultadoExamenResponseDTO;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examenes")
@AllArgsConstructor
@Tag(name = "Exámenes", description = "Gestión de exámenes y resultados de laboratorio")
@SecurityRequirement(name = "bearerAuth")
public class ExamenController {

    private final ExamenApplicationService examenApplicationService;

    @GetMapping
    @Operation(summary = "Listar todos los exámenes activos")
    public ResponseEntity<APIResponse> getAll() {
        List<Examen> examenes = examenApplicationService.findAll();
        return ResponseEntity.ok(new APIResponse("Exámenes encontrados", ExamenMapper.toResponseDTOList(examenes), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener examen por ID")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        Examen examen = examenApplicationService.findOne(id);
        return ResponseEntity.ok(new APIResponse("Examen encontrado", ExamenMapper.toResponseDTO(examen), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo examen")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody ExamenRequestDTO dto) {
        Examen examen = ExamenMapper.toEntity(dto);
        Examen saved = examenApplicationService.create(examen);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Examen registrado exitosamente", ExamenMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar examen")
    public ResponseEntity<APIResponse> update(@PathVariable Long id, @Validated @RequestBody ExamenRequestDTO dto) {
        Examen examen = ExamenMapper.toEntity(dto);
        examen.setId(id);
        Examen updated = examenApplicationService.update(examen);
        return ResponseEntity.ok(new APIResponse("Examen actualizado", ExamenMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @GetMapping("/resultados/paciente/uuid/{uuid}")
    @Operation(summary = "Obtener resultados de examen por UUID del paciente")
    public ResponseEntity<APIResponse> getResultadosByPacienteUUID(@PathVariable String uuid) {
        List<ResultadoExamen> resultados = examenApplicationService.findAllResultadoByUUID(uuid);
        return ResponseEntity.ok(new APIResponse("Resultados encontrados", ResultadoExamenMapper.toResponseDTOList(resultados), false, HttpStatus.OK));
    }

    @GetMapping("/resultados/paciente/folio/{folio}")
    @Operation(summary = "Obtener resultados de examen por folio del paciente")
    public ResponseEntity<APIResponse> getResultadosByPacienteFolio(@PathVariable String folio) {
        List<ResultadoExamen> resultados = examenApplicationService.findAllResultadoByFolio(folio);
        return ResponseEntity.ok(new APIResponse("Resultados encontrados", ResultadoExamenMapper.toResponseDTOList(resultados), false, HttpStatus.OK));
    }

    @PostMapping("/resultados")
    @Operation(summary = "Registrar resultado de examen")
    public ResponseEntity<APIResponse> saveResultado(@Validated @RequestBody ResultadoExamenRequestDTO dto) {
        ResultadoExamen resultado = ResultadoExamenMapper.toEntity(dto);
        ResultadoExamen saved = examenApplicationService.createResultado(resultado);
        ExamenResponseDTO examenDTO = ExamenMapper.toResponseDTO(saved.getExamen());
        Boolean dentroDeRango = calcularDentroDeRango(saved);
        ResultadoExamenResponseDTO responseDTO = ResultadoExamenMapper.toResponseDTO(saved, examenDTO, dentroDeRango);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Resultado registrado exitosamente", responseDTO, false, HttpStatus.CREATED));
    }

    @PutMapping("/resultados/{id}")
    @Operation(summary = "Actualizar resultado de examen")
    public ResponseEntity<APIResponse> updateResultado(@PathVariable Long id, @Validated @RequestBody ResultadoExamenRequestDTO dto) {
        ResultadoExamen resultado = ResultadoExamenMapper.toEntity(dto);
        resultado.setId(id);
        ResultadoExamen updated = examenApplicationService.updateResultado(resultado);
        ExamenResponseDTO examenDTO = ExamenMapper.toResponseDTO(updated.getExamen());
        Boolean dentroDeRango = calcularDentroDeRango(updated);
        ResultadoExamenResponseDTO responseDTO = ResultadoExamenMapper.toResponseDTO(updated, examenDTO, dentroDeRango);
        return ResponseEntity.ok(new APIResponse("Resultado actualizado", responseDTO, false, HttpStatus.OK));
    }

    private Boolean calcularDentroDeRango(ResultadoExamen resultado) {
        Double valor = resultado.getValorObtenido();
        Examen examen = resultado.getExamen();
        if (valor == null || examen == null) return null;
        Double min = examen.getValorMinMujeres();
        Double max = examen.getValorMaxMujeres();
        if (min != null && valor < min) return false;
        if (max != null && valor > max) return false;
        return true;
    }
}
