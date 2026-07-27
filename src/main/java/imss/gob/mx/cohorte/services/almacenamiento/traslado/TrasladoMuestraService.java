package imss.gob.mx.cohorte.services.almacenamiento.traslado;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.PacienteEstadoValidator;
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
                                                  String observaciones,
                                                  LocalDateTime fechaLimite) {
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

            // Cuarentena: bloquear si el participante está inactivo
            PacienteEstadoValidator.requirePacienteActivo(muestra, "iniciar préstamo de la muestra '" + muestra.getEtiqueta() + "'");

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
            traslado.setFechaLimite(fechaLimite);
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
    public TrasladoMuestra confirmarRecepcion(Long idTraslado, String uuidConfirma, Long idPosicionCaja) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.ENVIADA) {
            throw new ObjConflictException(
                    "Solo se puede confirmar la recepción de préstamos en estado ENVIADA. "
                    + "Estado actual: " + traslado.getEstado());
        }

        BeanUser confirma = userRepository.findByUUID(uuidConfirma)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuidConfirma));

        Muestra muestra = traslado.getMuestra();

        if (idPosicionCaja != null) {
            PosicionCaja posicion = posicionCajaService.getById(idPosicionCaja);
            imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica caja = posicion.getCaja();
            if (caja == null) {
                throw new ObjConflictException("La posición no está asignada a ninguna caja.");
            }
            if (!Boolean.TRUE.equals(caja.getActivo())) {
                throw new ObjConflictException(
                        "La caja '" + caja.getCodigoCaja() + "' está desactivada; no se puede recibir la muestra ahí.");
            }
            Institucion instPos = caja.getInstitucion();
            if (!instPos.getId().equals(traslado.getInstitucionDestino().getId())) {
                throw new ValidationException(
                        "La posición seleccionada pertenece a otra institución.");
            }
            if (posicion.getOcupada()) {
                throw new ObjConflictException("La posición de caja seleccionada ya está ocupada.");
            }
            posicionCajaService.ocuparPosicion(idPosicionCaja);
            muestra.setPosicionCaja(posicion);
            muestra.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
        } else {
            muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
        }
        muestraRepository.save(muestra);

        traslado.setEstado(EstadoTraslado.RECIBIDA);
        traslado.setRecibidoPor(confirma);
        TrasladoMuestra updated = trasladoRepository.save(traslado);

        String notaRecepcion = idPosicionCaja != null
                ? "Recepción confirmada con posición asignada"
                : "Recepción confirmada SIN posición asignada — pendiente ubicar en biobanco";
        historialService.registrarEvento(muestra, confirma,
                TipoEventoMuestra.PRESTAMO_RECIBIDO,
                traslado.getInstitucionOrigen().getNombre(),
                traslado.getInstitucionDestino().getNombre(),
                notaRecepcion, updated);
        if (idPosicionCaja == null) {
            log.warn("Traslado {}: recepción confirmada sin posición para muestra {}; "
                    + "queda SIN_POSICION hasta que un operador la ubique.",
                    updated.getId(), muestra.getEtiqueta());
        }

        enviarEmailPrestamoRecibido(updated);

        return updated;
    }

    /**
     * Institución destino inicia la devolución de la muestra al tenedor anterior.
     */
    @Transactional
    public List<TrasladoMuestra> iniciarDevolucion(Long idTraslado, String uuidInicia,
                                                    String observaciones, List<Long> idsAlicuotasDevolver,
                                                    Long idInstitucionDestinoDevolucion) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.RECIBIDA) {
            throw new ObjConflictException(
                    "Solo se puede iniciar la devolución de préstamos en estado RECIBIDA. "
                    + "Estado actual: " + traslado.getEstado());
        }

        BeanUser inicia = userRepository.findByUUID(uuidInicia)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuidInicia));

        Institucion instOrigen = traslado.getInstitucionOrigen();
        Institucion instDestino = traslado.getInstitucionDestino();

        // Guard anti-corrupción: si la muestra ya fue re-prestada a otra institución,
        // devolver este traslado antiguo corromperia el estado (haría "aparecer" la
        // muestra en origen cuando físicamente está en otro tenedor).
        Muestra muestra = traslado.getMuestra();
        if (!muestra.getInstitucionActual().getId().equals(instDestino.getId())) {
            throw new ObjConflictException(
                    "Esta muestra ya fue re-prestada a otra institución. "
                    + "Debe devolverse desde su tenedor actual.");
        }

        // Atajo opcional: devolver directo a una institución previa en la cadena
        // (dueña original o cualquier tenedor anterior). Si viene null, se usa
        // el eslabón anterior (institucionOrigen del traslado que se está devolviendo).
        Institucion destinoDevolucion = instOrigen;
        if (idInstitucionDestinoDevolucion != null
                && !idInstitucionDestinoDevolucion.equals(instOrigen.getId())) {
            if (idInstitucionDestinoDevolucion.equals(instDestino.getId())) {
                throw new ObjConflictException(
                        "El destino de la devolución no puede ser la misma institución que la tiene.");
            }
            if (!institucionParticipoEnCadena(muestra, idInstitucionDestinoDevolucion)) {
                throw new ObjConflictException(
                        "La institución destino de la devolución debe haber participado "
                        + "previamente en la cadena de custodia de esta muestra.");
            }
            destinoDevolucion = institucionRepository.findById(idInstitucionDestinoDevolucion)
                    .orElseThrow(() -> new ObjNotFoundException(
                            "Institución destino de devolución no encontrada: " + idInstitucionDestinoDevolucion));
        }

        // Liberar posición en destino si la muestra padre tenía una asignada
        if (muestra.getPosicionCaja() != null) {
            posicionCajaService.liberarPosicion(muestra.getPosicionCaja().getId());
            muestra.setPosicionCaja(null);
        }
        muestra.setEstadoMuestra(EstadoMuestra.PRESTADA);
        // institucionActual NO cambia aquí: la muestra sigue físicamente en destino
        // hasta que el origen confirme la devolución (confirmarDevolucion)
        muestraRepository.save(muestra);

        traslado.setEstado(EstadoTraslado.EN_DEVOLUCION);
        // Persistir el atajo si aplica (solo si el destino difiere del institucionOrigen).
        if (!destinoDevolucion.getId().equals(instOrigen.getId())) {
            traslado.setIdInstitucionDestinoDevolucion(destinoDevolucion.getId());
        }
        // Marcar el traslado padre y las alícuotas asociadas con el mismo grupoDevolucion
        // para poder confirmarlos en lote de forma determinística en confirmarDevolucion.
        // Preservamos el grupoTraslado original del préstamo saliente (histórico).
        String grupoDevolucion = UUID.randomUUID().toString();
        traslado.setGrupoDevolucion(grupoDevolucion);
        if (observaciones != null && !observaciones.isBlank()) {
            String obs = traslado.getObservaciones();
            traslado.setObservaciones(
                    obs != null ? obs + " | Devolución: " + observaciones : "Devolución: " + observaciones);
        }

        List<TrasladoMuestra> resultado = new ArrayList<>();
        resultado.add(trasladoRepository.save(traslado));

        // Devolver alícuotas seleccionadas junto con la padre
        if (idsAlicuotasDevolver != null && !idsAlicuotasDevolver.isEmpty()) {
            for (Long idAlicuota : idsAlicuotasDevolver) {
                Muestra alicuota = muestraRepository.findById(idAlicuota)
                        .orElseThrow(() -> new ObjNotFoundException("Alícuota no encontrada: " + idAlicuota));

                if (alicuota.getMuestraPadre() == null
                        || !alicuota.getMuestraPadre().getId().equals(muestra.getId())) {
                    throw new ValidationException(
                            "La muestra '" + alicuota.getEtiqueta()
                            + "' no es alícuota de la muestra padre del traslado.");
                }
                if (!alicuota.getInstitucionActual().getId().equals(instDestino.getId())) {
                    throw new ValidationException(
                            "La alícuota '" + alicuota.getEtiqueta()
                            + "' no se encuentra en la institución destino del traslado.");
                }

                if (alicuota.getPosicionCaja() != null) {
                    posicionCajaService.liberarPosicion(alicuota.getPosicionCaja().getId());
                    alicuota.setPosicionCaja(null);
                }
                alicuota.setEstadoMuestra(EstadoMuestra.PRESTADA);
                // institucionActual NO cambia — sigue en destino hasta confirmarDevolucion
                muestraRepository.save(alicuota);

                TrasladoMuestra trasladoAlicuota = new TrasladoMuestra();
                trasladoAlicuota.setMuestra(alicuota);
                trasladoAlicuota.setInstitucionOrigen(instDestino);
                trasladoAlicuota.setInstitucionDestino(destinoDevolucion);
                trasladoAlicuota.setAutorizadoPor(inicia);
                trasladoAlicuota.setEstado(EstadoTraslado.EN_DEVOLUCION);
                trasladoAlicuota.setFechaTraslado(LocalDateTime.now());
                trasladoAlicuota.setMotivo("Devolución junto con muestra padre " + muestra.getEtiqueta());
                trasladoAlicuota.setGrupoDevolucion(grupoDevolucion);
                trasladoAlicuota.setFechaRegistro(Timestamp.from(Instant.now()));

                resultado.add(trasladoRepository.save(trasladoAlicuota));

                historialService.registrarEvento(alicuota, inicia,
                        TipoEventoMuestra.PRESTAMO_ENVIADO,
                        instDestino.getNombre(), destinoDevolucion.getNombre(),
                        "Devolución junto con muestra padre", trasladoAlicuota);
            }
        }

        enviarEmailDevolucionIniciada(traslado, inicia);
        return resultado;
    }

    /**
     * Institución origen (anterior tenedor) confirma que recibió la muestra de vuelta.
     * Ahora es cuando {@code institucionActual} cambia al origen (la muestra llegó físicamente).
     * Si el traslado tiene alícuotas en el mismo grupo de devolución, se confirman todas.
     */
    @Transactional
    public List<TrasladoMuestra> confirmarDevolucion(Long idTraslado, String uuidConfirma, String observaciones) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.EN_DEVOLUCION) {
            throw new ObjConflictException(
                    "Solo se puede confirmar la devolución de préstamos en estado EN_DEVOLUCION. "
                    + "Estado actual: " + traslado.getEstado());
        }

        BeanUser confirma = userRepository.findByUUID(uuidConfirma)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuidConfirma));

        List<TrasladoMuestra> porConfirmar = new ArrayList<>();
        porConfirmar.add(traslado);

        // Agrupar por grupoDevolucion (fuente única de verdad: se popula en
        // iniciarDevolucion tanto en el padre como en las alícuotas asociadas).
        // Esto reemplaza el heurístico frágil anterior que dependía de
        // grupoTraslado + iteración lazy sobre muestraPadre.getAlicuotas().
        String grupoDev = traslado.getGrupoDevolucion();
        if (grupoDev != null) {
            List<TrasladoMuestra> hermanos = trasladoRepository
                    .findAllByGrupoDevolucionOrderByFechaTrasladoDesc(grupoDev);
            for (TrasladoMuestra h : hermanos) {
                if (!h.getId().equals(traslado.getId()) && h.getEstado() == EstadoTraslado.EN_DEVOLUCION) {
                    porConfirmar.add(h);
                }
            }
        }

        List<TrasladoMuestra> resultado = new ArrayList<>();
        for (TrasladoMuestra t : porConfirmar) {
            resultado.add(confirmarDevolucionIndividual(t, confirma, observaciones));
        }

        return resultado;
    }

    private TrasladoMuestra confirmarDevolucionIndividual(TrasladoMuestra traslado,
                                                          BeanUser confirma, String observaciones) {
        Muestra muestra = traslado.getMuestra();

        // Defensa en profundidad: entre iniciarDevolucion y confirmarDevolucion
        // no debe haberse re-prestado esta muestra a otro tenedor.
        if (!muestra.getInstitucionActual().getId().equals(traslado.getInstitucionDestino().getId())) {
            throw new ObjConflictException(
                    "La muestra '" + muestra.getEtiqueta() + "' ya no se encuentra en "
                    + traslado.getInstitucionDestino().getNombre()
                    + "; no se puede confirmar la devolución de este traslado.");
        }

        // Determinar destino real: si el traslado padre tiene atajo, usarlo;
        // si no, el eslabón anterior (comportamiento estándar).
        Institucion destinoFinal = resolverDestinoDevolucion(traslado);

        muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
        muestra.setInstitucionActual(destinoFinal);
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
                destinoFinal.getNombre(),
                "Devolución confirmada", saved);

        return saved;
    }

    /**
     * Devuelve la institución que efectivamente recibe la muestra al confirmar la devolución.
     * Si el traslado tiene un atajo en la cadena de custodia (idInstitucionDestinoDevolucion),
     * se usa ese destino; en caso contrario, el eslabón anterior (institucionOrigen).
     */
    private Institucion resolverDestinoDevolucion(TrasladoMuestra traslado) {
        Long idAtajo = traslado.getIdInstitucionDestinoDevolucion();
        if (idAtajo != null) {
            return institucionRepository.findById(idAtajo)
                    .orElseThrow(() -> new ObjNotFoundException(
                            "Institución destino de devolución no encontrada: " + idAtajo));
        }
        return traslado.getInstitucionOrigen();
    }

    /**
     * Verifica si una institución participó previamente en la cadena de custodia
     * de una muestra (es la dueña original o aparece como origen/destino de algún
     * traslado histórico distinto al que se está devolviendo).
     */
    private boolean institucionParticipoEnCadena(Muestra muestra, Long idInstitucion) {
        if (muestra.getInstitucion() != null && idInstitucion.equals(muestra.getInstitucion().getId())) {
            return true;
        }
        List<TrasladoMuestra> historial = trasladoRepository
                .findAllByMuestra_IdOrderByFechaTrasladoDesc(muestra.getId());
        for (TrasladoMuestra t : historial) {
            if (idInstitucion.equals(t.getInstitucionOrigen().getId())
                    || idInstitucion.equals(t.getInstitucionDestino().getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancela un préstamo en estado ENVIADA antes de que el destino confirme recepción.
     * Si el traslado pertenece a un grupo (padre + alícuotas), cancela todos los
     * hermanos en ENVIADA atómicamente para evitar grupos en estado inconsistente.
     */
    @Transactional
    public List<TrasladoMuestra> cancelarPrestamo(Long idTraslado, String uuidUsuario, String motivo) {
        TrasladoMuestra traslado = getById(idTraslado);

        if (traslado.getEstado() != EstadoTraslado.ENVIADA) {
            throw new ObjConflictException(
                    "Solo se puede cancelar un préstamo en estado ENVIADA. "
                    + "Estado actual: " + traslado.getEstado());
        }

        BeanUser usuario = userRepository.findByUUID(uuidUsuario)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuidUsuario));

        List<TrasladoMuestra> porCancelar = new ArrayList<>();
        porCancelar.add(traslado);

        if (traslado.getGrupoTraslado() != null) {
            List<TrasladoMuestra> hermanos = trasladoRepository
                    .findAllByGrupoTrasladoOrderByFechaTrasladoDesc(traslado.getGrupoTraslado());
            for (TrasladoMuestra h : hermanos) {
                if (!h.getId().equals(traslado.getId()) && h.getEstado() == EstadoTraslado.ENVIADA) {
                    porCancelar.add(h);
                }
            }
        }

        List<TrasladoMuestra> resultado = new ArrayList<>();
        for (TrasladoMuestra t : porCancelar) {
            resultado.add(cancelarTraslado(t, usuario, motivo));
        }

        if (!resultado.isEmpty()) {
            enviarEmailPrestamoCancelado(resultado.get(0), usuario, motivo);
        }

        return resultado;
    }

    private TrasladoMuestra cancelarTraslado(TrasladoMuestra traslado, BeanUser usuario, String motivo) {
        Muestra muestra = traslado.getMuestra();

        boolean posicionAnteriorPerdida = false;
        Long idPosAnterior = traslado.getIdPosicionCajaAnterior();
        if (idPosAnterior != null) {
            try {
                posicionCajaService.ocuparPosicion(idPosAnterior);
                PosicionCaja posicion = posicionCajaService.getById(idPosAnterior);
                muestra.setPosicionCaja(posicion);
                muestra.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
            } catch (ObjConflictException e) {
                log.warn("Cancelación traslado {}: posición anterior {} ya ocupada; "
                        + "muestra {} queda SIN_POSICION. Detalle: {}",
                        traslado.getId(), idPosAnterior, muestra.getEtiqueta(), e.getMessage());
                posicionAnteriorPerdida = true;
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

        String motivoHistorial = motivo;
        if (posicionAnteriorPerdida) {
            String notaPosicion = "Posición anterior ya ocupada — muestra queda sin posición";
            motivoHistorial = (motivo != null && !motivo.isBlank())
                    ? motivo + " | " + notaPosicion
                    : notaPosicion;
        }

        historialService.registrarEvento(muestra, usuario,
                TipoEventoMuestra.PRESTAMO_CANCELADO,
                traslado.getInstitucionDestino().getNombre(),
                traslado.getInstitucionOrigen().getNombre(),
                motivoHistorial, saved);

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

    private void enviarEmailPrestamoCancelado(TrasladoMuestra t, BeanUser cancela, String motivoCancelacion) {
        Institucion destino = t.getInstitucionDestino();
        if (destino.getEncargado() == null) return;
        String email = destino.getEncargado().getPersona() != null
                ? destino.getEncargado().getPersona().getEmail() : null;
        if (email == null || email.isBlank()) return;

        try {
            String nombreEncargado = destino.getEncargado().getPersona().getNombre()
                    + " " + destino.getEncargado().getPersona().getApellidoPaterno();
            String nombreCancela = cancela.getPersona() != null
                    ? cancela.getPersona().getNombre() + " " + cancela.getPersona().getApellidoPaterno()
                    : cancela.getUsername();

            Context ctx = new Context();
            ctx.setVariable("nombreEncargado",          nombreEncargado.trim());
            ctx.setVariable("etiquetaMuestra",          t.getMuestra().getEtiqueta());
            ctx.setVariable("nombreInstitucionOrigen",  t.getInstitucionOrigen().getNombre());
            ctx.setVariable("nombreCancela",            nombreCancela.trim());
            ctx.setVariable("fechaCancelacion",         LocalDateTime.now().format(FMT));
            ctx.setVariable("motivo",                   motivoCancelacion != null && !motivoCancelacion.isBlank()
                    ? motivoCancelacion : "Sin motivo especificado");

            emailService.enviar(email,
                    "Préstamo cancelado — Muestra " + t.getMuestra().getEtiqueta(),
                    "email/traslado-cancelado", ctx);
        } catch (Exception e) {
            log.error("Error enviando email préstamo cancelado: {}", e.getMessage());
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
