package imss.gob.mx.cohorte.controllers.institucion;

import imss.gob.mx.cohorte.application.institucion.InstitucionModuloApplicationService;
import imss.gob.mx.cohorte.controllers.institucion.dto.InstitucionModuloMapper;
import imss.gob.mx.cohorte.controllers.institucion.dto.InstitucionModuloRequestDTO;
import imss.gob.mx.cohorte.modules.institucion.InstitucionModulo;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instituciones/{idInstitucion}/modulos")
@RequiredArgsConstructor
@Tag(name = "Permisos de Módulo por Institución",
        description = "Gestión de acceso de cada institución a los módulos del sistema (BIOBANCO, EXAMENES, ESTUDIOS_MEDICOS, etc.). " +
                "Sólo una institución ancestra en la jerarquía puede otorgar o revocar estos permisos.")
@SecurityRequirement(name = "bearerAuth")
public class InstitucionModuloController {

    private final InstitucionModuloApplicationService institucionModuloApplicationService;

    @GetMapping
    @Operation(summary = "Listar permisos de módulo de una institución")
    public ResponseEntity<APIResponse> getByInstitucion(@PathVariable Long idInstitucion) {
        List<InstitucionModulo> list = institucionModuloApplicationService.getByInstitucion(idInstitucion);
        return ResponseEntity.ok(new APIResponse("Permisos de módulo", InstitucionModuloMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('ENCARGADO')")
    @Operation(summary = "Otorgar o actualizar un permiso de módulo",
            description = "Sólo procede si la institución indicada en `idOtorgante` es ancestra (padre directo o de nivel superior) " +
                    "de la institución destino — el servicio valida la jerarquía y rechaza la operación en caso contrario.")
    public ResponseEntity<APIResponse> otorgar(
            @Parameter(description = "ID de la institución que recibe el permiso", required = true) @PathVariable Long idInstitucion,
            @Validated @RequestBody InstitucionModuloRequestDTO dto) {
        InstitucionModulo registro = institucionModuloApplicationService.otorgar(
                idInstitucion, dto.getModulo(), dto.getHabilitado(), dto.getIdOtorgante());
        String msg = Boolean.TRUE.equals(dto.getHabilitado()) ? "Permiso otorgado exitosamente" : "Permiso revocado exitosamente";
        return ResponseEntity.ok(new APIResponse(msg, InstitucionModuloMapper.toResponseDTO(registro), false, HttpStatus.OK));
    }

    @DeleteMapping("/{modulo}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('ENCARGADO')")
    @Operation(summary = "Revocar un permiso de módulo",
            description = "Equivalente a deshabilitar el módulo (no elimina el registro — preserva la auditoría de quién otorgó/revocó).")
    public ResponseEntity<APIResponse> revocar(
            @PathVariable Long idInstitucion,
            @PathVariable imss.gob.mx.cohorte.modules.institucion.ModuloSistema modulo,
            @Parameter(description = "ID de la institución otorgante (debe ser ancestra)", required = true) @RequestParam Long idOtorgante) {
        InstitucionModulo registro = institucionModuloApplicationService.revocar(idInstitucion, modulo, idOtorgante);
        return ResponseEntity.ok(new APIResponse("Permiso revocado exitosamente", InstitucionModuloMapper.toResponseDTO(registro), false, HttpStatus.OK));
    }
}
