package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class EstudioService {
    private final EstudioMedicoRepository estudioMedicoRepository;

    @Transactional(readOnly = true)
    public EstudioMedico getOne(Long id) {
        return estudioMedicoRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el estudio medico"));
    }

    @Transactional(readOnly = true)
    public List<EstudioMedico> getAll() {
        return estudioMedicoRepository.findAllByOrderByFechaEstudioDesc();
    }

    @Transactional(rollbackFor = Exception.class)
    public EstudioMedico create(EstudioMedico estudioMedico) {
        if (estudioMedico.getFechaRegistro() == null) {
            estudioMedico.setFechaRegistro(LocalDateTime.now());
        }
        return estudioMedicoRepository.save(estudioMedico);
    }

    @Transactional(rollbackFor = Exception.class)
    public EstudioMedico update(EstudioMedico estudioMedico) {
        if (!estudioMedicoRepository.existsById(estudioMedico.getId())) {
            throw new ObjNotFoundException("No se encontro el estudio medico");
        }
        return estudioMedicoRepository.save(estudioMedico);
    }
}
