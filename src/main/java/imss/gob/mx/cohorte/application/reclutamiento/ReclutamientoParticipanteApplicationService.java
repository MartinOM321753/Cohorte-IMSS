package imss.gob.mx.cohorte.application.reclutamiento;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.reclutamiento.EstadoContacto;
import imss.gob.mx.cohorte.modules.reclutamiento.MedioContacto;
import imss.gob.mx.cohorte.modules.reclutamiento.ReclutamientoParticipante;
import imss.gob.mx.cohorte.modules.reclutamiento.TipoReclutamiento;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.reclutamiento.ReclutamientoParticipanteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReclutamientoParticipanteApplicationService {

    private final ReclutamientoParticipanteService reclutamientoService;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public Optional<ReclutamientoParticipante> findByPaciente(Long idPaciente) {
        return reclutamientoService.findByPaciente(idPaciente);
    }

    @Transactional(readOnly = true)
    public Optional<ReclutamientoParticipante> findByPacienteUuid(String uuidPaciente) {
        return reclutamientoService.findByPacienteUuid(uuidPaciente);
    }

    @Transactional
    public ReclutamientoParticipante create(Paciente paciente, TipoReclutamiento tipoReclutamiento,
                                            EstadoContacto estadoContacto, MedioContacto medioContacto,
                                            String uuidUsuarioRecluta,
                                            String observaciones, Timestamp fechaContacto) {
        Long idInstitucionReclutamiento = institucionContextService.getIdInstitucionActual();
        return reclutamientoService.create(paciente, tipoReclutamiento, estadoContacto, medioContacto,
                idInstitucionReclutamiento, uuidUsuarioRecluta, observaciones, fechaContacto);
    }

    @Transactional
    public ReclutamientoParticipante update(Long idPaciente, EstadoContacto estadoContacto, MedioContacto medioContacto,
                                            String observaciones, Timestamp fechaContacto) {
        return reclutamientoService.update(idPaciente, estadoContacto, medioContacto, observaciones, fechaContacto);
    }
}
