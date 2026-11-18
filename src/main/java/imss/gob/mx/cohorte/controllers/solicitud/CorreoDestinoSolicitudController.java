package imss.gob.mx.cohorte.controllers.solicitud;

import imss.gob.mx.cohorte.controllers.solicitud.dto.CorreoDestinoRequestDTO;
import imss.gob.mx.cohorte.modules.solicitud.CorreoDestinoSolicitud;
import imss.gob.mx.cohorte.services.solicitud.SolicitudAccesoDatosService;
import imss.gob.mx.cohorte.utils.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracion/correos-solicitud")
@RequiredArgsConstructor
public class CorreoDestinoSolicitudController {

    private final SolicitudAccesoDatosService service;

    @GetMapping
    public ResponseEntity<APIResponse> listar() {
        List<CorreoDestinoSolicitud> correos = service.listarCorreosDestino();
        return ResponseEntity.ok(new APIResponse("Correos destino", correos, false, HttpStatus.OK));
    }

    @PostMapping
    public ResponseEntity<APIResponse> agregar(@Valid @RequestBody CorreoDestinoRequestDTO dto) {
        CorreoDestinoSolicitud c = service.agregarCorreoDestino(dto.getEmail(), dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Correo agregado", c, false, HttpStatus.CREATED));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<APIResponse> toggle(@PathVariable Long id) {
        CorreoDestinoSolicitud c = service.toggleActivoCorreoDestino(id);
        return ResponseEntity.ok(new APIResponse("Estado actualizado", c, false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse> eliminar(@PathVariable Long id) {
        service.eliminarCorreoDestino(id);
        return ResponseEntity.ok(new APIResponse("Correo eliminado", true, HttpStatus.OK));
    }
}
