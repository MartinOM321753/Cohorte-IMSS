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
 * Corre cada hora y envía recordatorios a pacientes con cita en las próximas ~24 horas.
 *
 * Ventana de búsqueda: 23h – 25h desde ahora.
 * Si el scheduler se ejecuta cada hora, cada cita caerá en la ventana exactamente una vez.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CitaReminderScheduler {

    private final CitaRepository          citaRepository;
    private final NotificacionCitaRepository notificacionRepo;
    private final CitaNotificacionService notificacionService;

    @Scheduled(cron = "0 0 * * * *")   // cada hora en punto
    public void enviarRecordatorios24h() {
        Instant desde = Instant.now().plusSeconds(23 * 3600L);
        Instant hasta = Instant.now().plusSeconds(25 * 3600L);

        List<Cita> candidatas = citaRepository.findByStartAtUtcBetween(desde, hasta)
                .stream()
                .filter(c -> c.getEstadoCita() != EstadoCita.Cancelada)
                .filter(c -> !yaNotificadoExitosamente(c))
                .toList();

        log.info("Scheduler recordatorio 24h: {} cita(s) a notificar", candidatas.size());

        for (Cita cita : candidatas) {
            try {
                notificacionService.enviarRecordatorio(cita);
            } catch (Exception e) {
                log.error("Error en recordatorio cita {}: {}", cita.getUuid(), e.getMessage());
            }
        }
    }

    /**
     * Considera "ya notificada" si al menos uno de los canales fue exitoso.
     * Así si solo falló WhatsApp pero el email llegó, no se reenvía todo.
     */
    private boolean yaNotificadoExitosamente(Cita cita) {
        return notificacionRepo.existsByCitaAndTipoAndCanalAndExitoso(
                       cita, TipoNotificacion.RECORDATORIO_24H, CanalNotificacion.EMAIL, true)
               || notificacionRepo.existsByCitaAndTipoAndCanalAndExitoso(
                       cita, TipoNotificacion.RECORDATORIO_24H, CanalNotificacion.WHATSAPP, true);
    }
}
