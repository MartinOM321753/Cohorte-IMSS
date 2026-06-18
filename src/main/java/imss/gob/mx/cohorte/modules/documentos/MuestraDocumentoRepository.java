package imss.gob.mx.cohorte.modules.documentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MuestraDocumentoRepository extends JpaRepository<MuestraDocumento, Long> {

    List<MuestraDocumento> findByMuestra_IdOrderByDocumento_FechaSubidaDesc(Long muestraId);

    void deleteByDocumento_Id(Long documentoId);

    void deleteAllByMuestra_Id(Long muestraId);
}
