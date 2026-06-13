package imss.gob.mx.cohorte.services.reclutamiento;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.reclutamiento.*;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReclutamientoParticipanteService {

    private final ReclutamientoParticipanteRepository reclutamientoRepository;
    private final InstitucionRepository institucionRepository;
    private final UserRepository userRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public Optional<ReclutamientoParticipante> findByPaciente(Long idPaciente) {
        return reclutamientoRepository.findByPaciente_IdAndPaciente_Institucion_Id(
                idPaciente, institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public Optional<ReclutamientoParticipante> findByPacienteUuid(String uuidPaciente) {
        return reclutamientoRepository.findByPaciente_UuidAndPaciente_Institucion_Id(
                uuidPaciente, institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public ReclutamientoParticipante getById(Long id) {
        ReclutamientoParticipante r = reclutamientoRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el registro de reclutamiento con id: " + id));
        institucionContextService.verificarPertenece(r.getPaciente().getInstitucion());
        return r;
    }

    /**
     * Crea la clasificación de reclutamiento para un paciente recién registrado.
     * Debe invocarse dentro de la misma transacción del alta del paciente.
     */
    @Transactional
    public ReclutamientoParticipante create(Paciente paciente,
                                            TipoReclutamiento tipoReclutamiento,
                                            EstadoContacto estadoContacto,
                                            MedioContacto medioContacto,
                                            Long idInstitucionReclutamiento,
                                            String uuidUsuarioRecluta,
                                            String observaciones,
                                            Timestamp fechaContacto) {
        if (reclutamientoRepository.existsByPaciente_Id(paciente.getId())) {
            throw new ObjConflictException("El participante ya cuenta con una clasificación de reclutamiento.");
        }
        if (tipoReclutamiento == null) {
            throw new ValidationException("El tipo de reclutamiento (RETORNO/NUEVO) es obligatorio.");
        }

        Institucion institucion = resolverInstitucion(idInstitucionReclutamiento);
        BeanUser usuarioRecluta = resolverUsuarioRecluta(uuidUsuarioRecluta);

        ReclutamientoParticipante r = new ReclutamientoParticipante();
        r.setPaciente(paciente);
        r.setTipoReclutamiento(tipoReclutamiento);
        r.setEstadoContacto(estadoContacto != null ? estadoContacto : EstadoContacto.PENDIENTE);
        r.setMedioContacto(medioContacto);
        r.setInstitucionReclutamiento(institucion);
        r.setUsuarioRecluta(usuarioRecluta);
        r.setObservaciones(observaciones);
        r.setFechaContacto(fechaContacto);
        r.setFechaRegistro(Timestamp.from(Instant.now()));

        return reclutamientoRepository.save(r);
    }

    @Transactional
    public ReclutamientoParticipante update(Long idPaciente,
                                            EstadoContacto estadoContacto,
                                            MedioContacto medioContacto,
                                            String observaciones,
                                            Timestamp fechaContacto) {
        ReclutamientoParticipante r = reclutamientoRepository.findByPaciente_IdAndPaciente_Institucion_Id(
                        idPaciente, institucionContextService.getIdInstitucionActual())
                .orElseThrow(() -> new ObjNotFoundException("El participante no cuenta con clasificación de reclutamiento."));

        if (estadoContacto != null) r.setEstadoContacto(estadoContacto);
        if (medioContacto != null) r.setMedioContacto(medioContacto);
        if (observaciones != null) r.setObservaciones(observaciones);
        if (fechaContacto != null) r.setFechaContacto(fechaContacto);

        return reclutamientoRepository.save(r);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private Institucion resolverInstitucion(Long idInstitucion) {
        if (idInstitucion == null) {
            throw new ValidationException("La institución de reclutamiento es obligatoria.");
        }
        Institucion institucion = institucionRepository.findById(idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución con id: " + idInstitucion));
        institucionContextService.verificarPertenece(institucion);
        return institucion;
    }

    private BeanUser resolverUsuarioRecluta(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            throw new ValidationException("El usuario que realizó el reclutamiento es obligatorio.");
        }
        return userRepository.findByUUID(uuid)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el usuario reclutador con UUID: " + uuid));
    }
}
