package imss.gob.mx.cohorte.services.solicitud.security;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;

@Component
public class SolicitudThrottle {
    private record Window(Instant expires, int count) {}
    private final Map<String, Window> counters = new HashMap<>();
    private final Clock clock;
    public SolicitudThrottle() { this(Clock.systemUTC()); }
    SolicitudThrottle(Clock clock) { this.clock = clock; }
    public synchronized void acquire(String key, int limit, Duration duration) {
        Instant now = clock.instant();
        Window current = counters.get(key);
        if (current == null || !current.expires.isAfter(now)) {
            if (counters.size() >= 20000) counters.entrySet().removeIf(e -> !e.getValue().expires.isAfter(now));
            if (counters.size() >= 20000 && current == null) throw new SolicitudSecurityException(HttpStatus.SERVICE_UNAVAILABLE, "SECURITY_BUSY", "La verificación está ocupada. Intenta más tarde.");
            current = new Window(now.plus(duration), 0);
        }
        if (current.count >= limit) throw new SolicitudSecurityException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Has realizado demasiados intentos. Intenta más tarde.");
        counters.put(key, new Window(current.expires, current.count + 1));
    }
}
