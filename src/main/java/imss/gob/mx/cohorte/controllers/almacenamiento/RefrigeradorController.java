package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.PisoRefrigeradorApplicationService;
import imss.gob.mx.cohorte.application.almacenamiento.RefrieradorApplicationService;
import imss.gob.mx.cohorte.controllers.DTO.PisosDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.RefrigeradorMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.RefrigeradorRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.RefrigeradorResponseDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
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
@RequestMapping("/api/almacenamiento/refrigeradores")
@AllArgsConstructor
@Tag(name = "Refrigeradores", description = "Gestión de refrigeradores criogénicos")
@SecurityRequirement(name = "bearerAuth")
public class RefrigeradorController {

    private final RefrieradorApplicationService refrieradorApplicationService;
    private final PisoRefrigeradorApplicationService pisoRefrigeradorApplicationService;

    @GetMapping
    @Operation(summary = "Listar todos los refrigeradores", description = "Obtiene una lista completa de todos los refrigeradores criogénicos registrados en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAll() {
        List<Refrigerador> list = refrieradorApplicationService.getAllRefrigeradores();
        return ResponseEntity.ok(new APIResponse("Refrigeradores encontrados", RefrigeradorMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener refrigerador por ID", description = "Obtiene los detalles de un refrigerador criogénico específico mediante su identificador único")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getById(
        @Parameter(description = "ID numérico del refrigerador criogénico", required = true)
        @PathVariable Long id) {
        Refrigerador ref = refrieradorApplicationService.getRefrigerador(id);
        return ResponseEntity.ok(new APIResponse("Refrigerador encontrado", RefrigeradorMapper.toResponseDTO(ref), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear refrigerador", description = "Registra un nuevo refrigerador criogénico en el sistema con los datos proporcionados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Recurso creado exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody RefrigeradorRequestDTO dto) {
        Refrigerador entity = RefrigeradorMapper.toEntity(dto);
        Refrigerador saved = refrieradorApplicationService.createRefrigerador(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Refrigerador creado exitosamente", RefrigeradorMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar refrigerador", description = "Actualiza la información de un refrigerador criogénico existente identificado por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> update(
        @Parameter(description = "ID numérico del refrigerador criogénico", required = true)
        @PathVariable Long id, @Validated @RequestBody RefrigeradorRequestDTO dto) {
        Refrigerador entity = RefrigeradorMapper.toEntity(dto);
        Refrigerador updated = refrieradorApplicationService.updateRefrigerador(id, entity);
        return ResponseEntity.ok(new APIResponse("Refrigerador actualizado", RefrigeradorMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar refrigerador", description = "Elimina un refrigerador criogénico del sistema mediante su identificador único")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> delete(
        @Parameter(description = "ID numérico del refrigerador criogénico", required = true)
        @PathVariable Long id) {
        refrieradorApplicationService.deleteRefrigerador(id);
        return ResponseEntity.ok(new APIResponse("Refrigerador eliminado", null, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/pisos")
    @Operation(summary = "Listar pisos del refrigerador", description = "Obtiene todos los pisos asociados a un refrigerador criogénico específico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getPisos(
        @Parameter(description = "ID numérico del refrigerador criogénico", required = true)
        @PathVariable Long id) {
        List<PisoRefrigerador> pisos = pisoRefrigeradorApplicationService.getAllPisos(id);
        return ResponseEntity.ok(new APIResponse("Pisos encontrados", pisos, false, HttpStatus.OK));
    }

    @PostMapping("/pisos")
    @Operation(summary = "Crear piso(s) con generación automática de posiciones", description = "Crea uno o más pisos para un refrigerador criogénico y genera automáticamente las posiciones de almacenamiento")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Recurso creado exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> createPisos(@Validated @RequestBody PisosDTO pisosDTO) {
        List<PisoRefrigerador> pisos = pisoRefrigeradorApplicationService.createPisos(pisosDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Pisos creados exitosamente", pisos, false, HttpStatus.CREATED));
    }

    @PutMapping("/pisos/{id}")
    @Operation(summary = "Actualizar piso de refrigerador", description = "Actualiza dimensiones y datos de un piso. Elimina posiciones libres fuera del nuevo rango y crea las nuevas. Rechaza si hay posiciones ocupadas que quedarían fuera.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflicto: posiciones ocupadas afectadas",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> updatePiso(
        @Parameter(description = "ID numérico del piso", required = true)
        @PathVariable Long id, @RequestBody PisoRefrigerador piso) {
        PisoRefrigerador updated = pisoRefrigeradorApplicationService.updatePiso(id, piso);
        return ResponseEntity.ok(new APIResponse("Piso actualizado", updated, false, HttpStatus.OK));
    }

    @GetMapping("/pisos/{id}/posiciones")
    @Operation(summary = "Listar posiciones de un piso", description = "Obtiene todas las posiciones de almacenamiento asociadas a un piso específico mediante su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getPosicionesByPiso(
        @Parameter(description = "ID numérico del piso", required = true)
        @PathVariable Long id) {
        List<PosicionPiso> posiciones = pisoRefrigeradorApplicationService.getPosiciones(id);
        return ResponseEntity.ok(new APIResponse("Posiciones encontradas", posiciones, false, HttpStatus.OK));
    }
}
