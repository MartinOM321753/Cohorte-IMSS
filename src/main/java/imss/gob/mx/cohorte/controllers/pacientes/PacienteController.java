package imss.gob.mx.cohorte.controllers.pacientes;

import imss.gob.mx.cohorte.application.PacienteApplicationService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<APIResponse> getAll() {
        List<Paciente> pacientes = pacienteApplicationService.getAll();
        return ResponseEntity.ok(new APIResponse("Pacientes encontrados", PacienteMapper.toResponseDTOList(pacientes), false, HttpStatus.OK));
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
        return ResponseEntity.ok(new APIResponse("Pacientes activos encontrados", PacienteMapper.toResponseDTOList(pacientes), false, HttpStatus.OK));
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
        return ResponseEntity.ok(new APIResponse("Paciente encontrado", PacienteMapper.toResponseDTO(paciente), false, HttpStatus.OK));
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
        return ResponseEntity.ok(new APIResponse("Paciente encontrado", PacienteMapper.toResponseDTO(paciente), false, HttpStatus.OK));
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
        return ResponseEntity.ok(new APIResponse("Paciente encontrado", PacienteMapper.toResponseDTO(paciente), false, HttpStatus.OK));
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
    public ResponseEntity<APIResponse> create(@Validated @RequestBody PacienteRequestDTO dto) {
        Paciente paciente = PacienteMapper.toEntity(dto);
        Paciente saved = pacienteApplicationService.saveUser(paciente);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Paciente registrado exitosamente", PacienteMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PatchMapping("/uuid/{uuid}/toggle-activo")
    @Operation(summary = "Activar / desactivar paciente",
               description = "Alterna el campo activo del paciente (activo → inactivo o viceversa) sin eliminar el registro.")
    public ResponseEntity<APIResponse> toggleActivo(@PathVariable String uuid) {
        Paciente updated = pacienteApplicationService.toggleActivo(uuid);
        return ResponseEntity.ok(new APIResponse(
                updated.getActivo() ? "Paciente activado" : "Paciente desactivado",
                PacienteMapper.toResponseDTO(updated), false, HttpStatus.OK));
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
        return ResponseEntity.ok(new APIResponse("Paciente actualizado", PacienteMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }
}
