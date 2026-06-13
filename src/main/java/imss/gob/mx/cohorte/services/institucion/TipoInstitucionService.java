package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.TipoInstitucion;
import imss.gob.mx.cohorte.modules.institucion.TipoInstitucionRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoInstitucionService {

    private final TipoInstitucionRepository repository;

    @Transactional(readOnly = true)
    public List<TipoInstitucion> getAllActivas() {
        return repository.findAllByActivo(true);
    }

    @Transactional(readOnly = true)
    public List<TipoInstitucion> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public TipoInstitucion getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tipo de institución con id: " + id));
    }

    @Transactional
    public TipoInstitucion create(TipoInstitucion tipo) {
        String nombre = tipo.getNombre().trim();
        if (repository.findByNombreIgnoreCase(nombre).isPresent()) {
            throw new ObjConflictException("Ya existe un tipo de institución con el nombre: " + nombre);
        }
        tipo.setNombre(nombre);
        tipo.setActivo(true);
        return repository.save(tipo);
    }

    @Transactional
    public TipoInstitucion update(Long id, TipoInstitucion tipo) {
        TipoInstitucion bd = getById(id);
        String nombre = tipo.getNombre().trim();
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ObjConflictException("Ya existe otro tipo de institución con el nombre: " + nombre);
        }
        bd.setNombre(nombre);
        if (tipo.getActivo() != null) {
            bd.setActivo(tipo.getActivo());
        }
        return repository.save(bd);
    }

    @Transactional
    public TipoInstitucion toggleActivo(Long id) {
        TipoInstitucion bd = getById(id);
        bd.setActivo(!bd.getActivo());
        return repository.save(bd);
    }
}
