package imss.gob.mx.cohorte.modules.usuarios.paciente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByFolio(String folio);

    Optional<Paciente> findByUUID(String uuid);
}
