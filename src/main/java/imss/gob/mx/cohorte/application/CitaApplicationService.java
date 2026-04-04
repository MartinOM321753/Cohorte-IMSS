package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.citas.CitaService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class CitaApplicationService {
    private final CitaService citaService;
    private final PacienteService pacienteService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<Cita> getAll(){
        return citaService.getAll();
    }
    @Transactional(readOnly = true)
    public Cita findById(Long id){
        return citaService.getById(id);
    }
    @Transactional(readOnly = true)
    public Cita findByFolio(String folio){
        return citaService.findPatientFolio(folio);
    }
    @Transactional(readOnly = true)
    public Cita findByUuid(String uuid){
        return citaService.findPatientUuid(uuid);
    }
    @Transactional(readOnly = true)
    public Cita findByFolio(LocalDateTime fecha){
        return citaService.findCitaFecha(fecha);
    }

    @Transactional
    public Cita save(Cita cita){

        Paciente paciente = pacienteService.getByUUID(cita.getPaciente().getUuid());
        BeanUser usuario = userService.getByUUID(cita.getUsuarioAgenda().getUUID());
        cita.setPaciente(paciente);
        cita.setUsuarioAgenda(usuario);

        return citaService.create(cita);
    }
    @Transactional
    public Cita update (Cita cita){
        return  citaService.update(cita);
    }
    @Transactional
    public void cancelar(Long idCita){
        citaService.cancelar(idCita);
    }

}
