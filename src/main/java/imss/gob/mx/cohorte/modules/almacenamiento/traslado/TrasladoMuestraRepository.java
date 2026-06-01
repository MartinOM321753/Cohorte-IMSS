package imss.gob.mx.cohorte.modules.almacenamiento.traslado;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrasladoMuestraRepository extends JpaRepository<TrasladoMuestra, Long> {

    List<TrasladoMuestra> findAllByMuestra_IdOrderByFechaTrasladoDesc(Long idMuestra);

    Optional<TrasladoMuestra> findFirstByMuestra_IdAndEstadoOrderByFechaTrasladoDesc(
            Long idMuestra, EstadoTraslado estado);

    boolean existsByMuestra_IdAndEstadoIn(Long idMuestra, List<EstadoTraslado> estados);

    boolean existsByAlmacen_IdAndEstadoIn(Long idAlmacen, List<EstadoTraslado> estados);

    List<TrasladoMuestra> findAllByOrderByFechaTrasladoDesc();

    List<TrasladoMuestra> findAllByAlmacen_IdOrderByFechaTrasladoDesc(Long idAlmacen);

    Page<TrasladoMuestra> findAllByAlmacen_Id(Long idAlmacen, Pageable pageable);
}
