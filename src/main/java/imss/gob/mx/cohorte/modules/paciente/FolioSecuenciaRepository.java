package imss.gob.mx.cohorte.modules.paciente;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FolioSecuenciaRepository extends JpaRepository<FolioSecuencia, FolioSecuenciaId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FolioSecuencia f where f.anio = :anio and f.idInstitucion = :idInst")
    Optional<FolioSecuencia> findByAnioAndIdInstitucionForUpdate(
            @Param("anio") Integer anio, @Param("idInst") Long idInstitucion);
}
