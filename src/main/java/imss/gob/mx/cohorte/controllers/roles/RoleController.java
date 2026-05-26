package imss.gob.mx.cohorte.controllers.roles;

import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Catálogo de roles del sistema")
@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private final RoleRepository roleRepository;

    public record RoleResponseDTO(String uuid, String nombre) {}

    @GetMapping
    @Operation(summary = "Listar todos los roles", description = "Retorna el catálogo de roles disponibles en el sistema")
    public ResponseEntity<APIResponse> getAll() {
        List<RoleResponseDTO> roles = roleRepository.findAll().stream()
                .map(r -> new RoleResponseDTO(r.getUuid(), r.getRole()))
                .toList();
        return ResponseEntity.ok(new APIResponse("Roles encontrados", roles, false, HttpStatus.OK));
    }
}
