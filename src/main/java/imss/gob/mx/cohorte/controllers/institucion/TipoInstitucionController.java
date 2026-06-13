package imss.gob.mx.cohorte.controllers.institucion;

import imss.gob.mx.cohorte.controllers.institucion.dto.TipoInstitucionRequestDTO;
import imss.gob.mx.cohorte.modules.institucion.TipoInstitucion;
import imss.gob.mx.cohorte.services.institucion.TipoInstitucionService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos/tipos-institucion")
@RequiredArgsConstructor
@Tag(name = "Tipos de Institución", description = "Catálogo configurable de tipos de institución (IMSS, INSP, HOSPITAL, etc.)")
@SecurityRequirement(name = "bearerAuth")
public class TipoInstitucionController {

    private final TipoInstitucionService service;

    @GetMapping
    @Operation(summary = "Listar tipos activos", description = "Obtiene los tipos de institución activos. Utilizar en formularios de selección.")
    public ResponseEntity<APIResponse> getAllActivas() {
        List<TipoInstitucion> tipos = service.getAllActivas();
        return ResponseEntity.ok(new APIResponse("Tipos de institución activos", tipos, false, HttpStatus.OK));
    }

    @GetMapping("/todas")
    @Operation(summary = "Listar todos los tipos (admin)")
    public ResponseEntity<APIResponse> getAll() {
        List<TipoInstitucion> tipos = service.getAll();
        return ResponseEntity.ok(new APIResponse("Todos los tipos de institución", tipos, false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener tipo de institución por ID")
    public ResponseEntity<APIResponse> getById(
            @Parameter(description = "ID del tipo", required = true) @PathVariable Long id) {
        TipoInstitucion tipo = service.getById(id);
        return ResponseEntity.ok(new APIResponse("Tipo de institución encontrado", tipo, false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear tipo de institución")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody TipoInstitucionRequestDTO dto) {
        TipoInstitucion tipo = new TipoInstitucion();
        tipo.setNombre(dto.getNombre());
        TipoInstitucion saved = service.create(tipo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Tipo de institución creado exitosamente", saved, false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tipo de institución")
    public ResponseEntity<APIResponse> update(
            @Parameter(description = "ID del tipo", required = true) @PathVariable Long id,
            @Validated @RequestBody TipoInstitucionRequestDTO dto) {
        TipoInstitucion tipo = new TipoInstitucion();
        tipo.setNombre(dto.getNombre());
        TipoInstitucion updated = service.update(id, tipo);
        return ResponseEntity.ok(new APIResponse("Tipo de institución actualizado", updated, false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activar / desactivar tipo de institución")
    public ResponseEntity<APIResponse> toggle(
            @Parameter(description = "ID del tipo", required = true) @PathVariable Long id) {
        TipoInstitucion updated = service.toggleActivo(id);
        String msg = updated.getActivo() ? "Tipo de institución activado" : "Tipo de institución desactivado";
        return ResponseEntity.ok(new APIResponse(msg, updated, false, HttpStatus.OK));
    }
}
