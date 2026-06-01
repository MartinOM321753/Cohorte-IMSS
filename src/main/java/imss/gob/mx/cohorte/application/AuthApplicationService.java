package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.audit.events.AccesoAuditEvent;
import imss.gob.mx.cohorte.audit.model.TipoEventoAcceso;
import imss.gob.mx.cohorte.controllers.auth.dto.LoginRequestDTO;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.services.auth.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

/**
 * Orquesta el flujo de autenticación y publica eventos de acceso
 * para la bitácora (LOGIN, LOGIN_FALLIDO).
 */
@Service
@AllArgsConstructor
public class AuthApplicationService {

    private final AuthService              authService;
    private final UserRepository           userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Autentica al usuario y publica el evento correspondiente.
     *
     * @param payload  credenciales + coordenadas GPS opcionales
     * @param ip       IP del cliente extraída del HttpServletRequest
     * @param userAgent User-Agent del cliente
     * @return JWT token si la autenticación fue exitosa
     */
    public String login(LoginRequestDTO payload, String ip, String userAgent) {
        try {
            String token = authService.login(payload);

            // Publicar LOGIN exitoso
            String identifier = payload.getIdentifier().trim();
            BeanUser user = userRepository.findByUsername(identifier)
                    .or(() -> userRepository.findActiveUserByPersonaEmail(identifier))
                    .orElse(null);

            publishAcceso(user, ip, payload.getLatitud(), payload.getLongitud(),
                    payload.getPrecisionM(), userAgent, TipoEventoAcceso.LOGIN, null, null);

            return token;

        } catch (BadCredentialsException ex) {
            // Publicar LOGIN_FALLIDO
            publishAcceso(null, ip, payload.getLatitud(), payload.getLongitud(),
                    payload.getPrecisionM(), userAgent, TipoEventoAcceso.LOGIN_FALLIDO, null, payload.getIdentifier());
            throw ex;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void publishAcceso(BeanUser user,
                                String ip,
                                Double latitud,
                                Double longitud,
                                Integer precisionM,
                                String userAgent,
                                TipoEventoAcceso tipo,
                                Integer duracionSeg,
                                String identificadorIntento) {
        String uuid     = user != null ? user.getUUID()     : null;
        String username = user != null ? user.getUsername() : null;
        String nombre   = user != null ? buildNombre(user)  : null;
        String rol      = (user != null && user.getRol() != null) ? user.getRol().getRole() : null;

        eventPublisher.publishEvent(new AccesoAuditEvent(
                this, uuid, username, nombre, rol,
                ip, latitud, longitud, precisionM, tipo,
                userAgent, duracionSeg, identificadorIntento
        ));
    }

    private String buildNombre(BeanUser u) {
        if (u.getPersona() == null) return u.getUsername();
        StringBuilder sb = new StringBuilder(u.getPersona().getNombre())
                .append(" ").append(u.getPersona().getApellidoPaterno());
        if (u.getPersona().getApellidoMaterno() != null &&
                !u.getPersona().getApellidoMaterno().isBlank()) {
            sb.append(" ").append(u.getPersona().getApellidoMaterno());
        }
        return sb.toString().trim();
    }

    /**
     * Expone el método de publicación de acceso para usarlo desde el AuthController
     * (endpoint /api/auth/logout).
     */
    public void publicarLogout(BeanUser user, String ip, String userAgent, Integer duracionSeg) {
        publishAcceso(user, ip, null, null, null, userAgent, TipoEventoAcceso.LOGOUT, duracionSeg, null);
    }
}
