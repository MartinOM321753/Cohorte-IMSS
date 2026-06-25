package imss.gob.mx.cohorte.modules.paciente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // ── Búsqueda paginada con filtro de texto (server-side search) ──

    @Query("SELECT p FROM Paciente p JOIN p.persona per WHERE p.institucion.id = :idInstitucion "
         + "AND (:buscar IS NULL OR :buscar = '' OR "
         + "LOWER(per.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.segundoNombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoMaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(CONCAT(per.nombre, ' ', COALESCE(per.segundoNombre, ''), ' ', per.apellidoPaterno, ' ', COALESCE(per.apellidoMaterno, ''))) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.curp) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.email) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(p.folio) LIKE LOWER(CONCAT('%', :buscar, '%')))")
    Page<Paciente> buscarPaginado(@Param("idInstitucion") Long idInstitucion,
                                  @Param("buscar") String buscar,
                                  Pageable pageable);

    @Query("SELECT p FROM Paciente p JOIN p.persona per WHERE p.institucion.id IN :ids "
         + "AND (:buscar IS NULL OR :buscar = '' OR "
         + "LOWER(per.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.segundoNombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoMaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(CONCAT(per.nombre, ' ', COALESCE(per.segundoNombre, ''), ' ', per.apellidoPaterno, ' ', COALESCE(per.apellidoMaterno, ''))) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.curp) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.email) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(p.folio) LIKE LOWER(CONCAT('%', :buscar, '%')))")
    Page<Paciente> buscarPaginadoEnInstituciones(@Param("ids") List<Long> ids,
                                                 @Param("buscar") String buscar,
                                                 Pageable pageable);
}
