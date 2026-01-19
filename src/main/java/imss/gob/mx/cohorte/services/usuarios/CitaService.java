package imss.gob.mx.cohorte.services.usuarios;


import imss.gob.mx.cohorte.modules.usuarios.cita.Cita;
import imss.gob.mx.cohorte.modules.usuarios.cita.CitaRepository;
import imss.gob.mx.cohorte.modules.usuarios.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class CitaService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final UserRepository userRepository;


    @Transactional(readOnly = true)
    public List<Cita> getAll() {
        return citaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cita getById(Long id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("La cita no existe"));
    }


    @Transactional
    public Cita create(Cita cita) {

        Paciente paciente = pacienteRepository.findById(cita.getPaciente().getId())
                .orElseThrow(() -> new ObjNotFoundException("El paciente no existe"));

        BeanUser usuarioAgenda = userRepository.findById(cita.getUsuarioAgenda().getId())
                .orElseThrow(() -> new ObjNotFoundException("El usuario que agenda no existe"));

        cita.setPaciente(paciente);
        cita.setUsuarioAgenda(usuarioAgenda);
        cita.setFechaRegistro(Timestamp.valueOf(LocalDateTime.now()));
        cita.setEstadoCita(Cita.EstadoCita.Programada);

        return citaRepository.save(cita);
    }



    @Transactional
    public Cita update(Cita cita) {

        Cita citaBD = citaRepository.findById(cita.getId())
                .orElseThrow(() -> new ObjNotFoundException("La cita no existe"));

        citaBD.setFechaCita(cita.getFechaCita());
        citaBD.setDuracionMinutos(cita.getDuracionMinutos());
        citaBD.setEstadoCita(cita.getEstadoCita());
        citaBD.setObservaciones(cita.getObservaciones());
        citaBD.setFechaActualizacion(Timestamp.valueOf(LocalDateTime.now()));

        return citaRepository.save(citaBD);
    }



    @Transactional
    public void cancelar(Long idCita) {

        Cita citaBD = citaRepository.findById(idCita)
                .orElseThrow(() -> new ObjNotFoundException("La cita no existe"));

        citaBD.setEstadoCita(Cita.EstadoCita.Cancelada);
        citaBD.setFechaActualizacion(Timestamp.valueOf(LocalDateTime.now()));

        citaRepository.save(citaBD);
    }
}