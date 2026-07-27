package imss.gob.mx.cohorte.modules.documentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface EstudioDocumentoRepository extends JpaRepository<EstudioDocumento, Long> {

    List<EstudioDocumento> findByEstudio_IdOrderByOrdenAsc(Long estudioId);

    void deleteByDocumento_Id(Long documentoId);

    @Modifying
    @Query("DELETE FROM EstudioDocumento ed WHERE ed.estudio.id = :estudioId")
    void deleteByEstudio_Id(@Param("estudioId") Long estudioId);
}
