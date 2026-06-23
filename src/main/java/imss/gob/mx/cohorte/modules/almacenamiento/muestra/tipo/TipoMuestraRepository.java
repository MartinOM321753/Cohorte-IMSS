package imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoMuestraRepository extends JpaRepository<TipoMuestra, Long> {

    List<TipoMuestra> findAllByActivoTrueOrderByNombreAsc();

    Optional<TipoMuestra> findByNombreIgnoreCase(String nombre);

    List<TipoMuestra> findAllByInstitucion_IdOrderByNombreAsc(Long idInstitucion);

    List<TipoMuestra> findAllByInstitucion_IdAndActivoTrueOrderByNombreAsc(Long idInstitucion);

    Optional<TipoMuestra> findByNombreIgnoreCaseAndInstitucion_Id(String nombre, Long idInstitucion);
}
