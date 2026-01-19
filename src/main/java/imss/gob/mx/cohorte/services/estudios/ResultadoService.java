package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudioRepository;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudioRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ResultadoService {
    private final EstudioMedicoRepository  estudioRepository;
    private final ParametroEstudioRepository parametroRepository;
    private final ResultadoEstudioRepository resultadoRepository;

    @Transactional(readOnly = true)
    public ResultadoEstudio getOne(Long id){return resultadoRepository.findById(id).orElseThrow(()->new ObjNotFoundException("No se encontro el valor"));}

    @Transactional(readOnly = true)
    public List<ResultadoEstudio> getAll(Long id){return resultadoRepository.findAll();}

    @Transactional(rollbackFor = Exception.class)
    public ResultadoEstudio create(ResultadoEstudio resultado){

         EstudioMedico estudio = estudioRepository.findById(resultado.getEstudio().getId()).orElseThrow(()-> new ObjNotFoundException("No se encontro el estudio medico"));
         ParametroEstudio parametro = parametroRepository.findById(resultado.getParametro().getId()).orElseThrow(()-> new ObjNotFoundException("No se encontro el parametro de estudio"));
        if (!parametro.getTipoEstudio().getNombre().equals(estudio.getTipoEstudio().getNombre())) throw new ObjNotFoundException("El tipo de estudio no coincide con el tipo de prueba");

        resultado.setParametro(parametro);
        resultado.setEstudio(estudio);

        return resultadoRepository.save(resultado);

    }
    @Transactional(rollbackFor = Exception.class)
    public ResultadoEstudio update(ResultadoEstudio resultado){
        ResultadoEstudio resultadoBD = resultadoRepository.findById(resultado.getId()).orElseThrow(()-> new ObjNotFoundException("No se encontro el resultado de prueba"));
        EstudioMedico estudio = estudioRepository.findById(resultado.getEstudio().getId()).orElseThrow(()-> new ObjNotFoundException("No se encontro el estudio medico"));
        ParametroEstudio parametro = parametroRepository.findById(resultado.getParametro().getId()).orElseThrow(()-> new ObjNotFoundException("No se encontro el parametro de estudio"));
        if (!parametro.getTipoEstudio().getNombre().equals(estudio.getTipoEstudio().getNombre())) throw new ObjNotFoundException("El tipo de estudio no coincide con el tipo de prueba");

        resultadoBD.setParametro(parametro);
        resultadoBD.setEstudio(estudio);

        resultadoBD.setValorNumerico(resultado.getValorNumerico());
        resultadoBD.setValorTexto(resultado.getValorTexto());


        return resultadoRepository.save(resultadoBD);

    }

}
