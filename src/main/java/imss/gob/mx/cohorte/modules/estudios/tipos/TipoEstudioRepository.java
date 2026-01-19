package imss.gob.mx.cohorte.modules.estudios.tipos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface TipoEstudioRepository extends JpaRepository<TipoEstudio, Long> {
    Optional<TipoEstudio> findByNombre(String nombre);
}
