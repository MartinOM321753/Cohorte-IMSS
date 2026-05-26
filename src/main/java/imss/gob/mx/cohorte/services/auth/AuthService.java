package imss.gob.mx.cohorte.services.auth;

import imss.gob.mx.cohorte.controllers.auth.dto.LoginRequestDTO;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.security.jwt.JWTUtils;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService  {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JWTUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    /**
     * Autentica al usuario aceptando nombre de usuario O correo electrónico.
     * Busca primero por username; si no encuentra, busca por email del persona asociada.
     * El mensaje de error es siempre el mismo para no revelar qué existe en el sistema.
     */
    @Transactional(readOnly = true)
    public String login(LoginRequestDTO payload) {
        String identifier = payload.getIdentifier().trim();

        // Intentar por username primero; si no existe, intentar por email (solo activos)
        BeanUser found = userService.findByUsername(identifier)
                .or(() -> userRepository.findActiveUserByPersonaEmail(identifier))
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));

        if (!Boolean.TRUE.equals(found.getActivo())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        if (!passwordEncoder.matches(payload.getPassword(), found.getPassword())) {
            throw new BadCredentialsException("Usuario o contraseña incorrectos");
        }

        return jwtUtils.generateToken(found);
    }
}
