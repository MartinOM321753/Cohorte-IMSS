package imss.gob.mx.cohorte.modules.cuestionarios.respuestas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RespuestaCuestionarioRepository extends JpaRepository<RespuestaCuestionario, Long> {

    /**
     * Las respuestas de una sección, con su variable y las opciones de esa variable.
     *
     * <p>Es lo que necesita una página del formulario ya contestada. El
     * {@code JOIN FETCH} sobre las opciones evita que pintar cuarenta preguntas
     * dispare cuarenta consultas más, que es lo que pasaría con las opciones en
     * LAZY si no se traen aquí.</p>
     */
    @Query("""
           SELECT DISTINCT r FROM RespuestaCuestionario r
           JOIN FETCH r.variable v
           LEFT JOIN FETCH v.opciones
           WHERE r.cuestionario.id = :idCuestionario AND v.seccion.id = :idSeccion
           ORDER BY v.orden ASC, r.ordenRespuesta ASC
           """)
    List<RespuestaCuestionario> findDeSeccion(@Param("idCuestionario") Long idCuestionario,
                                              @Param("idSeccion") Long idSeccion);

    List<RespuestaCuestionario> findAllByCuestionario_Id(Long idCuestionario);

    List<RespuestaCuestionario> findAllByCuestionario_IdAndVariable_Id(
            Long idCuestionario, Long idVariable);

    /** Las que quedaron sin leer. Alimenta la pantalla de recaptura. */
    @Query("""
           SELECT r FROM RespuestaCuestionario r
           JOIN FETCH r.variable v
           WHERE r.cuestionario.id = :idCuestionario
             AND r.marcaAusencia IN (
                 imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia.NO_AUTORIZADO,
                 imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia.ERROR_LECTURA)
           ORDER BY v.orden ASC
           """)
    List<RespuestaCuestionario> findPendientesDeLectura(@Param("idCuestionario") Long idCuestionario);

    @Query("""
           SELECT COUNT(r) FROM RespuestaCuestionario r
           WHERE r.cuestionario.id = :idCuestionario
             AND r.marcaAusencia IN (
                 imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia.NO_AUTORIZADO,
                 imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia.ERROR_LECTURA)
           """)
    long contarPendientesDeLectura(@Param("idCuestionario") Long idCuestionario);

    /** Cuántas respuestas con valor hay por variable. Es la frecuencia de una opción. */
    @Query("""
           SELECT r.valorCodigo, COUNT(r) FROM RespuestaCuestionario r
           WHERE r.variable.id = :idVariable
             AND r.marcaAusencia = imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia.NINGUNA
           GROUP BY r.valorCodigo
           """)
    List<Object[]> frecuenciaPorCodigo(@Param("idVariable") Long idVariable);

    boolean existsByVariable_Id(Long idVariable);

    void deleteAllByCuestionario_Id(Long idCuestionario);
}
