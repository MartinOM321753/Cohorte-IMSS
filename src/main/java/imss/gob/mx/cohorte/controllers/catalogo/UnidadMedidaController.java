package imss.gob.mx.cohorte.controllers.catalogo;

import imss.gob.mx.cohorte.controllers.catalogo.dto.UnidadMedidaRequestDTO;
import imss.gob.mx.cohorte.modules.catalogo.UnidadMedida;
import imss.gob.mx.cohorte.services.catalogo.UnidadMedidaService;
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
@RequestMapping("/api/catalogos/unidades")
@RequiredArgsConstructor
@Tag(name = "Unidades de Medida", description = "Catálogo de unidades de medida del sistema")
@SecurityRequirement(name = "bearerAuth")
public class UnidadMedidaController {

    private final UnidadMedidaService service;

    @GetMapping
    @Operation(summary = "Listar unidades activas", description = "Obtiene todas las unidades de medida activas. Utilizar en formularios de selección.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAllActivas() {
        List<UnidadMedida> unidades = service.getAllActivas();
        return ResponseEntity.ok(new APIResponse("Unidades activas", unidades, false, HttpStatus.OK));
    }

    @GetMapping("/todas")
    @Operation(summary = "Listar todas las unidades (admin)", description = "Obtiene todas las unidades de medida, activas e inactivas. Exclusivo para administración.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAll() {
        List<UnidadMedida> unidades = service.getAll();
        return ResponseEntity.ok(new APIResponse("Todas las unidades", unidades, false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener unidad por ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "No encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getById(
        @Parameter(description = "ID de la unidad", required = true)
        @PathVariable Long id) {
        UnidadMedida unidad = service.getById(id);
        return ResponseEntity.ok(new APIResponse("Unidad encontrada", unidad, false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear unidad de medida")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Creada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nombre duplicado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody UnidadMedidaRequestDTO dto) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre(dto.getNombre());
        UnidadMedida saved = service.create(unidad);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Unidad creada exitosamente", saved, false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar unidad de medida")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "No encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Nombre duplicado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> update(
        @Parameter(description = "ID de la unidad", required = true)
        @PathVariable Long id,
        @Validated @RequestBody UnidadMedidaRequestDTO dto) {
        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre(dto.getNombre());
        UnidadMedida updated = service.update(id, unidad);
        return ResponseEntity.ok(new APIResponse("Unidad actualizada", updated, false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activar / desactivar unidad de medida")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "No encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> toggle(
        @Parameter(description = "ID de la unidad", required = true)
        @PathVariable Long id) {
        UnidadMedida updated = service.toggleActivo(id);
        String msg = updated.getActivo() ? "Unidad activada" : "Unidad desactivada";
        return ResponseEntity.ok(new APIResponse(msg, updated, false, HttpStatus.OK));
    }
}
