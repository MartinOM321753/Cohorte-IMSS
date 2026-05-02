package imss.gob.mx.cohorte.modules.estudios.adjuntos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstudioAdjuntoRepository extends JpaRepository<EstudioAdjunto, Long> {
}
