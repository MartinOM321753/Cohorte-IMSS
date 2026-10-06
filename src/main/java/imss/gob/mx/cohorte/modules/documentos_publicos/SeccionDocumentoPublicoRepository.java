package imss.gob.mx.cohorte.modules.documentos_publicos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeccionDocumentoPublicoRepository extends JpaRepository<SeccionDocumentoPublico, Long> {
    List<SeccionDocumentoPublico> findAllByInstitucion_IdOrderByOrdenAscNombreAsc(Long idInstitucion);
    List<SeccionDocumentoPublico> findAllByInstitucion_IdAndActivoOrderByOrdenAscNombreAsc(Long idInstitucion, Boolean activo);
    Optional<SeccionDocumentoPublico> findByIdAndInstitucion_Id(Long id, Long idInstitucion);
    Optional<SeccionDocumentoPublico> findByNombreIgnoreCaseAndInstitucion_Id(String nombre, Long idInstitucion);
    boolean existsByNombreIgnoreCaseAndInstitucion_IdAndIdNot(String nombre, Long idInstitucion, Long id);
}
