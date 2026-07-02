package imss.gob.mx.cohorte.controllers.permisos;

import imss.gob.mx.cohorte.application.PermisoAdminApplicationService;
import imss.gob.mx.cohorte.controllers.permisos.dto.*;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/permisos")
@RequiredArgsConstructor
@Tag(name = "Administración de Permisos",
        description = "CRUD de roles, permisos por usuario, concesiones/restricciones individuales y bitácora")
@SecurityRequirement(name = "bearerAuth")
public class PermisoAdminController {

    private final PermisoAdminApplicationService permisoAdminService;

    // ── Catálogo ────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISOS_VER')")
    @Operation(summary = "Catálogo completo de permisos agrupado por módulo")
    public ResponseEntity<APIResponse> getCatalogo() {
        Map<String, List<PermisoDTO>> catalogo = permisoAdminService.getCatalogoAgrupado();
        return ResponseEntity.ok(new APIResponse("Catálogo de permisos", catalogo, false, HttpStatus.OK));
    }

    // ── Roles ───────────────────────────────────────────────────────────────────

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('PERMISOS_VER')")
    @Operation(summary = "Roles con sus permisos plantilla")
    public ResponseEntity<APIResponse> getRolesConPermisos() {
        List<RolConPermisosDTO> roles = permisoAdminService.getRolesConPermisos();
        return ResponseEntity.ok(new APIResponse("Roles con permisos", roles, false, HttpStatus.OK));
    }

    @PutMapping("/roles/{idRol}")
    @PreAuthorize("hasAuthority('PERMISOS_EDITAR')")
    @Operation(summary = "Actualizar permisos plantilla de un rol")
    public ResponseEntity<APIResponse> actualizarPermisosRol(
            @PathVariable Long idRol,
            @Valid @RequestBody ActualizarPermisosRolRequestDTO request) {
        RolConPermisosDTO resultado = permisoAdminService.actualizarPermisosRol(idRol, request.getCodigosPermisos());
        return ResponseEntity.ok(new APIResponse("Permisos del rol actualizados", resultado, false, HttpStatus.OK));
    }

    // ── Permisos de usuario ─────────────────────────────────────────────────────

    @GetMapping("/usuarios/{uuid}")
    @PreAuthorize("hasAuthority('PERMISOS_VER')")
    @Operation(summary = "Resumen completo de permisos de un usuario (efectivos + origen)")
    public ResponseEntity<APIResponse> getPermisosUsuario(@PathVariable String uuid) {
        UsuarioPermisosResumenDTO resumen = permisoAdminService.getPermisosUsuario(uuid);
        return ResponseEntity.ok(new APIResponse("Permisos del usuario", resumen, false, HttpStatus.OK));
    }

    @GetMapping("/usuarios/{uuid}/roles")
    @PreAuthorize("hasAuthority('PERMISOS_VER')")
    @Operation(summary = "Roles asignados a un usuario")
    public ResponseEntity<APIResponse> getRolesUsuario(@PathVariable String uuid) {
        List<RolAsignadoDTO> roles = permisoAdminService.getRolesUsuario(uuid);
        return ResponseEntity.ok(new APIResponse("Roles del usuario", roles, false, HttpStatus.OK));
    }

    @PostMapping("/usuarios/{uuid}/roles")
    @PreAuthorize("hasAuthority('PERMISOS_EDITAR')")
    @Operation(summary = "Asignar un rol adicional a un usuario")
    public ResponseEntity<APIResponse> asignarRol(
            @PathVariable String uuid,
            @Valid @RequestBody AsignarRolRequestDTO request) {
        RolAsignadoDTO resultado = permisoAdminService.asignarRol(uuid, request.getIdRol());
        return ResponseEntity.ok(new APIResponse("Rol asignado exitosamente", resultado, false, HttpStatus.OK));
    }

    @DeleteMapping("/usuarios/{uuid}/roles/{idRol}")
    @PreAuthorize("hasAuthority('PERMISOS_EDITAR')")
    @Operation(summary = "Quitar un rol a un usuario")
    public ResponseEntity<APIResponse> quitarRol(
            @PathVariable String uuid,
            @PathVariable Long idRol) {
        permisoAdminService.quitarRol(uuid, idRol);
        return ResponseEntity.ok(new APIResponse("Rol removido exitosamente", false, HttpStatus.OK));
    }

    // ── Permisos individuales ───────────────────────────────────────────────────

    @PostMapping("/usuarios/{uuid}/concesiones")
    @PreAuthorize("hasAuthority('PERMISOS_EDITAR')")
    @Operation(summary = "Conceder un permiso individual a un usuario")
    public ResponseEntity<APIResponse> concederPermiso(
            @PathVariable String uuid,
            @Valid @RequestBody PermisoIndividualRequestDTO request) {
        PermisoIndividualDTO resultado = permisoAdminService.concederPermiso(uuid, request);
        return ResponseEntity.ok(new APIResponse("Permiso concedido exitosamente", resultado, false, HttpStatus.OK));
    }

    @PostMapping("/usuarios/{uuid}/restricciones")
    @PreAuthorize("hasAuthority('PERMISOS_EDITAR')")
    @Operation(summary = "Restringir un permiso individual a un usuario")
    public ResponseEntity<APIResponse> restringirPermiso(
            @PathVariable String uuid,
            @Valid @RequestBody PermisoIndividualRequestDTO request) {
        PermisoIndividualDTO resultado = permisoAdminService.restringirPermiso(uuid, request);
        return ResponseEntity.ok(new APIResponse("Permiso restringido exitosamente", resultado, false, HttpStatus.OK));
    }

    @DeleteMapping("/usuarios/{uuid}/individuales/{id}")
    @PreAuthorize("hasAuthority('PERMISOS_EDITAR')")
    @Operation(summary = "Revocar una concesión o restricción individual")
    public ResponseEntity<APIResponse> quitarPermisoIndividual(
            @PathVariable String uuid,
            @PathVariable Long id) {
        permisoAdminService.quitarPermisoIndividual(uuid, id);
        return ResponseEntity.ok(new APIResponse("Permiso individual revocado", false, HttpStatus.OK));
    }

    // ── Bitácora ────────────────────────────────────────────────────────────────

    @GetMapping("/bitacora")
    @PreAuthorize("hasAuthority('PERMISOS_VER')")
    @Operation(summary = "Historial de cambios de permisos (paginado)")
    public ResponseEntity<APIResponse> getBitacora(
            @RequestParam(required = false) String uuid,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<BitacoraPermisoDTO> page = permisoAdminService.getBitacora(uuid, pageable);
        return ResponseEntity.ok(new APIResponse("Bitácora de permisos", page, false, HttpStatus.OK));
    }
}
