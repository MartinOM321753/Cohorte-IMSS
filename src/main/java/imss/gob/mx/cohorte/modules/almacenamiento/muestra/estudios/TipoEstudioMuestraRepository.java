package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoEstudioMuestraRepository extends JpaRepository<TipoEstudioMuestra, Long> {

    Optional<TipoEstudioMuestra> findByNombreIgnoreCase(String nombre);

    List<TipoEstudioMuestra> findAllByActivo(Boolean activo);
}
