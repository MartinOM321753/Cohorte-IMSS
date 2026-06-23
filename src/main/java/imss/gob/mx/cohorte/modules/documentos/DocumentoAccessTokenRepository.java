package imss.gob.mx.cohorte.modules.documentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DocumentoAccessTokenRepository extends JpaRepository<DocumentoAccessToken, Long> {

    Optional<DocumentoAccessToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM DocumentoAccessToken t WHERE t.fechaExpiracion < :ahora")
    int eliminarExpirados(@Param("ahora") LocalDateTime ahora);
}
