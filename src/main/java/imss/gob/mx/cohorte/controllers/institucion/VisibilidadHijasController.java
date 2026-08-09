package imss.gob.mx.cohorte.controllers.institucion;

import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.institucion.InstitucionVisibilidadService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Qué hijas quiere ver una institución.
 *
 * <p>El permiso de pantalla (INSTITUCIONES_EDITAR) solo dice que el usuario puede
 * tocar instituciones; <em>sobre cuál</em> puede decidir lo comprueba el servicio,
 * porque el id del padre llega por la URL.</p>
 */
@RestController
@RequestMapping("/api/instituciones/{idInstitucion}/visibilidad-hijas")
@RequiredArgsConstructor
@Tag(name = "Visibilidad de instituciones hijas",
        description = "Permite a una institución decidir de qué hijas quiere ver los participantes.")
@SecurityRequirement(name = "bearerAuth")
public class VisibilidadHijasController {

    private final InstitucionVisibilidadService visibilidadService;
    private final InstitucionContextService institucionContextService;

    @GetMapping
    @Operation(summary = "Estado de visibilidad de cada hija directa")
    public ResponseEntity<APIResponse> listar(@PathVariable Long idInstitucion) {
        List<InstitucionVisibilidadService.EstadoHija> estado =
                visibilidadService.estadoDeHijas(idInstitucion);
        return ResponseEntity.ok(new APIResponse("Visibilidad de hijas", estado, false, HttpStatus.OK));
    }

    @PutMapping("/{idHija}")
    @PreAuthorize("hasAuthority('INSTITUCIONES_EDITAR')")
    @Operation(summary = "Decidir si se ven los participantes de una hija concreta")
    public ResponseEntity<APIResponse> fijar(
            @PathVariable Long idInstitucion,
            @PathVariable Long idHija,
            @RequestBody Map<String, Boolean> body) {
        boolean ver = Boolean.TRUE.equals(body.get("verParticipantes"));
        String usuarioUuid = institucionContextService.getUsuarioActual().getUUID();
        visibilidadService.fijarVisibilidad(idInstitucion, idHija, ver, usuarioUuid);
        return ResponseEntity.ok(new APIResponse(
                ver ? "Se mostrarán los participantes de la institución hija"
                    : "Se dejarán de ver los participantes de la institución hija",
                visibilidadService.estadoDeHijas(idInstitucion), false, HttpStatus.OK));
    }

    @PutMapping("/defecto")
    @PreAuthorize("hasAuthority('INSTITUCIONES_EDITAR')")
    @Operation(summary = "Decidir de golpe sobre todas las hijas",
               description = "Fija el valor por defecto de la institución y borra las excepciones por hija. "
                       + "Las sedes que se creen después heredan esta decisión.")
    public ResponseEntity<APIResponse> fijarDefecto(
            @PathVariable Long idInstitucion,
            @RequestBody Map<String, Boolean> body) {
        boolean ver = Boolean.TRUE.equals(body.get("verParticipantes"));
        visibilidadService.fijarDefecto(idInstitucion, ver);
        return ResponseEntity.ok(new APIResponse(
                ver ? "Se verán los participantes de todas las hijas"
                    : "Se dejarán de ver los participantes de las hijas",
                visibilidadService.estadoDeHijas(idInstitucion), false, HttpStatus.OK));
    }
}
