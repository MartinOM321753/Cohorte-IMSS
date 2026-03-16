package imss.gob.mx.cohorte.modules.usuarios.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<BeanUser, Long> {

    List<BeanUser> findAllByActivo(Boolean activo);
    Optional <BeanUser> findByUsername(String username);
    Optional <BeanUser> findByUUID(String username);
    boolean existsByUUID(String uuid);

}
