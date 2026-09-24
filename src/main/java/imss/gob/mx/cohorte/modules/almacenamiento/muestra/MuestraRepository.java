package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MuestraRepository extends JpaRepository<Muestra, Long>, MuestraCursorRepository {
    Optional<Muestra> findByEtiquetaIgnoreCase(String etiqueta);
    Optional<Muestra> findByEtiquetaIgnoreCaseAndInstitucion_Id(String etiqueta, Long idInstitucion);
    List<Muestra> findAllByPaciente_Uuid(String uuid);
    List<Muestra> findAllByPaciente_Folio(String folio);
    List<Muestra> findAllByPosicionCajaIsNull();
    /** Muestras y alícuotas que referencian un tubo. Bloquea su eliminación. */
    long countByTuboMuestra_Id(Long idTuboMuestra);

    long countByPaciente_Uuid(String uuid);

    List<Muestra> findAllByInstitucion_Id(Long idInstitucion);
    Page<Muestra> findAllByInstitucion_Id(Long idInstitucion, Pageable pageable);
    List<Muestra> findAllByPaciente_UuidAndInstitucion_Id(String uuid, Long idInstitucion);
    long countByPaciente_UuidAndInstitucion_Id(String uuid, Long idInstitucion);

    /** Variante por conjunto de instituciones (atencion entre sedes del grupo). */
    long countByPaciente_UuidAndInstitucion_IdIn(String uuid, java.util.List<Long> idsInstituciones);

    List<Muestra> findAllByPaciente_UuidAndInstitucion_IdIn(String uuid, java.util.List<Long> idsInstituciones);
    long countByInstitucion_Id(Long idInstitucion);

    /** Muestras cuyo tenedor actual es la institución dada (biobanco en tiempo real). */
    List<Muestra> findAllByInstitucionActual_Id(Long idInstitucion);
    Page<Muestra> findAllByInstitucionActual_Id(Long idInstitucion, Pageable pageable);

    /** Alícuotas de un padre que están actualmente en una institución específica. */
    List<Muestra> findAllByMuestraPadre_IdAndInstitucionActual_Id(Long idMuestraPadre, Long idInstitucionActual);

    /** Alícuotas de un padre creadas en una institución específica. */
    List<Muestra> findAllByMuestraPadre_IdAndInstitucion_Id(Long idMuestraPadre, Long idInstitucion);

    /**
     * Vista default: muestras "actuales" — propias o en posesión.
     * Excluye muestras que solo tuve prestadas en el pasado y ya devolví, para
     * evitar que la lista crezca indefinidamente con el uso a lo largo del tiempo.
     * Para acceder al histórico completo, usar {@link #findAllVisiblesConHistoricoPorInstitucion}.
     */
    @Query("SELECT DISTINCT m FROM Muestra m WHERE m.institucion.id = :idInst "
         + "OR m.institucionActual.id = :idInst")
    List<Muestra> findAllVisiblesPorInstitucion(@Param("idInst") Long idInstitucion);

    /**
     * Vista extendida: propias + en posesión + histórico como destino de traslados no cancelados.
     * Se activa cuando el usuario pide explícitamente "mostrar histórico" (toggle en la UI).
     */
    @Query("SELECT DISTINCT m FROM Muestra m WHERE m.institucion.id = :idInst "
         + "OR m.institucionActual.id = :idInst "
         + "OR EXISTS (SELECT 1 FROM imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra t "
         + "WHERE t.muestra.id = m.id "
         + "AND t.institucionDestino.id = :idInst "
         + "AND t.estado <> imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.CANCELADO)")
    List<Muestra> findAllVisiblesConHistoricoPorInstitucion(@Param("idInst") Long idInstitucion);

    /**
     * Resuelve una etiqueta escaneada dentro de lo que la institución puede ver.
     *
     * <p>El alcance es el mismo de {@link #findAllVisiblesConHistoricoPorInstitucion}
     * —propia, en posesión, o destino de un traslado no cancelado— y no el de la
     * vista por omisión. Es a propósito: quien pasa una etiqueta por el lector
     * tiene el tubo en la mano, así que responder «no existe» porque el panel trae
     * el histórico apagado sería mentirle. La pantalla se encarga de encender el
     * filtro que haga falta para mostrarla.</p>
     *
     * <p>La etiqueta es única por institución, no globalmente: dos sedes pueden
     * tener una con el mismo texto. Por eso se acota al conjunto visible y se
     * ordena dando prioridad a la propia, que es la que el usuario espera.</p>
     */
    @Query("SELECT m FROM Muestra m WHERE UPPER(m.etiqueta) = UPPER(:etiqueta) AND ("
         + "  m.institucion.id = :idInst "
         + "  OR m.institucionActual.id = :idInst "
         + "  OR EXISTS (SELECT 1 FROM imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra t "
         + "     WHERE t.muestra.id = m.id "
         + "     AND t.institucionDestino.id = :idInst "
         + "     AND t.estado <> imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.CANCELADO)) "
         + "ORDER BY CASE WHEN m.institucion.id = :idInst THEN 0 ELSE 1 END, m.id ASC")
    List<Muestra> buscarVisiblesPorEtiqueta(@Param("etiqueta") String etiqueta,
                                            @Param("idInst") Long idInstitucion);

    @Query("SELECT COALESCE(MAX(m.numeroLote), 0) FROM Muestra m "
         + "WHERE m.paciente.folio = :folio "
         + "AND m.tuboMuestra.prefijoCodigo = :prefijo")
    int findMaxLoteByFolioAndTuboPrefix(@Param("folio") String folio, @Param("prefijo") String prefijo);

    /**
     * Muestras alojadas en cualquier posición de una caja. La rejilla del
     * visualizador 3D necesita la etiqueta de cada celda ocupada, y recorrer
     * PosicionCaja no la da: la referencia vive del lado de Muestra.
     */
    @Query("SELECT m FROM Muestra m WHERE m.posicionCaja.caja.id = :idCaja")
    List<Muestra> findAllByCaja_Id(@Param("idCaja") Long idCaja);

    boolean existsByMuestraPadre_IdAndTipoMuestra_IdAndTuboMuestra_IdAndInstitucion_Id(
            Long idMuestraPadre, Long idTipoMuestra, Long idTuboMuestra, Long idInstitucion);

    /**
     * Alícuotas que una institución ya creó de una padre con un tubo dado: el
     * lote, en el sentido de los huecos que ese tubo define.
     *
     * <p>Se usa para saber qué números de alícuota están ocupados y cuáles
     * quedan libres, de modo que un lote pueda completarse en varias tandas sin
     * repetir etiqueta.</p>
     */
    List<Muestra> findAllByMuestraPadre_IdAndTuboMuestra_IdAndInstitucion_Id(
            Long idMuestraPadre, Long idTuboMuestra, Long idInstitucion);

    // ── Contabilidad de volumen ──────────────────────────────────────────────

    /** Alícuotas creadas y todavía sin ubicar: lo que la padre tiene reservado. */
    long countByMuestraPadre_IdAndFechaMaterializacionIsNull(Long idMuestraPadre);

    /** Alícuotas sin ubicar de una padre, para ubicarlas en bloque. */
    List<Muestra> findAllByMuestraPadre_IdAndFechaMaterializacionIsNullOrderByNumeroAlicuotaAsc(
            Long idMuestraPadre);

    /**
     * Comprometido real de una muestra padre, recalculado desde sus alícuotas.
     *
     * <p>{@code valor_comprometido} está desnormalizado —se mantiene al crear el
     * lote y al materializar— porque calcularlo en cada lectura provocaría un
     * N+1 en el listado paginado, que mapea cada fila con un mapper estático
     * usado en 23 sitios. Esta consulta es la red de seguridad: permite
     * comprobar el invariante en las pruebas y reparar el dato si se desvía.</p>
     */
    @Query("SELECT COALESCE(SUM(a.valor), 0) FROM Muestra a "
         + "WHERE a.muestraPadre.id = :idPadre "
         + "AND a.fechaMaterializacion IS NULL "
         + "AND a.estadoMuestra <> imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra.BAJA")
    double sumarComprometidoReal(@Param("idPadre") Long idMuestraPadre);

    /**
     * Comprometido de varias padres a la vez, para no disparar una consulta por
     * fila al pintar un listado.
     */
    @Query("SELECT a.muestraPadre.id, COALESCE(SUM(a.valor), 0) FROM Muestra a "
         + "WHERE a.muestraPadre.id IN :idsPadre "
         + "AND a.fechaMaterializacion IS NULL "
         + "AND a.estadoMuestra <> imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra.BAJA "
         + "GROUP BY a.muestraPadre.id")
    List<Object[]> sumarComprometidoPorPadre(@Param("idsPadre") List<Long> idsPadre);

    /**
     * Vista del biobanco propio ocultando lo ya consumido.
     *
     * <p>Una padre agotada es un tubo que se fue a la basura: sigue siendo la
     * cabeza de la procedencia de sus alícuotas, pero no está en ningún
     * congelador y no tiene por qué ensuciar el inventario. Se llega a ella
     * desde cualquiera de sus alícuotas y desde el expediente del participante.</p>
     */
    @Query("SELECT m FROM Muestra m WHERE m.institucionActual.id = :idInst "
         + "AND (m.fechaAgotamiento IS NULL OR m.posicionCaja IS NOT NULL)")
    Page<Muestra> findEnBiobancoNoAgotadas(@Param("idInst") Long idInstitucion, Pageable pageable);

    @Query("SELECT m FROM Muestra m WHERE (m.institucion.id = :idInst OR m.institucionActual.id = :idInst) "
         + "AND (m.fechaAgotamiento IS NULL OR m.posicionCaja IS NOT NULL)")
    List<Muestra> findAllVisiblesNoAgotadasPorInstitucion(@Param("idInst") Long idInstitucion);

    /**
     * Cuáles de estas etiquetas ya existen en la institución.
     *
     * <p>Para la carga masiva: comprobarlas una por una serían tantas consultas
     * como viales traiga el archivo, y el choque hay que detectarlo antes de
     * escribir nada, no cuando salte {@code uk_muestra_etiqueta_institucion} a
     * mitad de la transacción.</p>
     */
    @Query("SELECT m.etiqueta FROM Muestra m WHERE m.institucion.id = :idInst "
         + "AND UPPER(m.etiqueta) IN :etiquetas")
    List<String> findEtiquetasExistentes(@Param("etiquetas") List<String> etiquetasEnMayusculas,
                                         @Param("idInst") Long idInstitucion);
}
