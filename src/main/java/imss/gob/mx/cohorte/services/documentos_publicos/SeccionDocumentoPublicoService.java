package imss.gob.mx.cohorte.services.documentos_publicos;

import imss.gob.mx.cohorte.modules.documentos_publicos.SeccionDocumentoPublico;
import imss.gob.mx.cohorte.modules.documentos_publicos.SeccionDocumentoPublicoRepository;
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
public class SeccionDocumentoPublicoService {

    private final SeccionDocumentoPublicoRepository repository;
    private final InstitucionContextService institucionContextService;

    private Long myInstId() {
        return institucionContextService.getIdInstitucionActual();
    }

    @Transactional(readOnly = true)
    public List<SeccionDocumentoPublico> getAllActivas() {
        return repository.findAllByInstitucion_IdAndActivoOrderByOrdenAscNombreAsc(myInstId(), true);
    }

    @Transactional(readOnly = true)
    public List<SeccionDocumentoPublico> getAll() {
        return repository.findAllByInstitucion_IdOrderByOrdenAscNombreAsc(myInstId());
    }

    @Transactional(readOnly = true)
    public SeccionDocumentoPublico getById(Long id) {
        return repository.findByIdAndInstitucion_Id(id, myInstId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la sección con id: " + id));
    }

    @Transactional
    public SeccionDocumentoPublico create(String nombre, Integer orden) {
        Long idInst = myInstId();
        String trimmed = nombre.trim();
        if (repository.findByNombreIgnoreCaseAndInstitucion_Id(trimmed, idInst).isPresent()) {
            throw new ObjConflictException("Ya existe una sección con el nombre: " + trimmed);
        }
        Institucion institucion = institucionContextService.getInstitucionActual();
        SeccionDocumentoPublico sec = new SeccionDocumentoPublico();
        sec.setNombre(trimmed);
        sec.setOrden(orden);
        sec.setActivo(true);
        sec.setInstitucion(institucion);
        return repository.save(sec);
    }

    @Transactional
    public SeccionDocumentoPublico update(Long id, String nombre, Integer orden) {
        SeccionDocumentoPublico bd = getById(id);
        Long idInst = myInstId();
        String trimmed = nombre.trim();
        if (repository.existsByNombreIgnoreCaseAndInstitucion_IdAndIdNot(trimmed, idInst, id)) {
            throw new ObjConflictException("Ya existe otra sección con el nombre: " + trimmed);
        }
        bd.setNombre(trimmed);
        bd.setOrden(orden);
        return repository.save(bd);
    }

    @Transactional
    public SeccionDocumentoPublico toggleActivo(Long id) {
        SeccionDocumentoPublico bd = getById(id);
        bd.setActivo(!bd.getActivo());
        return repository.save(bd);
    }

    @Transactional(readOnly = true)
    public List<SeccionDocumentoPublico> getAllActivasByInstitucion(Long idInstitucion) {
        return repository.findAllByInstitucion_IdAndActivoOrderByOrdenAscNombreAsc(idInstitucion, true);
    }
}
