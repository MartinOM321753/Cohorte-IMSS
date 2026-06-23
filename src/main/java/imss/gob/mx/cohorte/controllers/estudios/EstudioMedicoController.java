package imss.gob.mx.cohorte.controllers.estudios;

import imss.gob.mx.cohorte.application.EstudiosApplicationService;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.controllers.estudios.dto.*;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estudios")
@AllArgsConstructor
@Validated
@Tag(name = "Estudios Médicos", description = "Gestión de estudios médicos y catálogos")
@SecurityRequirement(name = "bearerAuth")
public class EstudioMedicoController {

    private final EstudiosApplicationService estudiosApplicationService;
    private final InstitucionContextService institucionContextService;

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

    @GetMapping("/paginado")
    @Operation(summary = "Listar estudios médicos paginados", description = "Obtiene los estudios médicos en páginas (parámetros estándar de Spring: page, size, sort) para evitar cargar todo el listado en una sola respuesta")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAllPaginado(Pageable pageable) {
        Page<EstudioMedico> estudios = estudiosApplicationService.getAllEstudiosPaginado(pageable);
        Map<String, Object> body = Map.of(
            "content", EstudioMapper.toResponseDTOList(estudios.getContent()),
            "page", estudios.getNumber(),
            "size", estudios.getSize(),
            "totalElements", estudios.getTotalElements(),
            "totalPages", estudios.getTotalPages()
        );
        return ResponseEntity.ok(new APIResponse(body, "Estudios obtenidos correctamente", HttpStatus.OK, false));
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
        entity.setInstitucion(institucionContextService.getInstitucionActual());
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
        return ResponseEntity.ok(new APIResponse(dtos, "Estudios del participante obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/paciente/{uuid}/paginado")
    @Operation(summary = "Listar estudios médicos de un paciente paginados", description = "Obtiene los estudios médicos de un paciente en páginas (parámetros estándar de Spring: page, size, sort)")
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
    public ResponseEntity<APIResponse> getByPacientePaginado(
        @Parameter(description = "UUID del paciente", required = true)
        @PathVariable @NotBlank String uuid,
        Pageable pageable) {
        Page<EstudioMedico> estudios = estudiosApplicationService.getEstudiosByPacientePaginado(uuid, pageable);
        Map<String, Object> body = Map.of(
            "content", EstudioMapper.toResponseDTOList(estudios.getContent()),
            "page", estudios.getNumber(),
            "size", estudios.getSize(),
            "totalElements", estudios.getTotalElements(),
            "totalPages", estudios.getTotalPages()
        );
        return ResponseEntity.ok(new APIResponse(body, "Estudios del participante obtenidos correctamente", HttpStatus.OK, false));
    }
}
