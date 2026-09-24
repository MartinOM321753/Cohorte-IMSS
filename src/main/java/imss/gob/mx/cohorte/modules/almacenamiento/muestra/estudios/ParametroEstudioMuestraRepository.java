package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParametroEstudioMuestraRepository extends JpaRepository<ParametroEstudioMuestra, Long> {

    /** Los parámetros del tipo en el orden configurado. Ver la nota del repositorio de estudios. */
    List<ParametroEstudioMuestra> findAllByTipoEstudioMuestra_IdOrderByOrdenAscIdAsc(Long idTipo);

    List<ParametroEstudioMuestra> findAllByTipoEstudioMuestra_Id(Long idTipo);

    Optional<ParametroEstudioMuestra> findByTipoEstudioMuestra_IdAndNombreIgnoreCase(Long idTipo, String nombre);
}
