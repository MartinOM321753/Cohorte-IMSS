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

    /**
     * Historial completo de préstamos de una muestra (cadena de custodia).
     * Ordenado por fechaTraslado DESC con fechaRegistro DESC como desempate,
     * para garantizar orden determinístico cuando dos traslados comparten
     * el mismo día/segundo (evita ambigüedad al reconstruir la cadena).
     */
    @Query("""
            SELECT t FROM TrasladoMuestra t
            WHERE t.muestra.id = :idMuestra
            ORDER BY t.fechaTraslado DESC, t.fechaRegistro DESC, t.id DESC
            """)
    List<TrasladoMuestra> findAllByMuestra_IdOrderByFechaTrasladoDesc(@Param("idMuestra") Long idMuestra);

    /** Todos los préstamos de un lote (padre + alícuotas). */
    List<TrasladoMuestra> findAllByGrupoTrasladoOrderByFechaTrasladoDesc(String grupoTraslado);

    /** Traslados que forman parte de la misma operación de devolución (padre + alícuotas). */
    List<TrasladoMuestra> findAllByGrupoDevolucionOrderByFechaTrasladoDesc(String grupoDevolucion);

    /** Traslados activos (no DEVUELTA ni CANCELADO) donde la institución es origen O destino. */
    @Query("""
            SELECT t FROM TrasladoMuestra t
            WHERE (t.institucionOrigen.id = :idInstitucion
               OR t.institucionDestino.id = :idInstitucion)
              AND t.estado NOT IN (
                  imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.DEVUELTA,
                  imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.CANCELADO)
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

    /** ¿Tiene la muestra algún traslado activo (no DEVUELTA ni CANCELADO)? */
    @Query("""
            SELECT COUNT(t) > 0 FROM TrasladoMuestra t
            WHERE t.muestra.id = :idMuestra
              AND t.estado NOT IN (
                  imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.DEVUELTA,
                  imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.CANCELADO)
            """)
    boolean existsTrasladoActivoByMuestra(@Param("idMuestra") Long idMuestra);

    @Query("""
            SELECT COUNT(t) > 0 FROM TrasladoMuestra t
            WHERE t.muestra.id = :idMuestra
              AND (t.institucionOrigen.id = :idInst OR t.institucionDestino.id = :idInst)
            """)
    boolean existsByMuestraAndInstitucion(@Param("idMuestra") Long idMuestra,
                                          @Param("idInst") Long idInst);

    boolean existsByMuestra_Id(Long idMuestra);

    // NOTA: intencionalmente NO se expone deleteAllByMuestra_Id. Los traslados son
    // historial de custodia inmutable — borrarlos violaría trazabilidad forense.
    // MuestraService.delete ya bloquea el borrado de muestras con existsByMuestra_Id.
}
