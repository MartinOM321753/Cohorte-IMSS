package imss.gob.mx.cohorte.services.solicitud.security;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.solicitud.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class SolicitudEmailVerification {
    public record Challenge(String challengeId, int expiresInSeconds) {}
    public record Verified(String verificationToken, Instant expiresAt) {}
    private final SolicitudCorreoVerificacionRepository repository;
    private final EmailService mail;
    private final SolicitudEmailDomain domains;
    private final SolicitudThrottle throttle;
    private final boolean enabled;
    private final String secret;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    @Autowired
    public SolicitudEmailVerification(SolicitudCorreoVerificacionRepository repository, EmailService mail,
            SolicitudEmailDomain domains, SolicitudThrottle throttle,
            @Value("${notificaciones.mail.enabled:false}") boolean enabled,
            @Value("${solicitudes.security.verification-secret:${secret.key:}}") String secret) {
        this(repository, mail, domains, throttle, enabled, secret, Clock.systemUTC());
    }
    SolicitudEmailVerification(SolicitudCorreoVerificacionRepository repository, EmailService mail,
            SolicitudEmailDomain domains, SolicitudThrottle throttle, boolean enabled, String secret, Clock clock) {
        this.repository=repository;this.mail=mail;this.domains=domains;this.throttle=throttle;this.enabled=enabled;this.secret=secret;this.clock=clock;
    }
    public void requireAvailable() {
        if (!enabled || secret.length()<32) throw new SolicitudSecurityException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_VERIFICATION_UNAVAILABLE", "La verificación de correo no está disponible. Intenta más tarde.");
    }
    private SolicitudSecurityException invalid() {
        return new SolicitudSecurityException(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_INVALID", "La verificación de correo no es válida o expiró. Solicita un código nuevo.");
    }
    private String hash(String id, String code) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("request-email-v1|" + id + "|" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) { throw new IllegalStateException("No se pudo iniciar la verificación"); }
    }
    @Transactional(noRollbackFor=SolicitudSecurityException.class)
    public Challenge issue(String rawEmail, String ip) {
        requireAvailable();
        String email=SolicitudInputValidator.email(rawEmail);
        throttle.acquire("email-ip:"+ip,5,Duration.ofHours(1));
        throttle.acquire("email-address:"+hash("address",email),3,Duration.ofHours(1));
        Instant now=clock.instant();
        if (repository.existsByEmailAndCreatedAtAfter(email,now.minusSeconds(60))
                || repository.countByEmailAndCreatedAtAfter(email,now.minusSeconds(3600))>=3
                || repository.countByIpAndCreatedAtAfter(ip,now.minusSeconds(3600))>=5) {
            throw new SolicitudSecurityException(HttpStatus.TOO_MANY_REQUESTS,"RATE_LIMITED","Espera antes de solicitar otro código.");
        }
        domains.check(email);
        byte[] token=new byte[32];random.nextBytes(token);
        String id=Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        String code=String.format(Locale.ROOT,"%06d",random.nextInt(1000000));
        SolicitudCorreoVerificacion v=new SolicitudCorreoVerificacion();
        v.setId(id);v.setEmail(email);v.setIp(ip);v.setCodeHash(hash(id,code));v.setCreatedAt(now);v.setExpiresAt(now.plusSeconds(600));
        repository.deleteByExpiresAtBefore(now.minusSeconds(86400));
        repository.save(v);
        Context context=new Context();context.setVariable("codigo",code);
        try { mail.enviar(email,"Verifica tu correo — Cohorte de Trabajadores de la Salud","email/verificacion-solicitud-datos",context); }
        catch (RuntimeException e) {
            throw new SolicitudSecurityException(HttpStatus.SERVICE_UNAVAILABLE,"EMAIL_DELIVERY_UNAVAILABLE","No se pudo enviar el código. Intenta más tarde.");
        }
        return new Challenge(id,600);
    }
    @Transactional(noRollbackFor=SolicitudSecurityException.class)
    public Verified confirm(String id,String code,String rawEmail,String ip) {
        requireAvailable();
        throttle.acquire("confirm-ip:"+ip,30,Duration.ofHours(1));
        SolicitudCorreoVerificacion v=repository.locked(id).orElseThrow(this::invalid);
        Instant now=clock.instant();
        if (!v.getEmail().equals(SolicitudInputValidator.email(rawEmail)) || !v.getIp().equals(ip)
                || !v.getExpiresAt().isAfter(now) || v.getUsedAt()!=null || v.getFailedAttempts()>=5) throw invalid();
        if (code==null || !code.matches("[0-9]{6}") || !MessageDigest.isEqual(v.getCodeHash().getBytes(StandardCharsets.US_ASCII),hash(id,code).getBytes(StandardCharsets.US_ASCII))) {
            v.setFailedAttempts(v.getFailedAttempts()+1);repository.save(v);throw invalid();
        }
        if (v.getVerifiedAt()==null) {
            v.setVerifiedAt(now);v.setExpiresAt(now.plusSeconds(1800));repository.save(v);
        }
        return new Verified(v.getId(),v.getExpiresAt());
    }
    @Transactional
    public Instant consume(String id,String rawEmail,String ip) {
        requireAvailable();
        SolicitudCorreoVerificacion v=repository.locked(id).orElseThrow(this::invalid);
        Instant now=clock.instant();
        if (v.getVerifiedAt()==null || v.getUsedAt()!=null || !v.getExpiresAt().isAfter(now)
                || !v.getEmail().equals(SolicitudInputValidator.email(rawEmail)) || !v.getIp().equals(ip)) throw invalid();
        v.setUsedAt(now);repository.save(v);
        return v.getVerifiedAt();
    }
}
