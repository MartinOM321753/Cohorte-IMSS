package imss.gob.mx.cohorte.controllers.institucion;

import imss.gob.mx.cohorte.application.institucion.InstitucionApplicationService;
import imss.gob.mx.cohorte.application.institucion.InstitucionModuloApplicationService;
import imss.gob.mx.cohorte.controllers.institucion.dto.InstitucionMapper;
import imss.gob.mx.cohorte.controllers.institucion.dto.InstitucionRequestDTO;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/instituciones")
@RequiredArgsConstructor
@Tag(name = "Instituciones", description = "Gestión de instituciones de reclutamiento (sedes, sucursales) y su jerarquía de permisos")
@SecurityRequirement(name = "bearerAuth")
public class InstitucionController {

    private final InstitucionApplicationService institucionApplicationService;
    private final InstitucionModuloApplicationService institucionModuloApplicationService;
    private final InstitucionContextService institucionContextService;

    @GetMapping("/actual/modulos")
    @Operation(summary = "Listar módulos habilitados de la institución del usuario autenticado",
            description = "Devuelve únicamente los módulos del sistema (BIOBANCO, EXAMENES, CITAS, etc.) " +
                    "habilitados para la institución a la que pertenece el usuario autenticado — " +
                    "se usa para construir el menú/sidebar gated por módulo en el frontend.")
    public ResponseEntity<APIResponse> getModulosHabilitadosActual() {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        // Usa la query dedicada (WHERE habilitado = true) en lugar de cargar todos y filtrar en memoria.
        List<String> modulos = institucionModuloApplicationService.getHabilitadosByInstitucion(idInstitucion).stream()
                .map(im -> im.getModulo().name())
                .toList();
        return ResponseEntity.ok(new APIResponse("Módulos habilitados", modulos, false, HttpStatus.OK));
    }

    @GetMapping("/gestionables")
    @Operation(summary = "IDs de instituciones que el usuario puede gestionar",
            description = "Retorna los IDs de las instituciones donde el usuario es encargado, más todos sus " +
                    "descendientes en la jerarquía, más las que aún no tienen encargado (bootstrap). " +
                    "El frontend usa este listado para habilitar/deshabilitar botones de edición.")
    public ResponseEntity<APIResponse> getGestionables() {
        Set<Long> ids = institucionApplicationService.getIdsGestionables();
        return ResponseEntity.ok(new APIResponse("IDs gestionables", ids, false, HttpStatus.OK));
    }

    @GetMapping("/gestionables-estado")
    @Operation(summary = "IDs de instituciones cuyo estado puede cambiar el usuario",
            description = "Retorna las instituciones que el usuario actual puede activar o desactivar segun la jerarquia.")
    public ResponseEntity<APIResponse> getGestionablesEstado() {
        Set<Long> ids = institucionApplicationService.getIdsConEstadoGestionable();
        return ResponseEntity.ok(new APIResponse("IDs con estado gestionable", ids, false, HttpStatus.OK));
    }

    @GetMapping("/paginado")
    @Operation(summary = "Listar instituciones paginadas",
            description = "Obtiene las instituciones en páginas (parámetros estándar de Spring: page, size, sort) para evitar cargar toda la tabla en una sola respuesta")
    public ResponseEntity<APIResponse> getAllPaginado(Pageable pageable) {
        Page<Institucion> page = institucionApplicationService.getAllPaginado(pageable);
        return ResponseEntity.ok(new APIResponse("Instituciones encontradas", toPageBody(page), false, HttpStatus.OK));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar instituciones (server-side, para selects con autocompletado)",
            description = "Búsqueda paginada por nombre. Diseñada para alimentar selects con buscador integrado: " +
                    "el cliente debe enviar el texto con debounce y el servidor filtra/pagina — nunca cargar la tabla completa al cliente.")
    public ResponseEntity<APIResponse> search(
            @Parameter(description = "Texto a buscar en el nombre") @RequestParam(required = false, defaultValue = "") String q,
            @Parameter(description = "Si es true, sólo retorna instituciones activas") @RequestParam(required = false, defaultValue = "true") boolean soloActivas,
            @Parameter(description = "Página (0-based)") @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(required = false, defaultValue = "10") int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        Page<Institucion> result = institucionApplicationService.search(q, soloActivas, pageable);
        Map<String, Object> body = Map.of(
                "content", InstitucionMapper.toResumenDTOList(result.getContent()),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()
        );
        return ResponseEntity.ok(new APIResponse("Instituciones encontradas", body, false, HttpStatus.OK));
    }

