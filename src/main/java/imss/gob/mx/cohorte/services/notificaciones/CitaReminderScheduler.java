package imss.gob.mx.cohorte.services.notificaciones;

import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.cita.CitaRepository;
import imss.gob.mx.cohorte.modules.cita.EstadoCita;
import imss.gob.mx.cohorte.modules.notificaciones.CanalNotificacion;
import imss.gob.mx.cohorte.modules.notificaciones.NotificacionCitaRepository;
import imss.gob.mx.cohorte.modules.notificaciones.TipoNotificacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Se ejecuta a las 8:00 AM hora de la Ciudad de México y envía recordatorios
 * a participantes con cita en las próximas 30 horas.
 *
 * La ventana amplia (0–30h) asegura que se cubran todas las citas del día
 * siguiente. La deduplicación evita envíos repetidos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CitaReminderScheduler {

    private final CitaRepository          citaRepository;
    private final NotificacionCitaRepository notificacionRepo;
    private final CitaNotificacionService notificacionService;

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Mexico_City")
    public void enviarRecordatorios() {
        Instant desde = Instant.now();
        Instant hasta = Instant.now().plusSeconds(30 * 3600L);

        List<Cita> candidatas = citaRepository.findByStartAtUtcBetween(desde, hasta)
                .stream()
                .filter(c -> c.getEstadoCita() != EstadoCita.Cancelada)
                .filter(c -> !yaNotificadoExitosamente(c))
                .toList();

        log.info("Scheduler recordatorio 8AM: {} cita(s) a notificar", candidatas.size());

        for (Cita cita : candidatas) {
            try {
                notificacionService.enviarRecordatorio(cita);
            } catch (Exception e) {
                log.error("Error en recordatorio cita {}: {}", cita.getUuid(), e.getMessage());
            }
        }
    }

    private boolean yaNotificadoExitosamente(Cita cita) {
        return notificacionRepo.existsByCitaAndTipoAndCanalAndExitoso(
                cita, TipoNotificacion.RECORDATORIO_24H, CanalNotificacion.EMAIL, true);
    }
}
