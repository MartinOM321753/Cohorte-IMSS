package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MuestraRepository extends JpaRepository<Muestra, Long> {
    Optional<Muestra> findByEtiquetaIgnoreCase(String etiqueta);
    List<Muestra> findAllByPaciente_Uuid(String uuid);
    List<Muestra> findAllByPaciente_Folio(String folio);
    List<Muestra> findAllByPosicionCajaIsNull();
}
