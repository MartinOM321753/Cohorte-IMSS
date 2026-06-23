package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class EstudioService {
    private final EstudioMedicoRepository estudioMedicoRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public EstudioMedico getOne(Long id) {
        EstudioMedico estudio = estudioMedicoRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el estudio medico"));
        institucionContextService.verificarPertenece(estudio.getInstitucion());
        return estudio;
    }

    @Transactional(readOnly = true)
    public List<EstudioMedico> getAll() {
        return estudioMedicoRepository.findAllByInstitucion_IdOrderByFechaEstudioDesc(
                institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public List<EstudioMedico> getAllByPacienteUUID(String uuid) {
        return estudioMedicoRepository.findAllByPaciente_UuidAndInstitucion_IdOrderByFechaEstudioDesc(
                uuid, institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public Page<EstudioMedico> getAllPaginado(Pageable pageable) {
        return estudioMedicoRepository.findAllByInstitucion_IdOrderByFechaEstudioDesc(
                institucionContextService.getIdInstitucionActual(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<EstudioMedico> getAllByPacienteUUIDPaginado(String uuid, Pageable pageable) {
        return estudioMedicoRepository.findAllByPaciente_UuidAndInstitucion_IdOrderByFechaEstudioDesc(
                uuid, institucionContextService.getIdInstitucionActual(), pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public EstudioMedico create(EstudioMedico estudioMedico) {
        if (estudioMedico.getFechaRegistro() == null) {
            estudioMedico.setFechaRegistro(LocalDateTime.now());
        }
        estudioMedico.setInstitucion(institucionContextService.getInstitucionActual());
        return estudioMedicoRepository.save(estudioMedico);
    }

    @Transactional(rollbackFor = Exception.class)
    public EstudioMedico update(EstudioMedico estudioMedico) {
        EstudioMedico estudioBD = estudioMedicoRepository.findById(estudioMedico.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el estudio medico"));
        institucionContextService.verificarPertenece(estudioBD.getInstitucion());
        return estudioMedicoRepository.save(estudioMedico);
    }
}
