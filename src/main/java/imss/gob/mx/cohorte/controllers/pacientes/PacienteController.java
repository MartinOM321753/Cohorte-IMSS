package imss.gob.mx.cohorte.controllers.pacientes;

import imss.gob.mx.cohorte.application.PacienteApplicationService;
import imss.gob.mx.cohorte.controllers.pacientes.dto.ImportResultDTO;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteMapper;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteRequestDTO;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteResponseDTO;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pacientes")
@AllArgsConstructor
@Tag(name = "Pacientes", description = "Gestión de pacientes de la cohorte")
@SecurityRequirement(name = "bearerAuth")
public class PacienteController {

    private final PacienteApplicationService pacienteApplicationService;

    @GetMapping
    @Operation(summary = "Listar todos los pacientes", description = "Obtiene una lista completa de todos los pacientes registrados en la cohorte")
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
    public ResponseEntity<APIResponse> getAll(
            @RequestParam(value = "incluirJerarquia", defaultValue = "false") boolean incluirJerarquia) {
        Long idInstActual = pacienteApplicationService.getIdInstitucionActual();
        List<Paciente> pacientes = incluirJerarquia
                ? pacienteApplicationService.getAllConJerarquia()
                : pacienteApplicationService.getAll();
        return ResponseEntity.ok(new APIResponse("Participantes encontrados",
                PacienteMapper.toResponseDTOList(pacientes, idInstActual), false, HttpStatus.OK));
    }

