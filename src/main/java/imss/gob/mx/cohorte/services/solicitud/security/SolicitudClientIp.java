package imss.gob.mx.cohorte.services.solicitud.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.util.*;

@Component
public class SolicitudClientIp {
    private final Set<String> trusted;
    public SolicitudClientIp(@Value("${solicitudes.security.trusted-proxies:}") String proxies) {
        Set<String> values = new HashSet<>();
        for (String proxy : proxies.split(",")) if (!proxy.isBlank()) values.add(canonical(proxy.strip()));
        trusted = Set.copyOf(values);
    }
    public String resolve(HttpServletRequest request) {
        String peer = canonical(request.getRemoteAddr());
        if (!trusted.contains(peer)) return peer;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.length() > 1000) return peer;
        String[] hops = forwarded.split(",", -1);
        if (hops.length > 16) return peer;
        String current = peer;
        for (int i = hops.length - 1; i >= 0 && trusted.contains(current); i--) {
            try { current = canonical(hops[i].strip()); } catch (IllegalArgumentException e) { return peer; }
        }
        return current;
    }
    public static String canonical(String raw) {
        if (raw == null || raw.length() > 45 || !raw.matches("[0-9a-fA-F:.]+")
                || (!raw.contains(":") && !raw.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))) throw new IllegalArgumentException("IP inválida");
        try { return InetAddress.getByName(raw).getHostAddress(); } catch (Exception e) { throw new IllegalArgumentException("IP inválida"); }
    }
}
