package imss.gob.mx.cohorte.services.catalogo;

import imss.gob.mx.cohorte.modules.catalogo.UnidadMedida;
import imss.gob.mx.cohorte.modules.catalogo.UnidadMedidaRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
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
    private final InstitucionContextService institucionContextService;

    private Long myInstId() {
        return institucionContextService.getIdInstitucionActual();
    }

    @Transactional(readOnly = true)
    public List<UnidadMedida> getAllActivas() {
        return repository.findAllByInstitucion_IdAndActivoOrderByNombreAsc(myInstId(), true);
    }

    @Transactional(readOnly = true)
    public List<UnidadMedida> getAll() {
        return repository.findAllByInstitucion_IdOrderByNombreAsc(myInstId());
    }

    @Transactional(readOnly = true)
    public UnidadMedida getById(Long id) {
        return repository.findByIdAndInstitucion_Id(id, myInstId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la unidad con id: " + id));
    }

    @Transactional
    public UnidadMedida create(UnidadMedida unidad) {
        Long idInst = myInstId();
        String nombre = unidad.getNombre().trim();
        if (repository.findByNombreIgnoreCaseAndInstitucion_Id(nombre, idInst).isPresent()) {
            throw new ObjConflictException("Ya existe una unidad de medida con el nombre: " + nombre);
        }

        Institucion institucion = institucionContextService.getInstitucionActual();
        unidad.setNombre(nombre);
        unidad.setActivo(true);
        unidad.setInstitucion(institucion);
        return repository.save(unidad);
    }

    @Transactional
    public UnidadMedida update(Long id, UnidadMedida unidad) {
        UnidadMedida bd = getById(id);
        Long idInst = myInstId();
        String nombre = unidad.getNombre().trim();
        if (repository.existsByNombreIgnoreCaseAndInstitucion_IdAndIdNot(nombre, idInst, id)) {
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
