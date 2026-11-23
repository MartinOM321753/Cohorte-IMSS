package imss.gob.mx.cohorte.services.solicitud.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.util.Arrays;

/** Explicit opt-in, only on a development profile and an actual loopback peer. */
@Component
public class SolicitudLocalTesting {
    private final boolean enabled;
    public SolicitudLocalTesting(Environment environment,
            @Value("${solicitudes.security.local-testing:false}") boolean requested) {
        var profiles=Arrays.asList(environment.getActiveProfiles());
        enabled=requested && (profiles.contains("dev") || profiles.contains("test")) && !profiles.contains("prod");
    }
    public boolean allowed(String ip) {
        if(!enabled)return false;
        try{return InetAddress.getByName(SolicitudClientIp.canonical(ip)).isLoopbackAddress();}
        catch(Exception ignored){return false;}
    }
}
