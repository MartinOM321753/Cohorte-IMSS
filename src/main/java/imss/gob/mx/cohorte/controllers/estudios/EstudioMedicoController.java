package imss.gob.mx.cohorte.controllers.estudios;

import imss.gob.mx.cohorte.application.EstudiosApplicationService;
import imss.gob.mx.cohorte.application.GestionEstudiosApplicationService;
import imss.gob.mx.cohorte.controllers.estudios.dto.*;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import jakarta.validation.constraints.NotBlank;
import imss.gob.mx.cohorte.utils.APIResponse;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/estudios")
@AllArgsConstructor
@Validated
@Tag(name = "Estudios Médicos", description = "Gestión de estudios médicos y catálogos")
@SecurityRequirement(name = "bearerAuth")
public class EstudioMedicoController {

    private final EstudiosApplicationService estudiosApplicationService;
    private final GestionEstudiosApplicationService gestionEstudiosApplicationService;

    @GetMapping
    @Operation(summary = "Listar todos los estudios médicos", description = "Obtiene una lista completa de todos los estudios médicos registrados en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAll() {
        List<EstudioMedico> estudios = estudiosApplicationService.getAllEstudios();
        List<EstudioListRequestDTO> dtos = EstudioMapper.toResponseDTOList(estudios);
        return ResponseEntity.ok(new APIResponse(dtos, "Estudios obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener estudio médico por ID", description = "Obtiene los detalles de un estudio médico específico utilizando su identificador único")
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
    public ResponseEntity<APIResponse> getById(
        @Parameter(description = "Identificador único del estudio médico", required = true)
        @PathVariable Long id) {
        EstudioMedico estudio = estudiosApplicationService.getEstudio(id);
        EstudioMedicoResponseDTO dto = EstudioMapper.toResponseDTO(estudio);
        return ResponseEntity.ok(new APIResponse(dto, "Estudio obtenido correctamente", HttpStatus.OK, false));
    }

    @PostMapping
    @Operation(summary = "Crear estudio médico con resultados", description = "Registra un nuevo estudio médico incluyendo sus resultados asociados")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Estudio creado exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Valid @RequestBody EstudioMedicoRequestDTO dto) {
        EstudioMedico entity = EstudioMapper.toEntity(dto);
        entity.setFechaRegistro(LocalDateTime.now());
        EstudioMedico creado = estudiosApplicationService.createEstudio(entity);
        EstudioMedicoResponseDTO responseDTO = EstudioMapper.toResponseDTO(creado);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(responseDTO, "Estudio creado correctamente", HttpStatus.CREATED, false));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar estudio médico", description = "Actualiza la información de un estudio médico existente identificado por su ID")
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
    public ResponseEntity<APIResponse> update(
        @Parameter(description = "Identificador único del estudio médico a actualizar", required = true)
        @PathVariable Long id,
        @Valid @RequestBody EstudioMedicoRequestDTO dto) {
        EstudioMedico entity = EstudioMapper.toEntity(dto);
        EstudioMedico actualizado = estudiosApplicationService.updateEstudio(id, entity);
        EstudioMedicoResponseDTO responseDTO = EstudioMapper.toResponseDTO(actualizado);
        return ResponseEntity.ok(new APIResponse(responseDTO, "Estudio actualizado correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/tipos/todos")
    @Operation(summary = "Listar todos los tipos de estudio", description = "Obtiene todos los tipos de estudio (activos e inactivos) para gestión en catálogos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAllTipos() {
        List<TipoEstudio> tipos = gestionEstudiosApplicationService.getAllTipos();
        List<TipoEstudioResponseDTO> dtos = tipos.stream()
            .map(t -> TipoEstudioResponseDTO.builder()
                .id(t.getId())
                .nombre(t.getNombre())
                .descripcion(t.getDescripcion())
                .activo(t.getActivo())
                .parametroEstudios(t.getParametros() != null
                    ? t.getParametros().stream().map(this::toParametroDTO).collect(Collectors.toList())
                    : List.of())
                .build())
            .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Tipos de estudio obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/tipos")
    @Operation(summary = "Listar tipos de estudio activos", description = "Obtiene una lista de todos los tipos de estudio que se encuentran activos")
    public ResponseEntity<APIResponse> getTiposActivos() {
        List<TipoEstudio> tipos = gestionEstudiosApplicationService.getAllByEstatus();
        List<TipoEstudioResponseDTO> dtos = tipos.stream()
            .map(t -> TipoEstudioResponseDTO.builder()
                .id(t.getId())
                .nombre(t.getNombre())
                .descripcion(t.getDescripcion())
                .activo(t.getActivo())
                .parametroEstudios(t.getParametros() != null
                    ? t.getParametros().stream().map(this::toParametroDTO).collect(Collectors.toList())
                    : List.of())
                .build())
            .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Tipos de estudio obtenidos correctamente", HttpStatus.OK, false));
    }

    @PostMapping("/tipos")
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
    public ResponseEntity<APIResponse> createTipo(@Valid @RequestBody TipoEstudioRequestDTO dto) {
        TipoEstudio tipo = new TipoEstudio();
        tipo.setNombre(dto.getNombre());
        tipo.setDescripcion(dto.getDescripcion());
        tipo.setActivo(true);
        tipo.setFechaCreacion(LocalDateTime.now());
        TipoEstudio creado = gestionEstudiosApplicationService.createTipoService(tipo);
        TipoEstudioResponseDTO responseDTO = TipoEstudioResponseDTO.builder()
            .id(creado.getId())
            .nombre(creado.getNombre())
            .descripcion(creado.getDescripcion())
            .activo(creado.getActivo())
            .build();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(responseDTO, "Tipo de estudio creado correctamente", HttpStatus.CREATED, false));
    }

    @PutMapping("/tipos/{id}")
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
    public ResponseEntity<APIResponse> updateTipo(
        @Parameter(description = "Identificador único del tipo de estudio a actualizar", required = true)
        @PathVariable Long id,
        @Valid @RequestBody TipoEstudioRequestDTO dto) {
        TipoEstudio existente = gestionEstudiosApplicationService.getOne(id);
        existente.setNombre(dto.getNombre());
        existente.setDescripcion(dto.getDescripcion());
        TipoEstudio actualizado = gestionEstudiosApplicationService.update(existente);
        TipoEstudioResponseDTO responseDTO = TipoEstudioResponseDTO.builder()
            .id(actualizado.getId())
            .nombre(actualizado.getNombre())
            .descripcion(actualizado.getDescripcion())
            .activo(actualizado.getActivo())
            .build();
        return ResponseEntity.ok(new APIResponse(responseDTO, "Tipo de estudio actualizado correctamente", HttpStatus.OK, false));
    }

    @PutMapping("/tipos/{id}/toggle")
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
    public ResponseEntity<APIResponse> toggleTipo(
        @Parameter(description = "Identificador único del tipo de estudio", required = true)
        @PathVariable Long id) {
        Boolean activo = gestionEstudiosApplicationService.Active(id);
        return ResponseEntity.ok(new APIResponse(activo, "Estado del tipo de estudio actualizado correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/tipos/{id}/parametros")
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
    public ResponseEntity<APIResponse> getParametrosByTipo(
        @Parameter(description = "Identificador único del tipo de estudio", required = true)
        @PathVariable Long id) {
        List<ParametroEstudio> parametros = gestionEstudiosApplicationService.getParametrosByTipo(id);
        List<ParametroEstudioResponseDTO> dtos = parametros.stream()
            .map(this::toParametroDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Parámetros obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/paciente/{uuid}")
    @Operation(summary = "Listar estudios médicos por paciente", description = "Obtiene todos los estudios médicos registrados para un paciente identificado por su UUID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "UUID inválido",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getByPaciente(
        @Parameter(description = "UUID del paciente", required = true)
        @PathVariable @NotBlank String uuid) {
        List<EstudioMedico> estudios = estudiosApplicationService.getEstudiosByPaciente(uuid);
        List<EstudioListRequestDTO> dtos = EstudioMapper.toResponseDTOList(estudios);
        return ResponseEntity.ok(new APIResponse(dtos, "Estudios del paciente obtenidos correctamente", HttpStatus.OK, false));
    }

    @PostMapping("/parametros")
    @Operation(summary = "Crear parámetro de estudio", description = "Registra un nuevo parámetro. Si el tipo es TEXTO_OPCIONES, incluir el campo 'opciones'.")
    public ResponseEntity<APIResponse> createParametro(@Valid @RequestBody ParametroEstudioRequestDTO dto) {
        ParametroEstudio parametro = new ParametroEstudio();
        TipoEstudio tipoEstudio = new TipoEstudio();
        tipoEstudio.setId(dto.getIdTipoEstudio());
        parametro.setTipoEstudio(tipoEstudio);
        parametro.setNombre(dto.getNombre());
        parametro.setUnidad(dto.getUnidad());
        parametro.setTipo(dto.getTipo());
        parametro.setValorMinimo(dto.getValorMinimo());
        parametro.setValorMaximo(dto.getValorMaximo());
        ParametroEstudio creado = gestionEstudiosApplicationService.createParametro(parametro, dto.getOpciones());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(toParametroDTO(creado), "Parámetro creado correctamente", HttpStatus.CREATED, false));
    }

    @PutMapping("/parametros/{id}")
    @Operation(summary = "Actualizar parámetro de estudio", description = "Actualiza el parámetro. Si el tipo es TEXTO_OPCIONES, el campo 'opciones' reemplaza las existentes.")
    public ResponseEntity<APIResponse> updateParametro(
        @Parameter(description = "Identificador único del parámetro de estudio a actualizar", required = true)
        @PathVariable Long id,
        @Valid @RequestBody ParametroEstudioRequestDTO dto) {
        ParametroEstudio parametro = new ParametroEstudio();
        parametro.setId(id);
        TipoEstudio tipoEstudio = new TipoEstudio();
        tipoEstudio.setId(dto.getIdTipoEstudio());
        parametro.setTipoEstudio(tipoEstudio);
        parametro.setNombre(dto.getNombre());
        parametro.setUnidad(dto.getUnidad());
        parametro.setTipo(dto.getTipo());
        parametro.setValorMinimo(dto.getValorMinimo());
        parametro.setValorMaximo(dto.getValorMaximo());
        ParametroEstudio actualizado = gestionEstudiosApplicationService.updateParametro(parametro, dto.getOpciones());
        return ResponseEntity.ok(new APIResponse(toParametroDTO(actualizado), "Parámetro actualizado correctamente", HttpStatus.OK, false));
    }

    @DeleteMapping("/parametros/{id}")
    @Operation(summary = "Eliminar parámetro de estudio")
    public ResponseEntity<APIResponse> deleteParametro(
        @Parameter(description = "Identificador único del parámetro de estudio a eliminar", required = true)
        @PathVariable Long id) {
        gestionEstudiosApplicationService.deleteParametro(id);
        return ResponseEntity.ok(new APIResponse("Parámetro eliminado correctamente", HttpStatus.OK, false));
    }

    // ─── Opciones para TEXTO_OPCIONES ────────────────────────────────────────

    @PostMapping("/parametros/{id}/opciones")
    @Operation(summary = "Agregar opción a parámetro TEXTO_OPCIONES")
    public ResponseEntity<APIResponse> addOpcion(
        @PathVariable Long id,
        @Valid @RequestBody OpcionParametroRequestDTO dto) {
        OpcionParametro opcion = gestionEstudiosApplicationService.addOpcion(id, dto.getValor());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(opcion.getValor(), "Opción agregada correctamente", HttpStatus.CREATED, false));
    }

    @DeleteMapping("/parametros/opciones/{opcionId}")
    @Operation(summary = "Eliminar opción de parámetro TEXTO_OPCIONES")
    public ResponseEntity<APIResponse> deleteOpcion(@PathVariable Long opcionId) {
        gestionEstudiosApplicationService.deleteOpcion(opcionId);
        return ResponseEntity.ok(new APIResponse("Opción eliminada correctamente", HttpStatus.OK, false));
    }

    // ─── Helper: entidad → DTO ────────────────────────────────────────────────

    private ParametroEstudioResponseDTO toParametroDTO(ParametroEstudio p) {
        List<String> opciones = (p.getTipo() == TipoParametro.TEXTO_OPCIONES && p.getOpciones() != null)
            ? p.getOpciones().stream().map(op -> op.getValor()).collect(Collectors.toList())
            : null;
        return ParametroEstudioResponseDTO.builder()
            .id(p.getId())
            .nombre(p.getNombre())
            .unidad(p.getUnidad())
            .tipo(p.getTipo())
            .tipoEstudio(p.getTipoEstudio() != null ? p.getTipoEstudio().getNombre() : null)
            .valorMinimo(p.getValorMinimo())
            .valorMaximo(p.getValorMaximo())
            .opciones(opciones)
            .build();
    }
}
