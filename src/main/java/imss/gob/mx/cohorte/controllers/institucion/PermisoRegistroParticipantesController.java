package imss.gob.mx.cohorte.controllers.institucion;

import imss.gob.mx.cohorte.controllers.institucion.dto.PermisoRegistroParticipantesResponseDTO;
import imss.gob.mx.cohorte.modules.institucion.PermisoRegistroParticipantes;
import imss.gob.mx.cohorte.services.institucion.InstitucionRegistroService;
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

@RestController
@RequestMapping("/api/instituciones/{idInstitucion}/permisos-registro")
@RequiredArgsConstructor
@Tag(name = "Permisos de Registro de Participantes",
        description = "Gestión de la autorización para que una institución hija registre participantes a nombre de otras del mismo grupo.")
@SecurityRequirement(name = "bearerAuth")
public class PermisoRegistroParticipantesController {

    private final InstitucionRegistroService institucionRegistroService;

    @GetMapping("/otorgados")
    @Operation(summary = "Listar autorizaciones otorgadas por esta institución")
    public ResponseEntity<APIResponse> listarOtorgados(@PathVariable Long idInstitucion) {
        List<PermisoRegistroParticipantes> permisos = institucionRegistroService.listarPermisosOtorgados(idInstitucion);
        return ResponseEntity.ok(new APIResponse("Autorizaciones otorgadas",
                permisos.stream().map(this::toDTO).toList(), false, HttpStatus.OK));
    }

    @GetMapping("/recibidos")
    @Operation(summary = "Listar autorizaciones activas recibidas por esta institución")
    public ResponseEntity<APIResponse> listarRecibidos(@PathVariable Long idInstitucion) {
        List<PermisoRegistroParticipantes> permisos = institucionRegistroService.listarPermisosRecibidos(idInstitucion);
        return ResponseEntity.ok(new APIResponse("Autorizaciones recibidas",
                permisos.stream().map(this::toDTO).toList(), false, HttpStatus.OK));
    }

    @PostMapping("/otorgar/{idInstitucionRecibe}")
    @PreAuthorize("hasAuthority('INSTITUCIONES_EDITAR')")
    @Operation(summary = "Autorizar a una institución hija a registrar participantes dentro del grupo")
    public ResponseEntity<APIResponse> otorgar(
            @PathVariable Long idInstitucion,
            @PathVariable Long idInstitucionRecibe) {
        PermisoRegistroParticipantes permiso = institucionRegistroService.otorgarPermiso(idInstitucion, idInstitucionRecibe);
        return ResponseEntity.ok(new APIResponse("Autorización otorgada", toDTO(permiso), false, HttpStatus.OK));
    }

    @DeleteMapping("/revocar/{idInstitucionRecibe}")
    @PreAuthorize("hasAuthority('INSTITUCIONES_EDITAR')")
    @Operation(summary = "Revocar la autorización de registro de una institución hija")
    public ResponseEntity<APIResponse> revocar(
            @PathVariable Long idInstitucion,
            @PathVariable Long idInstitucionRecibe) {
        PermisoRegistroParticipantes permiso = institucionRegistroService.revocarPermiso(idInstitucion, idInstitucionRecibe);
        return ResponseEntity.ok(new APIResponse("Autorización revocada", toDTO(permiso), false, HttpStatus.OK));
    }

    private PermisoRegistroParticipantesResponseDTO toDTO(PermisoRegistroParticipantes p) {
        return PermisoRegistroParticipantesResponseDTO.builder()
                .id(p.getId())
                .institucionOtorgaId(p.getInstitucionOtorga().getId())
                .institucionOtorgaNombre(p.getInstitucionOtorga().getNombre())
                .institucionRecibeId(p.getInstitucionRecibe().getId())
                .institucionRecibeNombre(p.getInstitucionRecibe().getNombre())
                .habilitado(p.getHabilitado())
                .fechaOtorgamiento(p.getFechaOtorgamiento())
                .build();
    }
}
