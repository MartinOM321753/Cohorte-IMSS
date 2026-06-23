package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParametroEstudioMuestraRepository extends JpaRepository<ParametroEstudioMuestra, Long> {

    List<ParametroEstudioMuestra> findAllByTipoEstudioMuestra_Id(Long idTipo);

    Optional<ParametroEstudioMuestra> findByTipoEstudioMuestra_IdAndNombreIgnoreCase(Long idTipo, String nombre);
}
