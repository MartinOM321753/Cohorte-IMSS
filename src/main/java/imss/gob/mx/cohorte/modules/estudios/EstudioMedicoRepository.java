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
}
