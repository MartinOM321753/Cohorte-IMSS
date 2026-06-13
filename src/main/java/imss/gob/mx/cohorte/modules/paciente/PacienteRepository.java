package imss.gob.mx.cohorte.modules.paciente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByFolio(String folio);

    List<Paciente> findAllByActivo(Boolean activo);

    Optional<Paciente> findByUuid(String uuid);

    boolean existsByUuid(String uuid);

    /** Cuenta pacientes según su estado activo/inactivo (para el dashboard). */
    long countByActivo(boolean activo);

    // ── Variantes filtradas por institución (aislamiento de datos) ──

    Optional<Paciente> findByFolioAndInstitucion_Id(String folio, Long idInstitucion);

    Optional<Paciente> findByUuidAndInstitucion_Id(String uuid, Long idInstitucion);

    Optional<Paciente> findByIdAndInstitucion_Id(Long id, Long idInstitucion);

    List<Paciente> findAllByInstitucion_Id(Long idInstitucion);

    Page<Paciente> findAllByInstitucion_Id(Long idInstitucion, Pageable pageable);

    List<Paciente> findAllByActivoAndInstitucion_Id(Boolean activo, Long idInstitucion);

    long countByActivoAndInstitucion_Id(boolean activo, Long idInstitucion);
}
