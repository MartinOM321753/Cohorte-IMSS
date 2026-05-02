package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PacienteService {
    private final PacienteRepository pacienteRepository;

    public List<Paciente> findAll() {
        return pacienteRepository.findAll();
    }

    public List<Paciente> findAllStatus(Boolean activo) {
        return pacienteRepository.findAllByActivo(activo);
    }

    public Paciente getPatient(Long idPaciente) {
        Paciente findPatient = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El paciente no se encuentra activo");
        }
        return findPatient;
    }

    public Paciente getByUUID(String uuid) {
        Paciente findPatient = pacienteRepository.findByUuid(uuid)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El paciente no se encuentra activo");
        }
        return findPatient;
    }

    public Paciente getByFolio(String folio) {
        Paciente findPatient = pacienteRepository.findByFolio(folio)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El paciente no se encuentra activo");
        }
        return findPatient;
    }

    public Paciente cretePatient(Paciente paciente) {
        Optional<Paciente> findPatient = pacienteRepository.findByFolio(paciente.getFolio());
        if (findPatient.isPresent()) {
            throw new ObjConflictException("El folio ya existe");
        }

        paciente.setFechaRegistro(LocalDateTime.now());
        paciente.setFechaActualizacion(LocalDateTime.now());
        paciente.setUuid(java.util.UUID.randomUUID().toString());
        return pacienteRepository.save(paciente);
    }

    public Paciente updatePatient(Paciente paciente) {
        Paciente pacienteBD = pacienteRepository.findById(paciente.getId())
                .orElseThrow(() -> new ObjNotFoundException("El paciente no existe"));

        if (!paciente.getFolio().equals(pacienteBD.getFolio())
                && pacienteRepository.findByFolio(paciente.getFolio()).isPresent()) {
            throw new ObjConflictException("El folio ya existe");
        }

        pacienteBD.setFolio(paciente.getFolio());
        pacienteBD.setPersona(paciente.getPersona());
        pacienteBD.setFechaActualizacion(LocalDateTime.now());
        return pacienteRepository.save(pacienteBD);
    }
}
