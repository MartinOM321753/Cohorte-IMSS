package imss.gob.mx.cohorte.services.usuarios;


import imss.gob.mx.cohorte.modules.usuarios.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.persona.Persona;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PacienteService {
    private final PacienteRepository pacienteRepository;
    private final PersonaService personaService;


    @Transactional(readOnly = true)
    public List<Paciente> getAllPatient(){
        return pacienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Paciente getPatient(Long idPaciente){
        return pacienteRepository.findById(idPaciente)
                .orElseThrow(()-> new ObjNotFoundException("No se encontró el paciente"));
    }

    @Transactional(rollbackFor = Exception.class )
    public Paciente cretePatient(Paciente paciente){
      Optional <Paciente> findPatient =  pacienteRepository.findByFolio(paciente.getFolio());

      if(findPatient.isPresent()){
          throw new ObjConflictException("El folio ya existe");
      }
      Persona personaCreada = personaService.createPerson(paciente.getPersona());
      paciente.setPersona(personaCreada);

      paciente.setFechaRegistro(LocalDateTime.now());
      paciente.setFechaActualizacion(LocalDateTime.now());

      return pacienteRepository.save(paciente);
    }
    @Transactional(rollbackFor = Exception.class)
    public Paciente updatePatient(Paciente paciente) {

        Paciente pacienteBD = pacienteRepository.findById(paciente.getId())
                .orElseThrow(() -> new ObjNotFoundException("El paciente no existe"));

        // Validar folio solo si cambió
        if (!paciente.getFolio().equals(pacienteBD.getFolio())) {
            if (pacienteRepository.findByFolio(paciente.getFolio()).isPresent()) {
                throw new ObjConflictException("El folio ya existe");
            }
            pacienteBD.setFolio(paciente.getFolio());
        }

        personaService.update(paciente.getPersona());

        pacienteBD.setFechaActualizacion(LocalDateTime.now());

        return pacienteRepository.save(pacienteBD);
    }




}
