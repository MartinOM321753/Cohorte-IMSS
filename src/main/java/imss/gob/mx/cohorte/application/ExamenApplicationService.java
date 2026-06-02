package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.examenes.ExamenService;
import imss.gob.mx.cohorte.services.examenes.ResultadoExamenService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ExamenApplicationService {
    private final ExamenService examenService;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final ResultadoExamenService resultadoExamenService;

    @Transactional
    public List<Examen> findAll() {
        return examenService.getAllExamenes();
    }
    @Transactional
    public Examen findOne(Long id) {
        return examenService.getExamen(id);
    }

    @Transactional
    public Examen create(Examen examen) {
        return examenService.createExamen(examen);
    }
    @Transactional
    public Examen update(Examen examen) {
        return examenService.updateExamen(examen);
    }
    @Transactional
    public List<ResultadoExamen> findAllResultadoByFolio(String folio) {
        return resultadoExamenService.findAllByFolio(folio);
    }

    @Transactional
    public List<ResultadoExamen> findAllResultadoByUUID(String uuid) {
        return resultadoExamenService.findAllByUUID(uuid);
    }

    @Transactional(readOnly = true)
    public long countResultadosByPacienteUuid(String uuid) {
        return resultadoExamenService.countByPacienteUuid(uuid);
    }
    @Transactional
    public ResultadoExamen createResultado(ResultadoExamen resultadoExamen) {

       Paciente findPatient =  pacienteService.getByUUID(resultadoExamen.getPaciente().getUuid());
       Examen findExamen =  examenService.getExamen(resultadoExamen.getExamen().getId());
       BeanUser usuarioRegistro = userService.getByUUID(resultadoExamen.getUsuarioRegistro().getUUID());

       resultadoExamen.setUsuarioRegistro(usuarioRegistro);
       resultadoExamen.setPaciente(findPatient);
       resultadoExamen.setExamen(findExamen);
       return resultadoExamenService.createResultado(resultadoExamen);

    }
    @Transactional
    public ResultadoExamen updateResultado(ResultadoExamen resultadoExamen) {

        Paciente findPatient =  pacienteService.getByUUID(resultadoExamen.getPaciente().getUuid());
        Examen findExamen =  examenService.getExamen(resultadoExamen.getExamen().getId());
        BeanUser usuarioRegistro = userService.getByUUID(resultadoExamen.getUsuarioRegistro().getUUID());

        resultadoExamen.setUsuarioRegistro(usuarioRegistro);
        resultadoExamen.setPaciente(findPatient);
        resultadoExamen.setExamen(findExamen);
        return resultadoExamenService.updateResultado(resultadoExamen);

    }








}
