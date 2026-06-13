package imss.gob.mx.cohorte.modules.estudios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EstudioMedicoRepository extends JpaRepository<EstudioMedico, Long> {

    /*
     * Las colecciones resultadoEstudio y adjuntos son LAZY + @BatchSize(30).
     * Hibernate las carga en queries secundarias con IN(...) agrupando hasta 30 IDs
     * por lote, lo que evita tanto el N+1 como el MultipleBagFetchException que
     * causaba el @EntityGraph con dos bags simultáneos.
     */

    List<EstudioMedico> findAllByOrderByFechaEstudioDesc();

    List<EstudioMedico> findAllByInstitucion_IdOrderByFechaEstudioDesc(Long idInstitucion);

    List<EstudioMedico> findAllByPaciente_UuidOrderByFechaEstudioDesc(String uuid);

    List<EstudioMedico> findAllByPaciente_UuidAndInstitucion_IdOrderByFechaEstudioDesc(String uuid, Long idInstitucion);

    /**
     * Variantes paginadas: sin fetch de colecciones para no forzar paginación en memoria.
     * El @BatchSize de la entidad acota el N+1 al tamaño de página.
     */
    org.springframework.data.domain.Page<EstudioMedico> findAllByOrderByFechaEstudioDesc(org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<EstudioMedico> findAllByInstitucion_IdOrderByFechaEstudioDesc(Long idInstitucion, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<EstudioMedico> findAllByPaciente_UuidOrderByFechaEstudioDesc(String uuid, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<EstudioMedico> findAllByPaciente_UuidAndInstitucion_IdOrderByFechaEstudioDesc(String uuid, Long idInstitucion, org.springframework.data.domain.Pageable pageable);

    /** Cuenta estudios que tienen al menos un resultado registrado. */
    @Query("SELECT COUNT(DISTINCT e.Id) FROM EstudioMedico e WHERE SIZE(e.resultadoEstudio) > 0")
    long countEstudiosConResultados();

    /** Cuenta estudios con al menos un resultado cuya fechaEstudio cae en el rango [inicio, fin]. */
    @Query("SELECT COUNT(DISTINCT e.Id) FROM EstudioMedico e " +
           "WHERE SIZE(e.resultadoEstudio) > 0 " +
           "AND e.fechaEstudio >= :inicio AND e.fechaEstudio <= :fin")
    long countEstudiosConResultadosEnMes(@Param("inicio") LocalDate inicio,
                                         @Param("fin")    LocalDate fin);

    /** Variante de countEstudiosConResultadosEnMes acotada a la institución dada (aislamiento de datos). */
    @Query("SELECT COUNT(DISTINCT e.Id) FROM EstudioMedico e " +
           "WHERE SIZE(e.resultadoEstudio) > 0 " +
           "AND e.fechaEstudio >= :inicio AND e.fechaEstudio <= :fin " +
           "AND e.paciente.institucion.id = :idInstitucion")
    long countEstudiosConResultadosEnMes(@Param("inicio") LocalDate inicio,
                                         @Param("fin")    LocalDate fin,
                                         @Param("idInstitucion") Long idInstitucion);

    // ── Cobertura ────────────────────────────────────────────────────────────

    /** Cuenta pacientes distintos (de la institución dada) con ≥ 1 estudio médico del tipo dado. */
    @Query("SELECT COUNT(DISTINCT e.paciente.Id) FROM EstudioMedico e " +
           "WHERE e.tipoEstudio.Id = :tipoId AND e.paciente.institucion.id = :idInstitucion")
    long countDistinctPacienteByTipoEstudioId(@Param("tipoId") Long tipoId, @Param("idInstitucion") Long idInstitucion);

    /**
     * Por cada paciente activo (de la institución dada) con al menos un estudio, devuelve
     * cuántos tipos de estudio distintos tiene. Devuelve pares [pacienteId, count].
     */
    @Query("SELECT e.paciente.Id, COUNT(DISTINCT e.tipoEstudio.Id) " +
           "FROM EstudioMedico e " +
           "WHERE e.paciente.activo = true AND e.paciente.institucion.id = :idInstitucion " +
           "GROUP BY e.paciente.Id")
    List<Object[]> countDistinctTipoByPacienteActivo(@Param("idInstitucion") Long idInstitucion);

    /**
     * Pacientes activos de la institución dada que NO tienen EstudioMedico con tipoEstudio.id = tipoId.
     */
    @Query("SELECT p.Id FROM Paciente p WHERE p.activo = true AND p.institucion.id = :idInstitucion " +
           "AND p.Id NOT IN (" +
           "  SELECT DISTINCT e.paciente.Id FROM EstudioMedico e WHERE e.tipoEstudio.Id = :tipoId)")
    List<Long> findPacientesActivosSinTipoEstudio(@Param("tipoId") Long tipoId, @Param("idInstitucion") Long idInstitucion);

    /**
     * Pacientes activos de la institución dada cuyo conteo de tipos de estudio distintos = k.
     */
    @Query("SELECT e.paciente.Id FROM EstudioMedico e " +
           "WHERE e.paciente.activo = true AND e.paciente.institucion.id = :idInstitucion " +
           "GROUP BY e.paciente.Id " +
           "HAVING COUNT(DISTINCT e.tipoEstudio.Id) = :k")
    List<Long> findPacientesConExactamenteKEstudios(@Param("k") long k, @Param("idInstitucion") Long idInstitucion);

    /**
     * Tipos de estudio cubiertos (ids) para un paciente.
     */
    @Query("SELECT DISTINCT e.tipoEstudio.Id FROM EstudioMedico e WHERE e.paciente.Id = :pacienteId")
    List<Long> findTiposEstudioCubiertosIdsForPaciente(@Param("pacienteId") Long pacienteId);
}
