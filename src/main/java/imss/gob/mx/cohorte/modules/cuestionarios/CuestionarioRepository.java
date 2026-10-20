package imss.gob.mx.cohorte.modules.cuestionarios;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuestionarioRepository extends JpaRepository<Cuestionario, Long> {

    Optional<Cuestionario> findByPaciente_IdAndTipoCuestionario_Id(Long idPaciente, Long idTipo);

    boolean existsByPaciente_IdAndTipoCuestionario_Id(Long idPaciente, Long idTipo);

    List<Cuestionario> findAllByPaciente_UuidOrderByFechaRegistroDesc(String uuid);

    Optional<Cuestionario> findByIdAndInstitucion_Id(Long id, Long idInstitucion);

    Page<Cuestionario> findAllByInstitucion_IdOrderByFechaRegistroDesc(
            Long idInstitucion, Pageable pageable);

    Page<Cuestionario> findAllByInstitucion_IdAndTipoCuestionario_IdOrderByFechaRegistroDesc(
            Long idInstitucion, Long idTipo, Pageable pageable);

    List<Cuestionario> findAllByEtiqueta(String etiqueta);

    long countByTipoCuestionario_Id(Long idTipo);

    /**
     * Los cuestionarios con lecturas pendientes, los más afectados primero.
     *
     * <p>Es la bandeja de trabajo del capturista. Ordena por el contador guardado y
     * no por un conteo al vuelo: con dos mil respuestas por cuestionario, contarlas
     * para ordenar una lista de veinte renglones recorrería la tabla entera.</p>
     */
    @Query("""
           SELECT c FROM Cuestionario c
           WHERE c.institucion.id = :idInstitucion
             AND c.respuestasPendientes > 0
             AND c.estado <> imss.gob.mx.cohorte.modules.cuestionarios.EstadoCuestionario.ANULADO
           ORDER BY c.respuestasPendientes DESC, c.fechaRegistro DESC
           """)
    Page<Cuestionario> findConPendientes(@Param("idInstitucion") Long idInstitucion, Pageable pageable);

    @Query("""
           SELECT c.estado, COUNT(c) FROM Cuestionario c
           WHERE c.institucion.id = :idInstitucion AND c.tipoCuestionario.id = :idTipo
           GROUP BY c.estado
           """)
    List<Object[]> contarPorEstado(@Param("idInstitucion") Long idInstitucion,
                                   @Param("idTipo") Long idTipo);
}
