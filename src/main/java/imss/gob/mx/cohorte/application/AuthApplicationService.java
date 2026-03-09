package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.auth.AuthService;
import imss.gob.mx.cohorte.modules.auth.LoginRequestDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthApplicationService {
    private final AuthService authService;

    public String login(LoginRequestDTO payload) {

        return authService.doLogin(payload);
    }




}
