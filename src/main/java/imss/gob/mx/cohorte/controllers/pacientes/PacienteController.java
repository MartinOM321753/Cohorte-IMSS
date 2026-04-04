package imss.gob.mx.cohorte.controllers.pacientes;

import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteMapper;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteRequestDTO;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteResponseDTO;
import imss.gob.mx.cohorte.application.PacienteApplicationService;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
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
@RequestMapping("/api/pacientes")
@AllArgsConstructor
@Tag(name = "Pacientes", description = "Gestión de pacientes de la cohorte")
@SecurityRequirement(name = "bearerAuth")
public class PacienteController {

    private final PacienteApplicationService pacienteApplicationService;

    @GetMapping
    @Operation(summary = "Listar todos los pacientes")
    public ResponseEntity<APIResponse> getAll() {
        List<Paciente> pacientes = pacienteApplicationService.getAll();
        return ResponseEntity.ok(new APIResponse("Pacientes encontrados", PacienteMapper.toResponseDTOList(pacientes), false, HttpStatus.OK));
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar pacientes activos")
    public ResponseEntity<APIResponse> getActivos() {
        List<Paciente> pacientes = pacienteApplicationService.getActivos();
        return ResponseEntity.ok(new APIResponse("Pacientes activos encontrados", PacienteMapper.toResponseDTOList(pacientes), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener paciente por ID")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        Paciente paciente = pacienteApplicationService.getById(id);
        return ResponseEntity.ok(new APIResponse("Paciente encontrado", PacienteMapper.toResponseDTO(paciente), false, HttpStatus.OK));
    }

    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "Obtener paciente por UUID")
    public ResponseEntity<APIResponse> getByUUID(@PathVariable String uuid) {
        Paciente paciente = pacienteApplicationService.getByUUID(uuid);
        return ResponseEntity.ok(new APIResponse("Paciente encontrado", PacienteMapper.toResponseDTO(paciente), false, HttpStatus.OK));
    }

    @GetMapping("/folio/{folio}")
    @Operation(summary = "Obtener paciente por folio")
    public ResponseEntity<APIResponse> getByFolio(@PathVariable String folio) {
        Paciente paciente = pacienteApplicationService.getByFolio(folio);
        return ResponseEntity.ok(new APIResponse("Paciente encontrado", PacienteMapper.toResponseDTO(paciente), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo paciente")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody PacienteRequestDTO dto) {
        Paciente paciente = PacienteMapper.toEntity(dto);
        Paciente saved = pacienteApplicationService.save(paciente);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Paciente registrado exitosamente", PacienteMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar paciente")
    public ResponseEntity<APIResponse> update(@PathVariable Long id, @Validated @RequestBody PacienteRequestDTO dto) {
        Paciente paciente = PacienteMapper.toEntity(dto);
        paciente.setId(id);
        Paciente updated = pacienteApplicationService.update(paciente);
        return ResponseEntity.ok(new APIResponse("Paciente actualizado", PacienteMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }
}
