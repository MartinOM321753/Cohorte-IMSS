package imss.gob.mx.cohorte.modules.documentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ResultadoExamenDocumentoRepository extends JpaRepository<ResultadoExamenDocumento, Long> {

    List<ResultadoExamenDocumento> findByResultadoExamen_IdOrderByDocumento_FechaSubidaDesc(Long resultadoId);

    void deleteByDocumento_Id(Long documentoId);

    @Modifying
    @Query("DELETE FROM ResultadoExamenDocumento r WHERE r.resultadoExamen.id = :resultadoId")
    void deleteByResultadoExamen_Id(@Param("resultadoId") Long resultadoId);
}
