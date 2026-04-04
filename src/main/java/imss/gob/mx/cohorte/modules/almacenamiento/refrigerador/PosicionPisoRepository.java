package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosicionPisoRepository extends JpaRepository<PosicionPiso, Long> {
    List<PosicionPiso> findAllByPiso_Id(Long pisoId);
    List<PosicionPiso> findAllByPiso_IdAndOcupada(Long pisoId, Boolean ocupada);
    Optional<PosicionPiso> findByPiso_IdAndFilaAndColumnaAndAltura(
        Long pisoId, String fila, String columna, String altura);
}
