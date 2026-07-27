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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios del sistema")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private static final String ROOT_ROLE = "ROOT";

    private final UserApplicationService userApplicationService;

    private boolean isCallerRoot(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ROOT"::equals);
    }

    @SuppressWarnings("deprecation")
    private boolean isRootUser(BeanUser user) {
        return user.getRol() != null && ROOT_ROLE.equals(user.getRol().getRole());
    }

    @SuppressWarnings("deprecation")
    private List<BeanUser> hideRootUsers(List<BeanUser> users, Authentication auth) {
        if (isCallerRoot(auth)) return users;
        return users.stream()
                .filter(u -> !isRootUser(u))
                .toList();
    }

    private void guardRoot(BeanUser user, Authentication auth, String identifier) {
        if (!isCallerRoot(auth) && isRootUser(user)) {
            throw new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException(
                    "Usuario no encontrado: " + identifier);
        }
    }

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
    public ResponseEntity<APIResponse> getAll(Authentication auth) {
        List<BeanUser> users = hideRootUsers(userApplicationService.findAllByInstitucion(), auth);
        return ResponseEntity.ok(new APIResponse("Usuarios encontrados", UserMapper.toResponseDTOList(users), false, HttpStatus.OK));
    }

    @GetMapping("/paginado")
    @Operation(summary = "Listar usuarios paginados con búsqueda server-side",
               description = "Obtiene los usuarios en páginas con filtro de texto opcional. " +
                       "El parámetro 'buscar' filtra por nombre, apellidos, username, correo o rol. " +
                       "Incluye usuarios de la institución actual + invitaciones pendientes de cualquier institución.")
    public ResponseEntity<APIResponse> getAllPaginado(
            Authentication auth,
            Pageable pageable,
            @RequestParam(value = "buscar", required = false) String buscar) {
        Page<BeanUser> usuarios = userApplicationService.buscarPaginado(buscar, pageable);
        List<BeanUser> content = hideRootUsers(usuarios.getContent(), auth);
        long rootDelta = usuarios.getContent().size() - content.size();
        Map<String, Object> body = Map.of(
            "content", UserMapper.toResponseDTOList(content),
            "page", usuarios.getNumber(),
            "size", usuarios.getSize(),
            "totalElements", Math.max(0, usuarios.getTotalElements() - rootDelta),
            "totalPages", usuarios.getTotalPages()
        );
        return ResponseEntity.ok(new APIResponse("Usuarios encontrados", body, false, HttpStatus.OK));
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
    public ResponseEntity<APIResponse> getActivos(Authentication auth) {
        List<BeanUser> users = hideRootUsers(userApplicationService.findAllActiveByInstitucion(), auth);
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
            @PathVariable Long id,
            Authentication auth) {
        BeanUser user = userApplicationService.findUser(id);
        guardRoot(user, auth, String.valueOf(id));
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
            @PathVariable String uuid,
            Authentication auth) {
        BeanUser user = userApplicationService.findByUUID(uuid);
        guardRoot(user, auth, uuid);
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
            @PathVariable String roleName,
            Authentication auth) {
        List<BeanUser> users = hideRootUsers(userApplicationService.findByRoleName(roleName), auth);
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
            @PathVariable Long id,
            Authentication auth) {
        guardRoot(userApplicationService.findUser(id), auth, String.valueOf(id));
        BeanUser updated = userApplicationService.toggleActivo(id);
        String msg = Boolean.TRUE.equals(updated.getActivo()) ? "Usuario activado" : "Usuario desactivado";
        return ResponseEntity.ok(new APIResponse(msg, UserMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @PostMapping("/{uuid}/reenviar-invitacion")
    @Operation(summary = "Reenviar invitacion de usuario", description = "Genera un nuevo enlace de invitacion de un solo uso para usuarios que aun no definen su contrasena inicial")
    public ResponseEntity<APIResponse> reenviarInvitacion(
            @Parameter(description = "UUID del usuario", required = true)
            @PathVariable String uuid,
            Authentication auth) {
        guardRoot(userApplicationService.findByUUID(uuid), auth, uuid);
        BeanUser updated = userApplicationService.reenviarInvitacion(uuid);
        return ResponseEntity.ok(new APIResponse("Invitacion reenviada correctamente", UserMapper.toResponseDTO(updated), false, HttpStatus.OK));
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
            @Validated @RequestBody UserRequestDTO dto,
            Authentication auth) {
        guardRoot(userApplicationService.findUser(id), auth, String.valueOf(id));
        BeanUser user = UserMapper.toEntity(dto);
        user.setId(id);
        BeanUser updated = userApplicationService.updateUser(user);
        return ResponseEntity.ok(new APIResponse("Usuario actualizado", UserMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }
}
