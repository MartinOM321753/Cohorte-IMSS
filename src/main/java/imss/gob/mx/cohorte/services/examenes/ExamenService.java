package imss.gob.mx.cohorte.services.examenes;

import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.ExamenRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamenService {

    private final ExamenRepository examenRepository;

    @Transactional(readOnly = true)
    public List<Examen> getAllExamenes() {
        return examenRepository.findAllByActivo(true);
    }


    @Transactional(readOnly = true)
    public Examen getExamen(Long id) {
        return examenRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el examen con id: " + id));
    }

    @Transactional
    public Examen createExamen(Examen examen) {
        examen.setFechaCreacion(Timestamp.from(Instant.now()));
        return examenRepository.save(examen);
    }

    @Transactional
    public Examen updateExamen(Examen examen) {
        Examen examenBD = examenRepository.findById(examen.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el examen con id: " + examen.getId()));

        examenBD.setParametro(examen.getParametro());
        examenBD.setDescripcion(examen.getDescripcion());
        examenBD.setUnidad(examen.getUnidad());
        examenBD.setValorMinMujeres(examen.getValorMinMujeres());
        examenBD.setValorMaxMujeres(examen.getValorMaxMujeres());
        examenBD.setValorMinHombres(examen.getValorMinHombres());
        examenBD.setValorMaxHombres(examen.getValorMaxHombres());
        examenBD.setActivo(examen.getActivo());
        // La fechaCreacion no se actualiza (por ser updatable = false)
        return examenRepository.save(examenBD);
    }
}