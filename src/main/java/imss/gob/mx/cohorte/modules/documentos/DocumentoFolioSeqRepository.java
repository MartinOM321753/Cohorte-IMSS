package imss.gob.mx.cohorte.modules.documentos;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentoFolioSeqRepository extends JpaRepository<DocumentoFolioSeq, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DocumentoFolioSeq s WHERE s.idInstitucion = :idInst AND s.anio = :anio")
    Optional<DocumentoFolioSeq> findByInstitucionAndAnioForUpdate(@Param("idInst") Long idInst,
                                                                   @Param("anio") Integer anio);
}
