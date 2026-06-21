package imss.gob.mx.cohorte.modules.usuarios.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<BeanUser, Long> {

    List<BeanUser> findAllByActivo(Boolean activo);

    List<BeanUser> findAllByInstitucion_Id(Long idInstitucion);

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

    List<BeanUser> findAllByRol_RoleAndActivoTrue(String roleName);

    List<BeanUser> findAllByRol_RoleAndActivoTrueAndInstitucion_Id(String roleName, Long idInstitucion);

    /**
     * Administradores activos que NO están asignados como encargado de ninguna institución.
     * Usado para poblar el selector de encargado en el formulario de creación de institución.
     */
    @Query("SELECT u FROM BeanUser u WHERE u.rol.role = 'ADMINISTRADOR' AND u.activo = true " +
           "AND NOT EXISTS (SELECT i FROM Institucion i WHERE i.encargado = u)")
    List<BeanUser> findAdministradoresActivosSinAsignacion();

    /**
     * Administradores activos disponibles para una institución concreta.
     * Devuelve los que no tienen asignación PLUS el que ya está asignado a esa institución
     * (necesario para que el selector en modo edición incluya al encargado actual).
     */
    @Query("SELECT u FROM BeanUser u WHERE u.rol.role = 'ADMINISTRADOR' AND u.activo = true " +
           "AND (NOT EXISTS (SELECT i FROM Institucion i WHERE i.encargado = u) " +
           "     OR EXISTS (SELECT i FROM Institucion i WHERE i.encargado = u AND i.uuid = :uuidInstitucion))")
    List<BeanUser> findAdministradoresDisponiblesParaInstitucion(@Param("uuidInstitucion") String uuidInstitucion);

    @Query("SELECT DISTINCT u FROM BeanUser u " +
           "JOIN FETCH u.persona JOIN FETCH u.rol " +
           "WHERE u.institucion.id = :idInstitucion " +
           "AND u.UUID IN (SELECT DISTINCT ba.usuarioUuid FROM BitacoraAcceso ba WHERE ba.usuarioUuid IS NOT NULL) " +
           "ORDER BY u.persona.nombre")
    List<BeanUser> findUsuariosConAccesos(@Param("idInstitucion") Long idInstitucion);

    @Query("SELECT DISTINCT u FROM BeanUser u " +
           "JOIN FETCH u.persona JOIN FETCH u.rol " +
           "WHERE u.institucion.id = :idInstitucion " +
           "AND u.UUID IN (SELECT DISTINCT bac.usuarioUuid FROM BitacoraAcciones bac WHERE bac.usuarioUuid IS NOT NULL) " +
           "ORDER BY u.persona.nombre")
    List<BeanUser> findUsuariosConAcciones(@Param("idInstitucion") Long idInstitucion);
}
