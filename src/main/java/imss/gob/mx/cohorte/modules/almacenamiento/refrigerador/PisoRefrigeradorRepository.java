package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PisoRefrigeradorRepository extends JpaRepository<PisoRefrigerador, Long> {
    boolean existsByNumeroPiso(String numeroPiso);

    Optional<PisoRefrigerador> findByNumeroPiso(String numeroPiso);

    List<PisoRefrigerador> findAllByRefrigerador_Id(Long refrigeradorId);

}
