package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.citas.dto.CitaPatchDTO;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaAgendadaEvent;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaCanceladaEvent;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.citas.CitaService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.CITAS)
public class CitaApplicationService {
    private final CitaService citaService;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final InstitucionRepository institucionRepository;
    private final InstitucionContextService institucionContextService;

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
        Cita cita = citaService.findPatientFolio(folio);
        institucionContextService.verificarPertenece(cita.getInstitucion());
        return cita;
    }

    @Transactional(readOnly = true)
    public Cita findByUuid(String uuid){
        Cita cita = citaService.getByUuid(uuid);
        institucionContextService.verificarPertenece(cita.getInstitucion());
        return cita;
    }

    @Transactional(readOnly = true)
    public Cita findByPatientUuid(String uuid){
        return citaService.findPatientUuid(uuid);
    }

    @Transactional(readOnly = true)
    public List<Cita> findAllByPacienteUuid(String uuid) {
        return citaService.findAllByPacienteUuid(uuid);
    }

    @Transactional
    public Cita save(Cita cita){
        Paciente paciente = pacienteService.getByUUID(cita.getPaciente().getUuid(), institucionContextService.getIdInstitucionActual());
        BeanUser usuario = userService.getByUUID(cita.getUsuarioAgenda().getUUID());
        if (cita.getInstitucion() == null || cita.getInstitucion().getId() == null) {
            throw new ObjNotFoundException("Falta informacion de institucion responsable de la cita");
        }
        Institucion institucion = institucionRepository.findById(cita.getInstitucion().getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución con id: " + cita.getInstitucion().getId()));
        // Aislamiento por institución: no se permite agendar citas a nombre de otra institución
        institucionContextService.verificarPertenece(institucion);
        cita.setPaciente(paciente);
        cita.setUsuarioAgenda(usuario);
        cita.setInstitucion(institucion);
        Cita saved = citaService.create(cita);
        // El listener se ejecuta AFTER_COMMIT en hilo async — no bloquea la respuesta HTTP
        eventPublisher.publishEvent(new CitaAgendadaEvent(saved));
        return saved;
    }

    @Transactional
    public Cita patch(String uuid, CitaPatchDTO patchDto) {
        Cita existente = citaService.getByUuid(uuid);
        institucionContextService.verificarPertenece(existente.getInstitucion());
        return citaService.patch(uuid, patchDto);
    }

    @Transactional
    public void cancelar(String uuid){
        Cita existente = citaService.getByUuid(uuid);
        institucionContextService.verificarPertenece(existente.getInstitucion());
        citaService.cancelar(uuid);
        Cita cita = citaService.getByUuid(uuid);
        eventPublisher.publishEvent(new CitaCanceladaEvent(cita));
    }
}
