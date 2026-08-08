package imss.gob.mx.cohorte.services.examenes;

import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;

import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.ExamenRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultadoExamenService {

    private final ResultadoExamenRepository resultadoExamenRepository;
    private final ParticipanteAccesoService participanteAccesoService;
    private final InstitucionContextService institucionContextService;

    public List<ResultadoExamen> findAllByFolio(String folioPaciente) {
        return resultadoExamenRepository.findByPaciente_FolioAndPaciente_Institucion_IdIn(
                folioPaciente, participanteAccesoService.institucionesAlcanzables());
    }
    public List<ResultadoExamen> findAllByUUID(String uuidPaciente) {
        participanteAccesoService.resolver(uuidPaciente);
        return resultadoExamenRepository.findByPaciente_Uuid(uuidPaciente);
    }
    public Page<ResultadoExamen> findAllByUUIDPaginado(String uuidPaciente, Pageable pageable) {
        participanteAccesoService.resolver(uuidPaciente);
        return resultadoExamenRepository.findByPaciente_Uuid(uuidPaciente, pageable);
    }
    public ResultadoExamen getResultado(Long id) {
        ResultadoExamen resultado = resultadoExamenRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró resultado de examen con id: " + id));
        // El resultado de examen no lleva institucion propia: hereda la del paciente,
        // asi que su lectura se decide unicamente por el alcance al participante.
        participanteAccesoService.verificarLecturaRegistro(null, resultado.getPaciente());
        return resultado;
    }

    /** Resultados capturados por mi institucion, sin pasar por el participante. */
    public List<ResultadoExamen> findAllDeMiInstitucion() {
        return resultadoExamenRepository.findAllByInstitucion_IdOrderByFechaResultadoDesc(
                institucionContextService.getIdInstitucionActual());
    }

    public Page<ResultadoExamen> findAllDeMiInstitucionPaginado(Pageable pageable) {
        return resultadoExamenRepository.findAllByInstitucion_IdOrderByFechaResultadoDesc(
                institucionContextService.getIdInstitucionActual(), pageable);
    }

    public long countByPacienteUuid(String uuid) {
        participanteAccesoService.resolver(uuid);
        return resultadoExamenRepository.countByPaciente_Uuid(uuid);
    }

    public ResultadoExamen createResultado(ResultadoExamen resultadoExamen) {
        // La sede que captura, no la del participante: al atender entre sedes pueden
        // ser distintas, y el registro debe guardar quien lo hizo.
        resultadoExamen.setInstitucion(institucionContextService.getInstitucionActual());
        resultadoExamen.setFechaRegistro(new Timestamp(System.currentTimeMillis()));
        if (resultadoExamen.getFechaResultado() == null) {
            resultadoExamen.setFechaResultado(LocalDateTime.now());
        }

        return resultadoExamenRepository.save(resultadoExamen);
    }

    public void deleteResultado(Long id) {
        ResultadoExamen resultado = resultadoExamenRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró resultado de examen con id: " + id));
        institucionContextService.verificarPertenece(resultado.getPaciente().getInstitucion());
        resultadoExamenRepository.delete(resultado);
    }

    public ResultadoExamen updateResultado(ResultadoExamen resultadoExamen) {
        ResultadoExamen resultadoBD = resultadoExamenRepository.findById(resultadoExamen.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró resultado de examen con id: " + resultadoExamen.getId()));
        institucionContextService.verificarPertenece(resultadoBD.getPaciente().getInstitucion());

        resultadoBD.setValorObtenido(resultadoExamen.getValorObtenido());
        resultadoBD.setObservaciones(resultadoExamen.getObservaciones());
        resultadoBD.setFechaResultado(resultadoExamen.getFechaResultado());


        return resultadoExamenRepository.save(resultadoBD);
    }
}