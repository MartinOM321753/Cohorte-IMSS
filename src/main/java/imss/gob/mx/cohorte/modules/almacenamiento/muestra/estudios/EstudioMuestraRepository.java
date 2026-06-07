package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstudioMuestraRepository extends JpaRepository<EstudioMuestra, Long> {

    List<EstudioMuestra> findAllByMuestra_IdOrderByFechaEstudioDescIdDesc(Long idMuestra);
}
