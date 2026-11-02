package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParametroEstudioMuestraRepository extends JpaRepository<ParametroEstudioMuestra, Long> {

    /** Parámetros de estudios de muestra de tipos activos en el alcance (catálogo de métricas). */
    @Query("SELECT p FROM ParametroEstudioMuestra p JOIN FETCH p.tipoEstudioMuestra t "
         + "WHERE t.institucion.id IN :instituciones AND t.activo = true "
         + "ORDER BY t.nombre ASC, p.id ASC")
    List<ParametroEstudioMuestra> findActivosByInstituciones(@Param("instituciones") Collection<Long> instituciones);

    /** Los parámetros del tipo en el orden configurado. Ver la nota del repositorio de estudios. */
    List<ParametroEstudioMuestra> findAllByTipoEstudioMuestra_IdOrderByOrdenAscIdAsc(Long idTipo);

    List<ParametroEstudioMuestra> findAllByTipoEstudioMuestra_Id(Long idTipo);

    Optional<ParametroEstudioMuestra> findByTipoEstudioMuestra_IdAndNombreIgnoreCase(Long idTipo, String nombre);
}
