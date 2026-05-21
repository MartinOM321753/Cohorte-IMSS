package imss.gob.mx.cohorte.infrastructure.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Servicio de envío de correo usando Brevo SMTP relay + plantillas Thymeleaf.
 * Cuando notificaciones.mail.enabled=false solo loguea — el app arranca sin credenciales.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${notificaciones.mail.from:noreply@cohorte.imss.gob.mx}")
    private String mailFrom;

    @Value("${notificaciones.mail.enabled:false}")
    private boolean enabled;

    /**
     * @param destinatario email del paciente
     * @param asunto       asunto del correo
     * @param template     ruta relativa a /templates (p.ej. "email/confirmacion-cita")
     * @param context      variables para la plantilla Thymeleaf
     */
    public void enviar(String destinatario, String asunto, String template, Context context) {
        if (!enabled) {
            log.info("[EMAIL DESACTIVADO] Para: {} | Asunto: {}", destinatario, asunto);
            return;
        }
        if (destinatario == null || destinatario.isBlank()) {
            log.warn("Email omitido: destinatario vacío");
            return;
        }
        try {
            String html = templateEngine.process(template, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("Email enviado a {} | {}", destinatario, asunto);
        } catch (Exception e) {
            log.error("Error enviando email a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("Error enviando email: " + e.getMessage(), e);
        }
    }
}
