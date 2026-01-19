package imss.gob.mx.cohorte.modules.escalonPrueba;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PruebaEscalonRepository extends JpaRepository<PruebaEscalon, Long> {

}
