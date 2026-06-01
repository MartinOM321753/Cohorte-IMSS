package imss.gob.mx.cohorte.modules.almacenamiento.almacen;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlmacenRepository extends JpaRepository<Almacen, Long> {

    Optional<Almacen> findByNombreIgnoreCase(String nombre);

    List<Almacen> findAllByActivo(Boolean activo);

    Optional<Almacen> findFirstByEncargado_UUIDAndActivoTrue(String uuid);

    List<Almacen> findAllByEncargado_UUIDAndActivoTrue(String uuid);

    List<Almacen> findAllByEncargado_UUID(String uuid);
}
