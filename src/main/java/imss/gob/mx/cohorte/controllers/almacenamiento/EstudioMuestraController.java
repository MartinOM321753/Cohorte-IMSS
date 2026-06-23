package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.EstudioMuestraApplicationService;
import imss.gob.mx.cohorte.application.almacenamiento.GestionEstudioMuestraApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra.*;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.EstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.TipoEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestra;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/muestras")
@AllArgsConstructor
@Validated
@Tag(name = "Estudios de Muestras", description = "Gestión de estudios de calidad por muestra e historial de cambios")
@SecurityRequirement(name = "bearerAuth")
public class EstudioMuestraController {

    private final EstudioMuestraApplicationService estudioAppService;
    private final GestionEstudioMuestraApplicationService gestionAppService;

    // ═══════════════════════════════════════════════════════════════════════════
    //  TIPOS DE ESTUDIO DE MUESTRA — Catálogo
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/estudios/tipos")
    @Operation(summary = "Listar tipos de estudio de muestra activos")
    public ResponseEntity<APIResponse> getTiposActivos() {
        List<TipoEstudioMuestra> tipos = gestionAppService.getAllActivos();
        List<TipoEstudioMuestraResponseDTO> dtos = tipos.stream()
                .map(t -> EstudioMuestraMapper.toTipoResponseDTO(t, true))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Tipos de estudio de muestra obtenidos", HttpStatus.OK, false));
    }

    @GetMapping("/estudios/tipos/todos")
    @Operation(summary = "Listar todos los tipos de estudio de muestra (activos e inactivos)")
    public ResponseEntity<APIResponse> getAllTipos() {
        List<TipoEstudioMuestra> tipos = gestionAppService.getAll();
        List<TipoEstudioMuestraResponseDTO> dtos = tipos.stream()
                .map(t -> EstudioMuestraMapper.toTipoResponseDTO(t, true))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Tipos de estudio de muestra obtenidos", HttpStatus.OK, false));
    }

    @PostMapping("/estudios/tipos")
    @Operation(summary = "Crear tipo de estudio de muestra")
    public ResponseEntity<APIResponse> createTipo(@Valid @RequestBody TipoEstudioMuestraRequestDTO dto) {
        TipoEstudioMuestra tipo = new TipoEstudioMuestra();
        tipo.setNombre(dto.getNombre());
        tipo.setDescripcion(dto.getDescripcion());
        TipoEstudioMuestra creado = gestionAppService.createTipo(tipo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse(EstudioMuestraMapper.toTipoResponseDTO(creado, false),
                        "Tipo de estudio de muestra creado", HttpStatus.CREATED, false));
    }

    @PutMapping("/estudios/tipos/{id}")
    @Operation(summary = "Actualizar tipo de estudio de muestra")
    public ResponseEntity<APIResponse> updateTipo(@PathVariable Long id,
                                                   @Valid @RequestBody TipoEstudioMuestraRequestDTO dto) {
        TipoEstudioMuestra datos = new TipoEstudioMuestra();
        datos.setNombre(dto.getNombre());
        datos.setDescripcion(dto.getDescripcion());
        TipoEstudioMuestra actualizado = gestionAppService.updateTipo(id, datos);
        return ResponseEntity.ok(new APIResponse(EstudioMuestraMapper.toTipoResponseDTO(actualizado, true),
                "Tipo de estudio de muestra actualizado", HttpStatus.OK, false));
    }

    @PutMapping("/estudios/tipos/{id}/toggle")
    @Operation(summary = "Activar o desactivar tipo de estudio de muestra")
    public ResponseEntity<APIResponse> toggleTipo(@PathVariable Long id) {
        boolean activo = gestionAppService.toggleTipo(id);
        return ResponseEntity.ok(new APIResponse(activo, "Estado actualizado", HttpStatus.OK, false));
    }

