package imss.gob.mx.cohorte.controllers.citas;

import imss.gob.mx.cohorte.application.CitaApplicationService;
import imss.gob.mx.cohorte.controllers.citas.dto.*;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@AllArgsConstructor
@Tag(name = "Citas", description = "Gestión de citas médicas")
@SecurityRequirement(name = "bearerAuth")
public class CitaController {

    private final CitaApplicationService citaApplicationService;

    @GetMapping
    @Operation(summary = "Listar citas", description = "Obtiene una lista de citas, opcionalmente filtrada por rango de fechas (ISO-8601 UTC)")
    public ResponseEntity<APIResponse> getAll(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        
        List<Cita> citas;
        if (start != null && end != null) {
            citas = citaApplicationService.getByRange(Instant.parse(start), Instant.parse(end));
        } else {
            citas = citaApplicationService.getAll();
        }
        return ResponseEntity.ok(new APIResponse("Citas encontradas", CitaMapper.toResponseDTOList(citas), false, HttpStatus.OK));
    }

    @GetMapping("/paciente/{uuid}/resumen")
    @Operation(summary = "Resumen de citas por UUID de paciente",
               description = "Devuelve solo fecha (en zona local), tipo (observaciones) y estado de cada cita del paciente")
    public ResponseEntity<APIResponse> getResumenByPacienteUuid(@PathVariable String uuid) {
        List<Cita> citas = citaApplicationService.findAllByPacienteUuid(uuid);
        return ResponseEntity.ok(new APIResponse(
                "Citas del paciente", CitaResumenMapper.toResumenDTOList(citas), false, HttpStatus.OK));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Obtener cita por UUID")
    public ResponseEntity<APIResponse> getByUuid(@PathVariable String uuid) {
        Cita cita = citaApplicationService.findByUuid(uuid);
        return ResponseEntity.ok(new APIResponse("Cita encontrada", CitaMapper.toResponseDTO(cita), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Registrar nueva cita")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody CitaRequestDTO dto) {
        Cita cita = CitaMapper.toEntity(dto);
        Cita saved = citaApplicationService.save(cita);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Cita registrada exitosamente", CitaMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PatchMapping("/{uuid}")
    @Operation(summary = "Actualización parcial (PATCH) para drag&drop y ediciones")
    public ResponseEntity<APIResponse> patch(
            @PathVariable String uuid,
            @Validated @RequestBody CitaPatchDTO dto) {
        Cita updated = citaApplicationService.patch(uuid, dto);
        return ResponseEntity.ok(new APIResponse("Cita actualizada exitosamente", CitaMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{uuid}/cancelar")
    @Operation(summary = "Cancelar cita por UUID")
    public ResponseEntity<APIResponse> cancelar(@PathVariable String uuid) {
        citaApplicationService.cancelar(uuid);
        return ResponseEntity.ok(new APIResponse("Cita cancelada", null, false, HttpStatus.OK));
    }
}
