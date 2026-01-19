package imss.gob.mx.cohorte.modules.escalonPrueba.medicion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PruebaEscalonMedicionRepository  extends JpaRepository<PruebaEscalonMedicion, Long> {


    
}
