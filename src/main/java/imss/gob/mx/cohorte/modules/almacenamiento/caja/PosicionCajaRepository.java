package imss.gob.mx.cohorte.modules.almacenamiento.caja;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PosicionCajaRepository extends JpaRepository<PosicionCaja, Long> {
}
