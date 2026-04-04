package imss.gob.mx.cohorte.controllers.users;

import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserRequestDTO;
import imss.gob.mx.cohorte.controllers.users.dto.UserResponseDTO;
import imss.gob.mx.cohorte.application.UserApplicationService;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Listar todos los usuarios")
    public ResponseEntity<APIResponse> getAll() {
        List<BeanUser> users = userApplicationService.getAll();
        return ResponseEntity.ok(new APIResponse("Usuarios encontrados", UserMapper.toResponseDTOList(users), false, HttpStatus.OK));
    }

    @GetMapping("/activos")
    @Operation(summary = "Listar usuarios activos")
    public ResponseEntity<APIResponse> getActivos() {
        List<BeanUser> users = userApplicationService.getActivos();
        return ResponseEntity.ok(new APIResponse("Usuarios activos encontrados", UserMapper.toResponseDTOList(users), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener usuario por ID")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        BeanUser user = userApplicationService.getById(id);
        return ResponseEntity.ok(new APIResponse("Usuario encontrado", UserMapper.toResponseDTO(user), false, HttpStatus.OK));
    }

    @GetMapping("/uuid/{uuid}")
    @Operation(summary = "Obtener usuario por UUID")
    public ResponseEntity<APIResponse> getByUUID(@PathVariable String uuid) {
        BeanUser user = userApplicationService.getByUUID(uuid);
        return ResponseEntity.ok(new APIResponse("Usuario encontrado", UserMapper.toResponseDTO(user), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear nuevo usuario")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody UserRequestDTO dto) {
        BeanUser user = UserMapper.toEntity(dto);
        BeanUser saved = userApplicationService.save(user, dto.getIdRol());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Usuario creado exitosamente", UserMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario")
    public ResponseEntity<APIResponse> update(@PathVariable Long id, @Validated @RequestBody UserRequestDTO dto) {
        BeanUser user = UserMapper.toEntity(dto);
        user.setId(id);
        BeanUser updated = userApplicationService.update(user, dto.getIdRol());
        return ResponseEntity.ok(new APIResponse("Usuario actualizado", UserMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }
}
