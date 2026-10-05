package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.ProtocoloApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ProtocoloMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ProtocoloRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.TuboProtocoloRequestDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.Protocolo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.TuboProtocolo;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/muestras/protocolos")
@AllArgsConstructor
@Tag(name = "Protocolos", description = "Plantillas de procesamiento: tubos primarios y qué se le hace a cada uno")
@SecurityRequirement(name = "bearerAuth")
public class ProtocoloController {

    private final ProtocoloApplicationService protocoloApplicationService;

    // ── Protocolo ──────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Listar protocolos activos")
    public ResponseEntity<APIResponse> getAllActivos() {
        List<Protocolo> list = protocoloApplicationService.getAllActivos();
        return ResponseEntity.ok(new APIResponse("Protocolos encontrados",
                ProtocoloMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos los protocolos (activos e inactivos)")
    public ResponseEntity<APIResponse> getAll() {
        List<Protocolo> list = protocoloApplicationService.getAll();
        return ResponseEntity.ok(new APIResponse("Protocolos encontrados",
                ProtocoloMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener protocolo por ID")
    public ResponseEntity<APIResponse> getById(
            @Parameter(description = "ID del protocolo") @PathVariable Long id) {
        Protocolo p = protocoloApplicationService.getById(id);
        return ResponseEntity.ok(new APIResponse("Protocolo encontrado",
                ProtocoloMapper.toResponseDTO(p), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear protocolo")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody ProtocoloRequestDTO dto) {
        Protocolo p = ProtocoloMapper.toEntity(dto);
        Protocolo saved = protocoloApplicationService.create(p, dto.getIdTipoOrigen());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Protocolo creado",
                        ProtocoloMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar protocolo")
    public ResponseEntity<APIResponse> update(
            @Parameter(description = "ID del protocolo") @PathVariable Long id,
            @Validated @RequestBody ProtocoloRequestDTO dto) {
        Protocolo datos = ProtocoloMapper.toEntity(dto);
        Protocolo updated = protocoloApplicationService.update(id, datos, dto.getIdTipoOrigen());
        return ResponseEntity.ok(new APIResponse("Protocolo actualizado",
                ProtocoloMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Activar / Desactivar protocolo")
    public ResponseEntity<APIResponse> toggle(
            @Parameter(description = "ID del protocolo") @PathVariable Long id) {
        Protocolo toggled = protocoloApplicationService.toggleActivo(id);
        String mensaje = toggled.getActivo() ? "Protocolo activado" : "Protocolo desactivado";
        return ResponseEntity.ok(new APIResponse(mensaje,
                ProtocoloMapper.toResponseDTO(toggled), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar protocolo", description = "Solo si no tiene tubos configurados.")
    public ResponseEntity<APIResponse> delete(
            @Parameter(description = "ID del protocolo") @PathVariable Long id) {
        protocoloApplicationService.deleteProtocolo(id);
        return ResponseEntity.ok(new APIResponse("Protocolo eliminado", null, false, HttpStatus.OK));
    }

    // ── TuboProtocolo ────────────────────────────────────────────────────────────

    @PostMapping("/{idProtocolo}/tubos")
    @Operation(summary = "Agregar tubo primario al protocolo")
    public ResponseEntity<APIResponse> addTubo(
            @Parameter(description = "ID del protocolo") @PathVariable Long idProtocolo,
            @Validated @RequestBody TuboProtocoloRequestDTO dto) {
        TuboProtocolo tubo = ProtocoloMapper.tuboToEntity(dto);
        TuboProtocolo saved = protocoloApplicationService.addTubo(idProtocolo, tubo, dto.getIdTipoResultante());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Tubo agregado",
                        ProtocoloMapper.tuboToResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/tubos/{idTubo}")
    @Operation(summary = "Actualizar tubo primario")
    public ResponseEntity<APIResponse> updateTubo(
            @Parameter(description = "ID del tubo") @PathVariable Long idTubo,
            @Validated @RequestBody TuboProtocoloRequestDTO dto) {
        TuboProtocolo datos = ProtocoloMapper.tuboToEntity(dto);
        TuboProtocolo updated = protocoloApplicationService.updateTubo(idTubo, datos, dto.getIdTipoResultante());
        return ResponseEntity.ok(new APIResponse("Tubo actualizado",
                ProtocoloMapper.tuboToResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/tubos/{idTubo}")
    @Operation(summary = "Eliminar tubo primario")
    public ResponseEntity<APIResponse> deleteTubo(
            @Parameter(description = "ID del tubo") @PathVariable Long idTubo) {
        protocoloApplicationService.deleteTubo(idTubo);
        return ResponseEntity.ok(new APIResponse("Tubo eliminado", null, false, HttpStatus.OK));
    }
}
