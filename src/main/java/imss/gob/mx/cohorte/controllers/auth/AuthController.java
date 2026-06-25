package imss.gob.mx.cohorte.controllers.auth;

import imss.gob.mx.cohorte.application.AuthApplicationService;
import imss.gob.mx.cohorte.controllers.auth.dto.ForgotPasswordRequestDTO;
import imss.gob.mx.cohorte.controllers.auth.dto.LoginRequestDTO;
import imss.gob.mx.cohorte.controllers.auth.dto.ResetPasswordRequestDTO;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserResponseDTO;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Controlador de Autenticación", description = "Operaciones relacionadas con autenticación y registro de usuarios")
public class AuthController {

    /** Nombre de la cookie httpOnly que transporta el JWT. */
    public static final String AUTH_COOKIE_NAME = "auth_token";

    /** Duración de la cookie en segundos — debe coincidir con la expiración del JWT (10 horas). */
    private static final long AUTH_COOKIE_MAX_AGE_SECONDS = 60L * 60 * 10;

    private final AuthApplicationService authService;
    private final PasswordResetService   passwordResetService;
    private final JWTUtils               jwtUtils;
    private final UserRepository         userRepository;

    @Value("${app.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${app.cookie-samesite:Lax}")
    private String cookieSameSite;

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

        ResponseCookie cookie = buildAuthCookie(token, AUTH_COOKIE_MAX_AGE_SECONDS);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new APIResponse("Inicio de sesión exitoso", null, false, HttpStatus.OK));
    }

    // ── Sesión actual ─────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Obtener sesión actual",
               description = "Devuelve los datos del usuario autenticado a partir de la cookie de sesión vigente.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<APIResponse> me(@AuthenticationPrincipal UserDetails userDetails,
                                          HttpServletRequest request) {
        String uuid = userDetails.getUsername(); // El subject del JWT/UserDetails es el UUID
        BeanUser user = userRepository.findByUUID(uuid)
                .orElseThrow(() -> new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException(
                        "No se encontró el usuario de la sesión actual"));

        UserResponseDTO userDTO = UserMapper.toResponseDTO(user);

        boolean mustChangePassword = readMustChangePasswordFromCookie(request);

        Map<String, Object> body = new HashMap<>();
        body.put("user", userDTO);
        body.put("mustChangePassword", mustChangePassword);

        return ResponseEntity.ok(new APIResponse("Sesión vigente", body, false, HttpStatus.OK));
    }

    // ── Limpieza de cookie pre-login ─────────────────────────────────────────

    @PostMapping("/clear-session")
    @Operation(summary = "Limpiar cookie de sesión",
               description = "Expira la cookie auth_token sin registrar nada en bitácora. "
                           + "Se invoca antes del login para eliminar cookies stale de deploys previos.")
    public ResponseEntity<APIResponse> clearSession() {
        ResponseCookie expiredCookie = buildAuthCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(new APIResponse("Cookie eliminada", null, false, HttpStatus.OK));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión",
               description = "Registra el evento de cierre de sesión en la bitácora y elimina la cookie de sesión.")
    public ResponseEntity<APIResponse> logout(HttpServletRequest request) {
        String token      = extractTokenFromCookie(request);
        String ip         = extractIp(request);
        String userAgent  = request.getHeader("User-Agent");

        if (token != null) {
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

        // Eliminar la cookie: mismo nombre/path/atributos, Max-Age=0
        ResponseCookie expiredCookie = buildAuthCookie("", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .body(new APIResponse("Sesión cerrada correctamente", null, false, HttpStatus.OK));
    }

    // ── Helpers de cookie ─────────────────────────────────────────────────────

    private ResponseCookie buildAuthCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(AUTH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (AUTH_COOKIE_NAME.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }

    private boolean readMustChangePasswordFromCookie(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        if (token == null) return false;
        try {
            return Boolean.TRUE.equals(jwtUtils.extractAllClaims(token).get("mustChangePassword", Boolean.class));
        } catch (Exception e) {
            return false;
        }
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
