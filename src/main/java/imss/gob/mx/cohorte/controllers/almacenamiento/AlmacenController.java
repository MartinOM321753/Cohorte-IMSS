package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.institucion.InstitucionApplicationService;
import imss.gob.mx.cohorte.controllers.institucion.dto.InstitucionMapper;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lookup de instituciones destino para los módulos de almacenamiento.
 *
 * <p>El antiguo módulo «Almacén» se eliminó por obsoleto —los destinos ya son
 * las propias instituciones—, pero su punto de lectura seguía haciendo falta:
 * el configurador de tubos (destino sugerido de un tubo) y los traslados
 * necesitan ofrecer la lista de instituciones sin exigir el permiso
 * {@code INSTITUCIONES_LOOKUP}. Por eso vive bajo {@code /almacenamiento} y no
 * bajo {@code /instituciones}: la regla de seguridad de esta ruta ya está
 * compartida con los permisos de muestras y traslados
 * (ver {@code MainSecurity}, «Almacenes (instituciones destino)»).</p>
 *
 * <p>Es solo lectura. El alta y edición de instituciones vive en
 * {@code /api/instituciones}, con sus propios permisos.</p>
 */
@RestController
@RequestMapping("/api/almacenamiento/almacenes")
@RequiredArgsConstructor
@Tag(name = "Almacenes (instituciones destino)",
     description = "Lookup de instituciones destino para configurar tubos y traslados")
@SecurityRequirement(name = "bearerAuth")
public class AlmacenController {

    private final InstitucionApplicationService institucionApplicationService;

    @GetMapping
    @Operation(summary = "Listar instituciones destino activas",
               description = "Todas las instituciones activas, como posibles destinos de alícuotas.")
    public ResponseEntity<APIResponse> listarDestinos() {
        List<Institucion> activas = institucionApplicationService.getAllActivas();
        return ResponseEntity.ok(new APIResponse(
                "Instituciones destino", InstitucionMapper.toResponseDTOList(activas), false, HttpStatus.OK));
    }
}
