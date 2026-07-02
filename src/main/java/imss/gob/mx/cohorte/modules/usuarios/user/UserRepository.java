package imss.gob.mx.cohorte.modules.usuarios.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<BeanUser, Long> {

    List<BeanUser> findAllByActivo(Boolean activo);

    List<BeanUser> findAllByInstitucion_Id(Long idInstitucion);

    @Query("SELECT DISTINCT u FROM BeanUser u " +
           "WHERE u.institucion.id = :idInstitucion " +
           "   OR (u.activo = true AND u.debeResetear = true) " +
           "ORDER BY u.fechaCreacion DESC")
    List<BeanUser> findAllByInstitucionOrInvitacionPendiente(@Param("idInstitucion") Long idInstitucion);

    // ── Queries para acceso de participantes ──

    boolean existsByPersona_Id(Long personaId);

    @Query("SELECT u.persona.id FROM BeanUser u WHERE u.persona.id IN :personaIds")
    Set<Long> findPersonaIdsWithUserAccount(@Param("personaIds") List<Long> personaIds);

    // ── Queries excluyendo rol PACIENTE (para panel de usuarios) ──

    @Query("SELECT DISTINCT u FROM BeanUser u " +
           "WHERE (u.institucion.id = :idInstitucion OR (u.activo = true AND u.debeResetear = true)) " +
           "AND NOT EXISTS (SELECT ur FROM UsuarioRol ur WHERE ur.usuario = u AND ur.rol.role = 'PACIENTE') " +
           "ORDER BY u.fechaCreacion DESC")
    List<BeanUser> findAllByInstitucionExcluyendoPacientes(@Param("idInstitucion") Long idInstitucion);

    @Query(value = "SELECT DISTINCT u FROM BeanUser u JOIN u.persona per LEFT JOIN u.rol r "
         + "WHERE (u.institucion.id = :idInstitucion OR (u.activo = true AND u.debeResetear = true)) "
         + "AND NOT EXISTS (SELECT ur FROM UsuarioRol ur WHERE ur.usuario = u AND ur.rol.role = 'PACIENTE') "
         + "AND (:buscar IS NULL OR :buscar = '' OR "
         + "LOWER(per.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.segundoNombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoMaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(CONCAT(per.nombre, ' ', COALESCE(per.segundoNombre, ''), ' ', per.apellidoPaterno, ' ', COALESCE(per.apellidoMaterno, ''))) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(u.username) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.email) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(r.role) LIKE LOWER(CONCAT('%', :buscar, '%')))",
           countQuery = "SELECT COUNT(DISTINCT u) FROM BeanUser u JOIN u.persona per LEFT JOIN u.rol r "
         + "WHERE (u.institucion.id = :idInstitucion OR (u.activo = true AND u.debeResetear = true)) "
         + "AND NOT EXISTS (SELECT ur FROM UsuarioRol ur WHERE ur.usuario = u AND ur.rol.role = 'PACIENTE') "
         + "AND (:buscar IS NULL OR :buscar = '' OR "
         + "LOWER(per.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.segundoNombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoMaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(CONCAT(per.nombre, ' ', COALESCE(per.segundoNombre, ''), ' ', per.apellidoPaterno, ' ', COALESCE(per.apellidoMaterno, ''))) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(u.username) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.email) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(r.role) LIKE LOWER(CONCAT('%', :buscar, '%')))")
    Page<BeanUser> buscarPaginadoExcluyendoPacientes(@Param("idInstitucion") Long idInstitucion,
                                                      @Param("buscar") String buscar,
                                                      Pageable pageable);

    List<BeanUser> findAllByActivoAndInstitucion_Id(Boolean activo, Long idInstitucion);
    Optional<BeanUser> findByUsername(String username);
    Optional<BeanUser> findByUUID(String username);
    boolean existsByUUID(String uuid);

    /**
     * Busca un usuario activo cuya persona tenga el email indicado (case-insensitive).
     * Usado en el flujo de recuperación de contraseña.
     */
    @Query("SELECT u FROM BeanUser u WHERE LOWER(u.persona.email) = LOWER(:email) AND u.activo = true")
    Optional<BeanUser> findActiveUserByPersonaEmail(@Param("email") String email);

    @Query("SELECT u FROM BeanUser u WHERE u.activo = true " +
           "AND EXISTS (SELECT ur FROM UsuarioRol ur WHERE ur.usuario = u AND ur.rol.role = :roleName)")
    List<BeanUser> findAllByRol_RoleAndActivoTrue(@Param("roleName") String roleName);

    @Query("SELECT u FROM BeanUser u WHERE u.activo = true AND u.institucion.id = :idInstitucion " +
           "AND EXISTS (SELECT ur FROM UsuarioRol ur WHERE ur.usuario = u AND ur.rol.role = :roleName)")
    List<BeanUser> findAllByRol_RoleAndActivoTrueAndInstitucion_Id(@Param("roleName") String roleName, @Param("idInstitucion") Long idInstitucion);

    /**
     * Administradores/Encargados activos que NO están asignados como encargado de ninguna institución.
     * Busca a través de usuario_rol (fuente de verdad para roles).
     */
    @Query("SELECT u FROM BeanUser u " +
           "WHERE u.activo = true " +
           "AND EXISTS (SELECT ur FROM UsuarioRol ur WHERE ur.usuario = u AND ur.rol.role IN ('ADMINISTRADOR', 'ENCARGADO')) " +
           "AND NOT EXISTS (SELECT i FROM Institucion i WHERE i.encargado = u)")
    List<BeanUser> findAdministradoresActivosSinAsignacion();

    /**
     * Administradores/Encargados activos disponibles para una institución concreta.
     * Devuelve los que no tienen asignación + el que ya esté asignado a ESA institución.
     */
    @Query("SELECT u FROM BeanUser u " +
           "WHERE u.activo = true " +
           "AND EXISTS (SELECT ur FROM UsuarioRol ur WHERE ur.usuario = u AND ur.rol.role IN ('ADMINISTRADOR', 'ENCARGADO')) " +
           "AND (NOT EXISTS (SELECT i FROM Institucion i WHERE i.encargado = u) " +
           "     OR EXISTS (SELECT i FROM Institucion i WHERE i.encargado = u AND i.uuid = :uuidInstitucion))")
    List<BeanUser> findAdministradoresDisponiblesParaInstitucion(@Param("uuidInstitucion") String uuidInstitucion);

    // ── Búsqueda paginada con filtro de texto (server-side search) ──
    // Mantiene la misma lógica que findAllByInstitucionOrInvitacionPendiente:
    // usuarios de la institución actual + usuarios con invitación pendiente de cualquier institución.
    @Query(value = "SELECT DISTINCT u FROM BeanUser u JOIN u.persona per LEFT JOIN u.rol r "
         + "WHERE (u.institucion.id = :idInstitucion OR (u.activo = true AND u.debeResetear = true)) "
         + "AND (:buscar IS NULL OR :buscar = '' OR "
         + "LOWER(per.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.segundoNombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoMaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(CONCAT(per.nombre, ' ', COALESCE(per.segundoNombre, ''), ' ', per.apellidoPaterno, ' ', COALESCE(per.apellidoMaterno, ''))) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(u.username) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.email) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(r.role) LIKE LOWER(CONCAT('%', :buscar, '%')))",
           countQuery = "SELECT COUNT(DISTINCT u) FROM BeanUser u JOIN u.persona per LEFT JOIN u.rol r "
         + "WHERE (u.institucion.id = :idInstitucion OR (u.activo = true AND u.debeResetear = true)) "
         + "AND (:buscar IS NULL OR :buscar = '' OR "
         + "LOWER(per.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.segundoNombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.apellidoMaterno) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(CONCAT(per.nombre, ' ', COALESCE(per.segundoNombre, ''), ' ', per.apellidoPaterno, ' ', COALESCE(per.apellidoMaterno, ''))) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(u.username) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(per.email) LIKE LOWER(CONCAT('%', :buscar, '%')) OR "
         + "LOWER(r.role) LIKE LOWER(CONCAT('%', :buscar, '%')))")
    Page<BeanUser> buscarPaginadoConInvitacionesPendientes(@Param("idInstitucion") Long idInstitucion,
                                                           @Param("buscar") String buscar,
                                                           Pageable pageable);

    @Query("SELECT DISTINCT u FROM BeanUser u " +
           "JOIN FETCH u.persona LEFT JOIN FETCH u.rol " +
           "WHERE u.institucion.id = :idInstitucion " +
           "AND u.UUID IN (SELECT DISTINCT ba.usuarioUuid FROM BitacoraAcceso ba WHERE ba.usuarioUuid IS NOT NULL) " +
           "ORDER BY u.persona.nombre")
    List<BeanUser> findUsuariosConAccesos(@Param("idInstitucion") Long idInstitucion);

    @Query("SELECT DISTINCT u FROM BeanUser u " +
           "JOIN FETCH u.persona LEFT JOIN FETCH u.rol " +
           "WHERE u.institucion.id = :idInstitucion " +
           "AND u.UUID IN (SELECT DISTINCT bac.usuarioUuid FROM BitacoraAcciones bac WHERE bac.usuarioUuid IS NOT NULL) " +
           "ORDER BY u.persona.nombre")
    List<BeanUser> findUsuariosConAcciones(@Param("idInstitucion") Long idInstitucion);
}
