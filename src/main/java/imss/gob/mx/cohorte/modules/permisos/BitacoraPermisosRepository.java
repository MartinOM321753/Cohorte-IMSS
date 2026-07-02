package imss.gob.mx.cohorte.modules.permisos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BitacoraPermisosRepository extends JpaRepository<BitacoraPermisos, Long> {
    Page<BitacoraPermisos> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<BitacoraPermisos> findAllByUsuarioAfectadoUuidOrderByTimestampDesc(String uuid, Pageable pageable);
}
