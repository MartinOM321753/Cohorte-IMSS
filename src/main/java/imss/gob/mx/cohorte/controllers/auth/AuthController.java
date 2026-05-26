package imss.gob.mx.cohorte.controllers.auth;


import imss.gob.mx.cohorte.application.AuthApplicationService;
import imss.gob.mx.cohorte.controllers.auth.dto.ForgotPasswordRequestDTO;
import imss.gob.mx.cohorte.controllers.auth.dto.LoginRequestDTO;
import imss.gob.mx.cohorte.controllers.auth.dto.ResetPasswordRequestDTO;
import imss.gob.mx.cohorte.services.auth.PasswordResetService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@Tag(name = "Controlador de Autenticación", description = "Operaciones relacionadas con autenticación y registro de usuarios")
public class AuthController {
    private final AuthApplicationService authService;
    private final PasswordResetService passwordResetService;


    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Permite a un usuario iniciar sesión en el sistema")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Inicio de sesión exitoso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario o contraseña incorrectos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            )
    })

    public ResponseEntity<APIResponse> login(@jakarta.validation.Valid @RequestBody LoginRequestDTO payload) {

        String token = authService.login(payload);

        APIResponse response = new APIResponse(
                "Inicio de sesión exitoso",
                token,
                false,
                HttpStatus.OK
        );

        return ResponseEntity.ok(response);
    }



    // ── Recuperación de contraseña ─────────────────────────────────────────────

    /**
     * Paso 1: el usuario ingresa su correo. Si existe, se envía un email con un
     * enlace temporal válido 15 minutos. Siempre responde con el mismo mensaje
     * para no revelar si el email está registrado.
     * Límite: 1 solicitud por hora por cuenta.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse> forgotPassword(
            @jakarta.validation.Valid @RequestBody ForgotPasswordRequestDTO body) {
        passwordResetService.solicitarReset(body.email());
        return ResponseEntity.ok(new APIResponse(
                "Si el correo está registrado recibirás un enlace en los próximos minutos.",
                null, false, HttpStatus.OK));
    }

    /**
     * Paso 1b (opcional): el frontend puede pre-validar que el token sigue activo
     * antes de mostrar el formulario de nueva contraseña.
     */
    @GetMapping("/reset-password/validate")
    public ResponseEntity<APIResponse> validateToken(@RequestParam String token) {
        passwordResetService.validarToken(token);
        return ResponseEntity.ok(new APIResponse("Token válido.", null, false, HttpStatus.OK));
    }

    /**
     * Paso 2: el usuario envía token + nueva contraseña.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<APIResponse> resetPassword(
            @jakarta.validation.Valid @RequestBody ResetPasswordRequestDTO body) {
        passwordResetService.resetearPassword(body.token(), body.nuevaPassword());
        return ResponseEntity.ok(new APIResponse(
                "Contraseña actualizada correctamente. Ya puedes iniciar sesión.",
                null, false, HttpStatus.OK));
    }
}