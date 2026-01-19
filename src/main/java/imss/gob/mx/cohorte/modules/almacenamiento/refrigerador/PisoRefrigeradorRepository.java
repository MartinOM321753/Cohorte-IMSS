package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PisoRefrigeradorRepository extends JpaRepository<PisoRefrigerador, Long> {
}
