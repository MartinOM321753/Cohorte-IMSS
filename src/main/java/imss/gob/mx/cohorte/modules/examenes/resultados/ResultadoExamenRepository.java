package imss.gob.mx.cohorte.modules.examenes.resultados;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ResultadoExamenRepository extends JpaRepository<ResultadoExamen, Long> {

    List<ResultadoExamen> findByPaciente_Uuid(String uuid);
    List<ResultadoExamen> findByPaciente_Folio(String uuid);
    long countByPaciente_Uuid(String uuid);

    /** Cuenta resultados registrados en el rango de fechas indicado. */
    @Query("SELECT COUNT(r) FROM ResultadoExamen r " +
           "WHERE r.fechaResultado >= :inicio AND r.fechaResultado <= :fin")
    long countByFechaResultadoBetween(@Param("inicio") LocalDateTime inicio,
                                      @Param("fin")    LocalDateTime fin);

    /** Carga todos los resultados con examen y paciente (y persona) para el cálculo de calidad. */
    @Query("SELECT r FROM ResultadoExamen r " +
           "LEFT JOIN FETCH r.examen " +
           "LEFT JOIN FETCH r.paciente pac " +
           "LEFT JOIN FETCH pac.persona")
    List<ResultadoExamen> findAllWithExamenAndPaciente();

    // ── Cobertura ────────────────────────────────────────────────────────────

    /** Cuenta pacientes distintos que tienen ≥ 1 resultado para el examen dado. */
    @Query("SELECT COUNT(DISTINCT r.paciente.Id) FROM ResultadoExamen r WHERE r.examen.Id = :examenId")
    long countDistinctPacienteByExamenId(@Param("examenId") Long examenId);

    /**
     * Por cada paciente activo con al menos un resultado, devuelve cuántos exámenes distintos
     * tiene. Devuelve pares [pacienteId, count].
     */
    @Query("SELECT r.paciente.Id, COUNT(DISTINCT r.examen.Id) " +
           "FROM ResultadoExamen r " +
           "WHERE r.paciente.activo = true " +
           "GROUP BY r.paciente.Id")
    List<Object[]> countDistinctExamenByPacienteActivo();

    /**
     * Pacientes activos que NO tienen ResultadoExamen con examen.id = tipoId.
     * Devuelve IDs de paciente.
     */
    @Query("SELECT p.Id FROM Paciente p WHERE p.activo = true " +
           "AND p.Id NOT IN (" +
           "  SELECT DISTINCT r.paciente.Id FROM ResultadoExamen r WHERE r.examen.Id = :examenId)")
    List<Long> findPacientesActivosSinExamen(@Param("examenId") Long examenId);

    /**
     * Pacientes activos cuyo conteo de exámenes distintos = k.
     * Devuelve IDs de paciente.
     */
    @Query("SELECT r.paciente.Id FROM ResultadoExamen r " +
           "WHERE r.paciente.activo = true " +
           "GROUP BY r.paciente.Id " +
           "HAVING COUNT(DISTINCT r.examen.Id) = :k")
    List<Long> findPacientesConExactamenteKExamenes(@Param("k") long k);

    /**
     * Para un paciente, cuenta exámenes distintos cubiertos.
     */
    @Query("SELECT COUNT(DISTINCT r.examen.Id) FROM ResultadoExamen r WHERE r.paciente.Id = :pacienteId")
    long countDistinctExamenByPacienteId(@Param("pacienteId") Long pacienteId);

    /**
     * Exámenes cubiertos (ids) para un paciente.
     */
    @Query("SELECT DISTINCT r.examen.Id FROM ResultadoExamen r WHERE r.paciente.Id = :pacienteId")
    List<Long> findExamenesCubiertosIdsForPaciente(@Param("pacienteId") Long pacienteId);
}
