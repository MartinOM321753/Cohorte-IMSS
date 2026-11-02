package imss.gob.mx.cohorte.services.almacenamiento.entrega;

import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.Colaborador;
import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.ColaboradorRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.entrega.EntregaMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.entrega.EntregaMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.entrega.EstadoEntrega;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.PacienteEstadoValidator;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.services.almacenamiento.caja.PosicionCajaService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.HistorialCambioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PlanificadorAlicuotas;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entrega/préstamo de muestras a un colaborador (persona física). Ver
 * {@link EntregaMuestra} para la semántica: entrega de un paso, la muestra no
 * cambia de institución, y el cierre NUNCA toca el volumen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntregaMuestraService {

    private final EntregaMuestraRepository entregaRepository;
    private final MuestraRepository muestraRepository;
    private final ColaboradorRepository colaboradorRepository;
    private final InstitucionRepository institucionRepository;
    private final UserRepository userRepository;
    private final PosicionCajaService posicionCajaService;
    private final HistorialCambioMuestraService historialService;

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EntregaMuestra getById(Long id) {
        return entregaRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la entrega con id: " + id));
    }

    @Transactional(readOnly = true)
    public List<EntregaMuestra> getActivasByInstitucion(Long idInstitucion) {
        return entregaRepository.findActivasByInstitucion(idInstitucion);
    }

    @Transactional(readOnly = true)
    public Page<EntregaMuestra> getAllByInstitucionPaginado(Long idInstitucion, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaEntrega"));
        return entregaRepository.findAllByInstitucionPaginado(idInstitucion, pageable);
    }

    @Transactional(readOnly = true)
    public List<EntregaMuestra> getByGrupo(String grupoEntrega) {
        return entregaRepository.findAllByGrupoEntregaOrderByFechaEntregaDesc(grupoEntrega);
    }

    @Transactional(readOnly = true)
    public List<EntregaMuestra> getHistorialByMuestra(Long idMuestra) {
        if (!muestraRepository.existsById(idMuestra)) {
            throw new ObjNotFoundException("No se encontró la muestra con id: " + idMuestra);
        }
        return entregaRepository.findHistorialByMuestra(idMuestra);
    }

    // ── Mutaciones ───────────────────────────────────────────────────────────

    /**
     * Entrega una o varias muestras (padre y/o alícuotas) a un colaborador.
     *
     * <p>Reglas: solo la institución tenedora puede entregar; el colaborador debe
     * pertenecer a esa institución y estar activo; ninguna muestra puede estar
     * prestada, entregada, consumida, de baja ni agotada. La posición se libera
     * (y se guarda para poder restaurarla al cancelar). {@code institucionActual}
     * NO cambia.
     */
    @Transactional
    public List<EntregaMuestra> entregar(List<Long> idsMuestras, Long idColaborador, Long idUsuario,
                                         Long idInstitucion, String uuidEntrega,
                                         String motivo, String observaciones,
                                         LocalDateTime fechaLimite) {
        if (idsMuestras == null || idsMuestras.isEmpty()) {
            throw new ValidationException("Debe seleccionar al menos una muestra para entregar.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new ValidationException("El motivo de la entrega es obligatorio.");
        }

        Institucion institucion = institucionRepository.findById(idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException("Institución no encontrada: " + idInstitucion));

        // El destinatario es un colaborador O un usuario del sistema, nunca ambos
        // ni ninguno. Ambos quedan acotados a la institución del que entrega.
        if ((idColaborador == null) == (idUsuario == null)) {
            throw new ValidationException("Indique exactamente un destinatario: un colaborador o un usuario.");
        }

        Colaborador colaborador = null;
        BeanUser usuarioDestino = null;
        String nombreDestino;
        if (idColaborador != null) {
            colaborador = colaboradorRepository.findByIdAndInstitucion_Id(idColaborador, idInstitucion)
                    .orElseThrow(() -> new ObjNotFoundException(
                            "Colaborador no encontrado en su institución: " + idColaborador));
            if (!Boolean.TRUE.equals(colaborador.getActivo())) {
                throw new ObjConflictException("El colaborador '"
                        + nombrePersona(colaborador.getPersona()) + "' está inactivo y no puede recibir muestras.");
            }
            nombreDestino = nombrePersona(colaborador.getPersona());
        } else {
            usuarioDestino = userRepository.findById(idUsuario)
                    .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + idUsuario));
            if (usuarioDestino.getInstitucion() == null
                    || !usuarioDestino.getInstitucion().getId().equals(idInstitucion)) {
                throw new ValidationException("El usuario destino pertenece a otra institución.");
            }
            if (!Boolean.TRUE.equals(usuarioDestino.getActivo())) {
                throw new ObjConflictException("El usuario destino está inactivo y no puede recibir muestras.");
            }
            nombreDestino = nombrePersona(usuarioDestino.getPersona());
        }

        BeanUser entrega = userRepository.findByUUID(uuidEntrega)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuidEntrega));

        String grupoEntrega = idsMuestras.size() > 1 ? UUID.randomUUID().toString() : null;
        List<EntregaMuestra> resultado = new ArrayList<>();

        for (Long idMuestra : idsMuestras) {
            Muestra muestra = muestraRepository.findById(idMuestra)
                    .orElseThrow(() -> new ObjNotFoundException("Muestra no encontrada: " + idMuestra));

            PacienteEstadoValidator.requirePacienteActivo(muestra,
                    "entregar la muestra '" + muestra.getEtiqueta() + "'");

            if (!muestra.getInstitucionActual().getId().equals(idInstitucion)) {
                throw new ValidationException("La muestra '" + muestra.getEtiqueta()
                        + "' no pertenece actualmente a su institución.");
            }
            validarEntregable(muestra);

            Long idPosicionAnterior = null;
            if (muestra.getPosicionCaja() != null) {
                idPosicionAnterior = muestra.getPosicionCaja().getId();
                posicionCajaService.liberarPosicion(idPosicionAnterior);
                muestra.setPosicionCaja(null);
            }

            // institucionActual NO cambia: la muestra sigue siendo de esta institución,
            // solo sale físicamente del biobanco.
            muestra.setEstadoMuestra(EstadoMuestra.ENTREGADA);
            muestraRepository.save(muestra);

            EntregaMuestra entregaMuestra = new EntregaMuestra();
            entregaMuestra.setMuestra(muestra);
            entregaMuestra.setColaborador(colaborador);
            entregaMuestra.setUsuario(usuarioDestino);
            entregaMuestra.setInstitucion(institucion);
            entregaMuestra.setEntregadaPor(entrega);
            entregaMuestra.setEstado(EstadoEntrega.ENTREGADA);
            entregaMuestra.setFechaEntrega(LocalDateTime.now());
            entregaMuestra.setFechaLimite(fechaLimite);
            entregaMuestra.setMotivo(motivo);
            entregaMuestra.setObservaciones(observaciones);
            entregaMuestra.setGrupoEntrega(grupoEntrega);
            entregaMuestra.setIdPosicionCajaAnterior(idPosicionAnterior);
            entregaMuestra.setFechaRegistro(Timestamp.from(Instant.now()));

            EntregaMuestra saved = entregaRepository.save(entregaMuestra);

            if (idPosicionAnterior != null) {
                historialService.registrarEvento(muestra, entrega,
                        TipoEventoMuestra.POSICION_LIBERADA,
                        "PosicionCaja liberada", null,
                        "Entrega a " + nombreDestino, null);
            }
            historialService.registrarEvento(muestra, entrega,
                    TipoEventoMuestra.ENTREGA_REALIZADA,
                    institucion.getNombre(), nombreDestino, motivo, null);

            resultado.add(saved);
        }

        return resultado;
    }

    /**
     * Cierra una entrega como DEVUELTA: la muestra regresó al biobanco. Vuelve a
     * SIN_POSICION (el operador la reubica). El volumen NO se toca.
     */
    @Transactional
    public EntregaMuestra devolver(Long idEntrega, String uuidCierra, String observaciones) {
        EntregaMuestra entrega = requireActiva(idEntrega);
        BeanUser cierra = requireUsuario(uuidCierra);
        Muestra muestra = entrega.getMuestra();

        // Entre entregar y devolver no debe haberse dado de baja; reescribir aquí
        // resucitaría la baja sin dejar rastro.
        if (muestra.getEstadoMuestra() == EstadoMuestra.BAJA) {
            throw new ObjConflictException("La muestra '" + muestra.getEtiqueta()
                    + "' está dada de baja y no puede registrarse como devuelta.");
        }

        muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
        muestra.setFechaActualizacion(Timestamp.from(Instant.now()));
        muestraRepository.save(muestra);

        cerrar(entrega, EstadoEntrega.DEVUELTA, cierra, observaciones, "Devuelta");
        historialService.registrarEvento(muestra, cierra,
                TipoEventoMuestra.ENTREGA_DEVUELTA,
                nombreDestinatario(entrega), entrega.getInstitucion().getNombre(),
                "Devolución confirmada", null);
        return entrega;
    }

    /**
     * Cierra una entrega como CONSUMIDA: la muestra no regresó. Estado terminal.
     *
     * <p>El volumen NO se toca a propósito: la captura de los estudios puede
     * llegar después y necesita volumen. El agotamiento lo sellan esos estudios.
     * La muestra queda {@link EstadoMuestra#CONSUMIDA} (no es una baja).
     */
    @Transactional
    public EntregaMuestra marcarConsumida(Long idEntrega, String uuidCierra, String observaciones) {
        EntregaMuestra entrega = requireActiva(idEntrega);
        BeanUser cierra = requireUsuario(uuidCierra);
        Muestra muestra = entrega.getMuestra();

        if (muestra.getEstadoMuestra() == EstadoMuestra.BAJA) {
            throw new ObjConflictException("La muestra '" + muestra.getEtiqueta()
                    + "' está dada de baja; no puede marcarse como consumida.");
        }

        // No se toca valor ni fechaAgotamiento: ver EntregaMuestra. La posición ya
        // se liberó al entregar.
        muestra.setEstadoMuestra(EstadoMuestra.CONSUMIDA);
        muestra.setFechaActualizacion(Timestamp.from(Instant.now()));
        muestraRepository.save(muestra);

        cerrar(entrega, EstadoEntrega.CONSUMIDA, cierra, observaciones, "Consumida");
        historialService.registrarEvento(muestra, cierra,
                TipoEventoMuestra.ENTREGA_CONSUMIDA,
                nombreDestinatario(entrega), null,
                "Consumida en el uso del colaborador; no regresó. Conserva volumen para "
                        + "capturar estudios pendientes.", null);
        return entrega;
    }

    /**
     * Cancela una entrega (registro corregido): la muestra regresa al biobanco y,
     * si es posible, a su posición anterior.
     */
    @Transactional
    public EntregaMuestra cancelar(Long idEntrega, String uuidUsuario, String motivo) {
        EntregaMuestra entrega = requireActiva(idEntrega);
        BeanUser usuario = requireUsuario(uuidUsuario);
        Muestra muestra = entrega.getMuestra();

        boolean posicionPerdida = restaurarPosicion(entrega, muestra);
        muestra.setFechaActualizacion(Timestamp.from(Instant.now()));
        muestraRepository.save(muestra);

        String nota = motivo;
        if (posicionPerdida) {
            String aviso = "Posición anterior no disponible — muestra queda sin posición";
            nota = (motivo != null && !motivo.isBlank()) ? motivo + " | " + aviso : aviso;
        }
        cerrar(entrega, EstadoEntrega.CANCELADA, usuario, motivo, "Cancelada");
        historialService.registrarEvento(muestra, usuario,
                TipoEventoMuestra.ENTREGA_CANCELADA,
                nombreDestinatario(entrega), entrega.getInstitucion().getNombre(),
                nota, null);
        return entrega;
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private void validarEntregable(Muestra muestra) {
        switch (muestra.getEstadoMuestra()) {
            case PRESTADA -> throw new ObjConflictException("La muestra '" + muestra.getEtiqueta()
                    + "' está en préstamo a otra institución.");
            case ENTREGADA -> throw new ObjConflictException("La muestra '" + muestra.getEtiqueta()
                    + "' ya está entregada a un colaborador.");
            case CONSUMIDA -> throw new ObjConflictException("La muestra '" + muestra.getEtiqueta()
                    + "' está consumida y ya no existe físicamente.");
            case BAJA -> throw new ObjConflictException("La muestra '" + muestra.getEtiqueta()
                    + "' está dada de baja.");
            default -> { /* SIN_POSICION, EN_BIOBANCO: entregable */ }
        }
        if (PlanificadorAlicuotas.agotado(muestra.getValor())) {
            throw new ValidationException("La muestra '" + muestra.getEtiqueta()
                    + "' no tiene volumen y no se puede entregar.");
        }
    }

    private EntregaMuestra requireActiva(Long idEntrega) {
        EntregaMuestra entrega = getById(idEntrega);
        if (entrega.getEstado() != EstadoEntrega.ENTREGADA) {
            throw new ObjConflictException(
                    "Solo se puede cerrar una entrega en estado ENTREGADA. Estado actual: " + entrega.getEstado());
        }
        return entrega;
    }

    private BeanUser requireUsuario(String uuid) {
        return userRepository.findByUUID(uuid)
                .orElseThrow(() -> new ObjNotFoundException("Usuario no encontrado: " + uuid));
    }

    private void cerrar(EntregaMuestra entrega, EstadoEntrega estado, BeanUser usuario,
                        String observaciones, String prefijo) {
        entrega.setEstado(estado);
        entrega.setCerradaPor(usuario);
        entrega.setFechaCierre(LocalDateTime.now());
        if (observaciones != null && !observaciones.isBlank()) {
            String obs = entrega.getObservaciones();
            String nuevo = prefijo + ": " + observaciones;
            entrega.setObservaciones(obs != null ? obs + " | " + nuevo : nuevo);
        }
        entregaRepository.save(entrega);
    }

    /** Restaura la posición anterior al cancelar. Devuelve true si no se pudo recuperar. */
    private boolean restaurarPosicion(EntregaMuestra entrega, Muestra muestra) {
        Long idPosAnterior = entrega.getIdPosicionCajaAnterior();
        if (idPosAnterior == null) {
            muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
            return false;
        }
        try {
            posicionCajaService.ocuparPosicion(idPosAnterior);
            muestra.setPosicionCaja(posicionCajaService.getById(idPosAnterior));
            muestra.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
            return false;
        } catch (ObjConflictException | ObjNotFoundException e) {
            log.warn("Cancelación entrega {}: posición anterior {} no disponible; muestra {} queda SIN_POSICION. {}",
                    entrega.getId(), idPosAnterior, muestra.getEtiqueta(), e.getMessage());
            muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
            return true;
        }
    }

    /** Nombre del destinatario de una entrega, sea colaborador o usuario. */
    private String nombreDestinatario(EntregaMuestra entrega) {
        if (entrega.getColaborador() != null) return nombrePersona(entrega.getColaborador().getPersona());
        if (entrega.getUsuario() != null) return nombrePersona(entrega.getUsuario().getPersona());
        return "destinatario";
    }

    private String nombrePersona(imss.gob.mx.cohorte.modules.persona.Persona p) {
        if (p == null) return "destinatario";
        return (p.getNombre()
                + (p.getSegundoNombre() != null ? " " + p.getSegundoNombre() : "")
                + " " + p.getApellidoPaterno()
                + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "")).trim();
    }
}
