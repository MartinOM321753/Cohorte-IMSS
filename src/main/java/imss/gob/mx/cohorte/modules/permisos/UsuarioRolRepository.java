package imss.gob.mx.cohorte.modules.permisos;

import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {
    List<UsuarioRol> findAllByUsuario(BeanUser usuario);
    boolean existsByUsuarioAndRol(BeanUser usuario, Role rol);
    void deleteAllByUsuario(BeanUser usuario);
    void deleteByUsuarioAndRol(BeanUser usuario, Role rol);
    long countByUsuario(BeanUser usuario);
}
