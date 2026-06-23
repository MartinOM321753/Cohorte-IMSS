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
public interface MuestraRepository extends JpaRepository<Muestra, Long> {
    Optional<Muestra> findByEtiquetaIgnoreCase(String etiqueta);
    Optional<Muestra> findByEtiquetaIgnoreCaseAndInstitucion_Id(String etiqueta, Long idInstitucion);
    List<Muestra> findAllByPaciente_Uuid(String uuid);
    List<Muestra> findAllByPaciente_Folio(String folio);
    List<Muestra> findAllByPosicionCajaIsNull();
    long countByPaciente_Uuid(String uuid);

    List<Muestra> findAllByInstitucion_Id(Long idInstitucion);
    Page<Muestra> findAllByInstitucion_Id(Long idInstitucion, Pageable pageable);
    List<Muestra> findAllByPaciente_UuidAndInstitucion_Id(String uuid, Long idInstitucion);
    long countByPaciente_UuidAndInstitucion_Id(String uuid, Long idInstitucion);
    long countByInstitucion_Id(Long idInstitucion);

    /** Muestras cuyo tenedor actual es la institución dada (biobanco en tiempo real). */
    List<Muestra> findAllByInstitucionActual_Id(Long idInstitucion);
    Page<Muestra> findAllByInstitucionActual_Id(Long idInstitucion, Pageable pageable);

    /** Alícuotas de un padre que están actualmente en una institución específica. */
    List<Muestra> findAllByMuestraPadre_IdAndInstitucionActual_Id(Long idMuestraPadre, Long idInstitucionActual);

    /** Alícuotas de un padre creadas en una institución específica. */
    List<Muestra> findAllByMuestraPadre_IdAndInstitucion_Id(Long idMuestraPadre, Long idInstitucion);

    /** Muestras propias + en posesión actual + historial de préstamos (visibilidad permanente). */
    @Query("SELECT DISTINCT m FROM Muestra m WHERE m.institucion.id = :idInst "
         + "OR m.institucionActual.id = :idInst "
         + "OR EXISTS (SELECT 1 FROM imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra t "
         + "WHERE t.muestra.id = m.id "
         + "AND t.institucionDestino.id = :idInst "
         + "AND t.estado <> imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.CANCELADO)")
    List<Muestra> findAllVisiblesPorInstitucion(@Param("idInst") Long idInstitucion);

    @Query("SELECT COALESCE(MAX(m.numeroLote), 0) FROM Muestra m "
         + "WHERE m.paciente.folio = :folio "
         + "AND m.tuboMuestra.prefijoCodigo = :prefijo")
    int findMaxLoteByFolioAndTuboPrefix(@Param("folio") String folio, @Param("prefijo") String prefijo);

    boolean existsByMuestraPadre_IdAndTipoMuestra_IdAndTuboMuestra_IdAndInstitucion_Id(
            Long idMuestraPadre, Long idTipoMuestra, Long idTuboMuestra, Long idInstitucion);
}
