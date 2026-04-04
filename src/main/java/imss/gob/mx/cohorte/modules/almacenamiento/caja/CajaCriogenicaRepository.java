package imss.gob.mx.cohorte.modules.almacenamiento.caja;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CajaCriogenicaRepository extends JpaRepository<CajaCriogenica, Long> {
    Optional<CajaCriogenica> findByCodigoCaja(String codigoCaja);
    List<CajaCriogenica> findAllByActivo(Boolean activo);
    List<CajaCriogenica> findAllByPosicionPiso_Id(Long posicionPisoId);
}
