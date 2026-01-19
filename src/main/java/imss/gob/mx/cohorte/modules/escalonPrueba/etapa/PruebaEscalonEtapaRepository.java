package imss.gob.mx.cohorte.modules.escalonPrueba.etapa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface PruebaEscalonEtapaRepository extends JpaRepository<PruebaEscalonEtapa, Long> {
    List<PruebaEscalonEtapa> id(Long id);
}
