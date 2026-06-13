package imss.gob.mx.cohorte.controllers.users;

import imss.gob.mx.cohorte.application.UserApplicationService;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserRequestDTO;
import imss.gob.mx.cohorte.controllers.users.dto.UserResponseDTO;
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
@RequestMapping("/api/users")
@AllArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserApplicationService userApplicationService;

    @GetMapping
    @Operation(summary = "Listar todos los usuarios", description = "Obtiene una lista completa de todos los usuarios registrados en el sistema")
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
        List<BeanUser> users = userApplicationService.findAllByInstitucion();
        return ResponseEntity.ok(new APIResponse("Usuarios encontrados", UserMapper.toResponseDTOList(users), false, HttpStatus.OK));
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar usuarios activos", description = "Obtiene una lista de todos los usuarios que se encuentran activos en el sistema")
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
        List<BeanUser> users = userApplicationService.findAllActiveByInstitucion();
        return ResponseEntity.ok(new APIResponse("Usuarios activos encontrados", UserMapper.toResponseDTOList(users), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID", description = "Obtiene los datos de un usuario específico utilizando su identificador numérico")
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
            @Parameter(description = "ID numérico del usuario", required = true)
            @PathVariable Long id) {
        BeanUser user = userApplicationService.findUser(id);
        return ResponseEntity.ok(new APIResponse("Usuario encontrado", UserMapper.toResponseDTO(user), false, HttpStatus.OK));
    }

    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "Obtener usuario por UUID", description = "Obtiene los datos de un usuario específico utilizando su identificador UUID")
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
            @Parameter(description = "UUID del usuario", required = true)
            @PathVariable String uuid) {
        BeanUser user = userApplicationService.findByUUID(uuid);
        return ResponseEntity.ok(new APIResponse("Usuario encontrado", UserMapper.toResponseDTO(user), false, HttpStatus.OK));
    }

    @GetMapping("/administradores-disponibles")
    @Operation(summary = "Administradores disponibles para ser asignados como encargado",
               description = "Devuelve los ADMINISTRADORES activos que no están asignados como encargado de ninguna institución. " +
                             "Si se proporciona 'institucionUuid', también incluye el admin ya asignado a esa institución " +
                             "(necesario para que el selector en modo edición no pierda al encargado actual).")
    public ResponseEntity<APIResponse> getAdministradoresDisponibles(
            @Parameter(description = "UUID de la institución que se está editando (opcional)")
            @RequestParam(required = false) String institucionUuid) {
        List<BeanUser> users = (institucionUuid != null && !institucionUuid.isBlank())
                ? userApplicationService.getAdministradoresDisponiblesParaInstitucion(institucionUuid)
                : userApplicationService.getAdministradoresDisponibles();
        return ResponseEntity.ok(new APIResponse("Administradores disponibles", UserMapper.toResponseDTOList(users), false, HttpStatus.OK));
    }

    @GetMapping("/rol/{roleName}")
    @Operation(summary = "Listar usuarios activos por rol", description = "Obtiene todos los usuarios activos que tienen el rol indicado (ej. ENCARGADO)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getByRol(
            @Parameter(description = "Nombre del rol (ej. ENCARGADO)", required = true)
            @PathVariable String roleName) {
        List<BeanUser> users = userApplicationService.findByRoleName(roleName);
        return ResponseEntity.ok(new APIResponse("Usuarios encontrados", UserMapper.toResponseDTOList(users), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo usuario", description = "Registra un nuevo usuario en el sistema con los datos proporcionados en el cuerpo de la solicitud")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody UserRequestDTO dto) {
        BeanUser user = UserMapper.toEntity(dto);
        BeanUser saved = userApplicationService.saveUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Usuario creado exitosamente", UserMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PatchMapping("/{id}/activo")
    @Operation(summary = "Activar o desactivar usuario", description = "Invierte el estado activo/inactivo del usuario especificado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado actualizado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> toggleActivo(
            @Parameter(description = "ID numérico del usuario", required = true)
            @PathVariable Long id) {
        BeanUser updated = userApplicationService.toggleActivo(id);
        String msg = Boolean.TRUE.equals(updated.getActivo()) ? "Usuario activado" : "Usuario desactivado";
        return ResponseEntity.ok(new APIResponse(msg, UserMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza los datos de un usuario existente identificado por su ID numérico")
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
            @Parameter(description = "ID numérico del usuario a actualizar", required = true)
            @PathVariable Long id,
            @Validated @RequestBody UserRequestDTO dto) {
        BeanUser user = UserMapper.toEntity(dto);
        user.setId(id);
        BeanUser updated = userApplicationService.updateUser(user);
        return ResponseEntity.ok(new APIResponse("Usuario actualizado", UserMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }
}