    @GetMapping("/paginado")
    @Operation(summary = "Listar pacientes paginados", description = "Obtiene los pacientes en páginas (parámetros estándar de Spring: page, size, sort) para evitar cargar toda la tabla en una sola respuesta")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAllPaginado(
            Pageable pageable,
            @RequestParam(value = "incluirJerarquia", defaultValue = "false") boolean incluirJerarquia) {
        Long idInstActual = pacienteApplicationService.getIdInstitucionActual();
        Page<Paciente> pacientes = incluirJerarquia
                ? pacienteApplicationService.getAllPaginadoConJerarquia(pageable)
                : pacienteApplicationService.getAllPaginado(pageable);
        Map<String, Object> body = Map.of(
            "content", PacienteMapper.toResponseDTOList(pacientes.getContent(), idInstActual),
            "page", pacientes.getNumber(),
            "size", pacientes.getSize(),
            "totalElements", pacientes.getTotalElements(),
            "totalPages", pacientes.getTotalPages()
        );
        return ResponseEntity.ok(new APIResponse("Participantes encontrados", body, false, HttpStatus.OK));
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar pacientes activos", description = "Obtiene una lista de todos los pacientes que se encuentran activos en la cohorte")
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
    public ResponseEntity<APIResponse> getActivos() {
        List<Paciente> pacientes = pacienteApplicationService.getActivos();
        return ResponseEntity.ok(new APIResponse("Participantes activos encontrados", PacienteMapper.toResponseDTOList(pacientes), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener paciente por ID", description = "Obtiene los datos de un paciente específico utilizando su identificador numérico")
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
            @Parameter(description = "ID numérico del paciente", required = true)
            @PathVariable Long id) {
        Paciente paciente = pacienteApplicationService.findUser(id);
        var reclutamiento = pacienteApplicationService.getReclutamiento(paciente.getId());
        Long idInstActual = pacienteApplicationService.getIdInstitucionActual();
        return ResponseEntity.ok(new APIResponse("Participante encontrado", PacienteMapper.toResponseDTO(paciente, reclutamiento, idInstActual), false, HttpStatus.OK));
    }

    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "Obtener paciente por UUID", description = "Obtiene los datos de un paciente específico utilizando su identificador UUID")
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
    public ResponseEntity<APIResponse> getByUUID(
            @Parameter(description = "UUID del paciente", required = true)
            @PathVariable String uuid) {
        Paciente paciente = pacienteApplicationService.findByUUID(uuid);
        var reclutamiento = pacienteApplicationService.getReclutamiento(paciente.getId());
        Long idInstActual = pacienteApplicationService.getIdInstitucionActual();
        return ResponseEntity.ok(new APIResponse("Participante encontrado", PacienteMapper.toResponseDTO(paciente, reclutamiento, idInstActual), false, HttpStatus.OK));
    }

    @GetMapping("/folio/{folio}")
    @Operation(summary = "Obtener paciente por folio", description = "Obtiene los datos de un paciente específico utilizando su número de folio")
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
    public ResponseEntity<APIResponse> getByFolio(
            @Parameter(description = "Número de folio del paciente", required = true)
            @PathVariable String folio) {
        Paciente paciente = pacienteApplicationService.findByFolio(folio);
        var reclutamiento = pacienteApplicationService.getReclutamiento(paciente.getId());
        Long idInstActual = pacienteApplicationService.getIdInstitucionActual();
        return ResponseEntity.ok(new APIResponse("Participante encontrado", PacienteMapper.toResponseDTO(paciente, reclutamiento, idInstActual), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Registrar nuevo paciente", description = "Registra un nuevo paciente en la cohorte con los datos proporcionados en el cuerpo de la solicitud")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Paciente registrado exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody PacienteRequestDTO dto,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        Paciente paciente = PacienteMapper.toEntity(dto);
        String uuidUsuarioAutenticado = userDetails != null ? userDetails.getUsername() : null;
        Paciente saved = pacienteApplicationService.saveUserConReclutamiento(paciente, dto.getReclutamiento(), uuidUsuarioAutenticado);
        var reclutamiento = pacienteApplicationService.getReclutamiento(saved.getId());
        Long idInstActual = pacienteApplicationService.getIdInstitucionActual();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Participante registrado exitosamente", PacienteMapper.toResponseDTO(saved, reclutamiento, idInstActual), false, HttpStatus.CREATED));
    }

    @PatchMapping("/uuid/{uuid}/toggle-activo")
    @Operation(summary = "Activar / desactivar paciente",
               description = "Alterna el campo activo del paciente (activo → inactivo o viceversa) sin eliminar el registro.")
    public ResponseEntity<APIResponse> toggleActivo(@PathVariable String uuid) {
        Paciente updated = pacienteApplicationService.toggleActivo(uuid);
        var reclutamiento = pacienteApplicationService.getReclutamiento(updated.getId());
        Long idInstActual = pacienteApplicationService.getIdInstitucionActual();
        return ResponseEntity.ok(new APIResponse(
                updated.getActivo() ? "Participante activado" : "Participante desactivado",
                PacienteMapper.toResponseDTO(updated, reclutamiento, idInstActual), false, HttpStatus.OK));
    }

    @PostMapping(value = "/importar", consumes = "multipart/form-data")
    @Operation(summary = "Importar participantes desde CSV o Excel",
               description = "Recibe un archivo CSV o XLSX con columnas: folio, nombre, apellidoPaterno, apellidoMaterno, curp, fechaNacimiento, sexo, telefono, email. Solo folio (o vacío para auto-generar), nombre y apellidoPaterno son obligatorios.")
    public ResponseEntity<APIResponse> importar(@RequestParam("archivo") MultipartFile archivo) {
        ImportResultDTO resultado = pacienteApplicationService.importarPacientes(archivo);
        String msg = "Importación completada: " + resultado.getExitosos() + " exitosos, " + resultado.getErrores() + " errores";
        return ResponseEntity.ok(new APIResponse(msg, resultado, false, HttpStatus.OK));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar paciente", description = "Actualiza los datos de un paciente existente identificado por su ID numérico")
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
            @Parameter(description = "ID numérico del paciente a actualizar", required = true)
            @PathVariable Long id,
            @Validated @RequestBody PacienteRequestDTO dto) {
        Paciente paciente = PacienteMapper.toEntity(dto);
        paciente.setId(id);
        Paciente updated = pacienteApplicationService.updateUser(paciente);
        var reclutamiento = pacienteApplicationService.getReclutamiento(updated.getId());
        Long idInstActual = pacienteApplicationService.getIdInstitucionActual();
        return ResponseEntity.ok(new APIResponse("Participante actualizado", PacienteMapper.toResponseDTO(updated, reclutamiento, idInstActual), false, HttpStatus.OK));
    }
}
