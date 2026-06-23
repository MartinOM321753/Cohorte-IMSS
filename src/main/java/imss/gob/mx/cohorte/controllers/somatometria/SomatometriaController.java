package imss.gob.mx.cohorte.controllers.somatometria;

import imss.gob.mx.cohorte.application.SomatometriaApplicationService;
import imss.gob.mx.cohorte.controllers.somatometria.dto.SomatometriaMapper;
import imss.gob.mx.cohorte.controllers.somatometria.dto.SomatometriaRequestDTO;
import imss.gob.mx.cohorte.controllers.somatometria.dto.SomatometriaResponseDTO;
import imss.gob.mx.cohorte.modules.somatometria.Somatometria;
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
@RequestMapping("/api/somatometria")
@AllArgsConstructor
@Tag(name = "Somatometría", description = "Historial de mediciones antropométricas y de signos vitales por paciente")
@SecurityRequirement(name = "bearerAuth")
public class SomatometriaController {

    private final SomatometriaApplicationService appService;

    // ── GET /api/somatometria/paciente/{uuid} ─────────────────────────────────
    @GetMapping("/paciente/{uuid}")
    @Operation(summary = "Historial de somatometría del paciente",
               description = "Devuelve todos los registros ordenados del más reciente al más antiguo")
    public ResponseEntity<APIResponse> getByPaciente(@PathVariable String uuid) {
        List<SomatometriaResponseDTO> list = appService.getHistorialByPaciente(uuid)
                .stream()
                .map(SomatometriaMapper::toDTO)
                .toList();
        return ResponseEntity.ok(new APIResponse("Historial encontrado", list, false, HttpStatus.OK));
    }

    // ── GET /api/somatometria/paciente/{uuid}/latest ───────────────────────────
    @GetMapping("/paciente/{uuid}/latest")
    @Operation(summary = "Última medición del paciente",
               description = "Devuelve el registro más reciente, o 404 si no hay ninguno")
    public ResponseEntity<APIResponse> getLatest(@PathVariable String uuid) {
        return appService.getLatest(uuid)
                .map(SomatometriaMapper::toDTO)
                .map(dto -> ResponseEntity.ok(new APIResponse("Última medición encontrada", dto, false, HttpStatus.OK)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new APIResponse("No hay registros de somatometría para este paciente", null, true, HttpStatus.NOT_FOUND)));
    }

    // ── GET /api/somatometria/{id} ────────────────────────────────────────────
    @GetMapping("/{id}")
    @Operation(summary = "Obtener registro por ID")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        Somatometria s = appService.getById(id);
        return ResponseEntity.ok(new APIResponse("Registro encontrado", SomatometriaMapper.toDTO(s), false, HttpStatus.OK));
    }

    // ── POST /api/somatometria ────────────────────────────────────────────────
    @PostMapping
    @Operation(summary = "Registrar medición",
               description = "Crea un nuevo registro de somatometría. El IMC se calcula automáticamente si se proporcionan peso y talla.")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody SomatometriaRequestDTO dto) {
        Somatometria saved = appService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Medición registrada", SomatometriaMapper.toDTO(saved), false, HttpStatus.CREATED));
    }

    // ── PUT /api/somatometria/{id} ────────────────────────────────────────────
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar registro de somatometría")
    public ResponseEntity<APIResponse> update(@PathVariable Long id,
                                              @Validated @RequestBody SomatometriaRequestDTO dto) {
        Somatometria updated = appService.update(id, dto);
        return ResponseEntity.ok(new APIResponse("Medición actualizada", SomatometriaMapper.toDTO(updated), false, HttpStatus.OK));
    }

    // ── DELETE /api/somatometria/{id} ─────────────────────────────────────────
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar registro de somatometría")
    public ResponseEntity<APIResponse> delete(@PathVariable Long id) {
        appService.delete(id);
        return ResponseEntity.ok(new APIResponse("Medición eliminada", null, false, HttpStatus.OK));
    }
}
