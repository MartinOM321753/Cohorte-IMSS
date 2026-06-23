package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.TipoMuestraApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.*;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
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

import java.util.List;

@RestController
@RequestMapping("/api/muestras/tipos")
@AllArgsConstructor
@Tag(name = "Tipos de Muestra", description = "Catálogo de tipos de muestra y configuración de tubos/alícuotas")
@SecurityRequirement(name = "bearerAuth")
public class TipoMuestraController {

    private final TipoMuestraApplicationService tipoMuestraApplicationService;

    // ── TipoMuestra ──────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Listar tipos de muestra activos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAllActivos() {
        List<TipoMuestra> list = tipoMuestraApplicationService.getAllActivos();
        return ResponseEntity.ok(new APIResponse("Tipos de muestra encontrados",
                TipoMuestraMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/todos")
    @Operation(summary = "Listar todos los tipos de muestra (activos e inactivos)")
    public ResponseEntity<APIResponse> getAll() {
        List<TipoMuestra> list = tipoMuestraApplicationService.getAll();
        return ResponseEntity.ok(new APIResponse("Tipos de muestra encontrados",
                TipoMuestraMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tipo de muestra por ID")
    public ResponseEntity<APIResponse> getById(
            @Parameter(description = "ID del tipo de muestra") @PathVariable Long id) {
        TipoMuestra tipo = tipoMuestraApplicationService.getById(id);
        return ResponseEntity.ok(new APIResponse("Tipo de muestra encontrado",
                TipoMuestraMapper.toResponseDTO(tipo), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear tipo de muestra")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Creado exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Ya existe un tipo con ese nombre",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody TipoMuestraRequestDTO dto) {
        TipoMuestra tipo = TipoMuestraMapper.toEntity(dto);
        TipoMuestra saved = tipoMuestraApplicationService.create(tipo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Tipo de muestra creado",
                        TipoMuestraMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tipo de muestra")
    public ResponseEntity<APIResponse> update(
            @Parameter(description = "ID del tipo de muestra") @PathVariable Long id,
            @Validated @RequestBody TipoMuestraRequestDTO dto) {
        TipoMuestra datos = TipoMuestraMapper.toEntity(dto);
        TipoMuestra updated = tipoMuestraApplicationService.update(id, datos);
        return ResponseEntity.ok(new APIResponse("Tipo de muestra actualizado",
                TipoMuestraMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Activar / Desactivar tipo de muestra")
    public ResponseEntity<APIResponse> toggle(
            @Parameter(description = "ID del tipo de muestra") @PathVariable Long id) {
        TipoMuestra toggled = tipoMuestraApplicationService.toggleActivo(id);
        String mensaje = toggled.getActivo() ? "Tipo de muestra activado" : "Tipo de muestra desactivado";
        return ResponseEntity.ok(new APIResponse(mensaje,
                TipoMuestraMapper.toResponseDTO(toggled), false, HttpStatus.OK));
    }

    // ── TuboMuestra ──────────────────────────────────────────────────────────

    @PostMapping("/{idTipo}/tubos")
    @Operation(summary = "Agregar tubo a tipo de muestra")
    public ResponseEntity<APIResponse> addTubo(
            @Parameter(description = "ID del tipo de muestra") @PathVariable Long idTipo,
            @Validated @RequestBody TuboMuestraRequestDTO dto) {
        TuboMuestra tubo = TipoMuestraMapper.tuboToEntity(dto);
        TuboMuestra saved = tipoMuestraApplicationService.addTubo(idTipo, tubo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Tubo agregado",
                        TipoMuestraMapper.tuboToResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/tubos/{idTubo}")
    @Operation(summary = "Actualizar tubo")
    public ResponseEntity<APIResponse> updateTubo(
            @Parameter(description = "ID del tubo") @PathVariable Long idTubo,
            @Validated @RequestBody TuboMuestraRequestDTO dto) {
        TuboMuestra datos = TipoMuestraMapper.tuboToEntity(dto);
        TuboMuestra updated = tipoMuestraApplicationService.updateTubo(idTubo, datos);
        return ResponseEntity.ok(new APIResponse("Tubo actualizado",
                TipoMuestraMapper.tuboToResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/tubos/{idTubo}")
    @Operation(summary = "Eliminar tubo")
    public ResponseEntity<APIResponse> deleteTubo(
            @Parameter(description = "ID del tubo") @PathVariable Long idTubo) {
        tipoMuestraApplicationService.deleteTubo(idTubo);
        return ResponseEntity.ok(new APIResponse("Tubo eliminado", null, false, HttpStatus.OK));
    }
}
