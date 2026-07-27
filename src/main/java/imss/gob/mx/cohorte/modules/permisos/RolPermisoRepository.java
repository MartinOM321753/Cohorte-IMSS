package imss.gob.mx.cohorte.modules.permisos;

import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolPermisoRepository extends JpaRepository<RolPermiso, Long> {
    List<RolPermiso> findAllByRol(Role rol);
    List<RolPermiso> findAllByRolIn(List<Role> roles);
    boolean existsByRolAndPermiso(Role rol, Permiso permiso);
    long countByRol(Role rol);

    @Modifying
    @Query("DELETE FROM RolPermiso rp WHERE rp.rol = :rol")
    void deleteAllByRol(@Param("rol") Role rol);

    void deleteByRolAndPermiso(Role rol, Permiso permiso);
}
