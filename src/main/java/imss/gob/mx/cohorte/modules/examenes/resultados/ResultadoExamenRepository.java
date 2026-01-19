package imss.gob.mx.cohorte.modules.examenes.resultados;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultadoExamenRepository extends JpaRepository<ResultadoExamen, Long> {
}
