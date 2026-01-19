package imss.gob.mx.cohorte.modules.estudios.resultados;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultadoEstudioRepository extends JpaRepository<ResultadoEstudio, Long> {

}
