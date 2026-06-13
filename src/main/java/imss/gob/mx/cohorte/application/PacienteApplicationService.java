package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.reclutamiento.dto.ReclutamientoParticipanteRequestDTO;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.reclutamiento.ReclutamientoParticipante;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.reclutamiento.ReclutamientoParticipanteService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.PARTICIPANTES)
public class PacienteApplicationService {

    private final PacienteService pacienteService;
    private final PersonaService personaService;
    private final ReclutamientoParticipanteService reclutamientoService;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public List<Paciente> getAll() {
        return pacienteService.findAllByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public Page<Paciente> getAllPaginado(Pageable pageable) {
        return pacienteService.findAllPaginadoByInstitucion(institucionContextService.getIdInstitucionActual(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Paciente> getActivos() {
        return pacienteService.findAllStatusByInstitucion(true, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public List<Paciente> findAll(Paciente paciente) {
        return pacienteService.findAllByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public List<Paciente> findAllActive(Paciente paciente) {
        return pacienteService.findAllStatusByInstitucion(true, institucionContextService.getIdInstitucionActual());
    }

    public List<Paciente> findAllInactive(Paciente paciente) {
        return pacienteService.findAllStatusByInstitucion(false, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public Paciente findUser(Long id) {
        return pacienteService.getPatient(id, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public Paciente findByUUID(String uuid) {
        return pacienteService.getByUUID(uuid, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public Paciente findByFolio(String folio) {
        return pacienteService.getByFolio(folio, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public Paciente saveUser(Paciente paciente) {
        Persona savePersona = personaService.createPerson(paciente.getPersona());
        paciente.setPersona(savePersona);
        // La institución del participante SIEMPRE se infiere del usuario autenticado —
        // nunca se acepta del cliente, para evitar que se registre en otra institución.
        Institucion institucionActual = institucionContextService.getInstitucionActual();
        paciente.setInstitucion(institucionActual);
        return pacienteService.cretePatient(paciente);
    }

    /**
     * Registra al participante junto con su clasificación de reclutamiento
     * (origen RETORNO/NUEVO, institución y medio de contacto). Ambas entidades
     * se persisten en la misma transacción para mantener la integridad 1:1.
     *
     * @param uuidUsuarioAutenticado UUID del usuario en sesión — se usa como reclutador
     *                               por defecto cuando el DTO no especifica uno explícito.
     */
    @Transactional
    public Paciente saveUserConReclutamiento(Paciente paciente, ReclutamientoParticipanteRequestDTO reclutamientoDto, String uuidUsuarioAutenticado) {
        Paciente saved = saveUser(paciente);

        String uuidRecluta = (reclutamientoDto.getUuidUsuarioRecluta() != null && !reclutamientoDto.getUuidUsuarioRecluta().isBlank())
                ? reclutamientoDto.getUuidUsuarioRecluta()
                : uuidUsuarioAutenticado;

        reclutamientoService.create(
                saved,
                reclutamientoDto.getTipoReclutamiento(),
                reclutamientoDto.getEstadoContacto(),
                reclutamientoDto.getMedioContacto(),
                institucionContextService.getIdInstitucionActual(),
                uuidRecluta,
                reclutamientoDto.getObservaciones(),
                reclutamientoDto.getFechaContacto() != null
                        ? java.sql.Timestamp.valueOf(reclutamientoDto.getFechaContacto().atStartOfDay())
                        : null
        );

        return saved;
    }

    /** Obtiene la clasificación de reclutamiento (1:1) asociada a un paciente, si existe. */
    @Transactional(readOnly = true)
    public ReclutamientoParticipante getReclutamiento(Long idPaciente) {
        return reclutamientoService.findByPaciente(idPaciente).orElse(null);
    }

    @Transactional
    public Paciente updateUser(Paciente paciente) {
        Long idInstitucionActual = institucionContextService.getIdInstitucionActual();
        Paciente existing = pacienteService.getPatient(paciente.getId(), idInstitucionActual);
        paciente.getPersona().setId(existing.getPersona().getId());
        Persona updatePersona = personaService.update(paciente.getPersona());
        paciente.setPersona(updatePersona);
        return pacienteService.updatePatient(paciente, idInstitucionActual);
    }

    /** Alterna el estado activo/inactivo del paciente. Devuelve el paciente actualizado. */
    @Transactional
    public Paciente toggleActivo(String uuid) {
        return pacienteService.toggleActivo(uuid, institucionContextService.getIdInstitucionActual());
    }
}
