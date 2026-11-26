package imss.gob.mx.cohorte.services.solicitud.security;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Component
public class MaxMindSolicitudNetworkCheck implements SolicitudNetworkCheck {
    private record Cached(Result result, boolean blocked, Instant expires) {}
    private final Map<String, Cached> cache = new HashMap<>();
    private final ObjectMapper mapper;
    private final String account, license;
    private final HttpClient client;
    private SolicitudLocalTesting localTesting;
    @Autowired
    public MaxMindSolicitudNetworkCheck(ObjectMapper mapper,
            @Value("${solicitudes.security.maxmind-account-id:}") String account,
            @Value("${solicitudes.security.maxmind-license-key:}") String license,
            SolicitudLocalTesting localTesting) {
        this(mapper, account, license, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build());
        this.localTesting=localTesting;
    }
    MaxMindSolicitudNetworkCheck(ObjectMapper mapper, String account, String license, HttpClient client) {
        this.mapper = mapper; this.account = account; this.license = license; this.client = client;
    }
    private static SolicitudSecurityException unavailable() {
        return new SolicitudSecurityException(HttpStatus.SERVICE_UNAVAILABLE, "NETWORK_CHECK_UNAVAILABLE", "La comprobación de conexión no está disponible. Intenta más tarde.");
    }
    private static void reject() {
        throw new SolicitudSecurityException(HttpStatus.FORBIDDEN, "ANONYMOUS_NETWORK", "La conexión se identifica como VPN, proxy o red de alojamiento. Usa una conexión directa para continuar.");
    }
    @Override public Result requireAllowed(String ip) {
        if(localTesting!=null && localTesting.allowed(ip)) return new Result("ZZ","Prueba local; VPN sin verificar",Instant.now());
        if (account.isBlank() || license.isBlank()) return new Result("XX","MaxMind no configurado; verificación de red omitida",Instant.now());
        synchronized (cache) {
            Cached c = cache.get(ip);
            if (c != null && c.expires.isAfter(Instant.now())) { if (c.blocked) reject(); return c.result; }
        }
        try {
            InetAddress address = InetAddress.getByName(SolicitudClientIp.canonical(ip));
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) throw unavailable();
            String auth = Base64.getEncoder().encodeToString((account + ":" + license).getBytes(StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://geoip.maxmind.com/geoip/v2.1/insights/" + ip))
                    .timeout(Duration.ofSeconds(4)).header("Authorization", "Basic " + auth).header("Accept", "application/json").GET().build();
            HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            byte[] body;
            try (var stream = response.body()) { body = stream.readNBytes(65537); }
            if (response.statusCode() != 200 || body.length > 65536) throw unavailable();
            JsonNode root = mapper.readTree(body), traits = root.path("traits"), anonymizer = root.path("anonymizer");
            String country = root.path("country").path("iso_code").asText();
            if (!country.matches("[A-Z]{2}") || !SolicitudClientIp.canonical(traits.path("ip_address").asText()).equals(ip)) throw unavailable();
            boolean blocked = false;
            for (String flag : List.of("is_anonymous", "is_anonymous_vpn", "is_public_proxy", "is_residential_proxy", "is_tor_exit_node", "is_hosting_provider")) {
                blocked |= anonymizer.path(flag).asBoolean(false) || traits.path(flag).asBoolean(false);
            }
            Result result = new Result(country, "MaxMind GeoIP Insights", Instant.now());
            synchronized (cache) {
                if (cache.size() >= 10000) cache.entrySet().removeIf(e -> !e.getValue().expires.isAfter(Instant.now()));
                if (cache.size() < 10000) cache.put(ip, new Cached(result, blocked, Instant.now().plusSeconds(300)));
            }
            if (blocked) reject();
            return result;
        } catch (SolicitudSecurityException e) { throw e; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw unavailable(); }
        catch (Exception e) { throw unavailable(); }
    }
}
