package imss.gob.mx.cohorte.modules.almacenamiento.traslado;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrasladoMuestraRepository extends JpaRepository<TrasladoMuestra, Long> {

    /** Historial completo de préstamos de una muestra (cadena de custodia). */
    List<TrasladoMuestra> findAllByMuestra_IdOrderByFechaTrasladoDesc(Long idMuestra);

    /** Todos los préstamos de un lote (padre + alícuotas). */
    List<TrasladoMuestra> findAllByGrupoTrasladoOrderByFechaTrasladoDesc(String grupoTraslado);

    /** Traslados activos (no DEVUELTA) donde la institución es origen O destino. */
    @Query("""
            SELECT t FROM TrasladoMuestra t
            WHERE (t.institucionOrigen.id = :idInstitucion
                   OR t.institucionDestino.id = :idInstitucion)
              AND t.estado <> imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.DEVUELTA
            ORDER BY t.fechaTraslado DESC
            """)
    List<TrasladoMuestra> findActivosByInstitucion(@Param("idInstitucion") Long idInstitucion);

    /** Todos los traslados (activos + históricos) de una institución. */
    @Query("""
            SELECT t FROM TrasladoMuestra t
            WHERE t.institucionOrigen.id = :idInstitucion
               OR t.institucionDestino.id = :idInstitucion
            ORDER BY t.fechaTraslado DESC
            """)
    List<TrasladoMuestra> findAllByInstitucion(@Param("idInstitucion") Long idInstitucion);

    /** Paginado de traslados de una institución. */
    @Query("""
            SELECT t FROM TrasladoMuestra t
            WHERE t.institucionOrigen.id = :idInstitucion
               OR t.institucionDestino.id = :idInstitucion
            ORDER BY t.fechaTraslado DESC
            """)
    Page<TrasladoMuestra> findAllByInstitucionPaginado(@Param("idInstitucion") Long idInstitucion, Pageable pageable);

    /** ¿Tiene la muestra algún traslado activo (no DEVUELTA)? */
    @Query("""
            SELECT COUNT(t) > 0 FROM TrasladoMuestra t
            WHERE t.muestra.id = :idMuestra
              AND t.estado <> imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.DEVUELTA
            """)
    boolean existsTrasladoActivoByMuestra(@Param("idMuestra") Long idMuestra);

    List<TrasladoMuestra> findAllByOrderByFechaTrasladoDesc();
}
