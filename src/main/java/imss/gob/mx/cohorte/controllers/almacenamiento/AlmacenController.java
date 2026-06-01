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
@RequestMapping("/api/almacenamiento/almacenes")
@RequiredArgsConstructor
@Tag(name = "Almacenes Externos", description = "Gestión de almacenes externos para traslado de muestras")
@SecurityRequirement(name = "bearerAuth")
public class AlmacenController {

    private final AlmacenApplicationService almacenApplicationService;

    @GetMapping
    @Operation(summary = "Listar almacenes", description = "Obtiene todos los almacenes externos registrados en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAll() {
        List<Almacen> list = almacenApplicationService.getAllAlmacenes();
        return ResponseEntity.ok(
            new APIResponse("Almacenes encontrados", AlmacenMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener almacén por ID", description = "Obtiene el detalle de un almacén externo específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Almacén no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getById(
            @Parameter(description = "ID del almacén", required = true) @PathVariable Long id) {
        Almacen almacen = almacenApplicationService.getAlmacen(id);
        return ResponseEntity.ok(
            new APIResponse("Almacén encontrado", AlmacenMapper.toResponseDTO(almacen), false, HttpStatus.OK));
    }

    @GetMapping("/encargado/{uuid}")
    @Operation(summary = "Listar almacenes por UUID de encargado", description = "Devuelve todos los almacenes activos asignados al encargado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAllByEncargado(
            @Parameter(description = "UUID del encargado", required = true) @PathVariable String uuid) {
        List<Almacen> almacenes = almacenApplicationService.findAllByEncargadoUuid(uuid);
        return ResponseEntity.ok(
            new APIResponse("Almacenes encontrados", AlmacenMapper.toResponseDTOList(almacenes), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear almacén", description = "Registra un nuevo almacén externo en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Almacén creado exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Ya existe un almacén con ese nombre",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody AlmacenRequestDTO dto) {
        Almacen entity = AlmacenMapper.toEntity(dto);
        Almacen saved = almacenApplicationService.createAlmacen(entity, dto.getUuidEncargado());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Almacén creado exitosamente", AlmacenMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar almacén", description = "Actualiza los datos de un almacén externo existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Almacén no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Ya existe un almacén con ese nombre",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> update(
            @Parameter(description = "ID del almacén", required = true) @PathVariable Long id,
            @Validated @RequestBody AlmacenRequestDTO dto) {
        Almacen entity = AlmacenMapper.toEntity(dto);
        Almacen updated = almacenApplicationService.updateAlmacen(id, entity, dto.getUuidEncargado());
        return ResponseEntity.ok(
            new APIResponse("Almacén actualizado exitosamente", AlmacenMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar almacén", description = "Desactiva un almacén externo (soft delete). No elimina el historial de traslados asociado.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Almacén no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> delete(
            @Parameter(description = "ID del almacén", required = true) @PathVariable Long id) {
        almacenApplicationService.deleteAlmacen(id);
        return ResponseEntity.ok(
            new APIResponse("Almacén desactivado exitosamente", null, false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Activar almacén", description = "Reactiva un almacén externo previamente desactivado.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Almacén no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "El almacén ya está activo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> activate(
            @Parameter(description = "ID del almacén", required = true) @PathVariable Long id) {
        Almacen almacen = almacenApplicationService.activateAlmacen(id);
        return ResponseEntity.ok(
            new APIResponse("Almacén activado exitosamente", AlmacenMapper.toResponseDTO(almacen), false, HttpStatus.OK));
    }
}
