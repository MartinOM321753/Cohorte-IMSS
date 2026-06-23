package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.EstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.EstudioMuestraRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class EstudioMuestraService {

    private final EstudioMuestraRepository repository;

    @Transactional(readOnly = true)
    public List<EstudioMuestra> getByMuestra(Long idMuestra) {
        return repository.findAllByMuestra_IdOrderByFechaEstudioDescIdDesc(idMuestra);
    }

    @Transactional(readOnly = true)
    public EstudioMuestra getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el estudio de muestra"));
    }

    @Transactional(rollbackFor = Exception.class)
    public EstudioMuestra create(EstudioMuestra estudio) {
        estudio.setFechaRegistro(LocalDateTime.now());
        return repository.save(estudio);
    }

    @Transactional(rollbackFor = Exception.class)
    public EstudioMuestra update(EstudioMuestra estudio) {
        return repository.save(estudio);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        EstudioMuestra estudio = getById(id);
        repository.delete(estudio);
    }
}
