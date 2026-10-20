package imss.gob.mx.cohorte.modules.cuestionarios.carga;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TareaCargaVolcadoRepository extends JpaRepository<TareaCargaVolcado, Long> {

    /**
     * La tarea con sus filas omitidas ya cargadas.
     *
     * <p>Se trae de una vez porque es como se consulta siempre: la pantalla que
     * pregunta «¿ya acabó?» quiere, en la misma respuesta, qué quedó fuera.</p>
     */
    @Query("select t from TareaCargaVolcado t left join fetch t.filasOmitidas "
            + "where t.id = :id")
    Optional<TareaCargaVolcado> conFilasOmitidas(@Param("id") Long id);

    /** Las últimas cargas de una institución, para la bitácora de la pantalla. */
    List<TareaCargaVolcado> findTop10ByInstitucion_IdOrderByFechaInicioDesc(Long idInstitucion);

    /**
     * Si ese instrumento ya tiene una carga corriendo.
     *
     * <p>Es lo que impide que un doble clic —o un reintento de quien no ve avance—
     * lance dos cargas del mismo archivo a la vez.</p>
     */
    boolean existsByInstitucion_IdAndTipoCuestionario_IdAndEstado(
            Long idInstitucion, Long idTipo, EstadoTareaVolcado estado);
}
