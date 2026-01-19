package imss.gob.mx.cohorte.modules.almacenamiento.caja;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CajaCriogenicaRepository extends JpaRepository<CajaCriogenica, Long> {
}
