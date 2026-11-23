package imss.gob.mx.cohorte.services.solicitud.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Component
public class SolicitudCaptcha {
    public record Configuration(String siteKey, boolean testMode) {}
    private static final String TEST_SITE="1x00000000000000000000AA";
    private static final String TEST_SECRET="1x0000000000000000000000000000000AA";
    private final ObjectMapper mapper;
    private final SolicitudLocalTesting localTesting;
    private final String siteKey,secret;
    private final Set<String> hosts;
    private final HttpClient client;
    @Autowired
    public SolicitudCaptcha(ObjectMapper mapper,SolicitudLocalTesting localTesting,
            @Value("${solicitudes.security.turnstile-site-key:}") String siteKey,
            @Value("${solicitudes.security.turnstile-secret-key:}") String secret,
            @Value("${solicitudes.security.turnstile-hostnames:}") String hosts) {
        this(mapper,localTesting,siteKey,secret,hosts,HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build());
    }
    SolicitudCaptcha(ObjectMapper mapper,SolicitudLocalTesting localTesting,String siteKey,String secret,String hosts,HttpClient client) {
        this.mapper=mapper;this.localTesting=localTesting;this.siteKey=siteKey;this.secret=secret;this.client=client;
        this.hosts=new HashSet<>();for(String host:hosts.split(","))if(!host.isBlank())this.hosts.add(host.strip().toLowerCase(Locale.ROOT));
    }
    private static SolicitudSecurityException unavailable() {
        return new SolicitudSecurityException(HttpStatus.SERVICE_UNAVAILABLE,"CAPTCHA_UNAVAILABLE","La verificación antispam no está disponible. Intenta más tarde.");
    }
    public Configuration configuration(String ip) {
        if(localTesting.allowed(ip))return new Configuration(TEST_SITE,true);
        if(siteKey.isBlank()||secret.isBlank()||hosts.isEmpty()||siteKey.matches("[123]x0+.*")||secret.matches("[123]x0+.*"))throw unavailable();
        return new Configuration(siteKey,false);
    }
    public void verify(String token,String ip) {
        if(token==null||token.isBlank()||token.length()>2048)throw invalid();
        Configuration config=configuration(ip);
        try {
            String body=mapper.writeValueAsString(Map.of("secret",config.testMode()?TEST_SECRET:secret,"response",token,"remoteip",ip));
            var request=HttpRequest.newBuilder(URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .timeout(Duration.ofSeconds(8)).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200||response.body().length()>65536)throw unavailable();
            var result=mapper.readTree(response.body());
            if(!result.path("success").asBoolean(false))throw invalid();
            if(!config.testMode()&&(!hosts.contains(result.path("hostname").asText().toLowerCase(Locale.ROOT))||!"data_request".equals(result.path("action").asText())))throw invalid();
        }catch(SolicitudSecurityException e){throw e;}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}
        catch(Exception e){throw unavailable();}
    }
    private static SolicitudSecurityException invalid() {
        return new SolicitudSecurityException(HttpStatus.BAD_REQUEST,"CAPTCHA_INVALID","Completa de nuevo la verificación antispam antes de enviar.");
    }
}
