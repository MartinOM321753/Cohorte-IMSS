package imss.gob.mx.cohorte.modules.examenes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Long> {

    Optional<Examen> findByParametro(String nombre);

    /** Exámenes activos cuyas instituciones están en el alcance dado (catálogo de métricas). */
    @Query("SELECT e FROM Examen e WHERE e.institucion.id IN :instituciones AND e.activo = true "
         + "ORDER BY e.parametro ASC")
    List<Examen> findActivosByInstituciones(@Param("instituciones") Collection<Long> instituciones);

    List<Examen> findAllByActivo(Boolean activo);

    Optional<Examen> findByIdAndActivo(Long id, Boolean activo);

    /** Catálogo de exámenes de una institución (aislamiento de datos). */
    List<Examen> findAllByActivoAndInstitucion_Id(Boolean activo, Long idInstitucion);

    List<Examen> findAllByInstitucion_Id(Long idInstitucion);

    boolean existsByParametroIgnoreCaseAndInstitucion_Id(String parametro, Long idInstitucion);

}
