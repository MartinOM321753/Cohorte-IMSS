package imss.gob.mx.cohorte.controllers.configuracion;

import imss.gob.mx.cohorte.services.configuracion.ConfiguracionClaveService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracion/claves")
@RequiredArgsConstructor
@Tag(name = "Configuración por clave", description = "Almacén clave-valor para configuraciones simples")
@SecurityRequirement(name = "bearerAuth")
public class ConfiguracionClaveController {

    private final ConfiguracionClaveService service;

    @GetMapping("/{clave}")
    public ResponseEntity<APIResponse> getValor(@PathVariable String clave) {
        String valor = service.getValor(clave).orElse(null);
        return ResponseEntity.ok(new APIResponse("ok", valor, false, HttpStatus.OK));
    }

    @PutMapping("/{clave}")
    public ResponseEntity<APIResponse> setValor(
            @PathVariable String clave,
            @RequestBody String valor) {
        service.setValor(clave, valor);
        return ResponseEntity.ok(new APIResponse("ok", null, false, HttpStatus.OK));
    }
}
