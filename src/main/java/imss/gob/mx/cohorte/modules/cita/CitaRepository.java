package imss.gob.mx.cohorte.modules.cita;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByPaciente_Folio(String pacienteFolio);

    Optional<Cita> findByPaciente_UUID(String pacienteUUID);

    Optional<Cita> findByFechaCita(LocalDateTime fechaCita);

}
