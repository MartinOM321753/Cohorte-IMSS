package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.citas.dto.CitaPatchDTO;
import imss.gob.mx.cohorte.modules.cita.Cita;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaAgendadaEvent;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaCanceladaEvent;
import imss.gob.mx.cohorte.modules.notificaciones.events.CitaReprogramadaEvent;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.citas.CitaService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.services.institucion.InstitucionJerarquiaService;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.CITAS)
public class CitaApplicationService {
    private final CitaService citaService;
    private final PacienteService pacienteService;
    private final ParticipanteAccesoService participanteAccesoService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final InstitucionRepository institucionRepository;
    private final InstitucionContextService institucionContextService;
    private final InstitucionJerarquiaService institucionJerarquiaService;

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
        participanteAccesoService.verificarLecturaRegistro(cita.getInstitucion(), cita.getPaciente());
        return cita;
    }

    @Transactional(readOnly = true)
    public Cita findByUuid(String uuid){
        Cita cita = citaService.getByUuid(uuid);
        participanteAccesoService.verificarLecturaRegistro(cita.getInstitucion(), cita.getPaciente());
        return cita;
    }

    @Transactional(readOnly = true)
    public Cita findByPatientUuid(String uuid){
        Cita cita = citaService.findPatientUuid(uuid);
        participanteAccesoService.verificarLecturaRegistro(cita.getInstitucion(), cita.getPaciente());
        return cita;
    }

    @Transactional(readOnly = true)
    public List<Cita> findAllByPacienteUuid(String uuid) {
        // Si alcanzo al participante veo su historial completo; si no lo alcanzo pero
        // conservo registros suyos, veo solo los mios. Es la regla de union a nivel
        // de lista — ver ParticipanteAccesoService.resolverParaLectura.
        var acceso = participanteAccesoService.resolverParaLectura(uuid);
        return acceso.soloPropio()
                ? citaService.findAllByPacienteUuidDeMiInstitucion(uuid)
                : citaService.findAllByPacienteUuid(uuid);
    }

    @Transactional
    public Cita save(Cita cita, String institucionDestinoUUID){
        Paciente paciente = participanteAccesoService.resolverIncluyendoInactivo(cita.getPaciente().getUuid());
        BeanUser usuario = userService.getByUUID(cita.getUsuarioAgenda().getUUID());

        Institucion institucion;
        if (institucionDestinoUUID != null && !institucionDestinoUUID.isBlank()) {
            institucion = institucionRepository.findByUuid(institucionDestinoUUID)
                    .orElseThrow(() -> new ObjNotFoundException(
                            "No se encontró la institución destino con UUID: " + institucionDestinoUUID));
            Long idActual = institucionContextService.getIdInstitucionActual();
            List<Long> visibles = institucionJerarquiaService.getInstitucionesVisibles(idActual);
            if (!visibles.contains(institucion.getId())) {
                throw new ObjNotFoundException(
                        "La institución destino no está entre las instituciones visibles");
            }
        } else {
            institucion = institucionContextService.getInstitucionActual();
        }

        cita.setPaciente(paciente);
        cita.setUsuarioAgenda(usuario);
        cita.setInstitucion(institucion);
        Cita saved = citaService.create(cita);
        eventPublisher.publishEvent(new CitaAgendadaEvent(saved));
        return saved;
    }

    @Transactional
    public Cita patch(String uuid, CitaPatchDTO patchDto) {
        Cita existente = citaService.getByUuid(uuid);
        institucionContextService.verificarPertenece(existente.getInstitucion());

        Instant fechaAnterior = existente.getStartAtUtc();
        boolean cambiaHorario = patchDto.getStartAtLocal() != null;

        Cita updated = citaService.patch(uuid, patchDto);

        if (cambiaHorario && !updated.getStartAtUtc().equals(fechaAnterior)) {
            eventPublisher.publishEvent(new CitaReprogramadaEvent(updated, fechaAnterior));
        }

        return updated;
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
