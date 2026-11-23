package imss.gob.mx.cohorte.services.solicitud.security;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SolicitudSecurityException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public SolicitudSecurityException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
}
