package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
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


    public List<Paciente> findAll(){
        return pacienteRepository.findAll();
    }

    public List<Paciente> findAllStatus(Boolean activo){
        return pacienteRepository.findAllByActivo(activo);
    }
    public Paciente getPatient(Long idPaciente){
        return pacienteRepository.findById(idPaciente)
                .orElseThrow(()-> new ObjNotFoundException("No se encontró el paciente"));
    }
    public Paciente getByUUID(String uuid){
        return pacienteRepository.findByUUID(uuid)
                .orElseThrow(()-> new ObjNotFoundException("No se encontró el paciente"));
    }

    public Paciente getByFolio(String folio){
        return pacienteRepository.findByFolio(folio)
                .orElseThrow(()-> new ObjNotFoundException("No se encontró el paciente"));
    }

    public Paciente cretePatient(Paciente paciente){
      Optional <Paciente> findPatient =  pacienteRepository.findByFolio(paciente.getFolio());

      if(findPatient.isPresent()){throw new ObjConflictException("El folio ya existe");}

      paciente.setFechaRegistro(LocalDateTime.now());
      paciente.setFechaActualizacion(LocalDateTime.now());

      return pacienteRepository.save(paciente);
    }
    public Paciente updatePatient(Paciente paciente) {

        Paciente pacienteBD = pacienteRepository.findById(paciente.getId())
                .orElseThrow(() -> new ObjNotFoundException("El paciente no existe"));

        if (!paciente.getFolio().equals(pacienteBD.getFolio())) {
            if (pacienteRepository.findByFolio(paciente.getFolio()).isPresent()) {
                throw new ObjConflictException("El folio ya existe");
            }
            pacienteBD.setFolio(paciente.getFolio());
        }
        paciente.setActivo(pacienteBD.getActivo());
        pacienteBD.setFechaActualizacion(LocalDateTime.now());
        return pacienteRepository.save(pacienteBD);
    }




}
