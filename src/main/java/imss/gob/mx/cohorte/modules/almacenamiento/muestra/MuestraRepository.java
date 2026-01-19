package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MuestraRepository extends JpaRepository<Muestra, Long> {
}
