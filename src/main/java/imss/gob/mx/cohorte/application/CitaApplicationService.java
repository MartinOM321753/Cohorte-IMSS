package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.citas.dto.CitaPatchDTO;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaAgendadaEvent;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaCanceladaEvent;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.citas.CitaService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
public class CitaApplicationService {
    private final CitaService citaService;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<Cita> getAll(){
        return citaService.getAll();
    }

    @Transactional(readOnly = true)
    public List<Cita> getByRange(Instant start, Instant end) {
        return citaService.getByRange(start, end);
    }

    @Transactional(readOnly = true)
    public Cita findByFolio(String folio){
        return citaService.findPatientFolio(folio);
    }

    @Transactional(readOnly = true)
    public Cita findByUuid(String uuid){
        return citaService.getByUuid(uuid);
    }

    @Transactional(readOnly = true)
    public Cita findByPatientUuid(String uuid){
        return citaService.findPatientUuid(uuid);
    }

    @Transactional
    public Cita save(Cita cita){
        Paciente paciente = pacienteService.getByUUID(cita.getPaciente().getUuid());
        BeanUser usuario = userService.getByUUID(cita.getUsuarioAgenda().getUUID());
        cita.setPaciente(paciente);
        cita.setUsuarioAgenda(usuario);
        Cita saved = citaService.create(cita);
        // El listener se ejecuta AFTER_COMMIT en hilo async — no bloquea la respuesta HTTP
        eventPublisher.publishEvent(new CitaAgendadaEvent(saved));
        return saved;
    }

    @Transactional
    public Cita patch(String uuid, CitaPatchDTO patchDto) {
        return citaService.patch(uuid, patchDto);
    }

    @Transactional
    public void cancelar(String uuid){
        citaService.cancelar(uuid);
        Cita cita = citaService.getByUuid(uuid);
        eventPublisher.publishEvent(new CitaCanceladaEvent(cita));
    }
}
