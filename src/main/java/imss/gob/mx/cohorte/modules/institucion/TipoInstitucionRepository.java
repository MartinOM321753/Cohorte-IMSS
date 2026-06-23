package imss.gob.mx.cohorte.modules.institucion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoInstitucionRepository extends JpaRepository<TipoInstitucion, Long> {
    List<TipoInstitucion> findAllByActivo(Boolean activo);
    Optional<TipoInstitucion> findByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
