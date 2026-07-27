package imss.gob.mx.cohorte.controllers.estudios;

import imss.gob.mx.cohorte.application.GestionEstudiosApplicationService;
import imss.gob.mx.cohorte.controllers.estudios.dto.*;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.utils.APIResponse;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/estudios/tipos")
@AllArgsConstructor
@Validated
@Tag(name = "Tipos de Estudio", description = "Gestión del catálogo de tipos de estudio médico")
@SecurityRequirement(name = "bearerAuth")
public class TipoEstudioController {

    private final GestionEstudiosApplicationService gestionEstudiosApplicationService;
    private final EstudioMedicoRepository estudioMedicoRepository;

    @GetMapping("/todos")
    @Operation(summary = "Listar todos los tipos de estudio", description = "Obtiene todos los tipos de estudio (activos e inactivos) para gestión en catálogos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    @PreAuthorize("hasAuthority('ESTUDIOS_CATALOGO_ACCEDER')")
    public ResponseEntity<APIResponse> getAllTipos() {
        List<TipoEstudio> tipos = gestionEstudiosApplicationService.getAllTipos();
        List<TipoEstudioResponseDTO> dtos = tipos.stream()
            .map(this::toTipoDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Tipos de estudio obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping
    @Operation(summary = "Listar tipos de estudio activos", description = "Obtiene una lista de todos los tipos de estudio que se encuentran activos")
    @PreAuthorize("hasAnyAuthority('ESTUDIOS_TIPOS_LOOKUP', 'ESTUDIOS_LLENADO_ACCEDER', 'ESTUDIOS_CATALOGO_ACCEDER')")
    public ResponseEntity<APIResponse> getTiposActivos() {
        List<TipoEstudio> tipos = gestionEstudiosApplicationService.getAllByEstatus();
        List<TipoEstudioResponseDTO> dtos = tipos.stream()
            .map(this::toTipoDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Tipos de estudio obtenidos correctamente", HttpStatus.OK, false));
    }

    @PostMapping
    @Operation(summary = "Crear tipo de estudio", description = "Registra un nuevo tipo de estudio en el catálogo del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Tipo de estudio creado exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    @PreAuthorize("hasAuthority('ESTUDIOS_TIPOS_CREAR')")
    public ResponseEntity<APIResponse> createTipo(@Valid @RequestBody TipoEstudioRequestDTO dto) {
        TipoEstudio tipo = new TipoEstudio();
        tipo.setNombre(dto.getNombre());
        tipo.setDescripcion(dto.getDescripcion());
        tipo.setTipoCapturaDefecto(dto.getTipoCapturaDefecto() != null ? dto.getTipoCapturaDefecto() : "NORMAL");
        tipo.setActivo(true);
        tipo.setFechaCreacion(LocalDateTime.now());
        TipoEstudio creado = gestionEstudiosApplicationService.createTipoService(tipo);
        TipoEstudioResponseDTO responseDTO = TipoEstudioResponseDTO.builder()
            .id(creado.getId())
            .nombre(creado.getNombre())
            .descripcion(creado.getDescripcion())
            .tipoCapturaDefecto(creado.getTipoCapturaDefecto())
            .activo(creado.getActivo())
            .tieneResultados(false)
            .build();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(responseDTO, "Tipo de estudio creado correctamente", HttpStatus.CREATED, false));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar tipo de estudio", description = "Actualiza la información de un tipo de estudio existente identificado por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    @PreAuthorize("hasAuthority('ESTUDIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> updateTipo(
        @Parameter(description = "Identificador único del tipo de estudio a actualizar", required = true)
        @PathVariable Long id,
        @Valid @RequestBody TipoEstudioRequestDTO dto) {
        TipoEstudio existente = gestionEstudiosApplicationService.getOne(id);
        if (dto.getTipoCapturaDefecto() != null
                && !dto.getTipoCapturaDefecto().equals(existente.getTipoCapturaDefecto())
                && estudioMedicoRepository.existsByTipoEstudio_Id(id)) {
            throw new ObjConflictException("No se puede cambiar el modo de captura porque la plantilla tiene estudios registrados");
        }
        existente.setNombre(dto.getNombre());
        existente.setDescripcion(dto.getDescripcion());
        existente.setTipoCapturaDefecto(dto.getTipoCapturaDefecto() != null ? dto.getTipoCapturaDefecto() : existente.getTipoCapturaDefecto());
        TipoEstudio actualizado = gestionEstudiosApplicationService.update(existente);
        TipoEstudioResponseDTO responseDTO = TipoEstudioResponseDTO.builder()
            .id(actualizado.getId())
            .nombre(actualizado.getNombre())
            .descripcion(actualizado.getDescripcion())
            .tipoCapturaDefecto(actualizado.getTipoCapturaDefecto())
            .activo(actualizado.getActivo())
            .tieneResultados(estudioMedicoRepository.existsByTipoEstudio_Id(actualizado.getId()))
            .build();
        return ResponseEntity.ok(new APIResponse(responseDTO, "Tipo de estudio actualizado correctamente", HttpStatus.OK, false));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Activar o desactivar tipo de estudio", description = "Cambia el estado activo/inactivo de un tipo de estudio identificado por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    @PreAuthorize("hasAuthority('ESTUDIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> toggleTipo(
        @Parameter(description = "Identificador único del tipo de estudio", required = true)
        @PathVariable Long id) {
        Boolean activo = gestionEstudiosApplicationService.Active(id);
        return ResponseEntity.ok(new APIResponse(activo, "Estado del tipo de estudio actualizado correctamente", HttpStatus.OK, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar tipo de estudio", description = "Elimina un tipo de estudio y todos sus parámetros. Solo es posible si no tiene estudios registrados.")
    @PreAuthorize("hasAuthority('ESTUDIOS_TIPOS_ELIMINAR')")
    public ResponseEntity<APIResponse> deleteTipo(@PathVariable Long id) {
        gestionEstudiosApplicationService.deleteTipo(id);
        return ResponseEntity.ok(new APIResponse("Plantilla eliminada", HttpStatus.OK, false));
    }

    @GetMapping("/{id}/parametros")
    @Operation(summary = "Listar parámetros de un tipo de estudio", description = "Obtiene todos los parámetros asociados a un tipo de estudio identificado por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tipo de estudio no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    @PreAuthorize("hasAnyAuthority('ESTUDIOS_TIPOS_LOOKUP', 'ESTUDIOS_LLENADO_ACCEDER', 'ESTUDIOS_CATALOGO_ACCEDER')")
    public ResponseEntity<APIResponse> getParametrosByTipo(
        @Parameter(description = "Identificador único del tipo de estudio", required = true)
        @PathVariable Long id) {
        List<ParametroEstudio> parametros = gestionEstudiosApplicationService.getParametrosByTipo(id);
        List<ParametroEstudioResponseDTO> dtos = parametros.stream()
            .map(EstudioMapper::toParametroDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Parámetros obtenidos correctamente", HttpStatus.OK, false));
    }

    private TipoEstudioResponseDTO toTipoDTO(TipoEstudio t) {
        return TipoEstudioResponseDTO.builder()
            .id(t.getId())
            .nombre(t.getNombre())
            .descripcion(t.getDescripcion())
            .tipoCapturaDefecto(t.getTipoCapturaDefecto())
            .activo(t.getActivo())
            .tieneResultados(estudioMedicoRepository.existsByTipoEstudio_Id(t.getId()))
            .parametroEstudios(t.getParametros() != null
                ? t.getParametros().stream().map(EstudioMapper::toParametroDTO).collect(Collectors.toList())
                : List.of())
            .build();
    }
}
