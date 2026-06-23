package imss.gob.mx.cohorte.modules.institucion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitucionModuloRepository extends JpaRepository<InstitucionModulo, Long> {

    List<InstitucionModulo> findAllByInstitucion_Id(Long idInstitucion);

    List<InstitucionModulo> findAllByInstitucion_IdAndHabilitadoTrue(Long idInstitucion);

    Optional<InstitucionModulo> findByInstitucion_IdAndModulo(Long idInstitucion, ModuloSistema modulo);

    boolean existsByInstitucion_IdAndModuloAndHabilitadoTrue(Long idInstitucion, ModuloSistema modulo);
}
