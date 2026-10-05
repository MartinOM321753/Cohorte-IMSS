package imss.gob.mx.cohorte.modules.documentos_publicos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaDocumentoPublicoRepository extends JpaRepository<CategoriaDocumentoPublico, Long> {
    List<CategoriaDocumentoPublico> findAllByInstitucion_IdOrderByNombreAsc(Long idInstitucion);
    List<CategoriaDocumentoPublico> findAllByInstitucion_IdAndActivoOrderByNombreAsc(Long idInstitucion, Boolean activo);
    Optional<CategoriaDocumentoPublico> findByIdAndInstitucion_Id(Long id, Long idInstitucion);
    Optional<CategoriaDocumentoPublico> findByNombreIgnoreCaseAndInstitucion_Id(String nombre, Long idInstitucion);
    boolean existsByNombreIgnoreCaseAndInstitucion_IdAndIdNot(String nombre, Long idInstitucion, Long id);
}
