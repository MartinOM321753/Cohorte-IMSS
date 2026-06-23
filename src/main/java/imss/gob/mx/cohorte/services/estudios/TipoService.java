package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
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
    private final InstitucionContextService institucionContextService;

    /** Todos los TipoEstudio de la institución actual, filtrados por estado. */
    public List<TipoEstudio> getAllByStatus(Boolean status) {
        return tipoEstudioRepository.findAllByActivoAndInstitucion_Id(
                status, institucionContextService.getIdInstitucionActual());
    }

    /** Todos los TipoEstudio de la institución actual (activos e inactivos). */
    public List<TipoEstudio> getAllByInstitucion() {
        return tipoEstudioRepository.findAllByInstitucion_Id(
                institucionContextService.getIdInstitucionActual());
    }

    public TipoEstudio getByName(String nombre) {
        return tipoEstudioRepository.findByNombreIgnoreCaseAndInstitucion_Id(
                nombre, institucionContextService.getIdInstitucionActual())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio solicitado"));
    }

    public TipoEstudio getOne(Long id) {
        TipoEstudio tipo = tipoEstudioRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio solicitado"));
        if (!tipo.getActivo()) {
            throw new ObjNotFoundException("El tipo de estudio no se encuentra activo");
        }
        institucionContextService.verificarPertenece(tipo.getInstitucion());
        return tipo;
    }

    public TipoEstudio create(TipoEstudio tipoEstudio) {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        Optional<TipoEstudio> existente = tipoEstudioRepository.findByNombreIgnoreCaseAndInstitucion_Id(
                tipoEstudio.getNombre(), idInstitucion);
        if (existente.isPresent()) {
            throw new ObjConflictException("Ya existe un tipo de estudio con ese nombre");
        }
        tipoEstudio.setInstitucion(institucionContextService.getInstitucionActual());
        tipoEstudio.setFechaCreacion(LocalDateTime.now());
        return tipoEstudioRepository.save(tipoEstudio);
    }

    public TipoEstudio update(TipoEstudio tipoEstudio) {
        TipoEstudio tipoBD = tipoEstudioRepository.findById(tipoEstudio.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio"));
        institucionContextService.verificarPertenece(tipoBD.getInstitucion());

        if (!tipoEstudio.getNombre().equalsIgnoreCase(tipoBD.getNombre())) {
            Long idInstitucion = institucionContextService.getIdInstitucionActual();
            Optional<TipoEstudio> duplicado = tipoEstudioRepository.findByNombreIgnoreCaseAndInstitucion_Id(
                    tipoEstudio.getNombre(), idInstitucion);
            if (duplicado.isPresent()) {
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
        institucionContextService.verificarPertenece(tipoEstudio.getInstitucion());
        tipoEstudio.setActivo(!tipoEstudio.getActivo());
        tipoEstudioRepository.save(tipoEstudio);
        return true;
    }
}
