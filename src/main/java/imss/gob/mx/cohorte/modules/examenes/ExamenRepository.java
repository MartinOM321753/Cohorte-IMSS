package imss.gob.mx.cohorte.modules.examenes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamenRepository extends JpaRepository<Examen, Long> {

    Optional<Examen> findByParametro(String nombre);

    List<Examen> findAllByActivo(Boolean activo);

    Optional<Examen> findByIdAndActivo(Long id, Boolean activo);

    /** Catálogo de exámenes de una institución (aislamiento de datos). */
    List<Examen> findAllByActivoAndInstitucion_Id(Boolean activo, Long idInstitucion);





}
