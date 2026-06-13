package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MuestraRepository extends JpaRepository<Muestra, Long> {
    Optional<Muestra> findByEtiquetaIgnoreCase(String etiqueta);
    Optional<Muestra> findByEtiquetaIgnoreCaseAndInstitucion_Id(String etiqueta, Long idInstitucion);
    List<Muestra> findAllByPaciente_Uuid(String uuid);
    List<Muestra> findAllByPaciente_Folio(String folio);
    List<Muestra> findAllByPosicionCajaIsNull();
    long countByPaciente_Uuid(String uuid);

    List<Muestra> findAllByInstitucion_Id(Long idInstitucion);
    Page<Muestra> findAllByInstitucion_Id(Long idInstitucion, Pageable pageable);
    List<Muestra> findAllByPaciente_UuidAndInstitucion_Id(String uuid, Long idInstitucion);
    long countByPaciente_UuidAndInstitucion_Id(String uuid, Long idInstitucion);
    long countByInstitucion_Id(Long idInstitucion);

    /** Muestras cuyo tenedor actual es la institución dada (biobanco en tiempo real). */
    List<Muestra> findAllByInstitucionActual_Id(Long idInstitucion);
    Page<Muestra> findAllByInstitucionActual_Id(Long idInstitucion, Pageable pageable);
}
