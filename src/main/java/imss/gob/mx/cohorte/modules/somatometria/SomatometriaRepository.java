package imss.gob.mx.cohorte.modules.somatometria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SomatometriaRepository extends JpaRepository<Somatometria, Long> {

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
}
