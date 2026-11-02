package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

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
public interface ResultadoEstudioMuestraRepository extends JpaRepository<ResultadoEstudioMuestra, Long> {

    boolean existsByParametro_Id(Long parametroId);

    /**
     * Proyección de los resultados de un parámetro de estudio de muestra para el
     * motor de análisis. El participante y la institución se alcanzan vía la
     * muestra; la fecha es la del estudio.
     */
    @Query("SELECT new imss.gob.mx.cohorte.modules.analisis.ResultadoProyeccion("
         + "r.valorNumerico, r.valorTexto, r.valorBooleano, p.sexo, p.fechaNacimiento, "
         + "e.fechaEstudio, m.institucion.id) "
         + "FROM ResultadoEstudioMuestra r JOIN r.estudio e JOIN e.muestra m "
         + "JOIN m.paciente pac JOIN pac.persona p "
         + "WHERE r.parametro.id = :parametroId AND m.institucion.id IN :instituciones "
         + "AND (:desde IS NULL OR e.fechaEstudio >= :desde) "
         + "AND (:hasta IS NULL OR e.fechaEstudio <= :hasta)")
    List<ResultadoProyeccion> proyeccionPorParametro(
            @Param("parametroId")   Long parametroId,
            @Param("instituciones") Collection<Long> instituciones,
            @Param("desde")         LocalDateTime desde,
            @Param("hasta")         LocalDateTime hasta);

    /** Filas crudas (con folio) de un parámetro de muestra, para la exportación a nivel participante. */
    @Query("SELECT new imss.gob.mx.cohorte.modules.analisis.CrudoProy("
         + "pac.folio, m.institucion.id, p.sexo, p.fechaNacimiento, e.fechaEstudio, "
         + "r.valorNumerico, r.valorTexto, r.valorBooleano) "
         + "FROM ResultadoEstudioMuestra r JOIN r.estudio e JOIN e.muestra m "
         + "JOIN m.paciente pac JOIN pac.persona p "
         + "WHERE r.parametro.id = :parametroId AND m.institucion.id IN :instituciones "
         + "AND (:desde IS NULL OR e.fechaEstudio >= :desde) "
         + "AND (:hasta IS NULL OR e.fechaEstudio <= :hasta)")
    List<CrudoProy> crudoPorParametro(
            @Param("parametroId")   Long parametroId,
            @Param("instituciones") Collection<Long> instituciones,
            @Param("desde")         LocalDateTime desde,
            @Param("hasta")         LocalDateTime hasta);
}
