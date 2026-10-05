package imss.gob.mx.cohorte.modules.documentos_publicos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoPublicoRepository extends JpaRepository<DocumentoPublico, Long> {
    List<DocumentoPublico> findAllByIdInstitucionAndActivoOrderByFechaPublicacionDesc(Long idInstitucion, Boolean activo);
    List<DocumentoPublico> findAllByIdInstitucionOrderByFechaCreacionDesc(Long idInstitucion);
    Optional<DocumentoPublico> findByIdAndIdInstitucion(Long id, Long idInstitucion);
}
