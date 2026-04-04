package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudioRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ResultadoService {

    private final ResultadoEstudioRepository resultadoRepository;

    public ResultadoEstudio findResultadoByParametroId(Long id) {
        return resultadoRepository.findByParametro_Id(id);
    }

    public ResultadoEstudio create(ResultadoEstudio resultado) {
        return resultadoRepository.save(resultado);
    }

    public ResultadoEstudio update(ResultadoEstudio resultado) {
        ResultadoEstudio resultadoBD = resultadoRepository.findById(resultado.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el resultado de prueba"));

        resultadoBD.setEstudio(resultado.getEstudio());
        resultadoBD.setParametro(resultado.getParametro());
        resultadoBD.setValorNumerico(resultado.getValorNumerico());
        resultadoBD.setValorTexto(resultado.getValorTexto());
        return resultadoRepository.save(resultadoBD);
    }

    public void delete(Long id) {
        ResultadoEstudio resultado = resultadoRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el resultado de estudio con id: " + id));

        if (resultado.getEstudio() == null) {
            throw new ObjConflictException("El resultado no está asociado a ningún estudio.");
        }

        resultadoRepository.deleteById(id);
    }
}