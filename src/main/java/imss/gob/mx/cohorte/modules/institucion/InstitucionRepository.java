package imss.gob.mx.cohorte.modules.institucion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitucionRepository extends JpaRepository<Institucion, Long> {

    Optional<Institucion> findByUuid(String uuid);

    Optional<Institucion> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Institucion> findAllByActivo(Boolean activo);

    /** Búsqueda server-side para combos/selectores con autocompletado (evita cargar toda la tabla en el cliente). */
    Page<Institucion> findAllByActivoTrueAndNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Institucion> findAllByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    List<Institucion> findAllByInstitucionPadre_Id(Long idInstitucionPadre);

    /** La institución raíz no tiene padre — es la única que puede otorgar permisos de módulo a otras. */
    List<Institucion> findAllByInstitucionPadreIsNull();

    List<Institucion> findAllByEncargado_UUID(String uuidEncargado);

    List<Institucion> findAllByEncargadoIsNull();

    List<Institucion> findAllByEncargado_Id(Long idEncargado);
}
