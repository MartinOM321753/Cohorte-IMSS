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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 15;
    private static final int RATE_LIMIT_MINUTES = 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional
    public void solicitarReset(String email) {
        BeanUser usuario = userRepository.findActiveUserByPersonaEmail(email.trim()).orElse(null);

        if (usuario == null) {
            log.info("Solicitud reset: email sin usuario activo asociado - {}", email);
            return;
        }

        Instant hace60min = Instant.now().minus(RATE_LIMIT_MINUTES, ChronoUnit.MINUTES);
        if (tokenRepository.existsByUsuarioAndCreadoEnAfter(usuario, hace60min)) {
            log.info("Rate-limit de reset alcanzado para usuario: {}", usuario.getUsername());
            throw new RateLimitException("Ya enviamos un correo de recuperacion en la ultima hora. Por favor revisa tu bandeja de entrada.");
        }

        enviarCorreoReset(usuario, "Recuperacion de contraseña - Sistema Cohorte", "email/reset-password");
    }

    @Transactional
    public void enviarInvitacion(BeanUser usuario) {
        enviarCorreoReset(usuario, "Bienvenido al Sistema Cohorte - Define tu contraseña", "email/bienvenida-usuario");
    }

    @Transactional(readOnly = true)
    public void validarToken(String token) {
        PasswordResetToken prt = buscarToken(token);
        validarTokenVigente(prt);
    }

    @Transactional
    public void resetearPassword(String token, String nuevaPassword) {
        PasswordResetToken prt = buscarToken(token);
        validarTokenVigente(prt);

        BeanUser usuario = prt.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuario.setDebeResetear(false);
        userRepository.save(usuario);

        prt.setUsado(true);
        tokenRepository.save(prt);

        log.info("Contraseña restablecida para usuario: {}", usuario.getUsername());
    }

    private void enviarCorreoReset(BeanUser usuario, String asunto, String template) {
        PasswordResetTokenData tokenData = crearTokenParaUsuario(usuario);
        String resetLink = frontendUrl + "/reset-password?token=" + tokenData.rawToken();
        String nombreCompleto = usuario.getPersona().getNombre() + " " + usuario.getPersona().getApellidoPaterno();

        Context ctx = new Context();
        ctx.setVariable("nombre", nombreCompleto);
        ctx.setVariable("username", usuario.getUsername());
        ctx.setVariable("resetLink", resetLink);
        ctx.setVariable("expiracionMinutos", TOKEN_EXPIRY_MINUTES);

        String destinatario = usuario.getPersona().getEmail();

        try {
            emailService.enviar(destinatario, asunto, template, ctx);
            log.info("Correo de acceso enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("Error enviando correo de acceso a {}: {}", destinatario, e.getMessage());
            tokenRepository.delete(tokenData.entity());
            throw new RuntimeException("No se pudo enviar el correo. Intenta de nuevo mas tarde.");
        }
    }

    private PasswordResetTokenData crearTokenParaUsuario(BeanUser usuario) {
        tokenRepository.marcarTokensActivosComoUsados(usuario);

        String rawToken = generarTokenSeguro();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(hashToken(rawToken));
        prt.setUsuario(usuario);
        prt.setExpiraEn(Instant.now().plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES));
        tokenRepository.save(prt);
        return new PasswordResetTokenData(rawToken, prt);
    }

    private PasswordResetToken buscarToken(String token) {
        return tokenRepository.findByToken(hashToken(token))
                .orElseThrow(() -> new TokenInvalidoException("El enlace no es valido o ya fue utilizado."));
    }

    private void validarTokenVigente(PasswordResetToken prt) {
        Instant ahora = Instant.now();

        if (prt.isUsado()) {
            log.info("Token de reset ya utilizado: id={}, usuario={}",
                    prt.getId(), prt.getUsuario().getUsername());
            throw new TokenInvalidoException("El enlace ya fue utilizado. Solicita uno nuevo.");
        }
        if (!prt.getExpiraEn().isAfter(ahora)) {
            log.info("Token de reset expirado: id={}, usuario={}, expiraEn={}, ahora={}",
                    prt.getId(), prt.getUsuario().getUsername(), prt.getExpiraEn(), ahora);
            throw new TokenInvalidoException("El enlace ha expirado. Solicita uno nuevo.");
        }
        if (!Boolean.TRUE.equals(prt.getUsuario().getActivo())) {
            throw new TokenInvalidoException("Tu cuenta esta desactivada. Contacta al administrador.");
        }
    }

    private String generarTokenSeguro() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible", e);
        }
    }

    private record PasswordResetTokenData(String rawToken, PasswordResetToken entity) {}

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) { super(message); }
    }

    public static class TokenInvalidoException extends RuntimeException {
        public TokenInvalidoException(String message) { super(message); }
    }
}
