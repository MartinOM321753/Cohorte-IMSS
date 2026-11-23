package imss.gob.mx.cohorte.services.solicitud.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import javax.naming.*;
import javax.naming.directory.*;
import java.util.*;

@Component
public class SolicitudEmailDomain {
    private final Set<String> blocked = new HashSet<>(Set.of("mailinator.com", "yopmail.com", "guerrillamail.com", "sharklasers.com", "10minutemail.com", "temp-mail.org", "guerrillamailblock.com", "grr.la"));
    private final SolicitudLocalTesting localTesting;
    public SolicitudEmailDomain(SolicitudLocalTesting localTesting,
            @Value("${solicitudes.security.blocked-email-domains:}") String extra) {
        this.localTesting = localTesting;
        for (String item : extra.split(",")) if (!item.isBlank()) blocked.add(item.strip().toLowerCase(Locale.ROOT));
    }
    public void check(String email) {
        check(email, null);
    }
    public void check(String email, String ip) {
        String domain = email.substring(email.lastIndexOf('@') + 1);
        if (blocked.stream().anyMatch(d -> domain.equals(d) || domain.endsWith("." + d))) throw SolicitudInputValidator.invalid("Utiliza un correo de contacto permanente.");
        if (ip != null && localTesting.allowed(ip)) return;
        Hashtable<String,String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "2000"); env.put("com.sun.jndi.dns.timeout.retries", "1");
        DirContext context = null;
        try {
            context = new InitialDirContext(env);
            Attributes records = context.getAttributes(domain, new String[]{"MX", "A", "AAAA"});
            Attribute mx = records.get("MX");
            if (mx != null && mx.size() > 0) {
                boolean usable = false;
                for (int i = 0; i < mx.size(); i++) if (!String.valueOf(mx.get(i)).strip().matches("0\\s+\\.")) usable = true;
                if (!usable) throw SolicitudInputValidator.invalid("El dominio de correo no acepta mensajes.");
            } else if (records.get("A") == null && records.get("AAAA") == null) throw SolicitudInputValidator.invalid("El dominio de correo no acepta mensajes.");
        } catch (NameNotFoundException e) { throw SolicitudInputValidator.invalid("El dominio de correo no existe."); }
        catch (NamingException e) { throw new SolicitudSecurityException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_DNS_UNAVAILABLE", "No se pudo comprobar el dominio de correo. Intenta más tarde."); }
        finally { if (context != null) try { context.close(); } catch (NamingException ignored) {} }
    }
}
