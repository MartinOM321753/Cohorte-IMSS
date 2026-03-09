package imss.gob.mx.cohorte.services.examenes;

import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.ExamenRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
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
    private final ExamenRepository examenRepository;
    private final PacienteRepository pacienteRepository;

    @Transactional(readOnly = true)
    public List<ResultadoExamen> getAllResultados() {
        return resultadoExamenRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ResultadoExamen getResultado(Long id) {
        return resultadoExamenRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró resultado de examen con id: " + id));
    }

    @Transactional
    public ResultadoExamen createResultado(ResultadoExamen resultadoExamen) {
        // Validaciones de existencia
        Long idExamen = resultadoExamen.getExamen().getId();
        Examen examen = examenRepository.findById(idExamen)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el examen con id: " + idExamen));

        Long idPaciente = resultadoExamen.getPaciente().getId();
        Paciente paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró paciente con id: " + idPaciente));

        resultadoExamen.setExamen(examen);
        resultadoExamen.setPaciente(paciente);
        resultadoExamen.setFechaRegistro(new Timestamp(System.currentTimeMillis()));
        if (resultadoExamen.getFechaResultado() == null) {
            resultadoExamen.setFechaResultado(LocalDateTime.now());
        }

        return resultadoExamenRepository.save(resultadoExamen);
    }

    @Transactional
    public ResultadoExamen updateResultado(ResultadoExamen resultadoExamen) {
        ResultadoExamen resultadoBD = resultadoExamenRepository.findById(resultadoExamen.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró resultado de examen con id: " + resultadoExamen.getId()));

        resultadoBD.setValorObtenido(resultadoExamen.getValorObtenido());
        resultadoBD.setObservaciones(resultadoExamen.getObservaciones());
        resultadoBD.setFechaResultado(resultadoExamen.getFechaResultado());

        // Si se quiere actualizar el examen asociado, validar existencia
        if (resultadoExamen.getExamen() != null) {
            Long idExamen = resultadoExamen.getExamen().getId();
            Examen examen = examenRepository.findById(idExamen)
                    .orElseThrow(() -> new ObjNotFoundException("No se encontró el examen con id: " + idExamen));
            resultadoBD.setExamen(examen);
        }
        // Si se quiere actualizar el paciente asociado, validar existencia
        if (resultadoExamen.getPaciente() != null) {
            Long idPaciente = resultadoExamen.getPaciente().getId();
            Paciente paciente = pacienteRepository.findById(idPaciente)
                    .orElseThrow(() -> new ObjNotFoundException("No se encontró paciente con id: " + idPaciente));
            resultadoBD.setPaciente(paciente);
        }

        return resultadoExamenRepository.save(resultadoBD);
    }
}