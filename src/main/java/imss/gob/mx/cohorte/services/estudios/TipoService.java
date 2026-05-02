package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TipoService {

    private final TipoEstudioRepository tipoEstudioRepository;

    public List<TipoEstudio> getAllByStatus(Boolean status) {
        return tipoEstudioRepository.findAllByActivo(status);
    }

    public TipoEstudio getByName(String nombre) {
        return tipoEstudioRepository.findByNombre(nombre)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio solicitado"));
    }

    public TipoEstudio getOne(Long id) {
        TipoEstudio tipo = tipoEstudioRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio solicitado"));
        if (!tipo.getActivo()) {
            throw new ObjNotFoundException("El tipo de estudio no se encuentra activo");
        }
        return tipo;
    }

    public TipoEstudio create(TipoEstudio tipoEstudio) {
        Optional<TipoEstudio> tipo = tipoEstudioRepository.findByNombre(tipoEstudio.getNombre());
        if (tipo.isPresent()) {
            throw new ObjConflictException("Ya existe un tipo de estudio con ese nombre");
        }
        tipoEstudio.setFechaCreacion(LocalDateTime.now());
        return tipoEstudioRepository.save(tipoEstudio);
    }

    public TipoEstudio update(TipoEstudio tipoEstudio) {
        TipoEstudio tipoBD = tipoEstudioRepository.findById(tipoEstudio.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio"));

        if (!tipoEstudio.getNombre().equals(tipoBD.getNombre())) {
            Optional<TipoEstudio> tipo = tipoEstudioRepository.findByNombre(tipoEstudio.getNombre());
            if (tipo.isPresent()) {
                throw new ObjConflictException("Ya existe un tipo de estudio con ese nombre");
            }
            tipoBD.setNombre(tipoEstudio.getNombre());
        }

        tipoBD.setParametros(tipoEstudio.getParametros());
        tipoBD.setDescripcion(tipoEstudio.getDescripcion());
        tipoBD.setActivo(tipoEstudio.getActivo());

        return tipoEstudioRepository.save(tipoBD);
    }

    public Boolean Active(Long id) {
        TipoEstudio tipoEstudio = tipoEstudioRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio"));
        tipoEstudio.setActivo(!tipoEstudio.getActivo());
        tipoEstudioRepository.save(tipoEstudio);
        return true;
    }
}
