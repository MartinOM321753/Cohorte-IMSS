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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/almacenamiento/traslados")
@RequiredArgsConstructor
@Tag(name = "Préstamos de Muestras", description = "Gestión de préstamos de muestras entre instituciones con biobanco (cadena de custodia)")
@SecurityRequirement(name = "bearerAuth")
public class TrasladoMuestraController {

    private final TrasladoMuestraApplicationService trasladoApplicationService;

    // ── Consultas ────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Préstamos activos de mi institución",
               description = "Lista los préstamos activos (ENVIADA, RECIBIDA, EN_DEVOLUCION) donde mi institución es origen O destino.")
    public ResponseEntity<APIResponse> getActivos() {
        List<TrasladoMuestra> list = trasladoApplicationService.getActivosByMiInstitucion();
        return ResponseEntity.ok(
            new APIResponse("Préstamos activos", TrasladoMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/historial")
    @Operation(summary = "Historial completo de préstamos de mi institución (paginado)")
    public ResponseEntity<APIResponse> getHistorialPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TrasladoMuestra> pageResult = trasladoApplicationService.getAllByMiInstitucionPaginado(page, size);
        Page<TrasladoResponseDTO> dtoPage = pageResult.map(TrasladoMapper::toResponseDTO);
        return ResponseEntity.ok(
            new APIResponse("Historial de préstamos", dtoPage, false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un préstamo por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "No encontrado",
            content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getById(
            @Parameter(description = "ID del préstamo") @PathVariable Long id) {
        TrasladoMuestra t = trasladoApplicationService.getTraslado(id);
        return ResponseEntity.ok(
            new APIResponse("Préstamo encontrado", TrasladoMapper.toResponseDTO(t), false, HttpStatus.OK));
    }

    @GetMapping("/muestra/{idMuestra}")
    @Operation(summary = "Cadena de custodia de una muestra",
               description = "Historial completo de préstamos de la muestra, ordenado por fecha descendente.")
    public ResponseEntity<APIResponse> getHistorialByMuestra(
            @Parameter(description = "ID de la muestra") @PathVariable Long idMuestra) {
        List<TrasladoMuestra> historial = trasladoApplicationService.getHistorialByMuestra(idMuestra);
        return ResponseEntity.ok(
            new APIResponse("Cadena de custodia", TrasladoMapper.toResponseDTOList(historial), false, HttpStatus.OK));
    }

    @GetMapping("/grupo/{grupoTraslado}")
    @Operation(summary = "Traslados de un lote",
               description = "Obtiene todos los TrasladoMuestra que forman un préstamo en lote (padre + alícuotas).")
    public ResponseEntity<APIResponse> getByGrupo(
            @Parameter(description = "UUID del grupo de traslado") @PathVariable String grupoTraslado) {
        List<TrasladoMuestra> list = trasladoApplicationService.getByGrupo(grupoTraslado);
        return ResponseEntity.ok(
            new APIResponse("Traslados del lote", TrasladoMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    // ── Mutaciones ───────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Iniciar préstamo",
               description = """
                   Presta una o varias muestras (padre + alícuotas seleccionadas) a otra institución.
                   La institución origen es la del usuario logueado (tenedor actual).
                   Las muestras pasan a estado PRESTADA y sus posiciones se liberan.
                   Si se incluyen múltiples muestras, se agrupan con el mismo grupoTraslado.
                   """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Préstamo iniciado",
            content = @Content(schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "Muestra ya prestada o institución sin biobanco",
            content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> iniciarPrestamo(@Validated @RequestBody TrasladoRequestDTO dto) {
        List<TrasladoMuestra> traslados = trasladoApplicationService.iniciarPrestamo(
            dto.getIdsMuestras(),
            dto.getIdInstitucionDestino(),
            dto.getUuidAutoriza(),
            dto.getMotivo(),
            dto.getObservaciones()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Préstamo iniciado exitosamente",
                TrasladoMapper.toResponseDTOList(traslados), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}/confirmar-recepcion")
    @Operation(summary = "Confirmar recepción",
               description = "La institución destino confirma que recibió físicamente la muestra. Opcionalmente asigna posición en caja. Estado: ENVIADA → RECIBIDA.")
    public ResponseEntity<APIResponse> confirmarRecepcion(
            @PathVariable Long id,
            @Validated @RequestBody ConfirmarRecepcionRequestDTO dto) {
        TrasladoMuestra updated = trasladoApplicationService.confirmarRecepcion(
            id, dto.getUuidConfirma(), dto.getIdPosicionCaja());
        return ResponseEntity.ok(
            new APIResponse("Recepción confirmada", TrasladoMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @GetMapping("/{id}/alicuotas-en-destino")
    @Operation(summary = "Alícuotas en institución destino",
               description = "Lista las alícuotas de la muestra padre del traslado que están actualmente en la institución destino.")
    public ResponseEntity<APIResponse> getAlicuotasEnDestino(@PathVariable Long id) {
        var alicuotas = trasladoApplicationService.getAlicuotasEnDestino(id);
        return ResponseEntity.ok(
            new APIResponse("Alícuotas en destino", MuestraMapper.toResponseDTOList(alicuotas), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/iniciar-devolucion")
    @Operation(summary = "Iniciar devolución",
               description = "La institución destino inicia la devolución. Opcionalmente incluye alícuotas para devolver junto con la padre. Estado: RECIBIDA → EN_DEVOLUCION.")
    public ResponseEntity<APIResponse> iniciarDevolucion(
            @PathVariable Long id,
            @Validated @RequestBody IniciarDevolucionRequestDTO dto) {
        List<TrasladoMuestra> traslados = trasladoApplicationService.iniciarDevolucion(
            id, dto.getUuidInicia(), dto.getObservaciones(), dto.getIdsAlicuotasDevolver());
        return ResponseEntity.ok(
            new APIResponse("Devolución iniciada", TrasladoMapper.toResponseDTOList(traslados), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/confirmar-devolucion")
    @Operation(summary = "Confirmar devolución",
               description = "La institución anterior confirma la recepción de vuelta. Estado: EN_DEVOLUCION → DEVUELTA.")
    public ResponseEntity<APIResponse> confirmarDevolucion(
            @PathVariable Long id,
            @Validated @RequestBody DevolucionRequestDTO dto) {
        TrasladoMuestra devuelto = trasladoApplicationService.confirmarDevolucion(
            id, dto.getUuidConfirma(), dto.getObservaciones());
        return ResponseEntity.ok(
            new APIResponse("Devolución confirmada", TrasladoMapper.toResponseDTO(devuelto), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar préstamo",
               description = "La institución origen cancela el préstamo mientras está en estado ENVIADA (antes de que el destino confirme recepción). La muestra regresa a SIN_POSICION en la institución origen.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Préstamo cancelado",
            content = @Content(schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "409", description = "El préstamo no está en estado ENVIADA",
            content = @Content(schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> cancelarPrestamo(
            @PathVariable Long id,
            @Validated @RequestBody CancelarPrestamoRequestDTO dto) {
        TrasladoMuestra cancelado = trasladoApplicationService.cancelarPrestamo(
            id, dto.getUuidUsuario(), dto.getMotivo());
        return ResponseEntity.ok(
            new APIResponse("Préstamo cancelado", TrasladoMapper.toResponseDTO(cancelado), false, HttpStatus.OK));
    }
}
