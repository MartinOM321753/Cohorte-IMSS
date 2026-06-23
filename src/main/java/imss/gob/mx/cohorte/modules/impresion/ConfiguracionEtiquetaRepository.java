package imss.gob.mx.cohorte.modules.impresion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConfiguracionEtiquetaRepository extends JpaRepository<ConfiguracionEtiqueta, Long> {

    List<ConfiguracionEtiqueta> findByInstitucionIdOrderByNombre(Long institucionId);

    List<ConfiguracionEtiqueta> findByInstitucionIdAndActivoTrueOrderByNombre(Long institucionId);

    Optional<ConfiguracionEtiqueta> findByInstitucionIdAndPredeterminadaTrue(Long institucionId);

    Optional<ConfiguracionEtiqueta> findByIdAndInstitucionId(Long id, Long institucionId);

    boolean existsByInstitucionIdAndNombreAndIdNot(Long institucionId, String nombre, Long id);

    boolean existsByInstitucionIdAndNombre(Long institucionId, String nombre);
}
