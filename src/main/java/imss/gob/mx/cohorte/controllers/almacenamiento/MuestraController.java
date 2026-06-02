package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.MuestraApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.MuestraMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.MuestraRequestDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
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
@RequestMapping("/api/almacenamiento/muestras")
@AllArgsConstructor
@Tag(name = "Muestras", description = "Gestión de muestras biológicas")
@SecurityRequirement(name = "bearerAuth")
public class  MuestraController {

    private final MuestraApplicationService muestraApplicationService;

    @GetMapping
    @Operation(summary = "Listar todas las muestras", description = "Obtiene una lista completa de todas las muestras biológicas registradas en el sistema")
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
        List<Muestra> list = muestraApplicationService.getAllMuestras();
        return ResponseEntity.ok(new APIResponse("Muestras encontradas", MuestraMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener muestra por ID", description = "Obtiene los detalles de una muestra biológica específica mediante su identificador único")
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
        @Parameter(description = "ID numérico de la muestra biológica", required = true)
        @PathVariable Long id) {
        Muestra muestra = muestraApplicationService.getMuestra(id);
        return ResponseEntity.ok(new APIResponse("Muestra encontrada", MuestraMapper.toResponseDTO(muestra), false, HttpStatus.OK));
    }

    @GetMapping("/paciente/uuid/{uuid}/count")
    @Operation(summary = "Contar muestras de un paciente por UUID")
    public ResponseEntity<APIResponse> countByPacienteUUID(@PathVariable String uuid) {
        long count = muestraApplicationService.countMuestrasByPacienteUuid(uuid);
        return ResponseEntity.ok(new APIResponse("Conteo de muestras", count, false, HttpStatus.OK));
    }

    @GetMapping("/paciente/uuid/{uuid}")
    @Operation(summary = "Obtener muestras de un paciente por UUID", description = "Obtiene todas las muestras biológicas asociadas a un paciente específico mediante su UUID")
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
        @Parameter(description = "UUID único del paciente", required = true)
        @PathVariable String uuid) {
        List<Muestra> list = muestraApplicationService.getMuestrasByPacienteUUID(uuid);
        return ResponseEntity.ok(new APIResponse("Muestras del paciente encontradas", MuestraMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Registrar nueva muestra", description = "Registra una nueva muestra biológica en el sistema con su ubicación, paciente y usuario recolector")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Recurso creado exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody MuestraRequestDTO dto) {
        Muestra entity = MuestraMapper.toEntity(dto);
        if (dto.getIdPosicionCaja() != null) {
            PosicionCaja pos = new PosicionCaja();
            pos.setId(dto.getIdPosicionCaja());
            entity.setPosicionCaja(pos);
        }
        Paciente paciente = new Paciente();
        paciente.setUuid(dto.getPacienteUUID());
        entity.setPaciente(paciente);

        BeanUser usuario = new BeanUser();
        usuario.setUUID(dto.getUsuarioRecolectaUUID());
        entity.setUsuarioRecolecta(usuario);

        Muestra saved = muestraApplicationService.createMuestra(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Muestra registrada exitosamente", MuestraMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar muestra / reubicar", description = "Actualiza la información de una muestra biológica existente o la reubica en una nueva posición de almacenamiento")
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
        @Parameter(description = "ID numérico de la muestra biológica", required = true)
        @PathVariable Long id, @Validated @RequestBody MuestraRequestDTO dto) {
        Muestra entity = MuestraMapper.toEntity(dto);
        if (dto.getIdPosicionCaja() != null) {
            PosicionCaja pos = new PosicionCaja();
            pos.setId(dto.getIdPosicionCaja());
            entity.setPosicionCaja(pos);
        }
        if (dto.getPacienteUUID() != null) {
            Paciente paciente = new Paciente();
            paciente.setUuid(dto.getPacienteUUID());
            entity.setPaciente(paciente);
        }
        if (dto.getUsuarioRecolectaUUID() != null) {
            BeanUser usuario = new BeanUser();
            usuario.setUUID(dto.getUsuarioRecolectaUUID());
            entity.setUsuarioRecolecta(usuario);
        }
        Muestra updated = muestraApplicationService.updateMuestra(id, entity);
        return ResponseEntity.ok(new APIResponse("Muestra actualizada", MuestraMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }
}
