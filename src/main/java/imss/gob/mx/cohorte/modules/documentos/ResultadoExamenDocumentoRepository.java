package imss.gob.mx.cohorte.modules.documentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultadoExamenDocumentoRepository extends JpaRepository<ResultadoExamenDocumento, Long> {

    List<ResultadoExamenDocumento> findByResultadoExamen_IdOrderByDocumento_FechaSubidaDesc(Long resultadoId);

    void deleteByDocumento_Id(Long documentoId);
}
