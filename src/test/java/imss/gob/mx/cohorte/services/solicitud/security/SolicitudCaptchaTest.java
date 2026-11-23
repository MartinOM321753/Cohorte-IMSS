package imss.gob.mx.cohorte.services.solicitud.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolicitudCaptchaTest {
    private SolicitudLocalTesting local(String... profiles){var env=new MockEnvironment();env.setActiveProfiles(profiles);return new SolicitudLocalTesting(env,true);}
    @Test void localModeCannotEnableProductionOrExternalRequests(){
        assertTrue(local("dev").allowed("127.0.0.1"));assertTrue(local("test").allowed("::1"));
        assertFalse(local("dev").allowed("8.8.8.8"));assertFalse(local("prod").allowed("127.0.0.1"));assertFalse(local("dev","prod").allowed("127.0.0.1"));
        assertFalse(new SolicitudLocalTesting(new MockEnvironment(),true).allowed("127.0.0.1"));
    }
    @Test void testKeysNeverAuthorizePublicOrProductionTraffic(){
        var check=new SolicitudCaptcha(new ObjectMapper(),local("dev"),"1x00000000000000000000AA","1x0000000000000000000000000000000AA","localhost",mock(HttpClient.class));
        assertTrue(check.configuration("127.0.0.1").testMode());assertThrows(SolicitudSecurityException.class,()->check.configuration("8.8.8.8"));
    }
    @Test void rejectsMissingAndOverlongTokensBeforeExternalCall(){
        var client=mock(HttpClient.class);var check=new SolicitudCaptcha(new ObjectMapper(),local("dev"),"","","",client);
        assertThrows(SolicitudSecurityException.class,()->check.verify("","127.0.0.1"));assertThrows(SolicitudSecurityException.class,()->check.verify("x".repeat(2049),"127.0.0.1"));verifyNoInteractions(client);
    }
    @SuppressWarnings("unchecked") private SolicitudCaptcha provider(String body) throws Exception {
        var client=mock(HttpClient.class);HttpResponse<String> response=mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);when(response.body()).thenReturn(body);
        when(client.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(response);
        return new SolicitudCaptcha(new ObjectMapper(),local("prod"),"real-site-key","server-only-secret","cohorte.example.org",client);
    }
    @Test void requiresProviderSuccessExpectedHostnameAndAction()throws Exception{
        assertDoesNotThrow(()->provider("{\"success\":true,\"hostname\":\"cohorte.example.org\",\"action\":\"data_request\"}").verify("a-token","8.8.8.8"));
        assertThrows(SolicitudSecurityException.class,()->provider("{\"success\":false}").verify("a-token","8.8.8.8"));
        assertThrows(SolicitudSecurityException.class,()->provider("{\"success\":true,\"hostname\":\"attacker.example.org\",\"action\":\"data_request\"}").verify("a-token","8.8.8.8"));
        assertThrows(SolicitudSecurityException.class,()->provider("{\"success\":true,\"hostname\":\"cohorte.example.org\",\"action\":\"login\"}").verify("a-token","8.8.8.8"));
    }
}
