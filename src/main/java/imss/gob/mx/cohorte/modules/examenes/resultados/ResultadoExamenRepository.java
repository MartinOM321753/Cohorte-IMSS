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
}
