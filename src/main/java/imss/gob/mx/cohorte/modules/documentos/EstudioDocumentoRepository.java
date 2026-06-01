package imss.gob.mx.cohorte.modules.documentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudioDocumentoRepository extends JpaRepository<EstudioDocumento, Long> {

    List<EstudioDocumento> findByEstudio_IdOrderByOrdenAsc(Long estudioId);

    void deleteByDocumento_Id(Long documentoId);
}
