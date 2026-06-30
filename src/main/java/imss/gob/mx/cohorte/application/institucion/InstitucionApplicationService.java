package imss.gob.mx.cohorte.application.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.services.institucion.InstitucionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InstitucionApplicationService {

    private final InstitucionService institucionService;

    @Transactional(readOnly = true)
    public Page<Institucion> getAllPaginado(Pageable pageable) {
        return institucionService.getAllPaginado(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Institucion> search(String texto, boolean soloActivas, Pageable pageable) {
        return institucionService.search(texto, soloActivas, pageable);
    }

    @Transactional(readOnly = true)
    public List<Institucion> getAllActivas() {
        return institucionService.getAllActivas();
    }

    @Transactional(readOnly = true)
    public Institucion getById(Long id) {
        return institucionService.getById(id);
    }

    @Transactional(readOnly = true)
    public Institucion getByUuid(String uuid) {
        return institucionService.getByUuid(uuid);
    }

    @Transactional(readOnly = true)
    public List<Institucion> getRaices() {
        return institucionService.getRaices();
    }

    @Transactional(readOnly = true)
    public List<Institucion> getHijas(Long idPadre) {
        return institucionService.getHijas(idPadre);
    }

    @Transactional
    public Institucion create(Institucion institucion, Long idTipoInstitucion, Long idInstitucionPadre, String uuidEncargado) {
        return institucionService.create(institucion, idTipoInstitucion, idInstitucionPadre, uuidEncargado);
    }

    @Transactional
    public Institucion update(Long id, Institucion institucion, Long idTipoInstitucion, Long idInstitucionPadre, String uuidEncargado) {
        return institucionService.update(id, institucion, idTipoInstitucion, idInstitucionPadre, uuidEncargado);
    }

    @Transactional
    public Institucion toggleActivo(Long id) {
        return institucionService.toggleActivo(id);
    }

    @Transactional(readOnly = true)
    public Set<Long> getIdsGestionables() {
        return institucionService.getIdsGestionables();
    }

    @Transactional(readOnly = true)
    public Set<Long> getIdsConEstadoGestionable() {
        return institucionService.getIdsConEstadoGestionable();
    }

    @Transactional(readOnly = true)
    public List<Institucion> getVisiblesParaJerarquia() {
        return institucionService.getVisiblesParaJerarquia();
    }
}