    @GetMapping("/visibles")
    @Operation(summary = "Instituciones visibles para el usuario actual según jerarquía",
            description = "Retorna la propia institución, sus descendientes y las ancestras que le otorgaron " +
                    "acceso — usado para alimentar el selector de institución del filtro de participantes " +
                    "cuando el modo jerarquía está activo.")
    public ResponseEntity<APIResponse> getVisibles() {
        List<Institucion> list = institucionApplicationService.getVisiblesParaJerarquia();
        return ResponseEntity.ok(new APIResponse("Instituciones visibles", InstitucionMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/activas")
    @Operation(summary = "Listar instituciones activas", description = "Lista completa de instituciones activas. Usar sólo en catálogos pequeños — para selects grandes usar /buscar.")
    public ResponseEntity<APIResponse> getAllActivas() {
        List<Institucion> list = institucionApplicationService.getAllActivas();
        return ResponseEntity.ok(new APIResponse("Instituciones activas", InstitucionMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/raices")
    @Operation(summary = "Listar instituciones raíz", description = "Instituciones sin institución padre — son las únicas que pueden otorgar permisos de módulo a otras instituciones.")
    public ResponseEntity<APIResponse> getRaices() {
        List<Institucion> list = institucionApplicationService.getRaices();
        return ResponseEntity.ok(new APIResponse("Instituciones raíz", InstitucionMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}/hijas")
    @Operation(summary = "Listar instituciones hijas", description = "Obtiene las instituciones que dependen directamente de la institución indicada.")
    public ResponseEntity<APIResponse> getHijas(@PathVariable Long id) {
        List<Institucion> list = institucionApplicationService.getHijas(id);
        return ResponseEntity.ok(new APIResponse("Instituciones hijas", InstitucionMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener institución por ID")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        Institucion institucion = institucionApplicationService.getById(id);
        return ResponseEntity.ok(new APIResponse("Institución encontrada", InstitucionMapper.toResponseDTO(institucion), false, HttpStatus.OK));
    }

    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "Obtener institución por UUID")
    public ResponseEntity<APIResponse> getByUuid(@PathVariable String uuid) {
        Institucion institucion = institucionApplicationService.getByUuid(uuid);
        return ResponseEntity.ok(new APIResponse("Institución encontrada", InstitucionMapper.toResponseDTO(institucion), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear institución")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody InstitucionRequestDTO dto) {
        Institucion entity = InstitucionMapper.toEntity(dto);
        Institucion saved = institucionApplicationService.create(entity, dto.getIdTipoInstitucion(), dto.getIdInstitucionPadre(), dto.getUuidEncargado());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Institución creada exitosamente", InstitucionMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar institución")
    public ResponseEntity<APIResponse> update(@PathVariable Long id, @Validated @RequestBody InstitucionRequestDTO dto) {
        Institucion entity = InstitucionMapper.toEntity(dto);
        Institucion updated = institucionApplicationService.update(id, entity, dto.getIdTipoInstitucion(), dto.getIdInstitucionPadre(), dto.getUuidEncargado());
        return ResponseEntity.ok(new APIResponse("Institución actualizada exitosamente", InstitucionMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activar / desactivar institución")
    public ResponseEntity<APIResponse> toggle(@PathVariable Long id) {
        Institucion updated = institucionApplicationService.toggleActivo(id);
        String msg = updated.getActivo() ? "Institución activada" : "Institución desactivada";
        return ResponseEntity.ok(new APIResponse(msg, InstitucionMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    private Map<String, Object> toPageBody(Page<Institucion> page) {
        return Map.of(
                "content", InstitucionMapper.toResponseDTOList(page.getContent()),
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()
        );
    }
}
