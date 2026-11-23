package imss.gob.mx.cohorte.modules.solicitud;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface SolicitudCorreoVerificacionRepository extends JpaRepository<SolicitudCorreoVerificacion,String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from SolicitudCorreoVerificacion v where v.id=:id")
    Optional<SolicitudCorreoVerificacion> locked(@Param("id") String id);
    boolean existsByEmailAndCreatedAtAfter(String email, Instant createdAt);
    long countByEmailAndCreatedAtAfter(String email, Instant createdAt);
    long countByIpAndCreatedAtAfter(String ip, Instant createdAt);
    void deleteByExpiresAtBefore(Instant expires);
}
