package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.examenes.ExamenService;
import imss.gob.mx.cohorte.services.examenes.ResultadoExamenService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
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
@RequireModulo(ModuloSistema.EXAMENES)
public class ExamenApplicationService {
    private final ExamenService examenService;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final ResultadoExamenService resultadoExamenService;
    private final InstitucionRepository institucionRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional
    public List<Examen> findAll() {
        return examenService.getAllExamenes();
    }
    @Transactional
    public Examen findOne(Long id) {
        Examen examen = examenService.getExamen(id);
        institucionContextService.verificarPertenece(examen.getInstitucion());
        return examen;
    }

    @Transactional
    public Examen create(Examen examen) {
        examen.setInstitucion(resolverInstitucion(examen.getInstitucion()));
        return examenService.createExamen(examen);
    }
    @Transactional
    public Examen update(Examen examen) {
        Examen existente = examenService.getExamen(examen.getId());
        institucionContextService.verificarPertenece(existente.getInstitucion());
        examen.setInstitucion(resolverInstitucion(examen.getInstitucion()));
        return examenService.updateExamen(examen);
    }

    private Institucion resolverInstitucion(Institucion referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new ObjNotFoundException("Falta informacion de institucion propietaria del examen");
        }
        Institucion institucion = institucionRepository.findById(referencia.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución con id: " + referencia.getId()));
        // Aislamiento por institución: no se permite registrar/editar examenes a nombre de otra institución
        institucionContextService.verificarPertenece(institucion);
        return institucion;
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
    public Page<ResultadoExamen> findAllResultadoByUUIDPaginado(String uuid, Pageable pageable) {
        return resultadoExamenService.findAllByUUIDPaginado(uuid, pageable);
    }

    @Transactional(readOnly = true)
    public long countResultadosByPacienteUuid(String uuid) {
        return resultadoExamenService.countByPacienteUuid(uuid);
    }
    @Transactional
    public ResultadoExamen createResultado(ResultadoExamen resultadoExamen) {

       Paciente findPatient =  pacienteService.getByUUID(resultadoExamen.getPaciente().getUuid(), institucionContextService.getIdInstitucionActual());
       Examen findExamen =  examenService.getExamen(resultadoExamen.getExamen().getId());
       BeanUser usuarioRegistro = userService.getByUUID(resultadoExamen.getUsuarioRegistro().getUUID());

       resultadoExamen.setUsuarioRegistro(usuarioRegistro);
       resultadoExamen.setPaciente(findPatient);
       resultadoExamen.setExamen(findExamen);
       return resultadoExamenService.createResultado(resultadoExamen);

    }
    @Transactional
    public ResultadoExamen updateResultado(ResultadoExamen resultadoExamen) {

        Paciente findPatient =  pacienteService.getByUUID(resultadoExamen.getPaciente().getUuid(), institucionContextService.getIdInstitucionActual());
        Examen findExamen =  examenService.getExamen(resultadoExamen.getExamen().getId());
        BeanUser usuarioRegistro = userService.getByUUID(resultadoExamen.getUsuarioRegistro().getUUID());

        resultadoExamen.setUsuarioRegistro(usuarioRegistro);
        resultadoExamen.setPaciente(findPatient);
        resultadoExamen.setExamen(findExamen);
        return resultadoExamenService.updateResultado(resultadoExamen);

    }








}
