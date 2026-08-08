package imss.gob.mx.cohorte.modules.somatometria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SomatometriaRepository extends JpaRepository<Somatometria, Long> {

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
