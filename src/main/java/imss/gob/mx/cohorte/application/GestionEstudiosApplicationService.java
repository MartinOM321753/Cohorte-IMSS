package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import imss.gob.mx.cohorte.services.estudios.ParametroEstudioService;
import imss.gob.mx.cohorte.services.estudios.ResultadoService;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class GestionEstudiosApplicationService {

    private final TipoService tipoService;
    private final ParametroEstudioService parametroService;
    private final ResultadoService resultadoService;
    private final TipoEstudioRepository tipoEstudioRepository;

    @Transactional(readOnly = true)
    public List<TipoEstudio> getAllByEstatus() {
        return tipoService.getAllByStatus(true);
    }

    @Transactional(readOnly = true)
    public List<TipoEstudio> getAllTipos() {
        return tipoEstudioRepository.findAll();
    }
    @Transactional
    public TipoEstudio getByName(String nombre) {
        return tipoService.getByName(nombre);
    }
    @Transactional
    public TipoEstudio getOne(Long id) {
        return tipoService.getOne(id);
    }

    @Transactional
    public TipoEstudio createTipoService(TipoEstudio tipoEstudio) {
        return tipoService.create(tipoEstudio);
    }

    @Transactional
    public TipoEstudio update(TipoEstudio tipoEstudio) {
        return tipoService.update(tipoEstudio);
    }
    @Transactional
    public Boolean Active(Long id){
        return tipoService.Active(id);
    }
    //PARAMETROS
    @Transactional(readOnly = true)
    public List<ParametroEstudio> getParametrosByTipo(Long tipoEstudioId) {
        // No se valida el estado activo: los parámetros de un tipo deshabilitado
        // deben seguir siendo accesibles para editar estudios ya registrados.
        if (!tipoEstudioRepository.existsById(tipoEstudioId)) {
            throw new ObjNotFoundException("No se encontró el tipo de estudio");
        }
        return parametroService.getByTipoEstudio(tipoEstudioId);
    }

    @Transactional
    public ParametroEstudio createParametro(ParametroEstudio parametroEstudio) {
        TipoEstudio tipoEstudio = tipoService.getOne(parametroEstudio.getTipoEstudio().getId());
        if (tipoEstudio == null) throw new ObjNotFoundException("No se encontro el tipo de estudio");
        parametroEstudio.setTipoEstudio(tipoEstudio);
        return parametroService.create(parametroEstudio);
    }
    @Transactional
    public ParametroEstudio updateParametro(ParametroEstudio parametroEstudio) {
        TipoEstudio tipoEstudio = tipoService.getOne(parametroEstudio.getTipoEstudio().getId());
        if (tipoEstudio == null) throw new ObjNotFoundException("No se encontro el tipo de estudio");
        parametroEstudio.setTipoEstudio(tipoEstudio);
        return parametroService.update(parametroEstudio);
    }
    @Transactional
    public ParametroEstudio deleteParametro(Long id) {
        ResultadoEstudio resultadoEstudio = resultadoService.findResultadoByParametroId(id);
        if (resultadoEstudio != null) throw new RuntimeException("No se puede eliminar el parametro porque tiene resultados");
        return parametroService.delete(id);
    }




}
