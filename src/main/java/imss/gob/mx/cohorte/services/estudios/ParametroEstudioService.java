package imss.gob.mx.cohorte.services.estudios;


import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudioRepository;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ParametroEstudioService {

    private final TipoEstudioRepository tipoEstudioRepository;
    private final ParametroEstudioRepository parametroRepository;


    @Transactional(readOnly = true)
    public ParametroEstudio getOne(Long id){return parametroRepository.findById(id).orElseThrow(()-> new ObjNotFoundException("No se encontro el valor solicitado"));}
    @Transactional(readOnly = true)
    public List<ParametroEstudio> getAll(){return parametroRepository.findAll();}

    @Transactional(rollbackFor = Exception.class)
    public ParametroEstudio create(ParametroEstudio parametroEstudio){

        TipoEstudio tipo = tipoEstudioRepository.findById(parametroEstudio.getTipoEstudio().getId()).orElseThrow(()-> new ObjNotFoundException(("No se encontro el tipo de estudio")));
        Optional<ParametroEstudio> parametro = parametroRepository.findByNombre( parametroEstudio.getNombre());
        if (parametro.isPresent()) throw new RuntimeException("Ya existe un parametro con ese nombre");

        parametroEstudio.setTipoEstudio(tipo);
        return parametroRepository.save(parametroEstudio);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParametroEstudio update(ParametroEstudio parametroEstudio){

        ParametroEstudio parametroDB = parametroRepository.findById(parametroEstudio.getId()).orElseThrow(()-> new ObjNotFoundException(("No se encontro el parametro de estudio")));
        TipoEstudio tipo = tipoEstudioRepository.findById(parametroEstudio.getTipoEstudio().getId()).orElseThrow(()-> new ObjNotFoundException(("No se encontro el tipo de estudio")));

      if (!parametroEstudio.getNombre().equals(parametroDB.getNombre())) {
          Optional<ParametroEstudio> parametro = parametroRepository.findByNombre( parametroEstudio.getNombre());

          if (parametro.isPresent()) throw new RuntimeException("Ya existe un parametro con ese nombre");
          parametroDB.setNombre(parametroEstudio.getNombre());
      }
        parametroDB.setTipoEstudio(tipo);
      parametroDB.setUnidad(parametroEstudio.getUnidad());

        return parametroRepository.save(parametroDB);
    }


}