    @GetMapping("/estudios/tipos/{id}/parametros")
    @Operation(summary = "Listar parámetros de un tipo de estudio de muestra")
    public ResponseEntity<APIResponse> getParametrosByTipo(@PathVariable Long id) {
        List<ParametroEstudioMuestra> parametros = gestionAppService.getParametrosByTipo(id);
        List<ParametroEstudioMuestraResponseDTO> dtos = parametros.stream()
                .map(EstudioMuestraMapper::toParametroDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Parámetros obtenidos", HttpStatus.OK, false));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PARÁMETROS DE ESTUDIO DE MUESTRA
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/estudios/parametros")
    @Operation(summary = "Crear parámetro de estudio de muestra")
    public ResponseEntity<APIResponse> createParametro(@Valid @RequestBody ParametroEstudioMuestraRequestDTO dto) {
        ParametroEstudioMuestra parametro = new ParametroEstudioMuestra();
        TipoEstudioMuestra tipo = new TipoEstudioMuestra();
        tipo.setId(dto.getIdTipoEstudioMuestra());
        parametro.setTipoEstudioMuestra(tipo);
        parametro.setNombre(dto.getNombre());
        parametro.setUnidad(dto.getUnidad());
        parametro.setTipo(dto.getTipo());
        parametro.setValorMinimo(dto.getValorMinimo());
        parametro.setValorMaximo(dto.getValorMaximo());
        ParametroEstudioMuestra creado = gestionAppService.createParametro(parametro, dto.getOpciones());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse(EstudioMuestraMapper.toParametroDTO(creado),
                        "Parámetro creado", HttpStatus.CREATED, false));
    }

    @PutMapping("/estudios/parametros/{id}")
    @Operation(summary = "Actualizar parámetro de estudio de muestra")
    public ResponseEntity<APIResponse> updateParametro(@PathVariable Long id,
                                                        @Valid @RequestBody ParametroEstudioMuestraRequestDTO dto) {
        ParametroEstudioMuestra datos = new ParametroEstudioMuestra();
        TipoEstudioMuestra tipo = new TipoEstudioMuestra();
        tipo.setId(dto.getIdTipoEstudioMuestra());
        datos.setTipoEstudioMuestra(tipo);
        datos.setNombre(dto.getNombre());
        datos.setUnidad(dto.getUnidad());
        datos.setTipo(dto.getTipo());
        datos.setValorMinimo(dto.getValorMinimo());
        datos.setValorMaximo(dto.getValorMaximo());
        ParametroEstudioMuestra actualizado = gestionAppService.updateParametro(id, datos, dto.getOpciones());
        return ResponseEntity.ok(new APIResponse(EstudioMuestraMapper.toParametroDTO(actualizado),
                "Parámetro actualizado", HttpStatus.OK, false));
    }

    @DeleteMapping("/estudios/parametros/{id}")
    @Operation(summary = "Eliminar parámetro de estudio de muestra")
    public ResponseEntity<APIResponse> deleteParametro(@PathVariable Long id) {
        gestionAppService.deleteParametro(id);
        return ResponseEntity.ok(new APIResponse("Parámetro eliminado", HttpStatus.OK, false));
    }

    @PostMapping("/estudios/parametros/{id}/opciones")
    @Operation(summary = "Agregar opción a parámetro TEXTO_OPCIONES")
    public ResponseEntity<APIResponse> addOpcion(@PathVariable Long id,
                                                  @Valid @RequestBody OpcionEstudioMuestraRequestDTO dto) {
        gestionAppService.addOpcion(id, dto.getValor());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Opción agregada", HttpStatus.CREATED, false));
    }

    @DeleteMapping("/estudios/parametros/opciones/{opcionId}")
    @Operation(summary = "Eliminar opción de parámetro TEXTO_OPCIONES")
    public ResponseEntity<APIResponse> deleteOpcion(@PathVariable Long opcionId) {
        gestionAppService.deleteOpcion(opcionId);
        return ResponseEntity.ok(new APIResponse("Opción eliminada", HttpStatus.OK, false));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ESTUDIOS POR MUESTRA
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{idMuestra}/estudios")
    @Operation(summary = "Listar estudios realizados a una muestra")
    public ResponseEntity<APIResponse> getEstudiosByMuestra(@PathVariable Long idMuestra) {
        List<EstudioMuestra> estudios = estudioAppService.getByMuestra(idMuestra);
        List<EstudioMuestraResponseDTO> dtos = estudios.stream()
                .map(EstudioMuestraMapper::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Estudios de muestra obtenidos", HttpStatus.OK, false));
    }

    @GetMapping("/estudios/{id}")
    @Operation(summary = "Obtener estudio de muestra por ID")
    public ResponseEntity<APIResponse> getEstudioById(@PathVariable Long id) {
        EstudioMuestra estudio = estudioAppService.getById(id);
        return ResponseEntity.ok(new APIResponse(EstudioMuestraMapper.toResponseDTO(estudio),
                "Estudio de muestra obtenido", HttpStatus.OK, false));
    }

    @PostMapping("/{idMuestra}/estudios")
    @Operation(summary = "Registrar estudio de muestra")
    public ResponseEntity<APIResponse> createEstudio(@PathVariable Long idMuestra,
                                                      @Valid @RequestBody EstudioMuestraRequestDTO dto) {
        EstudioMuestra entity = EstudioMuestraMapper.toEntity(dto);
        EstudioMuestra creado = estudioAppService.create(idMuestra, entity);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse(EstudioMuestraMapper.toResponseDTO(creado),
                        "Estudio de muestra registrado", HttpStatus.CREATED, false));
    }

    @PutMapping("/estudios/{id}")
    @Operation(summary = "Actualizar estudio de muestra")
    public ResponseEntity<APIResponse> updateEstudio(@PathVariable Long id,
                                                      @Valid @RequestBody EstudioMuestraRequestDTO dto) {
        EstudioMuestra entity = EstudioMuestraMapper.toEntity(dto);
        EstudioMuestra actualizado = estudioAppService.update(id, entity);
        return ResponseEntity.ok(new APIResponse(EstudioMuestraMapper.toResponseDTO(actualizado),
                "Estudio de muestra actualizado", HttpStatus.OK, false));
    }

    // Estudios de muestra NO se pueden eliminar — solo editar sus resultados y consumo.

    // ═══════════════════════════════════════════════════════════════════════════
    //  HISTORIAL DE CAMBIOS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{idMuestra}/historial")
    @Operation(summary = "Obtener historial de cambios de una muestra")
    public ResponseEntity<APIResponse> getHistorial(@PathVariable Long idMuestra) {
        List<HistorialCambioMuestra> historial = estudioAppService.getHistorial(idMuestra);
        List<HistorialCambioMuestraResponseDTO> dtos = historial.stream()
                .map(EstudioMuestraMapper::toHistorialDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Historial de cambios obtenido", HttpStatus.OK, false));
    }
}
