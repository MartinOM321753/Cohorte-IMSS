package imss.gob.mx.cohorte.controllers.solicitud;

import imss.gob.mx.cohorte.controllers.solicitud.dto.SolicitudAccesoDatosResponseDTO;
import imss.gob.mx.cohorte.services.solicitud.SolicitudAccesoDatosService;
import imss.gob.mx.cohorte.utils.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracion/solicitudes-datos")
@RequiredArgsConstructor
public class SolicitudAccesoDatosAdminController {

    private final SolicitudAccesoDatosService service;

    @GetMapping
    public ResponseEntity<APIResponse> listar() {
        List<SolicitudAccesoDatosResponseDTO> solicitudes = service.listarTodas().stream()
                .map(SolicitudAccesoDatosResponseDTO::from)
                .toList();
        return ResponseEntity.ok(new APIResponse("Solicitudes de acceso a datos", solicitudes, false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<APIResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado
    ) {
        SolicitudAccesoDatosResponseDTO dto = SolicitudAccesoDatosResponseDTO.from(
                service.cambiarEstado(id, estado)
        );
        return ResponseEntity.ok(new APIResponse("Estado actualizado", dto, false, HttpStatus.OK));
    }
}
