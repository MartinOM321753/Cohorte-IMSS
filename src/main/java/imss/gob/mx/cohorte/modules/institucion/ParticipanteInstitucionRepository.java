package imss.gob.mx.cohorte.modules.institucion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipanteInstitucionRepository extends JpaRepository<ParticipanteInstitucion, Long> {

    List<ParticipanteInstitucion> findAllByPaciente_IdAndActivoTrue(Long idPaciente);

    List<ParticipanteInstitucion> findAllByInstitucion_IdAndActivoTrue(Long idInstitucion);

    Optional<ParticipanteInstitucion> findByPaciente_IdAndInstitucion_Id(Long idPaciente, Long idInstitucion);

    boolean existsByPaciente_IdAndInstitucion_IdAndActivoTrue(Long idPaciente, Long idInstitucion);
}
