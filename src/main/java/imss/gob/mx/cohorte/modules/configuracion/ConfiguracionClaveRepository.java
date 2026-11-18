package imss.gob.mx.cohorte.modules.configuracion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionClaveRepository extends JpaRepository<ConfiguracionClave, Long> {

    Optional<ConfiguracionClave> findByInstitucion_IdAndClave(Long institucionId, String clave);
}
