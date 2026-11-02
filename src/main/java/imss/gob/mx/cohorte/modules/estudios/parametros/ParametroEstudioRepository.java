package imss.gob.mx.cohorte.modules.estudios.parametros;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
@Repository
public interface ParametroEstudioRepository extends JpaRepository<ParametroEstudio, Long> {
    Optional<ParametroEstudio> findByTipoEstudio_IdAndNombreIgnoreCase(Long tipoEstudioId, String nombre);

    /**
     * Parámetros activos de tipos de estudio activos cuyas instituciones están en
     * el alcance dado. Alimenta el catálogo de métricas del módulo de análisis.
     * El JOIN FETCH trae el tipo de estudio para etiquetar sin N+1.
     */
    @Query("SELECT p FROM ParametroEstudio p JOIN FETCH p.tipoEstudio t "
         + "WHERE t.institucion.id IN :instituciones AND p.activo = true AND t.activo = true "
         + "ORDER BY t.nombre ASC, p.orden ASC, p.Id ASC")
    List<ParametroEstudio> findActivosByInstituciones(@Param("instituciones") Collection<Long> instituciones);

    List<ParametroEstudio> findAllByTipoEstudio_Nombre(String tipoEstudioNombre);

    /**
     * Los parámetros del tipo en el orden configurado. Es el finder que debe usar
     * todo lo que muestre o recorra parámetros: sin ORDER BY, el orden lo decide
     * la base y cambia entre motores y entre planes de consulta.
     */
    List<ParametroEstudio> findAllByTipoEstudio_IdOrderByOrdenAscIdAsc(Long tipoEstudioId);

    List<ParametroEstudio> findAllByTipoEstudio_Id(Long tipoEstudioId);
}
