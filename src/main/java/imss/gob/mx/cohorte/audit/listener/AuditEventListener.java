package imss.gob.mx.cohorte.audit.listener;

import imss.gob.mx.cohorte.audit.events.AccesoAuditEvent;
import imss.gob.mx.cohorte.audit.events.AccionAuditEvent;
import imss.gob.mx.cohorte.audit.service.AuditService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener asíncrono de eventos de auditoría.
 * Recibe los eventos publicados por el aspecto y por AuthApplicationService,
 * y los delega a {@link AuditService} para persistencia.
 *
 * <p>Al ser {@code @Async}, la persistencia no bloquea el hilo HTTP
 * ni afecta el tiempo de respuesta del endpoint.</p>
 */
@Component
@AllArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditService auditService;

    @Async("auditExecutor")
    @EventListener
    public void onAcceso(AccesoAuditEvent event) {
        try {
            auditService.registrarAcceso(event);
        } catch (Exception e) {
            log.error("[AUDIT] Error al registrar acceso tipo {}: {}",
                    event.getTipoEvento(), e.getMessage());
        }
    }

    @Async("auditExecutor")
    @EventListener
    public void onAccion(AccionAuditEvent event) {
        try {
            auditService.registrarAccion(event);
        } catch (Exception e) {
            log.error("[AUDIT] Error al registrar acción {} en {}: {}",
                    event.getTipoAccion(), event.getEntidadAfectada(), e.getMessage());
        }
    }
}
