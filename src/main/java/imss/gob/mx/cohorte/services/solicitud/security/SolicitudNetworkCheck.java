package imss.gob.mx.cohorte.services.solicitud.security;
import java.time.Instant;
public interface SolicitudNetworkCheck {
    record Result(String country, String provider, Instant checkedAt) {}
    Result requireAllowed(String ip);
}
