package imss.gob.mx.cohorte.modules.permisos;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UsuarioPermisoRepository extends JpaRepository<UsuarioPermiso, Long> {
    List<UsuarioPermiso> findAllByUsuarioAndActivoTrue(BeanUser usuario);
    List<UsuarioPermiso> findAllByActivoTrueAndFechaFinBeforeAndFechaFinIsNotNull(LocalDateTime now);
    List<UsuarioPermiso> findAllByPermiso(Permiso permiso);
}
