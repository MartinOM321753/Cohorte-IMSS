package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.EntregaMuestraApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega.CancelarEntregaRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega.CerrarEntregaRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega.EntregaMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega.EntregaRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega.EntregaResponseDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.entrega.EntregaMuestra;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/almacenamiento/entregas")
@RequiredArgsConstructor
@Tag(name = "Entregas a colaboradores", description = "Entrega/préstamo de muestras a personas físicas (colaboradores), con devolución o consumo")
@SecurityRequirement(name = "bearerAuth")
public class EntregaMuestraController {

    private final EntregaMuestraApplicationService entregaApplicationService;

    // ── Consultas ────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Entregas activas de mi institución")
    public ResponseEntity<APIResponse> getActivas() {
        List<EntregaMuestra> list = entregaApplicationService.getActivasDeMiInstitucion();
        return ResponseEntity.ok(new APIResponse("Entregas activas",
                EntregaMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/historial")
    @Operation(summary = "Historial de entregas (paginado)")
    public ResponseEntity<APIResponse> getHistorial(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EntregaMuestra> pageResult = entregaApplicationService.getHistorialPaginado(page, size);
        Page<EntregaResponseDTO> dtoPage = pageResult.map(EntregaMapper::toResponseDTO);
        return ResponseEntity.ok(new APIResponse("Historial de entregas", dtoPage, false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una entrega")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        EntregaMuestra e = entregaApplicationService.getEntrega(id);
        return ResponseEntity.ok(new APIResponse("Entrega",
                EntregaMapper.toResponseDTO(e), false, HttpStatus.OK));
    }

    @GetMapping("/grupo/{grupoEntrega}")
    @Operation(summary = "Entregas de un lote (padre + alícuotas)")
    public ResponseEntity<APIResponse> getByGrupo(@PathVariable String grupoEntrega) {
        List<EntregaMuestra> list = entregaApplicationService.getByGrupo(grupoEntrega);
        return ResponseEntity.ok(new APIResponse("Entregas del lote",
                EntregaMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/muestra/{idMuestra}")
    @Operation(summary = "Historial de entregas de una muestra")
    public ResponseEntity<APIResponse> getHistorialByMuestra(@PathVariable Long idMuestra) {
        List<EntregaMuestra> list = entregaApplicationService.getHistorialByMuestra(idMuestra);
        return ResponseEntity.ok(new APIResponse("Entregas de la muestra",
                EntregaMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/destinatarios")
    @Operation(summary = "Destinatarios posibles de una entrega",
               description = "Colaboradores activos y usuarios del sistema de mi institución, para el selector del formulario.")
    public ResponseEntity<APIResponse> getDestinatarios() {
        return ResponseEntity.ok(new APIResponse("Destinatarios",
                entregaApplicationService.getDestinatarios(), false, HttpStatus.OK));
    }

    // ── Mutaciones ───────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Entregar muestra(s) a un colaborador",
               description = "Registra la entrega inmediata (un paso) de una o varias muestras a un colaborador. La posición se libera; la institución NO cambia.")
    public ResponseEntity<APIResponse> entregar(@Validated @RequestBody EntregaRequestDTO dto) {
        List<EntregaMuestra> entregas = entregaApplicationService.entregar(
                dto.getIdsMuestras(), dto.getIdColaborador(), dto.getIdUsuario(), dto.getUuidEntrega(),
                dto.getMotivo(), dto.getObservaciones(), dto.getFechaLimite());
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse("Entrega registrada",
                EntregaMapper.toResponseDTOList(entregas), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}/devolver")
    @Operation(summary = "Registrar devolución", description = "La muestra regresó al biobanco. Vuelve a SIN_POSICION; el volumen no se toca.")
    public ResponseEntity<APIResponse> devolver(@PathVariable Long id,
                                                @Validated @RequestBody CerrarEntregaRequestDTO dto) {
        EntregaMuestra e = entregaApplicationService.devolver(id, dto.getUuidCierra(), dto.getObservaciones());
        return ResponseEntity.ok(new APIResponse("Devolución registrada",
                EntregaMapper.toResponseDTO(e), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/consumir")
    @Operation(summary = "Marcar como consumida", description = "La muestra no regresó porque se consumió. Estado terminal; conserva volumen para capturar estudios pendientes. No es una baja.")
    public ResponseEntity<APIResponse> consumir(@PathVariable Long id,
                                                @Validated @RequestBody CerrarEntregaRequestDTO dto) {
        EntregaMuestra e = entregaApplicationService.marcarConsumida(id, dto.getUuidCierra(), dto.getObservaciones());
        return ResponseEntity.ok(new APIResponse("Entrega marcada como consumida",
                EntregaMapper.toResponseDTO(e), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar entrega", description = "Corrige un registro de entrega: la muestra regresa al biobanco y, de ser posible, a su posición anterior.")
    public ResponseEntity<APIResponse> cancelar(@PathVariable Long id,
                                                @Validated @RequestBody CancelarEntregaRequestDTO dto) {
        EntregaMuestra e = entregaApplicationService.cancelar(id, dto.getUuidUsuario(), dto.getMotivo());
        return ResponseEntity.ok(new APIResponse("Entrega cancelada",
                EntregaMapper.toResponseDTO(e), false, HttpStatus.OK));
    }
}
