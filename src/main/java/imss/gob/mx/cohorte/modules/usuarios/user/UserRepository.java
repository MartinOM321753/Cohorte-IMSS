package imss.gob.mx.cohorte.modules.usuarios.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<BeanUser, Long> {

    List<BeanUser> findAllByActivo(Boolean activo);
    Optional<BeanUser> findByUsername(String username);
    Optional<BeanUser> findByUUID(String username);
    boolean existsByUUID(String uuid);

    /**
     * Busca un usuario activo cuya persona tenga el email indicado (case-insensitive).
     * Usado en el flujo de recuperación de contraseña.
     */
    @Query("SELECT u FROM BeanUser u WHERE LOWER(u.persona.email) = LOWER(:email) AND u.activo = true")
    Optional<BeanUser> findActiveUserByPersonaEmail(@Param("email") String email);
}
