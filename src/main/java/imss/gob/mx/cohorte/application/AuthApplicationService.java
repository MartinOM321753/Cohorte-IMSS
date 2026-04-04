package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.auth.dto.LoginRequestDTO;
import imss.gob.mx.cohorte.services.auth.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthApplicationService {

    private final AuthService authService;

    public String login(LoginRequestDTO payload) {
        return authService.login(payload);
    }
}
