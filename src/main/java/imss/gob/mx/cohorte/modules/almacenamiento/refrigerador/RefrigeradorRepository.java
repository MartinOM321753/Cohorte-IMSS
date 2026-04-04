package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefrigeradorRepository extends JpaRepository<Refrigerador, Long> {

    Optional<Refrigerador> findByCodigo(String codigo);

}
