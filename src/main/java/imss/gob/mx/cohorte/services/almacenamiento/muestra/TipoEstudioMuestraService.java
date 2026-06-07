package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.TipoEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.TipoEstudioMuestraRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class TipoEstudioMuestraService {

    private final TipoEstudioMuestraRepository repository;

    @Transactional(readOnly = true)
    public List<TipoEstudioMuestra> getAllActivos() {
        return repository.findAllByActivo(true);
    }

    @Transactional(readOnly = true)
    public List<TipoEstudioMuestra> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public TipoEstudioMuestra getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tipo de estudio de muestra"));
    }

    @Transactional(readOnly = true)
    public TipoEstudioMuestra getByIdActivo(Long id) {
        TipoEstudioMuestra tipo = getById(id);
        if (!tipo.getActivo()) {
            throw new ObjNotFoundException("El tipo de estudio de muestra no está activo");
        }
        return tipo;
    }

    @Transactional(rollbackFor = Exception.class)
    public TipoEstudioMuestra create(TipoEstudioMuestra tipo) {
        repository.findByNombreIgnoreCase(tipo.getNombre()).ifPresent(t -> {
            throw new ObjConflictException("Ya existe un tipo de estudio de muestra con ese nombre");
        });
        tipo.setFechaCreacion(LocalDateTime.now());
        tipo.setActivo(true);
        return repository.save(tipo);
    }

    @Transactional(rollbackFor = Exception.class)
    public TipoEstudioMuestra update(Long id, TipoEstudioMuestra datos) {
        TipoEstudioMuestra tipoBD = getById(id);
        if (!datos.getNombre().equalsIgnoreCase(tipoBD.getNombre())) {
            repository.findByNombreIgnoreCase(datos.getNombre()).ifPresent(t -> {
                throw new ObjConflictException("Ya existe un tipo de estudio de muestra con ese nombre");
            });
            tipoBD.setNombre(datos.getNombre());
        }
        tipoBD.setDescripcion(datos.getDescripcion());
        return repository.save(tipoBD);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean toggleActivo(Long id) {
        TipoEstudioMuestra tipo = getById(id);
        tipo.setActivo(!tipo.getActivo());
        repository.save(tipo);
        return tipo.getActivo();
    }
}
