package imss.gob.mx.cohorte.controllers.citas;

import imss.gob.mx.cohorte.application.CitaApplicationService;
import imss.gob.mx.cohorte.controllers.citas.dto.CitaMapper;
import imss.gob.mx.cohorte.controllers.citas.dto.CitaRequestDTO;
import imss.gob.mx.cohorte.controllers.citas.dto.CitaUpdateRequestDTO;
import imss.gob.mx.cohorte.modules.cita.Cita;
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
@RequestMapping("/api/citas")
@AllArgsConstructor
@Tag(name = "Citas", description = "Gestión de citas médicas")
@SecurityRequirement(name = "bearerAuth")
public class CitaController {

    private final CitaApplicationService citaApplicationService;

    @GetMapping
    @Operation(summary = "Listar todas las citas", description = "Obtiene una lista completa de todas las citas médicas registradas en el sistema")
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
        List<Cita> citas = citaApplicationService.getAll();
        return ResponseEntity.ok(new APIResponse("Citas encontradas", CitaMapper.toResponseDTOList(citas), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cita por ID", description = "Obtiene los datos de una cita médica específica utilizando su identificador numérico")
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
            @Parameter(description = "ID numérico de la cita", required = true)
            @PathVariable Long id) {
        Cita cita = citaApplicationService.findById(id);
        return ResponseEntity.ok(new APIResponse("Cita encontrada", CitaMapper.toResponseDTO(cita), false, HttpStatus.OK));
    }

    @GetMapping("/paciente/uuid/{uuid}")
    @Operation(summary = "Obtener citas de un paciente por UUID", description = "Obtiene las citas médicas asociadas a un paciente identificado por su UUID")
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
    public ResponseEntity<APIResponse> getByPacienteUUID(
            @Parameter(description = "UUID del paciente", required = true)
            @PathVariable String uuid) {
        Cita cita = citaApplicationService.findByUuid(uuid);
        return ResponseEntity.ok(new APIResponse("Cita del paciente encontrada", CitaMapper.toResponseDTO(cita), false, HttpStatus.OK));
    }

    @GetMapping("/paciente/folio/{folio}")
    @Operation(summary = "Obtener citas de un paciente por folio", description = "Obtiene las citas médicas asociadas a un paciente identificado por su número de folio")
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
    public ResponseEntity<APIResponse> getByPacienteFolio(
            @Parameter(description = "Número de folio del paciente", required = true)
            @PathVariable String folio) {
        Cita cita = citaApplicationService.findByFolio(folio);
        return ResponseEntity.ok(new APIResponse("Cita del paciente encontrada", CitaMapper.toResponseDTO(cita), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Registrar nueva cita", description = "Registra una nueva cita médica en el sistema con los datos proporcionados en el cuerpo de la solicitud")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Cita registrada exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody CitaRequestDTO dto) {
        Cita cita = CitaMapper.toEntity(dto);
        Cita saved = citaApplicationService.save(cita);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Cita registrada exitosamente", CitaMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cita", description = "Actualiza los datos de una cita médica existente identificada por su ID numérico")
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
            @Parameter(description = "ID numérico de la cita a actualizar", required = true)
            @PathVariable Long id,
            @Validated @RequestBody CitaUpdateRequestDTO dto) {
        Cita cita = new Cita();
        cita.setId(id);
        cita.setFechaCita(dto.getFechaCita());
        cita.setDuracionMinutos(dto.getDuracionMinutos());
        cita.setEstadoCita(Cita.EstadoCita.valueOf(dto.getEstadoCita()));
        cita.setObservaciones(dto.getObservaciones());
        Cita updated = citaApplicationService.update(cita);
        return ResponseEntity.ok(new APIResponse("Cita actualizada", CitaMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar cita", description = "Cancela una cita médica existente identificada por su ID numérico")
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
    public ResponseEntity<APIResponse> cancelar(
            @Parameter(description = "ID numérico de la cita a cancelar", required = true)
            @PathVariable Long id) {
        citaApplicationService.cancelar(id);
        return ResponseEntity.ok(new APIResponse("Cita cancelada", null, false, HttpStatus.OK));
    }
}
