package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.CajasApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.CajaMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.CajaRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.CajaResponseDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
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
@RequestMapping("/api/almacenamiento/cajas")
@AllArgsConstructor
@Tag(name = "Cajas Criogénicas", description = "Gestión de cajas criogénicas")
@SecurityRequirement(name = "bearerAuth")
public class CajaController {

    private final CajasApplicationService cajasApplicationService;

    @GetMapping
    @Operation(summary = "Listar todas las cajas criogénicas", description = "Obtiene una lista completa de todas las cajas criogénicas registradas en el sistema")
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
        List<CajaCriogenica> list = cajasApplicationService.getAllCajas();
        return ResponseEntity.ok(new APIResponse("Cajas encontradas", CajaMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener caja criogénica por ID", description = "Obtiene los detalles de una caja criogénica específica mediante su identificador único")
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
        @Parameter(description = "ID numérico de la caja criogénica", required = true)
        @PathVariable Long id) {
        CajaCriogenica caja = cajasApplicationService.getCaja(id);
        return ResponseEntity.ok(new APIResponse("Caja encontrada", CajaMapper.toResponseDTO(caja), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear caja criogénica", description = "Registra una nueva caja criogénica en el sistema y la asigna a una posición de piso")
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
    public ResponseEntity<APIResponse> create(@Validated @RequestBody CajaRequestDTO dto) {
        CajaCriogenica entity = CajaMapper.toEntity(dto);
        CajaCriogenica saved = cajasApplicationService.createCaja(entity, dto.getIdPosicionPiso());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Caja criogénica creada exitosamente", CajaMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar caja criogénica", description = "Actualiza la información de una caja criogénica existente identificada por su ID")
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
        @Parameter(description = "ID numérico de la caja criogénica", required = true)
        @PathVariable Long id, @Validated @RequestBody CajaRequestDTO dto) {
        CajaCriogenica entity = CajaMapper.toEntity(dto);
        CajaCriogenica updated = cajasApplicationService.updateCaja(id, entity);
        return ResponseEntity.ok(new APIResponse("Caja criogénica actualizada", CajaMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @GetMapping("/{id}/posiciones")
    @Operation(summary = "Obtener posiciones de la caja", description = "Obtiene todas las posiciones de almacenamiento asociadas a una caja criogénica específica")
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
    public ResponseEntity<APIResponse> getPosiciones(
        @Parameter(description = "ID numérico de la caja criogénica", required = true)
        @PathVariable Long id) {
        List<PosicionCaja> posiciones = cajasApplicationService.getPosicionesByCaja(id);
        return ResponseEntity.ok(new APIResponse("Posiciones encontradas", posiciones, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/posiciones/libres")
    @Operation(summary = "Obtener posiciones libres de la caja", description = "Obtiene todas las posiciones de almacenamiento disponibles (no ocupadas) en una caja criogénica específica")
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
    public ResponseEntity<APIResponse> getPosicionesLibres(
        @Parameter(description = "ID numérico de la caja criogénica", required = true)
        @PathVariable Long id) {
        List<PosicionCaja> posiciones = cajasApplicationService.getPosicionesLibresByCaja(id);
        return ResponseEntity.ok(new APIResponse("Posiciones libres encontradas", posiciones, false, HttpStatus.OK));
    }
}
