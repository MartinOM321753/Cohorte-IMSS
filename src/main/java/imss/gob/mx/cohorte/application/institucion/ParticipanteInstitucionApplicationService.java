package imss.gob.mx.cohorte.application.institucion;

import imss.gob.mx.cohorte.modules.institucion.ParticipanteInstitucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.institucion.ParticipanteInstitucionService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipanteInstitucionApplicationService {

    private final ParticipanteInstitucionService participanteInstitucionService;
    private final PacienteService pacienteService;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public List<ParticipanteInstitucion> findAllByPacienteUuid(String uuidPaciente) {
        Paciente paciente = pacienteService.getByUUID(uuidPaciente, institucionContextService.getIdInstitucionActual());
        return participanteInstitucionService.findAllByPaciente(paciente.getId());
    }

    @Transactional
    public ParticipanteInstitucion vincular(String uuidPaciente, Long idInstitucion, String observaciones) {
        Paciente paciente = pacienteService.getByUUID(uuidPaciente, institucionContextService.getIdInstitucionActual());
        return participanteInstitucionService.vincular(paciente, idInstitucion, observaciones);
    }

    @Transactional
    public void desvincular(String uuidPaciente, Long idInstitucion) {
        Paciente paciente = pacienteService.getByUUID(uuidPaciente, institucionContextService.getIdInstitucionActual());
        participanteInstitucionService.desvincular(paciente.getId(), idInstitucion);
    }
}
