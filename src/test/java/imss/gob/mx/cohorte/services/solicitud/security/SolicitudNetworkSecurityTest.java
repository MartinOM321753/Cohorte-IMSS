package imss.gob.mx.cohorte.services.solicitud.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.security.SolicitudPublicRequestFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.http.HttpStatus;
import java.io.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolicitudNetworkSecurityTest {
    @Test void ignoresForgedForwardingHeadersAndStopsAtFirstUntrustedHop() {
        var request=new MockHttpServletRequest();request.setRemoteAddr("198.51.100.10");request.addHeader("X-Forwarded-For","1.1.1.1");
        assertEquals("198.51.100.10",new SolicitudClientIp("").resolve(request));
        request.setRemoteAddr("127.0.0.1");request.removeHeader("X-Forwarded-For");request.addHeader("X-Forwarded-For","1.1.1.1, 198.51.100.10, 10.0.0.2");
        assertEquals("198.51.100.10",new SolicitudClientIp("127.0.0.1,10.0.0.2").resolve(request));
        request.removeHeader("X-Forwarded-For");request.addHeader("X-Forwarded-For","not-an-ip");
        assertEquals("127.0.0.1",new SolicitudClientIp("127.0.0.1").resolve(request));
    }
    @Test void throttleLimitsAndExpires() {
        var now=new AtomicReference<>(Instant.parse("2026-11-22T12:00:00Z"));
        Clock clock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return now.get();}};
        var throttle=new SolicitudThrottle(clock);throttle.acquire("a",2,Duration.ofMinutes(1));throttle.acquire("a",2,Duration.ofMinutes(1));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS,assertThrows(SolicitudSecurityException.class,()->throttle.acquire("a",2,Duration.ofMinutes(1))).getStatus());
        now.set(now.get().plusSeconds(60));assertDoesNotThrow(()->throttle.acquire("a",2,Duration.ofMinutes(1)));
    }
    private MockHttpServletRequest request(byte[] body,String type) {
        var request=new MockHttpServletRequest("POST","/api/publico/solicitud-datos");request.setRemoteAddr("198.51.100.10");request.setContentType(type);request.setContent(body);return request;
    }
    @Test void filterRejectsOversizedAndNonJsonBodiesBeforeControllerAndPreservesValidJson() throws Exception {
        var filter=new SolicitudPublicRequestFilter(new SolicitudThrottle(),new SolicitudClientIp(""),new ObjectMapper());
        var called=new AtomicBoolean();var response=new MockHttpServletResponse();
        filter.doFilter(request(new byte[16385],"application/json"),response,(req,res)->called.set(true));assertEquals(413,response.getStatus());assertFalse(called.get());
        response=new MockHttpServletResponse();filter.doFilter(request("{}".getBytes(),"text/plain"),response,(req,res)->called.set(true));assertEquals(415,response.getStatus());assertFalse(called.get());
        response=new MockHttpServletResponse();filter.doFilter(request("{\"name\":\"José\"}".getBytes(StandardCharsets.UTF_8),"application/json;charset=UTF-8"),response,(req,res)->{
            called.set(true);assertEquals("{\"name\":\"José\"}",new String(req.getInputStream().readAllBytes(),StandardCharsets.UTF_8));
        });assertTrue(called.get());
    }
    @Test void unconfiguredProviderFailsClosedWithoutExternalCall() {
        HttpClient client=mock(HttpClient.class);var check=new MaxMindSolicitudNetworkCheck(new ObjectMapper(),"","",client);
        var error=assertThrows(SolicitudSecurityException.class,()->check.requireAllowed("8.8.8.8"));assertEquals("NETWORK_CHECK_UNAVAILABLE",error.getCode());verifyNoInteractions(client);
    }
    @ParameterizedTest @ValueSource(strings={"is_anonymous_vpn","is_public_proxy","is_residential_proxy","is_tor_exit_node","is_hosting_provider"})
    void rejectsCurrentAndLegacyAnonymizerFlags(String flag) throws Exception {
        for(String node:new String[]{"anonymizer","traits"}) {
            String json=node.equals("traits")?"{\"country\":{\"iso_code\":\"MX\"},\"traits\":{\"ip_address\":\"8.8.8.8\",\""+flag+"\":true}}":"{\"country\":{\"iso_code\":\"MX\"},\"traits\":{\"ip_address\":\"8.8.8.8\"},\"anonymizer\":{\""+flag+"\":true}}";
            var check=provider(json,200);assertEquals("ANONYMOUS_NETWORK",assertThrows(SolicitudSecurityException.class,()->check.requireAllowed("8.8.8.8")).getCode());
        }
    }
    @Test void acceptsValidResponseAndRejectsMismatchedOrUnavailableResponse() throws Exception {
        var result=provider("{\"country\":{\"iso_code\":\"MX\"},\"traits\":{\"ip_address\":\"8.8.8.8\"}}",200).requireAllowed("8.8.8.8");assertEquals("MX",result.country());
        var mismatched=provider("{\"country\":{\"iso_code\":\"MX\"},\"traits\":{\"ip_address\":\"1.1.1.1\"}}",200);
        assertEquals("NETWORK_CHECK_UNAVAILABLE",assertThrows(SolicitudSecurityException.class,()->mismatched.requireAllowed("8.8.8.8")).getCode());
        var unavailable=provider("{}",503);assertThrows(SolicitudSecurityException.class,()->unavailable.requireAllowed("8.8.8.8"));
    }
    @SuppressWarnings("unchecked") private MaxMindSolicitudNetworkCheck provider(String json,int status) throws Exception {
        HttpClient client=mock(HttpClient.class);HttpResponse<InputStream> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        return new MaxMindSolicitudNetworkCheck(new ObjectMapper(),"test-account","test-license",client);
    }
}
