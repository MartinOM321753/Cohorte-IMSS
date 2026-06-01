package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.TrasladoMuestraApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.*;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
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

import org.springframework.data.domain.Page;

import java.util.List;

@RestController
@RequestMapping("/api/almacenamiento/traslados")
@RequiredArgsConstructor
@Tag(name = "Traslados de Muestras", description = "Registro y consulta de traslados de muestras a laboratorios externos")
@SecurityRequirement(name = "bearerAuth")
public class TrasladoMuestraController {

    private final TrasladoMuestraApplicationService trasladoApplicationService;

    @GetMapping
    @Operation(summary = "Listar todos los traslados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAll() {
        List<TrasladoMuestra> list = trasladoApplicationService.getAllTraslados();
        return ResponseEntity.ok(
            new APIResponse("Traslados encontrados", TrasladoMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener traslado por ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Traslado no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getById(
            @Parameter(description = "ID del traslado", required = true) @PathVariable Long id) {
        TrasladoMuestra traslado = trasladoApplicationService.getTraslado(id);
        return ResponseEntity.ok(
            new APIResponse("Traslado encontrado", TrasladoMapper.toResponseDTO(traslado), false, HttpStatus.OK));
    }

    @GetMapping("/muestra/{idMuestra}")
    @Operation(summary = "Historial de traslados por muestra")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Muestra no encontrada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getHistorialByMuestra(
            @Parameter(description = "ID de la muestra", required = true) @PathVariable Long idMuestra) {
        List<TrasladoMuestra> historial = trasladoApplicationService.getHistorialByMuestra(idMuestra);
        return ResponseEntity.ok(
            new APIResponse("Historial de traslados encontrado", TrasladoMapper.toResponseDTOList(historial), false, HttpStatus.OK));
    }

    @GetMapping("/almacen/{idAlmacen}")
    @Operation(summary = "Traslados por almacén (paginado)", description = "Obtiene los traslados de un almacén con paginación. Usado por el ENCARGADO.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getByAlmacen(
            @Parameter(description = "ID del almacén", required = true) @PathVariable Long idAlmacen,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<TrasladoMuestra> pageResult = trasladoApplicationService.getTrasladosByAlmacenPaginated(idAlmacen, page, size);
        Page<TrasladoResponseDTO> dtoPage = pageResult.map(TrasladoMapper::toResponseDTO);
        return ResponseEntity.ok(
            new APIResponse("Traslados del almacén encontrados", dtoPage, false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Registrar traslado", description = "Registra el traslado de una muestra a un almacén externo. La posición en la caja queda reservada mientras dure el traslado.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Traslado registrado exitosamente",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Muestra, almacén o usuario no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "La muestra ya tiene un traslado activo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> registrarTraslado(@Validated @RequestBody TrasladoRequestDTO dto) {
        TrasladoMuestra traslado = trasladoApplicationService.registrarTraslado(
            dto.getIdMuestra(),
            dto.getIdAlmacen(),
            dto.getUuidAutoriza(),
            dto.getMotivo(),
            dto.getObservaciones()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Traslado registrado exitosamente", TrasladoMapper.toResponseDTO(traslado), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}/confirmar-recepcion")
    @Operation(summary = "Confirmar recepción", description = "El encargado del almacén confirma que la muestra fue recibida físicamente. Cambia estado de TRASLADADA → RECIBIDA.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recepción confirmada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Traslado no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Estado incorrecto para confirmar recepción",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> confirmarRecepcion(
            @Parameter(description = "ID del traslado", required = true) @PathVariable Long id,
            @Validated @RequestBody ConfirmarRecepcionRequestDTO dto) {
        TrasladoMuestra updated = trasladoApplicationService.confirmarRecepcion(id, dto.getUuidEncargado());
        return ResponseEntity.ok(
            new APIResponse("Recepción confirmada exitosamente", TrasladoMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/iniciar-devolucion")
    @Operation(summary = "Iniciar devolución", description = "El encargado inicia el proceso de devolución. Cambia estado de RECIBIDA → EN_DEVOLUCION.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Devolución iniciada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Traslado no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Estado incorrecto para iniciar devolución",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> iniciarDevolucion(
            @Parameter(description = "ID del traslado", required = true) @PathVariable Long id,
            @Validated @RequestBody IniciarDevolucionRequestDTO dto) {
        TrasladoMuestra updated = trasladoApplicationService.iniciarDevolucion(id, dto.getUuidEncargado(), dto.getObservaciones());
        return ResponseEntity.ok(
            new APIResponse("Devolución iniciada exitosamente", TrasladoMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/devolver")
    @Operation(summary = "Confirmar devolución (admin)", description = "El administrador confirma la devolución física de la muestra. Cambia estado de EN_DEVOLUCION → DEVUELTA.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Devolución confirmada",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Traslado no encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Estado incorrecto para confirmar devolución",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> confirmarDevolucion(
            @Parameter(description = "ID del traslado a devolver", required = true) @PathVariable Long id,
            @Validated @RequestBody DevolucionRequestDTO dto) {
        TrasladoMuestra devuelto = trasladoApplicationService.confirmarDevolucion(id, dto.getObservaciones());
        return ResponseEntity.ok(
            new APIResponse("Devolución confirmada exitosamente", TrasladoMapper.toResponseDTO(devuelto), false, HttpStatus.OK));
    }
}
