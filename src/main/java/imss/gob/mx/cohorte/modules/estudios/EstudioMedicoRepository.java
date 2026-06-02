package imss.gob.mx.cohorte.modules.estudios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudioMedicoRepository extends JpaRepository<EstudioMedico, Long> {

    List<EstudioMedico> findAllByOrderByFechaEstudioDesc();

    List<EstudioMedico> findAllByPaciente_UuidOrderByFechaEstudioDesc(String uuid);

    /** Cuenta estudios que tienen al menos un resultado registrado. */
    @Query("SELECT COUNT(DISTINCT e.Id) FROM EstudioMedico e WHERE SIZE(e.resultadoEstudio) > 0")
    long countEstudiosConResultados();
}
