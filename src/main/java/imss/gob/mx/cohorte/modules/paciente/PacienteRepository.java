package imss.gob.mx.cohorte.modules.paciente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByFolio(String folio);

    boolean existsByFolio(String folio);

    @Query(value = "SELECT CAST(p.folio AS UNSIGNED) FROM paciente p WHERE p.folio REGEXP '^[0-9]{6}$' ORDER BY 1", nativeQuery = true)
    List<Integer> findAllFoliosNumericos();

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

    // ── Variantes multi-institución (jerarquía) ──

    List<Paciente> findAllByInstitucion_IdIn(List<Long> ids);

    Page<Paciente> findAllByInstitucion_IdIn(List<Long> ids, Pageable pageable);

    List<Paciente> findAllByActivoAndInstitucion_IdIn(Boolean activo, List<Long> ids);

    Optional<Paciente> findByUuidAndInstitucion_IdIn(String uuid, List<Long> ids);

    Optional<Paciente> findByFolioAndInstitucion_IdIn(String folio, List<Long> ids);

    Optional<Paciente> findByIdAndInstitucion_IdIn(Long id, List<Long> ids);
}
