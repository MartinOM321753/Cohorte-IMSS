package imss.gob.mx.cohorte.modules.almacenamiento.protocolo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TuboProtocoloRepository extends JpaRepository<TuboProtocolo, Long> {
}
