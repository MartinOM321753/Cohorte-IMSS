package imss.gob.mx.cohorte.modules.permisos;

import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolPermisoRepository extends JpaRepository<RolPermiso, Long> {
    List<RolPermiso> findAllByRol(Role rol);
    List<RolPermiso> findAllByRolIn(List<Role> roles);
    boolean existsByRolAndPermiso(Role rol, Permiso permiso);
    void deleteAllByRol(Role rol);
}
