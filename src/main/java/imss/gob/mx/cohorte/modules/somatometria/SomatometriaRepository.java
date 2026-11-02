package imss.gob.mx.cohorte.modules.somatometria;

import imss.gob.mx.cohorte.modules.analisis.CrudoSomatoProy;
import imss.gob.mx.cohorte.modules.analisis.SomatometriaProy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SomatometriaRepository extends JpaRepository<Somatometria, Long> {

    /** Proyección de somatometría del alcance para el motor de análisis (todas las columnas numéricas). */
    @Query("SELECT new imss.gob.mx.cohorte.modules.analisis.SomatometriaProy("
         + "s.pesoKg, s.tallaM, s.imc, s.presionSistolica, s.presionDiastolica, "
         + "s.circunferenciaAbdominalCm, s.frecuenciaCardiacaReposo, "
         + "p.sexo, p.fechaNacimiento, s.fechaMedicion, s.institucion.id) "
         + "FROM Somatometria s JOIN s.paciente pac JOIN pac.persona p "
         + "WHERE s.institucion.id IN :instituciones "
         + "AND (:desde IS NULL OR s.fechaMedicion >= :desde) "
         + "AND (:hasta IS NULL OR s.fechaMedicion <= :hasta)")
    List<SomatometriaProy> proyeccionSomatometria(
            @Param("instituciones") Collection<Long> instituciones,
            @Param("desde")         LocalDateTime desde,
            @Param("hasta")         LocalDateTime hasta);

    /** Filas crudas (con folio) de somatometría, para la exportación a nivel participante. */
    @Query("SELECT new imss.gob.mx.cohorte.modules.analisis.CrudoSomatoProy("
         + "pac.folio, s.institucion.id, p.sexo, p.fechaNacimiento, s.fechaMedicion, "
         + "s.pesoKg, s.tallaM, s.imc, s.presionSistolica, s.presionDiastolica, "
         + "s.circunferenciaAbdominalCm, s.frecuenciaCardiacaReposo) "
         + "FROM Somatometria s JOIN s.paciente pac JOIN pac.persona p "
         + "WHERE s.institucion.id IN :instituciones "
         + "AND (:desde IS NULL OR s.fechaMedicion >= :desde) "
         + "AND (:hasta IS NULL OR s.fechaMedicion <= :hasta)")
    List<CrudoSomatoProy> crudoSomatometria(
            @Param("instituciones") Collection<Long> instituciones,
            @Param("desde")         LocalDateTime desde,
            @Param("hasta")         LocalDateTime hasta);

    /** Sin filtro de institucion: se usa para saber si un participante ya quedo vinculado a alguna. */
    long countByPaciente_Uuid(String uuid);

    /** Lo que mi sede le registro a un participante concreto. */
    java.util.List<Somatometria> findAllByPaciente_UuidAndInstitucion_IdOrderByFechaMedicionDesc(
            String uuid, Long idInstitucion);

    /** Lo que registro mi sede, independientemente de a quien. */
    java.util.List<Somatometria> findAllByInstitucion_IdOrderByFechaMedicionDesc(Long idInstitucion);

    /** Historial acotado a las instituciones alcanzables. */
    java.util.List<Somatometria> findByPaciente_UuidAndInstitucion_IdInOrderByFechaMedicionDesc(
            String uuid, java.util.List<Long> idsInstituciones);

    /** Historial ordenado por fecha desc para un paciente */
    @Query("""
        SELECT s FROM Somatometria s
        WHERE s.paciente.uuid = :uuid
        ORDER BY s.fechaMedicion DESC
    """)
    List<Somatometria> findByPacienteUuidOrderByFechaMedicionDesc(@Param("uuid") String uuid);

    /** Último registro del paciente */
    @Query("""
        SELECT s FROM Somatometria s
        WHERE s.paciente.uuid = :uuid
        ORDER BY s.fechaMedicion DESC
        LIMIT 1
    """)
    Optional<Somatometria> findLatestByPacienteUuid(@Param("uuid") String uuid);

    /** Datos globales para gráficas, acotados a la institución actual (aislamiento de datos). */
    @Query("""
        SELECT s FROM Somatometria s
        WHERE s.institucion.id = :idInstitucion
        ORDER BY s.fechaMedicion ASC
    """)
    List<Somatometria> findAllByInstitucionOrderByFechaMedicionAsc(@Param("idInstitucion") Long idInstitucion);
}
