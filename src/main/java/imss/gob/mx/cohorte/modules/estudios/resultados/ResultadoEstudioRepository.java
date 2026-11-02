package imss.gob.mx.cohorte.modules.estudios.resultados;

import imss.gob.mx.cohorte.modules.analisis.CrudoProy;
import imss.gob.mx.cohorte.modules.analisis.ResultadoProyeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface ResultadoEstudioRepository extends JpaRepository<ResultadoEstudio, Long> {

    ResultadoEstudio findByParametro_Id(Long parametroId);

    boolean existsByParametro_Id(Long parametroId);

    /**
     * Proyección de los resultados de un parámetro para el motor de análisis.
     * Empuja a SQL los filtros grandes (institución del alcance, parámetro, rango
     * de fechas); el sexo, la edad por bucket y el rango de valor se resuelven en
     * el servicio porque dependen de cortes configurables por el analista.
     *
     * <p>Las fechas son opcionales: el patrón {@code (:x IS NULL OR …)} las hace
     * ignorables sin multiplicar los métodos del repositorio.</p>
     */
    @Query("SELECT new imss.gob.mx.cohorte.modules.analisis.ResultadoProyeccion("
         + "r.valorNumerico, r.valorTexto, r.valorBooleano, p.sexo, p.fechaNacimiento, "
         + "e.fechaEstudio, e.institucion.id) "
         + "FROM ResultadoEstudio r JOIN r.estudio e JOIN e.paciente pac JOIN pac.persona p "
         + "WHERE r.parametro.Id = :parametroId AND e.institucion.id IN :instituciones "
         + "AND (:desde IS NULL OR e.fechaEstudio >= :desde) "
         + "AND (:hasta IS NULL OR e.fechaEstudio <= :hasta)")
    List<ResultadoProyeccion> proyeccionPorParametro(
            @Param("parametroId")   Long parametroId,
            @Param("instituciones") Collection<Long> instituciones,
            @Param("desde")         LocalDateTime desde,
            @Param("hasta")         LocalDateTime hasta);

    /** Filas crudas (con folio) de un parámetro, para la exportación a nivel participante. */
    @Query("SELECT new imss.gob.mx.cohorte.modules.analisis.CrudoProy("
         + "pac.folio, e.institucion.id, p.sexo, p.fechaNacimiento, e.fechaEstudio, "
         + "r.valorNumerico, r.valorTexto, r.valorBooleano) "
         + "FROM ResultadoEstudio r JOIN r.estudio e JOIN e.paciente pac JOIN pac.persona p "
         + "WHERE r.parametro.Id = :parametroId AND e.institucion.id IN :instituciones "
         + "AND (:desde IS NULL OR e.fechaEstudio >= :desde) "
         + "AND (:hasta IS NULL OR e.fechaEstudio <= :hasta)")
    List<CrudoProy> crudoPorParametro(
            @Param("parametroId")   Long parametroId,
            @Param("instituciones") Collection<Long> instituciones,
            @Param("desde")         LocalDateTime desde,
            @Param("hasta")         LocalDateTime hasta);
}
