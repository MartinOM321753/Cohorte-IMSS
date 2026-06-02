package imss.gob.mx.cohorte.modules.examenes.resultados;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultadoExamenRepository extends JpaRepository<ResultadoExamen, Long> {

    List<ResultadoExamen> findByPaciente_Uuid(String uuid);
    List<ResultadoExamen> findByPaciente_Folio(String uuid);
    long countByPaciente_Uuid(String uuid);

}
