package imss.gob.mx.cohorte.services.documentos_publicos;

import imss.gob.mx.cohorte.modules.documentos_publicos.CategoriaDocumentoPublico;
import imss.gob.mx.cohorte.modules.documentos_publicos.CategoriaDocumentoPublicoRepository;
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
public class CategoriaDocumentoPublicoService {

    private final CategoriaDocumentoPublicoRepository repository;
    private final InstitucionContextService institucionContextService;

    private Long myInstId() {
        return institucionContextService.getIdInstitucionActual();
    }

    @Transactional(readOnly = true)
    public List<CategoriaDocumentoPublico> getAllActivas() {
        return repository.findAllByInstitucion_IdAndActivoOrderByNombreAsc(myInstId(), true);
    }

    @Transactional(readOnly = true)
    public List<CategoriaDocumentoPublico> getAll() {
        return repository.findAllByInstitucion_IdOrderByNombreAsc(myInstId());
    }

    @Transactional(readOnly = true)
    public CategoriaDocumentoPublico getById(Long id) {
        return repository.findByIdAndInstitucion_Id(id, myInstId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la categoría con id: " + id));
    }

    @Transactional
    public CategoriaDocumentoPublico create(String nombre) {
        Long idInst = myInstId();
        String trimmed = nombre.trim();
        if (repository.findByNombreIgnoreCaseAndInstitucion_Id(trimmed, idInst).isPresent()) {
            throw new ObjConflictException("Ya existe una categoría con el nombre: " + trimmed);
        }
        Institucion institucion = institucionContextService.getInstitucionActual();
        CategoriaDocumentoPublico cat = new CategoriaDocumentoPublico();
        cat.setNombre(trimmed);
        cat.setActivo(true);
        cat.setInstitucion(institucion);
        return repository.save(cat);
    }

    @Transactional
    public CategoriaDocumentoPublico update(Long id, String nombre) {
        CategoriaDocumentoPublico bd = getById(id);
        Long idInst = myInstId();
        String trimmed = nombre.trim();
        if (repository.existsByNombreIgnoreCaseAndInstitucion_IdAndIdNot(trimmed, idInst, id)) {
            throw new ObjConflictException("Ya existe otra categoría con el nombre: " + trimmed);
        }
        bd.setNombre(trimmed);
        return repository.save(bd);
    }

    @Transactional
    public CategoriaDocumentoPublico toggleActivo(Long id) {
        CategoriaDocumentoPublico bd = getById(id);
        bd.setActivo(!bd.getActivo());
        return repository.save(bd);
    }

    @Transactional(readOnly = true)
    public List<CategoriaDocumentoPublico> getAllActivasByInstitucion(Long idInstitucion) {
        return repository.findAllByInstitucion_IdAndActivoOrderByNombreAsc(idInstitucion, true);
    }
}
