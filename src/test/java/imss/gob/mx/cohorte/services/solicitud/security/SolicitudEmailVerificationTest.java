package imss.gob.mx.cohorte.services.solicitud.security;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.solicitud.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.thymeleaf.context.Context;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolicitudEmailVerificationTest {
    private final SolicitudCorreoVerificacionRepository repo=mock(SolicitudCorreoVerificacionRepository.class);
    private final EmailService mail=mock(EmailService.class);
    private final SolicitudEmailDomain domains=mock(SolicitudEmailDomain.class);
    private final Instant now=Instant.parse("2026-11-22T12:00:00Z");
    private final Clock clock=Clock.fixed(now,ZoneOffset.UTC);
    private final SolicitudEmailVerification service=new SolicitudEmailVerification(repo,mail,domains,new SolicitudThrottle(clock),true,"test-secret-32-characters-minimum-12345",clock);
    private SolicitudCorreoVerificacion ticket;
    private String code;
    @BeforeEach void setup() {
        when(repo.save(any())).thenAnswer(call->{ticket=call.getArgument(0);return ticket;});
        when(repo.locked(anyString())).thenAnswer(call->ticket!=null&&ticket.getId().equals(call.getArgument(0))?Optional.of(ticket):Optional.empty());
    }
    private String issue() {
        var challenge=service.issue("ana@example.org","198.51.100.10");
        var ctx=ArgumentCaptor.forClass(Context.class);
        verify(mail).enviar(eq("ana@example.org"),anyString(),eq("email/verificacion-solicitud-datos"),ctx.capture());
        code=(String)ctx.getValue().getVariable("codigo");return challenge.challengeId();
    }
    @Test void requiresProofBeforeConsumeAndPreventsReplay() {
        String id=issue();assertEquals(43,id.length());assertTrue(code.matches("[0-9]{6}"));assertNotEquals(code,ticket.getCodeHash());
        assertThrows(SolicitudSecurityException.class,()->service.consume(id,"ana@example.org","198.51.100.10"));
        var verified=service.confirm(id,code,"ana@example.org","198.51.100.10");
        assertEquals(now.plusSeconds(1800),verified.expiresAt());
        assertEquals(now,service.consume(id,"ana@example.org","198.51.100.10"));
        assertThrows(SolicitudSecurityException.class,()->service.consume(id,"ana@example.org","198.51.100.10"));
    }
    @Test void bindsProofToEmailAndIpAndExpires() {
        String id=issue();
        assertThrows(SolicitudSecurityException.class,()->service.confirm(id,code,"other@example.org","198.51.100.10"));
        assertThrows(SolicitudSecurityException.class,()->service.confirm(id,code,"ana@example.org","198.51.100.11"));
        service.confirm(id,code,"ana@example.org","198.51.100.10");
        assertThrows(SolicitudSecurityException.class,()->service.consume(id,"other@example.org","198.51.100.10"));
        ticket.setExpiresAt(now);assertThrows(SolicitudSecurityException.class,()->service.consume(id,"ana@example.org","198.51.100.10"));
    }
    @Test void locksAfterFiveIncorrectAttemptsAndPersistsFailureCounters() throws Exception {
        String id=issue();String wrong=code.equals("000000")?"000001":"000000";
        for(int i=0;i<5;i++)assertThrows(SolicitudSecurityException.class,()->service.confirm(id,wrong,"ana@example.org","198.51.100.10"));
        assertEquals(5,ticket.getFailedAttempts());assertThrows(SolicitudSecurityException.class,()->service.confirm(id,code,"ana@example.org","198.51.100.10"));
        var tx=new AnnotationTransactionAttributeSource().getTransactionAttribute(SolicitudEmailVerification.class.getMethod("confirm",String.class,String.class,String.class,String.class),SolicitudEmailVerification.class);
        assertNotNull(tx);assertFalse(tx.rollbackOn(SolicitudInputValidator.invalid("test")));
    }
    @Test void smtpDisabledCannotPretendToVerify() {
        var disabled=new SolicitudEmailVerification(repo,mail,domains,new SolicitudThrottle(clock),false,"test-secret-32-characters-minimum-12345",clock);
        assertThrows(SolicitudSecurityException.class,()->disabled.issue("ana@example.org","198.51.100.10"));verifyNoInteractions(mail,repo);
    }
    @Test void smtpFailureDoesNotYieldAChallenge() {
        doThrow(new IllegalStateException("mock delivery failure")).when(mail).enviar(anyString(),anyString(),anyString(),any(Context.class));
        var failure=assertThrows(SolicitudSecurityException.class,()->service.issue("ana@example.org","198.51.100.10"));
        assertEquals("EMAIL_DELIVERY_UNAVAILABLE",failure.getCode());assertNull(ticket.getVerifiedAt());
    }
}
