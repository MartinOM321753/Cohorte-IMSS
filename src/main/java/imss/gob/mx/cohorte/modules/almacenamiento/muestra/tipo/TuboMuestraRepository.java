package imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TuboMuestraRepository extends JpaRepository<TuboMuestra, Long> {

    List<TuboMuestra> findAllByTipoMuestra_IdOrderByOrdenAsc(Long idTipoMuestra);
}
