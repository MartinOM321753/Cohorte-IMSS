package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.AlmacenApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.AlmacenMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.AlmacenRequestDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.almacen.Almacen;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/almacenamiento/instituciones")
@RequiredArgsConstructor
@Tag(name = "Instituciones Externas", description = "Gestión de instituciones externas (INMEGEN, INSP, hospitales, laboratorios) a las que se trasladan muestras")
@SecurityRequirement(name = "bearerAuth")
public class AlmacenController {

    private final AlmacenApplicationService almacenApplicationService;

    @GetMapping
    @Operation(summary = "Listar instituciones", description = "Obtiene todas las instituciones externas registradas en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAll() {
        List<Almacen> list = almacenApplicationService.getAllAlmacenes();
        return ResponseEntity.ok(
            new APIResponse("Instituciones encontradas", AlmacenMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener institución por ID", description = "Obtiene el detalle de una institución externa específica")
    public ResponseEntity<APIResponse> getById(
            @Parameter(description = "ID de la institución", required = true) @PathVariable Long id) {
        Almacen almacen = almacenApplicationService.getAlmacen(id);
        return ResponseEntity.ok(
            new APIResponse("Institución encontrada", AlmacenMapper.toResponseDTO(almacen), false, HttpStatus.OK));
    }

    @GetMapping("/encargado/{uuid}")
    @Operation(summary = "Listar instituciones por UUID de encargado", description = "Devuelve todas las instituciones activas asignadas al encargado")
    public ResponseEntity<APIResponse> getAllByEncargado(
            @Parameter(description = "UUID del encargado", required = true) @PathVariable String uuid) {
        List<Almacen> almacenes = almacenApplicationService.findAllByEncargadoUuid(uuid);
        return ResponseEntity.ok(
            new APIResponse("Instituciones encontradas", AlmacenMapper.toResponseDTOList(almacenes), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear institución", description = "Registra una nueva institución externa en el sistema")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody AlmacenRequestDTO dto) {
        Almacen entity = AlmacenMapper.toEntity(dto);
        Almacen saved = almacenApplicationService.createAlmacen(entity, dto.getUuidEncargado());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Institución creada exitosamente", AlmacenMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar institución", description = "Actualiza los datos de una institución externa existente")
    public ResponseEntity<APIResponse> update(
            @Parameter(description = "ID de la institución", required = true) @PathVariable Long id,
            @Validated @RequestBody AlmacenRequestDTO dto) {
        Almacen entity = AlmacenMapper.toEntity(dto);
        Almacen updated = almacenApplicationService.updateAlmacen(id, entity, dto.getUuidEncargado());
        return ResponseEntity.ok(
            new APIResponse("Institución actualizada exitosamente", AlmacenMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar institución", description = "Desactiva una institución (soft delete). No elimina el historial de traslados asociado.")
    public ResponseEntity<APIResponse> delete(
            @Parameter(description = "ID de la institución", required = true) @PathVariable Long id) {
        almacenApplicationService.deleteAlmacen(id);
        return ResponseEntity.ok(
            new APIResponse("Institución desactivada exitosamente", null, false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Activar institución", description = "Reactiva una institución previamente desactivada.")
    public ResponseEntity<APIResponse> activate(
            @Parameter(description = "ID de la institución", required = true) @PathVariable Long id) {
        Almacen almacen = almacenApplicationService.activateAlmacen(id);
        return ResponseEntity.ok(
            new APIResponse("Institución activada exitosamente", AlmacenMapper.toResponseDTO(almacen), false, HttpStatus.OK));
    }
}
