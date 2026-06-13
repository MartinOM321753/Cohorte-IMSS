package imss.gob.mx.cohorte.modules.paciente;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FolioSecuenciaRepository extends JpaRepository<FolioSecuencia, Integer> {

    /**
     * Obtiene (con bloqueo de escritura pesimista) el contador del año indicado,
     * para incrementarlo de forma segura ante altas concurrentes del mismo año.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FolioSecuencia f where f.anio = :anio")
    Optional<FolioSecuencia> findByAnioForUpdate(Integer anio);
}
