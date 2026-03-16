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

    private final ResultadoEstudioRepository resultadoRepository;


    @Transactional(rollbackFor = Exception.class)
    public ResultadoEstudio create(ResultadoEstudio resultado){

        return resultadoRepository.save(resultado);

    }
    @Transactional(rollbackFor = Exception.class)
    public ResultadoEstudio update(ResultadoEstudio resultado){
        ResultadoEstudio resultadoBD = resultadoRepository.findById(resultado.getId()).orElseThrow(()-> new ObjNotFoundException("No se encontro el resultado de prueba"));

        resultadoBD.setEstudio(resultado.getEstudio());
        resultadoBD.setParametro(resultado.getParametro());
        resultadoBD.setValorNumerico(resultado.getValorNumerico());
        resultadoBD.setValorTexto(resultado.getValorTexto());
        return resultadoRepository.save(resultadoBD);
    }



}
