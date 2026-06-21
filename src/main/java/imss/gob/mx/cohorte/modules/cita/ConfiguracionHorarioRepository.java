package imss.gob.mx.cohorte.modules.cita;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionHorarioRepository extends JpaRepository<ConfiguracionHorario, Long> {

    List<ConfiguracionHorario> findByInstitucionIdOrderByNombre(Long institucionId);

    Optional<ConfiguracionHorario> findByInstitucionIdAndActivaTrue(Long institucionId);

    Optional<ConfiguracionHorario> findByIdAndInstitucionId(Long id, Long institucionId);

    boolean existsByInstitucionIdAndNombre(Long institucionId, String nombre);

    boolean existsByInstitucionIdAndNombreAndIdNot(Long institucionId, String nombre, Long id);
}
