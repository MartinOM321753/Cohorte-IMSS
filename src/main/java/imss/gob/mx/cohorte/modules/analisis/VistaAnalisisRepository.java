package imss.gob.mx.cohorte.modules.analisis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VistaAnalisisRepository extends JpaRepository<VistaAnalisis, Long> {

    List<VistaAnalisis> findAllByUsuarioUuidOrderByNombreAsc(String usuarioUuid);

    Optional<VistaAnalisis> findByUuidAndUsuarioUuid(String uuid, String usuarioUuid);
}
