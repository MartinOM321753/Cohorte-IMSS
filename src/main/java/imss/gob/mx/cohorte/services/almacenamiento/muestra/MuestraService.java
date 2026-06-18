package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.EstudioMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucionRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestraRepository;
import imss.gob.mx.cohorte.modules.documentos.MuestraDocumentoRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCajaRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MuestraService {

    private final MuestraRepository muestraRepository;
    private final PacienteRepository pacienteRepository;
    private final UserRepository userRepository;
    private final PosicionCajaRepository posicionCajaRepository;
    private final InstitucionContextService institucionContextService;
    private final HistorialCambioMuestraRepository historialCambioMuestraRepository;
    private final EstudioMuestraRepository estudioMuestraRepository;
    private final MuestraDocumentoRepository muestraDocumentoRepository;
    private final MuestraTipoInstitucionRepository muestraTipoInstitucionRepository;
    private final TrasladoMuestraRepository trasladoMuestraRepository;

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Muestra> getAll() {
        return muestraRepository.findAllByInstitucion_Id(institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public Page<Muestra> getAllPaginado(Pageable pageable) {
        return muestraRepository.findAllByInstitucion_Id(institucionContextService.getIdInstitucionActual(), pageable);
    }

    /** Muestras cuyo tenedor actual es la institución del usuario logueado (biobanco propio). */
    @Transactional(readOnly = true)
    public List<Muestra> getAllEnBiobanco() {
        return muestraRepository.findAllByInstitucionActual_Id(institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public Page<Muestra> getAllEnBiobancoPage(Pageable pageable) {
        return muestraRepository.findAllByInstitucionActual_Id(institucionContextService.getIdInstitucionActual(), pageable);
    }

    @Transactional(readOnly = true)
    public long countByPacienteUuid(String uuid) {
        return muestraRepository.countByPaciente_UuidAndInstitucion_Id(uuid, institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public List<Muestra> getAllVisibles() {
        return muestraRepository.findAllVisiblesPorInstitucion(institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public Muestra getById(Long id) {
        Muestra muestra = muestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));
        institucionContextService.verificarPertenece(muestra.getInstitucion());
        return muestra;
    }

    /**
     * Obtiene una muestra validando que el tenedor actual (institucionActual) sea
     * la institución del usuario. Usar para operaciones donde la institución receptora
     * necesita manipular muestras que no le pertenecen originalmente (asignar posición,
     * generar alícuotas, etc.).
     */
    @Transactional(readOnly = true)
    public Muestra getByIdComoTenedor(Long id) {
        Muestra muestra = muestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));
        institucionContextService.verificarPertenece(muestra.getInstitucionActual());
        return muestra;
    }

    /**
     * Obtiene una muestra si la institución del usuario es propietaria O tenedora actual.
     * Usar para lectura de datos compartidos (historial, estudios) en contexto de préstamos.
     */
    @Transactional(readOnly = true)
    public Muestra getByIdConAcceso(Long id) {
        Muestra muestra = muestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));
        Long idInst = institucionContextService.getIdInstitucionActual();
        boolean esPropietaria = muestra.getInstitucion().getId().equals(idInst);
        boolean esTenedora = muestra.getInstitucionActual().getId().equals(idInst);
        if (!esPropietaria && !esTenedora) {
            throw new ObjConflictException("No tiene acceso a esta muestra.");
        }
        return muestra;
    }

    /** Obtiene las alícuotas de una muestra padre. */
    @Transactional(readOnly = true)
    public List<Muestra> getAlicuotas(Long idMuestraPadre) {
        Muestra padre = getById(idMuestraPadre);
        return padre.getAlicuotas();
    }

    // ── Mutaciones ───────────────────────────────────────────────────────────

    @Transactional
    public Muestra create(Muestra muestra) {
        Long idInst = institucionContextService.getIdInstitucionActual();
        if (muestraRepository.findByEtiquetaIgnoreCaseAndInstitucion_Id(muestra.getEtiqueta(), idInst).isPresent()) {
            throw new ObjConflictException("La etiqueta de la muestra ya existe");
        }

        Paciente paciente = pacienteRepository.findById(muestra.getPaciente().getId())
                .orElseThrow(() -> new ObjNotFoundException("El participante no existe"));
        muestra.setPaciente(paciente);

        BeanUser usuarioRecolecta = userRepository.findById(muestra.getUsuarioRecolecta().getId())
                .orElseThrow(() -> new ObjNotFoundException("El usuario que recolecta no existe"));
        muestra.setUsuarioRecolecta(usuarioRecolecta);

        if (muestra.getPosicionCaja() != null && muestra.getPosicionCaja().getId() != null) {
            PosicionCaja posicionCaja = posicionCajaRepository.findById(muestra.getPosicionCaja().getId())
                    .orElseThrow(() -> new ObjNotFoundException("La posición de caja especificada no existe"));
            muestra.setPosicionCaja(posicionCaja);
        } else {
            muestra.setPosicionCaja(null);
        }

        // estadoMuestra inicial según tenga o no posición asignada
        if (muestra.getPosicionCaja() != null) {
            muestra.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
        } else {
            muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
        }

        // institucionActual = la propietaria al registrar (puede cambiar con préstamos)
        if (muestra.getInstitucionActual() == null) {
            muestra.setInstitucionActual(muestra.getInstitucion());
        }

        muestra.setFechaRegistro(Timestamp.valueOf(LocalDateTime.now()));
        muestra.setFechaActualizacion(null);

        return muestraRepository.save(muestra);
    }

    /**
     * Persiste una alícuota hija sin validar posición de caja
     * (posición es null → asignación diferida por el usuario).
     */
    @Transactional
    public Muestra createAlicuota(Muestra alicuota) {
        Long idInst = institucionContextService.getIdInstitucionActual();
        if (muestraRepository.findByEtiquetaIgnoreCaseAndInstitucion_Id(alicuota.getEtiqueta(), idInst).isPresent()) {
            alicuota.setEtiqueta(alicuota.getEtiqueta() + "-dup");
        }
        alicuota.setPosicionCaja(null);
        alicuota.setEstadoMuestra(EstadoMuestra.SIN_POSICION);

        // institucionActual = misma que la padre
        if (alicuota.getInstitucionActual() == null) {
            alicuota.setInstitucionActual(alicuota.getInstitucion());
        }

        return muestraRepository.save(alicuota);
    }

    @Transactional
    public Muestra update(Long id, Double valor, String unidad,
                          LocalDateTime fechaRecoleccion, String observaciones,
                          Long idPosicionCaja) {
        Muestra muestraBD = muestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));
        institucionContextService.verificarPertenece(muestraBD.getInstitucion());

        if (muestraBD.getEstadoMuestra() == EstadoMuestra.PRESTADA) {
            throw new ObjConflictException(
                    "La muestra está en tránsito hacia otra institución. No se puede editar mientras esté prestada.");
        }

        // Gestión de posición en caja
        Long idPosActual = muestraBD.getPosicionCaja() != null ? muestraBD.getPosicionCaja().getId() : null;

        if (idPosicionCaja != null && !idPosicionCaja.equals(idPosActual)) {
            PosicionCaja nuevaPos = posicionCajaRepository.findById(idPosicionCaja)
                    .orElseThrow(() -> new ObjNotFoundException("La posición de caja especificada no existe"));
            if (nuevaPos.getOcupada()) {
                throw new ObjConflictException("La posición de caja destino ya está ocupada");
            }
            if (idPosActual != null) {
                PosicionCaja posAnterior = posicionCajaRepository.findById(idPosActual).orElse(null);
                if (posAnterior != null) { posAnterior.setOcupada(false); posicionCajaRepository.save(posAnterior); }
            }
            nuevaPos.setOcupada(true);
            posicionCajaRepository.save(nuevaPos);
            muestraBD.setPosicionCaja(nuevaPos);
            muestraBD.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
        } else if (idPosicionCaja == null && idPosActual != null) {
            PosicionCaja posAnterior = posicionCajaRepository.findById(idPosActual).orElse(null);
            if (posAnterior != null) { posAnterior.setOcupada(false); posicionCajaRepository.save(posAnterior); }
            muestraBD.setPosicionCaja(null);
            if (muestraBD.getEstadoMuestra() == EstadoMuestra.EN_BIOBANCO) {
                muestraBD.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
            }
        }

        muestraBD.setValor(valor);
        muestraBD.setUnidad(unidad);
        muestraBD.setFechaRecoleccion(fechaRecoleccion);
        muestraBD.setObservaciones(observaciones);
        muestraBD.setFechaActualizacion(Timestamp.valueOf(LocalDateTime.now()));

        return muestraRepository.save(muestraBD);
    }

    @Transactional
    public void delete(Long id) {
        Muestra muestra = muestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));
        institucionContextService.verificarPertenece(muestra.getInstitucion());

        if (muestra.getEstadoMuestra() == EstadoMuestra.PRESTADA) {
            throw new ObjConflictException("No se puede eliminar una muestra en tránsito hacia otra institución.");
        }

        // Verificar que no haya traslados activos (ENVIADA, RECIBIDA, EN_DEVOLUCION)
        if (trasladoMuestraRepository.existsTrasladoActivoByMuestra(id)) {
            throw new ObjConflictException(
                    "No se puede eliminar la muestra porque tiene préstamos activos (enviados, recibidos o en devolución).");
        }

        Long idInst = institucionContextService.getIdInstitucionActual();
        List<Muestra> alicuotas = muestra.getAlicuotas();
        if (alicuotas != null) {
            for (Muestra alicuota : alicuotas) {
                // No permitir eliminar si alguna alícuota está prestada
                if (alicuota.getEstadoMuestra() == EstadoMuestra.PRESTADA) {
                    throw new ObjConflictException(
                            "No se puede eliminar la muestra porque la alícuota '"
                                    + alicuota.getEtiqueta() + "' está en préstamo activo.");
                }
                // No permitir eliminar si alguna alícuota tiene traslados activos
                if (trasladoMuestraRepository.existsTrasladoActivoByMuestra(alicuota.getId())) {
                    throw new ObjConflictException(
                            "No se puede eliminar la muestra porque la alícuota '"
                                    + alicuota.getEtiqueta() + "' tiene préstamos activos.");
                }
                // No permitir eliminar si alguna alícuota pertenece a otra institución
                if (!alicuota.getInstitucion().getId().equals(idInst)) {
                    throw new ObjConflictException(
                            "No se puede eliminar la muestra porque la alícuota '"
                                    + alicuota.getEtiqueta() + "' pertenece a la institución '"
                                    + alicuota.getInstitucion().getNombre() + "'.");
                }
                if (alicuota.getPosicionCaja() != null) {
                    throw new ObjConflictException(
                            "No se puede eliminar la muestra porque la alícuota '"
                                    + alicuota.getEtiqueta() + "' tiene posición asignada. "
                                    + "Libere todas las posiciones de las alícuotas antes de eliminar.");
                }
            }
        }

        // Eliminar dependencias de cada alícuota y luego las alícuotas mismas
        if (alicuotas != null && !alicuotas.isEmpty()) {
            for (Muestra alicuota : alicuotas) {
                eliminarDependencias(alicuota.getId());
            }
            muestraRepository.deleteAll(alicuotas);
        }

        // Eliminar dependencias de la muestra padre
        eliminarDependencias(muestra.getId());

        // Liberar posición de la muestra padre si la tiene
        if (muestra.getPosicionCaja() != null) {
            PosicionCaja pos = posicionCajaRepository.findById(muestra.getPosicionCaja().getId()).orElse(null);
            if (pos != null) {
                pos.setOcupada(false);
                posicionCajaRepository.save(pos);
            }
        }

        muestraRepository.delete(muestra);
    }

    private void eliminarDependencias(Long idMuestra) {
        historialCambioMuestraRepository.deleteAllByMuestra_Id(idMuestra);
        estudioMuestraRepository.deleteAllByMuestra_Id(idMuestra);
        muestraDocumentoRepository.deleteAllByMuestra_Id(idMuestra);
        muestraTipoInstitucionRepository.deleteAllByMuestra_Id(idMuestra);
        trasladoMuestraRepository.deleteAllByMuestra_Id(idMuestra);
    }

    /**
     * Asigna o mueve una muestra a una nueva posición dentro del biobanco.
     * Valida que la posición pertenezca a la misma institución que la muestra.
     */
    @Transactional
    public Muestra asignarPosicion(Long idMuestra, Long idPosicionCaja, String motivo) {
        Muestra muestra = muestraRepository.findById(idMuestra)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));

        if (muestra.getEstadoMuestra() == EstadoMuestra.PRESTADA) {
            throw new ObjConflictException("No se puede asignar posición a una muestra prestada a otra institución.");
        }
        if (muestra.getEstadoMuestra() == EstadoMuestra.BAJA) {
            throw new ObjConflictException("La muestra está dada de baja.");
        }

        PosicionCaja nuevaPos = posicionCajaRepository.findById(idPosicionCaja)
                .orElseThrow(() -> new ObjNotFoundException("Posición de caja no encontrada: " + idPosicionCaja));

        // Validar que la posición pertenece al biobanco de la institucionActual
        Institucion instPos = nuevaPos.getCaja().getInstitucion();
        if (!instPos.getId().equals(muestra.getInstitucionActual().getId())) {
            throw new ValidationException(
                    "La posición seleccionada pertenece a una institución diferente a la que tiene la muestra actualmente.");
        }

        if (nuevaPos.getOcupada()) {
            throw new ObjConflictException("La posición de caja seleccionada ya está ocupada.");
        }

        // Liberar posición anterior
        if (muestra.getPosicionCaja() != null) {
            PosicionCaja posAnterior = posicionCajaRepository.findById(muestra.getPosicionCaja().getId())
                    .orElse(null);
            if (posAnterior != null) {
                posAnterior.setOcupada(false);
                posicionCajaRepository.save(posAnterior);
            }
        }

        // Asignar nueva
        nuevaPos.setOcupada(true);
        posicionCajaRepository.save(nuevaPos);

        muestra.setPosicionCaja(nuevaPos);
        muestra.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
        muestra.setFechaActualizacion(Timestamp.valueOf(LocalDateTime.now()));

        return muestraRepository.save(muestra);
    }

    /**
     * Libera la posición física de la muestra (sin moverla a otra).
     */
    @Transactional
    public Muestra liberarPosicion(Long idMuestra, String motivo) {
        Muestra muestra = muestraRepository.findById(idMuestra)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));

        if (muestra.getPosicionCaja() == null) {
            throw new ObjConflictException("La muestra no tiene posición asignada.");
        }

        PosicionCaja pos = posicionCajaRepository.findById(muestra.getPosicionCaja().getId())
                .orElse(null);
        if (pos != null) {
            pos.setOcupada(false);
            posicionCajaRepository.save(pos);
        }

        muestra.setPosicionCaja(null);
        muestra.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
        muestra.setFechaActualizacion(Timestamp.valueOf(LocalDateTime.now()));

        return muestraRepository.save(muestra);
    }
}
