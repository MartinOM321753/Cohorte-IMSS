package imss.gob.mx.cohorte.modules.almacenamiento.entrega;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntregaMuestraRepository extends JpaRepository<EntregaMuestra, Long> {

    /** Historial de entregas de una muestra, más reciente primero. */
    @Query("""
            SELECT e FROM EntregaMuestra e
            WHERE e.muestra.id = :idMuestra
            ORDER BY e.fechaEntrega DESC, e.fechaRegistro DESC, e.id DESC
            """)
    List<EntregaMuestra> findHistorialByMuestra(@Param("idMuestra") Long idMuestra);

    /** Entregas de un lote (padre + alícuotas entregados juntos). */
    List<EntregaMuestra> findAllByGrupoEntregaOrderByFechaEntregaDesc(String grupoEntrega);

    /** Entregas activas (ENTREGADA) de una institución. */
    @Query("""
            SELECT e FROM EntregaMuestra e
            WHERE e.institucion.id = :idInstitucion
              AND e.estado = imss.gob.mx.cohorte.modules.almacenamiento.entrega.EstadoEntrega.ENTREGADA
            ORDER BY e.fechaEntrega DESC
            """)
    List<EntregaMuestra> findActivasByInstitucion(@Param("idInstitucion") Long idInstitucion);

    /** Todas las entregas (activas + históricas) de una institución, paginadas. */
    @Query("""
            SELECT e FROM EntregaMuestra e
            WHERE e.institucion.id = :idInstitucion
            ORDER BY e.fechaEntrega DESC
            """)
    Page<EntregaMuestra> findAllByInstitucionPaginado(@Param("idInstitucion") Long idInstitucion, Pageable pageable);

    /** ¿Tiene la muestra una entrega activa (ENTREGADA)? */
    @Query("""
            SELECT COUNT(e) > 0 FROM EntregaMuestra e
            WHERE e.muestra.id = :idMuestra
              AND e.estado = imss.gob.mx.cohorte.modules.almacenamiento.entrega.EstadoEntrega.ENTREGADA
            """)
    boolean existsEntregaActivaByMuestra(@Param("idMuestra") Long idMuestra);

    boolean existsByMuestra_Id(Long idMuestra);
}
