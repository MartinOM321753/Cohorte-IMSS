package imss.gob.mx.cohorte.services.auth;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.auth.PasswordResetToken;
import imss.gob.mx.cohorte.modules.auth.PasswordResetTokenRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /** URL base del frontend, ej: http://localhost:5173 */
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /** Minutos de validez del token (default 15). */
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    /** Ventana de rate-limit: solo 1 solicitud cada 60 minutos por usuario. */
    private static final int RATE_LIMIT_MINUTES = 60;

    // ── Solicitar reseteo ──────────────────────────────────────────────────────

    /**
     * Valida que el correo esté vinculado a un usuario, aplica rate-limit,
     * genera el token y envía el correo.
     *
     * <p>Siempre responde con el mismo mensaje genérico para no revelar si
     * el email existe o no en el sistema.
     */
    @Transactional
    public void solicitarReset(String email) {
        // Busca directamente el usuario activo cuya persona tiene ese email (case-insensitive).
        // Si no existe o no es usuario activo, responde igual (no revelar si el email existe).
        BeanUser usuario = userRepository.findActiveUserByPersonaEmail(email.trim()).orElse(null);

        if (usuario == null) {
            log.info("Solicitud reset: email sin usuario activo asociado — {}", email);
            return;
        }

        // Rate limit: máximo 1 solicitud por hora
        Instant hace60min = Instant.now().minus(RATE_LIMIT_MINUTES, ChronoUnit.MINUTES);
        if (tokenRepository.existsByUsuarioAndCreadoEnAfter(usuario, hace60min)) {
            log.info("Rate-limit de reset alcanzado para usuario: {}", usuario.getUsername());
            // Lanzar excepción con mensaje de negocio — el controller la propagará
            throw new RateLimitException("Ya enviamos un correo de recuperación en la última hora. Por favor revisa tu bandeja de entrada.");
        }

        // Generar token único (UUID sin guiones + aleatorio para evitar colisiones)
        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(rawToken);
        prt.setUsuario(usuario);
        prt.setExpiraEn(Instant.now().plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        tokenRepository.save(prt);

        // Enviar correo
        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        String nombreCompleto = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellidoPaterno();

        Context ctx = new Context();
        ctx.setVariable("nombre", nombreCompleto);
        ctx.setVariable("resetLink", resetLink);
        ctx.setVariable("expiracionMinutos", TOKEN_EXPIRY_MINUTES);

        // Usar el email real almacenado en la persona (ya validado por la query)
        String destinatario = usuario.getPersona().getEmail();

        try {
            emailService.enviar(
                    destinatario,
                    "Recuperación de contraseña — Sistema Cohorte",
                    "email/reset-password",
                    ctx
            );
            log.info("Email de recuperación enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("Error enviando email de recuperación a {}: {}", destinatario, e.getMessage());
            // Borrar el token si el correo falló para no contaminar el rate-limit
            tokenRepository.delete(prt);
            throw new RuntimeException("No se pudo enviar el correo. Intenta de nuevo más tarde.");
        }
    }

    // ── Validar token ──────────────────────────────────────────────────────────

    /**
     * Verifica que el token sea válido (existe, no expiró, no fue usado).
     * Solo para la pantalla de "restablecer contraseña".
     */
    @Transactional(readOnly = true)
    public void validarToken(String token) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenInvalidoException("El enlace no es válido o ya fue utilizado."));

        if (prt.isUsado() || prt.isExpirado()) {
            throw new TokenInvalidoException("El enlace ha expirado o ya fue utilizado. Solicita uno nuevo.");
        }

        if (!Boolean.TRUE.equals(prt.getUsuario().getActivo())) {
            throw new TokenInvalidoException("Tu cuenta está desactivada. Contacta al administrador.");
        }
    }

    // ── Aplicar nueva contraseña ───────────────────────────────────────────────

    @Transactional
    public void resetearPassword(String token, String nuevaPassword) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenInvalidoException("El enlace no es válido o ya fue utilizado."));

        if (prt.isUsado()) {
            throw new TokenInvalidoException("Este enlace ya fue utilizado. Solicita uno nuevo.");
        }
        if (prt.isExpirado()) {
            throw new TokenInvalidoException("El enlace ha expirado. Solicita uno nuevo.");
        }

        BeanUser usuario = prt.getUsuario();

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new TokenInvalidoException("Tu cuenta está desactivada. Contacta al administrador.");
        }

        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        userRepository.save(usuario);

        prt.setUsado(true);
        tokenRepository.save(prt);

        log.info("Contraseña restablecida para usuario: {}", usuario.getUsername());
    }

    // ── Excepciones internas ───────────────────────────────────────────────────

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) { super(message); }
    }

    public static class TokenInvalidoException extends RuntimeException {
        public TokenInvalidoException(String message) { super(message); }
    }
}
