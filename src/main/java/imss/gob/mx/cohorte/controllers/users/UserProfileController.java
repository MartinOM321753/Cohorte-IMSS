package imss.gob.mx.cohorte.controllers.users;

import imss.gob.mx.cohorte.controllers.users.dto.ChangePasswordRequestDTO;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "Perfil de usuario", description = "Operaciones del usuario sobre su propia cuenta")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserService userService;

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * Si el usuario tiene debeResetear=true (primer login con contraseña generada),
     * el campo passwordActual es ignorado. En caso contrario, es obligatorio y debe
     * coincidir con la contraseña actual.
     */
    @PutMapping("/password")
    @Operation(
        summary = "Cambiar contraseña",
        description = "Permite al usuario cambiar su propia contraseña. En el primer acceso " +
                      "(contraseña generada por el sistema) no se requiere la contraseña actual."
    )
    public ResponseEntity<APIResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDTO dto) {

        String uuid = userDetails.getUsername(); // El subject del JWT es el UUID
        userService.cambiarPassword(uuid, dto.passwordActual(), dto.nuevaPassword());

        return ResponseEntity.ok(new APIResponse(
                "Contraseña actualizada correctamente",
                null, false, HttpStatus.OK));
    }
}
