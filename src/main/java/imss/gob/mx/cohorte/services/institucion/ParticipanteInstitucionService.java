package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.institucion.ParticipanteInstitucion;
import imss.gob.mx.cohorte.modules.institucion.ParticipanteInstitucionRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Gestiona el vínculo M:N entre participantes (Paciente) e instituciones.
 * Mantiene un historial de auditoría — nunca se eliminan los registros,
 * sólo se desactivan (activo = false) para preservar trazabilidad.
 */
@Service
@RequiredArgsConstructor
public class ParticipanteInstitucionService {

    private final ParticipanteInstitucionRepository repository;
    private final InstitucionRepository institucionRepository;

    @Transactional(readOnly = true)
    public List<ParticipanteInstitucion> findAllByPaciente(Long idPaciente) {
        return repository.findAllByPaciente_IdAndActivoTrue(idPaciente);
    }

    @Transactional(readOnly = true)
    public List<ParticipanteInstitucion> findAllByInstitucion(Long idInstitucion) {
        return repository.findAllByInstitucion_IdAndActivoTrue(idInstitucion);
    }

    @Transactional
    public ParticipanteInstitucion vincular(Paciente paciente, Long idInstitucion, String observaciones) {
        if (idInstitucion == null) {
            throw new ValidationException("La institución a vincular es obligatoria.");
        }
        if (repository.existsByPaciente_IdAndInstitucion_IdAndActivoTrue(paciente.getId(), idInstitucion)) {
            throw new ObjConflictException("El participante ya está vinculado activamente a esta institución.");
        }

        Institucion institucion = institucionRepository.findById(idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución con id: " + idInstitucion));

        ParticipanteInstitucion vinculo = new ParticipanteInstitucion();
        vinculo.setPaciente(paciente);
        vinculo.setInstitucion(institucion);
        vinculo.setActivo(true);
        vinculo.setObservaciones(observaciones);
        vinculo.setFechaAsignacion(Timestamp.from(Instant.now()));

        return repository.save(vinculo);
    }

    @Transactional
    public void desvincular(Long idPaciente, Long idInstitucion) {
        ParticipanteInstitucion vinculo = repository.findByPaciente_IdAndInstitucion_Id(idPaciente, idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException("El participante no está vinculado a esta institución."));
        vinculo.setActivo(false);
        repository.save(vinculo);
    }
}
