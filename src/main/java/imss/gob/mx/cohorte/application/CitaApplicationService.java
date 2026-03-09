package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.services.citas.CitaService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CitaApplicationService {
    private final CitaService citaService;
    private final PacienteService pacienteService;
    private final UserService userService;


    @Transactional
    public Cita save(Cita cita){

        Paciente paciente = pacienteService.getByUUID(cita.getPaciente().getUUID());
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
