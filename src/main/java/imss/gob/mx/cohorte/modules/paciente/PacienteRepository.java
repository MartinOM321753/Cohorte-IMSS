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

    /** Resuelve el expediente propio de un usuario PACIENTE a partir de su Persona vinculada. */
    Optional<Paciente> findByPersona_Id(Long personaId);

    /** Cuenta pacientes según su estado activo/inactivo (para el dashboard). */
    long countByActivo(boolean activo);

    // ── Variantes filtradas por institución (aislamiento de datos) ──

    Optional<Paciente> findByFolioAndInstitucion_Id(String folio, Long idInstitucion);

    Optional<Paciente> findByUuidAndInstitucion_Id(String uuid, Long idInstitucion);

    Optional<Paciente> findByIdAndInstitucion_Id(Long id, Long idInstitucion);

    Page<Paciente> findAllByInstitucion_Id(Long idInstitucion, Pageable pageable);

    List<Paciente> findAllByActivoAndInstitucion_IdOrderByFolioAsc(Boolean activo, Long idInstitucion);

    long countByActivoAndInstitucion_Id(boolean activo, Long idInstitucion);

    // ── Variantes multi-institución (jerarquía) ──

    Page<Paciente> findAllByInstitucion_IdIn(List<Long> ids, Pageable pageable);

    List<Paciente> findAllByActivoAndInstitucion_IdIn(Boolean activo, List<Long> ids);

    List<Paciente> findAllByActivoAndInstitucion_IdInOrderByFolioAsc(Boolean activo, List<Long> ids);

    Optional<Paciente> findByUuidAndInstitucion_IdIn(String uuid, List<Long> ids);

    /**
     * Participantes que ya NO estan al alcance de la institucion pero de los que
     * conserva registros propios.
     *
     * <p>Es el caso de una sede a la que le revocaron el permiso: dejo de gestionar
     * al participante, pero los estudios, muestras, citas, somatometrias y
     * resultados que ella capturo siguen siendo suyos. Sin esta consulta esa
     * informacion queda inalcanzable, porque buscar al participante ya no lo
     * encuentra.</p>
     *
     * @param idInstitucion la institucion que conserva los registros
     * @param alcanzables   instituciones que ya ve por la via normal; se excluyen
     *                      para no duplicar lo que la busqueda habitual ya muestra
     */
    @Query("""
        SELECT DISTINCT p FROM Paciente p
        WHERE p.institucion.id NOT IN :alcanzables
          AND (
               EXISTS (SELECT 1 FROM EstudioMedico e   WHERE e.paciente = p AND e.institucion.id = :idInstitucion)
            OR EXISTS (SELECT 1 FROM Muestra m         WHERE m.paciente = p AND m.institucion.id = :idInstitucion)
            OR EXISTS (SELECT 1 FROM Cita c            WHERE c.paciente = p AND c.institucion.id = :idInstitucion)
            OR EXISTS (SELECT 1 FROM Somatometria s    WHERE s.paciente = p AND s.institucion.id = :idInstitucion)
            OR EXISTS (SELECT 1 FROM ResultadoExamen r WHERE r.paciente = p AND r.institucion.id = :idInstitucion)
          )
        ORDER BY p.folio ASC
    """)
    List<Paciente> findConRegistrosDeInstitucion(@Param("idInstitucion") Long idInstitucion,
                                                  @Param("alcanzables") List<Long> alcanzables);

    /**
     * Version puntual de la anterior: ¿esta institucion conserva algun registro de
     * este participante? Se consulta en cada lectura por participante, asi que
     * pregunta por uno en vez de traer la lista completa.
     *
     * <p>Las muestras quedan fuera a proposito. Una muestra es inventario propio que
     * se sigue usando —alicuotando, aplicandole estudios de calidad— con
     * independencia de quien gestione hoy al participante, y su aislamiento va por
     * propietaria/tenedora. Tenerla en cuenta aqui abriria el historial clinico a
     * quien solo conserva tubos en su congelador.</p>
     */
    @Query("""
        SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END
        FROM Paciente p
        WHERE p.uuid = :uuid
          AND (
               EXISTS (SELECT 1 FROM EstudioMedico e   WHERE e.paciente = p AND e.institucion.id = :idInstitucion)
            OR EXISTS (SELECT 1 FROM Cita c            WHERE c.paciente = p AND c.institucion.id = :idInstitucion)
            OR EXISTS (SELECT 1 FROM Somatometria s    WHERE s.paciente = p AND s.institucion.id = :idInstitucion)
            OR EXISTS (SELECT 1 FROM ResultadoExamen r WHERE r.paciente = p AND r.institucion.id = :idInstitucion)
          )
    """)
    boolean tieneRegistrosDeInstitucion(@Param("uuid") String uuid,
                                         @Param("idInstitucion") Long idInstitucion);

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
         + "LOWER(p.folio) LIKE LOWER(CONCAT('%', :buscar, '%'))) "
         + "AND (:soloActivos IS NULL OR p.activo = :soloActivos) "
         + "ORDER BY p.activo DESC, p.folio ASC")
    Page<Paciente> buscarPaginado(@Param("idInstitucion") Long idInstitucion,
                                  @Param("buscar") String buscar,
                                  @Param("soloActivos") Boolean soloActivos,
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
         + "LOWER(p.folio) LIKE LOWER(CONCAT('%', :buscar, '%'))) "
         + "AND (:soloActivos IS NULL OR p.activo = :soloActivos) "
         + "ORDER BY p.activo DESC, p.folio ASC")
    Page<Paciente> buscarPaginadoEnInstituciones(@Param("ids") List<Long> ids,
                                                 @Param("buscar") String buscar,
                                                 @Param("soloActivos") Boolean soloActivos,
                                                 Pageable pageable);
}
