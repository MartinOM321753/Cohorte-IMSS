package imss.gob.mx.cohorte.modules.estudios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EstudioMedicoRepository extends JpaRepository<EstudioMedico, Long> {

    List<EstudioMedico> findAllByOrderByFechaEstudioDesc();

    List<EstudioMedico> findAllByPaciente_UuidOrderByFechaEstudioDesc(String uuid);

    /** Cuenta estudios que tienen al menos un resultado registrado. */
    @Query("SELECT COUNT(DISTINCT e.Id) FROM EstudioMedico e WHERE SIZE(e.resultadoEstudio) > 0")
    long countEstudiosConResultados();

    /** Cuenta estudios con al menos un resultado cuya fechaEstudio cae en el rango [inicio, fin]. */
    @Query("SELECT COUNT(DISTINCT e.Id) FROM EstudioMedico e " +
           "WHERE SIZE(e.resultadoEstudio) > 0 " +
           "AND e.fechaEstudio >= :inicio AND e.fechaEstudio <= :fin")
    long countEstudiosConResultadosEnMes(@Param("inicio") LocalDate inicio,
                                         @Param("fin")    LocalDate fin);

    // ── Cobertura ────────────────────────────────────────────────────────────

    /** Cuenta pacientes distintos con ≥ 1 estudio médico del tipo dado. */
    @Query("SELECT COUNT(DISTINCT e.paciente.Id) FROM EstudioMedico e WHERE e.tipoEstudio.Id = :tipoId")
    long countDistinctPacienteByTipoEstudioId(@Param("tipoId") Long tipoId);

    /**
     * Por cada paciente activo con al menos un estudio, devuelve cuántos tipos de estudio distintos
     * tiene. Devuelve pares [pacienteId, count].
     */
    @Query("SELECT e.paciente.Id, COUNT(DISTINCT e.tipoEstudio.Id) " +
           "FROM EstudioMedico e " +
           "WHERE e.paciente.activo = true " +
           "GROUP BY e.paciente.Id")
    List<Object[]> countDistinctTipoByPacienteActivo();

    /**
     * Pacientes activos que NO tienen EstudioMedico con tipoEstudio.id = tipoId.
     */
    @Query("SELECT p.Id FROM Paciente p WHERE p.activo = true " +
           "AND p.Id NOT IN (" +
           "  SELECT DISTINCT e.paciente.Id FROM EstudioMedico e WHERE e.tipoEstudio.Id = :tipoId)")
    List<Long> findPacientesActivosSinTipoEstudio(@Param("tipoId") Long tipoId);

    /**
     * Pacientes activos cuyo conteo de tipos de estudio distintos = k.
     */
    @Query("SELECT e.paciente.Id FROM EstudioMedico e " +
           "WHERE e.paciente.activo = true " +
           "GROUP BY e.paciente.Id " +
           "HAVING COUNT(DISTINCT e.tipoEstudio.Id) = :k")
    List<Long> findPacientesConExactamenteKEstudios(@Param("k") long k);

    /**
     * Tipos de estudio cubiertos (ids) para un paciente.
     */
    @Query("SELECT DISTINCT e.tipoEstudio.Id FROM EstudioMedico e WHERE e.paciente.Id = :pacienteId")
    List<Long> findTiposEstudioCubiertosIdsForPaciente(@Param("pacienteId") Long pacienteId);
}
