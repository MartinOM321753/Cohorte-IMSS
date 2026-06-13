package imss.gob.mx.cohorte.modules.estudios.tipos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoEstudioRepository extends JpaRepository<TipoEstudio, Long> {

    // ── Filtrado por institución (uso normal) ────────────────────────────────
    List<TipoEstudio> findAllByInstitucion_Id(Long institucionId);
    List<TipoEstudio> findAllByActivoAndInstitucion_Id(Boolean activo, Long institucionId);
    Optional<TipoEstudio> findByNombreIgnoreCaseAndInstitucion_Id(String nombre, Long institucionId);
}
