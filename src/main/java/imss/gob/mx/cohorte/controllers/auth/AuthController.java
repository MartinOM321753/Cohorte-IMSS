package imss.gob.mx.cohorte.controllers.auth;

import imss.gob.mx.cohorte.application.AuthApplicationService;
import imss.gob.mx.cohorte.controllers.auth.dto.ForgotPasswordRequestDTO;
import imss.gob.mx.cohorte.controllers.auth.dto.LoginRequestDTO;
import imss.gob.mx.cohorte.controllers.auth.dto.ResetPasswordRequestDTO;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.security.jwt.JWTUtils;
import imss.gob.mx.cohorte.services.auth.PasswordResetService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@Tag(name = "Controlador de Autenticación", description = "Operaciones relacionadas con autenticación y registro de usuarios")
public class AuthController {

    private final AuthApplicationService authService;
    private final PasswordResetService   passwordResetService;
    private final JWTUtils               jwtUtils;
    private final UserRepository         userRepository;

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Permite a un usuario iniciar sesión en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = APIResponse.class))),
            @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> login(
            @jakarta.validation.Valid @RequestBody LoginRequestDTO payload,
            HttpServletRequest request) {

        String ip        = extractIp(request);
        String userAgent = request.getHeader("User-Agent");

        String token = authService.login(payload, ip, userAgent);

        return ResponseEntity.ok(new APIResponse(
                "Inicio de sesión exitoso", token, false, HttpStatus.OK));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión",
               description = "Registra el evento de cierre de sesión en la bitácora. "
                           + "El cliente debe eliminar el token localmente.")
    public ResponseEntity<APIResponse> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String ip         = extractIp(request);
        String userAgent  = request.getHeader("User-Agent");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            try {
                String uuid           = jwtUtils.extractUserUuid(token);
                Date   issuedAt       = jwtUtils.extractIssuedAt(token);
                long   durationSec    = (System.currentTimeMillis() - issuedAt.getTime()) / 1000;

                Optional<BeanUser> userOpt = userRepository.findByUUID(uuid);
                userOpt.ifPresent(user ->
                        authService.publicarLogout(user, ip, userAgent, (int) durationSec));
            } catch (Exception ignored) {
                // Si el token ya expiró o es inválido, registrar logout sin datos de usuario
            }
        }

        return ResponseEntity.ok(new APIResponse(
                "Sesión cerrada correctamente", null, false, HttpStatus.OK));
    }

    // ── Recuperación de contraseña ─────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse> forgotPassword(
            @jakarta.validation.Valid @RequestBody ForgotPasswordRequestDTO body) {
        passwordResetService.solicitarReset(body.email());
        return ResponseEntity.ok(new APIResponse(
                "Si el correo está registrado recibirás un enlace en los próximos minutos.",
                null, false, HttpStatus.OK));
    }

    @GetMapping("/reset-password/validate")
    public ResponseEntity<APIResponse> validateToken(@RequestParam String token) {
        passwordResetService.validarToken(token);
        return ResponseEntity.ok(new APIResponse("Token válido.", null, false, HttpStatus.OK));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<APIResponse> resetPassword(
            @jakarta.validation.Valid @RequestBody ResetPasswordRequestDTO body) {
        passwordResetService.resetearPassword(body.token(), body.nuevaPassword());
        return ResponseEntity.ok(new APIResponse(
                "Contraseña actualizada correctamente. Ya puedes iniciar sesión.",
                null, false, HttpStatus.OK));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String addr = request.getRemoteAddr();
        // Normalizar loopback IPv6 (::1) a IPv4 para consistencia en logs
        return "0:0:0:0:0:0:0:1".equals(addr) ? "127.0.0.1" : addr;
    }
}
