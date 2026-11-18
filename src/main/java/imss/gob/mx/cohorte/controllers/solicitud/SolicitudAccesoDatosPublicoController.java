package imss.gob.mx.cohorte.controllers.solicitud;

import imss.gob.mx.cohorte.controllers.solicitud.dto.SolicitudAccesoDatosRequestDTO;
import imss.gob.mx.cohorte.modules.solicitud.SolicitudAccesoDatos;
import imss.gob.mx.cohorte.services.solicitud.SolicitudAccesoDatosService;
import imss.gob.mx.cohorte.utils.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/publico/solicitud-datos")
@RequiredArgsConstructor
public class SolicitudAccesoDatosPublicoController {

    private final SolicitudAccesoDatosService service;

    @PostMapping
    public ResponseEntity<APIResponse> enviarSolicitud(
            @Valid @RequestBody SolicitudAccesoDatosRequestDTO dto,
            HttpServletRequest request
    ) {
        // Honeypot: si el campo oculto tiene contenido, simular éxito
        if (dto.getWebsite() != null && !dto.getWebsite().isBlank()) {
            return ResponseEntity.ok(new APIResponse("Solicitud enviada", null, false, HttpStatus.OK));
        }

        String ip = obtenerIP(request);

        if (service.excedeLimiteIP(ip)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new APIResponse("Has enviado demasiadas solicitudes. Intenta más tarde.", true, HttpStatus.TOO_MANY_REQUESTS));
        }

        SolicitudAccesoDatos solicitud = service.registrar(dto, ip);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Solicitud enviada", solicitud.getId(), false, HttpStatus.CREATED));
    }

    private String obtenerIP(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
