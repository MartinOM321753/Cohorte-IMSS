package imss.gob.mx.cohorte.controllers.cuestionarios;

import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;

import imss.gob.mx.cohorte.controllers.cuestionarios.dto.CuestionarioDTOs.*;
import imss.gob.mx.cohorte.controllers.cuestionarios.dto.CuestionarioMapper;
import imss.gob.mx.cohorte.modules.cuestionarios.CuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.PresentacionSeccion;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.DirigidoA;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.services.cuestionarios.TipoCuestionarioService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequireModulo(ModuloSistema.CUESTIONARIOS)
@RequestMapping("/api/cuestionarios/tipos")
@AllArgsConstructor
@Validated
@Tag(name = "Cuestionarios - catalogo", description = "Instrumentos y sus bloques de preguntas")
@SecurityRequirement(name = "bearerAuth")
public class TipoCuestionarioController {

    private final TipoCuestionarioService service;
    private final VariableCuestionarioRepository variableRepository;
    private final CuestionarioRepository cuestionarioRepository;

    @GetMapping("/todos")
    @Operation(summary = "Listar instrumentos", description = "Activos e inactivos, para el catalogo")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_CATALOGO_ACCEDER')")
    public ResponseEntity<APIResponse> todos() {
        return ResponseEntity.ok(new APIResponse(
                service.todos().stream().map(this::aDTO).toList(),
                "Cuestionarios obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping
    @Operation(summary = "Listar instrumentos activos")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_TIPOS_LOOKUP', 'CUESTIONARIOS_LLENADO_ACCEDER', 'CUESTIONARIOS_CATALOGO_ACCEDER')")
    public ResponseEntity<APIResponse> activos() {
        return ResponseEntity.ok(new APIResponse(
                service.activos().stream().map(this::aDTO).toList(),
                "Cuestionarios obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un instrumento")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_TIPOS_LOOKUP', 'CUESTIONARIOS_LLENADO_ACCEDER', 'CUESTIONARIOS_CATALOGO_ACCEDER')")
    public ResponseEntity<APIResponse> uno(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(aDTO(service.uno(id)),
                "Cuestionario obtenido correctamente", HttpStatus.OK, false));
    }

    @PostMapping
    @Operation(summary = "Crear instrumento")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_CREAR')")
    public ResponseEntity<APIResponse> crear(@Valid @RequestBody TipoRequest dto) {
        TipoCuestionario tipo = new TipoCuestionario();
        aplicar(tipo, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                aDTO(service.crear(tipo)), "Cuestionario creado correctamente",
                HttpStatus.CREATED, false));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar instrumento")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody TipoRequest dto) {
        TipoCuestionario datos = new TipoCuestionario();
        aplicar(datos, dto);
        return ResponseEntity.ok(new APIResponse(aDTO(service.actualizar(id, datos)),
                "Cuestionario actualizado correctamente", HttpStatus.OK, false));
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(summary = "Retirar el instrumento de uso",
            description = "No borra nada: un cuestionario contestado sin su catalogo es un monton de digitos sin significado")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> desactivar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.ok(new APIResponse(
                "Cuestionario retirado de uso", HttpStatus.OK, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar instrumento", description = "Solo si nadie lo ha contestado")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_ELIMINAR')")
    public ResponseEntity<APIResponse> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(new APIResponse(
                "Cuestionario eliminado correctamente", HttpStatus.OK, false));
    }

    // ── Bloques ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/bloques")
    @Operation(summary = "Listar los bloques del instrumento")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_LLENADO_ACCEDER', 'CUESTIONARIOS_CATALOGO_ACCEDER')")
    public ResponseEntity<APIResponse> bloques(@PathVariable Long id) {
        List<SeccionResponse> dtos = service.secciones(id).stream()
                .map(s -> CuestionarioMapper.seccion(s, contarDe(s)))
                .toList();
        return ResponseEntity.ok(new APIResponse(dtos,
                "Bloques obtenidos correctamente", HttpStatus.OK, false));
    }

    @PostMapping("/{id}/bloques")
    @Operation(summary = "Crear bloque")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> crearBloque(@PathVariable Long id,
                                                    @Valid @RequestBody SeccionRequest dto) {
        SeccionCuestionario seccion = new SeccionCuestionario();
        seccion.setNombre(dto.getNombre());
        seccion.setTitulo(dto.getTitulo());
        seccion.setDescripcion(dto.getDescripcion());
        seccion.setPagina(dto.getPagina());
        if (dto.getPresentacion() != null) {
            seccion.setPresentacion(PresentacionSeccion.valueOf(dto.getPresentacion()));
        }
        if (dto.getOrden() != null) seccion.setOrden(dto.getOrden());
        SeccionCuestionario creada = service.crearSeccion(id, seccion);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                CuestionarioMapper.seccion(creada, 0), "Bloque creado correctamente",
                HttpStatus.CREATED, false));
    }

    @PutMapping("/bloques/{idBloque}")
    @Operation(summary = "Actualizar bloque")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> actualizarBloque(@PathVariable Long idBloque,
                                                         @Valid @RequestBody SeccionRequest dto) {
        SeccionCuestionario datos = new SeccionCuestionario();
        datos.setNombre(dto.getNombre());
        datos.setTitulo(dto.getTitulo());
        datos.setDescripcion(dto.getDescripcion());
        datos.setPagina(dto.getPagina());
        // Null a proposito cuando el formulario no la manda: el servicio solo copia lo
        // que venga, y asi una pantalla vieja no devuelve una rejilla a lista.
        datos.setPresentacion(dto.getPresentacion() == null
                ? null : PresentacionSeccion.valueOf(dto.getPresentacion()));
        datos.setOrden(dto.getOrden());
        SeccionCuestionario actualizada = service.actualizarSeccion(idBloque, datos);
        return ResponseEntity.ok(new APIResponse(
                CuestionarioMapper.seccion(actualizada, contarDe(actualizada)),
                "Bloque actualizado correctamente", HttpStatus.OK, false));
    }

    @PatchMapping("/{id}/bloques/orden")
    @Operation(summary = "Reordenar los bloques",
            description = "De una sola vez: a medio camino el formulario no corresponde al papel")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> reordenarBloques(@PathVariable Long id,
                                                         @Valid @RequestBody ReordenarRequest dto) {
        service.reordenarSecciones(id, dto.getIds());
        return ResponseEntity.ok(new APIResponse("Bloques reordenados", HttpStatus.OK, false));
    }

    @DeleteMapping("/bloques/{idBloque}")
    @Operation(summary = "Eliminar bloque", description = "Solo si no tiene preguntas")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_ELIMINAR')")
    public ResponseEntity<APIResponse> eliminarBloque(@PathVariable Long idBloque) {
        service.eliminarSeccion(idBloque);
        return ResponseEntity.ok(new APIResponse("Bloque eliminado", HttpStatus.OK, false));
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private void aplicar(TipoCuestionario tipo, TipoRequest dto) {
        tipo.setClave(dto.getClave().toUpperCase());
        tipo.setNombre(dto.getNombre());
        tipo.setDescripcion(dto.getDescripcion());
        tipo.setVersion(dto.getVersion());
        if (dto.getColumnaEtiqueta() != null && !dto.getColumnaEtiqueta().isBlank()) {
            tipo.setColumnaEtiqueta(dto.getColumnaEtiqueta());
        }
        tipo.setDirigidoA(dto.getDirigidoA() == null
                ? DirigidoA.AMBOS : DirigidoA.valueOf(dto.getDirigidoA()));
        tipo.setEdadMinima(dto.getEdadMinima());
        tipo.setEdadMaxima(dto.getEdadMaxima());
        if (dto.getEdadMinima() != null && dto.getEdadMaxima() != null
                && dto.getEdadMinima() > dto.getEdadMaxima()) {
            // Un rango invertido no se corrige en silencio: significa que quien lo
            // escribio tenia otra intencion, y adivinarla produciria un criterio que no
            // es el que se pidio.
            throw new ObjConflictException("La edad minima (" + dto.getEdadMinima()
                    + ") no puede ser mayor que la maxima (" + dto.getEdadMaxima() + ").");
        }
        if (dto.getPermiteAutollenado() != null) {
            tipo.setPermiteAutollenado(dto.getPermiteAutollenado());
        }
        if (dto.getActivo() != null) tipo.setActivo(dto.getActivo());
    }

    private TipoResponse aDTO(TipoCuestionario tipo) {
        return CuestionarioMapper.tipo(tipo,
                variableRepository.countByTipoCuestionario_Id(tipo.getId()),
                variableRepository.contarSinDocumentar(tipo.getId()),
                service.secciones(tipo.getId()).size(),
                cuestionarioRepository.countByTipoCuestionario_Id(tipo.getId()));
    }

    private long contarDe(SeccionCuestionario seccion) {
        return variableRepository.findDeSeccionConOpciones(seccion.getId()).size();
    }
}
