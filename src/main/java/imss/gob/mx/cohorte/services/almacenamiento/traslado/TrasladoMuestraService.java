package imss.gob.mx.cohorte.services.almacenamiento.traslado;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.almacenamiento.almacen.Almacen;
import imss.gob.mx.cohorte.modules.almacenamiento.almacen.AlmacenRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestraRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrasladoMuestraService {

    private final TrasladoMuestraRepository trasladoRepository;
    private final MuestraRepository muestraRepository;
    private final AlmacenRepository almacenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Estados que indican que la muestra está fuera del biobanco. */
    private static final List<EstadoTraslado> ESTADOS_ACTIVOS =
            List.of(EstadoTraslado.TRASLADADA, EstadoTraslado.RECIBIDA, EstadoTraslado.EN_DEVOLUCION);

    // ── Consultas ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getAll() {
        return trasladoRepository.findAllByOrderByFechaTrasladoDesc();
    }

    @Transactional(readOnly = true)
    public TrasladoMuestra getById(Long id) {
        return trasladoRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el traslado con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getHistorialByMuestra(Long idMuestra) {
        if (!muestraRepository.existsById(idMuestra)) {
            throw new ObjNotFoundException("No se encontró la muestra con id: " + idMuestra);
        }
        return trasladoRepository.findAllByMuestra_IdOrderByFechaTrasladoDesc(idMuestra);
    }

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getTrasladosByAlmacen(Long idAlmacen) {
        return trasladoRepository.findAllByAlmacen_IdOrderByFechaTrasladoDesc(idAlmacen);
    }

    @Transactional(readOnly = true)
    public Page<TrasladoMuestra> getTrasladosByAlmacenPaginated(Long idAlmacen, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaTraslado"));
        return trasladoRepository.findAllByAlmacen_Id(idAlmacen, pageable);
    }

    // ── Mutaciones ───────────────────────────────────────────────────────────────

    @Transactional
    public TrasladoMuestra registrarTraslado(Long idMuestra, Long idAlmacen, String uuidAutoriza,
                                              String motivo, String observaciones) {
        Muestra muestra = muestraRepository.findById(idMuestra)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra con id: " + idMuestra));

        if (trasladoRepository.existsByMuestra_IdAndEstadoIn(idMuestra, ESTADOS_ACTIVOS)) {
            throw new ObjConflictException(
                    "La muestra '" + muestra.getEtiqueta() + "' ya tiene un traslado activo. " +
                    "Registre la devolución antes de crear un nuevo traslado.");
        }

        Almacen almacen = almacenRepository.findById(idAlmacen)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el almacén con id: " + idAlmacen));
        if (!almacen.getActivo()) {
            throw new ObjConflictException(
                    "El almacén '" + almacen.getNombre() + "' está inactivo y no puede recibir traslados.");
        }

        BeanUser autoriza = userRepository.findByUUID(uuidAutoriza)
                .orElseThrow(() -> new ObjNotFoundException(
                        "No se encontró el usuario autorizante con UUID: " + uuidAutoriza));

        TrasladoMuestra traslado = new TrasladoMuestra();
        traslado.setMuestra(muestra);
        traslado.setAlmacen(almacen);
        traslado.setAutorizadoPor(autoriza);
        traslado.setEstado(EstadoTraslado.TRASLADADA);
        traslado.setFechaTraslado(LocalDateTime.now());
        traslado.setMotivo(motivo);
        traslado.setObservaciones(observaciones);
        traslado.setFechaRegistro(Timestamp.from(Instant.now()));

        TrasladoMuestra saved = trasladoRepository.save(traslado);

        enviarEmailTrasladoEnviado(saved);

        return saved;
    }

    @Transactional
    public TrasladoMuestra confirmarRecepcion(Long idTraslado, String uuidEncargado) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.TRASLADADA) {
            throw new ObjConflictException(
                    "Solo se puede confirmar la recepción de traslados en estado TRASLADADA. " +
                    "Estado actual: " + traslado.getEstado());
        }

        // Validar que el encargado pertenece al almacén del traslado
        Almacen almacen = traslado.getAlmacen();
        if (almacen.getEncargado() == null ||
                !almacen.getEncargado().getUUID().equals(uuidEncargado)) {
            throw new ValidationException(
                    "No tienes permiso para confirmar la recepción de este traslado. " +
                    "Solo el encargado asignado al almacén puede hacerlo.");
        }

        traslado.setEstado(EstadoTraslado.RECIBIDA);
        TrasladoMuestra updated = trasladoRepository.save(traslado);

        enviarEmailTrasladoRecibido(updated);

        return updated;
    }

    @Transactional
    public TrasladoMuestra iniciarDevolucion(Long idTraslado, String uuidEncargado, String observaciones) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.RECIBIDA) {
            throw new ObjConflictException(
                    "Solo se puede iniciar la devolución de traslados en estado RECIBIDA. " +
                    "Estado actual: " + traslado.getEstado());
        }

        Almacen almacen = traslado.getAlmacen();
        if (almacen.getEncargado() == null ||
                !almacen.getEncargado().getUUID().equals(uuidEncargado)) {
            throw new ValidationException(
                    "No tienes permiso para iniciar la devolución de este traslado.");
        }

        traslado.setEstado(EstadoTraslado.EN_DEVOLUCION);
        if (observaciones != null && !observaciones.isBlank()) {
            String obs = traslado.getObservaciones();
            traslado.setObservaciones(
                    obs != null ? obs + " | Inicio devolución: " + observaciones : "Inicio devolución: " + observaciones);
        }

        TrasladoMuestra saved = trasladoRepository.save(traslado);
        enviarEmailDevolucionIniciada(saved);
        return saved;
    }

    @Transactional
    public TrasladoMuestra confirmarDevolucion(Long idTraslado, String observaciones) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.EN_DEVOLUCION) {
            throw new ObjConflictException(
                    "Solo se puede confirmar la devolución de traslados en estado EN_DEVOLUCION. " +
                    "Estado actual: " + traslado.getEstado());
        }

        traslado.setEstado(EstadoTraslado.DEVUELTA);
        traslado.setFechaRetorno(LocalDateTime.now());
        if (observaciones != null && !observaciones.isBlank()) {
            String obs = traslado.getObservaciones();
            traslado.setObservaciones(
                    obs != null ? obs + " | Devolución confirmada: " + observaciones : "Devolución confirmada: " + observaciones);
        }

        return trasladoRepository.save(traslado);
    }

    // ── Emails ───────────────────────────────────────────────────────────────────

    private void enviarEmailTrasladoEnviado(TrasladoMuestra t) {
        Almacen almacen = t.getAlmacen();
        if (almacen.getEncargado() == null) return;

        String emailEncargado = almacen.getEncargado().getPersona().getEmail();
        if (emailEncargado == null || emailEncargado.isBlank()) return;

        try {
            String nombreEncargado = almacen.getEncargado().getPersona().getNombre()
                    + " " + almacen.getEncargado().getPersona().getApellidoPaterno();
            String nombreAutoriza = t.getAutorizadoPor().getPersona().getNombre()
                    + " " + t.getAutorizadoPor().getPersona().getApellidoPaterno();

            Context ctx = new Context();
            ctx.setVariable("nombreEncargado", nombreEncargado.trim());
            ctx.setVariable("etiquetaMuestra", t.getMuestra().getEtiqueta());
            ctx.setVariable("nombreAlmacen",   almacen.getNombre());
            ctx.setVariable("nombreAutoriza",  nombreAutoriza.trim());
            ctx.setVariable("fechaTraslado",   t.getFechaTraslado().format(FMT));
            ctx.setVariable("motivo",          t.getMotivo());
            ctx.setVariable("loginUrl",        "");

            emailService.enviar(
                    emailEncargado,
                    "Muestra " + t.getMuestra().getEtiqueta() + " enviada a tu laboratorio",
                    "email/traslado-enviado",
                    ctx
            );
        } catch (Exception e) {
            log.error("Error enviando email de traslado enviado: {}", e.getMessage());
        }
    }

    private void enviarEmailDevolucionIniciada(TrasladoMuestra t) {
        String emailAutoriza = t.getAutorizadoPor().getPersona().getEmail();
        if (emailAutoriza == null || emailAutoriza.isBlank()) return;

        try {
            String nombreAutoriza = t.getAutorizadoPor().getPersona().getNombre()
                    + " " + t.getAutorizadoPor().getPersona().getApellidoPaterno();
            String nombreEncargado = t.getAlmacen().getEncargado() != null
                    ? t.getAlmacen().getEncargado().getPersona().getNombre()
                      + " " + t.getAlmacen().getEncargado().getPersona().getApellidoPaterno()
                    : t.getAlmacen().getNombre();

            Context ctx = new Context();
            ctx.setVariable("nombreAutoriza",  nombreAutoriza.trim());
            ctx.setVariable("etiquetaMuestra", t.getMuestra().getEtiqueta());
            ctx.setVariable("nombreAlmacen",   t.getAlmacen().getNombre());
            ctx.setVariable("nombreEncargado", nombreEncargado.trim());
            ctx.setVariable("fechaDevolucion", LocalDateTime.now().format(FMT));

            emailService.enviar(
                    emailAutoriza,
                    "Muestra " + t.getMuestra().getEtiqueta() + " en proceso de devolución",
                    "email/traslado-devolucion-iniciada",
                    ctx
            );
        } catch (Exception e) {
            log.error("Error enviando email de devolución iniciada: {}", e.getMessage());
        }
    }

    private void enviarEmailTrasladoRecibido(TrasladoMuestra t) {
        String emailAutoriza = t.getAutorizadoPor().getPersona().getEmail();
        if (emailAutoriza == null || emailAutoriza.isBlank()) return;

        try {
            String nombreAutoriza = t.getAutorizadoPor().getPersona().getNombre()
                    + " " + t.getAutorizadoPor().getPersona().getApellidoPaterno();
            String nombreEncargado = t.getAlmacen().getEncargado() != null
                    ? t.getAlmacen().getEncargado().getPersona().getNombre()
                      + " " + t.getAlmacen().getEncargado().getPersona().getApellidoPaterno()
                    : t.getAlmacen().getNombre();

            Context ctx = new Context();
            ctx.setVariable("nombreAutoriza",  nombreAutoriza.trim());
            ctx.setVariable("etiquetaMuestra", t.getMuestra().getEtiqueta());
            ctx.setVariable("nombreAlmacen",   t.getAlmacen().getNombre());
            ctx.setVariable("nombreEncargado", nombreEncargado.trim());
            ctx.setVariable("fechaRecepcion",  LocalDateTime.now().format(FMT));

            emailService.enviar(
                    emailAutoriza,
                    "Muestra " + t.getMuestra().getEtiqueta() + " confirmada como recibida",
                    "email/traslado-recibido",
                    ctx
            );
        } catch (Exception e) {
            log.error("Error enviando email de traslado recibido: {}", e.getMessage());
        }
    }
}
