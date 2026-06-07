package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestraRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ParametroEstudioMuestraService {

    private final ParametroEstudioMuestraRepository repository;

    @Transactional(readOnly = true)
    public List<ParametroEstudioMuestra> getByTipo(Long idTipo) {
        return repository.findAllByTipoEstudioMuestra_Id(idTipo);
    }

    @Transactional(readOnly = true)
    public ParametroEstudioMuestra getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el parámetro de estudio de muestra"));
    }

    @Transactional(rollbackFor = Exception.class)
    public ParametroEstudioMuestra create(ParametroEstudioMuestra parametro) {
        repository.findByTipoEstudioMuestra_IdAndNombreIgnoreCase(
                parametro.getTipoEstudioMuestra().getId(), parametro.getNombre()
        ).ifPresent(p -> {
            throw new ObjConflictException("Ya existe un parámetro con ese nombre en este tipo de estudio");
        });
        return repository.save(parametro);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParametroEstudioMuestra update(Long id, ParametroEstudioMuestra datos) {
        ParametroEstudioMuestra paramBD = getById(id);
        if (!datos.getNombre().equalsIgnoreCase(paramBD.getNombre())) {
            repository.findByTipoEstudioMuestra_IdAndNombreIgnoreCase(
                    paramBD.getTipoEstudioMuestra().getId(), datos.getNombre()
            ).ifPresent(p -> {
                throw new ObjConflictException("Ya existe un parámetro con ese nombre en este tipo de estudio");
            });
            paramBD.setNombre(datos.getNombre());
        }
        paramBD.setUnidad(datos.getUnidad());
        paramBD.setTipo(datos.getTipo());
        paramBD.setValorMinimo(datos.getValorMinimo());
        paramBD.setValorMaximo(datos.getValorMaximo());
        return repository.save(paramBD);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParametroEstudioMuestra param = getById(id);
        repository.delete(param);
    }
}
