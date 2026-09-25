package imss.gob.mx.cohorte.services.notificaciones;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.notificaciones.CanalNotificacion;
import imss.gob.mx.cohorte.modules.notificaciones.NotificacionCita;
import imss.gob.mx.cohorte.modules.notificaciones.NotificacionCitaRepository;
import imss.gob.mx.cohorte.modules.notificaciones.TipoNotificacion;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaAgendadaEvent;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaCanceladaEvent;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaReprogramadaEvent;
import imss.gob.mx.cohorte.modules.persona.Persona;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitaNotificacionService {

    private final EmailService emailService;
    private final NotificacionCitaRepository notificacionRepo;

    private static final ZoneId ZONA_MX =
            ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
    private static final DateTimeFormatter FMT_HORA =
            DateTimeFormatter.ofPattern("HH:mm");

    // ── Listeners de eventos (se ejecutan en hilo async DESPUÉS del commit) ────

    @Async("notificacionesExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCitaAgendada(CitaAgendadaEvent event) {
        log.info("Notificación CONFIRMACION para cita {}", event.cita().getUuid());
        enviarNotificacion(event.cita(), TipoNotificacion.CONFIRMACION, null);
    }

    @Async("notificacionesExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCitaCancelada(CitaCanceladaEvent event) {
        log.info("Notificación CANCELACION para cita {}", event.cita().getUuid());
        enviarNotificacion(event.cita(), TipoNotificacion.CANCELACION, null);
    }

    @Async("notificacionesExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCitaReprogramada(CitaReprogramadaEvent event) {
        log.info("Notificación REPROGRAMACION para cita {}", event.cita().getUuid());
        enviarNotificacion(event.cita(), TipoNotificacion.REPROGRAMACION, event.fechaAnteriorUtc());
    }

    // ── Llamado directamente por el scheduler ──────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviarRecordatorio(Cita cita) {
        log.info("Notificación RECORDATORIO_24H para cita {}", cita.getUuid());
        enviarNotificacion(cita, TipoNotificacion.RECORDATORIO_24H, null);
    }

    // ── Lógica central ─────────────────────────────────────────────────────────

    private void enviarNotificacion(Cita cita, TipoNotificacion tipo, Instant fechaAnteriorUtc) {
        Persona persona = cita.getPaciente().getPersona();
        String nombre   = persona.getNombre() + " " + persona.getApellidoPaterno();

        ZonedDateTime inicio = cita.getStartAtUtc().atZone(ZONA_MX);
        String fecha    = FMT_FECHA.format(inicio);
        String hora     = FMT_HORA.format(inicio);
        int    duracion = cita.getDurationMinutes() != null ? cita.getDurationMinutes() : 60;

        enviarEmail(cita, tipo, persona.getEmail(), nombre, fecha, hora, duracion, fechaAnteriorUtc);
    }

    private void enviarEmail(Cita cita, TipoNotificacion tipo,
                              String email, String nombre,
                              String fecha, String hora, int duracion,
                              Instant fechaAnteriorUtc) {
        if (email == null || email.isBlank()) {
            log.debug("Email omitido para cita {}: sin email registrado", cita.getUuid());
            return;
        }
        try {
            Context ctx = new Context();
            ctx.setVariable("nombre",       nombre);
            ctx.setVariable("fecha",        fecha);
            ctx.setVariable("hora",         hora);
            ctx.setVariable("duracion",     duracion);
            ctx.setVariable("observaciones", cita.getObservaciones());

            if (tipo == TipoNotificacion.REPROGRAMACION && fechaAnteriorUtc != null) {
                ZonedDateTime anteriorZoned = fechaAnteriorUtc.atZone(ZONA_MX);
                ctx.setVariable("fechaAnterior", FMT_FECHA.format(anteriorZoned));
                ctx.setVariable("horaAnterior",  FMT_HORA.format(anteriorZoned));
            }

            agregarDatosInstitucion(ctx, cita.getInstitucion());

            emailService.enviar(email, asuntoEmail(tipo, fecha), templateEmail(tipo), ctx);
            notificacionRepo.save(NotificacionCita.exitosa(cita, tipo, CanalNotificacion.EMAIL));
        } catch (Exception e) {
            log.error("Fallo email {} | cita {}: {}", tipo, cita.getUuid(), e.getMessage());
            notificacionRepo.save(NotificacionCita.fallida(cita, tipo, CanalNotificacion.EMAIL, e.getMessage()));
        }
    }

    private void agregarDatosInstitucion(Context ctx, Institucion inst) {
        if (inst == null) {
            ctx.setVariable("institucion", "");
            ctx.setVariable("direccionInstitucion", "");
            ctx.setVariable("telefonoInstitucion", null);
            ctx.setVariable("googleMapsUrl", null);
            return;
        }

        ctx.setVariable("institucion", inst.getNombre());
        ctx.setVariable("telefonoInstitucion", inst.getTelefono());

        StringBuilder dir = new StringBuilder();
        if (inst.getDireccion() != null && !inst.getDireccion().isBlank()) {
            dir.append(inst.getDireccion());
        }
        if (inst.getCiudad() != null && !inst.getCiudad().isBlank()) {
            if (!dir.isEmpty()) dir.append("\n");
            dir.append(inst.getCiudad());
            if (inst.getEstado() != null && !inst.getEstado().isBlank()) {
                dir.append(", ").append(inst.getEstado());
            }
        }
        ctx.setVariable("direccionInstitucion", dir.toString());

        if (inst.getLatitud() != null && inst.getLongitud() != null) {
            ctx.setVariable("googleMapsUrl",
                    "https://www.google.com/maps?q=" + inst.getLatitud() + "," + inst.getLongitud());
        } else {
            ctx.setVariable("googleMapsUrl", null);
        }
    }

    // ── Helpers de contenido ───────────────────────────────────────────────────

    private String templateEmail(TipoNotificacion tipo) {
        return switch (tipo) {
            case CONFIRMACION     -> "email/confirmacion-cita";
            case RECORDATORIO_24H -> "email/recordatorio-cita";
            case CANCELACION      -> "email/cancelacion-cita";
            case REPROGRAMACION   -> "email/reprogramacion-cita";
        };
    }

    private String asuntoEmail(TipoNotificacion tipo, String fecha) {
        return switch (tipo) {
            case CONFIRMACION     -> "Confirmación de cita — " + fecha;
            case RECORDATORIO_24H -> "Recordatorio: tiene cita próximamente — " + fecha;
            case CANCELACION      -> "Su cita ha sido cancelada";
            case REPROGRAMACION   -> "Su cita ha sido reprogramada — " + fecha;
        };
    }
}
