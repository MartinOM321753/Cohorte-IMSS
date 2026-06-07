package imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialCambioMuestraRepository extends JpaRepository<HistorialCambioMuestra, Long> {

    List<HistorialCambioMuestra> findAllByMuestra_IdOrderByFechaCambioDesc(Long idMuestra);
}
