package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.services.estudios.ParametroEstudioService;
import imss.gob.mx.cohorte.services.estudios.ResultadoService;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class GestionEstudiosApplicationService {

    private final TipoService tipoService;
    private final ParametroEstudioService parametroService;
    private final ResultadoService resultadoService ;

    @Transactional
    public List<TipoEstudio> getAllByEstatus() {
        return tipoService.getAllByStatus(true);
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
    @Transactional
    public ParametroEstudio createParametro(ParametroEstudio parametroEstudio) {
        TipoEstudio tipoEstudio = tipoService.getOne(parametroEstudio.getTipoEstudio().getId());
        if (tipoEstudio == null) throw new RuntimeException("No se encontro el tipo de estudio");
        parametroEstudio.setTipoEstudio(tipoEstudio);
        return parametroService.create(parametroEstudio);
    }
    @Transactional
    public ParametroEstudio updateParametro(ParametroEstudio parametroEstudio) {
        TipoEstudio tipoEstudio = tipoService.getOne(parametroEstudio.getTipoEstudio().getId());
        if (tipoEstudio == null) throw new RuntimeException("No se encontro el tipo de estudio");
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
