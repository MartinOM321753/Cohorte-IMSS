package imss.gob.mx.cohorte.services.almacenamiento.traslado;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestraRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.services.almacenamiento.caja.PosicionCajaService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.HistorialCambioMuestraService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrasladoMuestraService {

    private final TrasladoMuestraRepository trasladoRepository;
    private final MuestraRepository muestraRepository;
    private final InstitucionRepository institucionRepository;
    private final UserRepository userRepository;
    private final PosicionCajaService posicionCajaService;
    private final HistorialCambioMuestraService historialService;
    private final EmailService emailService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getAll() {
        return trasladoRepository.findAllByOrderByFechaTrasladoDesc();
    }

    @Transactional(readOnly = true)
    public TrasladoMuestra getById(Long id) {
        return trasladoRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el préstamo con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getHistorialByMuestra(Long idMuestra) {
        if (!muestraRepository.existsById(idMuestra)) {
            throw new ObjNotFoundException("No se encontró la muestra con id: " + idMuestra);
        }
        return trasladoRepository.findAllByMuestra_IdOrderByFechaTrasladoDesc(idMuestra);
    }

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getActivosByInstitucion(Long idInstitucion) {
        return trasladoRepository.findActivosByInstitucion(idInstitucion);
    }

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getAllByInstitucion(Long idInstitucion) {
        return trasladoRepository.findAllByInstitucion(idInstitucion);
    }

    @Transactional(readOnly = true)
    public Page<TrasladoMuestra> getAllByInstitucionPaginado(Long idInstitucion, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaTraslado"));
        return trasladoRepository.findAllByInstitucionPaginado(idInstitucion, pageable);
    }

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getByGrupo(String grupoTraslado) {
        return trasladoRepository.findAllByGrupoTrasladoOrderByFechaTrasladoDesc(grupoTraslado);
    }

    // ── Mutaciones ───────────────────────────────────────────────────────────

    /**
     * Inicia un préstamo de una o varias muestras (padre + alícuotas) hacia otra institución.
     *
     * <p>Reglas:
     * <ul>
     *   <li>Solo la institución que actualmente tiene la muestra ({@code institucionActual}) puede prestarla.</li>
     *   <li>La institución destino debe tener {@code tieneBiobanco = true}.</li>
     *   <li>Ninguna de las muestras puede estar ya en estado PRESTADA.</li>
     *   <li>Si se incluyen múltiples muestras, todas comparten el mismo {@code grupoTraslado}.</li>
     * </ul>
     */
    @Transactional
    public List<TrasladoMuestra> iniciarPrestamo(List<Long> idsMuestras,
                                                  Long idInstitucionOrigen,
                                                  Long idInstitucionDestino,
                                                  String uuidAutoriza,
                                                  String motivo,
                                                  String observaciones) {
        Institucion origen = institucionRepository.findById(idInstitucionOrigen)
                .orElseThrow(() -> new ObjNotFoundException("Institución origen no encontrada: " + idInstitucionOrigen));

        Institucion destino = institucionRepository.findById(idInstitucionDestino)
                .orElseThrow(() -> new ObjNotFoundException("Institución destino no encontrada: " + idInstitucionDestino));

        if (!destino.getActivo()) {
            throw new ObjConflictException("La institución destino está inactiva y no puede recibir préstamos.");
        }
        if (!Boolean.TRUE.equals(destino.getTieneBiobanco())) {
            throw new ObjConflictException("La institución destino '" + destino.getNombre()
                    + "' no tiene biobanco habilitado.");
        }
        if (idInstitucionOrigen.equals(idInstitucionDestino)) {
            throw new ObjConflictException("La institución origen y destino no pueden ser la misma.");
        }

        BeanUser autoriza = userRepository.findByUUID(uuidAutoriza)
                .orElseThrow(() -> new ObjNotFoundException("Usuario autorizante no encontrado: " + uuidAutoriza));

        // Generar UUID de grupo si son múltiples muestras
        String grupoTraslado = idsMuestras.size() > 1 ? UUID.randomUUID().toString() : null;

        List<TrasladoMuestra> resultado = new ArrayList<>();

        for (Long idMuestra : idsMuestras) {
            Muestra muestra = muestraRepository.findById(idMuestra)
                    .orElseThrow(() -> new ObjNotFoundException("Muestra no encontrada: " + idMuestra));

            // Solo el tenedor actual puede prestar
            if (!muestra.getInstitucionActual().getId().equals(idInstitucionOrigen)) {
                throw new ValidationException(
                        "La muestra '" + muestra.getEtiqueta()
                        + "' no pertenece actualmente a la institución que intenta prestarla.");
            }

            if (muestra.getEstadoMuestra() == EstadoMuestra.PRESTADA) {
                throw new ObjConflictException(
                        "La muestra '" + muestra.getEtiqueta() + "' ya está en préstamo activo.");
            }
            if (muestra.getEstadoMuestra() == EstadoMuestra.BAJA) {
                throw new ObjConflictException(
                        "La muestra '" + muestra.getEtiqueta() + "' está dada de baja.");
            }

            // Guardar ID de la posición antes de liberarla, para poder restaurarla al cancelar
            Long idPosicionAnterior = null;
            if (muestra.getPosicionCaja() != null) {
                idPosicionAnterior = muestra.getPosicionCaja().getId();
                posicionCajaService.liberarPosicion(idPosicionAnterior);
                muestra.setPosicionCaja(null);
            }

            // Actualizar estado de la muestra
            muestra.setEstadoMuestra(EstadoMuestra.PRESTADA);
            muestra.setInstitucionActual(destino);
            muestraRepository.save(muestra);

            // Crear el registro de traslado
            TrasladoMuestra traslado = new TrasladoMuestra();
            traslado.setMuestra(muestra);
            traslado.setInstitucionOrigen(origen);
            traslado.setInstitucionDestino(destino);
            traslado.setAutorizadoPor(autoriza);
            traslado.setEstado(EstadoTraslado.ENVIADA);
            traslado.setFechaTraslado(LocalDateTime.now());
            traslado.setMotivo(motivo);
            traslado.setObservaciones(observaciones);
            traslado.setGrupoTraslado(grupoTraslado);
            traslado.setFechaRegistro(Timestamp.from(Instant.now()));
            traslado.setIdPosicionCajaAnterior(idPosicionAnterior);

            TrasladoMuestra saved = trasladoRepository.save(traslado);

            // Historial
            historialService.registrarEvento(muestra, autoriza,
                    TipoEventoMuestra.POSICION_LIBERADA,
                    "PosicionCaja liberada", null, "Préstamo iniciado hacia " + destino.getNombre(), saved);
            historialService.registrarEvento(muestra, autoriza,
                    TipoEventoMuestra.PRESTAMO_ENVIADO,
                    origen.getNombre(), destino.getNombre(), motivo, saved);

            resultado.add(saved);
        }

        // Notificación por email al encargado de la institución destino
        if (!resultado.isEmpty()) {
            enviarEmailPrestamoEnviado(resultado.get(0), destino);
        }

        return resultado;
    }

    /**
     * Institución destino confirma la recepción física de la muestra.
     * La muestra pasa a SIN_POSICION en la institución destino (ya es el tenedor actual).
     */
    @Transactional
    public TrasladoMuestra confirmarRecepcion(Long idTraslado, String uuidConfirma) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.ENVIADA) {
            throw new ObjConflictException(
                    "Solo se puede confirmar la recepción de préstamos en estado ENVIADA. "
                    + "Estado actual: " + traslado.getEstado());
        }

        BeanUser confirma = userRepository.findByUUID(uuidConfirma)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuidConfirma));

        // La muestra ya tiene institucionActual = destino (se asignó al iniciar préstamo).
        // Ahora la marcamos como SIN_POSICION (llegó, sin caja aún).
        Muestra muestra = traslado.getMuestra();
        muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
        muestraRepository.save(muestra);

        traslado.setEstado(EstadoTraslado.RECIBIDA);
        traslado.setRecibidoPor(confirma);
        TrasladoMuestra updated = trasladoRepository.save(traslado);

        historialService.registrarEvento(muestra, confirma,
                TipoEventoMuestra.PRESTAMO_RECIBIDO,
                traslado.getInstitucionOrigen().getNombre(),
                traslado.getInstitucionDestino().getNombre(),
                "Recepción confirmada", updated);

        enviarEmailPrestamoRecibido(updated);

        return updated;
    }

    /**
     * Institución destino inicia la devolución de la muestra al tenedor anterior.
     */
    @Transactional
    public TrasladoMuestra iniciarDevolucion(Long idTraslado, String uuidInicia, String observaciones) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.RECIBIDA) {
            throw new ObjConflictException(
                    "Solo se puede iniciar la devolución de préstamos en estado RECIBIDA. "
                    + "Estado actual: " + traslado.getEstado());
        }

        BeanUser inicia = userRepository.findByUUID(uuidInicia)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuidInicia));

        // Liberar posición en destino si la muestra tenía una asignada
        Muestra muestra = traslado.getMuestra();
        if (muestra.getPosicionCaja() != null) {
            posicionCajaService.liberarPosicion(muestra.getPosicionCaja().getId());
            muestra.setPosicionCaja(null);
        }
        muestra.setEstadoMuestra(EstadoMuestra.PRESTADA); // en tránsito de regreso
        // institucionActual vuelve al origen
        muestra.setInstitucionActual(traslado.getInstitucionOrigen());
        muestraRepository.save(muestra);

        traslado.setEstado(EstadoTraslado.EN_DEVOLUCION);
        if (observaciones != null && !observaciones.isBlank()) {
            String obs = traslado.getObservaciones();
            traslado.setObservaciones(
                    obs != null ? obs + " | Devolución: " + observaciones : "Devolución: " + observaciones);
        }

        TrasladoMuestra saved = trasladoRepository.save(traslado);

        enviarEmailDevolucionIniciada(saved, inicia);
        return saved;
    }

    /**
     * Institución origen (anterior tenedor) confirma que recibió la muestra de vuelta.
     * La muestra queda SIN_POSICION en el tenedor anterior para que le asignen caja.
     */
    @Transactional
    public TrasladoMuestra confirmarDevolucion(Long idTraslado, String uuidConfirma, String observaciones) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.EN_DEVOLUCION) {
            throw new ObjConflictException(
                    "Solo se puede confirmar la devolución de préstamos en estado EN_DEVOLUCION. "
                    + "Estado actual: " + traslado.getEstado());
        }

        BeanUser confirma = userRepository.findByUUID(uuidConfirma)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuidConfirma));

        Muestra muestra = traslado.getMuestra();
        muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION); // lista para asignar posición
        // institucionActual ya se actualizó en iniciarDevolucion → es el origen
        muestraRepository.save(muestra);

        traslado.setEstado(EstadoTraslado.DEVUELTA);
        traslado.setFechaRetorno(LocalDateTime.now());
        if (observaciones != null && !observaciones.isBlank()) {
            String obs = traslado.getObservaciones();
            traslado.setObservaciones(
                    obs != null ? obs + " | Devuelta confirmada: " + observaciones : "Devuelta confirmada: " + observaciones);
        }

        TrasladoMuestra saved = trasladoRepository.save(traslado);

        historialService.registrarEvento(muestra, confirma,
                TipoEventoMuestra.PRESTAMO_DEVUELTO,
                traslado.getInstitucionDestino().getNombre(),
                traslado.getInstitucionOrigen().getNombre(),
                "Devolución confirmada", saved);

        return saved;
    }

    /**
     * Cancela un préstamo en estado ENVIADA antes de que el destino confirme recepción.
     * Solo puede ejecutarlo la institución origen (tenedor original).
     * La muestra regresa a SIN_POSICION en la institución origen.
     */
    @Transactional
    public TrasladoMuestra cancelarPrestamo(Long idTraslado, String uuidUsuario, String motivo) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.ENVIADA) {
            throw new ObjConflictException(
                    "Solo se puede cancelar un préstamo en estado ENVIADA. "
                    + "Estado actual: " + traslado.getEstado());
        }

        BeanUser usuario = userRepository.findByUUID(uuidUsuario)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuidUsuario));

        Muestra muestra = traslado.getMuestra();

        // Restaurar la posición original si sigue libre; de lo contrario dejar SIN_POSICION
        Long idPosAnterior = traslado.getIdPosicionCajaAnterior();
        if (idPosAnterior != null) {
            try {
                posicionCajaService.ocuparPosicion(idPosAnterior);
                PosicionCaja posicion = posicionCajaService.getById(idPosAnterior);
                muestra.setPosicionCaja(posicion);
                muestra.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
            } catch (Exception e) {
                // La posición fue eliminada o ya está ocupada; dejar sin posición asignada
                muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
            }
        } else {
            muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
        }

        muestra.setInstitucionActual(traslado.getInstitucionOrigen());
        muestraRepository.save(muestra);

        traslado.setEstado(EstadoTraslado.CANCELADO);
        if (motivo != null && !motivo.isBlank()) {
            String obs = traslado.getObservaciones();
            traslado.setObservaciones(obs != null ? obs + " | Cancelado: " + motivo : "Cancelado: " + motivo);
        }
        TrasladoMuestra saved = trasladoRepository.save(traslado);

        historialService.registrarEvento(muestra, usuario,
                imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra.PRESTAMO_CANCELADO,
                traslado.getInstitucionDestino().getNombre(),
                traslado.getInstitucionOrigen().getNombre(),
                motivo, saved);

        return saved;
    }

    // ── Emails ───────────────────────────────────────────────────────────────

    private void enviarEmailPrestamoEnviado(TrasladoMuestra t, Institucion destino) {
        if (destino.getEncargado() == null) return;
        String email = destino.getEncargado().getPersona() != null
                ? destino.getEncargado().getPersona().getEmail() : null;
        if (email == null || email.isBlank()) return;

        try {
            String nombreEncargado = destino.getEncargado().getPersona().getNombre()
                    + " " + destino.getEncargado().getPersona().getApellidoPaterno();
            String nombreAutoriza = t.getAutorizadoPor().getPersona().getNombre()
                    + " " + t.getAutorizadoPor().getPersona().getApellidoPaterno();

            Context ctx = new Context();
            ctx.setVariable("nombreEncargado", nombreEncargado.trim());
            ctx.setVariable("etiquetaMuestra",  t.getMuestra().getEtiqueta());
            ctx.setVariable("nombreInstitucionOrigen",  t.getInstitucionOrigen().getNombre());
            ctx.setVariable("nombreInstitucionDestino", destino.getNombre());
            ctx.setVariable("nombreAutoriza",   nombreAutoriza.trim());
            ctx.setVariable("fechaTraslado",    t.getFechaTraslado().format(FMT));
            ctx.setVariable("motivo",           t.getMotivo());

            emailService.enviar(email,
                    "Muestra " + t.getMuestra().getEtiqueta() + " enviada a tu institución",
                    "email/traslado-enviado", ctx);
        } catch (Exception e) {
            log.error("Error enviando email préstamo enviado: {}", e.getMessage());
        }
    }

    private void enviarEmailPrestamoRecibido(TrasladoMuestra t) {
        String email = t.getAutorizadoPor().getPersona() != null
                ? t.getAutorizadoPor().getPersona().getEmail() : null;
        if (email == null || email.isBlank()) return;

        try {
            String nombreAutoriza = t.getAutorizadoPor().getPersona().getNombre()
                    + " " + t.getAutorizadoPor().getPersona().getApellidoPaterno();

            Context ctx = new Context();
            ctx.setVariable("nombreAutoriza",           nombreAutoriza.trim());
            ctx.setVariable("etiquetaMuestra",          t.getMuestra().getEtiqueta());
            ctx.setVariable("nombreInstitucionDestino", t.getInstitucionDestino().getNombre());
            ctx.setVariable("fechaRecepcion",           LocalDateTime.now().format(FMT));

            emailService.enviar(email,
                    "Muestra " + t.getMuestra().getEtiqueta() + " confirmada como recibida",
                    "email/traslado-recibido", ctx);
        } catch (Exception e) {
            log.error("Error enviando email préstamo recibido: {}", e.getMessage());
        }
    }

    private void enviarEmailDevolucionIniciada(TrasladoMuestra t, BeanUser inicia) {
        String email = t.getAutorizadoPor().getPersona() != null
                ? t.getAutorizadoPor().getPersona().getEmail() : null;
        if (email == null || email.isBlank()) return;

        try {
            String nombreAutoriza = t.getAutorizadoPor().getPersona().getNombre()
                    + " " + t.getAutorizadoPor().getPersona().getApellidoPaterno();
            String nombreInicia = inicia.getPersona() != null
                    ? inicia.getPersona().getNombre() + " " + inicia.getPersona().getApellidoPaterno()
                    : inicia.getUsername();

            Context ctx = new Context();
            ctx.setVariable("nombreAutoriza",           nombreAutoriza.trim());
            ctx.setVariable("etiquetaMuestra",          t.getMuestra().getEtiqueta());
            ctx.setVariable("nombreInstitucionDestino", t.getInstitucionDestino().getNombre());
            ctx.setVariable("nombreInicia",             nombreInicia.trim());
            ctx.setVariable("fechaDevolucion",          LocalDateTime.now().format(FMT));

            emailService.enviar(email,
                    "Muestra " + t.getMuestra().getEtiqueta() + " en proceso de devolución",
                    "email/traslado-devolucion-iniciada", ctx);
        } catch (Exception e) {
            log.error("Error enviando email devolución iniciada: {}", e.getMessage());
        }
    }
}
