package imss.gob.mx.cohorte.modules.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadMedidaRepository extends JpaRepository<UnidadMedida, Long> {
    List<UnidadMedida> findAllByInstitucion_IdOrderByNombreAsc(Long idInstitucion);
    List<UnidadMedida> findAllByInstitucion_IdAndActivoOrderByNombreAsc(Long idInstitucion, Boolean activo);
    Optional<UnidadMedida> findByIdAndInstitucion_Id(Long id, Long idInstitucion);
    Optional<UnidadMedida> findByNombreIgnoreCaseAndInstitucion_Id(String nombre, Long idInstitucion);
    boolean existsByNombreIgnoreCaseAndInstitucion_IdAndIdNot(String nombre, Long idInstitucion, Long id);
}
