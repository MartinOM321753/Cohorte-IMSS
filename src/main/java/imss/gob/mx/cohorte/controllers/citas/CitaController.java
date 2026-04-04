package imss.gob.mx.cohorte.controllers.citas;

import imss.gob.mx.cohorte.controllers.citas.dto.CitaMapper;
import imss.gob.mx.cohorte.controllers.citas.dto.CitaRequestDTO;
import imss.gob.mx.cohorte.controllers.citas.dto.CitaResponseDTO;
import imss.gob.mx.cohorte.controllers.citas.dto.CitaUpdateRequestDTO;
import imss.gob.mx.cohorte.application.CitaApplicationService;
import imss.gob.mx.cohorte.modules.cita.Cita;
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
@RequestMapping("/api/citas")
@AllArgsConstructor
@Tag(name = "Citas", description = "Gestión de citas médicas")
@SecurityRequirement(name = "bearerAuth")
public class CitaController {

    private final CitaApplicationService citaApplicationService;

    @GetMapping
    @Operation(summary = "Listar todas las citas")
    public ResponseEntity<APIResponse> getAll() {
        List<Cita> citas = citaApplicationService.getAll();
        return ResponseEntity.ok(new APIResponse("Citas encontradas", CitaMapper.toResponseDTOList(citas), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cita por ID")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        Cita cita = citaApplicationService.getById(id);
        return ResponseEntity.ok(new APIResponse("Cita encontrada", CitaMapper.toResponseDTO(cita), false, HttpStatus.OK));
    }

    @GetMapping("/paciente/uuid/{uuid}")
    @Operation(summary = "Obtener citas de un paciente por UUID")
    public ResponseEntity<APIResponse> getByPacienteUUID(@PathVariable String uuid) {
        List<Cita> citas = citaApplicationService.findByPacienteUUID(uuid);
        return ResponseEntity.ok(new APIResponse("Citas del paciente encontradas", CitaMapper.toResponseDTOList(citas), false, HttpStatus.OK));
    }

    @GetMapping("/paciente/folio/{folio}")
    @Operation(summary = "Obtener citas de un paciente por folio")
    public ResponseEntity<APIResponse> getByPacienteFolio(@PathVariable String folio) {
        List<Cita> citas = citaApplicationService.findByPacienteFolio(folio);
        return ResponseEntity.ok(new APIResponse("Citas del paciente encontradas", CitaMapper.toResponseDTOList(citas), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Registrar nueva cita")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody CitaRequestDTO dto) {
        Cita cita = CitaMapper.toEntity(dto);
        Cita saved = citaApplicationService.save(cita);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Cita registrada exitosamente", CitaMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cita")
    public ResponseEntity<APIResponse> update(@PathVariable Long id, @Validated @RequestBody CitaUpdateRequestDTO dto) {
        Cita updated = citaApplicationService.update(id, dto.getFechaCita(), dto.getDuracionMinutos(),
            Cita.EstadoCita.valueOf(dto.getEstadoCita()), dto.getObservaciones());
        return ResponseEntity.ok(new APIResponse("Cita actualizada", CitaMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar cita")
    public ResponseEntity<APIResponse> cancelar(@PathVariable Long id) {
        citaApplicationService.cancelar(id);
        return ResponseEntity.ok(new APIResponse("Cita cancelada", null, false, HttpStatus.OK));
    }
}
