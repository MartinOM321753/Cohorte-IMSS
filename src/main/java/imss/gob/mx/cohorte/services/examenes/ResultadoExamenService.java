package imss.gob.mx.cohorte.services.examenes;

import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.ExamenRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultadoExamenService {

    private final ResultadoExamenRepository resultadoExamenRepository;

    public List<ResultadoExamen> findAllByFolio(String folioPaciente) {
        return resultadoExamenRepository.findByPaciente_Folio(folioPaciente);
    }
    public List<ResultadoExamen> findAllByUUID(String uuidPaciente) {
        return resultadoExamenRepository.findByPaciente_Uuid(uuidPaciente);
    }
    public ResultadoExamen getResultado(Long id) {
        return resultadoExamenRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró resultado de examen con id: " + id));
    }

    public long countByPacienteUuid(String uuid) {
        return resultadoExamenRepository.countByPaciente_Uuid(uuid);
    }

    public ResultadoExamen createResultado(ResultadoExamen resultadoExamen) {
        resultadoExamen.setFechaRegistro(new Timestamp(System.currentTimeMillis()));
        if (resultadoExamen.getFechaResultado() == null) {
            resultadoExamen.setFechaResultado(LocalDateTime.now());
        }

        return resultadoExamenRepository.save(resultadoExamen);
    }

    public ResultadoExamen updateResultado(ResultadoExamen resultadoExamen) {
        ResultadoExamen resultadoBD = resultadoExamenRepository.findById(resultadoExamen.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró resultado de examen con id: " + resultadoExamen.getId()));

        resultadoBD.setValorObtenido(resultadoExamen.getValorObtenido());
        resultadoBD.setObservaciones(resultadoExamen.getObservaciones());
        resultadoBD.setFechaResultado(resultadoExamen.getFechaResultado());


        return resultadoExamenRepository.save(resultadoBD);
    }
}