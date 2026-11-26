package imss.gob.mx.cohorte.services.solicitud.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.util.*;

@Component
public class SolicitudClientIp {
    private final Set<String> trustedIps;
    private final List<CidrRange> trustedRanges;

    public SolicitudClientIp(@Value("${solicitudes.security.trusted-proxies:}") String proxies) {
        Set<String> ips = new HashSet<>();
        List<CidrRange> ranges = new ArrayList<>();
        for (String entry : proxies.split(",")) {
            String trimmed = entry.strip();
            if (trimmed.isBlank()) continue;
            if (trimmed.contains("/")) {
                ranges.add(CidrRange.parse(trimmed));
            } else {
                ips.add(canonical(trimmed));
            }
        }
        trustedIps = Set.copyOf(ips);
        trustedRanges = List.copyOf(ranges);
    }

    private boolean isTrusted(String canonicalIp) {
        if (trustedIps.contains(canonicalIp)) return true;
        for (CidrRange range : trustedRanges) if (range.contains(canonicalIp)) return true;
        return false;
    }

    public String resolve(HttpServletRequest request) {
        String peer = canonical(request.getRemoteAddr());
        if (!isTrusted(peer)) return peer;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.length() > 1000) return peer;
        String[] hops = forwarded.split(",", -1);
        if (hops.length > 16) return peer;
        String current = peer;
        for (int i = hops.length - 1; i >= 0 && isTrusted(current); i--) {
            try { current = canonical(hops[i].strip()); } catch (IllegalArgumentException e) { return peer; }
        }
        return current;
    }

    public static String canonical(String raw) {
        if (raw == null || raw.length() > 45 || !raw.matches("[0-9a-fA-F:.]+")
                || (!raw.contains(":") && !raw.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))) throw new IllegalArgumentException("IP inválida");
        try { return InetAddress.getByName(raw).getHostAddress(); } catch (Exception e) { throw new IllegalArgumentException("IP inválida"); }
    }

    private record CidrRange(byte[] network, int prefixLen) {
        static CidrRange parse(String cidr) {
            String[] parts = cidr.split("/", 2);
            try {
                byte[] addr = InetAddress.getByName(parts[0].strip()).getAddress();
                int prefix = Integer.parseInt(parts[1].strip());
                if (prefix < 0 || prefix > addr.length * 8) throw new IllegalArgumentException("Prefijo CIDR inválido: " + cidr);
                return new CidrRange(addr, prefix);
            } catch (NumberFormatException | java.net.UnknownHostException e) {
                throw new IllegalArgumentException("CIDR inválido: " + cidr);
            }
        }

        boolean contains(String canonicalIp) {
            try {
                byte[] addr = InetAddress.getByName(canonicalIp).getAddress();
                if (addr.length != network.length) return false;
                int fullBytes = prefixLen / 8;
                for (int i = 0; i < fullBytes; i++) if (addr[i] != network[i]) return false;
                int remainingBits = prefixLen % 8;
                if (remainingBits > 0) {
                    int mask = 0xFF << (8 - remainingBits);
                    if ((addr[fullBytes] & mask) != (network[fullBytes] & mask)) return false;
                }
                return true;
            } catch (Exception e) { return false; }
        }
    }
}
