package imss.gob.mx.cohorte.controllers.institucion;

import imss.gob.mx.cohorte.controllers.institucion.dto.PermisoAccesoPacientesResponseDTO;
import imss.gob.mx.cohorte.modules.institucion.PermisoAccesoPacientes;
import imss.gob.mx.cohorte.services.institucion.InstitucionJerarquiaService;
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
@RequestMapping("/api/instituciones/{idInstitucion}/permisos-pacientes")
@RequiredArgsConstructor
@Tag(name = "Permisos de Acceso a Pacientes",
        description = "Gestión de permisos para que instituciones hijas puedan ver pacientes de la institución padre.")
@SecurityRequirement(name = "bearerAuth")
public class PermisoAccesoPacientesController {

    private final InstitucionJerarquiaService institucionJerarquiaService;

    @GetMapping("/otorgados")
    @Operation(summary = "Listar permisos otorgados por esta institución")
    public ResponseEntity<APIResponse> listarOtorgados(@PathVariable Long idInstitucion) {
        List<PermisoAccesoPacientes> permisos = institucionJerarquiaService.listarPermisosOtorgados(idInstitucion);
        return ResponseEntity.ok(new APIResponse("Permisos otorgados",
                permisos.stream().map(this::toDTO).toList(), false, HttpStatus.OK));
    }

    @GetMapping("/recibidos")
    @Operation(summary = "Listar permisos activos recibidos por esta institución")
    public ResponseEntity<APIResponse> listarRecibidos(@PathVariable Long idInstitucion) {
        List<PermisoAccesoPacientes> permisos = institucionJerarquiaService.listarPermisosRecibidos(idInstitucion);
        return ResponseEntity.ok(new APIResponse("Permisos recibidos",
                permisos.stream().map(this::toDTO).toList(), false, HttpStatus.OK));
    }

    @PostMapping("/otorgar/{idInstitucionRecibe}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('ENCARGADO')")
    @Operation(summary = "Otorgar acceso a pacientes a una institución hija")
    public ResponseEntity<APIResponse> otorgar(
            @PathVariable Long idInstitucion,
            @PathVariable Long idInstitucionRecibe) {
        PermisoAccesoPacientes permiso = institucionJerarquiaService.otorgarPermiso(idInstitucion, idInstitucionRecibe);
        return ResponseEntity.ok(new APIResponse("Permiso otorgado", toDTO(permiso), false, HttpStatus.OK));
    }

    @DeleteMapping("/revocar/{idInstitucionRecibe}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('ENCARGADO')")
    @Operation(summary = "Revocar acceso a pacientes de una institución hija")
    public ResponseEntity<APIResponse> revocar(
            @PathVariable Long idInstitucion,
            @PathVariable Long idInstitucionRecibe) {
        PermisoAccesoPacientes permiso = institucionJerarquiaService.revocarPermiso(idInstitucion, idInstitucionRecibe);
        return ResponseEntity.ok(new APIResponse("Permiso revocado", toDTO(permiso), false, HttpStatus.OK));
    }

    private PermisoAccesoPacientesResponseDTO toDTO(PermisoAccesoPacientes p) {
        return PermisoAccesoPacientesResponseDTO.builder()
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
