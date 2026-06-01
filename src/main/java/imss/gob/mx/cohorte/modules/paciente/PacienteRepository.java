package imss.gob.mx.cohorte.modules.paciente;

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
}
