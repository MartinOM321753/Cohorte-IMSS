package imss.gob.mx.cohorte.services.catalogo;

import imss.gob.mx.cohorte.modules.catalogo.UnidadMedida;
import imss.gob.mx.cohorte.modules.catalogo.UnidadMedidaRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnidadMedidaService {

    private final UnidadMedidaRepository repository;

    @Transactional(readOnly = true)
    public List<UnidadMedida> getAllActivas() {
        return repository.findAllByActivo(true);
    }

    @Transactional(readOnly = true)
    public List<UnidadMedida> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public UnidadMedida getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la unidad con id: " + id));
    }

    @Transactional
    public UnidadMedida create(UnidadMedida unidad) {
        String nombre = unidad.getNombre().trim();
        if (repository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new ObjConflictException("Ya existe una unidad de medida con el nombre: " + nombre);
        }
        unidad.setNombre(nombre);
        unidad.setActivo(true);
        return repository.save(unidad);
    }

    @Transactional
    public UnidadMedida update(Long id, UnidadMedida unidad) {
        UnidadMedida bd = getById(id);
        String nombre = unidad.getNombre().trim();
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ObjConflictException("Ya existe otra unidad de medida con el nombre: " + nombre);
        }
        bd.setNombre(nombre);
        if (unidad.getActivo() != null) {
            bd.setActivo(unidad.getActivo());
        }
        return repository.save(bd);
    }

    @Transactional
    public UnidadMedida toggleActivo(Long id) {
        UnidadMedida bd = getById(id);
        bd.setActivo(!bd.getActivo());
        return repository.save(bd);
    }
}
