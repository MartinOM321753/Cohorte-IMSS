package imss.gob.mx.cohorte.audit.controller;

import imss.gob.mx.cohorte.audit.dto.BitacoraAccesoResponseDTO;
import imss.gob.mx.cohorte.audit.dto.BitacoraAccionResponseDTO;
import imss.gob.mx.cohorte.audit.service.BitacoraQueryService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/bitacora")
@AllArgsConstructor
@Tag(name = "Bitácora", description = "Consulta de registros de acceso y acciones del sistema (solo ADMINISTRADOR)")
public class BitacoraController {

    private final BitacoraQueryService queryService;

    // ── Accesos ──────────────────────────────────────────────────────────────

    @GetMapping("/accesos")
    @Operation(summary = "Listar bitácora de accesos",
               description = "Devuelve eventos de login, logout y login fallido. Filtrable por fecha, usuario y tipo.")
    public ResponseEntity<APIResponse> getAccesos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuarioUuid,
            @RequestParam(required = false) String tipoEvento,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<BitacoraAccesoResponseDTO> resultado =
                queryService.consultarAccesos(desde, hasta, usuarioUuid, tipoEvento, page, size);

        return ResponseEntity.ok(new APIResponse(
                "Bitácora de accesos", resultado, false, HttpStatus.OK));
    }

    // ── Acciones ─────────────────────────────────────────────────────────────

    @GetMapping("/acciones")
    @Operation(summary = "Listar bitácora de acciones",
               description = "Devuelve acciones de escritura (CREAR, ACTUALIZAR, ELIMINAR). Filtrable por fecha, usuario, tipo y entidad.")
    public ResponseEntity<APIResponse> getAcciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String usuarioUuid,
            @RequestParam(required = false) String tipoAccion,
            @RequestParam(required = false) String entidad,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<BitacoraAccionResponseDTO> resultado =
                queryService.consultarAcciones(desde, hasta, usuarioUuid, tipoAccion, entidad, page, size);

        return ResponseEntity.ok(new APIResponse(
                "Bitácora de acciones", resultado, false, HttpStatus.OK));
    }
}
