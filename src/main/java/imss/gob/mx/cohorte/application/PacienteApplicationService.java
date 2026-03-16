package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class PacienteApplicationService {

    private final PacienteService pacienteService;
    private final PersonaService personaService;

    @Transactional
    public List<Paciente> findAll(Paciente paciente) {
        return pacienteService.findAll();
    }
    @Transactional
    public List<Paciente> findAllActive(Paciente paciente) {
        return pacienteService.findAllStatus(true);
    }

    public List<Paciente> findAllInactive(Paciente paciente) {
        return pacienteService.findAllStatus(false);
    }

    @Transactional
    public Paciente findUser(Long id) {
        return pacienteService.getPatient(id);
    }

    @Transactional
    public Paciente findByUUID(String uuid) {
        return pacienteService.getByUUID(uuid);
    }

    @Transactional
    public Paciente findByFolio(String folio) {return pacienteService.getByFolio(folio);}

    @Transactional
    public Paciente saveUser(Paciente paciente) {
        Persona savePersona = personaService.createPerson(paciente.getPersona());
        paciente.setPersona(savePersona);
        return pacienteService.cretePatient(paciente);
    }

    @Transactional
    public Paciente updateUser(Paciente paciente) {
        Persona updatePersona = personaService.createPerson(paciente.getPersona());
        paciente.setPersona(updatePersona);
        return pacienteService.updatePatient(paciente);
    }


}
