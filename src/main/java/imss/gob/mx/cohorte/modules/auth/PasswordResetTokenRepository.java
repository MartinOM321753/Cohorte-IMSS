package imss.gob.mx.cohorte.modules.auth;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Devuelve el token más reciente de un usuario creado después de {@code desde}.
     * Se usa para verificar el límite de 1 solicitud por hora.
     */
    boolean existsByUsuarioAndCreadoEnAfter(BeanUser usuario, Instant desde);
}
